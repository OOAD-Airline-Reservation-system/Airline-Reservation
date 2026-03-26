package com.airline.reservation.service;

import com.airline.reservation.dto.payment.PaymentRequest;
import com.airline.reservation.dto.payment.PaymentResponse;
import com.airline.reservation.entity.Booking;
import com.airline.reservation.entity.BookingStatus;
import com.airline.reservation.entity.Payment;
import com.airline.reservation.entity.PaymentStatus;
import com.airline.reservation.entity.Seat;
import com.airline.reservation.entity.SeatStatus;
import com.airline.reservation.exception.BadRequestException;
import com.airline.reservation.repository.BookingRepository;
import com.airline.reservation.repository.PaymentRepository;
import com.airline.reservation.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingService bookingService,
            BookingRepository bookingRepository,
            SeatRepository seatRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public PaymentResponse processPayment(String userEmail, PaymentRequest request) {
        Booking booking = bookingService.getBookingEntity(request.getBookingId());
        bookingService.validateBookingOwnership(booking, userEmail);

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT || booking.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Booking is not eligible for payment");
        }

        boolean invalidLockState = booking.getSeats().stream().anyMatch(seat -> seat.getStatus() != SeatStatus.LOCKED);
        if (invalidLockState) {
            throw new BadRequestException("Booking seat lock has expired or is invalid");
        }

        boolean paymentSucceeded = !request.getPaymentToken().isBlank();

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setProvider(request.getProvider());
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setProcessedAt(LocalDateTime.now());
        payment.setStatus(paymentSucceeded ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

        if (paymentSucceeded) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentStatus(PaymentStatus.SUCCESS);
            for (Seat seat : booking.getSeats()) {
                seat.setStatus(SeatStatus.BOOKED);
            }
            seatRepository.saveAll(booking.getSeats());
        } else {
            booking.setPaymentStatus(PaymentStatus.FAILED);
            booking.setStatus(BookingStatus.CANCELLED);
            for (Seat seat : booking.getSeats()) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setBooking(null);
            }
            seatRepository.saveAll(booking.getSeats());
        }

        booking.setPayment(payment);
        bookingRepository.save(booking);
        Payment savedPayment = paymentRepository.save(payment);
        return mapToResponse(savedPayment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setBookingId(payment.getBooking().getId());
        response.setStatus(payment.getStatus());
        response.setProvider(payment.getProvider());
        response.setTransactionId(payment.getTransactionId());
        response.setAmount(payment.getAmount());
        response.setProcessedAt(payment.getProcessedAt());
        return response;
    }
}
