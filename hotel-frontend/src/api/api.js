import axios from 'axios';

const BASE = 'http://localhost:8080/api';

const api = axios.create({ baseURL: BASE });

export default api;

export * from './auth';
export * from './phong';
export * from './loaiPhong';
export * from './khachHang';
export * from './datPhong';
export * from './dichVu';
export * from './hoaDon';
export * from './nhanVien';
export * from './khuyenMai';
export * from './thuePhong';
export * from './bangGia';
export * from './thongKe';
export * from './log';
export * from './chiTietDatPhong';
export * from './giaoCa';
