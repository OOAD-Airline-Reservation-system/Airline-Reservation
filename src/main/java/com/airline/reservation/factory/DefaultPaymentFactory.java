package com.airline.reservation.factory;

import com.airline.reservation.entity.Booking;
import com.airline.reservation.entity.Payment;
import com.airline.reservation.entity.PaymentStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DefaultPaymentFactory implements PaymentFactory {

    @Override
    public Payment create(Booking booking, String provider, boolean paymentSucceeded) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setProvider(provider);
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setProcessedAt(LocalDateTime.now());
        payment.setStatus(paymentSucceeded ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        return payment;
    }
}
