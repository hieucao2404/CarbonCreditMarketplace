# 📧 Email Verification Setup Guide

## Current Status
✅ Email verification system is implemented in the backend
✅ Frontend pages created (VerifyEmailPage, ForgotPasswordPage, ResetPasswordPage)
✅ Database schema updated with email verification columns
❌ Email not sending - requires credential configuration

## Why Emails Are Not Sending

The system is configured to send emails via Gmail SMTP, but requires **valid email credentials**. Currently using default placeholders:
- Username: `your-email@gmail.com`
- Password: `your-app-password`

## Option 1: Using Gmail (Recommended for Testing)

### Step 1: Create Gmail App Password

1. Go to https://myaccount.google.com/apppasswords
2. Select "Mail" and "Windows Computer" (or your device)
3. Google will generate a **16-character app password**
4. Copy this password

### Step 2: Set Environment Variables

**On Linux/Mac:**
```bash
export EMAIL_USERNAME="your-gmail@gmail.com"
export EMAIL_PASSWORD="xxxx xxxx xxxx xxxx"  # The 16-char password from Step 1
```

**On Windows (PowerShell):**
```powershell
$env:EMAIL_USERNAME="your-gmail@gmail.com"
$env:EMAIL_PASSWORD="xxxx xxxx xxxx xxxx"
```

### Step 3: Run the Application

```bash
cd backend
mvn clean spring-boot:run
```

### Step 4: Test Registration

Send a POST request to register:
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Password123!",
    "role": "EV_OWNER",
    "fullName": "Test User",
    "phone": "0123456789"
  }'
```

You should receive an email with verification link!

## Option 2: Using application.yml Configuration

Edit `backend/src/main/resources/application.yml`:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-gmail@gmail.com
    password: xxxx xxxx xxxx xxxx
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
```

## Option 3: Using Other SMTP Services

### Gmail
- Host: `smtp.gmail.com`
- Port: `587`
- Auth: Required (use App Password)

### Outlook/Office 365
- Host: `smtp-mail.outlook.com`
- Port: `587`
- Auth: Required

### SendGrid
- Host: `smtp.sendgrid.net`
- Port: `587`
- Username: `apikey`
- Password: Your SendGrid API key

### Mailgun
- Host: `smtp.mailgun.org`
- Port: `587`
- Username: `postmaster@yourdomain.mailgun.org`
- Password: Your Mailgun password

## Testing Email Verification Flow

### 1. User Registration
```
POST /api/users/register
→ Sends verification email with token
→ Returns 200 OK
```

### 2. Email Verification
User clicks link in email:
```
http://localhost:3000/verify-email?token=xxxxx
→ Calls GET /api/auth/verify-email?token=xxxxx
→ Marks user as verified
→ Redirects to login
```

### 3. Password Reset
User requests password reset:
```
POST /api/auth/forgot-password
→ Sends reset email with token (1 hour expiry)

User clicks link:
http://localhost:3000/reset-password?token=xxxxx
→ Calls GET /api/auth/verify-reset-token
→ Shows password reset form
→ Calls POST /api/auth/reset-password
```

## Debugging Email Issues

### Check Logs
Look for these log messages:

**Success:**
```
📧 Attempting to send verification email to: test@example.com
✅ Verification email sent successfully to: test@example.com
```

**Failure:**
```
❌ Email sending failed: Connection refused
```

### Common Issues

| Issue | Solution |
|-------|----------|
| "Connection refused" | SMTP host/port wrong or email service offline |
| "Authentication failed" | Invalid email credentials |
| "TLS required" | Enable TLS in SMTP settings (already done) |
| "Certificate error" | Disable SSL certificate verification (not recommended for production) |

### Enable Debug Logging

Add to `application.yml`:
```yaml
logging:
  level:
    org.springframework.mail: DEBUG
    com.carboncredit.service.EmailService: DEBUG
    com.carboncredit.service.UserService: DEBUG
```

## Production Deployment

For production, use one of these approaches:

### 1. Environment Variables (Recommended)
Set `EMAIL_USERNAME` and `EMAIL_PASSWORD` in your hosting environment.

### 2. Secrets Management
- Use AWS Secrets Manager
- Use Azure Key Vault
- Use HashiCorp Vault

### 3. application-prod.yml
Create a production configuration file that is not committed to Git.

## Email Templates

Currently using plain text emails. Templates are in `EmailService`:

- `buildVerificationEmailBody()` - Email verification
- `buildWelcomeEmailBody()` - Welcome after verification
- `buildPasswordResetEmailBody()` - Password reset link
- `buildPasswordResetConfirmationEmailBody()` - Password changed confirmation

To customize, edit the methods in `EmailService.java`.

## Summary

**Current Flow:**
```
User Register → Generate Token → Send Email → User Verifies → Login
```

**Requirements:**
- ✅ Backend implementation complete
- ✅ Frontend pages complete
- ✅ Database schema updated
- ❌ Email credentials needed (for testing)

**Next Step:** Set environment variables and run the application!
