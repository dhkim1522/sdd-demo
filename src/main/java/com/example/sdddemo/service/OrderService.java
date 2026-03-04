package com.example.sdddemo.service;

import com.example.sdddemo.domain.Order;
import com.example.sdddemo.domain.OrderHistory;
import com.example.sdddemo.repository.OrderHistoryRepository;
import com.example.sdddemo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderHistoryRepository orderHistoryRepository;

    @Transactional
    public Order createOrder(String productName, int quantity, int price) {
        Order order = Order.create(productName, quantity, price);
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirmOrder(Long orderId) {
        Order order = findOrderById(orderId);
        order.confirm();
        return orderRepository.save(order);
    }

    @Transactional
    public Order shipOrder(Long orderId) {
        Order order = findOrderById(orderId);
        order.ship();
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = findOrderById(orderId);
        order.cancel();
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> findAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderHistory> findOrderHistories(Long orderId) {
        findOrderById(orderId);
        return orderHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }
}
