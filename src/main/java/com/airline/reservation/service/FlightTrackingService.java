package com.airline.reservation.service;

import com.airline.reservation.dto.tracking.FlightTrackingResponse;
import com.airline.reservation.exception.ResourceNotFoundException;
import com.airline.reservation.repository.FlightRepository;
import org.springframework.stereotype.Service;

@Service
public class FlightTrackingService {

    private final FlightRepository flightRepository;

    public FlightTrackingService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    public FlightTrackingResponse trackByFlightNumber(String flightNumber) {
        var flight = flightRepository.findByFlightNumber(flightNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found: " + flightNumber));

        FlightTrackingResponse response = new FlightTrackingResponse();
        response.setFlightNumber(flight.getFlightNumber());
        response.setStatus("SCHEDULED");
        response.setCurrentLocation(flight.getSource());
        response.setRemarks("Stub tracking response. Replace with an external provider integration when available.");
        return response;
    }
}
