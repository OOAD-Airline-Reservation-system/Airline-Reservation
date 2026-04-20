package com.airline.reservation.dto.booking;

import com.airline.reservation.entity.BookingStatus;
import com.airline.reservation.entity.BookingStep;
import com.airline.reservation.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BookingResponse {

    private String id;
    private String bookingReference;
    private String flightId;
    private String flightNumber;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
    private LocalDateTime bookedAt;
    private List<String> seats;
    private BookingStep bookingStep;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }

    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }

    public List<String> getSeats() { return seats; }
    public void setSeats(List<String> seats) { this.seats = seats; }

    public BookingStep getBookingStep() { return bookingStep; }
    public void setBookingStep(BookingStep bookingStep) { this.bookingStep = bookingStep; }
}
