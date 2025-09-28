-- Creating tables for Carbon Credit Marketplace

-- Users table (for all types: EV Owner, Buyer, CVA, Admin)
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('EV_OWNER', 'BUYER', 'CVA', 'ADMIN')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users (user_id, username, email, password_hash, role, created_at, updated_at)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'evowner1', 'evowner1@example.com', 'evowner', 'EV_OWNER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('22222222-2222-2222-2222-222222222222', 'buyer1',   'buyer1@example.com',   'buyer', 'BUYER',    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('33333333-3333-3333-3333-333333333333', 'cva1',     'cva1@example.com',     'cva1', 'CVA',      CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('44444444-4444-4444-4444-444444444444', 'admin1',   'admin1@example.com',   'admin1', 'ADMIN',    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- Insert test users with proper password hashes
INSERT INTO users (user_id, username, email, password_hash, role, created_at, updated_at) 
VALUES 
    (gen_random_uuid(), 'ev_owner_test', 'evowner@test.com', '$2a$10$test.hash.here', 'EV_OWNER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'buyer_test', 'buyer@test.com', '$2a$10$test.hash.here', 'BUYER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'cva_test', 'cva@test.com', '$2a$10$test.hash.here', 'CVA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  SELECT * FROM users;

-- EV Vehicles table
CREATE TABLE vehicles (
    vehicle_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    vin VARCHAR(17) UNIQUE NOT NULL,
    model VARCHAR(50),
    registration_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Journey Data table (stores EV trip data)
CREATE TABLE journey_data (
    journey_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID REFERENCES vehicles(vehicle_id),
    user_id UUID REFERENCES users(user_id),
    distance_km DECIMAL(10,2),
    energy_consumed_kwh DECIMAL(10,2),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    co2_reduced_kg DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Carbon Credits table
CREATE TABLE carbon_credits (
    credit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    journey_id UUID REFERENCES journey_data(journey_id),
    co2_reduced_kg DECIMAL(10,2) NOT NULL,
    credit_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'VERIFIED', 'LISTED', 'SOLD', 'REJECTED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP,
    listed_at TIMESTAMP
);

-- Carbon Credit Listings table (for fixed price or auction)
CREATE TABLE credit_listings (
    listing_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credit_id UUID REFERENCES carbon_credits(credit_id),
    listing_type VARCHAR(20) NOT NULL CHECK (listing_type IN ('FIXED', 'AUCTION')),
    price DECIMAL(10,2),
    min_bid DECIMAL(10,2),
    auction_end_time TIMESTAMP,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'CLOSED', 'CANCELLED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- add updated_at collum to the credit_listings table
ALTER TABLE credit_listings ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- A trigger to automatically update the timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_credit_listings_updated_at
    BEFORE UPDATE ON credit_listings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Transactions table
CREATE TABLE transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credit_id UUID REFERENCES carbon_credits(credit_id),
    listing_id UUID REFERENCES credit_listings(listing_id),
    buyer_id UUID REFERENCES users(user_id),
    seller_id UUID REFERENCES users(user_id),
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED', 'DISPUTED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- Wallet table (for carbon credits and payments)
CREATE TABLE wallets (
    wallet_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    credit_balance DECIMAL(10,2) DEFAULT 0.0,
    cash_balance DECIMAL(10,2) DEFAULT 0.0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Certificates table (for buyers)
CREATE TABLE certificates (
    certificate_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID REFERENCES transactions(transaction_id),
    buyer_id UUID REFERENCES users(user_id),
    credit_id UUID REFERENCES carbon_credits(credit_id),
    issue_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    co2_reduced_kg DECIMAL(10,2) NOT NULL,
    certificate_code VARCHAR(50) UNIQUE NOT NULL
);

-- Audit Logs table (for CVA verification)
CREATE TABLE audit_logs (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credit_id UUID REFERENCES carbon_credits(credit_id),
    verifier_id UUID REFERENCES users(user_id),
    action VARCHAR(50) NOT NULL CHECK (action IN ('SUBMITTED', 'VERIFIED', 'REJECTED')),
    comments TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Disputes table (for transaction disputes)
CREATE TABLE disputes (
    dispute_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID REFERENCES transactions(transaction_id),
    raised_by_id UUID REFERENCES users(user_id),
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'RESOLVED', 'CLOSED')),
    resolution TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

ALTER TABLE disputes ADD COLUMN resolved_by_id UUID REFERENCES users(user_id);

-- Price Suggestions table (for AI price recommendations)
CREATE TABLE price_suggestions (
    suggestion_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credit_id UUID REFERENCES carbon_credits(credit_id),
    suggested_price DECIMAL(10,2),
    market_avg_price DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Reports table (for admin and user reports)
CREATE TABLE reports (
    report_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    report_type VARCHAR(50) NOT NULL CHECK (report_type IN ('USER_CO2', 'TRANSACTION', 'PLATFORM_SUMMARY')),
    data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance optimization
CREATE INDEX idx_user_role ON users(role);
CREATE INDEX idx_vehicle_user_id ON vehicles(user_id);
CREATE INDEX idx_journey_vehicle_id ON journey_data(vehicle_id);
CREATE INDEX idx_credit_user_id ON carbon_credits(user_id);
CREATE INDEX idx_listing_credit_id ON credit_listings(credit_id);
CREATE INDEX idx_transaction_buyer_id ON transactions(buyer_id);
CREATE INDEX idx_transaction_seller_id ON transactions(seller_id);
CREATE INDEX idx_wallet_user_id ON wallets(user_id);
CREATE INDEX idx_certificate_buyer_id ON certificates(buyer_id);
CREATE INDEX idx_audit_credit_id ON audit_logs(credit_id);

DROP TABLE price_suggestions;
DROP TABLE reports;

CREATE TABLE payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID REFERENCES transactions(transaction_id),
    payer_id UUID REFERENCES users(user_id),
    payee_id UUID REFERENCES users(user_id),
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    payment_reference VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
);

ALTER TABLE notifications ADD COLUMN related_entity_id UUID, ADD COLUMN related_entity_type VARCHAR(50);