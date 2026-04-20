package com.airline.reservation.repository;

import com.airline.reservation.entity.Seat;
import com.airline.reservation.entity.SeatClass;
import com.airline.reservation.entity.SeatStatus;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore-backed repository for Seat.
 * Collection: "seats" — document ID = UUID.
 * Pure Fabrication (GRASP): no domain logic, only persistence.
 *
 * Note: Firestore does not support pessimistic locking. Concurrent seat
 * conflicts are handled via Firestore transactions in BookingService.
 */
@Repository
public class SeatRepository {

    private static final String COLLECTION = "seats";
    private final Firestore firestore;

    public SeatRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Optional<Seat> findById(String id) {
        try {
            DocumentSnapshot doc = firestore.collection(COLLECTION).document(id).get().get();
            if (!doc.exists()) return Optional.empty();
            return Optional.of(fromDoc(doc));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore read failed for seat: " + id, e);
        }
    }

    public List<Seat> findByFlightId(String flightId) {
        try {
            return firestore.collection(COLLECTION)
                    .whereEqualTo("flightId", flightId)
                    .get().get().getDocuments()
                    .stream().map(this::fromDoc).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore query failed for seats of flight: " + flightId, e);
        }
    }

    /** Fetch specific seats by IDs that belong to a given flight — replaces @Lock JPQL query. */
    public List<Seat> findByFlightIdAndIds(String flightId, List<String> seatIds) {
        return seatIds.stream()
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(s -> flightId.equals(s.getFlightId()))
                .collect(Collectors.toList());
    }

    public Seat save(Seat seat) {
        try {
            if (seat.getId() == null) seat.setId(UUID.randomUUID().toString());
            firestore.collection(COLLECTION).document(seat.getId()).set(toMap(seat)).get();
            return seat;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore write failed for seat: " + seat.getId(), e);
        }
    }

    public List<Seat> saveAll(List<Seat> seats) {
        seats.forEach(this::save);
        return seats;
    }

    private Map<String, Object> toMap(Seat s) {
        Map<String, Object> m = new HashMap<>();
        m.put("seatNumber", s.getSeatNumber());
        m.put("seatClass", s.getSeatClass().name());
        m.put("status", s.getStatus().name());
        m.put("price", s.getPrice().toPlainString());
        m.put("flightId", s.getFlightId());
        m.put("bookingId", s.getBookingId());
        return m;
    }

    private Seat fromDoc(DocumentSnapshot doc) {
        Seat s = new Seat();
        s.setId(doc.getId());
        s.setSeatNumber(doc.getString("seatNumber"));
        s.setSeatClass(SeatClass.valueOf(doc.getString("seatClass")));
        s.setStatus(SeatStatus.valueOf(doc.getString("status")));
        s.setPrice(new BigDecimal(doc.getString("price")));
        s.setFlightId(doc.getString("flightId"));
        s.setBookingId(doc.getString("bookingId"));
        return s;
    }

    private Seat fromDoc(QueryDocumentSnapshot doc) {
        return fromDoc((DocumentSnapshot) doc);
    }
}
