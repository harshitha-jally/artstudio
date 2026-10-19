package com.artstudio.artstudio.dto;

public record AuthResponse(String token, String name, String email, String role) {
}