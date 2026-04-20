package com.airline.reservation.controller;

import com.airline.reservation.entity.PassengerDetails;
import com.airline.reservation.service.BookingService;
import com.airline.reservation.service.PassengerDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for passenger travel document management.
 * Controller (GRASP): coordinates BookingService (ownership) + PassengerDetailsService (data).
 * SRP: only HTTP routing concern.
 */
@RestController
@RequestMapping("/api/bookings/{bookingId}/passengers")
public class PassengerDetailsController {

    private final PassengerDetailsService passengerDetailsService;
    private final BookingService bookingService;

    public PassengerDetailsController(PassengerDetailsService passengerDetailsService,
                                      BookingService bookingService) {
        this.passengerDetailsService = passengerDetailsService;
        this.bookingService = bookingService;
    }

    /** POST /api/bookings/{bookingId}/passengers */
    @PostMapping
    public ResponseEntity<List<PassengerDetails>> savePassengers(
            @PathVariable String bookingId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody List<PassengerDetails> passengers) {

        bookingService.validateBookingOwnershipById(bookingId, userDetails.getUsername());
        return ResponseEntity.ok(passengerDetailsService.savePassengers(bookingId, passengers));
    }

    /** GET /api/bookings/{bookingId}/passengers */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getPassengers(
            @PathVariable String bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        bookingService.validateBookingOwnershipById(bookingId, userDetails.getUsername());
        return ResponseEntity.ok(passengerDetailsService.getPassengers(bookingId));
    }
}
