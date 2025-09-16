# Carbon Credit Marketplace Backend

A Spring Boot backend application for a Carbon Credit Marketplace platform where EV owners can generate, trade, and manage carbon credits.

## Features

### For EV Owners
- Connect and sync journey data from electric vehicles
- Calculate CO₂ emission reduction and convert to carbon credits
- Manage carbon wallet (track credit balance)
- List carbon credits for sale (fixed price / auction)
- Manage transactions: track, cancel, or complete
- Payment & withdrawal after selling credits
- Personal reports: CO₂ reduction, credit revenue
- AI price suggestions based on market data

### For Carbon Credit Buyers
- Search & filter credits by quantity, price, region
- Buy credits directly or participate in auctions
- Online payment (e-wallet, banking)
- Receive certificates for emission reduction reporting
- Manage purchase history

### For Carbon Verification & Audit (CVA)
- Verify emission data & credit records
- Approve or reject carbon credit issuance requests
- Issue credits and record in carbon wallet
- Export carbon credit issuance reports

### For Admins
- Manage users (EV owners, buyers, verifiers)
- Manage transactions: monitor, confirm, handle disputes
- Manage digital wallets and cash flow
- Manage credit listings & transactions
- Generate comprehensive transaction reports

## Technology Stack

- **Backend**: Java 17, Spring Boot 3.2.0
- **Database**: PostgreSQL with JPA/Hibernate
- **Security**: Spring Security
- **Build Tool**: Maven
- **Documentation**: Spring REST Docs (planned)

## Project Structure

```
src/
├── main/
│   ├── java/com/carboncredit/
│   │   ├── entity/          # JPA entities
│   │   ├── repository/      # Data access layer
│   │   ├── service/         # Business logic layer
│   │   ├── controller/      # REST API controllers
│   │   ├── config/          # Configuration classes
│   │   └── CarbonCreditMarketplaceApplication.java
│   └── resources/
│       ├── application.yml  # Application configuration
│       └── data.sql        # Sample data (optional)
└── test/                   # Test classes
```

## Database Entities

- **User**: EV owners, buyers, CVA verifiers, admins
- **Vehicle**: EV vehicle information
- **JourneyData**: Trip data and CO₂ reduction calculations
- **CarbonCredit**: Carbon credits generated from journeys
- **CreditListing**: Marketplace listings (fixed price/auction)
- **Transaction**: Purchase transactions
- **Wallet**: Credit and cash balances
- **Certificate**: Certificates issued to buyers
- **AuditLog**: Verification audit trail
- **Dispute**: Transaction dispute management

## Getting Started

### Prerequisites
- Java 17
- Maven 3.6+
- PostgreSQL 12+

### Database Setup
1. Create a PostgreSQL database named `carbon_credit_db`
2. Run the SQL schema from the project root directory
3. Update `application.yml` with your database credentials

### Running the Application
```bash
# Clone the repository
git clone <repository-url>
cd carbon-credit-backend

# Build the application
mvn clean compile

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080/api`

### Environment Variables
- `DB_USERNAME`: Database username (default: postgres)
- `DB_PASSWORD`: Database password (default: password)
- `JWT_SECRET`: JWT signing secret (default: mySecretKey)

## API Endpoints

### User Management
- `POST /api/users` - Create new user
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/role/{role}` - Get users by role
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Carbon Credits
- `GET /api/carbon-credits` - Get available credits
- `GET /api/carbon-credits/{id}` - Get credit by ID
- `GET /api/carbon-credits/pending` - Get pending credits (CVA)
- `GET /api/carbon-credits/user/{userId}` - Get user's credits
- `POST /api/carbon-credits/{id}/verify` - Verify credit (CVA only)
- `POST /api/carbon-credits/{id}/reject` - Reject credit (CVA only)

## Development

### Adding New Features
1. Create entity classes in `entity` package
2. Create repository interfaces in `repository` package
3. Implement business logic in `service` package
4. Create REST controllers in `controller` package
5. Add tests in the `test` directory

### Code Style
- Use Lombok for reducing boilerplate code
- Follow Spring Boot best practices
- Use proper HTTP status codes in controllers
- Implement proper error handling

## Future Enhancements

- JWT-based authentication
- Role-based access control
- File upload for journey data
- Real-time notifications
- Integration with payment gateways
- Mobile app API support
- Advanced reporting and analytics
- Blockchain integration for credit traceability

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.
