package com.airline.reservation.repository;

import com.airline.reservation.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Long> {
    List<Flight> findBySourceIgnoreCaseAndDestinationIgnoreCaseAndDepartureTimeBetween(
            String source,
            String destination,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay
    );

    Optional<Flight> findByFlightNumber(String flightNumber);
}
