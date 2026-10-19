package com.artstudio.artstudio.repository;

import com.artstudio.artstudio.entity.Painting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaintingRepository extends JpaRepository<Painting, String> {

    @Query("SELECT MAX(p.id) FROM Painting p")
    String findMaxId();

    // Returns 1 if the painting was available and is now sold, 0 if someone else got it first.
    @Modifying
    @Query("UPDATE Painting p SET p.status = 'sold' WHERE p.id = :id AND p.status = 'available'")
    int markSoldIfAvailable(@Param("id") String id);

    @Modifying
    @Query("UPDATE Painting p SET p.status = 'available' WHERE p.id = :id AND p.status = 'sold'")
    int markAvailable(@Param("id") String id);
}