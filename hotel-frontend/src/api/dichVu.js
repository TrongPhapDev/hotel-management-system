import api from './api';

export const dichVuAPI = {
  getAll: () => api.get('/dich-vu'),
  getById: (id) => api.get(`/dich-vu/${id}`),
  search: (kw, type) => api.get('/dich-vu/search', { params: { kw, type } }),
  create: (data) => api.post('/dich-vu', data),
  update: (id, data) => api.put(`/dich-vu/${id}`, data),
  delete: (id) => api.delete(`/dich-vu/${id}`),
  giaTrungBinh: () => api.get('/dich-vu/stats/gia-trung-binh'),
};
