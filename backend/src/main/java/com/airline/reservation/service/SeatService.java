package com.airline.reservation.service;

import com.airline.reservation.dto.seat.SeatResponse;
import com.airline.reservation.entity.Seat;
import com.airline.reservation.entity.SeatClass;
import com.airline.reservation.exception.ResourceNotFoundException;
import com.airline.reservation.repository.FlightRepository;
import com.airline.reservation.repository.SeatRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class SeatService {

    // 4 business rows x 4 cols + 16 economy rows x 6 cols
    private static final int EXPECTED_SEAT_COUNT = (4 * 4) + (16 * 6);

    private final SeatRepository seatRepository;
    private final FlightRepository flightRepository;

    public SeatService(SeatRepository seatRepository, FlightRepository flightRepository) {
        this.seatRepository = seatRepository;
        this.flightRepository = flightRepository;
    }

    public List<SeatResponse> getSeatsByFlight(String flightId) {
        List<Seat> seats = seatRepository.findByFlightId(flightId);

        // Rebuild if seats are missing or from old layout (fewer columns)
        if (seats.size() < EXPECTED_SEAT_COUNT) {
            var flight = flightRepository.findById(flightId)
                    .orElseThrow(() -> new ResourceNotFoundException("Flight not found: " + flightId));
            List<Seat> newSeats = buildSeats(flightId, flight.getBasePrice());
            // Only add seats that don't already exist
            var existingNums = seats.stream().map(Seat::getSeatNumber).collect(java.util.stream.Collectors.toSet());
            newSeats.stream()
                    .filter(s -> !existingNums.contains(s.getSeatNumber()))
                    .forEach(seatRepository::save);
            seats = seatRepository.findByFlightId(flightId);
        }

        return seats.stream().map(seat -> {
            SeatResponse r = new SeatResponse();
            r.setId(seat.getId());
            r.setSeatNumber(seat.getSeatNumber());
            r.setSeatClass(seat.getSeatClass());
            r.setStatus(seat.getStatus());
            r.setPrice(seat.getPrice());
            return r;
        }).toList();
    }

    private List<Seat> buildSeats(String flightId, BigDecimal basePrice) {
        List<Seat> seats = new ArrayList<>();
        String[] busCols = {"A", "B", "C", "D"};
        for (int row = 1; row <= 4; row++) {
            for (String col : busCols) {
                seats.add(seat(flightId, row + col, SeatClass.BUSINESS, new BigDecimal("9500.00")));
            }
        }
        String[] ecoCols = {"A", "B", "C", "D", "E", "F"};
        for (int row = 10; row <= 25; row++) {
            for (String col : ecoCols) {
                seats.add(seat(flightId, row + col, SeatClass.ECONOMY, basePrice));
            }
        }
        return seats;
    }

    private Seat seat(String flightId, String number, SeatClass cls, BigDecimal price) {
        Seat s = new Seat();
        s.setFlightId(flightId);
        s.setSeatNumber(number);
        s.setSeatClass(cls);
        s.setPrice(price);
        return s;
    }
}
