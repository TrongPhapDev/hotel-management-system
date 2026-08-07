import api from './api';

export const khachHangAPI = {
  getAll: (kw) => api.get('/khach-hang', { params: { kw } }),
  getById: (id) => api.get(`/khach-hang/${id}`),
  create: (data) => api.post('/khach-hang', data),
  update: (id, data) => api.put(`/khach-hang/${id}`, data),
  delete: (id) => api.delete(`/khach-hang/${id}`),
};
