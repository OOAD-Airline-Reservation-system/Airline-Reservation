package com.airline.reservation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment — stored in Firestore collection "payments", document ID = auto-generated UUID.
 * References bookingId as a string foreign key.
 */
public class Payment {

    private String id;
    private String bookingId;   // replaces @OneToOne Booking
    private BigDecimal amount;
    private String provider;
    private String transactionId;
    private PaymentStatus status;
    private LocalDateTime processedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
