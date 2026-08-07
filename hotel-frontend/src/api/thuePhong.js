import api from './api';

export const thuePhongAPI = {
  checkIn: (data) => api.post('/thue-phong/check-in', data),
  themDichVu: (data) => api.post('/thue-phong/them-dich-vu', data),
  doiPhong: (data) => api.post('/thue-phong/doi-phong', data),
  giaHan: (data) => api.post('/thue-phong/gia-han', data),
  previewCheckout: (maChiTiet, params) => api.get(`/thue-phong/check-out/preview/${maChiTiet}`, { params }),
  checkOut: (data) => api.post('/thue-phong/check-out', data),
  checkOutMaster: (data) => api.post('/thue-phong/check-out-master', data),
  xoaSuDungDichVu: (maSuDung) => api.delete(`/thue-phong/su-dung-dich-vu/${maSuDung}`),
  suaSuDungDichVu: (maSuDung, soLuong) => api.put(`/thue-phong/su-dung-dich-vu/${maSuDung}`, { soLuong }),
};
