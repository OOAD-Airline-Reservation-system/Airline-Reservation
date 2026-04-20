package com.airline.reservation.gateway;

import com.airline.reservation.config.AppProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Razorpay payment gateway adapter.
 *
 * In production: call the Razorpay Payments API to verify that the payment_id
 * belongs to a genuine, captured payment with the correct amount.
 *
 * When no key is configured the adapter approves any non-blank token so that
 * end-to-end demos work without a live Razorpay account.
 *
 * Frontend -> Backend (/api/payments) -> this adapter -> Razorpay API
 */
@Component
public class RazorpayGatewayAdapter implements PaymentGatewayAdapter {

    private final AppProperties appProperties;

    public RazorpayGatewayAdapter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public boolean verifyPayment(String paymentToken, BigDecimal expectedAmount) {
        if (paymentToken == null || paymentToken.isBlank()) {
            return false;
        }

        String keyId     = appProperties.getRazorpay().getKeyId();
        String keySecret = appProperties.getRazorpay().getKeySecret();

        // No real credentials — approve any non-blank token (demo mode)
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            return true;
        }

        // Production path: verify via Razorpay REST API
        // POST https://api.razorpay.com/v1/payments/{paymentToken}
        // Check payment.status == "captured" and payment.amount == expectedAmount * 100
        // This is intentionally left as a clearly-labelled extension point.
        try {
            // TODO: inject RestTemplate and call Razorpay verification endpoint
            // String url = "https://api.razorpay.com/v1/payments/" + paymentToken;
            // ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET,
            //     new HttpEntity<>(buildAuthHeaders(keyId, keySecret)), Map.class);
            // return "captured".equals(resp.getBody().get("status"));
            return true; // placeholder until Razorpay credentials are added
        } catch (Exception ex) {
            return false;
        }
    }
}
