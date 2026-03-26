package com.airline.reservation.config;

import com.airline.reservation.entity.Flight;
import com.airline.reservation.entity.Seat;
import com.airline.reservation.entity.SeatClass;
import com.airline.reservation.repository.FlightRepository;
import com.airline.reservation.repository.SeatRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(FlightRepository flightRepository, SeatRepository seatRepository) {
        return args -> {
            if (flightRepository.count() > 0) {
                return;
            }

            Flight flight1 = new Flight();
            flight1.setFlightNumber("AR101");
            flight1.setSource("Delhi");
            flight1.setDestination("Mumbai");
            flight1.setDepartureTime(LocalDate.now().plusDays(1).atTime(9, 0));
            flight1.setArrivalTime(LocalDate.now().plusDays(1).atTime(11, 10));
            flight1.setBasePrice(new BigDecimal("5500.00"));

            Flight flight2 = new Flight();
            flight2.setFlightNumber("AR202");
            flight2.setSource("Delhi");
            flight2.setDestination("Bengaluru");
            flight2.setDepartureTime(LocalDate.now().plusDays(1).atTime(14, 15));
            flight2.setArrivalTime(LocalDate.now().plusDays(1).atTime(17, 0));
            flight2.setBasePrice(new BigDecimal("6800.00"));

            flightRepository.saveAll(List.of(flight1, flight2));
            seatRepository.saveAll(buildSeats(flight1));
            seatRepository.saveAll(buildSeats(flight2));
        };
    }

    private List<Seat> buildSeats(Flight flight) {
        List<Seat> seats = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            seats.add(createSeat(flight, row + "A", SeatClass.BUSINESS, new BigDecimal("9500.00")));
            seats.add(createSeat(flight, row + "B", SeatClass.BUSINESS, new BigDecimal("9500.00")));
        }
        for (int row = 10; row <= 14; row++) {
            seats.add(createSeat(flight, row + "A", SeatClass.ECONOMY, new BigDecimal("5500.00")));
            seats.add(createSeat(flight, row + "B", SeatClass.ECONOMY, new BigDecimal("5500.00")));
            seats.add(createSeat(flight, row + "C", SeatClass.ECONOMY, new BigDecimal("5500.00")));
        }
        return seats;
    }

    private Seat createSeat(Flight flight, String seatNumber, SeatClass seatClass, BigDecimal price) {
        Seat seat = new Seat();
        seat.setFlight(flight);
        seat.setSeatNumber(seatNumber);
        seat.setSeatClass(seatClass);
        seat.setPrice(price);
        return seat;
    }
}
