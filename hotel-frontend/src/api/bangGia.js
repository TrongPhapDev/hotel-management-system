import api from './api';

export const bangGiaAPI = {
  getAll: () => api.get('/bang-gia'),
  getById: (id) => api.get(`/bang-gia/${id}`),
  create: (data) => api.post('/bang-gia', data),
  update: (id, data) => api.put(`/bang-gia/${id}`, data),
  delete: (id) => api.delete(`/bang-gia/${id}`),
  getChiTiet: (id) => api.get(`/bang-gia/${id}/chi-tiet`),
  saveChiTiet: (id, data) => api.post(`/bang-gia/${id}/chi-tiet`, data),
};
