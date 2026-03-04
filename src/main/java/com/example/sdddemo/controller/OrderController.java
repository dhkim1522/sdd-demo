package com.example.sdddemo.controller;

import com.example.sdddemo.controller.dto.CreateOrderRequest;
import com.example.sdddemo.domain.Order;
import com.example.sdddemo.domain.OrderHistory;
import com.example.sdddemo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request.productName(), request.quantity(), request.price());
    }

    @PostMapping("/{id}/confirm")
    public Order confirmOrder(@PathVariable Long id) {
        return orderService.confirmOrder(id);
    }

    @PostMapping("/{id}/ship")
    public Order shipOrder(@PathVariable Long id) {
        return orderService.shipOrder(id);
    }

    @PostMapping("/{id}/cancel")
    public Order cancelOrder(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }

    @GetMapping
    public List<Order> findAllOrders() {
        return orderService.findAllOrders();
    }

    @GetMapping("/{id}")
    public Order findOrder(@PathVariable Long id) {
        return orderService.findOrderById(id);
    }

    @GetMapping("/{id}/histories")
    public List<OrderHistory> findOrderHistories(@PathVariable Long id) {
        return orderService.findOrderHistories(id);
    }
}
