package com.artstudio.artstudio.service;

import com.artstudio.artstudio.dto.PaintingRequest;
import com.artstudio.artstudio.entity.Painting;
import com.artstudio.artstudio.exception.ApiException;
import com.artstudio.artstudio.repository.PaintingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaintingService {

    private final PaintingRepository paintingRepository;

    public PaintingService(PaintingRepository paintingRepository) {
        this.paintingRepository = paintingRepository;
    }

    public List<Painting> list() {
        return paintingRepository.findAll(Sort.by("id"));
    }

    public synchronized Painting create(PaintingRequest request) {
        Painting painting = new Painting();
        painting.setId(nextId());
        painting.setStatus("available");
        applyFields(painting, request);
        return paintingRepository.save(painting);
    }

    public Painting update(String id, PaintingRequest request) {
        Painting painting = paintingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Painting not found"));

        if (!"available".equals(painting.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Only available paintings can be edited");
        }

        applyFields(painting, request);
        return paintingRepository.save(painting);
    }

    private void applyFields(Painting painting, PaintingRequest request) {
        painting.setTitle(request.title().trim());
        painting.setArtist(request.artist().trim());
        painting.setDescription(request.description());
        painting.setPrice(request.price());
        painting.setImageUrl(request.imageUrl());
    }

    // Seed ids look like p-001 ... p-012, so the next new painting becomes p-013.
    private String nextId() {
        String maxId = paintingRepository.findMaxId();
        int next = (maxId == null) ? 1 : Integer.parseInt(maxId.substring(2)) + 1;
        return String.format("p-%03d", next);
    }
}