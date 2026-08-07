import api from './api';

export const loaiPhongAPI = {
  getAll: () => api.get('/loai-phong'),
  getById: (id) => api.get(`/loai-phong/${id}`),
  create: (data) => api.post('/loai-phong', data),
  update: (id, data) => api.put(`/loai-phong/${id}`, data),
  delete: (id) => api.delete(`/loai-phong/${id}`),
};
