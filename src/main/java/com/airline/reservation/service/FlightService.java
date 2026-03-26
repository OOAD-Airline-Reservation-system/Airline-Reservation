package com.airline.reservation.service;

import com.airline.reservation.dto.flight.FlightResponse;
import com.airline.reservation.dto.flight.FlightSearchRequest;
import com.airline.reservation.entity.Flight;
import com.airline.reservation.exception.ResourceNotFoundException;
import com.airline.reservation.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FlightService {

    private final FlightRepository flightRepository;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    public List<FlightResponse> searchFlights(FlightSearchRequest request) {
        LocalDateTime start = request.getDate().atStartOfDay();
        LocalDateTime end = request.getDate().plusDays(1).atStartOfDay().minusNanos(1);
        return flightRepository.findBySourceIgnoreCaseAndDestinationIgnoreCaseAndDepartureTimeBetween(
                request.getSource(),
                request.getDestination(),
                start,
                end
        ).stream().map(this::mapToResponse).toList();
    }

    public Flight getFlightById(Long flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found: " + flightId));
    }

    private FlightResponse mapToResponse(Flight flight) {
        FlightResponse response = new FlightResponse();
        response.setId(flight.getId());
        response.setFlightNumber(flight.getFlightNumber());
        response.setSource(flight.getSource());
        response.setDestination(flight.getDestination());
        response.setDepartureTime(flight.getDepartureTime());
        response.setArrivalTime(flight.getArrivalTime());
        response.setBasePrice(flight.getBasePrice());
        return response;
    }
}
