package com.artstudio.artstudio.repository;

import com.artstudio.artstudio.entity.PaintingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaintingOrderRepository extends JpaRepository<PaintingOrder, Long> {

    List<PaintingOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PaintingOrder> findAllByOrderByCreatedAtDesc();

    // Used for customers: an order that belongs to someone else simply "does not exist" (404).
    Optional<PaintingOrder> findByIdAndUserId(Long id, Long userId);

    // Same trick as buying: only the first of two simultaneous cancels changes a row.
    @Modifying
    @Query("UPDATE PaintingOrder o SET o.status = 'cancelled' WHERE o.id = :id AND o.status = 'placed'")
    int cancelIfPlaced(@Param("id") Long id);
}