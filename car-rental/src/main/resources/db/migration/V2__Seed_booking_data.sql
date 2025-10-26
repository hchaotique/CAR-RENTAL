-- Seed basic booking data for testing
-- Run this after V1__Initial_schema.sql

USE [car_rental]
GO

-- Insert test users
INSERT INTO [users] ([email], [phone], [role], [is_active], [password_hash], [twofa_enabled], [created_at], [updated_at], [version])
VALUES
('customer1@example.com', '+84123456789', 'CUSTOMER', 1, '$2a$10$dummy.hash.for.customer1', 0, SYSUTCDATETIME(), SYSUTCDATETIME(), 0),
('host1@example.com', '+84987654321', 'HOST', 1, '$2a$10$dummy.hash.for.host1', 0, SYSUTCDATETIME(), SYSUTCDATETIME(), 0);

-- Get user IDs
DECLARE @customerId BIGINT = (SELECT id FROM users WHERE email = 'customer1@example.com');
DECLARE @hostId BIGINT = (SELECT id FROM users WHERE email = 'host1@example.com');

-- Insert user profiles with KYC verified for customer
INSERT INTO [user_profiles] ([user_id], [full_name], [kyc_status], [created_at], [updated_at])
VALUES
(@customerId, 'Test Customer', 'VERIFIED', SYSUTCDATETIME(), SYSUTCDATETIME()),
(@hostId, 'Test Host', 'PENDING', SYSUTCDATETIME(), SYSUTCDATETIME());

-- Insert test car
INSERT INTO [cars] ([owner_id], [make], [model], [transmission], [fuel_type], [seats], [status], [created_at], [updated_at], [version])
VALUES
(@hostId, 'Toyota', 'Camry', 'AUTO', 'GAS', 5, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), 0);

-- Get car ID
DECLARE @carId BIGINT = (SELECT id FROM cars WHERE owner_id = @hostId);

-- Insert car listing with ACTIVE status, price, and location
INSERT INTO [car_listings] ([vehicle_id], [title], [price_24h_cents], [instant_book], [status], [home_location], [created_at], [updated_at], [version])
VALUES
(@carId, 'Toyota Camry for rent', 50000, 1, 'ACTIVE', geography::Point(10.762622, 106.660172, 4326), SYSUTCDATETIME(), SYSUTCDATETIME(), 0);

-- Get listing ID
DECLARE @listingId BIGINT = (SELECT id FROM car_listings WHERE vehicle_id = @carId);

-- Seed availability calendar for next 365 days (FREE status)
DECLARE @startDate DATE = CAST(GETDATE() AS DATE);
DECLARE @endDate DATE = DATEADD(day, 364, @startDate);
DECLARE @currentDate DATE = @startDate;

WHILE @currentDate <= @endDate
BEGIN
    INSERT INTO [availability_calendar] ([listing_id], [day], [status])
    VALUES (@listingId, @currentDate, 'FREE');

    SET @currentDate = DATEADD(day, 1, @currentDate);
END;

-- Insert addons for the listing
INSERT INTO [addons] ([listing_id], [name], [charge_mode], [price_cents])
VALUES
(@listingId, 'GPS Navigation', 'PER_TRIP', 5000),
(@listingId, 'Child Seat', 'PER_DAY', 2000);

PRINT 'Booking test data seeded successfully';
PRINT 'Customer ID: ' + CAST(@customerId AS VARCHAR(10));
PRINT 'Host ID: ' + CAST(@hostId AS VARCHAR(10));
PRINT 'Listing ID: ' + CAST(@listingId AS VARCHAR(10));
GO
