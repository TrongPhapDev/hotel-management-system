import React, { useEffect, useState } from 'react';
import { bangGiaAPI, loaiPhongAPI } from '../api/api';
import { Calendar, Percent, Plus, Save, Trash2, Edit, X, DollarSign, Clock, Settings, Users, Info } from 'lucide-react';

const fmt = (n) => (n||0).toLocaleString('vi-VN');

const BangGiaPage = () => {
  const [list, setList] = useState([]);
  const [loaiPhongs, setLoaiPhongs] = useState([]);
  const [selected, setSelected] = useState(null);
  const [chiTiets, setChiTiets] = useState([]);
  const [openForm, setOpenForm] = useState(false);
  const [editingPlan, setEditingPlan] = useState(null);
  const [formData, setFormData] = useState({
    maBangGia: '', tenBangGia: '', ngayBatDau: '', ngayKetThuc: '',
    trangThai: true, loaiBangGia: 'RACK', doiTuongApDung: 'ALL',
    mucUuTien: 100, moTa: ''
  });
  const [loading, setLoading] = useState(false);
  const [planSearch, setPlanSearch] = useState('');
  const [planStatus, setPlanStatus] = useState('ALL');

  useEffect(() => {
    fetchPlans();
    fetchLoaiPhongs();
  }, []);

  const fetchPlans = async () => {
    try {
      const res = await bangGiaAPI.getAll();
      setList(res.data);
    } catch (e) { console.error(e); }
  };

  const fetchLoaiPhongs = async () => {
    try {
      const res = await loaiPhongAPI.getAll();
      setLoaiPhongs(res.data);
    } catch (e) { console.error(e); }
  };

  const selectPlan = async (plan) => {
    setSelected(plan);
    setLoading(true);
    try {
      const res = await bangGiaAPI.getChiTiet(plan.maBangGia);
      // Map existing chi tiet with all loai phong
      const mapped = loaiPhongs.map(lp => {
        const found = res.data.find(d => d.loaiPhong?.maLoaiPhong === lp.maLoaiPhong);
        return {
          loaiPhong: lp,
          giaNgay: found ? found.giaNgay : lp.giaTheoNgay,
          giaGioDau: found ? found.giaGioDau : lp.giaTheoGio || 100000,
          giaGioTiepTheo: found ? found.giaGioTiepTheo : 50000,
          phuPhiTraTre: found ? found.phuPhiTraTre : 30000,
          giaCuoiTuan: found ? found.giaCuoiTuan : 0,
        };
      });
      setChiTiets(mapped);
    } catch (e) { console.error(e); }
    setLoading(false);
  };

  const handleOpenAdd = () => {
    setEditingPlan(null);
    setFormData({
      maBangGia: '', tenBangGia: '',
      ngayBatDau: new Date().toISOString().substring(0, 16),
      ngayKetThuc: new Date(Date.now() + 30*24*3600*1000).toISOString().substring(0, 16),
      trangThai: true, loaiBangGia: 'SEASONAL', doiTuongApDung: 'ALL',
      mucUuTien: 10, moTa: ''
    });
    setOpenForm(true);
  };

  const handleOpenEdit = (plan) => {
    setEditingPlan(plan);
    setFormData({
      ...plan,
      ngayBatDau: plan.ngayBatDau ? plan.ngayBatDau.substring(0, 16) : '',
      ngayKetThuc: plan.ngayKetThuc ? plan.ngayKetThuc.substring(0, 16) : '',
    });
    setOpenForm(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingPlan) {
        await bangGiaAPI.update(formData.maBangGia, formData);
      } else {
        await bangGiaAPI.create(formData);
      }
      setOpenForm(false);
      fetchPlans();
      if (selected?.maBangGia === formData.maBangGia) {
        setSelected({ ...selected, ...formData });
      }
    } catch (e) { alert(e.response?.data || "Có lỗi xảy ra"); }
  };

  const handleDelete = async (plan) => {
    if (!window.confirm(`Bạn có chắc muốn xóa bảng giá: ${plan.tenBangGia}?`)) return;
    try {
      await bangGiaAPI.delete(plan.maBangGia);
      if (selected?.maBangGia === plan.maBangGia) {
        setSelected(null);
        setChiTiets([]);
      }
      fetchPlans();
    } catch (e) { alert(e.response?.data || "Không thể xóa"); }
  };

  const handleDetailChange = (index, field, value) => {
    const next = [...chiTiets];
    next[index][field] = Number(value);
    setChiTiets(next);
  };

  const handleSaveDetails = async () => {
    if (!selected) return;
    try {
      // Map to DTO for backend (which requires loaiPhong entity or maLoaiPhong)
      const dataToSave = chiTiets.map(ct => ({
        loaiPhong: { maLoaiPhong: ct.loaiPhong.maLoaiPhong },
        giaNgay: ct.giaNgay,
        giaGioDau: ct.giaGioDau,
        giaGioTiepTheo: ct.giaGioTiepTheo,
        phuPhiTraTre: ct.phuPhiTraTre,
        giaCuoiTuan: ct.giaCuoiTuan
      }));
      await bangGiaAPI.saveChiTiet(selected.maBangGia, dataToSave);
      alert("Đã lưu cấu hình giá phòng thành công!");
    } catch (e) { alert("Lỗi khi lưu: " + (e.response?.data || e.message)); }
  };

  const filteredPlans = list.filter(plan => {
    const kw = planSearch.trim().toLowerCase();
    const matchKw = !kw || plan.tenBangGia?.toLowerCase().includes(kw);
    
    let matchStatus = true;
    if (planStatus === 'ACTIVE') {
      matchStatus = plan.trangThai === true;
    } else if (planStatus === 'INACTIVE') {
      matchStatus = plan.trangThai === false;
    }
    
    return matchKw && matchStatus;
  });

  return (
    <div className="page-shell">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <div className="page-label">Doanh thu</div>
          <h1 className="page-title flex items-center gap-2">
            <DollarSign style={{ width: 20, height: 20, color: 'var(--text-secondary)' }} strokeWidth={2} />
            Chính Sách &amp; Bảng Giá Phòng
          </h1>
          <p className="page-subtitle">Thiết lập các kế hoạch giá (Rate Plans) theo mùa vụ, đối tượng hoặc sự kiện</p>
        </div>
        <button onClick={handleOpenAdd} className="btn-primary">
          <Plus size={15}/> Thêm Kế Hoạch Giá
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: List of rate plans */}
        <div className="lg:col-span-1 space-y-4">
          <div className="bg-[var(--bg-sidebar)] border border-[var(--border-color)] p-4 rounded-2xl space-y-3 shadow-sm">
            <h3 className="text-xs font-bold text-[var(--text-secondary)] uppercase tracking-widest mb-1">Danh sách kế hoạch</h3>
            
            {/* Left Filter Bar */}
            <div className="space-y-2">
              <input type="text" placeholder="Tìm theo tên bảng giá..." value={planSearch} onChange={(e) => setPlanSearch(e.target.value)}
                className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-xs text-[var(--input-text)] focus:border-blue-500 outline-none font-medium"/>
              <select value={planStatus} onChange={(e) => setPlanStatus(e.target.value)}
                className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-xs text-[var(--input-text)] focus:border-blue-500 outline-none font-semibold">
                <option value="ALL">Tất cả trạng thái</option>
                <option value="ACTIVE">Đang kích hoạt</option>
                <option value="INACTIVE">Ngừng</option>
              </select>
            </div>

            <div className="space-y-2.5 pt-2 border-t border-[var(--border-color)] max-h-[50vh] overflow-y-auto pr-1">
              {filteredPlans.length === 0 ? (
                <div className="text-center py-6 text-xs text-[var(--text-secondary)] font-semibold">Không tìm thấy kế hoạch</div>
              ) : (
                filteredPlans.map(plan => {
                  const active = selected?.maBangGia === plan.maBangGia;
                  return (
                    <div key={plan.maBangGia} onClick={() => selectPlan(plan)}
                      className={`p-3.5 rounded-xl border cursor-pointer transition-all ${active ? 'bg-blue-50/40 dark:bg-blue-950/20 border-blue-500 shadow-sm' : 'bg-[var(--bg-sidebar)] border-[var(--border-color)] hover:border-blue-500/50'}`}>
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <h4 className="font-bold text-[var(--text-primary)] text-sm">{plan.tenBangGia}</h4>
                          <span className="text-[10px] bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 px-2 py-0.5 rounded-full border border-blue-200 dark:border-blue-900 font-semibold mt-1 inline-block whitespace-nowrap">
                            {plan.loaiBangGia}
                          </span>
                        </div>
                        <div className="flex items-center gap-1 shrink-0">
                          <button onClick={(e) => { e.stopPropagation(); handleOpenEdit(plan); }}
                            className="p-1 text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-main)] rounded transition-colors"><Edit size={14}/></button>
                          {plan.loaiBangGia !== 'RACK' && (
                            <button onClick={(e) => { e.stopPropagation(); handleDelete(plan); }}
                              className="p-1 text-[var(--text-secondary)] hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/20 rounded transition-colors"><Trash2 size={14}/></button>
                          )}
                        </div>
                      </div>
                      <div className="text-xs text-[var(--text-secondary)] mt-2.5 space-y-1">
                        <div className="flex items-center gap-1.5"><Calendar size={12}/> {plan.ngayBatDau ? new Date(plan.ngayBatDau).toLocaleDateString('vi-VN') : 'Mãi mãi'} - {plan.ngayKetThuc ? new Date(plan.ngayKetThuc).toLocaleDateString('vi-VN') : 'Mãi mãi'}</div>
                        <div className="flex items-center gap-1.5"><Users size={12}/> Đối tượng: <span className="font-semibold text-[var(--text-primary)]">{plan.doiTuongApDung}</span></div>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>

        {/* Right: Plan Detail Configurator */}
        <div className="lg:col-span-2 space-y-4">
          {selected ? (
            <div className="bg-[var(--bg-sidebar)] border border-[var(--border-color)] p-5 rounded-2xl shadow-sm space-y-5">
              <div className="flex items-center justify-between border-b border-[var(--border-color)] pb-4">
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-lg font-bold text-[var(--text-primary)]">{selected.tenBangGia}</h3>
                    <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold border whitespace-nowrap ${selected.trangThai ? 'bg-emerald-50 dark:bg-emerald-950/30 text-emerald-600 dark:text-emerald-400 border-emerald-200 dark:border-emerald-900' : 'bg-[var(--bg-main)] text-[var(--text-secondary)] border-[var(--border-color)]'}`}>
                      {selected.trangThai ? 'Đang kích hoạt' : 'Tạm tắt'}
                    </span>
                  </div>
                  <p className="text-[var(--text-secondary)] text-xs mt-1">{selected.moTa || 'Không có mô tả cho kế hoạch này'}</p>
                </div>
                <button onClick={handleSaveDetails}
                  className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl font-bold transition-all text-xs shadow-lg shadow-emerald-600/20">
                  <Save size={14}/> Lưu Bảng Giá
                </button>
              </div>

              {loading ? (
                <div className="py-20 text-center text-[var(--text-secondary)]">Đang tải cấu hình bảng giá...</div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse">
                    <thead>
                      <tr className="border-b border-[var(--border-color)] text-[var(--text-secondary)] text-xs font-bold uppercase tracking-wider">
                        <th className="pb-3 pr-2">Loại phòng</th>
                        <th className="pb-3 px-2"><span className="flex items-center gap-1"><DollarSign size={13}/>Giá ngày thường</span></th>
                        <th className="pb-3 px-2"><span className="flex items-center gap-1"><Percent size={13}/>Cuối tuần</span></th>
                        <th className="pb-3 px-2"><span className="flex items-center gap-1"><Clock size={13}/>Giờ đầu</span></th>
                        <th className="pb-3 px-2">Giờ tiếp theo</th>
                        <th className="pb-3 pl-2">Phạt trả trễ (/h)</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[var(--border-color)] text-sm">
                      {chiTiets.map((ct, i) => (
                        <tr key={i} className="hover:bg-[var(--bg-main)]">
                          <td className="py-3 pr-2 font-semibold text-[var(--text-primary)]">
                            {ct.loaiPhong.tenLoaiPhong}
                            <div className="text-[10px] text-[var(--text-secondary)] font-normal">Mặc định: {fmt(ct.loaiPhong.giaTheoNgay)}đ</div>
                          </td>
                          <td className="py-3 px-2">
                            <input type="number" value={ct.giaNgay} onChange={(e)=>handleDetailChange(i, 'giaNgay', e.target.value)}
                              className="w-28 bg-[var(--input-bg)] border border-[var(--input-border)] rounded-lg px-2 py-1 text-right text-[var(--input-text)] text-xs font-semibold focus:border-blue-500 outline-none"/>
                          </td>
                          <td className="py-3 px-2">
                            <input type="number" value={ct.giaCuoiTuan} onChange={(e)=>handleDetailChange(i, 'giaCuoiTuan', e.target.value)}
                              placeholder="Trùng giá ngày" className="w-28 bg-[var(--input-bg)] border border-[var(--input-border)] rounded-lg px-2 py-1 text-right text-[var(--input-text)] text-xs font-semibold focus:border-blue-500 outline-none"/>
                          </td>
                          <td className="py-3 px-2">
                            <input type="number" value={ct.giaGioDau} onChange={(e)=>handleDetailChange(i, 'giaGioDau', e.target.value)}
                              className="w-24 bg-[var(--input-bg)] border border-[var(--input-border)] rounded-lg px-2 py-1 text-right text-[var(--input-text)] text-xs font-semibold focus:border-blue-500 outline-none"/>
                          </td>
                          <td className="py-3 px-2">
                            <input type="number" value={ct.giaGioTiepTheo} onChange={(e)=>handleDetailChange(i, 'giaGioTiepTheo', e.target.value)}
                              className="w-24 bg-[var(--input-bg)] border border-[var(--input-border)] rounded-lg px-2 py-1 text-right text-[var(--input-text)] text-xs font-semibold focus:border-blue-500 outline-none"/>
                          </td>
                          <td className="py-3 pl-2">
                            <input type="number" value={ct.phuPhiTraTre} onChange={(e)=>handleDetailChange(i, 'phuPhiTraTre', e.target.value)}
                              className="w-24 bg-[var(--input-bg)] border border-[var(--input-border)] rounded-lg px-2 py-1 text-right text-[var(--input-text)] text-xs font-semibold focus:border-blue-500 outline-none"/>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          ) : (
            <div className="bg-[var(--bg-sidebar)] border border-[var(--border-color)] p-10 rounded-2xl flex flex-col items-center justify-center text-center text-[var(--text-secondary)] shadow-sm">
              <Info size={36} className="text-[var(--text-secondary)] mb-3"/>
              <p className="text-sm font-semibold">Chọn một kế hoạch giá ở cột trái để cấu hình chi tiết bảng giá phòng</p>
            </div>
          )}
        </div>
      </div>

      {/* Dialog Form: Add/Edit Plan */}
      {openForm && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setOpenForm(false); }}>
          <div className="modal-panel" style={{ maxWidth: 512 }}>
            <div className="px-5 py-4 border-b border-[var(--border-color)] flex items-center justify-between">
              <h3 className="font-bold text-[var(--text-primary)] text-base">{editingPlan ? 'Sửa Kế Hoạch Giá' : 'Thêm Kế Hoạch Giá Mới'}</h3>
              <button onClick={() => setOpenForm(false)} className="text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors"><X size={18}/></button>
            </div>
            <form onSubmit={handleSubmit} className="p-5 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="text-xs font-bold text-[var(--text-secondary)] uppercase">Mã bảng giá</label>
                  <input type="text" required disabled={!!editingPlan} value={formData.maBangGia} onChange={(e)=>setFormData({...formData, maBangGia: e.target.value})}
                    placeholder="VD: BG_HE_2026" className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-sm text-[var(--input-text)] mt-1.5 focus:border-blue-500 outline-none"/>
                </div>
                <div className="col-span-2">
                  <label className="text-xs font-bold text-[var(--text-secondary)] uppercase">Tên kế hoạch</label>
                  <input type="text" required value={formData.tenBangGia} onChange={(e)=>setFormData({...formData, tenBangGia: e.target.value})}
                    placeholder="VD: Bảng giá Mùa Hè 2026" className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-sm text-[var(--input-text)] mt-1.5 focus:border-blue-500 outline-none"/>
                </div>
                <div>
                  <label className="text-xs font-bold text-[var(--text-secondary)] uppercase">Ngày bắt đầu</label>
                  <input type="datetime-local" required value={formData.ngayBatDau} onChange={(e)=>setFormData({...formData, ngayBatDau: e.target.value})}
                    className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-sm text-[var(--input-text)] mt-1.5 focus:border-blue-500 outline-none"/>
                </div>
                <div>
                  <label className="text-xs font-bold text-[var(--text-secondary)] uppercase">Ngày kết thúc</label>
                  <input type="datetime-local" required value={formData.ngayKetThuc} onChange={(e)=>setFormData({...formData, ngayKetThuc: e.target.value})}
                    className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-sm text-[var(--input-text)] mt-1.5 focus:border-blue-500 outline-none"/>
                </div>
                <div>
                  <label className="text-xs font-bold text-[var(--text-secondary)] uppercase">Loại kế hoạch</label>
                  <select value={formData.loaiBangGia} onChange={(e)=>setFormData({...formData, loaiBangGia: e.target.value})}
                    className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2.5 text-sm text-[var(--input-text)] mt-1.5 focus:border-blue-500 outline-none">
                    <option value="RACK">RACK (Giá gốc)</option>
                    <option value="SEASONAL">SEASONAL (Mùa vụ)</option>
                    <option value="CORPORATE">CORPORATE (Doanh nghiệp)</option>
                    <option value="OTA">OTA (Bán online)</option>
                    <option value="PROMOTION">PROMOTION (Khuyến mãi)</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs font-bold text-[var(--text-secondary)] uppercase">Đối tượng khách</label>
                  <select value={formData.doiTuongApDung} onChange={(e)=>setFormData({...formData, doiTuongApDung: e.target.value})}
                    className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2.5 text-sm text-[var(--input-text)] mt-1.5 focus:border-blue-500 outline-none">
                    <option value="ALL">Tất cả khách</option>
                    <option value="CA_NHAN">Khách lẻ</option>
                    <option value="DOAN">Khách đoàn</option>
                    <option value="VIP">Khách VIP</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs font-bold text-[var(--text-secondary)] uppercase">Mức ưu tiên</label>
                  <input type="number" required value={formData.mucUuTien} onChange={(e)=>setFormData({...formData, mucUuTien: Number(e.target.value)})}
                    className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-sm text-[var(--input-text)] mt-1.5 focus:border-blue-500 outline-none"/>
                </div>
                <div className="flex items-center pt-6 pl-2">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input type="checkbox" checked={formData.trangThai} onChange={(e)=>setFormData({...formData, trangThai: e.target.checked})}
                      className="w-4 h-4 text-blue-600 bg-[var(--input-bg)] border-[var(--input-border)] rounded focus:ring-blue-500"/>
                    <span className="text-sm font-bold text-[var(--text-primary)]">Kích hoạt áp dụng</span>
                  </label>
                </div>
                <div className="col-span-2">
                  <label className="text-xs font-bold text-[var(--text-secondary)] uppercase">Mô tả kế hoạch</label>
                  <textarea value={formData.moTa} onChange={(e)=>setFormData({...formData, moTa: e.target.value})} rows={2}
                    placeholder="Mô tả chính sách..." className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-sm text-[var(--input-text)] mt-1.5 focus:border-blue-500 outline-none"/>
                </div>
              </div>
              <div className="flex justify-end gap-2.5 pt-3 border-t border-[var(--border-color)]">
                <button type="button" onClick={() => setOpenForm(false)}
                  className="px-4 py-2 bg-[var(--bg-main)] hover:bg-[var(--border-color)] text-[var(--text-primary)] rounded-xl font-bold transition-all text-xs border border-[var(--border-color)]">Hủy</button>
                <button type="submit"
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-bold transition-all text-xs shadow-lg shadow-blue-600/20">Lưu</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default BangGiaPage;
