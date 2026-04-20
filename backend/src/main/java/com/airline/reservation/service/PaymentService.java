package com.airline.reservation.service;

import com.airline.reservation.dto.payment.PaymentRequest;
import com.airline.reservation.dto.payment.PaymentResponse;
import com.airline.reservation.entity.*;
import com.airline.reservation.exception.BadRequestException;
import com.airline.reservation.factory.PaymentFactory;
import com.airline.reservation.gateway.PaymentGatewayAdapter;
import com.airline.reservation.repository.BookingRepository;
import com.airline.reservation.repository.PaymentRepository;
import com.airline.reservation.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates payment verification, booking/seat status updates, and loyalty point awards.
 * SRP: payment flow coordination only.
 * DIP: depends on gateway and repository abstractions.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final PaymentFactory paymentFactory;
    private final PaymentGatewayAdapter paymentGatewayAdapter;
    private final LoyaltyService loyaltyService;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingService bookingService,
                          BookingRepository bookingRepository,
                          SeatRepository seatRepository,
                          PaymentFactory paymentFactory,
                          PaymentGatewayAdapter paymentGatewayAdapter,
                          LoyaltyService loyaltyService) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.paymentFactory = paymentFactory;
        this.paymentGatewayAdapter = paymentGatewayAdapter;
        this.loyaltyService = loyaltyService;
    }

    public PaymentResponse processPayment(String userEmail, PaymentRequest request) {
        Booking booking = bookingService.getBookingEntity(request.getBookingId());
        bookingService.validateBookingOwnership(booking, userEmail);

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                || booking.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Booking is not eligible for payment");
        }

        List<Seat> seats = seatRepository.findByFlightIdAndIds(booking.getFlightId(), booking.getSeatIds());
        if (seats.stream().anyMatch(s -> s.getStatus() != SeatStatus.LOCKED)) {
            throw new BadRequestException("Booking seat lock has expired or is invalid");
        }

        boolean paymentSucceeded = paymentGatewayAdapter.verifyPayment(
                request.getPaymentToken(), booking.getTotalAmount());

        Payment payment = paymentFactory.create(booking, request.getProvider(), paymentSucceeded);

        if (paymentSucceeded) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentStatus(PaymentStatus.SUCCESS);
            for (Seat seat : seats) seat.setStatus(SeatStatus.BOOKED);

            try {
                loyaltyService.earnPointsForBooking(
                        userEmail, booking.getTotalAmount(), booking.getBookingReference());
            } catch (Exception e) {
                log.warn("Failed to award loyalty points for booking {}: {}",
                        booking.getBookingReference(), e.getMessage());
            }
        } else {
            booking.setPaymentStatus(PaymentStatus.FAILED);
            booking.setStatus(BookingStatus.CANCELLED);
            for (Seat seat : seats) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setBookingId(null);
            }
        }

        seatRepository.saveAll(seats);
        Payment savedPayment = paymentRepository.save(payment);
        booking.setPaymentId(savedPayment.getId());
        bookingRepository.save(booking);

        return mapToResponse(savedPayment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse r = new PaymentResponse();
        r.setId(payment.getId());
        r.setBookingId(payment.getBookingId());
        r.setStatus(payment.getStatus());
        r.setProvider(payment.getProvider());
        r.setTransactionId(payment.getTransactionId());
        r.setAmount(payment.getAmount());
        r.setProcessedAt(payment.getProcessedAt());
        return r;
    }
}
