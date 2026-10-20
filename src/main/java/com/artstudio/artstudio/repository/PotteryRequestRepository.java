package com.artstudio.artstudio.repository;

import com.artstudio.artstudio.entity.Glaze;
import com.artstudio.artstudio.entity.PotteryItem;
import com.artstudio.artstudio.entity.PotteryRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PotteryRequestRepository extends JpaRepository<PotteryRequest, Long> {

    List<PotteryRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PotteryRequest> findAllByOrderByCreatedAtDesc();

    // Customers only: someone else's request is "not found" (404).
    Optional<PotteryRequest> findByIdAndUserId(Long id, Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PotteryRequest r SET r.item = :item, r.glaze = :glaze, r.quantity = :quantity, "
            + "r.neededBy = :neededBy WHERE r.id = :id AND r.status = 'requested'")
    int updateIfRequested(@Param("id") Long id,
                          @Param("item") PotteryItem item,
                          @Param("glaze") Glaze glaze,
                          @Param("quantity") Integer quantity,
                          @Param("neededBy") LocalDate neededBy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PotteryRequest r SET r.status = 'cancelled' WHERE r.id = :id AND r.status = 'requested'")
    int cancelIfRequested(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PotteryRequest r SET r.status = 'accepted' WHERE r.id = :id AND r.status = 'requested'")
    int acceptIfRequested(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PotteryRequest r SET r.status = 'declined' WHERE r.id = :id AND r.status = 'requested'")
    int declineIfRequested(@Param("id") Long id);
}