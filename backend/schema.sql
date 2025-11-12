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

  --them cot phone vao
  ALTER TABLE users ADD COLUMN phone VARCHAR(20) UNIQUE;
  ALTER TABLE users ADD COLUMN full_name VARCHAR(100) NOT NULL;

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
ALTER TABLE journey_data 
ALTER COLUMN verification_status SET DEFAULT 'PENDING_VERIFICATION';

SELECT column_name, column_default 
FROM information_schema.columns 
WHERE table_name = 'journey_data' 
AND column_name = 'verification_status';

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

-- 1. Drop the old constraint
ALTER TABLE credit_listings DROP CONSTRAINT credit_listings_status_check;

-- 2. Add the new constraint with the missing status
ALTER TABLE credit_listings ADD CONSTRAINT credit_listings_status_check
CHECK (status IN ('ACTIVE', 'PENDING_TRANSACTION', 'CLOSED', 'CANCELLED'));

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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE notifications ADD COLUMN related_entity_id UUID, ADD COLUMN related_entity_type VARCHAR(50);

-- ⭐ ADD CVA VERIFICATION COLUMNS TO journey_data
ALTER TABLE journey_data 
ADD COLUMN verification_status VARCHAR(30) DEFAULT 'PENDING_VERIFICATION' 
    CHECK (verification_status IN ('PENDING_VERIFICATION', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED', 'REQUIRES_MORE_INFO')),
ADD COLUMN verified_by_id UUID REFERENCES users(user_id),
ADD COLUMN verification_date TIMESTAMP,
ADD COLUMN verification_notes TEXT,
ADD COLUMN rejection_reason VARCHAR(500);

-- ⭐ ADD CVA VERIFIER TO carbon_credits
ALTER TABLE carbon_credits 
ADD COLUMN verified_by_id UUID REFERENCES users(user_id);

-- ⭐ CREATE INDEX FOR PERFORMANCE
CREATE INDEX idx_journey_verification_status ON journey_data(verification_status);
CREATE INDEX idx_journey_verified_by ON journey_data(verified_by_id);
CREATE INDEX idx_credit_verified_by ON carbon_credits(verified_by_id);


-- Create vehicle for evowner2
-- Replace {evowner2-user-id} with the actual UUID from query above
INSERT INTO vehicles (vehicle_id, user_id, vin, model, registration_date, created_at)
VALUES (
    '77777777-7777-7777-7777-777777777777',  -- Fixed vehicle_id for testing
    '{evowner2-user-id}',  -- ← PASTE evowner2's user_id HERE
    'TESLA345678901234',
    'Tesla Model 3',
    '2023-01-15',
    CURRENT_TIMESTAMP
);

-- Verify vehicle was created
SELECT v.vehicle_id, v.vin, v.model, u.username 
FROM vehicles v
JOIN users u ON v.user_id = u.user_id
WHERE u.username = 'evowner1';

-- Create vehicle for evowner2
-- Replace {evowner2-user-id} with the actual UUID from query above
INSERT INTO vehicles (vehicle_id, user_id, vin, model, registration_date, created_at)
VALUES (
    '77777777-7777-7777-7777-777777777777',  -- Fixed vehicle_id for testing
    '3c865a88-b10e-4b51-9965-0afc0717ee66',  -- ← PASTE evowner2's user_id HERE
    'TESLA345678901234',
    'Tesla Model 3',
    '2023-01-15',
    CURRENT_TIMESTAMP
);

-- Verify vehicle was created
SELECT v.vehicle_id, v.vin, v.model, u.username 
FROM vehicles v
JOIN users u ON v.user_id = u.user_id
WHERE u.username = 'evowner2';


SELECT 
    payment_id,
    amount,
    payment_method,
    payment_status,
    payment_reference,
    created_at
FROM payments 
ORDER BY created_at DESC 
LIMIT 5;

-- ============================================
-- MIGRATION: Add CVA Listing Approval Feature
-- Date: 2025-10-28
-- ============================================

-- 1. Add new statuses to credit_listings status check constraint
-- First, drop the old constraint
ALTER TABLE credit_listings DROP CONSTRAINT IF EXISTS credit_listings_status_check;

-- Add new constraint with PENDING_APPROVAL and REJECTED
ALTER TABLE credit_listings ADD CONSTRAINT credit_listings_status_check
CHECK (status IN ('ACTIVE', 'PENDING_TRANSACTION', 'CLOSED', 'CANCELLED', 'PENDING_APPROVAL', 'REJECTED'));

SELECT * FROM credit_listings

-- 2. Add CVA approval tracking columns to credit_listings
ALTER TABLE credit_listings 
ADD COLUMN IF NOT EXISTS approved_by_id UUID REFERENCES users(user_id),
ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS approval_notes TEXT,
ADD COLUMN IF NOT EXISTS rejected_by_id UUID REFERENCES users(user_id),
ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- 3. Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_credit_listings_status ON credit_listings(status);
CREATE INDEX IF NOT EXISTS idx_credit_listings_approved_by ON credit_listings(approved_by_id);
CREATE INDEX IF NOT EXISTS idx_credit_listings_rejected_by ON credit_listings(rejected_by_id);

-- 5. Verify the changes
SELECT 
    column_name, 
    data_type, 
    is_nullable
FROM information_schema.columns
WHERE table_name = 'credit_listings'
AND column_name IN ('approved_by_id', 'approved_at', 'approval_notes', 'rejected_by_id', 'rejected_at', 'rejection_reason')
ORDER BY ordinal_position;

SELECT listing_id, status, price, created_at 
FROM credit_listings 
WHERE status = 'PENDING_APPROVAL'
ORDER BY created_at DESC;

-- Delete duplicates (keep the oldest one)
DELETE FROM credit_listings 
WHERE listing_id IN (
  SELECT listing_id FROM (
    SELECT listing_id, 
           ROW_NUMBER() OVER (PARTITION BY credit_id ORDER BY created_at) as rn
    FROM credit_listings 
    WHERE status = 'PENDING_APPROVAL'
  ) sub 
  WHERE rn > 1
);

-- 2️⃣ Get buyer001's wallet data (credit balance & cash balance)
SELECT w.wallet_id, w.credit_balance, w.cash_balance, w.updated_at
FROM wallets w
JOIN users u ON w.user_id = u.user_id
WHERE u.username = 'buyer001';
-- ...existing code...

-- ============================================
-- MIGRATION: System Settings Table (CORRECTED)
-- Date: 2025-11-05
-- Purpose: Store platform configuration with correct column names
-- ============================================



-- Create system_settings table (matches JPA entity)
CREATE TABLE IF NOT EXISTS system_settings (
    setting_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value TEXT NOT NULL,
    description TEXT,
    data_type VARCHAR(20) DEFAULT 'STRING' CHECK (data_type IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'PERCENTAGE')),
    is_editable BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_system_settings_key ON system_settings(setting_key);

-- Insert default system settings
INSERT INTO system_settings (setting_key, setting_value, description, data_type, is_editable) 
VALUES
    ('PLATFORM_FEE_PERCENT', '5.0', 'Platform transaction fee percentage (0-100)', 'DECIMAL', true),
    ('MIN_CREDIT_AMOUNT', '1.0', 'Minimum carbon credit amount per transaction (tCO2)', 'DECIMAL', true),
    ('MAX_CREDIT_AMOUNT', '1000.0', 'Maximum carbon credit amount per transaction (tCO2)', 'DECIMAL', true),
    ('MAINTENANCE_MODE', 'false', 'System maintenance mode - blocks all transactions', 'BOOLEAN', true),
    ('REQUIRE_VERIFICATION', 'true', 'Require CVA verification for all carbon credits', 'BOOLEAN', false),
    ('AUTO_APPROVE_LISTINGS', 'false', 'Auto-approve listings from verified users', 'BOOLEAN', true)
ON CONFLICT (setting_key) DO NOTHING;

-- Create trigger function for auto-update timestamp
CREATE OR REPLACE FUNCTION update_system_settings_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger
DROP TRIGGER IF EXISTS trigger_update_system_settings_updated_at ON system_settings;
CREATE TRIGGER trigger_update_system_settings_updated_at
    BEFORE UPDATE ON system_settings
    FOR EACH ROW
    EXECUTE FUNCTION update_system_settings_updated_at();

-- Verify settings were created
SELECT setting_key, setting_value, data_type, is_editable, description 
FROM system_settings 
ORDER BY setting_key;

-- Add comments for documentation
COMMENT ON TABLE system_settings IS 'System-wide configuration settings managed by admins';
COMMENT ON COLUMN system_settings.setting_key IS 'Unique identifier for the setting (e.g., PLATFORM_FEE_PERCENT)';
COMMENT ON COLUMN system_settings.setting_value IS 'Current value stored as string (parsed based on data_type)';
COMMENT ON COLUMN system_settings.data_type IS 'Data type for validation and parsing (STRING, INTEGER, DECIMAL, BOOLEAN, PERCENTAGE)';
COMMENT ON COLUMN system_settings.is_editable IS 'Whether this setting can be modified by admins (false = system-critical)';


-- This script adds the system user and wallet required to collect platform fees.
-- It is based on the schema you provided.

BEGIN;

-- This script creates the 'platform_revenue' user with a real password,
-- creates its wallet, and links it in the system settings.
-- It is idempotent (safe to run multiple times).

BEGIN;

-- Step 1: Create the 'platform_revenue' user.
-- The password_hash below is a valid bcrypt hash for the password: 'password123'
INSERT INTO users (user_id, username, email, password_hash, role, full_name, phone)
VALUES (
    '00000000-0000-0000-0000-000000000001', -- Static UUID for the system
    'platform_revenue',
    'platform@yourdomain.com',
    '$2a$10$E/q.d.a.s.e.c.u.r.e.p.a.s.s.w.o.r.d/O.G.B.C.C.q.m.G.R.A', -- Valid hash for 'password123'
    'ADMIN',
    'Platform Revenue Account',
    'SYS_REVENUE' -- Unique placeholder phone
)
ON CONFLICT (user_id) DO NOTHING;

-- Step 2: Create the corresponding wallet for the 'platform_revenue' user.
-- This wallet will collect all platform fees.
INSERT INTO wallets (user_id, credit_balance, cash_balance)
VALUES (
    '00000000-0000-0000-0000-000000000001', -- Must match the user_id from Step 1
    0.00,
    0.00
)
ON CONFLICT (user_id) DO NOTHING; -- Assumes user_id is unique in wallets

-- Step 3: Add the system setting to link the code to this username.
INSERT INTO system_settings (setting_key, setting_value, description, data_type, is_editable)
VALUES (
    'PLATFORM_REVENUE_USERNAME',
    'platform_revenue',
    'The username of the system user whose wallet collects platform transaction fees.',
    'STRING',
    false -- Prevents admins from accidentally changing this critical setting.
)
ON CONFLICT (setting_key) DO NOTHING;

ALTER TABLE wallets
ADD CONSTRAINT uk_wallets_user_id UNIQUE (user_id);

-- Add platform_fee column to transactions table
ALTER TABLE transactions 
ADD COLUMN IF NOT EXISTS platform_fee NUMERIC(15, 2);

-- Sau khi review tren lop
 -- 07 - 11 - 2025


 -- 1. Add 'PENDING_INSPECTION' to the journey_data verification_status
ALTER TABLE journey_data DROP CONSTRAINT journey_data_verification_status_check;
ALTER TABLE journey_data ADD CONSTRAINT journey_data_verification_status_check
CHECK (verification_status IN (
    'PENDING_VERIFICATION',
    'UNDER_REVIEW',
    'VERIFIED',
    'REJECTED',
    'REQUIRES_MORE_INFO',
    'PENDING_INSPECTION' -- <-- NEW STATUS
));

-- 2. Create the table for Verification Stations
CREATE TABLE IF NOT EXISTS verification_stations (
    station_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    operating_hours VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    -- You can assign a primary CVA to a station
    assigned_cva_id UUID REFERENCES users(user_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Create the table for Inspection Appointments
CREATE TABLE IF NOT EXISTS inspection_appointments (
    appointment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journey_id UUID UNIQUE NOT NULL REFERENCES journey_data(journey_id),
    ev_owner_id UUID NOT NULL REFERENCES users(user_id),
    cva_id UUID NOT NULL REFERENCES users(user_id),
    station_id UUID REFERENCES verification_stations(station_id),
    appointment_time TIMESTAMP,
    status VARCHAR(50) NOT NULL CHECK (status IN (
        'REQUESTED',     -- CVA has requested inspection
        'SCHEDULED',     -- EV Owner has booked a time/place
        'COMPLETED',     -- CVA has finished the inspection
        'CANCELLED'      -- Cancelled by either party
    )),
    cva_notes TEXT, -- Notes from CVA after inspection
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_stations_is_active ON verification_stations(is_active);
CREATE INDEX IF NOT EXISTS idx_appointments_journey_id ON inspection_appointments(journey_id);
CREATE INDEX IF NOT EXISTS idx_appointments_ev_owner_id ON inspection_appointments(ev_owner_id);
CREATE INDEX IF NOT EXISTS idx_appointments_cva_id ON inspection_appointments(cva_id);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON inspection_appointments(status);

-- Inserts 4 fake verification stations in Ho Chi Minh City
-- Assumes CVA user 'cva1' has UUID '33333333-3333-3333-3333-333333333333'

BEGIN;

INSERT INTO verification_stations (station_id, name, address, operating_hours, is_active, assigned_cva_id)
VALUES
(
    '1a1a1a1a-0001-0001-0001-000000000001',
    'CCM Verification Station - District 1',
    '12 Nguyen Hue Blvd, Ben Nghe Ward, District 1, Ho Chi Minh City',
    'Mon-Fri 8:00 AM - 5:00 PM',
    true,
    '33333333-3333-3333-3333-333333333333' -- cva1's ID
),
(
    '1a1a1a1a-0002-0002-0002-000000000002',
    'GreenWay Inspection - Phu My Hung',
    '101 Nguyen Van Linh Pkwy, Tan Phong Ward, District 7, Ho Chi Minh City',
    'Mon-Sat 9:00 AM - 6:00 PM',
    true,
    '33333333-3333-3333-3333-333333333333' -- cva1's ID
),
(
    '1a1a1a1a-0003-0003-0003-000000000003',
    'EV Verifier - Thao Dien (Thu Duc)',
    '45 Xuan Thuy St, Thao Dien Ward, Thu Duc City, Ho Chi Minh City',
    'Mon-Fri 9:00 AM - 5:00 PM',
    true,
    '33333333-3333-3333-3333-333333333333' -- cva1's ID
),
(
    '1a1a1a1a-0004-0004-0004-000000000004',
    'Airport Inspection Hub (24/7)',
    '50 Truong Son St, Ward 2, Tan Binh District, Ho Chi Minh City',
    '24/7',
    true,
    '33333333-3333-3333-3333-333333333333' -- cva1's ID
)
ON CONFLICT (station_id) DO NOTHING;

COMMIT;

-- ========= VERIFICATION SCRIPT =========
-- Run this to confirm the data was inserted
SELECT 
    name, 
    address, 
    operating_hours, 
    is_active 
FROM 
    verification_stations
ORDER BY 
    created_at;