package com.airline.reservation.controller;

import com.airline.reservation.dto.tracking.FlightTrackingResponse;
import com.airline.reservation.service.FlightTrackingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
public class FlightTrackingController {

    private final FlightTrackingService flightTrackingService;

    public FlightTrackingController(FlightTrackingService flightTrackingService) {
        this.flightTrackingService = flightTrackingService;
    }

    @GetMapping("/{flightNumber}")
    public FlightTrackingResponse trackFlight(@PathVariable String flightNumber) {
        return flightTrackingService.trackByFlightNumber(flightNumber);
    }
}
