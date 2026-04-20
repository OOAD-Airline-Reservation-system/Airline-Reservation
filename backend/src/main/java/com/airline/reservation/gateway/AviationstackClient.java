package com.airline.reservation.gateway;

import com.airline.reservation.config.AppProperties;
import com.airline.reservation.dto.tracking.FlightTrackingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Component
public class AviationstackClient implements FlightTrackingClient {

    private static final Logger log = LoggerFactory.getLogger(AviationstackClient.class);

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public AviationstackClient(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public FlightTrackingResponse fetchStatus(String flightNumber) {
        String apiKey = appProperties.getAviationstack().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return null; // caller handles fallback
        }

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(appProperties.getAviationstack().getBaseUrl() + "/flights")
                    .queryParam("access_key", apiKey)
                    .queryParam("flight_iata", flightNumber)
                    .queryParam("limit", 1)
                    .toUriString();

            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body == null) return null;

            // Check for API error
            if (body.containsKey("error")) {
                log.warn("Aviationstack error: {}", body.get("error"));
                return null;
            }

            List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
            if (data == null || data.isEmpty()) return null;

            Map<String, Object> entry = data.get(0);
            String status = str(entry, "flight_status", "UNKNOWN").toUpperCase();

            Map<String, Object> dep = map(entry, "departure");
            Map<String, Object> arr = map(entry, "arrival");
            Map<String, Object> airline = map(entry, "airline");
            Map<String, Object> flightInfo = map(entry, "flight");

            String depAirport  = dep != null ? str(dep, "airport", "") : "";
            String depIata     = dep != null ? str(dep, "iata", "") : "";
            String arrAirport  = arr != null ? str(arr, "airport", "") : "";
            String arrIata     = arr != null ? str(arr, "iata", "") : "";
            String schedDep    = dep != null ? str(dep, "scheduled", "") : "";
            String actualDep   = dep != null ? str(dep, "actual", schedDep) : "";
            String schedArr    = arr != null ? str(arr, "scheduled", "") : "";
            String actualArr   = arr != null ? str(arr, "actual", schedArr) : "";
            String depTerminal = dep != null ? str(dep, "terminal", "") : "";
            String arrTerminal = arr != null ? str(arr, "terminal", "") : "";
            String depGate     = dep != null ? str(dep, "gate", "") : "";
            String airlineName = airline != null ? str(airline, "name", "") : "";

            FlightTrackingResponse r = new FlightTrackingResponse();
            r.setFlightNumber(flightNumber);
            r.setAirline(airlineName);
            r.setStatus(status);
            r.setCurrentLocation(depIata.isBlank() ? depAirport : depIata + " · " + depAirport);
            r.setDestination(arrIata.isBlank() ? arrAirport : arrIata + " · " + arrAirport);
            r.setScheduledDeparture(formatTime(schedDep));
            r.setActualDeparture(formatTime(actualDep));
            r.setScheduledArrival(formatTime(schedArr));
            r.setActualArrival(formatTime(actualArr));
            r.setDepartureTerminal(depTerminal);
            r.setArrivalTerminal(arrTerminal);
            r.setDepartureGate(depGate);
            r.setRemarks("Live data from Aviationstack");
            r.setDataSource("LIVE");
            return r;

        } catch (Exception ex) {
            log.error("Aviationstack error for {}: {}", flightNumber, ex.getMessage());
            return null;
        }
    }

    private String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null ? v.toString() : def;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    private String formatTime(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            // "2025-04-21T09:00:00+05:30" -> "09:00"
            return iso.substring(11, 16);
        } catch (Exception e) {
            return iso;
        }
    }
}
