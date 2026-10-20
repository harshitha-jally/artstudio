package com.artstudio.artstudio.controller;

import com.artstudio.artstudio.dto.PotteryForm;
import com.artstudio.artstudio.dto.PotteryOptionsResponse;
import com.artstudio.artstudio.dto.PotteryResponse;
import com.artstudio.artstudio.security.AuthUser;
import com.artstudio.artstudio.service.PotteryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pottery")
public class PotteryController {

    private final PotteryService potteryService;

    public PotteryController(PotteryService potteryService) {
        this.potteryService = potteryService;
    }

    @GetMapping("/options")
    public PotteryOptionsResponse options() {
        return potteryService.options();
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PotteryResponse> create(@AuthenticationPrincipal AuthUser user,
                                                  @Valid @RequestBody PotteryForm form) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(potteryService.create(user.id(), form));
    }

    @GetMapping
    public List<PotteryResponse> list(@AuthenticationPrincipal AuthUser user) {
        return potteryService.list(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public PotteryResponse update(@AuthenticationPrincipal AuthUser user,
                                  @PathVariable Long id,
                                  @Valid @RequestBody PotteryForm form) {
        return potteryService.update(user.id(), id, form);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public PotteryResponse cancel(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return potteryService.cancel(user.id(), id);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('STUDIO')")
    public PotteryResponse accept(@PathVariable Long id) {
        return potteryService.accept(id);
    }

    @PostMapping("/{id}/decline")
    @PreAuthorize("hasRole('STUDIO')")
    public PotteryResponse decline(@PathVariable Long id) {
        return potteryService.decline(id);
    }
}