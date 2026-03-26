package com.airline.reservation.controller;

import com.airline.reservation.dto.booking.BookingResponse;
import com.airline.reservation.dto.booking.CreateBookingRequest;
import com.airline.reservation.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request, Authentication authentication) {
        return bookingService.createBooking(authentication.getName(), request);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getBooking(@PathVariable Long bookingId, Authentication authentication) {
        return bookingService.getBooking(bookingId, authentication.getName());
    }

    @GetMapping("/me")
    public List<BookingResponse> getMyBookings(Authentication authentication) {
        return bookingService.getMyBookings(authentication.getName());
    }
}
