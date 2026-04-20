package com.airline.reservation.repository;

import com.airline.reservation.entity.Payment;
import com.airline.reservation.entity.PaymentStatus;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Firestore-backed repository for Payment.
 * Collection: "payments" — document ID = UUID.
 * Pure Fabrication (GRASP): no domain logic, only persistence.
 */
@Repository
public class PaymentRepository {

    private static final String COLLECTION = "payments";
    private final Firestore firestore;

    public PaymentRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Payment save(Payment payment) {
        try {
            if (payment.getId() == null) payment.setId(UUID.randomUUID().toString());
            firestore.collection(COLLECTION).document(payment.getId()).set(toMap(payment)).get();
            return payment;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore write failed for payment: " + payment.getId(), e);
        }
    }

    public Optional<Payment> findByTransactionId(String transactionId) {
        try {
            return firestore.collection(COLLECTION)
                    .whereEqualTo("transactionId", transactionId)
                    .get().get().getDocuments()
                    .stream().map(this::fromDoc).findFirst();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore query failed for transactionId: " + transactionId, e);
        }
    }

    private Map<String, Object> toMap(Payment p) {
        Map<String, Object> m = new HashMap<>();
        m.put("bookingId", p.getBookingId());
        m.put("amount", p.getAmount().toPlainString());
        m.put("provider", p.getProvider());
        m.put("transactionId", p.getTransactionId());
        m.put("status", p.getStatus().name());
        m.put("processedAt", p.getProcessedAt().toString());
        return m;
    }

    private Payment fromDoc(DocumentSnapshot doc) {
        Payment p = new Payment();
        p.setId(doc.getId());
        p.setBookingId(doc.getString("bookingId"));
        p.setAmount(new BigDecimal(doc.getString("amount")));
        p.setProvider(doc.getString("provider"));
        p.setTransactionId(doc.getString("transactionId"));
        p.setStatus(PaymentStatus.valueOf(doc.getString("status")));
        p.setProcessedAt(LocalDateTime.parse(doc.getString("processedAt")));
        return p;
    }
}
