import api from './api';

export const hoaDonAPI = {
  getAll: () => api.get('/hoa-don'),
  getById: (id) => api.get(`/hoa-don/${id}`),
  getByDatPhong: (maDatPhong) => api.get(`/hoa-don/dat-phong/${maDatPhong}`),
  getDoanhThu: (month, year) => api.get('/hoa-don/doanh-thu', { params: { month, year } }),
  create: (data) => api.post('/hoa-don', data),
  thanhToan: (id, phuongThuc) => api.patch(`/hoa-don/${id}/thanh-toan`, { phuongThuc }),
  update: (id, data) => api.put(`/hoa-don/${id}`, data),
};
