# Spring Data DDD Demo

`AbstractAggregateRoot`, `@DomainEvents`, `@AfterDomainEventPublication` 동작을 체험하는 데모 프로젝트입니다.

도메인 이벤트의 **두 가지 트랜잭션 패턴**을 시연합니다:

| 패턴 | 어노테이션 | 용도 | 트랜잭션 | 실패 시 |
|------|-----------|------|---------|--------|
| 패턴 1 | `@EventListener` | 재고 차감 | 같은 트랜잭션 | 주문도 롤백 (All or Nothing) |
| 패턴 2 | `@TransactionalEventListener(AFTER_COMMIT)` | 이력 저장 | 별도 트랜잭션 (`REQUIRES_NEW`) | 주문에 영향 없음 |

## 실행

```bash
./gradlew bootRun
```

## 데모 스크립트 실행

```bash
# 서버 실행 후
./gradlew bootRun

# 별도 터미널에서 데모 스크립트 실행
./demo-api.sh
```

### 스크립트 시연 흐름

| 단계 | API | 설명 | 이벤트 & 동작 |
|------|-----|------|--------------|
| 1 | `GET /api/stocks` | 초기 재고 확인 | - |
| 2 | `POST /api/orders` | 주문 생성 (MacBook Pro 1대) | `OrderCreatedEvent` → 재고 차감 |
| 3 | `GET /api/stocks` | 재고 차감 확인 (5→4) | - |
| 4 | `POST /api/orders` | 주문 생성 (iPad Air 2대) | `OrderCreatedEvent` → 재고 차감 |
| 5 | `POST /api/orders` | 주문 생성 실패 (MacBook Pro 100대) | 재고 부족 → 주문 롤백 |
| 6 | `POST /api/orders/{id}/confirm, ship` | 주문 확정 → 배송 | `OrderStatusChangedEvent` |
| 7 | `POST /api/orders/{id}/cancel` | 주문 취소 | `OrderStatusChangedEvent` |
| 8 | `GET /api/orders/{id}/histories` | 이력 조회 | 별도 트랜잭션으로 저장된 이력 확인 |
| 9 | `GET /api/orders` | 전체 주문 조회 | 롤백된 주문은 없음 |

## 이벤트 리스너 구조

```
OrderCreatedEvent
  ├─ OrderStockListener (@EventListener)           ← 같은 트랜잭션: 재고 차감
  └─ OrderHistoryListener (@TransactionalEventListener) ← 별도 트랜잭션: 이력 저장

OrderStatusChangedEvent
  └─ OrderHistoryListener (@TransactionalEventListener) ← 별도 트랜잭션: 이력 저장
```

### 패턴 1: 같은 트랜잭션 — 재고 차감

```java
@EventListener  // 같은 트랜잭션에서 실행
public void handleOrderCreated(OrderCreatedEvent event) {
    Stock stock = stockRepository.findByProductName(event.order().getProductName())...;
    stock.decrease(event.order().getQuantity());  // 재고 부족 시 예외 → 주문도 롤백
}
```

- `@EventListener`는 이벤트 발행자와 **같은 트랜잭션**에서 실행
- 재고 차감 실패 시 예외가 전파되어 주문 생성도 롤백 (All or Nothing)

### 패턴 2: 별도 트랜잭션 — 이력 저장

```java
@TransactionalEventListener  // 커밋 후 실행 (기본값: AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)  // 새 트랜잭션
public void handleOrderCreated(OrderCreatedEvent event) {
    orderHistoryRepository.save(OrderHistory.of(
        event.order().getId(), ...));  // 커밋 후이므로 ID 존재
}
```

- `@TransactionalEventListener`는 기본적으로 `AFTER_COMMIT` 시점에 실행
- `REQUIRES_NEW`로 새 트랜잭션을 열어 이력 저장
- 이력 저장 실패해도 이미 커밋된 주문에는 영향 없음

## OrderCreatedEvent의 orderId 문제 해결

`Order.create()` 시점에는 아직 DB에 저장되지 않아 ID가 없습니다.
이를 해결하기 위해 `OrderCreatedEvent`에 `Order` 객체 참조를 포함합니다.

```java
// Order.java
order.registerEvent(new OrderCreatedEvent(order));  // Order 참조 전달

// registerEvent() 후 save() 시점에 이벤트가 발행되므로,
// 리스너에서 event.order().getId() 호출 시 ID가 존재합니다.
```

## 개별 API 테스트

```bash
# 재고 조회
curl http://localhost:8081/api/stocks

# 주문 생성 (재고 차감)
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productName": "MacBook Pro", "quantity": 1, "price": 3000000}'

# 주문 확정
curl -X POST http://localhost:8081/api/orders/1/confirm

# 주문 배송
curl -X POST http://localhost:8081/api/orders/1/ship

# 주문 취소
curl -X POST http://localhost:8081/api/orders/1/cancel

# 이력 조회
curl http://localhost:8081/api/orders/1/histories

# 전체 주문 조회
curl http://localhost:8081/api/orders
```

## H2 Console

- URL: http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:mem:sdd-demo`
- Username: `sa`
- Password: (비워두기)

## 핵심 포인트

1. **Order** 엔티티가 `AbstractAggregateRoot<Order>`를 상속
2. 도메인 메서드(`create`, `confirm`, `ship`, `cancel`)에서 `registerEvent()` 호출
3. `repository.save()` 시점에 등록된 이벤트가 자동 발행
4. **OrderStockListener** (`@EventListener`) — 같은 트랜잭션에서 재고 차감
5. **OrderHistoryListener** (`@TransactionalEventListener`) — 별도 트랜잭션에서 이력 저장
