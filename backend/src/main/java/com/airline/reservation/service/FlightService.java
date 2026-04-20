package com.airline.reservation.service;

import com.airline.reservation.dto.flight.FlightResponse;
import com.airline.reservation.dto.flight.FlightSearchRequest;
import com.airline.reservation.entity.Flight;
import com.airline.reservation.entity.Seat;
import com.airline.reservation.entity.SeatClass;
import com.airline.reservation.exception.ResourceNotFoundException;
import com.airline.reservation.gateway.AeroDataBoxClient;
import com.airline.reservation.repository.FlightRepository;
import com.airline.reservation.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class FlightService {

    private static final Logger log = LoggerFactory.getLogger(FlightService.class);

    private final FlightRepository flightRepository;
    private final SeatRepository seatRepository;
    private final AeroDataBoxClient aeroDataBoxClient;

    public FlightService(FlightRepository flightRepository,
                         SeatRepository seatRepository,
                         AeroDataBoxClient aeroDataBoxClient) {
        this.flightRepository = flightRepository;
        this.seatRepository = seatRepository;
        this.aeroDataBoxClient = aeroDataBoxClient;
    }

    public List<FlightResponse> searchFlights(FlightSearchRequest request) {
        String source = request.getSource().toUpperCase().trim();
        String destination = request.getDestination().toUpperCase().trim();

        List<Flight> apiFlights = aeroDataBoxClient.fetchDepartures(source, destination, request.getDate());

        if (!apiFlights.isEmpty()) {
            List<FlightResponse> responses = new ArrayList<>();
            for (Flight f : apiFlights) {
                String[] srcParts = f.getSource().split("\\|", 3);
                String[] dstParts = f.getDestination().split("\\|", 2);
                String srcIata    = srcParts[0];
                String srcAirport = srcParts.length > 1 ? srcParts[1] : srcParts[0];
                String airline    = srcParts.length > 2 ? srcParts[2] : "";
                String dstIata    = dstParts[0];
                String dstAirport = dstParts.length > 1 ? dstParts[1] : dstParts[0];

                f.setSource(srcIata);
                f.setDestination(dstIata);

                // Skip flights that have already departed
                if (f.getDepartureTime() != null && f.getDepartureTime().isBefore(java.time.LocalDateTime.now())) {
                    continue;
                }

                String stableId = f.getFlightNumber() + "-" + request.getDate().toString();
                f.setId(stableId);
                flightRepository.save(f);

                if (seatRepository.findByFlightId(f.getId()).isEmpty()) {
                    seatRepository.saveAll(buildSeats(f));
                }

                FlightResponse r = toResponse(f);
                r.setSourceAirport(srcAirport);
                r.setDestinationAirport(dstAirport);
                r.setAirline(airline);
                responses.add(r);
            }
            return responses;
        }

        // Fallback: Firestore cache — also filter past flights
        log.warn("No route templates for {}->{}, checking Firestore cache", source, destination);
        var start = request.getDate().atStartOfDay();
        var end   = request.getDate().plusDays(1).atStartOfDay().minusNanos(1);
        var now   = java.time.LocalDateTime.now();
        return flightRepository
                .findBySourceAndDestinationAndDepartureBetween(source, destination, start, end)
                .stream()
                .filter(f -> f.getDepartureTime() == null || f.getDepartureTime().isAfter(now))
                .map(this::toResponse).toList();
    }

    public Flight getFlightById(String flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found: " + flightId));
    }

    private FlightResponse toResponse(Flight f) {
        FlightResponse r = new FlightResponse();
        r.setId(f.getId());
        r.setFlightNumber(f.getFlightNumber());
        r.setSource(f.getSource());
        r.setDestination(f.getDestination());
        r.setSourceAirport(f.getSource());
        r.setDestinationAirport(f.getDestination());
        r.setDepartureTime(f.getDepartureTime());
        r.setArrivalTime(f.getArrivalTime());
        r.setBasePrice(f.getBasePrice());
        return r;
    }

    private List<Seat> buildSeats(Flight flight) {
        List<Seat> seats = new ArrayList<>();
        // Business class: rows 1-4, 2+2 config (A B | C D)
        String[] busCols = {"A", "B", "C", "D"};
        for (int row = 1; row <= 4; row++) {
            for (String col : busCols) {
                seats.add(seat(flight, row + col, SeatClass.BUSINESS, "9500.00"));
            }
        }
        // Economy class: rows 10-25, 3+3 config (A B C | D E F)
        String[] ecoCols = {"A", "B", "C", "D", "E", "F"};
        for (int row = 10; row <= 25; row++) {
            for (String col : ecoCols) {
                seats.add(seat(flight, row + col, SeatClass.ECONOMY, flight.getBasePrice().toPlainString()));
            }
        }
        return seats;
    }

    private Seat seat(Flight f, String number, SeatClass cls, String price) {
        Seat s = new Seat();
        s.setFlightId(f.getId());
        s.setSeatNumber(number);
        s.setSeatClass(cls);
        s.setPrice(new BigDecimal(price));
        return s;
    }
}
