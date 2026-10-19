package com.artstudio.artstudio.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderRequest(
        @NotBlank(message = "paintingId is required") String paintingId
) {
}