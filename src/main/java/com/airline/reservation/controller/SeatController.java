package com.airline.reservation.controller;

import com.airline.reservation.dto.seat.SeatResponse;
import com.airline.reservation.service.SeatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping("/flight/{flightId}")
    public List<SeatResponse> getSeats(@PathVariable Long flightId) {
        return seatService.getSeatsByFlight(flightId);
    }
}
