package com.artstudio.artstudio.service;

import com.artstudio.artstudio.dto.OrderResponse;
import com.artstudio.artstudio.entity.Painting;
import com.artstudio.artstudio.entity.PaintingOrder;
import com.artstudio.artstudio.entity.User;
import com.artstudio.artstudio.exception.ApiException;
import com.artstudio.artstudio.repository.PaintingOrderRepository;
import com.artstudio.artstudio.repository.PaintingRepository;
import com.artstudio.artstudio.repository.UserRepository;
import com.artstudio.artstudio.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class OrderService {

    private final PaintingOrderRepository orderRepository;
    private final PaintingRepository paintingRepository;
    private final UserRepository userRepository;

    public OrderService(PaintingOrderRepository orderRepository,
                        PaintingRepository paintingRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.paintingRepository = paintingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderResponse buy(Long userId, String paintingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please log in again"));

        Painting painting = paintingRepository.findById(paintingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Painting not found"));

        // The one atomic step: only one buyer can ever change 'available' to 'sold'.
        int updated = paintingRepository.markSoldIfAvailable(paintingId);
        if (updated == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Sorry, this painting was just sold");
        }

        PaintingOrder order = new PaintingOrder();
        order.setUser(user);
        order.setPainting(painting);
        order.setPrice(painting.getPrice());
        order.setStatus("placed");
        order.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        orderRepository.save(order);

        return toResponse(order);
    }

    public List<OrderResponse> list(AuthUser user) {
        List<PaintingOrder> orders = "studio".equals(user.role())
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByUserIdOrderByCreatedAtDesc(user.id());
        return orders.stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse cancel(Long userId, Long orderId) {
        // Not yours (or not found) -> 404, as the brief requires.
        PaintingOrder order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        int updated = orderRepository.cancelIfPlaced(order.getId());
        if (updated == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "This order is already cancelled");
        }

        paintingRepository.markAvailable(order.getPainting().getId());
        order.setStatus("cancelled");

        return toResponse(order);
    }

    private OrderResponse toResponse(PaintingOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getPainting().getId(),
                order.getPainting().getTitle(),
                order.getPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUser().getName(),
                order.getUser().getEmail()
        );
    }
}