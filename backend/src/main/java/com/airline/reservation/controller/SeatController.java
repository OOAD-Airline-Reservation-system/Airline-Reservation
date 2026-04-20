package com.airline.reservation.controller;

import com.airline.reservation.dto.seat.SeatResponse;
import com.airline.reservation.service.SeatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping("/flight/{flightId}")
    public List<SeatResponse> getSeats(@PathVariable String flightId) {
        return seatService.getSeatsByFlight(flightId);
    }
}
