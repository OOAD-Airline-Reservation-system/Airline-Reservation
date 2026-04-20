package com.airline.reservation.service;

import com.airline.reservation.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";

    // All destinations reachable in our system, keyed by IATA → display name + description
    private static final Map<String, DestinationInfo> AVAILABLE_DESTINATIONS = new LinkedHashMap<>();
    static {
        // Domestic
        AVAILABLE_DESTINATIONS.put("DEL", new DestinationInfo("Delhi, India",         "Historic capital with Mughal monuments, street food, and vibrant markets"));
        AVAILABLE_DESTINATIONS.put("BOM", new DestinationInfo("Mumbai, India",         "Financial capital, Bollywood, Marine Drive, and coastal cuisine"));
        AVAILABLE_DESTINATIONS.put("BLR", new DestinationInfo("Bengaluru, India",      "Garden city, tech hub, craft breweries, and pleasant weather year-round"));
        AVAILABLE_DESTINATIONS.put("MAA", new DestinationInfo("Chennai, India",        "Classical culture, Marina Beach, temples, and South Indian cuisine"));
        AVAILABLE_DESTINATIONS.put("CCU", new DestinationInfo("Kolkata, India",        "Colonial architecture, literary heritage, Durga Puja, and street food"));
        AVAILABLE_DESTINATIONS.put("HYD", new DestinationInfo("Hyderabad, India",      "Biryani capital, Charminar, Golconda Fort, and pearl markets"));
        AVAILABLE_DESTINATIONS.put("GOI", new DestinationInfo("Goa, India",            "Beaches, Portuguese heritage, seafood, and vibrant nightlife"));
        // International
        AVAILABLE_DESTINATIONS.put("DXB", new DestinationInfo("Dubai, UAE",            "Luxury shopping, Burj Khalifa, desert safaris, and world-class dining"));
        AVAILABLE_DESTINATIONS.put("LHR", new DestinationInfo("London, UK",            "Royal history, museums, theatre, and multicultural food scene"));
        AVAILABLE_DESTINATIONS.put("SIN", new DestinationInfo("Singapore",             "Gardens by the Bay, hawker food, Marina Bay Sands, and shopping"));
        AVAILABLE_DESTINATIONS.put("JFK", new DestinationInfo("New York, USA",         "Times Square, Central Park, Broadway, and iconic skyline"));
        AVAILABLE_DESTINATIONS.put("CDG", new DestinationInfo("Paris, France",         "Eiffel Tower, Louvre, fine dining, and romantic boulevards"));
        AVAILABLE_DESTINATIONS.put("FRA", new DestinationInfo("Frankfurt, Germany",    "European finance hub, Christmas markets, and Rhine Valley day trips"));
        AVAILABLE_DESTINATIONS.put("NRT", new DestinationInfo("Tokyo, Japan",          "Temples, anime culture, ramen, cherry blossoms, and bullet trains"));
        AVAILABLE_DESTINATIONS.put("HKG", new DestinationInfo("Hong Kong",             "Dim sum, Victoria Peak, night markets, and harbour skyline"));
        AVAILABLE_DESTINATIONS.put("SYD", new DestinationInfo("Sydney, Australia",     "Opera House, Bondi Beach, harbour cruises, and wildlife"));
    }

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public GeminiService(RestTemplate restTemplate, AppProperties appProperties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> suggestTrips(String from, int budget, String interests, int duration) {
        // Build list of reachable destinations (exclude the origin itself)
        String fromUpper = from.toUpperCase().trim();
        List<String> reachable = AVAILABLE_DESTINATIONS.entrySet().stream()
                .filter(e -> !e.getKey().equals(fromUpper))
                .map(e -> e.getKey() + " (" + e.getValue().displayName + "): " + e.getValue().description)
                .toList();

        String apiKey = appProperties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return buildStubSuggestions(fromUpper, budget, reachable);
        }

        try {
            Map<String, Object> body = Map.of("contents",
                    List.of(Map.of("parts", List.of(Map.of("text",
                            buildPrompt(from, budget, interests, duration, reachable))))));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    GEMINI_URL + "?key=" + apiKey, new HttpEntity<>(body, headers), Map.class);

            if (response.getBody() == null) return buildStubSuggestions(fromUpper, budget, reachable);

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
            if (candidates == null || candidates.isEmpty()) return buildStubSuggestions(fromUpper, budget, reachable);

            String text = (String) ((List<Map<String, Object>>)
                    ((Map<String, Object>) candidates.get(0).get("content")).get("parts"))
                    .get(0).get("text");

            return parseGeminiResponse(text, fromUpper, budget, reachable);

        } catch (Exception e) {
            log.warn("Gemini API error: {}", e.getMessage());
            return buildStubSuggestions(fromUpper, budget, reachable);
        }
    }

    private String buildPrompt(String from, int budget, String interests, int duration,
                                List<String> reachable) {
        return String.format("""
                You are a travel expert for an airline booking system.
                A user is travelling FROM: %s
                Budget: INR %d total. Interests: %s. Duration: %d days.

                IMPORTANT: You MUST only suggest destinations from this exact list of available flights:
                %s

                Pick the 4 BEST matching destinations from the list above based on the user's budget and interests.
                Do NOT suggest any destination not in the list above.

                Respond ONLY with a valid JSON array. Each object must have exactly these fields:
                "destination" (city name), "iataCode" (3-letter code from the list),
                "reason" (why it matches their interests),
                "estimatedCost" (integer INR, must be within budget),
                "highlights" (array of 3 strings),
                "flightFrom" (the FROM airport code),
                "bestTimeToVisit" (string).
                Return ONLY the JSON array, no markdown, no explanation.
                """, from, budget, interests, duration, String.join("\n", reachable));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseGeminiResponse(String text, String from, int budget,
                                                           List<String> reachable) {
        try {
            String clean = text.replaceAll("```json", "").replaceAll("```", "").trim();
            List<Map<String, Object>> suggestions = objectMapper.readValue(clean, List.class);
            // Validate every suggestion is in our available destinations
            return suggestions.stream()
                    .filter(s -> {
                        String iata = (String) s.get("iataCode");
                        return iata != null && AVAILABLE_DESTINATIONS.containsKey(iata.toUpperCase());
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response: {}", e.getMessage());
            return buildStubSuggestions(from, budget, reachable);
        }
    }

    private List<Map<String, Object>> buildStubSuggestions(String from, int budget,
                                                             List<String> reachable) {
        // Pick 4 destinations that fit the budget from available routes
        List<Map<String, Object>> result = new ArrayList<>();

        Map<String, int[]> costs = Map.of(
            "GOI", new int[]{(int)(budget * 0.4), 2800},
            "BLR", new int[]{(int)(budget * 0.45), 3800},
            "BOM", new int[]{(int)(budget * 0.5), 5500},
            "DEL", new int[]{(int)(budget * 0.5), 5500},
            "MAA", new int[]{(int)(budget * 0.55), 4200},
            "DXB", new int[]{(int)(budget * 0.7), 18500},
            "SIN", new int[]{(int)(budget * 0.75), 22000},
            "LHR", new int[]{(int)(budget * 0.9), 45000},
            "JFK", new int[]{(int)(budget * 0.95), 65000}
        );

        for (Map.Entry<String, DestinationInfo> entry : AVAILABLE_DESTINATIONS.entrySet()) {
            if (result.size() >= 4) break;
            String iata = entry.getKey();
            if (iata.equals(from)) continue;
            DestinationInfo info = entry.getValue();
            int[] costInfo = costs.getOrDefault(iata, new int[]{(int)(budget * 0.6), 5000});
            if (costInfo[1] > budget) continue; // skip if flight price alone exceeds budget

            result.add(Map.of(
                "destination",   info.displayName,
                "iataCode",      iata,
                "reason",        info.description,
                "estimatedCost", costInfo[0],
                "highlights",    List.of("Local cuisine", "Cultural sites", "Shopping"),
                "flightFrom",    from,
                "bestTimeToVisit", "Oct–Mar"
            ));
        }

        // If nothing fits budget, return top 4 cheapest regardless
        if (result.isEmpty()) {
            AVAILABLE_DESTINATIONS.entrySet().stream()
                .filter(e -> !e.getKey().equals(from))
                .limit(4)
                .forEach(e -> result.add(Map.of(
                    "destination",   e.getValue().displayName,
                    "iataCode",      e.getKey(),
                    "reason",        e.getValue().description,
                    "estimatedCost", budget,
                    "highlights",    List.of("Local cuisine", "Cultural sites", "Shopping"),
                    "flightFrom",    from,
                    "bestTimeToVisit", "Oct–Mar"
                )));
        }
        return result;
    }

    private static class DestinationInfo {
        final String displayName, description;
        DestinationInfo(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
    }
}
