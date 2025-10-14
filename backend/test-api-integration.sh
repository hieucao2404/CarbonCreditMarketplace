#!/usr/bin/env bash
# Integration test script for CarbonCredit / CreditListing / JourneyData endpoints
# Usage: run from project backend folder: ./test-api-integration.sh

BASE_URL="${BASE_URL:-http://localhost:8080}"
# default basic auth header (change to your test user). Example admin:admin123
AUTH_HEADER="${AUTH_HEADER:-Authorization: Basic YWRtaW46YWRtaW4xMjM=}"
# Optional verifier credentials for verify endpoints (CVA)
VERIFIER_AUTH_HEADER="${VERIFIER_AUTH_HEADER:-Authorization: Basic dmVyaWZpZXI6cGFzcw==}"

# tools
CURL_BIN="$(command -v curl || true)"
JQ_BIN="$(command -v jq || true)"

if [ -z "$CURL_BIN" ]; then
  echo "curl is required. Install curl and re-run."
  exit 1
fi

echo "🚀 CarbonCredit / CreditListing / JourneyData integration tests"
echo "Base URL: $BASE_URL"
echo

check_server() {
  printf "🔍 Checking server health at %s/actuator/health ... " "$BASE_URL"
  if curl -s --connect-timeout 5 "$BASE_URL/actuator/health" | grep -qi '"status"\s*:\s*"UP"'; then
    echo "UP ✅"
  else
    echo "DOWN ❌"
    echo "Start server: mvn -f backend spring-boot:run  (or ./mvnw spring-boot:run)"
    exit 1
  fi
  echo
}

print_result() {
  local name="$1"; local status="$2"; local body="$3"; local time="$4"
  echo "Test: $name"
  echo "  Status: $status  Time: ${time}s"
  if [[ "$status" =~ ^2 ]]; then
    echo "  ✅ OK"
    if [ -n "$body" ]; then
      if [ ${#body} -lt 400 ]; then echo "  Body: $body"; else echo "  Body: ${body:0:300}..."; fi
    fi
  else
    echo "  ❌ Failed"
    echo "  Body: $body"
  fi
  echo
}

call_endpoint() {
  local method="$1"; local path="$2"; local auth="$3"; local data="$4"; local params="$5"
  local url="$BASE_URL$path"
  local headers=(-H "Content-Type: application/json")
  if [ -n "$auth" ]; then headers+=(-H "$auth"); fi
  if [ -n "$data" ]; then
    resp=$($CURL_BIN -s -w "\n__STATUS__%{http_code}\n__TIME__%{time_total}" -X "$method" "$url" "${headers[@]}" -d "$data" $params)
  else
    resp=$($CURL_BIN -s -w "\n__STATUS__%{http_code}\n__TIME__%{time_total}" -X "$method" "$url" "${headers[@]}" $params)
  fi
  status=$(echo "$resp" | sed -n 's/.*__STATUS__\([0-9][0-9][0-9]\).*/\1/p')
  time=$(echo "$resp" | sed -n 's/.*__TIME__\([0-9.]\+\).*/\1/p')
  body=$(echo "$resp" | sed '/__STATUS__/,$d')
  echo "$status|$time|$body"
}

extract_first_id() {
  local path="$1"; local auth="$2"
  local result
  result=$(call_endpoint GET "$path" "$auth" "" "")
  local status=$(echo "$result" | cut -d'|' -f1)
  local body=$(echo "$result" | cut -d'|' -f3-)
  if [ "$status" != "200" ]; then
    echo ""
    return
  fi
  if [ -n "$JQ_BIN" ]; then
    echo "$body" | jq -r '.[0].id // .[0].ID // .[0].uuid // empty'
  else
    # fallback: try to find first uuid-like string
    echo "$body" | grep -Eo '[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}' | head -n1 || true
  fi
}

# Start tests
check_server

# 1) List carbon credits
res=$(call_endpoint GET "/carbon-credits" "$AUTH_HEADER")
status=$(echo "$res" | cut -d'|' -f1); time=$(echo "$res" | cut -d'|' -f2); body=$(echo "$res" | cut -d'|' -f3-)
print_result "List Carbon Credits (GET /carbon-credits)" "$status" "$body" "$time"

# 2) List pending credits and extract one id
res_p=$(call_endpoint GET "/carbon-credits/pending" "$AUTH_HEADER")
status_p=$(echo "$res_p" | cut -d'|' -f1); body_p=$(echo "$res_p" | cut -d'|' -f3-)
pending_id=$(extract_first_id "/carbon-credits/pending" "$AUTH_HEADER")
if [ -n "$pending_id" ]; then
  echo "Found pending credit id: $pending_id"
else
  echo "No pending credits found (skipping verify test)"
fi
echo

# 3) Try verify (only if pending id found)
if [ -n "$pending_id" ]; then
  verify_json='{"comments":"integration test verify"}'
  resv=$(call_endpoint POST "/carbon-credits/${pending_id}/verify" "$VERIFIER_AUTH_HEADER" "$verify_json")
  statusv=$(echo "$resv" | cut -d'|' -f1); timev=$(echo "$resv" | cut -d'|' -f2); bodyv=$(echo "$resv" | cut -d'|' -f3-)
  print_result "Verify Credit (POST /carbon-credits/$pending_id/verify)" "$statusv" "$bodyv" "$timev"
fi

# 4) Create a new credit listing (requires an existing credit id)
# Try to extract any credit id from /carbon-credits
any_credit_id=$(extract_first_id "/carbon-credits" "$AUTH_HEADER")
if [ -n "$any_credit_id" ]; then
  # create listing via form params (as controller expects request params)
  echo "Creating listing for credit $any_credit_id"
  # Using curl query params for price
  resl=$(call_endpoint POST "/credit-listings/create?creditId=$any_credit_id&price=15.00" "$AUTH_HEADER" "" "")
  statusl=$(echo "$resl" | cut -d'|' -f1); timel=$(echo "$resl" | cut -d'|' -f2); bodyl=$(echo "$resl" | cut -d'|' -f3-)
  print_result "Create Listing (POST /credit-listings/create)" "$statusl" "$bodyl" "$timel"
else
  echo "No credit available to create listing. Skipping create-listing test."
fi

# 5) Create a journey (POST /journeys) - sample payload
journey_payload='{
  "vehicleId": null,
  "startTime": null,
  "endTime": null,
  "distanceKm": 10.0,
  "energyConsumedKwh": 3.5,
  "notes": "integration test journey"
}'
resj=$(call_endpoint POST "/journeys" "$AUTH_HEADER" "$journey_payload")
statusj=$(echo "$resj" | cut -d'|' -f1); timej=$(echo "$resj" | cut -d'|' -f2); bodyj=$(echo "$resj" | cut -d'|' -f3-)
print_result "Create Journey (POST /journeys)" "$statusj" "$bodyj" "$timej"

# 6) Get my journeys (authenticated)
res_myj=$(call_endpoint GET "/journeys/my-journeys" "$AUTH_HEADER")
status_myj=$(echo "$res_myj" | cut -d'|' -f1); body_myj=$(echo "$res_myj" | cut -d'|' -f3-); time_myj=$(echo "$res_myj" | cut -d'|' -f2)
print_result "Get My Journeys (GET /journeys/my-journeys)" "$status_myj" "$body_myj" "$time_myj"

# 7) Get credit listings
res_cl=$(call_endpoint GET "/credit-listings" "$AUTH_HEADER")
status_cl=$(echo "$res_cl" | cut -d'|' -f1); body_cl=$(echo "$res_cl" | cut -d'|' -f3-); time_cl=$(echo "$res_cl" | cut -d'|' -f2)
print_result "Get Credit Listings (GET /credit-listings)" "$status_cl" "$body_cl" "$time_cl"

echo "✅ Integration script finished."
echo "Tip: adjust AUTH_HEADER and VERIFIER_AUTH_HEADER for different users/roles."
exit 0