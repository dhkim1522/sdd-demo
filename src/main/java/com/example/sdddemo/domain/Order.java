package com.example.sdddemo.domain;

import com.example.sdddemo.event.OrderCreatedEvent;
import com.example.sdddemo.event.OrderStatusChangedEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends AbstractAggregateRoot<Order> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    private int quantity;

    private int price;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime createdAt;

    public static Order create(String productName, int quantity, int price) {
        Order order = new Order();
        order.productName = productName;
        order.quantity = quantity;
        order.price = price;
        order.status = OrderStatus.CREATED;
        order.createdAt = LocalDateTime.now();
        order.registerEvent(new OrderCreatedEvent(order));
        return order;
    }

    public void confirm() {
        validateStatus(OrderStatus.CREATED, "확정");
        OrderStatus previousStatus = this.status;
        this.status = OrderStatus.CONFIRMED;
        registerEvent(new OrderStatusChangedEvent(this.id, previousStatus, this.status));
    }

    public void ship() {
        validateStatus(OrderStatus.CONFIRMED, "배송");
        OrderStatus previousStatus = this.status;
        this.status = OrderStatus.SHIPPED;
        registerEvent(new OrderStatusChangedEvent(this.id, previousStatus, this.status));
    }

    public void cancel() {
        if (this.status == OrderStatus.SHIPPED) {
            throw new IllegalStateException("이미 배송된 주문은 취소할 수 없습니다.");
        }
        if (this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }
        OrderStatus previousStatus = this.status;
        this.status = OrderStatus.CANCELLED;
        registerEvent(new OrderStatusChangedEvent(this.id, previousStatus, this.status));
    }

    private void validateStatus(OrderStatus expected, String action) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    String.format("%s 상태의 주문만 %s 처리할 수 있습니다. 현재 상태: %s",
                            expected, action, this.status));
        }
    }
}
