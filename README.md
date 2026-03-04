# Spring Data DDD Demo

`AbstractAggregateRoot`, `@DomainEvents`, `@AfterDomainEventPublication` 동작을 체험하는 데모 프로젝트입니다.

## 실행

```bash
./gradlew bootRun
```

## API 테스트

```bash
# 1. 주문 생성
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productName": "맥북 프로", "quantity": 1, "price": 3000000}'

# 2. 주문 확정
curl -X POST http://localhost:8081/api/orders/1/confirm

# 3. 주문 배송
curl -X POST http://localhost:8081/api/orders/1/ship

# 4. 이력 조회 (이벤트 기반 자동 생성 확인)
curl http://localhost:8081/api/orders/1/histories

# 5. 전체 주문 조회
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
4. `OrderEventListener`가 이벤트를 수신하여 `OrderHistory` 자동 생성
