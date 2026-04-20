package com.airline.reservation.entity;

import java.math.BigDecimal;

/**
 * Seat — stored in Firestore collection "seats", document ID = auto-generated UUID.
 * References flightId and bookingId as string foreign keys.
 */
public class Seat {

    private String id;
    private String seatNumber;
    private SeatClass seatClass;
    private SeatStatus status = SeatStatus.AVAILABLE;
    private BigDecimal price;
    private String flightId;
    private String bookingId;  // null when not booked/locked

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public SeatClass getSeatClass() { return seatClass; }
    public void setSeatClass(SeatClass seatClass) { this.seatClass = seatClass; }

    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
}
