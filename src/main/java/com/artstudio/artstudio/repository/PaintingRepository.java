package com.artstudio.artstudio.repository;

import com.artstudio.artstudio.entity.Painting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaintingRepository extends JpaRepository<Painting, String> {
}
