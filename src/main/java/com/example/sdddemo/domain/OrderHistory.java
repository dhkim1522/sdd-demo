package com.example.sdddemo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private String eventType;

    private String description;

    private LocalDateTime createdAt;

    public static OrderHistory of(Long orderId, String eventType, String description) {
        OrderHistory history = new OrderHistory();
        history.orderId = orderId;
        history.eventType = eventType;
        history.description = description;
        history.createdAt = LocalDateTime.now();
        return history;
    }
}
