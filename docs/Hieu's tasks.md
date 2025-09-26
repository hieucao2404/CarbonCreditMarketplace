
# CARBON CREDIT LOGIC DOCUMENTATION

## 🧮 Core Business Logic

### 1. CO₂ Reduction Calculation Algorithm

The system calculates CO₂ emissions saved by using electric vehicles instead of traditional fossil fuel vehicles.

**Formula**: 
```
CO₂ Reduction = (Distance × ICE_EMISSION_FACTOR) - (Energy × GRID_EMISSION_FACTOR)
```

**Constants**:
- `ICE_EMISSION_FACTOR = 0.12 kg CO₂/km` (Internal Combustion Engine average)
- `GRID_EMISSION_FACTOR = 0.5 kg CO₂/kWh` (Electricity grid average)

**Implementation**:
```java
public BigDecimal calculateCO2Reduction(BigDecimal distanceKm, BigDecimal energyConsumedKwh) {
    BigDecimal iceEmissions = distanceKm.multiply(ICE_EMISSION_FACTOR);
    BigDecimal gridEmissions = energyConsumedKwh.multiply(GRID_EMISSION_FACTOR);
    BigDecimal reduction = iceEmissions.subtract(gridEmissions);
    
    // Ensure non-negative values (cannot have negative environmental impact)
    return reduction.max(BigDecimal.ZERO);
}
```

**Example Calculations**:
- **City Trip**: 50km, 15kWh → (50×0.12) - (15×0.5) = 6 - 7.5 = 0kg (no credit)
- **Highway Trip**: 200km, 45kWh → (200×0.12) - (45×0.5) = 24 - 22.5 = 1.5kg CO₂ saved
- **Long Journey**: 500km, 80kWh → (500×0.12) - (80×0.5) = 60 - 40 = 20kg CO₂ saved

### 2. Carbon Credit Amount Calculation

**Base Conversion Rate**: `1 kg CO₂ = 0.001 carbon credits`

#### Tier-Based Multipliers
Credits are calculated using trip-size tiers to incentivize longer, more efficient journeys:

| Trip Type | CO₂ Range | Multiplier | Rationale |
|-----------|-----------|------------|-----------|
| **Short** | ≤ 5 kg CO₂ | 0.5x | Lower efficiency, city driving |
| **Regular** | 5-20 kg CO₂ | 1.0x | Standard suburban/commuter trips |
| **Medium** | 20-50 kg CO₂ | 1.2x | Efficient highway driving |
| **Long** | > 50 kg CO₂ | 1.5x | Maximum efficiency, long-distance |

#### Status-Based Multipliers
Credits value changes based on verification status:

| Status | Multiplier | Description |
|--------|------------|-------------|
| **PENDING** | 0.8x | Unverified credits (20% discount) |
| **VERIFIED** | 1.0x | CVA-verified credits (full value) |
| **LISTED** | 1.0x | Available for sale (full value) |
| **SOLD** | 1.1x | Completed transaction bonus |

**Implementation**:
```java
private BigDecimal calculateCreditAmount(BigDecimal co2Reduced, CreditStatus status) {
    // Base conversion
    BigDecimal baseRate = new BigDecimal("0.001");
    BigDecimal amount = co2Reduced.multiply(baseRate);
    
    // Apply tier multiplier
    BigDecimal tierMultiplier = getTierMultiplier(co2Reduced);
    amount = amount.multiply(tierMultiplier);
    
    // Apply status multiplier
    BigDecimal statusMultiplier = getStatusMultiplier(status);
    
    return amount.multiply(statusMultiplier).setScale(6, RoundingMode.HALF_UP);
}

private BigDecimal getTierMultiplier(BigDecimal co2Reduced) {
    if (co2Reduced.compareTo(new BigDecimal("5")) <= 0) {
        return new BigDecimal("0.5");  // Short trip
    } else if (co2Reduced.compareTo(new BigDecimal("20")) <= 0) {
        return new BigDecimal("1.0");  // Regular trip
    } else if (co2Reduced.compareTo(new BigDecimal("50")) <= 0) {
        return new BigDecimal("1.2");  // Medium trip
    } else {
        return new BigDecimal("1.5");  // Long trip
    }
}
```

### 3. Carbon Credit Lifecycle Management

#### State Diagram
```
Journey Created → CO₂ Calculated → PENDING Credit
                                      ↓
                              CVA Review Process
                                   ↙     ↘
                            VERIFIED    REJECTED
                                ↓           ↓
                           Owner Lists   [END]
                                ↓
                            LISTED
                                ↓
                        Buyer Purchases
                                ↓
                             SOLD
```

#### State Transitions and Business Rules

**1. PENDING → VERIFIED**
```java
public CarbonCredit verifyCarbonCredit(UUID creditId, User verifier) {
    CarbonCredit credit = findCreditById(creditId);
    
    // Business rules
    if (credit.getStatus() != CreditStatus.PENDING) {
        throw new IllegalStateException("Only pending credits can be verified");
    }
    
    if (!verifier.getRole().equals("CVA")) {
        throw new UnauthorizedOperationException("Only CVA can verify credits");
    }
    
    // Update credit
    credit.setStatus(CreditStatus.VERIFIED);
    credit.setVerifiedAt(LocalDateTime.now());
    credit.setVerifiedBy(verifier.getId());
    
    // Recalculate credit amount with verified status
    credit.setCreditAmount(calculateCreditAmount(credit.getCo2ReducedKg(), CreditStatus.VERIFIED));
    
    return carbonCreditRepository.save(credit);
}
```

**2. PENDING → REJECTED**
```java
public CarbonCredit rejectCarbonCredit(UUID creditId, User rejector) {
    CarbonCredit credit = findCreditById(creditId);
    
    // Business rules
    if (credit.getStatus() != CreditStatus.PENDING) {
        throw new IllegalStateException("Only pending credits can be rejected");
    }
    
    if (!rejector.getRole().equals("CVA")) {
        throw new UnauthorizedOperationException("Only CVA can reject credits");
    }
    
    credit.setStatus(CreditStatus.REJECTED);
    credit.setVerifiedAt(LocalDateTime.now());
    credit.setVerifiedBy(rejector.getId());
    
    return carbonCreditRepository.save(credit);
}
```

**3. VERIFIED → LISTED**
```java
public CarbonCredit listCarbonCredit(UUID creditId, User owner) {
    CarbonCredit credit = findCreditById(creditId);
    
    // Business rules
    if (credit.getStatus() != CreditStatus.VERIFIED) {
        throw new IllegalStateException("Only verified credits can be listed");
    }
    
    if (!credit.getUser().getId().equals(owner.getId())) {
        throw new UnauthorizedOperationException("Only credit owner can list for sale");
    }
    
    credit.setStatus(CreditStatus.LISTED);
    credit.setListedAt(LocalDateTime.now());
    
    return carbonCreditRepository.save(credit);
}
```

**4. LISTED → SOLD**
```java
public CarbonCredit markAsSold(UUID creditId) {
    CarbonCredit credit = findCreditById(creditId);
    
    if (credit.getStatus() != CreditStatus.LISTED) {
        throw new IllegalStateException("Only listed credits can be sold");
    }
    
    credit.setStatus(CreditStatus.SOLD);
    
    // Apply sold bonus
    credit.setCreditAmount(calculateCreditAmount(credit.getCo2ReducedKg(), CreditStatus.SOLD));
    
    return carbonCreditRepository.save(credit);
}
```

## 🔄 Journey Data Processing Logic

### 1. Journey Creation and CO₂ Integration

**Process Flow**:
```
1. User completes EV journey
2. Journey data captured (distance, energy, time)
3. System validates journey data
4. CO₂ reduction calculated automatically
5. Journey saved to database
6. Optional: Auto-generate carbon credit
```

**Implementation**:
```java
@Transactional
public JourneyData createJourneyData(JourneyData journeyData) {
    // Validate journey data
    validateJourneyData(journeyData);
    
    // Calculate CO₂ reduction
    BigDecimal co2Reduction = carbonCreditService.calculateCO2Reduction(
        journeyData.getDistanceKm(), 
        journeyData.getEnergyConsumedKwh()
    );
    journeyData.setCo2ReducedKg(co2Reduction);
    
    // Save journey
    JourneyData saved = journeyDataRepository.save(journeyData);
    
    // Log environmental impact
    log.info("Journey {} saved: {} km, {} kWh, {} kg CO₂ reduced", 
        saved.getId(), saved.getDistanceKm(), saved.getEnergyConsumedKwh(), co2Reduction);
    
    return saved;
}
```

### 2. Bulk Carbon Credit Generation

**Process**: Convert eligible journeys to carbon credits in batch

```java
@Transactional
public List<CarbonCredit> createCarbonCreditsForUser(User user) {
    // Find journeys eligible for credit creation
    List<JourneyData> eligibleJourneys = findEligibleForCarbonCredits(user);
    List<CarbonCredit> createdCredits = new ArrayList<>();
    
    for (JourneyData journey : eligibleJourneys) {
        try {
            // Create carbon credit for each eligible journey
            CarbonCredit credit = carbonCreditService.createCarbonCredit(journey);
            createdCredits.add(credit);
            
            log.info("Created carbon credit {} for journey {} (CO₂: {} kg)", 
                credit.getId(), journey.getId(), credit.getCo2ReducedKg());
                
        } catch (Exception e) {
            log.error("Failed to create carbon credit for journey {}: {}", 
                journey.getId(), e.getMessage());
        }
    }
    
    return createdCredits;
}

public List<JourneyData> findEligibleForCarbonCredits(User user) {
    return journeyDataRepository.findByUser(user).stream()
        .filter(journey -> journey.getCarbonCredit() == null)  // No existing credit
        .filter(journey -> journey.getCo2ReducedKg() != null)  // CO₂ calculated
        .filter(journey -> journey.getCo2ReducedKg().compareTo(BigDecimal.ZERO) > 0)  // Positive impact
        .toList();
}
```

### 3. Journey Statistics and Analytics

**Comprehensive Statistics Calculation**:
```java
@Transactional(readOnly = true)
public JourneyStatisticsWithCredits getDetailedJourneyStatistics(User user) {
    List<JourneyData> journeys = findByUser(user);
    
    if (journeys.isEmpty()) {
        return new JourneyStatisticsWithCredits(/* zero values */);
    }

    // Basic journey statistics
    BigDecimal totalDistance = journeys.stream()
        .map(JourneyData::getDistanceKm)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalEnergy = journeys.stream()
        .map(JourneyData::getEnergyConsumedKwh)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalCo2Reduced = journeys.stream()
        .map(JourneyData::getCo2ReducedKg)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal averageDistance = totalDistance.divide(
        BigDecimal.valueOf(journeys.size()), 2, RoundingMode.HALF_UP);

    // Carbon credit analytics
    List<JourneyData> journeysWithCredits = journeys.stream()
        .filter(j -> j.getCarbonCredit() != null)
        .toList();

    int journeysWithCreditCount = journeysWithCredits.size();
    int journeysWithoutCreditCount = journeys.size() - journeysWithCreditCount;

    BigDecimal totalCreditAmount = journeysWithCredits.stream()
        .map(j -> j.getCarbonCredit().getCreditAmount())
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Calculate potential credits from unconverted journeys
    BigDecimal potentialCreditAmount = journeys.stream()
        .filter(j -> j.getCarbonCredit() == null)
        .filter(j -> j.getCo2ReducedKg() != null && j.getCo2ReducedKg().compareTo(BigDecimal.ZERO) > 0)
        .map(j -> carbonCreditService.calculateCreditAmount(j.getCo2ReducedKg(), CreditStatus.PENDING))
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new JourneyStatisticsWithCredits(
        journeys.size(), totalDistance, totalEnergy, averageDistance, totalCo2Reduced,
        journeysWithCreditCount, journeysWithoutCreditCount, totalCreditAmount, potentialCreditAmount
    );
}
```

## ⚠️ Exception Handling Strategy

### 1. Custom Exception Hierarchy

```java
// Base validation exception
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
    public ValidationException(String entityType, String field, String reason) {
        super(String.format("Validation failed for %s.%s: %s", entityType, field, reason));
    }
}

// Business operation failures
public class BusinessOperationException extends RuntimeException {
    public BusinessOperationException(String message) { super(message); }
    public BusinessOperationException(String message, Throwable cause) { super(message, cause); }
}

// Authorization failures
public class UnauthorizedOperationException extends RuntimeException {
    public UnauthorizedOperationException(String message) { super(message); }
}
```

### 2. Validation Logic Implementation

**Journey Data Validation**:
```java
public void validateJourneyData(JourneyData journeyData) {
    if (journeyData == null) {
        throw new ValidationException("Journey data cannot be null");
    }
    
    // Distance validation
    if (journeyData.getDistanceKm() == null || journeyData.getDistanceKm().compareTo(BigDecimal.ZERO) <= 0) {
        throw new ValidationException("JourneyData", "distanceKm", "must be positive");
    }
    
    // Energy validation  
    if (journeyData.getEnergyConsumedKwh() == null || journeyData.getEnergyConsumedKwh().compareTo(BigDecimal.ZERO) < 0) {
        throw new ValidationException("JourneyData", "energyConsumedKwh", "must be non-negative");
    }
    
    // Time validation
    if (journeyData.getStartTime() == null) {
        throw new ValidationException("JourneyData", "startTime", "cannot be null");
    }
    
    if (journeyData.getEndTime() == null) {
        throw new ValidationException("JourneyData", "endTime", "cannot be null");
    }
    
    if (journeyData.getStartTime().isAfter(journeyData.getEndTime())) {
        throw new ValidationException("JourneyData", "timeRange", "start time cannot be after end time");
    }
    
    // Entity relationships
    if (journeyData.getUser() == null) {
        throw new ValidationException("JourneyData", "user", "cannot be null");
    }
    
    if (journeyData.getVehicle() == null) {
        throw new ValidationException("JourneyData", "vehicle", "cannot be null");
    }
}
```

### 3. Error Scenarios and Handling

**Carbon Credit Operations**:
```java
// Handle non-existent resources
private CarbonCredit findCreditById(UUID creditId) {
    return carbonCreditRepository.findById(creditId)
        .orElseThrow(() -> new EntityNotFoundException(
            "Carbon credit not found with ID: " + creditId));
}

// Handle invalid state transitions
public CarbonCredit verifyCarbonCredit(UUID creditId, User verifier) {
    CarbonCredit credit = findCreditById(creditId);
    
    if (credit.getStatus() != CreditStatus.PENDING) {
        throw new IllegalStateException(
            "Invalid state transition: cannot verify credit with status " + credit.getStatus());
    }
    
    if (!hasVerificationRole(verifier)) {
        throw new UnauthorizedOperationException(
            "User " + verifier.getUsername() + " not authorized to verify credits");
    }
    
    // ... verification logic
}

// Handle business rule violations
public void validateCreditCreation(JourneyData journey) {
    if (journey.getCarbonCredit() != null) {
        throw new BusinessOperationException(
            "Journey " + journey.getId() + " already has associated carbon credit");
    }
    
    if (journey.getCo2ReducedKg() == null || journey.getCo2ReducedKg().compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessOperationException(
            "Cannot create credit: journey has no positive CO₂ reduction");
    }
}
```

### 4. Service-Level Error Handling

**Transactional Error Recovery**:
```java
@Transactional
public List<CarbonCredit> createCarbonCreditsForUser(User user) {
    List<JourneyData> eligibleJourneys = findEligibleForCarbonCredits(user);
    List<CarbonCredit> createdCredits = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    
    for (JourneyData journey : eligibleJourneys) {
        try {
            CarbonCredit credit = carbonCreditService.createCarbonCredit(journey);
            createdCredits.add(credit);
            
        } catch (ValidationException e) {
            errors.add("Journey " + journey.getId() + ": " + e.getMessage());
            log.warn("Validation failed for journey {}: {}", journey.getId(), e.getMessage());
            
        } catch (BusinessOperationException e) {
            errors.add("Journey " + journey.getId() + ": " + e.getMessage());
            log.error("Business rule violation for journey {}: {}", journey.getId(), e.getMessage());
            
        } catch (Exception e) {
            errors.add("Journey " + journey.getId() + ": Unexpected error - " + e.getMessage());
            log.error("Unexpected error creating credit for journey {}", journey.getId(), e);
        }
    }
    
    if (!errors.isEmpty() && createdCredits.isEmpty()) {
        throw new BusinessOperationException(
            "Failed to create any carbon credits. Errors: " + String.join("; ", errors));
    }
    
    if (!errors.isEmpty()) {
        log.warn("Partial success creating carbon credits. {} created, {} errors: {}", 
            createdCredits.size(), errors.size(), String.join("; ", errors));
    }
    
    return createdCredits;
}
```

Purpose of CreditListing
CreditListing is the marketplace layer that makes carbon credits visible and purchasable by buyers. Think of it as the "storefront" for carbon credits.

🏪 Marketplace Flow:
1. EV Journey → 2. Carbon Credit Created → 3. CVA Verification → 4. CreditListing Created → 5. Buyers Purchase

📊 CreditListing vs CarbonCredit
CarbonCredit	CreditListing
The actual environmental asset	The marketplace advertisement
Contains CO₂ data, verification status	Contains price, listing type, market status
Tracks credit lifecycle	Tracks marketplace activity
One-to-one with journey	One-to-one with credit (when listed)



# CREDIT LISTING
 Create new attrite updated time for creditlisting table

## 🗄️ Database Schema Updates

### Recent Schema Modifications (September 2025)

#### 1. Credit Listings Table - Added `updated_at` Column
```sql
-- Add updated_at column to the credit_listings table
ALTER TABLE credit_listings ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- A trigger to automatically update the timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_credit_listings_updated_at
    BEFORE UPDATE ON credit_listings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

#### 2. Disputes Table - Added `resolved_by_id` Column
```sql
-- Add foreign key reference to track which CVA resolved the dispute
ALTER TABLE disputes ADD COLUMN resolved_by_id UUID REFERENCES users(user_id);
```

#### 3. Notifications Table - Added Related Entity Tracking
```sql
-- Add columns to track which business entity triggered the notification
ALTER TABLE notifications ADD COLUMN related_entity_id UUID;
ALTER TABLE notifications ADD COLUMN related_entity_type VARCHAR(50);
```

**Purpose of Related Entity Fields**:
- `related_entity_id`: Stores the UUID of the business object (transaction, payment, credit, etc.)
- `related_entity_type`: Identifies the type of entity using enum values:
  - `TRANSACTION`, `PAYMENT`, `CREDIT`, `LISTING`, `DISPUTE`, `CERTIFICATE`, `USER`, `WALLET`

**Example Usage**:
```java
// Transaction completion notification
notification.setRelatedEntityId(transactionId);
notification.setRelatedEntityType(EntityType.TRANSACTION);

// Payment failure notification  
notification.setRelatedEntityId(paymentId);
notification.setRelatedEntityType(EntityType.PAYMENT);
```

This enables:
- **Clickable notifications** that navigate to specific records
- **Context-aware UI** that shows relevant actions based on entity type
- **Efficient querying** to find all notifications for a specific transaction/credit
- **Cleanup operations** when entities are deleted

### Schema Validation Configuration

**Current JPA Configuration**:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Strict schema validation enabled
```

**Validation Benefits**:
- Ensures database schema matches JPA entity definitions
- Prevents runtime errors from schema mismatches
- Enforces data consistency and integrity
- Catches schema drift early in development

### Database Connection Testing & Troubleshooting

#### Connection Status: ✅ **CONFIRMED WORKING**

**Test Results Summary**:
```
✅ HikariPool connection established successfully
✅ PostgreSQL database connectivity verified
✅ JPA repository operations functional
✅ Integration tests passing (DisputeTransactionIntegrationTest: 17/17)
✅ Service layer tests passing (JourneyDataServiceTest: 1/1)
✅ Application startup successful with schema validation
```

#### Common Schema Validation Issues & Solutions

**Issue**: `Schema-validation: missing column [column_name] in table [table_name]`

**Root Cause**: Database schema doesn't match JPA entity definitions

**Solution Process**:
1. **Identify the mismatch** - Compare entity field annotations with actual database columns
2. **Update database schema** - Apply missing ALTER TABLE statements
3. **Verify entity mappings** - Ensure @Column annotations match database column names
4. **Test validation** - Run application startup to confirm schema alignment

**Example Resolution**:
```sql
-- Problem: Notification entity has relatedEntityId field but database missing column
-- Solution: Add missing columns to existing table
PGPASSWORD=your_password psql -U postgres -h localhost -d carbon_credit_db -c "
ALTER TABLE notifications 
ADD COLUMN related_entity_id UUID, 
ADD COLUMN related_entity_type VARCHAR(50);"
```

#### Production Deployment Considerations

**Schema Migration Strategy**:
```sql
-- For production, use gradual migration approach
-- 1. Add columns as nullable first
ALTER TABLE notifications ADD COLUMN related_entity_id UUID;
ALTER TABLE notifications ADD COLUMN related_entity_type VARCHAR(50);

-- 2. Deploy application code that populates new columns
-- 3. After data migration, add constraints if needed
-- ALTER TABLE notifications ALTER COLUMN related_entity_type SET NOT NULL;
```

**Environment-Specific Settings**:
- **Development**: `ddl-auto: validate` (strict validation)
- **Production**: `ddl-auto: none` (manual migrations only)
- **Testing**: `ddl-auto: create-drop` (fresh schema per test)

## 💰 Transaction Management System

### 1. Transaction Lifecycle

**Transaction Flow**:
```
Credit Listed → Buyer Initiates Purchase → Payment Processing → Transaction Created → Credit Transferred → Transaction Completed
```

**Transaction States**:
```
PENDING → PROCESSING → COMPLETED
    ↓         ↓           ↓
 FAILED   CANCELLED   DISPUTED
```

### 2. Transaction Entity Structure

```java
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;
    
    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;
    
    @ManyToOne
    @JoinColumn(name = "credit_id", nullable = false)
    private CarbonCredit credit;
    
    @ManyToOne
    @JoinColumn(name = "listing_id", nullable = false)
    private CreditListing listing;
    
    @Column(name = "amount", precision = 12, scale = 6)
    private BigDecimal amount; // Credit amount purchased
    
    @Column(name = "price_per_credit", precision = 10, scale = 2)
    private BigDecimal pricePerCredit;
    
    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;
    
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "notes", length = 500)
    private String notes;
}

public enum TransactionStatus {
    PENDING,     // Transaction initiated
    PROCESSING,  // Payment being processed
    COMPLETED,   // Successfully completed
    FAILED,      // Payment/processing failed
    CANCELLED,   // Cancelled by user/system
    DISPUTED     // Under dispute resolution
}
```

### 3. Transaction Business Logic

**Purchase Initiation**:
```java
@Transactional
public Transaction initiatePurchase(User buyer, UUID listingId, BigDecimal amount) {
    // Validate listing availability
    CreditListing listing = validateListingForPurchase(listingId, amount);
    
    // Calculate pricing
    BigDecimal totalPrice = listing.getPrice().multiply(amount);
    
    // Create transaction record
    Transaction transaction = Transaction.builder()
        .buyer(buyer)
        .seller(listing.getCarbonCredit().getUser())
        .credit(listing.getCarbonCredit())
        .listing(listing)
        .amount(amount)
        .pricePerCredit(listing.getPrice())
        .totalPrice(totalPrice)
        .status(TransactionStatus.PENDING)
        .createdAt(LocalDateTime.now())
        .paymentMethod("CREDIT_CARD") // Default or from request
        .build();
    
    Transaction saved = transactionRepository.save(transaction);
    
    // Initiate payment processing
    Payment payment = paymentService.initiatePayment(saved);
    
    // Update transaction status
    saved.setStatus(TransactionStatus.PROCESSING);
    
    log.info("Purchase initiated: {} credits from listing {} by user {}", 
        amount, listingId, buyer.getUsername());
    
    return transactionRepository.save(saved);
}
```

**Transaction Completion**:
```java
@Transactional
public Transaction completeTransaction(UUID transactionId) {
    Transaction transaction = findTransactionById(transactionId);
    
    // Validate transaction can be completed
    if (transaction.getStatus() != TransactionStatus.PROCESSING) {
        throw new IllegalStateException("Transaction not in processing state");
    }
    
    // Verify payment completion
    Payment payment = paymentService.getPaymentByTransaction(transactionId);
    if (payment.getStatus() != PaymentStatus.COMPLETED) {
        throw new IllegalStateException("Payment not completed");
    }
    
    // Transfer credit ownership
    CarbonCredit credit = transaction.getCredit();
    credit.setUser(transaction.getBuyer());
    credit.setStatus(CreditStatus.SOLD);
    carbonCreditRepository.save(credit);
    
    // Update listing status
    CreditListing listing = transaction.getListing();
    listing.setAvailableAmount(listing.getAvailableAmount().subtract(transaction.getAmount()));
    
    if (listing.getAvailableAmount().compareTo(BigDecimal.ZERO) <= 0) {
        listing.setStatus(ListingStatus.SOLD_OUT);
    }
    creditListingRepository.save(listing);
    
    // Complete transaction
    transaction.setStatus(TransactionStatus.COMPLETED);
    transaction.setCompletedAt(LocalDateTime.now());
    
    // Send notifications
    notificationService.sendTransactionCompletedNotification(transaction);
    
    log.info("Transaction {} completed successfully", transactionId);
    return transactionRepository.save(transaction);
}
```

### 4. Transaction Analytics and Statistics

```java
public TransactionStatistics getTransactionStatistics(User user) {
    // Get user's buy/sell transactions
    List<Transaction> buyTransactions = transactionRepository.findByBuyer(user);
    List<Transaction> sellTransactions = transactionRepository.findBySeller(user);
    
    // Calculate buy statistics
    long totalPurchases = buyTransactions.size();
    BigDecimal totalSpent = buyTransactions.stream()
        .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
        .map(Transaction::getTotalPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalCreditsPurchased = buyTransactions.stream()
        .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Calculate sell statistics  
    long totalSales = sellTransactions.size();
    BigDecimal totalEarned = sellTransactions.stream()
        .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
        .map(Transaction::getTotalPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalCreditsSold = sellTransactions.stream()
        .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Calculate averages
    BigDecimal averagePurchasePrice = totalPurchases > 0 ? 
        totalSpent.divide(BigDecimal.valueOf(totalPurchases), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    
    BigDecimal averageSalePrice = totalSales > 0 ? 
        totalEarned.divide(BigDecimal.valueOf(totalSales), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    
    return TransactionStatistics.builder()
        .totalPurchases(totalPurchases)
        .totalSales(totalSales)
        .totalSpent(totalSpent)
        .totalEarned(totalEarned)
        .totalCreditsPurchased(totalCreditsPurchased)
        .totalCreditsSold(totalCreditsSold)
        .averagePurchasePrice(averagePurchasePrice)
        .averageSalePrice(averageSalePrice)
        .netBalance(totalEarned.subtract(totalSpent))
        .build();
}
```

## ⚖️ Dispute Resolution System

### 1. Dispute Lifecycle and States

**Dispute Flow**:
```
Transaction Issue → Dispute Raised → CVA Review → Investigation → Resolution
                                         ↓
                                   [OPEN] → [UNDER_REVIEW] → [RESOLVED/REJECTED]
```

**Dispute States**:
```java
public enum DisputeStatus {
    OPEN,           // Newly created, awaiting CVA assignment
    UNDER_REVIEW,   // CVA investigating the dispute
    RESOLVED,       // Dispute resolved in favor of disputant
    REJECTED,       // Dispute rejected (no grounds found)
    CLOSED          // Administratively closed
}

public enum DisputeReason {
    CREDIT_QUALITY,      // Issues with carbon credit verification/quality
    PAYMENT_ISSUE,       // Payment processing problems
    DELIVERY_FAILURE,    // Credit transfer not completed
    FRAUD_SUSPECTED,     // Suspected fraudulent activity
    TECHNICAL_ERROR,     // System/technical issues
    OTHER               // Other reasons (requires description)
}
```

### 2. Dispute Entity Structure

```java
@Entity
@Table(name = "disputes")
public class Dispute {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;
    
    @ManyToOne
    @JoinColumn(name = "raised_by_id", nullable = false)
    private User raisedBy;
    
    @ManyToOne
    @JoinColumn(name = "assigned_cva_id")
    private User assignedCva;
    
    @Enumerated(EnumType.STRING)
    private DisputeReason reason;
    
    @Enumerated(EnumType.STRING)
    private DisputeStatus status;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "resolution", length = 1000)
    private String resolution;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "priority")
    private Integer priority; // 1 = High, 2 = Medium, 3 = Low
}
```

### 3. Dispute Management Logic

**Creating a Dispute**:
```java
@Transactional
public Dispute createDispute(UUID transactionId, User disputant, DisputeReason reason, String description) {
    Transaction transaction = findTransactionById(transactionId);
    
    // Validate disputant has standing to raise dispute
    validateDisputeEligibility(transaction, disputant);
    
    // Check for existing open disputes
    Optional<Dispute> existingDispute = disputeRepository.findOpenDisputeByTransaction(transactionId);
    if (existingDispute.isPresent()) {
        throw new BusinessOperationException("Transaction already has an open dispute");
    }
    
    // Create dispute
    Dispute dispute = Dispute.builder()
        .transaction(transaction)
        .raisedBy(disputant)
        .reason(reason)
        .description(description)
        .status(DisputeStatus.OPEN)
        .priority(calculateDisputePriority(reason, transaction))
        .createdAt(LocalDateTime.now())
        .build();
    
    Dispute saved = disputeRepository.save(dispute);
    
    // Mark transaction as disputed
    transactionService.markAsDisputed(transactionId);
    
    // Notify CVA team
    notificationService.sendDisputeCreatedNotification(saved);
    
    log.info("Dispute {} created for transaction {} by user {}", 
        saved.getId(), transactionId, disputant.getUsername());
    
    return saved;
}

private void validateDisputeEligibility(Transaction transaction, User disputant) {
    // Only buyer or seller can raise dispute
    if (!transaction.getBuyer().getId().equals(disputant.getId()) && 
        !transaction.getSeller().getId().equals(disputant.getId())) {
        throw new UnauthorizedOperationException("Only transaction participants can raise disputes");
    }
    
    // Cannot dispute pending transactions
    if (transaction.getStatus() == TransactionStatus.PENDING) {
        throw new BusinessOperationException("Cannot dispute pending transactions");
    }
    
    // Time limit for disputes (e.g., 30 days after completion)
    if (transaction.getCompletedAt() != null && 
        transaction.getCompletedAt().isBefore(LocalDateTime.now().minusDays(30))) {
        throw new BusinessOperationException("Dispute period has expired");
    }
}

private Integer calculateDisputePriority(DisputeReason reason, Transaction transaction) {
    // High priority: Fraud, large amounts
    if (reason == DisputeReason.FRAUD_SUSPECTED || 
        transaction.getTotalPrice().compareTo(new BigDecimal("1000")) > 0) {
        return 1;
    }
    
    // Medium priority: Payment issues, delivery failures
    if (reason == DisputeReason.PAYMENT_ISSUE || reason == DisputeReason.DELIVERY_FAILURE) {
        return 2;
    }
    
    // Low priority: Other reasons
    return 3;
}
```

**Dispute Resolution Process**:
```java
@Transactional
public Dispute resolveDispute(UUID disputeId, User cva, DisputeStatus resolution, String resolutionNotes) {
    Dispute dispute = findDisputeById(disputeId);
    
    // Validate CVA authority
    if (!cva.getRole().equals("CVA")) {
        throw new UnauthorizedOperationException("Only CVA can resolve disputes");
    }
    
    if (dispute.getStatus() != DisputeStatus.UNDER_REVIEW && dispute.getStatus() != DisputeStatus.OPEN) {
        throw new IllegalStateException("Dispute not in reviewable state");
    }
    
    // Apply resolution
    dispute.setStatus(resolution);
    dispute.setResolution(resolutionNotes);
    dispute.setResolvedAt(LocalDateTime.now());
    dispute.setAssignedCva(cva);
    
    Dispute resolved = disputeRepository.save(dispute);
    
    // Handle resolution outcomes
    if (resolution == DisputeStatus.RESOLVED) {
        handleDisputeResolved(dispute);
    } else if (resolution == DisputeStatus.REJECTED) {
        handleDisputeRejected(dispute);
    }
    
    // Update transaction status
    transactionService.resolveDisputedTransaction(dispute.getTransaction().getId(), resolution);
    
    // Notify parties
    notificationService.sendDisputeResolvedNotification(resolved);
    
    log.info("Dispute {} resolved as {} by CVA {}", disputeId, resolution, cva.getUsername());
    return resolved;
}

private void handleDisputeResolved(Dispute dispute) {
    Transaction transaction = dispute.getTransaction();
    
    switch (dispute.getReason()) {
        case PAYMENT_ISSUE:
            // Refund buyer, reverse transaction
            paymentService.processRefund(transaction);
            break;
            
        case DELIVERY_FAILURE:
            // Re-attempt credit transfer or refund
            if (!creditTransferService.retryTransfer(transaction)) {
                paymentService.processRefund(transaction);
            }
            break;
            
        case CREDIT_QUALITY:
            // Refund and flag credit for re-verification
            paymentService.processRefund(transaction);
            carbonCreditService.flagForReVerification(transaction.getCredit());
            break;
            
        case FRAUD_SUSPECTED:
            // Refund, freeze accounts, escalate
            paymentService.processRefund(transaction);
            userService.freezeAccount(transaction.getSeller(), "Fraud investigation");
            break;
    }
}
```

### 4. Dispute Analytics and Monitoring

```java
public DisputeAnalytics getDisputeAnalytics() {
    List<Dispute> allDisputes = disputeRepository.findAll();
    
    // Status distribution
    Map<DisputeStatus, Long> statusCounts = allDisputes.stream()
        .collect(Collectors.groupingBy(Dispute::getStatus, Collectors.counting()));
    
    // Reason distribution
    Map<DisputeReason, Long> reasonCounts = allDisputes.stream()
        .collect(Collectors.groupingBy(Dispute::getReason, Collectors.counting()));
    
    // Resolution time analysis
    List<Dispute> resolvedDisputes = allDisputes.stream()
        .filter(d -> d.getResolvedAt() != null)
        .toList();
    
    OptionalDouble averageResolutionTime = resolvedDisputes.stream()
        .mapToLong(d -> ChronoUnit.HOURS.between(d.getCreatedAt(), d.getResolvedAt()))
        .average();
    
    // Dispute rate analysis
    long totalTransactions = transactionRepository.count();
    long disputedTransactions = allDisputes.size();
    double disputeRate = totalTransactions > 0 ? (double) disputedTransactions / totalTransactions * 100 : 0.0;
    
    return DisputeAnalytics.builder()
        .totalDisputes(allDisputes.size())
        .openDisputes(statusCounts.getOrDefault(DisputeStatus.OPEN, 0L))
        .underReviewDisputes(statusCounts.getOrDefault(DisputeStatus.UNDER_REVIEW, 0L))
        .resolvedDisputes(statusCounts.getOrDefault(DisputeStatus.RESOLVED, 0L))
        .rejectedDisputes(statusCounts.getOrDefault(DisputeStatus.REJECTED, 0L))
        .reasonDistribution(reasonCounts)
        .averageResolutionTimeHours(averageResolutionTime.orElse(0.0))
        .disputeRate(disputeRate)
        .build();
}

@Scheduled(cron = "0 0 9 * * MON") // Every Monday at 9 AM
public void generateDisputeReport() {
    DisputeAnalytics analytics = getDisputeAnalytics();
    
    // Find overdue disputes (open for more than 48 hours)
    List<Dispute> overdueDisputes = disputeRepository.findOpenDisputesOlderThan(
        LocalDateTime.now().minusHours(48));
    
    // Generate report
    String report = String.format("""
        Weekly Dispute System Report
        ===========================
        
        Summary:
        - Total Disputes: %d
        - Open: %d (Overdue: %d)
        - Under Review: %d  
        - Resolved: %d
        - Rejected: %d
        
        Performance:
        - Average Resolution Time: %.1f hours
        - Dispute Rate: %.2f%%
        
        Top Reasons:
        %s
        """, 
        analytics.getTotalDisputes(),
        analytics.getOpenDisputes(),
        overdueDisputes.size(),
        analytics.getUnderReviewDisputes(),
        analytics.getResolvedDisputes(),
        analytics.getRejectedDisputes(),
        analytics.getAverageResolutionTimeHours(),
        analytics.getDisputeRate(),
        formatReasonDistribution(analytics.getReasonDistribution())
    );
    
    // Send to administrators
    notificationService.sendAdminReport("Dispute System Weekly Report", report);
    
    log.info("Weekly dispute report generated: {} total disputes, {} overdue", 
        analytics.getTotalDisputes(), overdueDisputes.size());
}
```

### 5. Integration Between Transaction and Dispute Systems

**Transaction-Dispute Workflow**:
```java
@Transactional
public Transaction markAsDisputed(UUID transactionId) {
    Transaction transaction = findTransactionById(transactionId);
    
    // Update transaction status
    transaction.setStatus(TransactionStatus.DISPUTED);
    
    // Freeze any pending payments/transfers
    if (transaction.getStatus() == TransactionStatus.PROCESSING) {
        paymentService.freezePayment(transactionId);
    }
    
    // Prevent further actions on associated listing
    creditListingService.suspendListing(transaction.getListing().getId(), "Under dispute");
    
    return transactionRepository.save(transaction);
}

@Transactional  
public Transaction resolveDisputedTransaction(UUID transactionId, DisputeStatus disputeResolution) {
    Transaction transaction = findTransactionById(transactionId);
    
    if (transaction.getStatus() != TransactionStatus.DISPUTED) {
        throw new IllegalStateException("Transaction not in disputed state");
    }
    
    // Apply resolution outcome to transaction
    if (disputeResolution == DisputeStatus.RESOLVED) {
        // Dispute was upheld - typically means refund/reversal
        transaction.setStatus(TransactionStatus.CANCELLED);
        creditListingService.reactivateListing(transaction.getListing().getId());
        
    } else if (disputeResolution == DisputeStatus.REJECTED) {
        // Dispute was rejected - transaction stands as completed
        transaction.setStatus(TransactionStatus.COMPLETED);
        if (transaction.getCompletedAt() == null) {
            transaction.setCompletedAt(LocalDateTime.now());
        }
    }
    
    return transactionRepository.save(transaction);
}
```

This comprehensive transaction and dispute system provides:
- **Secure Transaction Processing**: Full lifecycle management with proper validation
- **Dispute Resolution**: Fair and transparent dispute handling with CVA oversight  
- **Analytics and Monitoring**: Comprehensive tracking and reporting capabilities
- **Integration**: Seamless integration between transactions, disputes, and the broader marketplace
- **Compliance**: Proper audit trails and regulatory compliance features
 