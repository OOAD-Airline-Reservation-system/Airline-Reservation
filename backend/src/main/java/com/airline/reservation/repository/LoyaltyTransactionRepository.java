package com.airline.reservation.repository;

import com.airline.reservation.entity.LoyaltyTransaction;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore-backed repository for LoyaltyTransaction.
 * Sub-collection: "loyaltyAccounts/{userId}/transactions".
 * Pure Fabrication (GRASP): no domain logic, only persistence concern.
 */
@Repository
public class LoyaltyTransactionRepository {

    private static final String ACCOUNTS = "loyaltyAccounts";
    private static final String TRANSACTIONS = "transactions";

    private final Firestore firestore;

    public LoyaltyTransactionRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public LoyaltyTransaction save(LoyaltyTransaction tx) {
        try {
            if (tx.getId() == null) tx.setId(UUID.randomUUID().toString());
            firestore.collection(ACCOUNTS)
                    .document(tx.getLoyaltyAccountId())
                    .collection(TRANSACTIONS)
                    .document(tx.getId())
                    .set(toMap(tx)).get();
            return tx;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore write failed for transaction " + tx.getId(), e);
        }
    }

    public List<LoyaltyTransaction> findByLoyaltyAccountIdOrderByCreatedAtDesc(String userId) {
        try {
            return firestore.collection(ACCOUNTS)
                    .document(userId)
                    .collection(TRANSACTIONS)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().get()
                    .getDocuments()
                    .stream()
                    .map(this::fromDoc)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore read failed for transactions of " + userId, e);
        }
    }

    private Map<String, Object> toMap(LoyaltyTransaction tx) {
        Map<String, Object> m = new HashMap<>();
        m.put("loyaltyAccountId", tx.getLoyaltyAccountId());
        m.put("type", tx.getType().name());
        m.put("points", tx.getPoints());
        m.put("description", tx.getDescription());
        m.put("bookingReference", tx.getBookingReference());
        m.put("createdAt", tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : null);
        return m;
    }

    private LoyaltyTransaction fromDoc(QueryDocumentSnapshot doc) {
        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setId(doc.getId());
        tx.setLoyaltyAccountId(doc.getString("loyaltyAccountId"));
        tx.setType(LoyaltyTransaction.TxType.valueOf(doc.getString("type")));
        tx.setPoints(doc.getLong("points").intValue());
        tx.setDescription(doc.getString("description"));
        tx.setBookingReference(doc.getString("bookingReference"));
        String createdAt = doc.getString("createdAt");
        if (createdAt != null) tx.setCreatedAt(LocalDateTime.parse(createdAt));
        return tx;
    }
}
