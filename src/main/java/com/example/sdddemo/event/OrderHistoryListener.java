package com.example.sdddemo.event;

import com.example.sdddemo.domain.OrderHistory;
import com.example.sdddemo.repository.OrderHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderHistoryListener {

    private final OrderHistoryRepository orderHistoryRepository;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderCreated(OrderCreatedEvent event) {
        var order = event.order();
        log.info("[별도 트랜잭션] 주문 생성 이력 저장 - 주문 ID: {}, 상품: {}",
                order.getId(), order.getProductName());

        OrderHistory history = OrderHistory.of(
                order.getId(),
                "ORDER_CREATED",
                String.format("주문 생성 - 상품: %s, 수량: %d, 가격: %d원",
                        order.getProductName(), order.getQuantity(), order.getPrice())
        );
        orderHistoryRepository.save(history);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("[별도 트랜잭션] 상태 변경 이력 저장 - 주문 ID: {}, {} → {}",
                event.orderId(), event.previousStatus(), event.newStatus());

        OrderHistory history = OrderHistory.of(
                event.orderId(),
                "STATUS_CHANGED",
                String.format("상태 변경: %s → %s", event.previousStatus(), event.newStatus())
        );
        orderHistoryRepository.save(history);
    }
}
