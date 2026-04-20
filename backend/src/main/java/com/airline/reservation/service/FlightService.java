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

        // Fetch from route templates (works for any date)
        List<Flight> apiFlights = aeroDataBoxClient.fetchDepartures(source, destination, request.getDate());

        if (!apiFlights.isEmpty()) {
            List<FlightResponse> responses = new ArrayList<>();
            for (Flight f : apiFlights) {
                // Parse display fields before saving plain IATA to Firestore
                String[] srcParts = f.getSource().split("\\|", 3);
                String[] dstParts = f.getDestination().split("\\|", 2);
                String srcIata     = srcParts[0];
                String srcAirport  = srcParts.length > 1 ? srcParts[1] : srcParts[0];
                String airline     = srcParts.length > 2 ? srcParts[2] : "";
                String dstIata     = dstParts[0];
                String dstAirport  = dstParts.length > 1 ? dstParts[1] : dstParts[0];

                // Store plain IATA in Firestore for reliable querying
                f.setSource(srcIata);
                f.setDestination(dstIata);

                // Upsert: use flightNumber+date as stable ID to avoid duplicates
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

        // Fallback: Firestore cache
        log.warn("No route templates for {}->{}, checking Firestore cache", source, destination);
        var start = request.getDate().atStartOfDay();
        var end   = request.getDate().plusDays(1).atStartOfDay().minusNanos(1);
        return flightRepository
                .findBySourceAndDestinationAndDepartureBetween(source, destination, start, end)
                .stream().map(this::toResponse).toList();
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
        for (int row = 1; row <= 4; row++) {
            seats.add(seat(flight, row + "A", SeatClass.BUSINESS, "9500.00"));
            seats.add(seat(flight, row + "B", SeatClass.BUSINESS, "9500.00"));
        }
        for (int row = 10; row <= 14; row++) {
            seats.add(seat(flight, row + "A", SeatClass.ECONOMY, flight.getBasePrice().toPlainString()));
            seats.add(seat(flight, row + "B", SeatClass.ECONOMY, flight.getBasePrice().toPlainString()));
            seats.add(seat(flight, row + "C", SeatClass.ECONOMY, flight.getBasePrice().toPlainString()));
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
