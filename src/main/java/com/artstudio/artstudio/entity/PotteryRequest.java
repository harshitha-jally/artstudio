package com.artstudio.artstudio.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pottery_requests")
public class PotteryRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private PotteryItem item;

    @ManyToOne(optional = false)
    @JoinColumn(name = "glaze_id", nullable = false)
    private Glaze glaze;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "needed_by", nullable = false)
    private LocalDate neededBy;

    @Column(nullable = false, length = 20)
    private String status;   // requested, accepted, declined, cancelled

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PotteryItem getItem() {
        return item;
    }

    public void setItem(PotteryItem item) {
        this.item = item;
    }

    public Glaze getGlaze() {
        return glaze;
    }

    public void setGlaze(Glaze glaze) {
        this.glaze = glaze;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getNeededBy() {
        return neededBy;
    }

    public void setNeededBy(LocalDate neededBy) {
        this.neededBy = neededBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}