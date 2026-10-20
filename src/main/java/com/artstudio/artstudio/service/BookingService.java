package com.artstudio.artstudio.service;

import com.artstudio.artstudio.dto.BookingRequest;
import com.artstudio.artstudio.dto.BookingResponse;
import com.artstudio.artstudio.entity.User;
import com.artstudio.artstudio.entity.WallBooking;
import com.artstudio.artstudio.exception.ApiException;
import com.artstudio.artstudio.repository.UserRepository;
import com.artstudio.artstudio.repository.WallBookingRepository;
import com.artstudio.artstudio.security.AuthUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final WallBookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BigDecimal muralRate;

    public BookingService(WallBookingRepository bookingRepository,
                          UserRepository userRepository,
                          @Value("${studio.mural-rate}") BigDecimal muralRate) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.muralRate = muralRate;
    }

    public BookingResponse create(Long userId, BookingRequest request) {
        LocalDate today = LocalDate.now(IST);
        LocalDate date = request.date();

        if (date.isBefore(today.plusDays(7)) || date.isAfter(today.plusDays(90))) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Date must be between 7 and 90 days from today");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please log in again"));

        // Friendly early check. The UNIQUE column below is what really protects us.
        if (bookingRepository.existsByActiveDate(date)) {
            throw new ApiException(HttpStatus.CONFLICT, "Sorry, this date was just taken");
        }

        WallBooking booking = new WallBooking();
        booking.setUser(user);
        booking.setBookingDate(date);
        booking.setActiveDate(date);
        booking.setWallSize(request.wallSize());
        booking.setAddress(request.address().trim());
        booking.setPrice(muralRate.multiply(BigDecimal.valueOf(request.wallSize())));
        booking.setStatus("requested");
        booking.setCreatedAt(LocalDateTime.now(IST));

        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            // Two people tried the same date at the same moment, and the database stopped the second one.
            throw new ApiException(HttpStatus.CONFLICT, "Sorry, this date was just taken");
        }

        return toResponse(booking);
    }

    public List<BookingResponse> list(AuthUser user) {
        List<WallBooking> bookings = "studio".equals(user.role())
                ? bookingRepository.findAllByOrderByBookingDateAsc()
                : bookingRepository.findByUserIdOrderByBookingDateAsc(user.id());
        return bookings.stream().map(this::toResponse).toList();
    }

    @Transactional
    public BookingResponse cancel(Long userId, Long bookingId) {
        // Not yours (or not found) gives 404.
        WallBooking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));

        LocalDate today = LocalDate.now(IST);
        long daysLeft = ChronoUnit.DAYS.between(today, booking.getBookingDate());
        if (daysLeft < 3) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Bookings can only be cancelled until 3 days before the date");
        }

        if (bookingRepository.cancelIfActive(bookingId) == 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This booking is already " + booking.getStatus());
        }

        return reload(bookingId);
    }

    @Transactional
    public BookingResponse confirm(Long bookingId) {
        findForStudio(bookingId);
        if (bookingRepository.confirmIfRequested(bookingId) == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Only requested bookings can be confirmed");
        }
        return reload(bookingId);
    }

    @Transactional
    public BookingResponse decline(Long bookingId) {
        findForStudio(bookingId);
        if (bookingRepository.declineIfRequested(bookingId) == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Only requested bookings can be declined");
        }
        return reload(bookingId);
    }

    private WallBooking findForStudio(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private BookingResponse reload(Long bookingId) {
        return toResponse(findForStudio(bookingId));
    }

    private BookingResponse toResponse(WallBooking b) {
        return new BookingResponse(
                b.getId(),
                b.getBookingDate(),
                b.getWallSize(),
                b.getAddress(),
                b.getPrice(),
                b.getStatus(),
                b.getCreatedAt(),
                b.getUser().getName(),
                b.getUser().getEmail()
        );
    }
}