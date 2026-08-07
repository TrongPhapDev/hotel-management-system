import api from './api';

export const giaoCaAPI = {
  moCa: (data) => api.post('/giao-ca/mo-ca', data),
  chotCa: (data) => api.post('/giao-ca/chot-ca', data),
  getCurrent: (maNhanVien) => api.get('/giao-ca/current', { params: { maNhanVien } }),
  getExpectedCash: (maGiaoCa) => api.get('/giao-ca/expected-cash', { params: { maGiaoCa } }),
  getHistory: (kw, status) => api.get('/giao-ca/history', { params: { kw, status } }),
};
