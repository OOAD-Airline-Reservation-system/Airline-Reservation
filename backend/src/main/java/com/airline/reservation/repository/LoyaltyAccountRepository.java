package com.airline.reservation.repository;

import com.airline.reservation.entity.LoyaltyAccount;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore-backed repository for LoyaltyAccount.
 * Collection: "loyaltyAccounts" — document ID = userId (email).
 * Pure Fabrication (GRASP): no domain logic, only persistence concern.
 */
@Repository
public class LoyaltyAccountRepository {

    private static final String COLLECTION = "loyaltyAccounts";

    private final Firestore firestore;

    public LoyaltyAccountRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Optional<LoyaltyAccount> findByUserId(String userId) {
        try {
            DocumentSnapshot doc = firestore.collection(COLLECTION).document(userId).get().get();
            if (!doc.exists()) return Optional.empty();
            return Optional.of(fromDoc(doc));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore read failed for loyaltyAccounts/" + userId, e);
        }
    }

    public LoyaltyAccount save(LoyaltyAccount account) {
        try {
            firestore.collection(COLLECTION).document(account.getUserId()).set(toMap(account)).get();
            return account;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore write failed for loyaltyAccounts/" + account.getUserId(), e);
        }
    }

    private Map<String, Object> toMap(LoyaltyAccount a) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", a.getUserId());
        m.put("pointsBalance", a.getPointsBalance());
        m.put("lifetimePoints", a.getLifetimePoints());
        m.put("tier", a.getTier().name());
        m.put("membershipNumber", a.getMembershipNumber());
        m.put("enrolledAt", a.getEnrolledAt() != null ? a.getEnrolledAt().toString() : null);
        m.put("lastActivityAt", a.getLastActivityAt() != null ? a.getLastActivityAt().toString() : null);
        return m;
    }

    private LoyaltyAccount fromDoc(DocumentSnapshot doc) {
        LoyaltyAccount a = new LoyaltyAccount();
        a.setUserId(doc.getId());
        a.setPointsBalance(doc.getLong("pointsBalance").intValue());
        a.setLifetimePoints(doc.getLong("lifetimePoints").intValue());
        a.setTier(LoyaltyAccount.Tier.valueOf(doc.getString("tier")));
        a.setMembershipNumber(doc.getString("membershipNumber"));
        String enrolledAt = doc.getString("enrolledAt");
        if (enrolledAt != null) a.setEnrolledAt(LocalDateTime.parse(enrolledAt));
        String lastActivity = doc.getString("lastActivityAt");
        if (lastActivity != null) a.setLastActivityAt(LocalDateTime.parse(lastActivity));
        return a;
    }
}
