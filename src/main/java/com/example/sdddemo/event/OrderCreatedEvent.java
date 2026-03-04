package com.example.sdddemo.event;

import com.example.sdddemo.domain.Order;

public record OrderCreatedEvent(
        Order order
) {}
