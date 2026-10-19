package com.artstudio.artstudio.repository;

import com.artstudio.artstudio.entity.PotteryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PotteryItemRepository extends JpaRepository<PotteryItem, String> {
}