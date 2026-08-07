-- ======================================================
-- MIGRATION SCRIPT v2 (Professional Upgrades)
-- ======================================================
USE HotelMs;
GO

-- 1. Nâng cấp bảng KhachHang (CRM Profiles)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('KhachHang') AND name = 'isBlacklist')
BEGIN
    ALTER TABLE KhachHang ADD isBlacklist BIT DEFAULT 0;
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('KhachHang') AND name = 'preferences')
BEGIN
    ALTER TABLE KhachHang ADD preferences NVARCHAR(MAX);
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('KhachHang') AND name = 'vipLevel')
BEGIN
    ALTER TABLE KhachHang ADD vipLevel VARCHAR(20) DEFAULT 'BRONZE';
END
GO

-- 2. Nâng cấp bảng DatPhong (Professional Logic)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DatPhong') AND name = 'hanNopCoc')
BEGIN
    ALTER TABLE DatPhong ADD hanNopCoc DATETIME;
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DatPhong') AND name = 'phiHuyPhong')
BEGIN
    ALTER TABLE DatPhong ADD phiHuyPhong DECIMAL(18,2) DEFAULT 0;
END
GO

PRINT '✓ Migration v2 completed successfully!';
GO
