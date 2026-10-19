package com.artstudio.artstudio.repository;

import com.artstudio.artstudio.entity.WallBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WallBookingRepository extends JpaRepository<WallBooking, Long> {

    boolean existsByActiveDate(LocalDate activeDate);

    List<WallBooking> findByUserIdOrderByBookingDateAsc(Long userId);

    List<WallBooking> findAllByOrderByBookingDateAsc();

    // Customers only: someone else's booking is "not found" (404).
    Optional<WallBooking> findByIdAndUserId(Long id, Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE WallBooking b SET b.status = 'confirmed' WHERE b.id = :id AND b.status = 'requested'")
    int confirmIfRequested(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE WallBooking b SET b.status = 'declined', b.activeDate = NULL WHERE b.id = :id AND b.status = 'requested'")
    int declineIfRequested(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE WallBooking b SET b.status = 'cancelled', b.activeDate = NULL WHERE b.id = :id AND b.status IN ('requested', 'confirmed')")
    int cancelIfActive(@Param("id") Long id);
}
