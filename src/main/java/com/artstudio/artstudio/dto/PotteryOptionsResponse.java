package com.artstudio.artstudio.dto;

import com.artstudio.artstudio.entity.Glaze;
import com.artstudio.artstudio.entity.PotteryItem;

import java.util.List;

public record PotteryOptionsResponse(List<PotteryItem> items, List<Glaze> glazes) {
}