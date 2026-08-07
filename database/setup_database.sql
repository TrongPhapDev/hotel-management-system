-- ======================================================
-- HỆ THỐNG QUẢN LÝ KHÁCH SẠN (HOTEL MS)
-- Consolidated Database Script (Final Version for Submission)
-- ======================================================

IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'HotelMs')
BEGIN
    CREATE DATABASE HotelMs;
END
GO

USE HotelMs;
GO

-- ======================================================
-- 0. DỌN DẸP DỮ LIỆU CŨ (CLEANUP)
-- ======================================================
IF OBJECT_ID('ChiTietKiemTien', 'U') IS NOT NULL DROP TABLE ChiTietKiemTien;
IF OBJECT_ID('ChiPhi', 'U') IS NOT NULL DROP TABLE ChiPhi;
IF OBJECT_ID('GiaoCa', 'U') IS NOT NULL DROP TABLE GiaoCa;
IF OBJECT_ID('NhatKyHeThong', 'U') IS NOT NULL DROP TABLE NhatKyHeThong;
IF OBJECT_ID('ChiTietHoaDon', 'U') IS NOT NULL DROP TABLE ChiTietHoaDon;
IF OBJECT_ID('HoaDon', 'U') IS NOT NULL DROP TABLE HoaDon;
IF OBJECT_ID('KhuyenMai', 'U') IS NOT NULL DROP TABLE KhuyenMai;
IF OBJECT_ID('SuDungDichVu', 'U') IS NOT NULL DROP TABLE SuDungDichVu;
IF OBJECT_ID('DichVu', 'U') IS NOT NULL DROP TABLE DichVu;
IF OBJECT_ID('ChiTietDatPhong', 'U') IS NOT NULL DROP TABLE ChiTietDatPhong;
IF OBJECT_ID('DatPhong', 'U') IS NOT NULL DROP TABLE DatPhong;
IF OBJECT_ID('KenhDatPhong', 'U') IS NOT NULL DROP TABLE KenhDatPhong;
IF OBJECT_ID('KhachHang', 'U') IS NOT NULL DROP TABLE KhachHang;
IF OBJECT_ID('LichSuTrangThaiPhong', 'U') IS NOT NULL DROP TABLE LichSuTrangThaiPhong;
IF OBJECT_ID('Phong', 'U') IS NOT NULL DROP TABLE Phong;
IF OBJECT_ID('TienNghi_LoaiPhong', 'U') IS NOT NULL DROP TABLE TienNghi_LoaiPhong;
IF OBJECT_ID('TienNghi', 'U') IS NOT NULL DROP TABLE TienNghi;
IF OBJECT_ID('ChiTietBangGia', 'U') IS NOT NULL DROP TABLE ChiTietBangGia;
IF OBJECT_ID('LoaiPhong', 'U') IS NOT NULL DROP TABLE LoaiPhong;
IF OBJECT_ID('HuongNhin', 'U') IS NOT NULL DROP TABLE HuongNhin;
IF OBJECT_ID('BangGia', 'U') IS NOT NULL DROP TABLE BangGia;
IF OBJECT_ID('TaiKhoan', 'U') IS NOT NULL DROP TABLE TaiKhoan;
IF OBJECT_ID('NhanVien', 'U') IS NOT NULL DROP TABLE NhanVien;
GO

-- ======================================================
-- 1. CẤU TRÚC BẢNG (SCHEMA)
-- ======================================================

-- 1.1 Nhật ký hệ thống
CREATE TABLE NhatKyHeThong (
    maLog INT IDENTITY(1,1) PRIMARY KEY,
    thoiGian DATETIME DEFAULT GETDATE(),
    tenDangNhap VARCHAR(50), 
    hanhDong NVARCHAR(50),    
    doiTuong NVARCHAR(100),   
    chiTiet NVARCHAR(MAX)     
);

-- 1.2 Nhân viên & Giao ca
CREATE TABLE NhanVien (
    maNhanVien VARCHAR(20) PRIMARY KEY,
    hoTen NVARCHAR(100) NOT NULL,
    sdt VARCHAR(20),
    chucVu NVARCHAR(50),
    email NVARCHAR(100),
    cccd NVARCHAR(20),
    ngaySinh DATE,
    gioiTinh NVARCHAR(10),
    diaChi NVARCHAR(255),
    ngayVaoLam DATE,
    dangLamViec BIT DEFAULT 1,
    luongCoBan DECIMAL(18, 2) DEFAULT 0
);

CREATE TABLE TaiKhoan (
    tenDangNhap VARCHAR(50) PRIMARY KEY,
    maNhanVien VARCHAR(20) UNIQUE REFERENCES NhanVien(maNhanVien),
    matKhau VARCHAR(100) NOT NULL,
    vaiTro VARCHAR(20) CHECK (vaiTro IN ('ADMIN', 'MANAGER', 'RECEPTIONIST')),
    trangThai BIT DEFAULT 1,
    lanDangNhapCuoi DATETIME
);

CREATE TABLE GiaoCa (
    maGiaoCa VARCHAR(20) PRIMARY KEY,
    maNhanVien VARCHAR(20) REFERENCES NhanVien(maNhanVien), 
    thoiGianBatDau DATETIME NOT NULL,
    thoiGianKetThuc DATETIME, 
    tienMatDauCa DECIMAL(18,2) DEFAULT 0,
    tienMatThuTrongCa DECIMAL(18,2) DEFAULT 0,
    tienMatBanGiao DECIMAL(18,2) DEFAULT 0,
    tienMatChenhLech DECIMAL(18,2) DEFAULT 0,
    maNhanVienNhan VARCHAR(20) REFERENCES NhanVien(maNhanVien), 
    ghiChu NVARCHAR(MAX),
    trangThai VARCHAR(20) DEFAULT 'OPEN' CHECK (trangThai IN ('OPEN', 'CLOSED'))
);

CREATE TABLE ChiPhi (
    maChiPhi INT IDENTITY(1,1) PRIMARY KEY,
    maNhanVien VARCHAR(20) REFERENCES NhanVien(maNhanVien),
    soTien DECIMAL(18,2) NOT NULL,
    lyDo NVARCHAR(MAX) NOT NULL,
    thoiGian DATETIME DEFAULT GETDATE(),
    maGiaoCa VARCHAR(20) REFERENCES GiaoCa(maGiaoCa)
);

CREATE TABLE ChiTietKiemTien (
    maGiaoCa VARCHAR(20) REFERENCES GiaoCa(maGiaoCa),
    menhGia INT NOT NULL, 
    soLuong INT NOT NULL,
    PRIMARY KEY (maGiaoCa, menhGia)
);

-- 1.3 Danh mục Phòng & Bảng giá
CREATE TABLE HuongNhin (
    maHuongNhin VARCHAR(20) PRIMARY KEY,
    tenHuongNhin NVARCHAR(100) NOT NULL,
    moTa NVARCHAR(MAX),
    heSoGia DECIMAL(18,2) DEFAULT 1.0,
    thuTu INT DEFAULT 0
);

CREATE TABLE LoaiPhong (
    maLoaiPhong VARCHAR(20) PRIMARY KEY,
    tenLoaiPhong NVARCHAR(100) NOT NULL,
    soNguoiToiDa INT,
    moTa NVARCHAR(MAX),
    giaTheoNgay DECIMAL(18,2)
);

CREATE TABLE TienNghi (
    maTienNghi VARCHAR(20) PRIMARY KEY,
    tenTienNghi NVARCHAR(100) NOT NULL,
    nhomTienNghi NVARCHAR(50),
    icon NVARCHAR(10),
    thuTu INT DEFAULT 0
);

CREATE TABLE TienNghi_LoaiPhong (
    maLoaiPhong VARCHAR(20) REFERENCES LoaiPhong(maLoaiPhong),
    maTienNghi VARCHAR(20) REFERENCES TienNghi(maTienNghi),
    PRIMARY KEY (maLoaiPhong, maTienNghi)
);

CREATE TABLE Phong (
    maPhong VARCHAR(20) PRIMARY KEY,
    tang INT,
    maHuongNhin VARCHAR(20) REFERENCES HuongNhin(maHuongNhin),
    maLoaiPhong VARCHAR(20) REFERENCES LoaiPhong(maLoaiPhong),
    trangThai VARCHAR(20) DEFAULT 'AVAILABLE' CHECK (trangThai IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'CLEANING'))
);

CREATE TABLE LichSuTrangThaiPhong (
    maLichSu INT IDENTITY(1,1) PRIMARY KEY,
    maPhong VARCHAR(20) REFERENCES Phong(maPhong),
    trangThaiCu VARCHAR(20),
    trangThaiMoi VARCHAR(20),
    thoiGianChuyen DATETIME DEFAULT GETDATE(),
    lyDo NVARCHAR(MAX)
);

CREATE TABLE BangGia (
    maBangGia VARCHAR(20) PRIMARY KEY,
    tenBangGia NVARCHAR(100) NOT NULL,
    ngayBatDau DATETIME,
    ngayKetThuc DATETIME,
    isKichHoat BIT DEFAULT 1,
    loaiBangGia VARCHAR(20) DEFAULT 'RACK' CHECK (loaiBangGia IN ('RACK', 'SEASONAL', 'CORPORATE', 'OTA', 'PROMOTION')),
    doiTuongApDung VARCHAR(20) DEFAULT 'ALL' CHECK (doiTuongApDung IN ('ALL', 'CA_NHAN', 'DOAN', 'CORPORATE', 'VIP')),
    mucUuTien INT DEFAULT 100,
    moTa NVARCHAR(MAX)
);

CREATE TABLE ChiTietBangGia (
    maChiTietBangGia INT IDENTITY(1,1) PRIMARY KEY,
    maBangGia VARCHAR(20) REFERENCES BangGia(maBangGia),
    maLoaiPhong VARCHAR(20) REFERENCES LoaiPhong(maLoaiPhong),
    giaTheoNgay DECIMAL(18,2),
    giaTheoGio DECIMAL(18,2),
    phuPhiTraTre DECIMAL(18,2),
    giaCuoiTuan DECIMAL(18,2) DEFAULT 0
);

-- 1.4 Khách hàng & Đặt phòng
CREATE TABLE KhachHang (
    maKhachHang VARCHAR(20) PRIMARY KEY,
    hoTen NVARCHAR(100) NOT NULL,
    sdt VARCHAR(20),
    cccd VARCHAR(20),
    ngaySinh DATE,
    gioiTinh NVARCHAR(10),
    quocTich NVARCHAR(50) DEFAULT N'Việt Nam',
    loaiGiayTo VARCHAR(20) DEFAULT 'CCCD' CHECK (loaiGiayTo IN ('CCCD', 'CMND', 'PASSPORT')),
    soHoChieu VARCHAR(30) NULL,
    soVisa VARCHAR(30) NULL,
    ngayHetHanVisa DATE NULL,
    noiCapHoChieu NVARCHAR(100) NULL,
    ngayNhapCanh DATE NULL
);

CREATE TABLE KenhDatPhong (
    maKenh VARCHAR(20) PRIMARY KEY,
    tenKenh NVARCHAR(100) NOT NULL,
    loaiKenh VARCHAR(20) CHECK (loaiKenh IN ('DIRECT', 'OTA', 'CORPORATE', 'TRAVEL_AGENT', 'OTHER')),
    heSoHoaHong DECIMAL(5,2) DEFAULT 0,
    trangThai BIT DEFAULT 1,
    moTa NVARCHAR(MAX)
);

CREATE TABLE DatPhong (
    maDatPhong VARCHAR(20) PRIMARY KEY,
    maKhachHang VARCHAR(20) REFERENCES KhachHang(maKhachHang),
    maNhanVien VARCHAR(20) REFERENCES NhanVien(maNhanVien),
    maKenh VARCHAR(20) DEFAULT 'DIRECT' REFERENCES KenhDatPhong(maKenh),
    ngayDat DATETIME DEFAULT GETDATE(),
    ngayNhanDuKien DATETIME,
    ngayTraDuKien DATETIME,
    soNguoi INT,
    tienDatCoc DECIMAL(18,2) DEFAULT 0,
    tongTienTamTinh DECIMAL(18,2) DEFAULT 0,
    trangThai VARCHAR(20) DEFAULT 'PENDING' CHECK (trangThai IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED', 'NO_SHOW', 'WAITLIST', 'PARTIALLY_CHECKED_IN')),
    ghiChu NVARCHAR(MAX),
    loaiKhach VARCHAR(10) DEFAULT 'CA_NHAN' CHECK (loaiKhach IN ('CA_NHAN', 'DOAN')),
    tenDoan NVARCHAR(200),
    maXacNhanKenh NVARCHAR(100) NULL,
    phiNoShow DECIMAL(18,2) DEFAULT 0,
    hanCheckIn DATETIME NULL,
    thuTuWaitlist INT DEFAULT 0
);

CREATE TABLE ChiTietDatPhong (
    maChiTiet VARCHAR(20) PRIMARY KEY,
    maDatPhong VARCHAR(20) REFERENCES DatPhong(maDatPhong),
    maPhong VARCHAR(20) REFERENCES Phong(maPhong),
    maKhachHang VARCHAR(20) REFERENCES KhachHang(maKhachHang),
    ngayNhanThucTe DATETIME,
    ngayTraThucTe DATETIME,
    giaThucTeChot DECIMAL(18,2),
    phuPhiPhatSinh DECIMAL(18,2) DEFAULT 0,
    daThanhToan BIT DEFAULT 0
);

-- 1.5 Dịch vụ & Hóa đơn
CREATE TABLE DichVu (
    maDichVu VARCHAR(20) PRIMARY KEY,
    tenDichVu NVARCHAR(100) NOT NULL,
    loai NVARCHAR(50),
    donGiaHienTai DECIMAL(18,2) NOT NULL,
    donViTinh NVARCHAR(20),
    soLuongMin INT DEFAULT 1,
    moTa NVARCHAR(MAX),
    trangThai BIT DEFAULT 1
);

CREATE TABLE SuDungDichVu (
    maSuDung VARCHAR(20) PRIMARY KEY,
    maChiTiet VARCHAR(20) REFERENCES ChiTietDatPhong(maChiTiet),
    maDichVu VARCHAR(20) REFERENCES DichVu(maDichVu),
    soLuong INT NOT NULL,
    donGiaLucDung DECIMAL(18,2),
    thoiGianDung DATETIME DEFAULT GETDATE()
);

CREATE TABLE KhuyenMai (
    maKhuyenMai VARCHAR(20) PRIMARY KEY,
    loaiGiam VARCHAR(10) CHECK (loaiGiam IN ('PERCENT', 'FIXED')),
    giaTriGiam DECIMAL(18,2),
    ngayBatDau DATETIME,
    ngayKetThuc DATETIME,
    dieuKienApDung NVARCHAR(MAX)
);

CREATE TABLE HoaDon (
    maHoaDon VARCHAR(20) PRIMARY KEY,
    maDatPhong VARCHAR(20) REFERENCES DatPhong(maDatPhong),
    maNhanVien VARCHAR(20) REFERENCES NhanVien(maNhanVien),
    maKhuyenMai VARCHAR(20) REFERENCES KhuyenMai(maKhuyenMai),
    ngayLap DATETIME DEFAULT GETDATE(),
    tongTienPhong DECIMAL(18,2) DEFAULT 0,
    tongTienDichVu DECIMAL(18,2) DEFAULT 0,
    tienDatCoc DECIMAL(18,2) DEFAULT 0,
    tienGiamKhuyenMai DECIMAL(18,2) DEFAULT 0,
    tongThanhToan DECIMAL(18,2) DEFAULT 0,
    trangThai VARCHAR(20) DEFAULT 'UNPAID' CHECK (trangThai IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'REFUNDED')),
    phuongThucThanhToan VARCHAR(20) DEFAULT 'CASH' CHECK (phuongThucThanhToan IN ('CASH', 'CARD', 'TRANSFER', 'DEPOSIT')),
    tenCongTy NVARCHAR(200) NULL,
    maSoThue VARCHAR(20) NULL,
    diaChiCongTy NVARCHAR(500) NULL
);

CREATE TABLE ChiTietHoaDon (
    maChiTiet INT IDENTITY(1,1) PRIMARY KEY,
    maHoaDon VARCHAR(20) REFERENCES HoaDon(maHoaDon),
    loai VARCHAR(10) CHECK (loai IN ('PHONG', 'DICH_VU', 'PHU_PHI', 'DAT_COC')),
    moTa NVARCHAR(MAX),
    soLuong INT,
    donGia DECIMAL(18,2),
    thanhTien DECIMAL(18,2),
    maNguonNoiBo VARCHAR(20) 
);

-- 1.6 INDEXES
CREATE INDEX IX_PHONG_LOAI         ON Phong(maLoaiPhong);
CREATE INDEX IX_PHONG_TRANGTHAI    ON Phong(trangThai);
CREATE INDEX IX_KHACH_SDT          ON KhachHang(sdt);
CREATE INDEX IX_KHACH_CCCD         ON KhachHang(cccd);
CREATE INDEX IX_KHACH_PASSPORT     ON KhachHang(soHoChieu);
CREATE INDEX IX_DATPHONG_KHACH     ON DatPhong(maKhachHang);
CREATE INDEX IX_DATPHONG_TRANGTHAI ON DatPhong(trangThai);
CREATE INDEX IX_DATPHONG_KENH      ON DatPhong(maKenh);
CREATE INDEX IX_HOADON_DATPHONG    ON HoaDon(maDatPhong);
GO

-- ======================================================
-- 2. DỮ LIỆU MẪU (DATA SEEDING)
-- ======================================================

-- 2.1 Kênh đặt phòng
INSERT INTO KenhDatPhong VALUES ('DIRECT',     N'Đặt trực tiếp',      'DIRECT',       0,    1, N'Khách walk-in hoặc gọi điện trực tiếp');
INSERT INTO KenhDatPhong VALUES ('WEBSITE',    N'Website khách sạn',  'DIRECT',       0,    1, N'Đặt qua website chính thức');
INSERT INTO KenhDatPhong VALUES ('BOOKING',    N'Booking.com',        'OTA',          15,   1, N'Đặt qua Booking.com');
INSERT INTO KenhDatPhong VALUES ('AGODA',      N'Agoda',              'OTA',          18,   1, N'Đặt qua Agoda');
INSERT INTO KenhDatPhong VALUES ('TRAVELOKA',  N'Traveloka',          'OTA',          12,   1, N'Đặt qua Traveloka');

-- 2.2 Nhân viên & Tài khoản
INSERT INTO NhanVien (maNhanVien, hoTen, sdt, chucVu, email, cccd, ngaySinh, gioiTinh, diaChi, ngayVaoLam, luongCoBan) 
VALUES ('admin_nv', N'Lê Quản Trị', '0901234567', N'Quản lý hệ thống', 'admin@hotel.com', '123456789', '1985-01-01', N'Nam', N'Hà Nội', '2020-01-01', 20000000),
       ('manager_nv', N'Trần Giám Đốc', '0912345678', N'Quản lý khách sạn', 'manager@hotel.com', '987654321', '1980-05-10', N'Nam', N'TP HCM', '2021-06-15', 15000000),
       ('recept_01', N'Nguyễn Lễ Tân 1', '0987654321', N'Lễ tân', 'recept1@hotel.com', '456789123', '1995-03-20', N'Nữ', N'Đà Nẵng', '2022-09-01', 8000000);

INSERT INTO TaiKhoan (tenDangNhap, maNhanVien, matKhau, vaiTro) 
VALUES ('admin', 'admin_nv', 'admin', 'ADMIN'),
       ('manager', 'manager_nv', '123', 'MANAGER'),
       ('recept1', 'recept_01', '123', 'RECEPTIONIST');

-- 2.3 Danh mục
INSERT INTO HuongNhin VALUES ('HN01', N'Hướng phố',    N'Nhìn ra đường phố sầm uất',       1.0,  1),
                             ('HN03', N'Hướng biển',   N'Nhìn ra bãi biển và đại dương',    1.3,  3),
                             ('HN06', N'VIP Panorama', N'View toàn cảnh 180° tầng cao',     1.5,  6);

INSERT INTO LoaiPhong VALUES ('LP001', N'Standard Single', 1, N'Phòng tiêu chuẩn cho 1 người', 400000),
                             ('LP002', N'Standard Double', 2, N'Phòng tiêu chuẩn cho 2 người', 600000),
                             ('LP003', N'Deluxe Twin', 2, N'Phòng hạng sang 2 giường đơn', 900000),
                             ('LP004', N'Deluxe King', 2, N'Phòng hạng sang giường lớn view biển', 1200000),
                             ('LP005', N'Suite Family', 4, N'Phòng cao cấp cho gia đình', 2000000);

INSERT INTO TienNghi VALUES ('TN01', N'Wifi miễn phí',     N'Cơ bản',      N'📶', 1),
                            ('TN02', N'Điều hòa',          N'Cơ bản',      N'❄️', 2),
                            ('TN08', N'Giường King',       N'Nội thất',    N'🛏️', 8);

INSERT INTO TienNghi_LoaiPhong VALUES ('LP001', 'TN01'), ('LP001', 'TN02'), ('LP004', 'TN08');

INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) 
VALUES ('P101', 1, 'HN01', 'LP001', 'AVAILABLE'),
       ('P301', 3, 'HN03', 'LP003', 'AVAILABLE'),
       ('P403', 4, 'HN03', 'LP005', 'AVAILABLE'),
       ('P501', 5, 'HN06', 'LP005', 'AVAILABLE');

-- 2.4 Bảng giá (Rack, Corporate, OTA)
INSERT INTO BangGia (maBangGia, tenBangGia, ngayBatDau, ngayKetThuc, isKichHoat, loaiBangGia, doiTuongApDung, mucUuTien, moTa)
VALUES ('BG2024', N'Bảng giá rack 2024', '2024-01-01', '2024-12-31', 1, 'RACK', 'ALL', 100, N'Giá niêm yết cơ bản'),
       ('BG_CORP', N'Giá doanh nghiệp 2024', '2024-01-01', '2024-12-31', 1, 'CORPORATE', 'CORPORATE', 50, N'Giảm 20% cho khách doanh nghiệp'),
       ('BG_OTA', N'Giá OTA 2024', '2024-01-01', '2024-12-31', 1, 'OTA', 'ALL', 80, N'Giá niêm yết trên các kênh OTA');

INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre)
VALUES ('BG2024', 'LP001', 400000, 70000, 25000),
       ('BG2024', 'LP002', 600000, 100000, 40000),
       ('BG2024', 'LP005', 2000000, 350000, 150000);

-- 2.5 Khách hàng
INSERT INTO KhachHang (maKhachHang, hoTen, sdt, cccd, ngaySinh, gioiTinh, quocTich) 
VALUES ('KH001', N'Nguyễn Văn An', '0911222333', '012345678901', '1990-05-15', N'Nam', N'Việt Nam');

INSERT INTO KhachHang (maKhachHang, hoTen, sdt, quocTich, loaiGiayTo, soHoChieu, noiCapHoChieu)
VALUES ('KH004', N'John Doe', '0123456789', N'Mỹ', 'PASSPORT', 'US1234567', N'USA');

-- 2.6 Dịch vụ
INSERT INTO DichVu (maDichVu, tenDichVu, loai, donGiaHienTai, donViTinh) 
VALUES ('DV001', N'Nước suối', N'Ăn uống', 15000, N'chai'),
       ('DV002', N'Coca Cola', N'Ăn uống', 25000, N'lon'),
       ('DV006', N'Thuê xe máy', N'Vận chuyển', 150000, N'ngày');

PRINT '✓ Consolidated Database setup completed successfully!';
GO
