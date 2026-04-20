package com.airline.reservation.controller;

import com.airline.reservation.dto.flight.FlightResponse;
import com.airline.reservation.dto.flight.FlightSearchRequest;
import com.airline.reservation.service.FlightService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}")
    public FlightResponse getFlightById(@PathVariable String id) {
        var f = flightService.getFlightById(id);
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
}
