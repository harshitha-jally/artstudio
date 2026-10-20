package com.artstudio.artstudio.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PotteryResponse(
        Long id,
        String itemId,
        String itemName,
        String glazeId,
        String glazeName,
        Integer quantity,
        LocalDate neededBy,
        String status,
        LocalDateTime createdAt,
        String customerName,
        String customerEmail
) {
}