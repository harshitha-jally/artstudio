package com.artstudio.artstudio.controller;

import com.artstudio.artstudio.dto.BookingRequest;
import com.artstudio.artstudio.dto.BookingResponse;
import com.artstudio.artstudio.security.AuthUser;
import com.artstudio.artstudio.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingResponse> create(@AuthenticationPrincipal AuthUser user,
                                                  @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.create(user.id(), request));
    }

    @GetMapping
    public List<BookingResponse> list(@AuthenticationPrincipal AuthUser user) {
        return bookingService.list(user);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public BookingResponse cancel(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return bookingService.cancel(user.id(), id);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('STUDIO')")
    public BookingResponse confirm(@PathVariable Long id) {
        return bookingService.confirm(id);
    }

    @PostMapping("/{id}/decline")
    @PreAuthorize("hasRole('STUDIO')")
    public BookingResponse decline(@PathVariable Long id) {
        return bookingService.decline(id);
    }
}