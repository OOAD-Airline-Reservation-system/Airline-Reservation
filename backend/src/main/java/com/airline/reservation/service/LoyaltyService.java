package com.airline.reservation.service;

import com.airline.reservation.entity.LoyaltyAccount;
import com.airline.reservation.entity.LoyaltyTransaction;
import com.airline.reservation.exception.BadRequestException;
import com.airline.reservation.repository.LoyaltyAccountRepository;
import com.airline.reservation.repository.LoyaltyTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages loyalty points lifecycle: enroll, earn, redeem, summarise.
 * SRP: only loyalty business logic.
 * DIP: depends on repository abstractions, not Firestore directly.
 * GRASP Creator: creates LoyaltyAccount and LoyaltyTransaction instances.
 */
@Service
public class LoyaltyService {

    private static final double EARN_RATE = 0.1;                   // 1 pt per ₹10
    private static final double REDEEM_RATE_INR_PER_POINT = 0.1;  // 10 pts = ₹1

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;

    public LoyaltyService(LoyaltyAccountRepository loyaltyAccountRepository,
                          LoyaltyTransactionRepository loyaltyTransactionRepository) {
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.loyaltyTransactionRepository = loyaltyTransactionRepository;
    }

    /** Auto-enroll on registration. userId = user's email (JWT subject). */
    public LoyaltyAccount enroll(String userId) {
        return loyaltyAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    LoyaltyAccount account = new LoyaltyAccount();
                    account.setUserId(userId);
                    account.setMembershipNumber("ARS" + System.currentTimeMillis() % 1_000_000_000L);
                    account.setEnrolledAt(LocalDateTime.now());
                    account.setLastActivityAt(LocalDateTime.now());
                    return loyaltyAccountRepository.save(account);
                });
    }

    /** Earn points when a booking is paid. */
    public void earnPointsForBooking(String userId, BigDecimal amount, String bookingRef) {
        int points = (int) (amount.doubleValue() * EARN_RATE);
        if (points <= 0) return;

        LoyaltyAccount account = getOrCreate(userId);
        account.addPoints(points);
        loyaltyAccountRepository.save(account);
        loyaltyTransactionRepository.save(buildTransaction(userId,
                LoyaltyTransaction.TxType.EARN, points,
                "Points earned for booking " + bookingRef, bookingRef));
    }

    /** Redeem points — returns INR discount amount. */
    public BigDecimal redeemPoints(String userId, int pointsToRedeem, String bookingRef) {
        if (pointsToRedeem <= 0) throw new BadRequestException("Points to redeem must be positive");

        LoyaltyAccount account = getOrCreate(userId);
        if (!account.redeemPoints(pointsToRedeem)) {
            throw new BadRequestException("Insufficient loyalty points. Balance: " + account.getPointsBalance());
        }
        loyaltyAccountRepository.save(account);
        loyaltyTransactionRepository.save(buildTransaction(userId,
                LoyaltyTransaction.TxType.REDEEM, -pointsToRedeem,
                "Points redeemed for booking " + bookingRef, bookingRef));

        return BigDecimal.valueOf(pointsToRedeem * REDEEM_RATE_INR_PER_POINT);
    }

    public Map<String, Object> getAccountSummary(String userId) {
        LoyaltyAccount account = getOrCreate(userId);
        List<LoyaltyTransaction> txs =
                loyaltyTransactionRepository.findByLoyaltyAccountIdOrderByCreatedAtDesc(userId);

        int nextTierPoints = nextTierThreshold(account) - account.getLifetimePoints();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("membershipNumber", account.getMembershipNumber());
        summary.put("tier", account.getTier().name());
        summary.put("pointsBalance", account.getPointsBalance());
        summary.put("lifetimePoints", account.getLifetimePoints());
        summary.put("pointsToNextTier", Math.max(0, nextTierPoints));
        summary.put("redeemableValueINR", account.getPointsBalance() * REDEEM_RATE_INR_PER_POINT);
        summary.put("enrolledAt", account.getEnrolledAt());
        summary.put("transactions", txs.stream().limit(20).map(this::txToMap).toList());
        return summary;
    }

    // ---- helpers ----

    private LoyaltyAccount getOrCreate(String userId) {
        return loyaltyAccountRepository.findByUserId(userId).orElseGet(() -> enroll(userId));
    }

    private LoyaltyTransaction buildTransaction(String userId, LoyaltyTransaction.TxType type,
                                                 int points, String description, String bookingRef) {
        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setLoyaltyAccountId(userId);
        tx.setType(type);
        tx.setPoints(points);
        tx.setDescription(description);
        tx.setBookingReference(bookingRef);
        tx.setCreatedAt(LocalDateTime.now());
        return tx;
    }

    private int nextTierThreshold(LoyaltyAccount account) {
        return switch (account.getTier()) {
            case BRONZE -> 5_000;
            case SILVER -> 20_000;
            case GOLD -> 50_000;
            case PLATINUM -> Integer.MAX_VALUE;
        };
    }

    private Map<String, Object> txToMap(LoyaltyTransaction tx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", tx.getId());
        m.put("type", tx.getType());
        m.put("points", tx.getPoints());
        m.put("description", tx.getDescription());
        m.put("bookingReference", tx.getBookingReference());
        m.put("createdAt", tx.getCreatedAt());
        return m;
    }
}
