# Carbon Credit Marketplace

A platform for EV owners to generate and trade carbon credits based on their sustainable transportation choices.

## 🚗 What It Does

- **EV Journey Tracking**: Record electric vehicle trips and energy consumption
- **CO2 Calculation**: Calculate environmental savings vs gasoline cars  
- **Carbon Credit Generation**: Convert CO2 savings into tradeable credits
- **Marketplace**: Buy and sell verified carbon credits
- **Verification System**: CVA approval process for credit authenticity

## ��️ Tech Stack

### Backend
- **Java 21** + **Spring Boot 3.2.0**
- **PostgreSQL** (Production) / **H2** (Testing)
- **JPA/Hibernate** for database operations
- **Spring Security** for authentication & authorization
- **Maven** for build management

### Project Structure
```
CarbonCreditMarketplace/
├── backend/          # Spring Boot API (Current)
└── frontend/         # Web App (Coming Soon)
```

## 📊 Key Features

### Credit Calculation
```
CO2 Saved = (Distance × 0.21) - (Energy × 0.5)
- 0.21 kg CO2/km: Average gasoline car emissions
- 0.5 kg CO2/kWh: Grid electricity emissions
- 1000 kg CO2 = 1 carbon credit
```

### Status-Based Rates
- **PENDING**: 70% rate (awaiting verification)
- **VERIFIED**: 100% rate (CVA approved)
- **LISTED/SOLD**: Locked rate

### Trip Bonuses
- **50+ kg CO2**: 1.5× bonus (long trips)
- **20-49 kg CO2**: 1.2× bonus
- **5-19 kg CO2**: 1.0× standard
- **<5 kg CO2**: 0.5× penalty

## 🚀 Quick Start

### 1. Setup Database
```bash
createdb carbon_credit_marketplace
psql carbon_credit_marketplace < backend/schema.sql
```

### 2. Run Backend
```bash
cd backend
mvn spring-boot:run
```

### 3. Test
```bash
mvn test -Dtest=CarbonCreditServiceTest
```

## 👥 User Roles

- **EV_OWNER**: Submit journeys, generate credits
- **BUYER**: Purchase credits from marketplace  
- **CVA**: Verify and approve credits
- **ADMIN**: System management

## 🧪 Test Users (from schema.sql)

| Username | Password | Role |
|----------|----------|------|
| evowner1 | evowner | EV_OWNER |
| buyer1 | buyer | BUYER |
| cva1 | cva1 | CVA |
| admin1 | admin1 | ADMIN |

## 📁 Key Components

- **Entities**: User, Vehicle, JourneyData, CarbonCredit, Transaction, Wallet
- **Services**: CarbonCreditService (CO2 calculations), UserService (authentication)
- **Controllers**: REST API endpoints (coming soon)
- **Repositories**: JPA data access layer

## 🔄 Credit Flow

1. **Record Journey** → EV owner logs trip
2. **Calculate CO2** → System computes savings  
3. **Generate Credit** → Creates PENDING credit
4. **CVA Verify** → Approves to VERIFIED status
5. **List/Sell** → Marketplace transaction

---

**Built for sustainable transportation and carbon offset markets**
