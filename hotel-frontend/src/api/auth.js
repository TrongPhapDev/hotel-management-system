import api from './api';

export const authAPI = {
  login: (data) => api.post('/auth/login', data),
  doiMatKhau: (data) => api.post('/auth/doi-mat-khau', data),
};
