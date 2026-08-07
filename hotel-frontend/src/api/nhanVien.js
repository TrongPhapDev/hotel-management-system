import api from './api';

export const nhanVienAPI = {
  getAll: () => api.get('/nhan-vien'),
  getDangLamViec: () => api.get('/nhan-vien/dang-lam-viec'),
  getById: (id) => api.get(`/nhan-vien/${id}`),
  create: (data) => api.post('/nhan-vien', data),
  update: (id, data) => api.put(`/nhan-vien/${id}`, data),
  nghi: (id) => api.patch(`/nhan-vien/${id}/nghi`),
};
