package com.airline.reservation.gateway;

/**
 * Adapter interface for payment gateway providers (Razorpay, Stripe, etc.).
 * The backend calls this; the frontend only sends a payment token it received
 * from the gateway SDK. This backend then verifies it server-side.
 */
public interface PaymentGatewayAdapter {
    /**
     * Verify a payment token returned by the frontend after user completes payment.
     * Returns true if the payment is genuine and the amount matches.
     */
    boolean verifyPayment(String paymentToken, java.math.BigDecimal expectedAmount);
}
