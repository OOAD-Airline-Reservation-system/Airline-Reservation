package com.airline.reservation.dto.payment;

import jakarta.validation.constraints.NotBlank;

public class PaymentRequest {

    @NotBlank
    private String bookingId;

    @NotBlank
    private String provider;

    @NotBlank
    private String paymentToken;

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getPaymentToken() { return paymentToken; }
    public void setPaymentToken(String paymentToken) { this.paymentToken = paymentToken; }
}
