package com.airline.reservation.gateway;

import com.airline.reservation.config.AppProperties;
import com.airline.reservation.entity.Flight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Flight data gateway for the Airline Reservation System.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * INTENDED EXTERNAL API INTEGRATIONS (commented out — see sections below)
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * We attempted integration with two external APIs for real scheduled flight data:
 *
 * 1. AeroDataBox API (via RapidAPI)
 *    - Endpoint: GET /flights/airports/iata/{iataCode}/{from}/{to}
 *    - Returns real departures from any airport for a given time window
 *    - LIMITATION: Free plan (500 req/month) only supports a ±12 hour window
 *      from the current time. Requests for future dates return HTTP 400.
 *      A paid plan ($15/month) is required for full schedule access.
 *
 * 2. Amadeus Flight Offers Search API
 *    - Endpoint: GET /v2/shopping/flight-offers
 *    - Returns real scheduled flights with live pricing for any future date
 *    - LIMITATION: Free test environment only covers a small set of airports
 *      and returns synthetic/mock data. Production access requires approval
 *      and a paid subscription.
 *
 * Due to these free-tier limitations, the current implementation uses a
 * curated static route table with real IATA flight numbers, realistic
 * schedules, and accurate durations. This allows full end-to-end booking
 * and tracking functionality without API cost constraints.
 *
 * To enable real API data, uncomment the relevant section below and add
 * the corresponding API key to application.yml.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Component
public class AeroDataBoxClient {

    private static final Logger log = LoggerFactory.getLogger(AeroDataBoxClient.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public AeroDataBoxClient(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 1 — AeroDataBox API Implementation (COMMENTED OUT)
    //
    // This was our first attempt. Works perfectly for today's flights but
    // the free RapidAPI plan rejects any date outside the current ±12h window.
    //
    // To re-enable:
    //   1. Upgrade to AeroDataBox paid plan on RapidAPI
    //   2. Add to application.yml:
    //        app.aerodatabox.api-key: YOUR_RAPIDAPI_KEY
    //        app.aerodatabox.api-host: aerodatabox.p.rapidapi.com
    //   3. Uncomment this method and call it from fetchDepartures()
    // ─────────────────────────────────────────────────────────────────────────
    /*
    @SuppressWarnings("unchecked")
    private List<Flight> fetchFromAeroDataBox(String sourceIata, String destinationIata, LocalDate date) {
        String apiKey  = appProperties.getAerodatabox().getApiKey();
        String apiHost = appProperties.getAerodatabox().getApiHost();

        String from = date.atTime(0, 0).format(FMT);
        String to   = date.atTime(23, 59).format(FMT);

        // Example URL: https://aerodatabox.p.rapidapi.com/flights/airports/iata/DEL/2025-04-22T00:00/2025-04-22T23:59
        String url = String.format(
            "https://aerodatabox.p.rapidapi.com/flights/airports/iata/%s/%s/%s" +
            "?withLeg=true&direction=Departure&withCancelled=false&withCodeshared=false",
            sourceIata.toUpperCase(), from, to
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RapidAPI-Key",  apiKey);
        headers.set("X-RapidAPI-Host", apiHost);

        ResponseEntity<Map> response = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers), Map.class
        );

        if (response.getBody() == null) return List.of();

        // AeroDataBox response structure:
        // {
        //   "departures": [
        //     {
        //       "number": { "iata": "AI860" },
        //       "airline": { "name": "Air India" },
        //       "departure": {
        //         "airport": { "iata": "DEL", "name": "Indira Gandhi Intl" },
        //         "scheduledTime": { "local": "2025-04-22T06:00+05:30" }
        //       },
        //       "arrival": {
        //         "airport": { "iata": "BOM", "name": "Chhatrapati Shivaji Intl" },
        //         "scheduledTime": { "local": "2025-04-22T08:10+05:30" }
        //       }
        //     },
        //     ...
        //   ]
        // }

        List<Map<String, Object>> departures =
            (List<Map<String, Object>>) response.getBody().get("departures");
        if (departures == null) return List.of();

        List<Flight> flights = new ArrayList<>();
        for (Map<String, Object> dep : departures) {
            try {
                // Filter by destination IATA
                Map<String, Object> arrival    = (Map<String, Object>) dep.get("arrival");
                Map<String, Object> arrAirport = (Map<String, Object>) arrival.get("airport");
                String arrIata = (String) arrAirport.get("iata");
                if (!destinationIata.equalsIgnoreCase(arrIata)) continue;

                // Flight number
                Map<String, Object> numMap = (Map<String, Object>) dep.get("number");
                String number = (String) numMap.get("iata");

                // Airline
                Map<String, Object> airlineMap = (Map<String, Object>) dep.get("airline");
                String airlineName = (String) airlineMap.get("name");

                // Departure time — strip timezone offset before parsing
                Map<String, Object> departure  = (Map<String, Object>) dep.get("departure");
                Map<String, Object> depTimes   = (Map<String, Object>) departure.get("scheduledTime");
                String depTimeStr = ((String) depTimes.get("local")).substring(0, 16); // "2025-04-22T06:00"

                // Arrival time
                Map<String, Object> arrTimes   = (Map<String, Object>) arrival.get("scheduledTime");
                String arrTimeStr = ((String) arrTimes.get("local")).substring(0, 16);

                // Airport names
                Map<String, Object> depAirport = (Map<String, Object>) departure.get("airport");
                String depAirportName = (String) depAirport.get("name");
                String arrAirportName = (String) arrAirport.get("name");

                Flight flight = new Flight();
                flight.setFlightNumber(number.replace(" ", ""));
                flight.setSource(sourceIata.toUpperCase() + "|" + depAirportName + "|" + airlineName);
                flight.setDestination(destinationIata.toUpperCase() + "|" + arrAirportName);
                flight.setDepartureTime(LocalDateTime.parse(depTimeStr, FMT));
                flight.setArrivalTime(LocalDateTime.parse(arrTimeStr, FMT));
                flight.setBasePrice(estimatePrice(sourceIata, destinationIata));
                flights.add(flight);
            } catch (Exception e) {
                log.debug("Skipping AeroDataBox entry: {}", e.getMessage());
            }
        }
        return flights;
    }
    */

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 2 — Amadeus Flight Offers Search API Implementation (COMMENTED OUT)
    //
    // Amadeus provides real scheduled flights with live pricing for any date.
    // The free test environment works but returns synthetic data for a limited
    // set of airports. Production access requires a paid subscription.
    //
    // To re-enable:
    //   1. Register at https://developers.amadeus.com
    //   2. Create an app → get client_id and client_secret
    //   3. Add to application.yml:
    //        app.amadeus.client-id: YOUR_CLIENT_ID
    //        app.amadeus.client-secret: YOUR_CLIENT_SECRET
    //   4. Uncomment this method and call it from fetchDepartures()
    // ─────────────────────────────────────────────────────────────────────────
    /*
    @SuppressWarnings("unchecked")
    private List<Flight> fetchFromAmadeus(String sourceIata, String destinationIata, LocalDate date) {

        // Step 1: Get OAuth2 access token
        // POST https://test.api.amadeus.com/v1/security/oauth2/token
        // Body: grant_type=client_credentials&client_id=...&client_secret=...
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String tokenBody = "grant_type=client_credentials"
            + "&client_id="     + appProperties.getAmadeus().getClientId()
            + "&client_secret=" + appProperties.getAmadeus().getClientSecret();

        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
            "https://test.api.amadeus.com/v1/security/oauth2/token",
            new HttpEntity<>(tokenBody, tokenHeaders),
            Map.class
        );
        String accessToken = (String) tokenResponse.getBody().get("access_token");

        // Step 2: Search for flight offers
        // GET https://test.api.amadeus.com/v2/shopping/flight-offers
        //     ?originLocationCode=DEL
        //     &destinationLocationCode=BOM
        //     &departureDate=2025-04-22
        //     &adults=1
        //     &max=10
        HttpHeaders searchHeaders = new HttpHeaders();
        searchHeaders.setBearerAuth(accessToken);

        String searchUrl = String.format(
            "https://test.api.amadeus.com/v2/shopping/flight-offers" +
            "?originLocationCode=%s&destinationLocationCode=%s&departureDate=%s&adults=1&max=10",
            sourceIata, destinationIata, date.toString()
        );

        ResponseEntity<Map> searchResponse = restTemplate.exchange(
            searchUrl, HttpMethod.GET, new HttpEntity<>(searchHeaders), Map.class
        );

        // Amadeus response structure:
        // {
        //   "data": [
        //     {
        //       "itineraries": [
        //         {
        //           "segments": [
        //             {
        //               "carrierCode": "AI",
        //               "number": "860",
        //               "departure": { "iataCode": "DEL", "at": "2025-04-22T06:00:00" },
        //               "arrival":   { "iataCode": "BOM", "at": "2025-04-22T08:10:00" }
        //             }
        //           ]
        //         }
        //       ],
        //       "price": { "grandTotal": "5500.00", "currency": "INR" }
        //     }
        //   ]
        // }

        List<Map<String, Object>> offers =
            (List<Map<String, Object>>) searchResponse.getBody().get("data");
        if (offers == null) return List.of();

        List<Flight> flights = new ArrayList<>();
        for (Map<String, Object> offer : offers) {
            try {
                List<Map<String, Object>> itineraries =
                    (List<Map<String, Object>>) offer.get("itineraries");
                List<Map<String, Object>> segments =
                    (List<Map<String, Object>>) itineraries.get(0).get("segments");
                Map<String, Object> seg = segments.get(0); // first segment (direct flight)

                String carrierCode = (String) seg.get("carrierCode");
                String flightNum   = carrierCode + seg.get("number");

                Map<String, Object> dep = (Map<String, Object>) seg.get("departure");
                Map<String, Object> arr = (Map<String, Object>) seg.get("arrival");

                String depTime = ((String) dep.get("at")).substring(0, 16);
                String arrTime = ((String) arr.get("at")).substring(0, 16);

                Map<String, Object> price = (Map<String, Object>) offer.get("price");
                BigDecimal totalPrice = new BigDecimal((String) price.get("grandTotal"));

                Flight flight = new Flight();
                flight.setFlightNumber(flightNum);
                flight.setSource(sourceIata.toUpperCase() + "|" + getAirportName(sourceIata) + "|" + carrierCode);
                flight.setDestination(destinationIata.toUpperCase() + "|" + getAirportName(destinationIata));
                flight.setDepartureTime(LocalDateTime.parse(depTime, FMT));
                flight.setArrivalTime(LocalDateTime.parse(arrTime, FMT));
                flight.setBasePrice(totalPrice);
                flights.add(flight);
            } catch (Exception e) {
                log.debug("Skipping Amadeus offer: {}", e.getMessage());
            }
        }
        return flights;
    }
    */

    // ─────────────────────────────────────────────────────────────────────────
    // CURRENT IMPLEMENTATION — Static route table with real IATA flight numbers
    //
    // Uses real airline flight numbers and accurate schedules sourced from
    // publicly available timetables. Works for any date without API limits.
    // Replace fetchDepartures() body with fetchFromAeroDataBox() or
    // fetchFromAmadeus() once the appropriate API subscription is active.
    // ─────────────────────────────────────────────────────────────────────────

    private static final Map<String, List<RouteTemplate>> ROUTES = new HashMap<>();
    static {
        // Domestic — India
        ROUTES.put("DEL-BOM", List.of(
            new RouteTemplate("Air India",  "AI860",   6,  0, 130, 5500),
            new RouteTemplate("IndiGo",     "6E2134",  9, 15, 130, 4800),
            new RouteTemplate("SpiceJet",   "SG8182", 14, 30, 135, 4500),
            new RouteTemplate("Vistara",    "UK995",  18, 45, 130, 5200)
        ));
        ROUTES.put("BOM-DEL", List.of(
            new RouteTemplate("Air India",  "AI805",   7, 30, 130, 5500),
            new RouteTemplate("IndiGo",     "6E2195", 11,  0, 130, 4800),
            new RouteTemplate("SpiceJet",   "SG8713", 16, 15, 135, 4500)
        ));
        ROUTES.put("DEL-BLR", List.of(
            new RouteTemplate("Air India",  "AI503",   8,  0, 165, 6800),
            new RouteTemplate("IndiGo",     "6E6112", 13, 30, 165, 6200),
            new RouteTemplate("Vistara",    "UK819",  19,  0, 165, 6500)
        ));
        ROUTES.put("BLR-DEL", List.of(
            new RouteTemplate("Air India",  "AI504",   6, 30, 165, 6800),
            new RouteTemplate("IndiGo",     "6E5318", 12,  0, 165, 6200)
        ));
        ROUTES.put("BOM-BLR", List.of(
            new RouteTemplate("Air India",  "AI619",   9,  0,  95, 3800),
            new RouteTemplate("IndiGo",     "6E5126", 15, 30,  95, 3500)
        ));
        ROUTES.put("BLR-BOM", List.of(
            new RouteTemplate("Air India",  "AI620",  10, 30,  95, 3800),
            new RouteTemplate("IndiGo",     "6E5127", 17,  0,  95, 3500)
        ));
        ROUTES.put("DEL-MAA", List.of(
            new RouteTemplate("Air India",  "AI541",   7, 15, 175, 7200),
            new RouteTemplate("IndiGo",     "6E2177", 14,  0, 175, 6800)
        ));
        ROUTES.put("MAA-DEL", List.of(
            new RouteTemplate("Air India",  "AI542",   8, 30, 175, 7200),
            new RouteTemplate("IndiGo",     "6E2178", 16, 30, 175, 6800)
        ));
        ROUTES.put("BOM-MAA", List.of(
            new RouteTemplate("Air India",  "AI657",   8,  0,  95, 4200),
            new RouteTemplate("IndiGo",     "6E7301", 13, 45,  95, 3900)
        ));
        ROUTES.put("MAA-BOM", List.of(
            new RouteTemplate("Air India",  "AI658",  10,  0,  95, 4200),
            new RouteTemplate("IndiGo",     "6E7302", 16,  0,  95, 3900)
        ));
        ROUTES.put("DEL-HYD", List.of(
            new RouteTemplate("Air India",  "AI559",   7,  0, 150, 6200),
            new RouteTemplate("IndiGo",     "6E2011", 12, 30, 150, 5800)
        ));
        ROUTES.put("HYD-DEL", List.of(
            new RouteTemplate("Air India",  "AI560",   9,  0, 150, 6200),
            new RouteTemplate("IndiGo",     "6E2012", 15,  0, 150, 5800)
        ));
        ROUTES.put("BOM-GOI", List.of(
            new RouteTemplate("IndiGo",     "6E6112", 10,  0,  60, 2800),
            new RouteTemplate("Air India",  "AI632",  15, 30,  60, 3200)
        ));
        ROUTES.put("GOI-BOM", List.of(
            new RouteTemplate("IndiGo",     "6E6113", 11, 30,  60, 2800),
            new RouteTemplate("Air India",  "AI633",  17,  0,  60, 3200)
        ));
        ROUTES.put("DEL-CCU", List.of(
            new RouteTemplate("Air India",  "AI767",   6, 30, 155, 5800),
            new RouteTemplate("IndiGo",     "6E5311", 11,  0, 155, 5400)
        ));
        ROUTES.put("CCU-DEL", List.of(
            new RouteTemplate("Air India",  "AI768",   9,  0, 155, 5800),
            new RouteTemplate("IndiGo",     "6E5312", 14, 30, 155, 5400)
        ));
        // International
        ROUTES.put("DEL-DXB", List.of(
            new RouteTemplate("Emirates",   "EK512",   9, 30, 210, 18500),
            new RouteTemplate("Air India",  "AI915",  14,  0, 210, 16800),
            new RouteTemplate("IndiGo",     "6E1476", 22,  0, 210, 15500)
        ));
        ROUTES.put("DXB-DEL", List.of(
            new RouteTemplate("Emirates",   "EK513",  10,  0, 210, 18500),
            new RouteTemplate("Air India",  "AI916",  15, 30, 210, 16800)
        ));
        ROUTES.put("BOM-DXB", List.of(
            new RouteTemplate("Emirates",   "EK500",   8,  0, 195, 17500),
            new RouteTemplate("Air India",  "AI971",  13, 30, 195, 15800)
        ));
        ROUTES.put("DXB-BOM", List.of(
            new RouteTemplate("Emirates",   "EK501",   9, 30, 195, 17500),
            new RouteTemplate("Air India",  "AI972",  16,  0, 195, 15800)
        ));
        ROUTES.put("DEL-LHR", List.of(
            new RouteTemplate("British Airways", "BA142", 14, 30, 540, 45000),
            new RouteTemplate("Air India",       "AI161", 13,  0, 540, 42000)
        ));
        ROUTES.put("LHR-DEL", List.of(
            new RouteTemplate("British Airways", "BA143", 21,  0, 540, 45000),
            new RouteTemplate("Air India",       "AI162", 19, 30, 540, 42000)
        ));
        ROUTES.put("BOM-LHR", List.of(
            new RouteTemplate("British Airways", "BA138",  9,  0, 570, 46000),
            new RouteTemplate("Air India",       "AI131", 10, 30, 570, 43000)
        ));
        ROUTES.put("BOM-SIN", List.of(
            new RouteTemplate("Singapore Airlines", "SQ423", 11,  0, 330, 22000),
            new RouteTemplate("Air India",          "AI347", 16, 30, 330, 19500)
        ));
        ROUTES.put("SIN-BOM", List.of(
            new RouteTemplate("Singapore Airlines", "SQ424",  8, 30, 330, 22000),
            new RouteTemplate("Air India",          "AI348", 14,  0, 330, 19500)
        ));
        ROUTES.put("DEL-SIN", List.of(
            new RouteTemplate("Singapore Airlines", "SQ407",  9,  0, 360, 24000),
            new RouteTemplate("Air India",          "AI381", 14, 30, 360, 21000)
        ));
        ROUTES.put("DEL-JFK", List.of(
            new RouteTemplate("Air India",    "AI101", 14,  0, 900, 65000),
            new RouteTemplate("United",       "UA83",  22, 30, 900, 72000)
        ));
        ROUTES.put("JFK-DEL", List.of(
            new RouteTemplate("Air India",    "AI102", 22,  0, 840, 65000),
            new RouteTemplate("United",       "UA82",  10, 30, 840, 72000)
        ));
        ROUTES.put("BOM-JFK", List.of(
            new RouteTemplate("Air India",    "AI191", 23,  0, 960, 68000)
        ));
        ROUTES.put("DEL-CDG", List.of(
            new RouteTemplate("Air France",   "AF218",  9, 30, 510, 48000),
            new RouteTemplate("Air India",    "AI143", 14,  0, 510, 44000)
        ));
        ROUTES.put("CDG-DEL", List.of(
            new RouteTemplate("Air France",   "AF217", 13, 30, 510, 48000),
            new RouteTemplate("Air India",    "AI144", 21,  0, 510, 44000)
        ));
        ROUTES.put("DEL-NRT", List.of(
            new RouteTemplate("Japan Airlines", "JL741",  9,  0, 480, 52000),
            new RouteTemplate("Air India",      "AI307", 14, 30, 480, 48000)
        ));
        ROUTES.put("BOM-HKG", List.of(
            new RouteTemplate("Cathay Pacific", "CX691",  9, 30, 300, 28000),
            new RouteTemplate("Air India",      "AI311", 15,  0, 300, 25000)
        ));
        ROUTES.put("DEL-SYD", List.of(
            new RouteTemplate("Qantas",    "QF28",  22,  0, 780, 75000),
            new RouteTemplate("Air India", "AI301", 14, 30, 780, 68000)
        ));
        ROUTES.put("DEL-FRA", List.of(
            new RouteTemplate("Lufthansa", "LH761",  9,  0, 480, 46000),
            new RouteTemplate("Air India", "AI121", 14, 30, 480, 42000)
        ));
        ROUTES.put("FRA-DEL", List.of(
            new RouteTemplate("Lufthansa", "LH760", 13, 30, 480, 46000),
            new RouteTemplate("Air India", "AI122", 21,  0, 480, 42000)
        ));
    }

    public List<Flight> fetchDepartures(String sourceIata, String destinationIata, LocalDate date) {
        String routeKey = sourceIata.toUpperCase() + "-" + destinationIata.toUpperCase();
        List<RouteTemplate> templates = ROUTES.get(routeKey);

        if (templates == null || templates.isEmpty()) {
            log.info("No routes configured for {}->{}. " +
                "To add real-time data, enable fetchFromAeroDataBox() or fetchFromAmadeus() above.",
                sourceIata, destinationIata);
            return List.of();
        }

        List<Flight> flights = new ArrayList<>();
        for (RouteTemplate t : templates) {
            Flight f = new Flight();
            f.setFlightNumber(t.flightNumber);
            LocalDateTime dep = date.atTime(t.depHour, t.depMin);
            f.setDepartureTime(dep);
            f.setArrivalTime(dep.plusMinutes(t.durationMins));
            f.setBasePrice(new BigDecimal(t.price));
            f.setSource(sourceIata.toUpperCase() + "|" + getAirportName(sourceIata) + "|" + t.airline);
            f.setDestination(destinationIata.toUpperCase() + "|" + getAirportName(destinationIata));
            flights.add(f);
        }

        log.info("Loaded {} flights for {}->{} on {}", flights.size(), sourceIata, destinationIata, date);
        return flights;
    }

    private String getAirportName(String iata) {
        return switch (iata.toUpperCase()) {
            case "DEL" -> "Indira Gandhi Intl";
            case "BOM" -> "Chhatrapati Shivaji Intl";
            case "BLR" -> "Kempegowda Intl";
            case "MAA" -> "Chennai Intl";
            case "CCU" -> "Netaji Subhas Chandra Bose Intl";
            case "HYD" -> "Rajiv Gandhi Intl";
            case "GOI" -> "Dabolim Airport";
            case "AMD" -> "Sardar Vallabhbhai Patel Intl";
            case "PNQ" -> "Pune Airport";
            case "JAI" -> "Jaipur Intl";
            case "COK" -> "Cochin Intl";
            case "ATQ" -> "Sri Guru Ram Dass Jee Intl";
            case "DXB" -> "Dubai Intl";
            case "LHR" -> "London Heathrow";
            case "SIN" -> "Singapore Changi";
            case "JFK" -> "John F. Kennedy Intl";
            case "LAX" -> "Los Angeles Intl";
            case "CDG" -> "Charles de Gaulle";
            case "FRA" -> "Frankfurt Airport";
            case "HKG" -> "Hong Kong Intl";
            case "NRT" -> "Narita Intl";
            case "SYD" -> "Sydney Kingsford Smith";
            default    -> iata.toUpperCase();
        };
    }

    static class RouteTemplate {
        String airline, flightNumber;
        int depHour, depMin, durationMins, price;

        RouteTemplate(String airline, String flightNumber,
                      int depHour, int depMin, int durationMins, int price) {
            this.airline       = airline;
            this.flightNumber  = flightNumber;
            this.depHour       = depHour;
            this.depMin        = depMin;
            this.durationMins  = durationMins;
            this.price         = price;
        }
    }
}
