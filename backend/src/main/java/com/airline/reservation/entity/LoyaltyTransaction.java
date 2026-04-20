package com.airline.reservation.entity;

import java.time.LocalDateTime;

/**
 * Loyalty transaction — stored in Firestore sub-collection
 * "loyaltyAccounts/{userId}/transactions/{txId}".
 */
public class LoyaltyTransaction {

    public enum TxType { EARN, REDEEM, BONUS, ADJUSTMENT }

    private String id;
    private String loyaltyAccountId;  // parent userId
    private TxType type;
    private int points;
    private String description;
    private String bookingReference;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLoyaltyAccountId() { return loyaltyAccountId; }
    public void setLoyaltyAccountId(String loyaltyAccountId) { this.loyaltyAccountId = loyaltyAccountId; }

    public TxType getType() { return type; }
    public void setType(TxType type) { this.type = type; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
