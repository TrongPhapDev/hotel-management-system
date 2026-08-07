import api from './api';

export const khuyenMaiAPI = {
  getAll: () => api.get('/khuyen-mai'),
  getActive: () => api.get('/khuyen-mai/dang-hoat-dong'),
  getById: (id) => api.get(`/khuyen-mai/${id}`),
  create: (data) => api.post('/khuyen-mai', data),
  update: (id, data) => api.put(`/khuyen-mai/${id}`, data),
  delete: (id) => api.delete(`/khuyen-mai/${id}`),
};
