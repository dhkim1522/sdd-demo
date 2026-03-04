package com.example.sdddemo.controller.dto;

public record CreateOrderRequest(
        String productName,
        int quantity,
        int price
) {}
