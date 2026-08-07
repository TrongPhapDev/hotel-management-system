import api from './api';

export const logAPI = {
  getAll: () => api.get('/log'),
};
