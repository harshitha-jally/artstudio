package com.artstudio.artstudio.controller;

import com.artstudio.artstudio.security.AuthUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal AuthUser user) {
        return Map.of("id", user.id(), "email", user.email(), "role", user.role());
    }

    @GetMapping("/studio/ping")
    @PreAuthorize("hasRole('STUDIO')")
    public Map<String, String> studioPing() {
        return Map.of("message", "Hello studio");
    }

    @GetMapping("/customer/ping")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Map<String, String> customerPing() {
        return Map.of("message", "Hello customer");
    }
}