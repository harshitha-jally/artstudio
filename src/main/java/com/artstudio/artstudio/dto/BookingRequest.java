package com.artstudio.artstudio.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BookingRequest(
        @NotNull(message = "Date is required")
        LocalDate date,

        @NotNull(message = "Wall size is required")
        @Min(value = 10, message = "Wall size must be at least 10 sq ft")
        @Max(value = 500, message = "Wall size must be at most 500 sq ft")
        Integer wallSize,

        @NotBlank(message = "Address is required")
        @Size(max = 300, message = "Address must be at most 300 characters")
        String address
) {
}