package com.example.sdddemo.event;

import com.example.sdddemo.domain.OrderHistory;
import com.example.sdddemo.repository.OrderHistoryRepository;
import com.example.sdddemo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderHistoryRepository orderHistoryRepository;
    private final OrderRepository orderRepository;

    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[이벤트 수신] OrderCreatedEvent - 상품: {}, 수량: {}, 가격: {}",
                event.productName(), event.quantity(), event.price());

        // 이벤트 발행 시점에는 아직 save 전이므로, 이력에는 orderId 없이 기록
        // AbstractAggregateRoot는 save() 호출 시 이벤트를 발행하므로
        // 실제로는 save 후 이벤트가 발행됨 → orderId 조회 가능
        var order = orderRepository.findAll().stream()
                .filter(o -> o.getProductName().equals(event.productName()))
                .reduce((first, second) -> second)
                .orElse(null);

        Long orderId = order != null ? order.getId() : null;

        OrderHistory history = OrderHistory.of(
                orderId,
                "ORDER_CREATED",
                String.format("주문 생성 - 상품: %s, 수량: %d, 가격: %d원",
                        event.productName(), event.quantity(), event.price())
        );
        orderHistoryRepository.save(history);
    }

    @EventListener
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("[이벤트 수신] OrderStatusChangedEvent - 주문 ID: {}, {} → {}",
                event.orderId(), event.previousStatus(), event.newStatus());

        OrderHistory history = OrderHistory.of(
                event.orderId(),
                "STATUS_CHANGED",
                String.format("상태 변경: %s → %s", event.previousStatus(), event.newStatus())
        );
        orderHistoryRepository.save(history);
    }
}
