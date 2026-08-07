import api from './api';

export const datPhongAPI = {
  getAll: (kw, trangThai) => api.get('/dat-phong', { params: { kw, trangThai } }),
  getAllFull: () => api.get('/dat-phong/all'),
  getById: (id) => api.get(`/dat-phong/${id}`),
  create: (data) => api.post('/dat-phong', data),
  update: (id, data) => api.put(`/dat-phong/${id}`, data),
  checkIn: (id) => api.patch(`/dat-phong/${id}/check-in`),
  checkOut: (id) => api.patch(`/dat-phong/${id}/check-out`),
  huy: (id, lyDo) => api.patch(`/dat-phong/${id}/huy`, { lyDo }),
};
