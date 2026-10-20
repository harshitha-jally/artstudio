package com.artstudio.artstudio.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PotteryForm(
        @NotBlank(message = "Item is required")
        String itemId,

        @NotBlank(message = "Glaze is required")
        String glazeId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 12, message = "Quantity must be at most 12")
        Integer quantity,

        @NotNull(message = "Needed-by date is required")
        LocalDate neededBy
) {
}