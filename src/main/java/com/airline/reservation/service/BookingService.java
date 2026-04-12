package com.airline.reservation.service;

import com.airline.reservation.dto.booking.BookingResponse;
import com.airline.reservation.dto.booking.CreateBookingRequest;
import com.airline.reservation.entity.Booking;
import com.airline.reservation.entity.Seat;
import com.airline.reservation.entity.SeatStatus;
import com.airline.reservation.entity.User;
import com.airline.reservation.exception.BadRequestException;
import com.airline.reservation.exception.ResourceNotFoundException;
import com.airline.reservation.factory.BookingFactory;
import com.airline.reservation.repository.BookingRepository;
import com.airline.reservation.repository.SeatRepository;
import com.airline.reservation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final FlightService flightService;
    private final UserRepository userRepository;
    private final BookingFactory bookingFactory;

    public BookingService(
            BookingRepository bookingRepository,
            SeatRepository seatRepository,
            FlightService flightService,
            UserRepository userRepository,
            BookingFactory bookingFactory
    ) {
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.flightService = flightService;
        this.userRepository = userRepository;
        this.bookingFactory = bookingFactory;
    }

    @Transactional
    public BookingResponse createBooking(String userEmail, CreateBookingRequest request) {
        if (request.getSeatIds().stream().distinct().count() != request.getSeatIds().size()) {
            throw new BadRequestException("Duplicate seat ids are not allowed");
        }

        User user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var flight = flightService.getFlightById(request.getFlightId());
        List<Seat> seats = seatRepository.findSeatsForUpdate(request.getFlightId(), request.getSeatIds());

        if (seats.size() != request.getSeatIds().size()) {
            throw new BadRequestException("One or more seats do not belong to the selected flight");
        }

        boolean unavailable = seats.stream().anyMatch(seat -> seat.getStatus() != SeatStatus.AVAILABLE);
        if (unavailable) {
            throw new BadRequestException("One or more seats are no longer available");
        }

        Booking booking = bookingFactory.create(user, flight, seats);
        bookingRepository.save(booking);

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.LOCKED);
            seat.setBooking(booking);
        }
        seatRepository.saveAll(seats);

        return mapToResponse(bookingRepository.findById(booking.getId()).orElseThrow());
    }

    public Booking getBookingEntity(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }

    public BookingResponse getBooking(Long bookingId, String userEmail) {
        Booking booking = getBookingEntity(bookingId);
        validateBookingOwnership(booking, userEmail);
        return mapToResponse(booking);
    }

    public List<BookingResponse> getMyBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return bookingRepository.findByUserId(user.getId()).stream().map(this::mapToResponse).toList();
    }

    public void validateBookingOwnership(Booking booking, String userEmail) {
        if (!booking.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new BadRequestException("Booking does not belong to the authenticated user");
        }
    }

    public BookingResponse mapToResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setBookingReference(booking.getBookingReference());
        response.setFlightId(booking.getFlight().getId());
        response.setFlightNumber(booking.getFlight().getFlightNumber());
        response.setStatus(booking.getStatus());
        response.setPaymentStatus(booking.getPaymentStatus());
        response.setTotalAmount(booking.getTotalAmount());
        response.setBookedAt(booking.getBookedAt());
        response.setSeats(booking.getSeats().stream().map(Seat::getSeatNumber).toList());
        return response;
    }
}
