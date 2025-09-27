-- V1__Initial_schema.sql
-- Users table
CREATE TABLE users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    email VARCHAR(190) NOT NULL UNIQUE,
    phone VARCHAR(30),
    role VARCHAR(16) NOT NULL,
    is_active BIT NOT NULL DEFAULT 1,
    password_hash VARCHAR(255) NOT NULL,
    twofa_enabled BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    version INT NOT NULL DEFAULT 0
);

-- UserProfiles table
CREATE TABLE user_profiles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    full_name NVARCHAR(120) NOT NULL,
    dob DATE,
    driver_license_no VARBINARY(256),
    driver_license_expiry DATE,
    address_encrypted VARBINARY(512),
    kyc_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Vehicles table
CREATE TABLE vehicles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    vin_encrypted VARBINARY(256),
    plate_masked VARCHAR(16),
    make NVARCHAR(50) NOT NULL,
    model NVARCHAR(80) NOT NULL,
    model_year SMALLINT,
    transmission VARCHAR(10) NOT NULL,
    fuel_type VARCHAR(10) NOT NULL,
    seats TINYINT NOT NULL DEFAULT 5,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    daily_price DECIMAL(10,2),
    image_url NVARCHAR(500),
    city NVARCHAR(100),
    rating DECIMAL(3,2) DEFAULT 4.5,
    num_reviews INT DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    version INT NOT NULL DEFAULT 0,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Indexes
CREATE INDEX ix_vehicle_owner_status ON vehicles (owner_id, status);
CREATE INDEX ix_users_email ON users (email);
