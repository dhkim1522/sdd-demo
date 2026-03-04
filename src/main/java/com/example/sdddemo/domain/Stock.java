package com.example.sdddemo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    private int quantity;

    public Stock(String productName, int quantity) {
        this.productName = productName;
        this.quantity = quantity;
    }

    public void decrease(int amount) {
        if (this.quantity < amount) {
            throw new IllegalStateException(
                    String.format("재고가 부족합니다. 상품: %s, 현재 재고: %d, 요청 수량: %d",
                            this.productName, this.quantity, amount));
        }
        this.quantity -= amount;
    }
}
