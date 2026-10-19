package com.artstudio.artstudio.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaintingRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 300, message = "Title must be at most 300 characters")
        String title,

        @NotBlank(message = "Artist is required")
        @Size(max = 100, message = "Artist must be at most 100 characters")
        String artist,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        @DecimalMax(value = "99999999.99", message = "Price is too large")
        BigDecimal price,

        @Size(max = 255, message = "Image path must be at most 255 characters")
        String imageUrl
) {
}