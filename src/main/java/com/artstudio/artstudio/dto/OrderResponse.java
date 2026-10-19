package com.artstudio.artstudio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        String paintingId,
        String paintingTitle,
        BigDecimal price,
        String status,
        LocalDateTime createdAt,
        String customerName,
        String customerEmail
) {
}