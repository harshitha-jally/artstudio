package com.artstudio.artstudio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        LocalDate date,
        Integer wallSize,
        String address,
        BigDecimal price,
        String status,
        LocalDateTime createdAt,
        String customerName,
        String customerEmail
) {
}