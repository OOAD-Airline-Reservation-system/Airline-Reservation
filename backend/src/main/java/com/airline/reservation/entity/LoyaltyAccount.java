package com.airline.reservation.entity;

import java.time.LocalDateTime;

/**
 * Loyalty program account — one per user, stored in Firestore collection "loyaltyAccounts".
 * Document ID = userId (user's email).
 *
 * Tier logic (driven by lifetimePoints):
 *   BRONZE  :    0 – 4,999
 *   SILVER  : 5,000 – 19,999
 *   GOLD    : 20,000 – 49,999
 *   PLATINUM: 50,000+
 */
public class LoyaltyAccount {

    public enum Tier { BRONZE, SILVER, GOLD, PLATINUM }

    private String userId;
    private int pointsBalance = 0;
    private int lifetimePoints = 0;  // never decreases; drives tier
    private Tier tier = Tier.BRONZE;
    private String membershipNumber;
    private LocalDateTime enrolledAt;
    private LocalDateTime lastActivityAt;

    // ---- business logic (Information Expert — GRASP) ----

    public void addPoints(int points) {
        this.pointsBalance += points;
        this.lifetimePoints += points;
        this.lastActivityAt = LocalDateTime.now();
        recalculateTier();
    }

    public boolean redeemPoints(int points) {
        if (this.pointsBalance < points) return false;
        this.pointsBalance -= points;
        this.lastActivityAt = LocalDateTime.now();
        return true;
    }

    private void recalculateTier() {
        if (lifetimePoints >= 50_000)      tier = Tier.PLATINUM;
        else if (lifetimePoints >= 20_000) tier = Tier.GOLD;
        else if (lifetimePoints >= 5_000)  tier = Tier.SILVER;
        else                               tier = Tier.BRONZE;
    }

    // ---- getters / setters ----

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getPointsBalance() { return pointsBalance; }
    public void setPointsBalance(int pointsBalance) { this.pointsBalance = pointsBalance; }

    public int getLifetimePoints() { return lifetimePoints; }
    public void setLifetimePoints(int lifetimePoints) { this.lifetimePoints = lifetimePoints; }

    public Tier getTier() { return tier; }
    public void setTier(Tier tier) { this.tier = tier; }

    public String getMembershipNumber() { return membershipNumber; }
    public void setMembershipNumber(String membershipNumber) { this.membershipNumber = membershipNumber; }

    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }

    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
}
