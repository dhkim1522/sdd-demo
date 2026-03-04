package com.example.sdddemo.event;

import com.example.sdddemo.domain.OrderStatus;

public record OrderStatusChangedEvent(
        Long orderId,
        OrderStatus previousStatus,
        OrderStatus newStatus
) {}
