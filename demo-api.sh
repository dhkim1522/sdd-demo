#!/bin/bash
#
# Spring Data DDD Demo - API 호출 스크립트
# 도메인 이벤트의 두 가지 트랜잭션 패턴을 시연합니다.
#   패턴 1: 같은 트랜잭션 (@EventListener) — 재고 차감
#   패턴 2: 별도 트랜잭션 (@TransactionalEventListener) — 이력 저장
#

BASE_URL="http://localhost:8081/api"

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

separator() {
  echo ""
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo ""
}

log_step() {
  echo -e "${GREEN}${BOLD}[$1]${NC} $2"
}

log_info() {
  echo -e "${YELLOW}>>>${NC} $1"
}

log_check() {
  echo -e "${CYAN}  ✔${NC} $1"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $1"
}

print_response() {
  echo -e "${BLUE}Response:${NC}"
  echo "$1" | python3 -m json.tool 2>/dev/null || echo "$1"
}

# JSON 값 추출 헬퍼
json_val() {
  echo "$1" | python3 -c "import sys,json; print(json.load(sys.stdin)$2)" 2>/dev/null
}

# 특정 상품의 재고 수량 추출
stock_qty() {
  echo "$1" | python3 -c "
import sys, json
stocks = json.load(sys.stdin)
for s in stocks:
    if s['productName'] == '$2':
        print(s['quantity'])
        break
" 2>/dev/null
}

# 서버 상태 확인
echo -e "${BOLD}Spring Data DDD Demo - 도메인 이벤트 트랜잭션 분리 시연${NC}"
separator

log_info "서버 상태 확인 중... (localhost:8081)"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/orders" 2>/dev/null)
if [ "$HTTP_CODE" = "000" ]; then
  log_error "서버가 실행 중이 아닙니다. './gradlew bootRun' 으로 먼저 서버를 시작해주세요."
  exit 1
fi
echo -e "${GREEN}서버 정상 동작 중${NC}"

# ============================================================
# STEP 1: 초기 재고 확인
# ============================================================
separator
log_step "STEP 1" "초기 재고 확인 - DataInitializer 로 등록된 재고"
log_info "GET $BASE_URL/stocks"

STOCKS=$(curl -s "$BASE_URL/stocks")
print_response "$STOCKS"

MACBOOK_STOCK=$(stock_qty "$STOCKS" "MacBook Pro")
IPAD_STOCK=$(stock_qty "$STOCKS" "iPad Air")
AIRPODS_STOCK=$(stock_qty "$STOCKS" "AirPods Pro")

log_check "MacBook Pro: ${MACBOOK_STOCK}개"
log_check "iPad Air: ${IPAD_STOCK}개"
log_check "AirPods Pro: ${AIRPODS_STOCK}개"

# ============================================================
# STEP 2: 주문 생성 (재고 차감 성공)
# ============================================================
separator
log_step "STEP 2" "주문 생성 - 같은 트랜잭션에서 재고 차감 (@EventListener)"
log_info "POST $BASE_URL/orders (MacBook Pro 1대)"

RESPONSE=$(curl -s -X POST "$BASE_URL/orders" \
  -H "Content-Type: application/json" \
  -d '{"productName": "MacBook Pro", "quantity": 1, "price": 3000000}')
print_response "$RESPONSE"

ORDER_ID=$(json_val "$RESPONSE" "['id']")
ORDER_STATUS=$(json_val "$RESPONSE" "['status']")
if [ -z "$ORDER_ID" ]; then
  log_error "주문 생성 실패. 응답을 확인해주세요."
  exit 1
fi
log_check "주문 ID: ${ORDER_ID}, 상태: ${ORDER_STATUS}"

# ============================================================
# STEP 3: 재고 차감 확인
# ============================================================
separator
log_step "STEP 3" "재고 차감 확인 - 같은 트랜잭션에서 재고가 차감되었는지 검증"
log_info "GET $BASE_URL/stocks"

STOCKS_AFTER=$(curl -s "$BASE_URL/stocks")
print_response "$STOCKS_AFTER"

MACBOOK_STOCK_AFTER=$(stock_qty "$STOCKS_AFTER" "MacBook Pro")
log_check "MacBook Pro 재고: ${MACBOOK_STOCK}개 → ${MACBOOK_STOCK_AFTER}개 (1개 차감)"

# ============================================================
# STEP 4: 두 번째 주문 생성 (취소 시연용)
# ============================================================
separator
log_step "STEP 4" "두 번째 주문 생성 (iPad Air 2대)"
log_info "POST $BASE_URL/orders"

RESPONSE2=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/orders" \
  -H "Content-Type: application/json" \
  -d '{"productName": "iPad Air", "quantity": 2, "price": 1200000}')
HTTP_BODY2=$(echo "$RESPONSE2" | python3 -c "import sys; lines=sys.stdin.read().strip().rsplit('\n',1); print(lines[0])" 2>/dev/null)
HTTP_STATUS2=$(echo "$RESPONSE2" | python3 -c "import sys; lines=sys.stdin.read().strip().rsplit('\n',1); print(lines[-1])" 2>/dev/null)

if [ "$HTTP_STATUS2" = "201" ] || [ "$HTTP_STATUS2" = "200" ]; then
  print_response "$HTTP_BODY2"
  ORDER_ID2=$(json_val "$HTTP_BODY2" "['id']")
  ORDER_STATUS2=$(json_val "$HTTP_BODY2" "['status']")
  log_check "주문 ID: ${ORDER_ID2}, 상태: ${ORDER_STATUS2}"

  STOCKS_AFTER2=$(curl -s "$BASE_URL/stocks")
  IPAD_STOCK_AFTER=$(stock_qty "$STOCKS_AFTER2" "iPad Air")
  log_check "iPad Air 재고: ${IPAD_STOCK}개 → ${IPAD_STOCK_AFTER}개 (2개 차감)"
else
  print_response "$HTTP_BODY2"
  log_error "iPad Air 주문 실패 (HTTP ${HTTP_STATUS2}) - 재고 부족으로 롤백됨"
  ORDER_ID2=""
fi

# ============================================================
# STEP 5: 재고 부족 시 주문 실패 시연
# ============================================================
separator
log_step "STEP 5" "재고 부족 시 주문 실패 - 같은 트랜잭션이므로 주문도 롤백 (All or Nothing)"
log_info "POST $BASE_URL/orders (MacBook Pro 100대 — 재고 부족!)"

RESPONSE5=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/orders" \
  -H "Content-Type: application/json" \
  -d '{"productName": "MacBook Pro", "quantity": 100, "price": 3000000}')
HTTP_BODY5=$(echo "$RESPONSE5" | python3 -c "import sys; lines=sys.stdin.read().strip().rsplit('\n',1); print(lines[0])" 2>/dev/null)
HTTP_STATUS5=$(echo "$RESPONSE5" | python3 -c "import sys; lines=sys.stdin.read().strip().rsplit('\n',1); print(lines[-1])" 2>/dev/null)

echo -e "${RED}HTTP Status: $HTTP_STATUS5 (주문 실패 — 재고 부족으로 전체 롤백)${NC}"
print_response "$HTTP_BODY5"

log_check "재고 부족 예외 발생 → @EventListener 가 같은 트랜잭션이므로 주문 생성도 롤백됨"

# 롤백 후 재고 변동 없음 확인
STOCKS_AFTER5=$(curl -s "$BASE_URL/stocks")
MACBOOK_STOCK_AFTER5=$(stock_qty "$STOCKS_AFTER5" "MacBook Pro")
log_check "MacBook Pro 재고 변동 없음: ${MACBOOK_STOCK_AFTER}개 → ${MACBOOK_STOCK_AFTER5}개 (롤백)"

ORDERS_COUNT=$(curl -s "$BASE_URL/orders" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null)
log_check "전체 주문 수: ${ORDERS_COUNT}건 (실패한 주문은 목록에 없음)"

# ============================================================
# STEP 6: 주문 확정 → 배송
# ============================================================
separator
log_step "STEP 6" "주문 확정 → 배송 (상태 변경 이벤트 발행)"
log_info "POST $BASE_URL/orders/$ORDER_ID/confirm"

RESPONSE=$(curl -s -X POST "$BASE_URL/orders/$ORDER_ID/confirm")
print_response "$RESPONSE"

CONFIRM_STATUS=$(json_val "$RESPONSE" "['status']")
log_check "주문 #${ORDER_ID} 상태: CREATED → ${CONFIRM_STATUS}"

echo ""
log_info "POST $BASE_URL/orders/$ORDER_ID/ship"

RESPONSE=$(curl -s -X POST "$BASE_URL/orders/$ORDER_ID/ship")
print_response "$RESPONSE"

SHIP_STATUS=$(json_val "$RESPONSE" "['status']")
log_check "주문 #${ORDER_ID} 상태: CONFIRMED → ${SHIP_STATUS}"

# ============================================================
# STEP 7: 주문 취소 (두 번째 주문)
# ============================================================
separator
if [ -n "$ORDER_ID2" ]; then
  log_step "STEP 7" "주문 취소 - Order.cancel() → OrderStatusChangedEvent 발행"
  log_info "POST $BASE_URL/orders/$ORDER_ID2/cancel"

  RESPONSE=$(curl -s -X POST "$BASE_URL/orders/$ORDER_ID2/cancel")
  print_response "$RESPONSE"

  CANCEL_STATUS=$(json_val "$RESPONSE" "['status']")
  log_check "주문 #${ORDER_ID2} 상태: CREATED → ${CANCEL_STATUS}"
else
  log_step "STEP 7" "주문 취소 - SKIP (STEP 4에서 주문 생성 실패)"
  log_info "iPad Air 재고 부족으로 두 번째 주문이 생성되지 않아 취소 시연 생략"
fi

# ============================================================
# STEP 8: 이력 조회 (별도 트랜잭션으로 저장된 이력 확인)
# ============================================================
separator
log_step "STEP 8" "주문 #$ORDER_ID 이력 조회 - @TransactionalEventListener(AFTER_COMMIT) 로 저장된 이력"
log_info "GET $BASE_URL/orders/$ORDER_ID/histories"

HISTORIES=$(curl -s "$BASE_URL/orders/$ORDER_ID/histories")
print_response "$HISTORIES"

HISTORY_COUNT=$(echo "$HISTORIES" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null)
log_check "이력 ${HISTORY_COUNT}건 저장됨 (별도 트랜잭션에서 저장)"

# 각 이력 요약 출력
echo "$HISTORIES" | python3 -c "
import sys, json
histories = json.load(sys.stdin)
for i, h in enumerate(histories, 1):
    print(f'  {i}. [{h[\"eventType\"]}] {h[\"description\"]}')
" 2>/dev/null

# ============================================================
# STEP 9: 전체 주문 조회
# ============================================================
separator
log_step "STEP 9" "전체 주문 목록 조회"
log_info "GET $BASE_URL/orders"

ORDERS=$(curl -s "$BASE_URL/orders")
print_response "$ORDERS"

# 각 주문 요약 출력
echo "$ORDERS" | python3 -c "
import sys, json
orders = json.load(sys.stdin)
print()
for o in orders:
    print(f'  - 주문 #{o[\"id\"]}: {o[\"productName\"]} {o[\"quantity\"]}개 → {o[\"status\"]}')
" 2>/dev/null

TOTAL_ORDERS=$(echo "$ORDERS" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null)
log_check "총 ${TOTAL_ORDERS}건 (재고 부족 주문은 롤백되어 없음)"

# ============================================================
# 최종 재고 확인
# ============================================================
separator
log_step "FINAL" "최종 재고 현황"
log_info "GET $BASE_URL/stocks"

STOCKS_FINAL=$(curl -s "$BASE_URL/stocks")
print_response "$STOCKS_FINAL"

echo ""
echo -e "${CYAN}재고 변동 요약:${NC}"
MACBOOK_FINAL=$(stock_qty "$STOCKS_FINAL" "MacBook Pro")
IPAD_FINAL=$(stock_qty "$STOCKS_FINAL" "iPad Air")
AIRPODS_FINAL=$(stock_qty "$STOCKS_FINAL" "AirPods Pro")
log_check "MacBook Pro: ${MACBOOK_STOCK}개 → ${MACBOOK_FINAL}개"
log_check "iPad Air:    ${IPAD_STOCK}개 → ${IPAD_FINAL}개"
log_check "AirPods Pro: ${AIRPODS_STOCK}개 → ${AIRPODS_FINAL}개 (주문 없음, 변동 없음)"

# ============================================================
# 결과 요약
# ============================================================
separator
echo -e "${BOLD}시연 완료!${NC}"
echo ""
echo -e "${GREEN}도메인 이벤트 트랜잭션 패턴 요약:${NC}"
echo ""
echo -e "  ${BOLD}패턴 1: 같은 트랜잭션 (@EventListener)${NC}"
echo "    - OrderCreatedEvent → 재고 차감 (OrderStockListener)"
echo "    - 재고 부족 시 예외 → 주문도 함께 롤백 (All or Nothing)"
echo ""
echo -e "  ${BOLD}패턴 2: 별도 트랜잭션 (@TransactionalEventListener + REQUIRES_NEW)${NC}"
echo "    - OrderCreatedEvent, OrderStatusChangedEvent → 이력 저장 (OrderHistoryListener)"
echo "    - 커밋 후 새 트랜잭션에서 실행, 실패해도 주문에 영향 없음"
echo ""
echo -e "${YELLOW}H2 Console:${NC} http://localhost:8081/h2-console"
echo -e "  JDBC URL: jdbc:h2:mem:sdd-demo | Username: sa | Password: (비워두기)"
separator
