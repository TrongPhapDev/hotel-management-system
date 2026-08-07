import api from './api';

export const thongKeAPI = {
  getDashboard: () => api.get('/thong-ke/dashboard'),
  getDoanhThu7Ngay: () => api.get('/thong-ke/doanh-thu-7-ngay'),
  getKy: (type) => api.get('/thong-ke/ky', { params: { type } }),
  getDoanhThuTheoNgay: (ky) => api.get('/thong-ke/doanh-thu-theo-ngay', { params: { ky } }),
  getTopPhong: (n, ky) => api.get('/thong-ke/top-phong', { params: { n, ky } }),
  getTopDichVu: (n, ky) => api.get('/thong-ke/top-dich-vu', { params: { n, ky } }),
  getCheckinHomNay: () => api.get('/thong-ke/checkin-hom-nay'),
  getCheckoutHomNay: () => api.get('/thong-ke/checkout-hom-nay'),
  getAlerts: () => api.get('/thong-ke/alerts'),
};
