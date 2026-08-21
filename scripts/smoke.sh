#!/usr/bin/env bash
# End-to-end smoke test against a locally running backend (http://localhost:8080).
# Registers a merchant, creates a high-value order, pays it through the step-up
# flow, submits fraud feedback and issues a partial refund.
set -euo pipefail

API="${PAYFLUX_API:-http://localhost:8080}"
EMAIL="smoke-$(date +%s)@payflux.test"
PASSWORD="Password123"

jqv() { python3 -c "import json,sys;print(json.load(sys.stdin)$1)"; }

echo "== register $EMAIL"
TOKEN=$(curl -sS -X POST "$API/api/auth/register" -H 'Content-Type: application/json' -d "{
  \"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"fullName\":\"Smoke Tester\",
  \"businessName\":\"Smoke Store\",\"legalName\":\"Smoke Store Pvt Ltd\",\"businessType\":\"RETAIL\",
  \"contactName\":\"Smoke Tester\",\"contactPhone\":\"9876543210\",\"panNumber\":\"ABCDE1234F\",
  \"gstin\":\"29ABCDE1234F1Z5\",\"addressLine1\":\"1 Market Road\",\"city\":\"Bengaluru\",
  \"state\":\"Karnataka\",\"postalCode\":\"560001\",\"country\":\"India\",
  \"bankAccountName\":\"Smoke Store\",\"bankAccountNumber\":\"1234567890\",\"bankIfsc\":\"HDFC0000123\"}" | jqv "['token']")

echo "== create order"
ORDER=$(curl -sS -X POST "$API/api/orders" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: smoke-$(date +%s)" \
  -d '{"amount":250000.00,"currency":"INR","receipt":"smoke-001","description":"Smoke test order",
       "customerName":"Riya Sharma","customerEmail":"riya@example.com","customerPhone":"9998887777"}')
ORDER_ID=$(echo "$ORDER" | jqv "['id']")
echo "order $ORDER_ID"

echo "== initiate payment"
PAY=$(curl -sS -X POST "$API/api/public/payments" -H 'Content-Type: application/json' \
  -d "{\"orderId\":\"$ORDER_ID\",\"method\":\"CARD\",\"cardNumber\":\"4111111111111111\",
       \"cardHolderName\":\"Riya Sharma\",\"cardExpiry\":\"12/29\",\"cardCvv\":\"123\",
       \"deviceFingerprint\":\"smoke-device\",\"simulateOutcome\":\"SUCCESS\"}")
PAYMENT_ID=$(echo "$PAY" | jqv "['id']")
echo "$PAY"

if [ "$(echo "$PAY" | jqv "['status']")" = "REQUIRES_VERIFICATION" ]; then
  echo "== verify OTP"
  curl -sS -X POST "$API/api/public/payments/$PAYMENT_ID/verify" -H 'Content-Type: application/json' \
    -d '{"otp":"123456"}'
  echo
fi

echo "== transaction detail"
DETAIL=$(curl -sS "$API/api/payments/$PAYMENT_ID" -H "Authorization: Bearer $TOKEN")
echo "$DETAIL" | python3 -m json.tool

ANALYSIS_ID=$(echo "$DETAIL" | jqv "['fraudAnalysis']['id']")
echo "== fraud feedback"
curl -sS -X POST "$API/api/fraud-alerts/$ANALYSIS_ID/feedback" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"feedback":"FALSE_POSITIVE","note":"Known customer"}'
echo

echo "== partial refund"
curl -sS -X POST "$API/api/payments/$PAYMENT_ID/refunds" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"amount":50000.00,"reason":"Partial cancellation"}'
echo
echo "== done"
