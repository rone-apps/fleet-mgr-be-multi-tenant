#!/bin/bash

# Fix script for AccountChargeService and AccountCustomerService
# Changes all IsPaid method calls to Paid

echo "🔧 Fixing service files..."

SERVICE_DIR="src/main/java/com/taxi/domain/account/service"

# Fix AccountChargeService.java
if [ -f "$SERVICE_DIR/AccountChargeService.java" ]; then
    echo "Fixing AccountChargeService.java..."
    sed -i.bak 's/findByAccountCustomerIdAndIsPaidFalse/findByAccountCustomerIdAndPaidFalse/g' "$SERVICE_DIR/AccountChargeService.java"
    sed -i.bak 's/findByAccountCustomerIdAndIsPaidFalseAndTripDateBetween/findByAccountCustomerIdAndPaidFalseAndTripDateBetween/g' "$SERVICE_DIR/AccountChargeService.java"
    sed -i.bak 's/findByIsPaidFalse/findByPaidFalse/g' "$SERVICE_DIR/AccountChargeService.java"
    echo "  ✅ AccountChargeService.java fixed"
else
    echo "  ❌ AccountChargeService.java not found"
fi

# Fix AccountCustomerService.java
if [ -f "$SERVICE_DIR/AccountCustomerService.java" ]; then
    echo "Fixing AccountCustomerService.java..."
    sed -i.bak 's/countByAccountCustomerIdAndIsPaidFalse/countByAccountCustomerIdAndPaidFalse/g' "$SERVICE_DIR/AccountCustomerService.java"
    echo "  ✅ AccountCustomerService.java fixed"
else
    echo "  ❌ AccountCustomerService.java not found"
fi

echo ""
echo "✅ All service files fixed!"
echo ""
echo "🔨 Building..."
./gradlew clean build

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
else
    echo ""
    echo "❌ Build failed"
    exit 1
fi`
