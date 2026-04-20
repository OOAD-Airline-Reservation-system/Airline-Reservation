package com.airline.reservation.factory;

import com.airline.reservation.entity.Booking;
import com.airline.reservation.entity.Flight;
import com.airline.reservation.entity.Seat;

import java.util.List;

public interface BookingFactory {
    Booking create(String userEmail, Flight flight, List<Seat> seats);
}
