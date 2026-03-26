package com.airline.reservation.repository;

import com.airline.reservation.entity.Seat;
import com.airline.reservation.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByFlightId(Long flightId);

    List<Seat> findByFlightIdAndStatus(Long flightId, SeatStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.flight.id = :flightId and s.id in :seatIds")
    List<Seat> findSeatsForUpdate(@Param("flightId") Long flightId, @Param("seatIds") List<Long> seatIds);
}
