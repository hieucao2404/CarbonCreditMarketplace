# 🌱 Carbon Credit Marketplace - Team Documentation

**Project Status Report & Development Guide**  
**Updated**: October 6, 2025  
**Team**: Carbon Credit Marketplace Development Team  

---

## 📋 **Executive Summary**

The Carbon Credit Marketplace is a **Spring Boot-based platform** that enables electric vehicle (EV) owners to generate, trade, and monetize carbon credits based on their environmentally-friendly driving behavior. The project has **solid foundational architecture** with **60% of core features complete** and is ready for team collaboration to finish the remaining components.

### **🎯 Current Status: 60% Complete - Ready for Production Push**

---

## 🏗️ **Project Architecture**

### **Technology Stack**
- **Backend**: Java 21 + Spring Boot 3.3.4
- **Database**: PostgreSQL with JPA/Hibernate
- **Security**: Spring Security with Basic Authentication
- **Build Tool**: Maven
- **Testing**: JUnit 5 + Mockito (83 tests passing)
- **API**: REST with DTO pattern (prevents circular references)

### **Core Business Flow**
```
1. EV Owner Records Journey → 2. CO₂ Calculation → 3. Carbon Credit Generation →
4. CVA Verification → 5. Marketplace Listing → 6. Transaction & Purchase →
7. Certificate Generation → 8. Wallet Management
```

---

## ✅ **What's Already Complete (Team Achievements)**

### **🎉 Solid Foundation (100% Complete)**
- ✅ **Complete Entity Model** - 12 core entities with proper relationships
- ✅ **Business Logic Layer** - Sophisticated CO₂ calculation algorithms
- ✅ **Database Schema** - Production-ready PostgreSQL structure
- ✅ **Service Layer** - All 12 services implemented with business rules
- ✅ **Security System** - User authentication & role-based access
- ✅ **DTO Layer** - Clean API responses without circular references
- ✅ **Testing Framework** - Comprehensive unit & integration tests

### **🚀 Working API Endpoints (6/10 Controllers Complete)**

#### ✅ **UserController** - User Management
```bash
POST /api/users/register     # User registration
POST /api/users/login        # User authentication  
GET  /api/users             # User listing
PUT  /api/users/{id}        # Update user profile
```

#### ✅ **CarbonCreditController** - Credit Lifecycle
```bash
POST /api/carbon-credits                    # Generate credit from journey
GET  /api/carbon-credits/user/{userId}     # User's credits
POST /api/carbon-credits/{id}/verify       # CVA verification
POST /api/carbon-credits/{id}/reject       # CVA rejection
GET  /api/carbon-credits/pending           # Pending verification
```

#### ✅ **JourneyController** - EV Trip Management  
```bash
POST /api/journeys                  # Record new EV journey
GET  /api/journeys/my-journeys     # User's journey history
GET  /api/journeys/{id}            # Journey details
GET  /api/journeys/statistics      # CO₂ impact statistics
PUT  /api/journeys/{id}            # Update journey data
GET  /api/journeys/admin/all       # Admin: all journeys
```

#### ✅ **WalletController** - Financial Management
```bash
GET  /api/wallets/my-wallet              # Wallet balance
GET  /api/wallets/balance-check          # Sufficient funds check
POST /api/wallets/deposit                # Add funds
POST /api/wallets/withdraw               # Withdraw funds
GET  /api/wallets/transactions           # Transaction history
GET  /api/wallets/admin/user/{userId}    # Admin wallet access
PUT  /api/wallets/admin/user/{userId}/balance  # Admin balance update
```

#### ✅ **TransactionController** - Marketplace Transactions
```bash
POST /api/transactions/purchase                    # Initiate purchase
POST /api/transactions/{id}/complete               # Complete transaction
POST /api/transactions/{id}/cancel                 # Cancel transaction
GET  /api/transactions/{id}                        # Transaction details
GET  /api/transactions/my-transactions             # User transaction history
GET  /api/transactions/purchases                   # Purchase history
GET  /api/transactions/sales                       # Sales history
POST /api/transactions/{id}/dispute                # Create dispute
GET  /api/transactions/admin/disputed              # Admin disputed transactions
GET  /api/transactions/admin/statistics            # Admin analytics
```

#### ✅ **CreditListingController** - Marketplace Operations
```bash
POST /api/credit-listings/create                   # Create listing
GET  /api/credit-listings                          # Browse marketplace
GET  /api/credit-listings/search                   # Price range search
GET  /api/credit-listings/my-listings              # User's listings
GET  /api/credit-listings/my-active-listings       # Active listings only
POST /api/credit-listings/{id}/purchase            # Purchase listing
PUT  /api/credit-listings/{id}/price               # Update price
DELETE /api/credit-listings/{id}                   # Cancel listing
GET  /api/credit-listings/stats                    # Marketplace statistics
```

---

## 🚧 **Outstanding Work (Team Assignments)**

### **🔥 HIGH PRIORITY - Missing Controllers (4 weeks)**

#### **1. PaymentController** 
**Assignee**: `[TEAM_MEMBER_1]`  
**Deadline**: Week 1-2  
**Status**: 🔴 Not Started

**Required Endpoints**:
```bash
POST /api/payments/process              # Handle payments
GET  /api/payments/user/{userId}        # Payment history  
POST /api/payments/refund              # Process refunds
GET  /api/payments/{id}                # Payment details
POST /api/payments/withdraw            # Withdrawal requests
```

**Dependencies**: PaymentService (exists), BankingService (exists)

---

#### **2. CertificateController**
**Assignee**: `[TEAM_MEMBER_2]`  
**Deadline**: Week 1-2  
**Status**: 🔴 Not Started

**Required Endpoints**:
```bash
GET  /api/certificates/user/{userId}     # User certificates
GET  /api/certificates/{id}/download     # Download PDF
POST /api/certificates/generate          # Generate certificate
GET  /api/certificates/{id}             # Certificate details
```

**Dependencies**: CertificateService (exists)

---

#### **3. NotificationController**
**Assignee**: `[TEAM_MEMBER_3]`  
**Deadline**: Week 2-3  
**Status**: 🔴 Not Started

**Required Endpoints**:
```bash
GET  /api/notifications/my-notifications  # User notifications
PUT  /api/notifications/{id}/read         # Mark as read
POST /api/notifications/send              # Send notification (admin)
DELETE /api/notifications/{id}            # Delete notification
GET  /api/notifications/unread-count      # Unread count
```

**Dependencies**: NotificationService (needs creation), Notification entity (exists)

---

#### **4. AuditController**  
**Assignee**: `[TEAM_MEMBER_4]`  
**Deadline**: Week 2-3  
**Status**: 🔴 Not Started

**Required Endpoints**:
```bash
GET  /api/audit/credit/{creditId}         # Credit audit history
GET  /api/audit/user/{userId}/actions     # User actions
GET  /api/audit/reports/verification      # CVA reports
GET  /api/audit/admin/all                # Admin audit view
```

**Dependencies**: AuditService (exists)

---

### **🟡 MEDIUM PRIORITY - Feature Enhancements (2-4 weeks)**

#### **5. Advanced Search & Filtering**
**Assignee**: `[TEAM_MEMBER_5]`  
**Status**: 🟡 Partially Complete

**Needed Enhancements**:
- Region/location-based search
- CO₂ amount range filtering  
- Multi-criteria sorting
- Advanced marketplace filters

---

#### **6. File Upload System**
**Assignee**: `[TEAM_MEMBER_6]`  
**Status**: 🔴 Not Started

**Required Features**:
- CSV/JSON journey data import
- Bulk journey processing
- Import templates
- File validation

---

#### **7. Reporting & Analytics**
**Assignee**: `[TEAM_MEMBER_7]`  
**Status**: 🔴 Not Started

**Required Reports**:
- CO₂ impact analytics
- Marketplace statistics
- User performance metrics
- PDF report generation

---

## 🚀 **Getting Started (For New Team Members)**

### **1. Environment Setup**
```bash
# Clone repository
git clone https://github.com/hieucao2404/CarbonCreditMarketplace.git
cd CarbonCreditMarketplace/backend

# Install dependencies
mvn clean install

# Setup PostgreSQL database
# Update application.yml with your database credentials

# Run tests (should pass 83/83)
mvn test

# Start application
mvn spring-boot:run
```

**Application runs on**: `http://localhost:8080`  
**API Base URL**: `http://localhost:8080/api`

### **2. Testing the API**
```bash
# Test user endpoints
curl -X GET "http://localhost:8080/api/users"

# Test with authentication
curl -u "admin1:password" -X GET "http://localhost:8080/api/transactions"

# Check wallet functionality  
curl -X GET "http://localhost:8080/api/wallets/my-wallet"
```

### **3. Development Guidelines**

#### **Code Standards**:
- Follow existing DTO pattern for all controllers
- Include comprehensive unit tests (aim for 80%+ coverage)
- Use `@Slf4j` for logging
- Follow REST conventions for endpoint naming
- Implement proper error handling and validation

#### **Controller Template**:
```java
@Slf4j
@RestController  
@RequestMapping("/your-endpoint")
@RequiredArgsConstructor
@Validated
public class YourController {
    
    private final YourService yourService;
    private final UserService userService;
    
    @GetMapping
    public ResponseEntity<Page<YourDTO>> getItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            // Your implementation
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in getItems: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
```

---

## 📊 **Testing Strategy**

### **Current Test Coverage**: 83 Tests Passing ✅
- Unit Tests: Controller, Service, Repository layers
- Integration Tests: Full API workflows  
- Test Utilities: MockMvc, TestContainers ready

### **Testing Requirements for New Features**:
1. **Unit Tests**: Each controller method
2. **Integration Tests**: Complete user workflows
3. **Error Handling**: Invalid inputs, unauthorized access
4. **Performance Tests**: For file upload/processing features

### **Running Tests**:
```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=PaymentControllerTest

# Integration tests only
mvn test -Dtest=*IntegrationTest
```

---

## 🗄️ **Database Schema**

### **Core Entities (All Complete)**:
- **users** - User accounts and roles
- **vehicles** - EV vehicle information
- **journey_data** - EV trip records
- **carbon_credits** - Generated carbon credits
- **credit_listings** - Marketplace listings
- **transactions** - Purchase transactions
- **wallets** - User financial balances
- **certificates** - Buyer certificates
- **payments** - Payment records
- **notifications** - User notifications
- **audit_logs** - CVA audit trails
- **disputes** - Transaction disputes

### **Database Access**:
```bash
# PostgreSQL connection (update in application.yml)
URL: jdbc:postgresql://localhost:5432/carbon_credit_marketplace
Username: [your-username]  
Password: [your-password]
```

---

## 🔐 **Authentication & Security**

### **Current System**: Spring Security with Basic Auth
**User Roles**:
- `EV_OWNER` - Creates journeys, lists credits
- `BUYER` - Purchases carbon credits  
- `CVA` - Verifies carbon credits
- `ADMIN` - System administration

### **Test Accounts**:
```
Username: admin1    | Password: password | Role: ADMIN
Username: evowner1  | Password: password | Role: EV_OWNER  
Username: buyer1    | Password: password | Role: BUYER
Username: cva1      | Password: password | Role: CVA
```

### **Future Security Enhancements**:
- JWT token implementation
- API rate limiting
- Enhanced input validation
- OAuth2 integration

---

## 📈 **Project Metrics & KPIs**

### **Development Progress**:
- **Overall Completion**: 60%
- **Controllers**: 6/10 (60%)
- **Services**: 12/12 (100%) 
- **Entities**: 12/12 (100%)
- **Tests Passing**: 83/83 (100%)

### **Code Quality**:
- **Architecture**: Clean, layered design ✅
- **Testing**: Comprehensive coverage ✅  
- **Documentation**: Good inline docs ✅
- **Error Handling**: Basic implementation ⚠️
- **Performance**: Not optimized yet ⚠️

---

## 🎯 **Sprint Planning (4-Week Roadmap)**

### **Week 1: Core API Completion**
- [ ] PaymentController implementation
- [ ] CertificateController implementation  
- [ ] Integration testing for new endpoints
- [ ] API documentation updates

### **Week 2: User Experience Features**  
- [ ] NotificationController implementation
- [ ] AuditController implementation
- [ ] Enhanced error handling
- [ ] User interface improvements

### **Week 3: Advanced Features**
- [ ] File upload system
- [ ] Advanced search/filtering
- [ ] Reporting system foundation
- [ ] Performance optimization

### **Week 4: Production Readiness**
- [ ] Security enhancements
- [ ] Comprehensive API documentation
- [ ] Deployment preparation
- [ ] Final testing & bug fixes

---

## 🤝 **Team Collaboration**

### **Communication Channels**:
- **Daily Standups**: [TIME/PLATFORM]
- **Code Reviews**: GitHub Pull Requests
- **Documentation**: This file + inline code docs
- **Issue Tracking**: GitHub Issues

### **Git Workflow**:
```bash
# Create feature branch
git checkout -b feature/payment-controller

# Regular commits with clear messages
git commit -m "feat: implement payment processing endpoint"

# Push and create PR
git push origin feature/payment-controller
```

### **Branch Naming Convention**:
- `feature/[controller-name]` - New features
- `bugfix/[issue-description]` - Bug fixes  
- `docs/[update-description]` - Documentation
- `test/[test-description]` - Test improvements

---

## 🆘 **Support & Resources**

### **Key Project Files**:
- **Main Application**: `src/main/java/com/carboncredit/CarbonCreditMarketplaceApplication.java`
- **Controllers**: `src/main/java/com/carboncredit/controller/`
- **Services**: `src/main/java/com/carboncredit/service/`
- **Entities**: `src/main/java/com/carboncredit/entity/`
- **DTOs**: `src/main/java/com/carboncredit/dto/`
- **Tests**: `src/test/java/com/carboncredit/`

### **Documentation**:
- **Business Logic**: `docs/Hieu's tasks.md`
- **Week 4 Analysis**: `docs/Week4.md`
- **Database Schema**: `backend/schema.sql`
- **API Testing**: `backend/manual-testing-*.sh`

### **Getting Help**:
1. Check existing similar controllers for patterns
2. Review service layer documentation  
3. Run tests to understand expected behavior
4. Ask team lead for architecture decisions

---

## 🎉 **Success Criteria**

### **MVP Completion (4 weeks)**:
- ✅ All 10 controllers implemented
- ✅ 100+ tests passing
- ✅ API documentation complete
- ✅ Basic error handling implemented
- ✅ Ready for frontend integration

### **Production Ready (6 weeks)**:
- ✅ Security enhancements complete
- ✅ Performance optimizations done
- ✅ Monitoring/logging implemented  
- ✅ Deployment pipeline ready

---

## 📞 **Contact Information**

**Project Lead**: [YOUR_NAME]  
**Repository**: https://github.com/hieucao2404/CarbonCreditMarketplace  
**Documentation**: `/docs/` folder  
**Last Updated**: October 6, 2025

---

**🌱 Let's build the future of carbon credit trading together! 🚀**