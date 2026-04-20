package com.airline.reservation.factory;

import com.airline.reservation.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * GRASP Creator: responsible for creating Booking instances.
 * OCP: new booking types can be added by implementing BookingFactory.
 */
@Component
public class DefaultBookingFactory implements BookingFactory {

    @Override
    public Booking create(String userEmail, Flight flight, List<Seat> seats) {
        Booking booking = new Booking();
        booking.setBookingReference("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setUserEmail(userEmail);
        booking.setFlightId(flight.getId());
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setBookedAt(LocalDateTime.now());
        booking.setTotalAmount(seats.stream().map(Seat::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add));
        booking.setSeatIds(seats.stream().map(Seat::getId).toList());
        return booking;
    }
}
