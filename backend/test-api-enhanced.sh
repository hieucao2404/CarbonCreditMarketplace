#!/bin/bash

# Carbon Credit Marketplace API Testing Script - Enhanced Version
BASE_URL="http://localhost:8080"
AUTH_HEADER="Authorization: Basic YWRtaW46YWRtaW4xMjM="  # admin:admin123 in base64

echo "🚀 Testing Carbon Credit Marketplace APIs (Enhanced)"
echo "=================================================="

# Function to check if server is running
check_server() {
    echo "🔍 Checking if server is running..."
    if curl -s --connect-timeout 5 "$BASE_URL/api/actuator/health" > /dev/null; then
        echo "✅ Server is running"
    else
        echo "❌ Server is not running on $BASE_URL"
        echo "💡 Start your server with: mvn spring-boot:run"
        exit 1
    fi
}

# Function to test endpoint with better error handling
test_endpoint() {
    local name="$1"
    local method="$2"
    local endpoint="$3"
    local auth_required="$4"
    
    echo "Testing $name..."
    
    if [ "$auth_required" = "true" ]; then
        response=$(curl -s -w "\nSTATUS_CODE:%{http_code}\nRESPONSE_TIME:%{time_total}" \
            -X "$method" "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -H "$AUTH_HEADER")
    else
        response=$(curl -s -w "\nSTATUS_CODE:%{http_code}\nRESPONSE_TIME:%{time_total}" \
            -X "$method" "$BASE_URL$endpoint" \
            -H "Content-Type: application/json")
    fi
    
    status_code=$(echo "$response" | grep "STATUS_CODE:" | cut -d: -f2)
    response_time=$(echo "$response" | grep "RESPONSE_TIME:" | cut -d: -f2)
    body=$(echo "$response" | sed '/STATUS_CODE:/d' | sed '/RESPONSE_TIME:/d')
    
    echo "   Status: $status_code"
    echo "   Time: ${response_time}s"
    
    if [[ "$status_code" =~ ^2[0-9][0-9]$ ]]; then
        echo "   ✅ SUCCESS"
        if [ ${#body} -lt 500 ]; then
            echo "   Response: $body"
        else
            echo "   Response: $(echo "$body" | head -c 200)... [truncated]"
        fi
    elif [[ "$status_code" == "401" ]]; then
        echo "   ❌ AUTHENTICATION FAILED"
        echo "   💡 Check if SecurityConfig is properly configured"
    elif [[ "$status_code" == "404" ]]; then
        echo "   ❌ ENDPOINT NOT FOUND"
        echo "   💡 Check if controller mapping is correct"
    elif [[ "$status_code" == "500" ]]; then
        echo "   ❌ SERVER ERROR"
        echo "   💡 Check application logs for details"
        echo "   Error: $body"
    else
        echo "   ⚠️  UNEXPECTED STATUS: $status_code"
        echo "   Response: $body"
    fi
    echo ""
}

# Check server availability first
check_server

# Test 1: Health Check (no auth needed)
test_endpoint "Health Check" "GET" "/api/actuator/health" "false"

# Test 2: Application Info (no auth needed)
test_endpoint "Application Info" "GET" "/api/actuator/info" "false"

# Test 3: Get Users (requires auth)
test_endpoint "Get Users" "GET" "/api/users" "true"

# Test 4: Get Carbon Credits (requires auth)
test_endpoint "Get Carbon Credits" "GET" "/api/carbon-credits" "true"

# Test 5: Get Vehicles (requires auth)
test_endpoint "Get Vehicles" "GET" "/api/vehicles" "true"

# Test 6: Get Journeys (requires auth)
test_endpoint "Get Journeys" "GET" "/api/journeys" "true"

# Test 7: Get Credit Listings (requires auth)
test_endpoint "Get Credit Listings" "GET" "/api/credit-listings" "true"

# Test 8: Get Transactions (requires auth)
test_endpoint "Get Transactions" "GET" "/api/transactions" "true"

# Test 9: Get Wallets (requires auth)
test_endpoint "Get Wallets" "GET" "/api/wallets" "true"

# Test 10: Test non-existent endpoint (should return 404)
test_endpoint "Non-existent Endpoint" "GET" "/api/nonexistent" "true"

echo "📊 Testing Summary:"
echo "==================="
echo "✅ Green checkmarks = Successful requests"
echo "❌ Red X marks = Failed requests" 
echo "⚠️  Yellow warnings = Unexpected responses"
echo ""
echo "💡 Common Issues & Solutions:"
echo "• 401 Unauthorized → Check SecurityConfig.java"
echo "• 404 Not Found → Check controller @RequestMapping"
echo "• 500 Server Error → Check application logs"
echo ""
echo "🚀 To start server: mvn spring-boot:run"
echo "📋 To view logs: tail -f logs/application.log"
echo ""
echo "✅ API Testing Complete!"