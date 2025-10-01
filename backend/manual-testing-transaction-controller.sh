#!/bin/bash

# Transaction Controller Manual Testing Script
# This script provides curl commands to manually test all Transaction Controller endpoints

echo "=== Transaction Controller Manual Testing Guide ==="
echo

BASE_URL="http://localhost:8080/api/transactions"

echo "📋 Prerequisites:"
echo "1. Start the application: cd backend && mvn spring-boot:run"
echo "2. Set up test data (users, credits, listings) via other endpoints"
echo "3. Replace UUIDs in examples below with actual values from your database"
echo

echo "🔐 Authentication:"
echo "Most endpoints require authentication. Add this header to all requests:"
echo '-H "Authorization: Bearer <your-jwt-token>"'
echo "Or use Basic Auth for testing:"
echo '-H "Authorization: Basic <base64-encoded-credentials>"'
echo

echo "=================================="
echo "1. INITIATE TRANSACTION (Purchase)"
echo "=================================="
echo "POST ${BASE_URL}/purchase"
echo
echo "curl -X POST ${BASE_URL}/purchase \\"
echo '  -H "Content-Type: application/json" \'
echo '  -H "Authorization: Bearer <token>" \'
echo '  -d "{'
echo '    \"listingId\": \"your-listing-id-here\",'
echo '    \"offerPrice\": 25.00,'
echo '    \"paymentMethodId\": \"payment-method-id\",'
echo '    \"notes\": \"Test purchase from manual testing\"'
echo '  }"'
echo
echo "Expected: 201 Created with transaction details"
echo

echo "=================================="
echo "2. COMPLETE TRANSACTION"
echo "=================================="
echo "POST ${BASE_URL}/{transactionId}/complete"
echo
echo "curl -X POST ${BASE_URL}/your-transaction-id/complete \\"
echo '  -H "Authorization: Bearer <token>"'
echo
echo "Expected: 200 OK with completed transaction"
echo

echo "=================================="
echo "3. CANCEL TRANSACTION"
echo "=================================="
echo "DELETE ${BASE_URL}/{transactionId}/cancel"
echo
echo "curl -X DELETE ${BASE_URL}/your-transaction-id/cancel \\"
echo '  -H "Authorization: Bearer <token>"'
echo
echo "Expected: 200 OK with cancelled transaction"
echo

echo "=================================="
echo "4. GET TRANSACTION DETAILS"
echo "=================================="
echo "GET ${BASE_URL}/{transactionId}"
echo
echo "curl -X GET ${BASE_URL}/your-transaction-id \\"
echo '  -H "Authorization: Bearer <token>"'
echo
echo "Expected: 200 OK with transaction details or 403 if not authorized"
echo

echo "=================================="
echo "5. GET MY TRANSACTIONS (Paginated)"
echo "=================================="
echo "GET ${BASE_URL}/my-transactions"
echo
echo "curl -X GET \"${BASE_URL}/my-transactions?page=0&size=10\" \\"
echo '  -H "Authorization: Bearer <token>"'
echo
echo "Expected: 200 OK with paginated list of user's transactions"
echo

echo "=================================="
echo "6. GET PURCHASE HISTORY"
echo "=================================="
echo "GET ${BASE_URL}/purchases"
echo
echo "curl -X GET \"${BASE_URL}/purchases?page=0&size=10\" \\"
echo '  -H "Authorization: Bearer <token>"'
echo
echo "Expected: 200 OK with user's purchase history"
echo

echo "=================================="
echo "7. GET SALES HISTORY"
echo "=================================="
echo "GET ${BASE_URL}/sales"
echo
echo "curl -X GET \"${BASE_URL}/sales?page=0&size=10\" \\"
echo '  -H "Authorization: Bearer <token>"'
echo
echo "Expected: 200 OK with user's sales history"
echo

echo "=================================="
echo "8. CREATE DISPUTE"
echo "=================================="
echo "POST ${BASE_URL}/{transactionId}/dispute"
echo
echo "curl -X POST ${BASE_URL}/your-transaction-id/dispute \\"
echo '  -H "Content-Type: application/json" \'
echo '  -H "Authorization: Bearer <token>" \'
echo '  -d "{'
echo '    \"reason\": \"Item not as described\",'
echo '    \"description\": \"The carbon credits were not properly verified\",'
echo '    \"evidence\": \"Screenshot of verification issues\"'
echo '  }"'
echo
echo "Expected: 201 Created with dispute details"
echo

echo "=================================="
echo "9. GET DISPUTED TRANSACTIONS (Admin Only)"
echo "=================================="
echo "GET ${BASE_URL}/admin/disputed"
echo
echo "curl -X GET \"${BASE_URL}/admin/disputed?page=0&size=10\" \\"
echo '  -H "Authorization: Bearer <admin-token>"'
echo
echo "Expected: 200 OK for admin users, 403 for non-admin users"
echo

echo "=================================="
echo "10. GET TRANSACTION STATISTICS (Admin Only)"
echo "=================================="
echo "GET ${BASE_URL}/admin/statistics"
echo
echo "curl -X GET \"${BASE_URL}/admin/statistics?startDate=2024-01-01&endDate=2024-12-31\" \\"
echo '  -H "Authorization: Bearer <admin-token>"'
echo
echo "Expected: 200 OK with statistics object containing:"
echo "  - totalTransactions"
echo "  - totalAmount"
echo "  - completedTransactions"
echo "  - etc."
echo

echo "=================================="
echo "🧪 TESTING SCENARIOS"
echo "=================================="
echo
echo "1. Happy Path Testing:"
echo "   - Create transaction → Complete it → Verify completion"
echo "   - Check purchase/sales history"
echo
echo "2. Authorization Testing:"
echo "   - Try accessing other user's transactions (should fail)"
echo "   - Try admin endpoints as regular user (should fail)"
echo
echo "3. Error Handling:"
echo "   - Try with invalid transaction IDs"
echo "   - Try with invalid listing IDs"
echo "   - Try completing already completed transactions"
echo
echo "4. Dispute Testing:"
echo "   - Create disputes for completed transactions"
echo "   - Try creating disputes for pending transactions"
echo
echo "5. Pagination Testing:"
echo "   - Test different page sizes and page numbers"
echo "   - Test edge cases (page beyond available data)"
echo

echo "=================================="
echo "📊 EXPECTED HTTP STATUS CODES"
echo "=================================="
echo "200 OK - Successful GET/PUT/DELETE operations"
echo "201 Created - Successful POST operations"
echo "400 Bad Request - Invalid request data"
echo "401 Unauthorized - Missing or invalid authentication"
echo "403 Forbidden - Insufficient permissions"
echo "404 Not Found - Resource not found"
echo "500 Internal Server Error - Server error"
echo

echo "=================================="
echo "🔍 DEBUGGING TIPS"
echo "=================================="
echo "1. Check application logs for detailed error messages"
echo "2. Verify JWT token is valid and not expired"
echo "3. Ensure test data exists in database"
echo "4. Use -v flag with curl for verbose output"
echo "5. Check Content-Type headers for POST requests"
echo

echo "Example with verbose output:"
echo "curl -v -X GET ${BASE_URL}/my-transactions \\"
echo '  -H "Authorization: Bearer <token>"'
echo