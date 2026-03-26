package com.airline.reservation.controller;

import com.airline.reservation.dto.flight.FlightResponse;
import com.airline.reservation.dto.flight.FlightSearchRequest;
import com.airline.reservation.service.FlightService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping("/search")
    public List<FlightResponse> searchFlights(@Valid @ModelAttribute FlightSearchRequest request) {
        return flightService.searchFlights(request);
    }
}
