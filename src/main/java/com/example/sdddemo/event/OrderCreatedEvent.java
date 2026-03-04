package com.example.sdddemo.event;

public record OrderCreatedEvent(
        String productName,
        int quantity,
        int price
) {}
