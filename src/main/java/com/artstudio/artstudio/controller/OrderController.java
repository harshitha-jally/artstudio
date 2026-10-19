package com.artstudio.artstudio.controller;

import com.artstudio.artstudio.dto.OrderRequest;
import com.artstudio.artstudio.dto.OrderResponse;
import com.artstudio.artstudio.security.AuthUser;
import com.artstudio.artstudio.service.OrderService;
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
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> buy(@AuthenticationPrincipal AuthUser user,
                                             @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.buy(user.id(), request.paintingId()));
    }

    @GetMapping
    public List<OrderResponse> list(@AuthenticationPrincipal AuthUser user) {
        return orderService.list(user);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse cancel(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return orderService.cancel(user.id(), id);
    }
}