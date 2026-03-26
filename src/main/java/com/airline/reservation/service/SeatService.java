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

    public List<SeatResponse> getSeatsByFlight(Long flightId) {
        return seatRepository.findByFlightId(flightId).stream().map(seat -> {
            SeatResponse response = new SeatResponse();
            response.setId(seat.getId());
            response.setSeatNumber(seat.getSeatNumber());
            response.setSeatClass(seat.getSeatClass());
            response.setStatus(seat.getStatus());
            response.setPrice(seat.getPrice());
            return response;
        }).toList();
    }
}
