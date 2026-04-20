package com.airline.reservation.repository;

import com.airline.reservation.entity.Flight;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore-backed repository for Flight.
 * Collection: "flights" — document ID = UUID.
 * Pure Fabrication (GRASP): no domain logic, only persistence.
 */
@Repository
public class FlightRepository {

    private static final String COLLECTION = "flights";
    private final Firestore firestore;

    public FlightRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Optional<Flight> findById(String id) {
        try {
            DocumentSnapshot doc = firestore.collection(COLLECTION).document(id).get().get();
            if (!doc.exists()) return Optional.empty();
            return Optional.of(fromDoc(doc));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore read failed for flight: " + id, e);
        }
    }

    public Optional<Flight> findByFlightNumber(String flightNumber) {
        try {
            return firestore.collection(COLLECTION)
                    .whereEqualTo("flightNumber", flightNumber)
                    .get().get().getDocuments()
                    .stream().map(this::fromDoc).findFirst();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore query failed for flightNumber: " + flightNumber, e);
        }
    }

    public List<Flight> findBySourceAndDestinationAndDepartureBetween(
            String source, String destination, LocalDateTime start, LocalDateTime end) {
        try {
            return firestore.collection(COLLECTION)
                    .whereEqualTo("source", source)
                    .whereEqualTo("destination", destination)
                    .get().get().getDocuments()
                    .stream().map(this::fromDoc)
                    .filter(f -> !f.getDepartureTime().isBefore(start) && !f.getDepartureTime().isAfter(end))
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore query failed for flights", e);
        }
    }

    public long count() {
        try {
            return firestore.collection(COLLECTION).get().get().size();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore count failed", e);
        }
    }

    public Flight save(Flight flight) {
        try {
            if (flight.getId() == null) flight.setId(UUID.randomUUID().toString());
            firestore.collection(COLLECTION).document(flight.getId()).set(toMap(flight)).get();
            return flight;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore write failed for flight: " + flight.getId(), e);
        }
    }

    public List<Flight> saveAll(List<Flight> flights) {
        flights.forEach(this::save);
        return flights;
    }

    private Map<String, Object> toMap(Flight f) {
        Map<String, Object> m = new HashMap<>();
        m.put("flightNumber", f.getFlightNumber());
        m.put("source", f.getSource());
        m.put("destination", f.getDestination());
        m.put("departureTime", f.getDepartureTime().toString());
        m.put("arrivalTime", f.getArrivalTime().toString());
        m.put("basePrice", f.getBasePrice().toPlainString());
        return m;
    }

    private Flight fromDoc(DocumentSnapshot doc) {
        Flight f = new Flight();
        f.setId(doc.getId());
        f.setFlightNumber(doc.getString("flightNumber"));
        f.setSource(doc.getString("source"));
        f.setDestination(doc.getString("destination"));
        f.setDepartureTime(LocalDateTime.parse(doc.getString("departureTime")));
        f.setArrivalTime(LocalDateTime.parse(doc.getString("arrivalTime")));
        f.setBasePrice(new BigDecimal(doc.getString("basePrice")));
        return f;
    }

    private Flight fromDoc(QueryDocumentSnapshot doc) {
        return fromDoc((DocumentSnapshot) doc);
    }
}
