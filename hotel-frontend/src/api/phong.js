import api from './api';

export const phongAPI = {
  getAll: (trangThai) => api.get('/phong', { params: { trangThai } }),
  getTrong: () => api.get('/phong/trong'),
  thongKe: () => api.get('/phong/thong-ke'),
  getById: (id) => api.get(`/phong/${id}`),
  create: (data) => api.post('/phong', data),
  update: (id, data) => api.put(`/phong/${id}`, data),
  capNhatTrangThai: (id, trangThai) => api.patch(`/phong/${id}/trang-thai`, { trangThai }),
  delete: (id) => api.delete(`/phong/${id}`),
  getAvailableInRange: (ngayNhan, ngayTra) => api.get('/phong/available-range', { params: { ngayNhan, ngayTra } }),
};
