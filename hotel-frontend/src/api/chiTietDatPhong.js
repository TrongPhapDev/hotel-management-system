import api from './api';

export const chiTietDatPhongAPI = {
  getAll: (maDatPhong) => api.get('/chi-tiet-dat-phong', { params: { maDatPhong } }),
  getActive: () => api.get('/chi-tiet-dat-phong/active'),
};
