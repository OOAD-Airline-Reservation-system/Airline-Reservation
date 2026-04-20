package com.airline.reservation.service;

import com.airline.reservation.dto.booking.BookingResponse;
import com.airline.reservation.dto.booking.CreateBookingRequest;
import com.airline.reservation.entity.*;
import com.airline.reservation.exception.BadRequestException;
import com.airline.reservation.exception.ResourceNotFoundException;
import com.airline.reservation.factory.BookingFactory;
import com.airline.reservation.repository.BookingRepository;
import com.airline.reservation.repository.FlightRepository;
import com.airline.reservation.repository.SeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates booking creation and retrieval.
 * SRP: booking lifecycle only.
 * DIP: depends on repository and factory abstractions.
 * GRASP Controller: coordinates between repositories and factory.
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final FlightRepository flightRepository;
    private final BookingFactory bookingFactory;

    public BookingService(BookingRepository bookingRepository,
                          SeatRepository seatRepository,
                          FlightRepository flightRepository,
                          BookingFactory bookingFactory) {
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.flightRepository = flightRepository;
        this.bookingFactory = bookingFactory;
    }

    public BookingResponse createBooking(String userEmail, CreateBookingRequest request) {
        List<String> seatIds = request.getSeatIds();
        if (seatIds.stream().distinct().count() != seatIds.size()) {
            throw new BadRequestException("Duplicate seat ids are not allowed");
        }

        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found: " + request.getFlightId()));

        List<Seat> seats = seatRepository.findByFlightIdAndIds(request.getFlightId(), seatIds);
        if (seats.size() != seatIds.size()) {
            throw new BadRequestException("One or more seats do not belong to the selected flight");
        }
        if (seats.stream().anyMatch(s -> s.getStatus() != SeatStatus.AVAILABLE)) {
            throw new BadRequestException("One or more seats are no longer available");
        }

        Booking booking = bookingFactory.create(userEmail, flight, seats);
        booking.setBookingStep(BookingStep.SEATS_SELECTED);
        bookingRepository.save(booking);

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.LOCKED);
            seat.setBookingId(booking.getId());
            seatRepository.save(seat);
        }

        return mapToResponse(booking, seats, flight);
    }

    public Booking getBookingEntity(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }

    public BookingResponse getBooking(String bookingId, String userEmail) {
        Booking booking = getBookingEntity(bookingId);
        validateBookingOwnership(booking, userEmail);
        List<Seat> seats = seatRepository.findByFlightIdAndIds(booking.getFlightId(), booking.getSeatIds());
        Flight flight = flightRepository.findById(booking.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
        return mapToResponse(booking, seats, flight);
    }

    public List<BookingResponse> getMyBookings(String userEmail) {
        return bookingRepository.findByUserEmail(userEmail).stream()
                .map(b -> {
                    List<Seat> seats = seatRepository.findByFlightIdAndIds(b.getFlightId(), b.getSeatIds());
                    Flight flight = flightRepository.findById(b.getFlightId())
                            .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
                    return mapToResponse(b, seats, flight);
                }).toList();
    }

    public void validateBookingOwnership(Booking booking, String userEmail) {
        if (!booking.getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new BadRequestException("Booking does not belong to the authenticated user");
        }
    }

    public void validateBookingOwnershipById(String bookingId, String userEmail) {
        validateBookingOwnership(getBookingEntity(bookingId), userEmail);
    }

    public BookingResponse cancelBooking(String bookingId, String userEmail) {
        Booking booking = getBookingEntity(bookingId);
        validateBookingOwnership(booking, userEmail);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        List<Seat> seats = seatRepository.findByFlightIdAndIds(booking.getFlightId(), booking.getSeatIds());
        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setBookingId(null);
            seatRepository.save(seat);
        }
        Flight flight = flightRepository.findById(booking.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
        return mapToResponse(booking, seats, flight);
    }

    public BookingResponse mapToResponse(Booking booking, List<Seat> seats, Flight flight) {
        BookingResponse r = new BookingResponse();
        r.setId(booking.getId());
        r.setBookingReference(booking.getBookingReference());
        r.setFlightId(flight.getId());
        r.setFlightNumber(flight.getFlightNumber());
        r.setStatus(booking.getStatus());
        r.setPaymentStatus(booking.getPaymentStatus());
        r.setTotalAmount(booking.getTotalAmount());
        r.setBookedAt(booking.getBookedAt());
        r.setSeats(seats.stream().map(Seat::getSeatNumber).toList());
        r.setBookingStep(booking.getBookingStep());
        return r;
    }
}
