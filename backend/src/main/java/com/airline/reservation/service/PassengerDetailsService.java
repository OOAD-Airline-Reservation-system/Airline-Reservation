package com.airline.reservation.service;

import com.airline.reservation.entity.BookingStep;
import com.airline.reservation.entity.PassengerDetails;
import com.airline.reservation.exception.BadRequestException;
import com.airline.reservation.repository.BookingRepository;
import com.airline.reservation.repository.PassengerDetailsRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages passenger travel document details per booking.
 * SRP: only passenger details persistence logic.
 * DIP: depends on PassengerDetailsRepository abstraction.
 * Ownership validation is the caller's responsibility (controller delegates to BookingService).
 */
@Service
public class PassengerDetailsService {

    private final PassengerDetailsRepository passengerDetailsRepository;
    private final BookingRepository bookingRepository;

    public PassengerDetailsService(PassengerDetailsRepository passengerDetailsRepository,
                                   BookingRepository bookingRepository) {
        this.passengerDetailsRepository = passengerDetailsRepository;
        this.bookingRepository = bookingRepository;
    }

    /** Replace all passenger details for a booking (delete-then-insert). */
    public List<PassengerDetails> savePassengers(String bookingId, List<PassengerDetails> passengers) {
        if (passengers == null || passengers.isEmpty()) {
            throw new BadRequestException("Passenger list must not be empty");
        }
        passengerDetailsRepository.deleteAllByBookingId(bookingId);
        List<PassengerDetails> saved = passengerDetailsRepository.saveAll(bookingId, passengers);
        bookingRepository.findById(bookingId).ifPresent(b -> {
            b.setBookingStep(BookingStep.PASSENGERS_SAVED);
            bookingRepository.save(b);
        });
        return saved;
    }

    public List<Map<String, Object>> getPassengers(String bookingId) {
        return passengerDetailsRepository.findByBookingId(bookingId)
                .stream().map(this::toMap).toList();
    }

    private Map<String, Object> toMap(PassengerDetails p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
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
}
