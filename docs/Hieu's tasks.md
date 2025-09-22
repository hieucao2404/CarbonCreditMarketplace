
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
 