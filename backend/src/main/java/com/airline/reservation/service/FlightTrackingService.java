package com.airline.reservation.service;

import com.airline.reservation.dto.tracking.FlightTrackingResponse;
import com.airline.reservation.entity.Flight;
import com.airline.reservation.gateway.FlightTrackingClient;
import com.airline.reservation.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class FlightTrackingService {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM HH:mm");
    private static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm");

    private final FlightRepository flightRepository;
    private final FlightTrackingClient trackingClient;

    public FlightTrackingService(FlightRepository flightRepository,
                                 FlightTrackingClient trackingClient) {
        this.flightRepository = flightRepository;
        this.trackingClient = trackingClient;
    }

    public FlightTrackingResponse trackByFlightNumber(String flightNumber) {
        // 1. Try Aviationstack live data first (works for real IATA flight numbers)
        FlightTrackingResponse live = trackingClient.fetchStatus(flightNumber);
        if (live != null) return live;

        // 2. Look up in our Firestore DB (all searched/booked flights are stored here)
        Optional<Flight> dbFlight = flightRepository.findByFlightNumber(flightNumber);
        if (dbFlight.isPresent()) {
            return deriveFromSchedule(dbFlight.get());
        }

        // 3. Not found anywhere — return helpful response (no 404)
        FlightTrackingResponse r = new FlightTrackingResponse();
        r.setFlightNumber(flightNumber);
        r.setStatus("NOT_FOUND");
        r.setCurrentLocation("N/A");
        r.setDestination("N/A");
        r.setRemarks("Flight not found. Search for a flight first — it will be trackable after booking.");
        r.setDataSource("NONE");
        return r;
    }

    private FlightTrackingResponse deriveFromSchedule(Flight flight) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dep = flight.getDepartureTime();
        LocalDateTime arr = flight.getArrivalTime();

        String status, location, remarks;

        if (now.isBefore(dep.minusHours(2))) {
            status   = "SCHEDULED";
            location = flight.getSource();
            remarks  = "Scheduled to depart " + dep.format(DISPLAY);
        } else if (now.isBefore(dep.minusMinutes(30))) {
            status   = "CHECK_IN_OPEN";
            location = flight.getSource();
            remarks  = "Check-in open · Departs " + dep.format(TIME_ONLY);
        } else if (now.isBefore(dep.plusMinutes(20))) {
            status   = "BOARDING";
            location = flight.getSource();
            remarks  = "Boarding now · Gate closes soon · Departs " + dep.format(TIME_ONLY);
        } else if (now.isBefore(arr)) {
            long total   = java.time.Duration.between(dep, arr).toMinutes();
            long elapsed = java.time.Duration.between(dep, now).toMinutes();
            int  pct     = (int) Math.min(99, (elapsed * 100) / total);
            status   = "IN_FLIGHT";
            location = flight.getSource() + " → " + flight.getDestination();
            remarks  = "En route · " + pct + "% complete · ETA " + arr.format(TIME_ONLY);
        } else if (now.isBefore(arr.plusMinutes(45))) {
            status   = "LANDED";
            location = flight.getDestination();
            remarks  = "Landed at " + flight.getDestination() + " · " + arr.format(TIME_ONLY);
        } else {
            status   = "ARRIVED";
            location = flight.getDestination();
            remarks  = "Flight completed · Arrived " + arr.format(DISPLAY);
        }

        FlightTrackingResponse r = new FlightTrackingResponse();
        r.setFlightNumber(flight.getFlightNumber());
        r.setStatus(status);
        r.setCurrentLocation(location);
        r.setDestination(flight.getDestination());
        r.setScheduledDeparture(dep.format(DISPLAY));
        r.setScheduledArrival(arr.format(DISPLAY));
        r.setRemarks(remarks);
        r.setDataSource("SCHEDULED");
        return r;
    }
}
