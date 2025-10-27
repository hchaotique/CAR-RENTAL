-- Add home_city column with enum constraint and backfill from home_location

USE [car_rental]
GO

-- Add home_city column
ALTER TABLE [car_listings] ADD [home_city] VARCHAR(20);
GO

-- Add check constraint for valid cities
ALTER TABLE [car_listings] ADD CONSTRAINT [chk_city] CHECK ([home_city] IN ('HA_NOI', 'HUE', 'HO_CHI_MINH', 'HAI_PHONG'));
GO

-- Define city centroids (approximate center coordinates)
DECLARE @haNoi GEOGRAPHY = geography::Point(21.0278, 105.8342, 4326);        -- Hà Nội
DECLARE @hue GEOGRAPHY = geography::Point(16.4637, 107.5909, 4326);          -- Huế
DECLARE @hoChiMinh GEOGRAPHY = geography::Point(10.7626, 106.6602, 4326);    -- Hồ Chí Minh
DECLARE @haiPhong GEOGRAPHY = geography::Point(20.8449, 106.6881, 4326);     -- Hải Phòng

-- Backfill home_city based on closest centroid
UPDATE [car_listings]
SET [home_city] = CASE
    WHEN [home_location].STDistance(@haNoi) <= [home_location].STDistance(@hue)
         AND [home_location].STDistance(@haNoi) <= [home_location].STDistance(@hoChiMinh)
         AND [home_location].STDistance(@haNoi) <= [home_location].STDistance(@haiPhong)
    THEN 'HA_NOI'

    WHEN [home_location].STDistance(@hue) <= [home_location].STDistance(@haNoi)
         AND [home_location].STDistance(@hue) <= [home_location].STDistance(@hoChiMinh)
         AND [home_location].STDistance(@hue) <= [home_location].STDistance(@haiPhong)
    THEN 'HUE'

    WHEN [home_location].STDistance(@hoChiMinh) <= [home_location].STDistance(@haNoi)
         AND [home_location].STDistance(@hoChiMinh) <= [home_location].STDistance(@hue)
         AND [home_location].STDistance(@hoChiMinh) <= [home_location].STDistance(@haiPhong)
    THEN 'HO_CHI_MINH'

    WHEN [home_location].STDistance(@haiPhong) <= [home_location].STDistance(@haNoi)
         AND [home_location].STDistance(@haiPhong) <= [home_location].STDistance(@hue)
         AND [home_location].STDistance(@haiPhong) <= [home_location].STDistance(@hoChiMinh)
    THEN 'HAI_PHONG'

    ELSE 'HO_CHI_MINH' -- Default fallback
END
WHERE [home_city] IS NULL;
GO

-- Make home_city NOT NULL after backfill
ALTER TABLE [car_listings] ALTER COLUMN [home_city] VARCHAR(20) NOT NULL;
GO

-- Create index for performance
CREATE NONCLUSTERED INDEX [ix_listing_city_status_price] ON [car_listings]
(
    [home_city] ASC,
    [status] ASC,
    [price_24h_cents] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY];
GO

-- Update existing seed data to include home_city
UPDATE [car_listings]
SET [home_city] = 'HO_CHI_MINH'
WHERE [home_location].STDistance(geography::Point(10.762622, 106.660172, 4326)) < 10000; -- Within 10km of HCM centroid

PRINT 'City column added and backfilled successfully';
PRINT 'Index created for (home_city, status, price_24h_cents)';
GO
