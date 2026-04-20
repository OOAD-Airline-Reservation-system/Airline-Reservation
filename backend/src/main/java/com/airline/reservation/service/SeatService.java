package com.airline.reservation.service;

import com.airline.reservation.dto.seat.SeatResponse;
import com.airline.reservation.repository.SeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;

    public SeatService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    public List<SeatResponse> getSeatsByFlight(String flightId) {
        return seatRepository.findByFlightId(flightId).stream().map(seat -> {
            SeatResponse r = new SeatResponse();
            r.setId(seat.getId());
            r.setSeatNumber(seat.getSeatNumber());
            r.setSeatClass(seat.getSeatClass());
            r.setStatus(seat.getStatus());
            r.setPrice(seat.getPrice());
            return r;
        }).toList();
    }
}
