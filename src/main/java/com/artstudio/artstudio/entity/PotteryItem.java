package com.artstudio.artstudio.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "pottery_items")
public class PotteryItem {

    @Id
    @Column(length = 10)
    private String id;   // "i-01"

    @Column(nullable = false, length = 100)
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}