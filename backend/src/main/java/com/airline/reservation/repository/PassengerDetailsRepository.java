package com.airline.reservation.repository;

import com.airline.reservation.entity.PassengerDetails;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore-backed repository for PassengerDetails.
 * Sub-collection: "bookings/{bookingId}/passengers".
 * Pure Fabrication (GRASP): no domain logic, only persistence concern.
 */
@Repository
public class PassengerDetailsRepository {

    private static final String BOOKINGS = "bookings";
    private static final String PASSENGERS = "passengers";

    private final Firestore firestore;

    public PassengerDetailsRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<PassengerDetails> findByBookingId(String bookingId) {
        try {
            return firestore.collection(BOOKINGS)
                    .document(bookingId)
                    .collection(PASSENGERS)
                    .get().get()
                    .getDocuments()
                    .stream()
                    .map(this::fromDoc)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore read failed for passengers of booking " + bookingId, e);
        }
    }

    public void deleteAllByBookingId(String bookingId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(BOOKINGS)
                    .document(bookingId)
                    .collection(PASSENGERS)
                    .get().get()
                    .getDocuments();
            for (QueryDocumentSnapshot doc : docs) {
                doc.getReference().delete().get();
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore delete failed for passengers of booking " + bookingId, e);
        }
    }

    public List<PassengerDetails> saveAll(String bookingId, List<PassengerDetails> passengers) {
        try {
            for (PassengerDetails p : passengers) {
                if (p.getId() == null) p.setId(UUID.randomUUID().toString());
                p.setBookingId(bookingId);
                firestore.collection(BOOKINGS)
                        .document(bookingId)
                        .collection(PASSENGERS)
                        .document(p.getId())
                        .set(toMap(p)).get();
            }
            return passengers;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore write failed for passengers of booking " + bookingId, e);
        }
    }

    private Map<String, Object> toMap(PassengerDetails p) {
        Map<String, Object> m = new HashMap<>();
        m.put("bookingId", p.getBookingId());
        m.put("firstName", p.getFirstName());
        m.put("lastName", p.getLastName());
        m.put("dateOfBirth", p.getDateOfBirth());
        m.put("nationality", p.getNationality());
        m.put("passportNumber", p.getPassportNumber());
        m.put("passportExpiry", p.getPassportExpiry());
        m.put("passportCountry", p.getPassportCountry());
        m.put("nationalIdNumber", p.getNationalIdNumber());
        m.put("nationalIdType", p.getNationalIdType());
        m.put("seatNumber", p.getSeatNumber());
        m.put("contactEmail", p.getContactEmail());
        m.put("contactPhone", p.getContactPhone());
        m.put("mealPreference", p.getMealPreference());
        m.put("specialAssistance", p.getSpecialAssistance());
        return m;
    }

    private PassengerDetails fromDoc(QueryDocumentSnapshot doc) {
        PassengerDetails p = new PassengerDetails();
        p.setId(doc.getId());
        p.setBookingId(doc.getString("bookingId"));
        p.setFirstName(doc.getString("firstName"));
        p.setLastName(doc.getString("lastName"));
        p.setDateOfBirth(doc.getString("dateOfBirth"));
        p.setNationality(doc.getString("nationality"));
        p.setPassportNumber(doc.getString("passportNumber"));
        p.setPassportExpiry(doc.getString("passportExpiry"));
        p.setPassportCountry(doc.getString("passportCountry"));
        p.setNationalIdNumber(doc.getString("nationalIdNumber"));
        p.setNationalIdType(doc.getString("nationalIdType"));
        p.setSeatNumber(doc.getString("seatNumber"));
        p.setContactEmail(doc.getString("contactEmail"));
        p.setContactPhone(doc.getString("contactPhone"));
        p.setMealPreference(doc.getString("mealPreference"));
        p.setSpecialAssistance(doc.getString("specialAssistance"));
        return p;
    }
}
