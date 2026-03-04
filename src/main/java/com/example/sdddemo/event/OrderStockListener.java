package com.example.sdddemo.event;

import com.example.sdddemo.domain.Stock;
import com.example.sdddemo.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStockListener {

    private final StockRepository stockRepository;

    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        var order = event.order();
        log.info("[같은 트랜잭션] 재고 차감 시작 - 상품: {}, 수량: {}",
                order.getProductName(), order.getQuantity());

        Stock stock = stockRepository.findByProductName(order.getProductName())
                .orElseThrow(() -> new IllegalStateException(
                        "재고 정보를 찾을 수 없습니다. 상품: " + order.getProductName()));

        stock.decrease(order.getQuantity());
        stockRepository.save(stock);

        log.info("[같은 트랜잭션] 재고 차감 완료 - 상품: {}, 남은 재고: {}",
                stock.getProductName(), stock.getQuantity());
    }
}
