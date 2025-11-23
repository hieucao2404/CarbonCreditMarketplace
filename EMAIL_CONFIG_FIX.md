# 🔧 Email Verification & Password Reset - Configuration Fix

## Problem
Application failed to start with error:
```
Parameter 0 of constructor in com.carboncredit.service.EmailService required a bean of type 
'org.springframework.mail.javamail.JavaMailSender' that could not be found.
```

## Solution Applied

### 1. ✅ Created MailConfig Bean
**File:** `backend/src/main/java/com/carboncredit/config/MailConfig.java`

Created a Spring configuration class to provide `JavaMailSender` bean:
- Reads email credentials from environment variables: `EMAIL_USERNAME`, `EMAIL_PASSWORD`
- Configures SMTP settings (Gmail SMTP on port 587)
- Sets up TLS encryption and timeouts
- Provides sensible defaults if env vars are not set

```java
@Configuration
public class MailConfig {
    @Bean
    public JavaMailSender javaMailSender() {
        // Configures mail sender with SMTP settings
    }
}
```

### 2. ✅ Updated application.yml
**File:** `backend/src/main/resources/application.yml`

Added/Fixed email configuration:
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${EMAIL_USERNAME:your-email@gmail.com}
    password: ${EMAIL_PASSWORD:your-app-password}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

app:
  mail:
    from: carbon-credit@example.com
    sender-name: Carbon Credit Marketplace
    verification-link-expiry: 24  # hours
    password-reset-link-expiry: 1 # hour
  task:
    scheduling:
      enabled: true
      pool:
        size: 2
```

### 3. ✅ Enabled Scheduling
**File:** `backend/src/main/java/com/carboncredit/CarbonCreditMarketplaceApplication.java`

Added `@EnableScheduling` annotation to enable scheduled cleanup tasks:
- Cleanup expired password reset tokens (daily at 2 AM)
- Delete unverified old accounts (daily at 3 AM)

## Environment Variables Required

Set these before running the application:

```bash
export EMAIL_USERNAME="your-gmail@gmail.com"
export EMAIL_PASSWORD="your-gmail-app-password"
```

For Gmail:
1. Enable 2-factor authentication
2. Create an App Password: https://myaccount.google.com/apppasswords
3. Use that 16-character password as `EMAIL_PASSWORD`

## Testing

To verify the configuration works:

```bash
cd backend
export EMAIL_USERNAME="your-email@gmail.com"
export EMAIL_PASSWORD="your-app-password"
mvn clean spring-boot:run
```

You should see:
```
Started CarbonCreditMarketplaceApplication in X seconds
```

## Summary of Changes

| File | Change |
|------|--------|
| `MailConfig.java` | **NEW** - Spring configuration bean for JavaMailSender |
| `application.yml` | Updated email and scheduling configuration |
| `CarbonCreditMarketplaceApplication.java` | Added @EnableScheduling annotation |

All changes are minimal, non-breaking, and follow Spring Boot best practices! ✅
