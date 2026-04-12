package com.airline.reservation.factory;

import com.airline.reservation.entity.Booking;
import com.airline.reservation.entity.BookingStatus;
import com.airline.reservation.entity.Flight;
import com.airline.reservation.entity.PaymentStatus;
import com.airline.reservation.entity.Seat;
import com.airline.reservation.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class DefaultBookingFactory implements BookingFactory {

    @Override
    public Booking create(User user, Flight flight, List<Seat> seats) {
        Booking booking = new Booking();
        booking.setBookingReference("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setUser(user);
        booking.setFlight(flight);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setBookedAt(LocalDateTime.now());
        booking.setTotalAmount(seats.stream().map(Seat::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add));
        return booking;
    }
}
