package com.airline.reservation.repository;

import com.airline.reservation.entity.Booking;
import com.airline.reservation.entity.BookingStatus;
import com.airline.reservation.entity.BookingStep;
import com.airline.reservation.entity.PaymentStatus;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore-backed repository for Booking.
 * Collection: "bookings" — document ID = UUID.
 * Pure Fabrication (GRASP): no domain logic, only persistence.
 */
@Repository
public class BookingRepository {

    private static final String COLLECTION = "bookings";
    private final Firestore firestore;

    public BookingRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Optional<Booking> findById(String id) {
        try {
            DocumentSnapshot doc = firestore.collection(COLLECTION).document(id).get().get();
            if (!doc.exists()) return Optional.empty();
            return Optional.of(fromDoc(doc));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore read failed for booking: " + id, e);
        }
    }

    public Optional<Booking> findByBookingReference(String ref) {
        try {
            return firestore.collection(COLLECTION)
                    .whereEqualTo("bookingReference", ref)
                    .get().get().getDocuments()
                    .stream().map(this::fromDoc).findFirst();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore query failed for bookingReference: " + ref, e);
        }
    }

    public List<Booking> findByUserEmail(String userEmail) {
        try {
            return firestore.collection(COLLECTION)
                    .whereEqualTo("userEmail", userEmail)
                    .get().get().getDocuments()
                    .stream().map(this::fromDoc).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore query failed for bookings of user: " + userEmail, e);
        }
    }

    public Booking save(Booking booking) {
        try {
            if (booking.getId() == null) booking.setId(UUID.randomUUID().toString());
            firestore.collection(COLLECTION).document(booking.getId()).set(toMap(booking)).get();
            return booking;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore write failed for booking: " + booking.getId(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Booking fromDoc(DocumentSnapshot doc) {
        Booking b = new Booking();
        b.setId(doc.getId());
        b.setBookingReference(doc.getString("bookingReference"));
        b.setUserEmail(doc.getString("userEmail"));
        b.setFlightId(doc.getString("flightId"));
        b.setStatus(BookingStatus.valueOf(doc.getString("status")));
        b.setPaymentStatus(PaymentStatus.valueOf(doc.getString("paymentStatus")));
        b.setTotalAmount(new BigDecimal(doc.getString("totalAmount")));
        b.setBookedAt(LocalDateTime.parse(doc.getString("bookedAt")));
        Object seatIds = doc.get("seatIds");
        if (seatIds instanceof List<?> list) {
            b.setSeatIds(list.stream().map(Object::toString).collect(Collectors.toList()));
        }
        b.setPaymentId(doc.getString("paymentId"));
        String step = doc.getString("bookingStep");
        if (step != null) b.setBookingStep(BookingStep.valueOf(step));
        return b;
    }

    private Booking fromDoc(QueryDocumentSnapshot doc) {
        return fromDoc((DocumentSnapshot) doc);
    }

    private Map<String, Object> toMap(Booking b) {
        Map<String, Object> m = new HashMap<>();
        m.put("bookingReference", b.getBookingReference());
        m.put("userEmail", b.getUserEmail());
        m.put("flightId", b.getFlightId());
        m.put("status", b.getStatus().name());
        m.put("paymentStatus", b.getPaymentStatus().name());
        m.put("totalAmount", b.getTotalAmount().toPlainString());
        m.put("bookedAt", b.getBookedAt().toString());
        m.put("seatIds", b.getSeatIds() != null ? b.getSeatIds() : new ArrayList<>());
        m.put("paymentId", b.getPaymentId());
        m.put("bookingStep", b.getBookingStep() != null ? b.getBookingStep().name() : null);
        return m;
    }
}
