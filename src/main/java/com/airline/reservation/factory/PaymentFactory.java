package com.airline.reservation.factory;

import com.airline.reservation.entity.Booking;
import com.airline.reservation.entity.Payment;

public interface PaymentFactory {

    Payment create(Booking booking, String provider, boolean paymentSucceeded);
}
