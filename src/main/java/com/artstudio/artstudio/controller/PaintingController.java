package com.artstudio.artstudio.controller;

import com.artstudio.artstudio.dto.PaintingRequest;
import com.artstudio.artstudio.entity.Painting;
import com.artstudio.artstudio.service.PaintingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/paintings")
public class PaintingController {

    private final PaintingService paintingService;

    public PaintingController(PaintingService paintingService) {
        this.paintingService = paintingService;
    }

    @GetMapping
    public List<Painting> list() {
        return paintingService.list();
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDIO')")
    public ResponseEntity<Painting> create(@Valid @RequestBody PaintingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paintingService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STUDIO')")
    public Painting update(@PathVariable String id, @Valid @RequestBody PaintingRequest request) {
        return paintingService.update(id, request);
    }
}