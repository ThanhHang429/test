#!/usr/bin/env bash
# =============================================================================
# ETicket backend — smoke test toàn bộ API của module Cảnh
# (FR-U03, U04, U05, U06, U07, U20, O03, O06, O09, O27, A07, A09, A14, A20)
#
# Cách dùng:
#   backend/scripts/api-smoke.sh                      # mặc định http://localhost:8080
#   BASE=http://localhost:8080 backend/scripts/api-smoke.sh
#
# Yêu cầu: backend chạy profile dev (app.seed.enabled=true, app.security.dev-header-enabled=true),
#          có curl + jq. Script chỉ ĐỌC và tạo dữ liệu nháp, không xoá dữ liệu có sẵn.
# =============================================================================
set -uo pipefail

BASE="${BASE:-http://localhost:8080}"
API="$BASE/api/v1"

PASS=0
FAIL=0
SKIP=0
FAILED_CASES=()

green() { printf '\033[32m%s\033[0m' "$1"; }
red()   { printf '\033[31m%s\033[0m' "$1"; }
yellow(){ printf '\033[33m%s\033[0m' "$1"; }
grey()  { printf '\033[90m%s\033[0m' "$1"; }

# call METHOD PATH USER_ID [BODY] [EXTRA_HEADER...]
# In ra: "<http_status>\n<body>"
call() {
  local method="$1" path="$2" user="${3:-}" body="${4:-}"
  shift 4 2>/dev/null || shift $#
  local args=(-s -o /tmp/smoke_body.json -w '%{http_code}' -X "$method" "$API$path")
  [[ -n "$user" ]] && args+=(-H "X-User-Id: $user")
  [[ -n "$body" ]] && args+=(-H "Content-Type: application/json" -d "$body")
  for h in "$@"; do args+=(-H "$h"); done
  local status
  status=$(curl "${args[@]}")
  echo "$status"
}

body() { cat /tmp/smoke_body.json 2>/dev/null; }
jqv()  { jq -r "$1" < /tmp/smoke_body.json 2>/dev/null; }

expect() {
  local name="$1" expected="$2" actual="$3"
  if [[ "$expected" == "$actual" ]]; then
    PASS=$((PASS+1)); printf '%s %s %s\n' "$(green '✓')" "$name" "$(grey "(HTTP $actual)")"
  else
    FAIL=$((FAIL+1)); FAILED_CASES+=("$name (mong đợi $expected, nhận $actual)")
    printf '%s %s %s\n' "$(red '✗')" "$name" "$(red "(mong đợi $expected, nhận $actual)")"
    grey "  → $(body | head -c 300)"; echo
  fi
}

expect_one_of() {
  local name="$1" expected="$2" actual="$3"
  if [[ " $expected " == *" $actual "* ]]; then
    PASS=$((PASS+1)); printf '%s %s %s\n' "$(green '✓')" "$name" "$(grey "(HTTP $actual)")"
  else
    FAIL=$((FAIL+1)); FAILED_CASES+=("$name (mong đợi một trong [$expected], nhận $actual)")
    printf '%s %s %s\n' "$(red '✗')" "$name" "$(red "(mong đợi [$expected], nhận $actual)")"
    grey "  → $(body | head -c 300)"; echo
  fi
}

skip() {
  SKIP=$((SKIP+1)); printf '%s %s %s\n' "$(yellow '•')" "$1" "$(grey "(bỏ qua: $2)")"
}

section() { printf '\n\033[1m%s\033[0m\n' "$1"; }

# ---------------------------------------------------------------------------
section "0. Kết nối & dữ liệu mẫu"
# ---------------------------------------------------------------------------
st=$(call GET "/categories")
expect "GET /categories (công khai)" 200 "$st"

st=$(call GET "/dev/accounts")
expect "GET /dev/accounts (dev helper)" 200 "$st"

ADMIN=$(jq -r '.data[] | select(.roles|index("ADMIN")) | .userId' < /tmp/smoke_body.json)
ORG1=$(jq -r '.data[] | select(.email|startswith("organizer1")) | .userId' < /tmp/smoke_body.json)
ORG2=$(jq -r '.data[] | select(.email|startswith("organizer2")) | .userId' < /tmp/smoke_body.json)
U1=$(jq -r '.data[] | select(.email|startswith("user1")) | .userId' < /tmp/smoke_body.json)
U2=$(jq -r '.data[] | select(.email|startswith("user2")) | .userId' < /tmp/smoke_body.json)
U3=$(jq -r '.data[] | select(.email|startswith("user3")) | .userId' < /tmp/smoke_body.json)

if [[ -z "$ADMIN" || -z "$ORG1" || -z "$U1" ]]; then
  echo "$(red 'Không đọc được danh sách tài khoản dev. Kiểm tra profile dev và seeder.')"
  exit 1
fi
echo "$(grey "  admin=$ADMIN organizer1=$ORG1 organizer2=$ORG2 user1=$U1 user2=$U2 user3=$U3")"

# ---------------------------------------------------------------------------
section "1. FR-U04 / U05 — khám phá, tìm kiếm, chi tiết"
# ---------------------------------------------------------------------------
st=$(call GET "/events?size=3")
expect "GET /events (phân trang)" 200 "$st"
EVENT_ID=$(jqv '.data[0].id')
echo "$(grey "  sự kiện dùng để test: $EVENT_ID")"

st=$(call GET "/events?size=200")
[[ "$(jqv '.meta.size')" == "50" ]] && expect "size bị chặn về 50" 50 50 || expect "size bị chặn về 50" 50 "$(jqv '.meta.size')"

st=$(call GET "/events?q=hoi%20nhac")
expect "tìm không dấu \"hoi nhac\"" 200 "$st"
echo "$(grey "  kết quả: $(jqv '.meta.totalItems')")"

st=$(call GET "/events?priceMin=100&priceMax=10")
expect "khoảng giá sai → 400" 400 "$st"

st=$(call GET "/events/$EVENT_ID")
expect "GET /events/{id}" 200 "$st"

st=$(call GET "/events/999999")
expect "sự kiện không tồn tại → 404" 404 "$st"

# ---------------------------------------------------------------------------
section "2. FR-U03 — sở thích"
# ---------------------------------------------------------------------------
st=$(call GET "/me/preferences" "$U1")
expect "GET /me/preferences" 200 "$st"

st=$(call PUT "/me/preferences" "$U1" '{"categoryIds":[9],"eventFormat":"ANY","priceMode":"ANY","maxBudgetVnd":500000,"preferredCities":["Hồ Chí Minh"]}')
expect "PUT /me/preferences" 200 "$st"
VERSION=$(jqv '.data.version')

st=$(call PUT "/me/preferences" "$U1" "{\"categoryIds\":[9],\"eventFormat\":\"ANY\",\"priceMode\":\"ANY\",\"maxBudgetVnd\":500000,\"preferredCities\":[\"Hồ Chí Minh\"],\"version\":$VERSION}")
expect "PUT lặp cùng nội dung (idempotent)" 200 "$st"

st=$(call PUT "/me/preferences" "$U1" '{"categoryIds":[9],"eventFormat":"ANY","priceMode":"ANY","version":1}')
expect "version cũ → 409" 409 "$st"

st=$(call PUT "/me/preferences" "$U1" '{"categoryIds":[9,10,11,12,13,14,15,16,17,18,19],"eventFormat":"ANY","priceMode":"ANY"}')
expect "quá 10 danh mục → 400" 400 "$st"

# ---------------------------------------------------------------------------
section "3. FR-U06 — yêu thích"
# ---------------------------------------------------------------------------
st=$(call PUT "/me/favorites/$EVENT_ID" "$U1")
expect "PUT /me/favorites/{id} (lần 1)" 200 "$st"
st=$(call PUT "/me/favorites/$EVENT_ID" "$U1")
expect "PUT /me/favorites/{id} (lần 2, idempotent)" 200 "$st"

st=$(call GET "/me/favorites?filter=ALL" "$U1")
expect "GET /me/favorites" 200 "$st"

st=$(call DELETE "/me/favorites/$EVENT_ID" "$U1")
expect "DELETE /me/favorites/{id}" 200 "$st"

st=$(call GET "/me/favorites?filter=SAI" "$U1")
expect "filter sai → 400" 400 "$st"

st=$(call GET "/me/favorites")
expect "chưa đăng nhập → 401" 401 "$st"

# ---------------------------------------------------------------------------
section "4. FR-U20 — đánh giá"
# ---------------------------------------------------------------------------
PAST_ID=$(call GET "/events?includePast=true&size=50" >/dev/null; jq -r '[.data[] | select(.endAt < (now|todate))][0].id // empty' < /tmp/smoke_body.json)
if [[ -z "$PAST_ID" ]]; then
  skip "đánh giá sự kiện đã kết thúc" "không có sự kiện COMPLETED trong dữ liệu mẫu"
else
  echo "$(grey "  sự kiện đã kết thúc: $PAST_ID")"

  st=$(call GET "/events/$PAST_ID/reviews")
  expect "GET /events/{id}/reviews" 200 "$st"

  st=$(call GET "/events/$PAST_ID/reviews/summary")
  expect "GET /events/{id}/reviews/summary" 200 "$st"

  st=$(call POST "/events/$PAST_ID/reviews" "$U3" '{"rating":5,"text":"Smoke test: sự kiện rất hay."}' "Idempotency-Key: smoke-$PAST_ID-$U3")
  expect_one_of "POST đánh giá (200 lần đầu / 409 nếu đã có)" "200 409" "$st"

  st=$(call POST "/events/$PAST_ID/reviews" "$U3" '{"rating":5,"text":"Smoke test: sự kiện rất hay."}')
  expect "đánh giá trùng → 409" 409 "$st"

  # Chạy lại script trong 7 ngày với cùng user sẽ hết lượt sửa (tối đa 3 lần) → 409 REVIEW_EDIT_LIMIT.
  st=$(call PUT "/events/$PAST_ID/reviews/me" "$U3" '{"rating":4,"text":"Smoke test: cập nhật đánh giá."}')
  if [[ "$st" == "409" ]]; then
    expect "PUT /events/{id}/reviews/me (đã hết lượt sửa)" REVIEW_EDIT_LIMIT "$(jqv '.error.code')"
  else
    expect "PUT /events/{id}/reviews/me" 200 "$st"
  fi

  st=$(call GET "/me/reviews" "$U3")
  expect "GET /me/reviews" 200 "$st"

  st=$(call POST "/events/$EVENT_ID/reviews" "$U1" '{"rating":5}')
  expect "đánh giá sự kiện chưa kết thúc → 403" 403 "$st"
fi

st=$(call POST "/reviews/1/report" "$U2" '{"reasonCode":"SPAM","note":"Smoke test"}')
expect_one_of "POST /reviews/{id}/report" "200 409 404" "$st"

st=$(call POST "/reviews/1/report" "$U2" '{"reasonCode":"SAI"}')
expect "mã lý do sai → 400" 400 "$st"

# ---------------------------------------------------------------------------
section "5. FR-A14 / A07 — kiểm duyệt (ADMIN)"
# ---------------------------------------------------------------------------
st=$(call GET "/admin/reviews" "$ADMIN")
expect "GET /admin/reviews" 200 "$st"

st=$(call GET "/admin/reviews" "$U1")
expect "không phải Admin → 403" 403 "$st"

st=$(call GET "/admin/moderation/flags?source=ALL" "$ADMIN")
expect "GET /admin/moderation/flags" 200 "$st"
FLAG_ID=$(jqv '.data[0].id')

st=$(call POST "/admin/moderation/rescan" "$ADMIN" '{"targetType":"REVIEW"}')
expect "POST /admin/moderation/rescan" 200 "$st"

if [[ -n "$FLAG_ID" && "$FLAG_ID" != "null" ]]; then
  st=$(call POST "/admin/moderation/flags/$FLAG_ID/dismiss" "$ADMIN" '{"reasonCode":"OTHER","note":"Smoke test"}')
  expect "POST /admin/moderation/flags/{id}/dismiss" 200 "$st"
else
  skip "xử lý cờ kiểm duyệt" "không có cờ nào đang mở"
fi

# ---------------------------------------------------------------------------
section "6. FR-U07 — gợi ý AI"
# ---------------------------------------------------------------------------
st=$(call GET "/recommendations?limit=5" "$U1")
expect "GET /recommendations" 200 "$st"
echo "$(grey "  isFallback=$(jqv '.data.meta.isFallback') personalized=$(jqv '.data.meta.personalized') source=$(jqv '[.data.items[].reasonSource]|unique|join(",")')")"

st=$(call POST "/recommendations/preview" "" '{"categoryIds":[9],"city":"Hà Nội","limit":5}')
expect "POST /recommendations/preview (khách)" 200 "$st"

st=$(call POST "/recommendations/preview" "" '{"priceMode":"SAI"}')
expect "preview priceMode sai → 400" 400 "$st"

REC_EVENT=$(call GET "/recommendations?limit=10" "$U1" >/dev/null; jqv '.data.items[0].event.id')
if [[ -n "$REC_EVENT" && "$REC_EVENT" != "null" ]]; then
  st=$(call POST "/recommendations/$REC_EVENT/dismiss" "$U1")
  expect "POST /recommendations/{id}/dismiss" 200 "$st"
else
  skip "«Không quan tâm»" "không có gợi ý nào"
fi

# ---------------------------------------------------------------------------
section "7. FR-O06 / O27 — AI cho Organizer"
# ---------------------------------------------------------------------------
st=$(call POST "/organizer/ai/event-description" "$ORG1" '{"title":"Smoke test sự kiện âm nhạc","categoryId":9,"format":"OFFLINE","startTime":"20:00 20/12/2026","location":"Nhà hát Thành phố","keyPoints":["Dàn nhạc giao hưởng","Khách mời quốc tế","Không gian ngoài trời"],"tone":"FRIENDLY","length":"MEDIUM","variants":2}')
# 429 là hành vi đúng khi đã dùng hết quota 10 lượt/giờ (chạy lại script trong cùng giờ).
expect_one_of "POST /organizer/ai/event-description" "200 429" "$st"
echo "$(grey "  source=$(jqv '.data.source') variants=$(jqv '.data.descriptions|length')")"

st=$(call POST "/organizer/ai/event-description" "$ORG1" '{"title":"Thiếu ý chính","categoryId":9,"keyPoints":["một"]}')
expect "thiếu ý chính → 400" 400 "$st"

st=$(call POST "/organizer/ai/event-description" "$ORG2" '{"title":"Organizer chưa duyệt","categoryId":9,"keyPoints":["một","hai","ba"]}')
expect "Organizer PENDING → 403" 403 "$st"

OWN_EVENT=$(call GET "/organizer/dashboard" "$ORG1" >/dev/null; jqv '.data.topEvents[0].eventId')
if [[ -n "$OWN_EVENT" && "$OWN_EVENT" != "null" ]]; then
  st=$(call POST "/organizer/events/$OWN_EVENT/ai-analysis" "$ORG1")
  expect "POST /organizer/events/{id}/ai-analysis" 200 "$st"
  echo "$(grey "  source=$(jqv '.data.source') insufficientData=$(jqv '.data.insufficientData')")"

  st=$(call POST "/organizer/events/$OWN_EVENT/ai-analysis" "$ORG2")
  expect "phân tích sự kiện người khác → 403" 403 "$st"
fi

# ---------------------------------------------------------------------------
section "8. FR-O09 — phân loại & nhãn"
# ---------------------------------------------------------------------------
st=$(call GET "/tags/suggest?q=ac" "$ORG1")
expect "GET /tags/suggest" 200 "$st"

if [[ -n "$OWN_EVENT" && "$OWN_EVENT" != "null" ]]; then
  st=$(call PUT "/organizer/events/$OWN_EVENT/classification" "$ORG1" '{"primaryCategoryId":9,"secondaryCategoryIds":[],"tags":["smoke-test"]}')
  expect "PUT /organizer/events/{id}/classification" 200 "$st"

  st=$(call PUT "/organizer/events/$OWN_EVENT/classification" "$ORG1" '{"primaryCategoryId":9,"tags":["cá độ"]}')
  expect "nhãn chứa từ khoá cấm → 400" 400 "$st"

  st=$(call PUT "/organizer/events/$OWN_EVENT/classification" "$ORG2" '{"primaryCategoryId":9}')
  expect "phân loại sự kiện người khác → 403" 403 "$st"
fi

# ---------------------------------------------------------------------------
section "9. FR-O03 — dashboard"
# ---------------------------------------------------------------------------
st=$(call GET "/organizer/dashboard?range=30d" "$ORG1")
expect "GET /organizer/dashboard?range=30d" 200 "$st"

st=$(call GET "/organizer/dashboard?range=7d" "$ORG1")
expect "GET /organizer/dashboard?range=7d" 200 "$st"

st=$(call GET "/organizer/dashboard?range=90d" "$ORG1")
expect "range không hỗ trợ → 400" 400 "$st"

st=$(call GET "/organizer/dashboard?range=custom&from=2026-01-01&to=2025-01-01" "$ORG1")
expect "custom đảo ngày → 400" 400 "$st"

# ---------------------------------------------------------------------------
section "10. FR-A20 — giám sát tích hợp"
# ---------------------------------------------------------------------------
st=$(call GET "/admin/integrations" "$ADMIN")
expect "GET /admin/integrations" 200 "$st"

st=$(call GET "/admin/integrations/ai/requests?range=24h" "$ADMIN")
expect "GET /admin/integrations/ai/requests" 200 "$st"
echo "$(grey "  total=$(jqv '.data.totalRequests') successRate=$(jqv '.data.successRate') circuit=$(jqv '.data.circuitState')")"

st=$(call GET "/admin/integrations/ai/requests/khong-ton-tai" "$ADMIN")
expect "correlationId lạ → 404" 404 "$st"

st=$(call PUT "/admin/integrations/ai/features/RECOMMEND" "$ADMIN" '{"enabled":false,"reason":"Smoke test tắt khẩn"}')
expect "PUT tắt khẩn RECOMMEND" 200 "$st"

st=$(call GET "/recommendations?refresh=true" "$U1")
expect "gợi ý vẫn chạy khi AI tắt (fallback)" 200 "$st"
echo "$(grey "  isFallback=$(jqv '.data.meta.isFallback')")"

st=$(call PUT "/admin/integrations/ai/features/RECOMMEND" "$ADMIN" '{"enabled":true,"reason":"Smoke test bật lại"}')
expect "PUT bật lại RECOMMEND" 200 "$st"

st=$(call PUT "/admin/integrations/ai/features/RECOMMEND" "$ADMIN" '{"enabled":true}')
expect "thiếu lý do → 400" 400 "$st"

# ---------------------------------------------------------------------------
section "11. FR-A09 — quản lý danh mục"
# ---------------------------------------------------------------------------
st=$(call GET "/admin/categories" "$ADMIN")
expect "GET /admin/categories" 200 "$st"

NEW_CODE="SMOKE$(date +%H%M%S)"
# Tên cũng phải duy nhất theo lượt chạy vì slug sinh từ tên (slug trùng → 409).
st=$(call POST "/admin/categories" "$ADMIN" "{\"categoryCode\":\"$NEW_CODE\",\"categoryName\":\"Danh mục smoke $NEW_CODE\"}")
expect "POST /admin/categories" 200 "$st"
NEW_CAT=$(jqv '.data.id')

st=$(call POST "/admin/categories" "$ADMIN" "{\"categoryCode\":\"$NEW_CODE\",\"categoryName\":\"Trùng mã\"}")
expect "trùng mã danh mục → 409" 409 "$st"

if [[ -n "$NEW_CAT" && "$NEW_CAT" != "null" ]]; then
  st=$(call POST "/admin/categories/$NEW_CAT/hide" "$ADMIN" '{"reason":"Smoke test ẩn"}')
  expect "POST /admin/categories/{id}/hide" 200 "$st"

  st=$(call POST "/admin/categories/$NEW_CAT/unhide" "$ADMIN")
  expect "POST /admin/categories/{id}/unhide" 200 "$st"

  st=$(call POST "/admin/categories/$NEW_CAT/hide" "$ADMIN" '{}')
  expect "ẩn không lý do → 400" 400 "$st"

  # Dọn dẹp: ẩn danh mục vừa tạo để số danh mục ACTIVE không tăng sau mỗi lượt chạy (giới hạn 50).
  st=$(call POST "/admin/categories/$NEW_CAT/hide" "$ADMIN" '{"reason":"Dọn dẹp smoke test"}')
  expect "dọn dẹp: ẩn danh mục smoke" 200 "$st"
fi

st=$(call PUT "/admin/categories/order" "$ADMIN" '{"items":[{"categoryId":9,"sortOrder":1}]}')
expect "PUT /admin/categories/order" 200 "$st"

st=$(call GET "/admin/categories" "$U1")
expect "quản lý danh mục khi không phải Admin → 403" 403 "$st"

# ---------------------------------------------------------------------------
section "12. Hạ tầng chung"
# ---------------------------------------------------------------------------
st=$(curl -s -o /tmp/smoke_body.json -w '%{http_code}' -H 'X-Correlation-Id: smoke-fixed-id' "$API/categories")
expect "X-Correlation-Id được echo lại" 200 "$st"
CORR=$(curl -s -D - -o /dev/null -H 'X-Correlation-Id: smoke-fixed-id' "$API/categories" | tr -d '\r' | awk -F': ' 'tolower($1)=="x-correlation-id"{print $2}')
expect "header phản hồi chứa correlationId" smoke-fixed-id "$CORR"

# Khách chưa đăng nhập: Spring Security chặn ở anyRequest().authenticated() trước khi routing.
st=$(call GET "/khong-ton-tai")
expect "route không tồn tại (khách) → 401" 401 "$st"

# Đã đăng nhập: đi qua security, không có handler → 404.
st=$(call GET "/khong-ton-tai" "$U1")
expect "route không tồn tại (đã đăng nhập) → 404" 404 "$st"

echo
echo "─────────────────────────────────────────────"
printf 'Kết quả: %s đạt, %s lỗi, %s bỏ qua\n' "$(green "$PASS")" "$(red "$FAIL")" "$(yellow "$SKIP")"
if (( FAIL > 0 )); then
  echo "$(red 'Các ca lỗi:')"
  for c in "${FAILED_CASES[@]}"; do echo "  - $c"; done
  exit 1
fi
echo "$(green 'Tất cả kiểm thử đều đạt.')"
