import React, { useEffect, useState } from 'react';
import { giaoCaAPI, nhanVienAPI } from '../api/api';
import { ClipboardList, Search, RefreshCw, Clock, User, Play, Key, X, Info } from 'lucide-react';

const fmt = (n) => (n||0).toLocaleString('vi-VN');

const GiaoCaPage = () => {
  const [activeShift, setActiveShift] = useState(null);
  const [expectedCash, setExpectedCash] = useState(0);
  const [history, setHistory] = useState([]);
  const [staffs, setStaffs] = useState([]);
  const [loading, setLoading] = useState(false);

  const [searchKw, setSearchKw] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const [showOpenDlg, setShowOpenDlg] = useState(false);
  const [startFloat, setStartFloat] = useState(1000000);

  const [showCloseDlg, setShowCloseDlg] = useState(false);
  const [closeForm, setCloseForm] = useState({
    tienMatBanGiao: 0,
    maNhanVienNhan: '',
    ghiChu: ''
  });

  const currentUser = JSON.parse(localStorage.getItem('user')) || { nhanVien: { maNhanVien: 'NV001', hoTen: 'Lễ Tân' } };
  const currentMaNV = currentUser.nhanVien?.maNhanVien || 'NV001';

  useEffect(() => {
    fetchActiveShift();
    fetchHistory();
    fetchStaffs();
  }, []);

  const fetchActiveShift = async () => {
    try {
      const res = await giaoCaAPI.getCurrent(currentMaNV);
      if (res.status === 200 && res.data) {
        setActiveShift(res.data);
        const exp = await giaoCaAPI.getExpectedCash(res.data.maGiaoCa);
        setExpectedCash(exp.data);
      } else {
        setActiveShift(null);
      }
    } catch(e) { console.error(e); }
  };

  const fetchHistory = async () => {
    setLoading(true);
    try {
      const statusArg = statusFilter === 'ALL' ? '' : statusFilter;
      const res = await giaoCaAPI.getHistory(searchKw, statusArg);
      setHistory(res.data);
    } catch(e) { console.error(e); }
    setLoading(false);
  };

  const fetchStaffs = async () => {
    try {
      const res = await nhanVienAPI.getAll();
      setStaffs(res.data);
    } catch(e) { console.error(e); }
  };

  const handleOpenShift = async (e) => {
    e.preventDefault();
    try {
      await giaoCaAPI.moCa({
        maNhanVien: currentMaNV,
        tienDauCa: Number(startFloat)
      });
      alert('Đã bắt đầu ca trực mới!');
      setShowOpenDlg(false);
      fetchActiveShift();
      fetchHistory();
    } catch (e) {
      alert(e.response?.data || 'Không thể bắt đầu ca trực!');
    }
  };

  const handleCloseShift = async (e) => {
    e.preventDefault();
    if (!activeShift) return;
    try {
      await giaoCaAPI.chotCa({
        maGiaoCa: activeShift.maGiaoCa,
        tienMatBanGiao: Number(closeForm.tienMatBanGiao),
        maNhanVienNhan: closeForm.maNhanVienNhan,
        ghiChu: closeForm.ghiChu
      });
      alert('Đã chốt ca làm việc và bàn giao thành công!');
      setShowCloseDlg(false);
      setActiveShift(null);
      fetchActiveShift();
      fetchHistory();
    } catch (e) {
      alert(e.response?.data || 'Có lỗi xảy ra khi bàn giao ca trực!');
    }
  };

  const hasFilter = searchKw || statusFilter !== 'ALL';
  const clearFilters = () => { setSearchKw(''); setStatusFilter('ALL'); };

  return (
    <div className="page-shell">
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="page-label">
            Vận hành
          </div>
          <h1 className="page-title flex items-center gap-2">
            <ClipboardList style={{ width: 20, height: 20, color: 'var(--text-secondary)' }} strokeWidth={2} />
            Giao Ca &amp; Bàn Giao Quầy
          </h1>
          <p className="page-subtitle">Theo dõi dòng tiền, đối soát chênh lệch và bàn giao ca trực lễ tân</p>
        </div>
        <div className="flex items-center gap-3">
          {!activeShift ? (
            <button onClick={() => setShowOpenDlg(true)} className="btn-primary">
              <Play style={{ width: 14, height: 14 }} /> Bắt Đầu Ca Trực
            </button>
          ) : (
            <button
              onClick={() => { setCloseForm({ tienMatBanGiao: expectedCash, maNhanVienNhan: '', ghiChu: '' }); setShowCloseDlg(true); }}
              className="flex items-center gap-2 px-4 py-2.5 text-white rounded-xl font-bold text-xs transition-all"
              style={{ background: '#10b981', boxShadow: '0 4px 20px -4px rgba(16,185,129,0.3)' }}
            >
              <Key style={{ width: 14, height: 14 }} /> Chốt Ca &amp; Bàn Giao
            </button>
          )}
        </div>
      </div>

      {/* Active Shift Banner */}
      {activeShift && (
        <div className="section-box" style={{ background: 'var(--accent-dim)', border: '1px solid var(--accent-border)' }}>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6 relative overflow-hidden p-1">
            <div className="absolute right-4 top-2 font-black font-mono text-7xl select-none pointer-events-none" style={{ color: 'var(--accent)', opacity: 0.07 }}>
              {activeShift.maGiaoCa}
            </div>
            <div className="space-y-1 z-10">
              <div className="text-[10px] font-bold uppercase tracking-wider" style={{ color: 'var(--accent)' }}>Ca đang hoạt động</div>
              <h3 className="text-lg font-extrabold" style={{ color: 'var(--text-primary)' }}>{activeShift.nhanVien?.hoTen}</h3>
              <div className="text-xs flex items-center gap-1.5 font-semibold" style={{ color: 'var(--text-secondary)' }}>
                <Clock style={{ width: 13, height: 13 }} /> Bắt đầu: {new Date(activeShift.thoiGianBatDau).toLocaleString('vi-VN')}
              </div>
            </div>
            <div className="space-y-1 z-10">
              <div className="text-[10px] font-bold uppercase" style={{ color: 'var(--text-muted)' }}>Vốn đầu ca</div>
              <div className="text-base font-extrabold" style={{ color: 'var(--text-primary)' }}>{fmt(activeShift.tienMatDauCa)}đ</div>
            </div>
            <div className="space-y-1 z-10">
              <div className="text-[10px] font-bold uppercase" style={{ color: 'var(--text-muted)' }}>Thu trong ca (tiền mặt)</div>
              <div className="text-base font-extrabold" style={{ color: 'var(--status-avail-text)' }}>{fmt(expectedCash - activeShift.tienMatDauCa)}đ</div>
            </div>
            <div className="space-y-1 z-10">
              <div className="text-[10px] font-bold uppercase" style={{ color: 'var(--accent)' }}>Tổng tiền mặt kỳ vọng</div>
              <div className="text-lg font-black" style={{ color: 'var(--status-avail-text)' }}>{fmt(expectedCash)}đ</div>
            </div>
          </div>
        </div>
      )}

      {/* Filter Bar */}
      <div className="filter-bar">
        <div className="filter-search">
          <Search className="filter-search-icon" style={{ width: 14, height: 14 }} />
          <input type="text" placeholder="Tìm tên nhân viên, mã ca trực..."
            value={searchKw} onChange={e => setSearchKw(e.target.value)} />
          {searchKw && (
            <button onClick={() => setSearchKw('')} className="filter-clear-btn">
              <X style={{ width: 10, height: 10 }} />
            </button>
          )}
        </div>

        <div className={`filter-select-wrap ${statusFilter !== 'ALL' ? 'active' : ''}`}>
          <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
            <option value="ALL">Tất cả trạng thái</option>
            <option value="OPEN">Ca đang mở</option>
            <option value="CLOSED">Ca đã bàn giao</option>
          </select>
        </div>

        {hasFilter && (
          <button onClick={clearFilters} className="filter-reset-btn">Xoá lọc</button>
        )}

        <button onClick={fetchHistory} className="filter-reset-btn" style={{ marginLeft: 'auto' }}>
          <RefreshCw style={{ width: 13, height: 13 }} /> Làm mới
        </button>
        <span className="filter-result-count">{history.length} ca trực</span>
      </div>

      {/* Table */}
      <div className="section-box overflow-x-auto">
        <table className="data-table">
          <thead>
            <tr>
              {['Mã ca', 'Nhân viên', 'Thời gian', 'Vốn đầu ca', 'Thu trong ca', 'Thực tế bàn giao', 'Chênh lệch', 'Trạng thái'].map(h => (
                <th key={h}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={8} className="py-16 text-center">
                <div className="spinner mx-auto mb-2" />
                <div style={{ color: 'var(--text-muted)', fontSize: 12 }}>Đang tải lịch sử giao ca...</div>
              </td></tr>
            ) : history.length === 0 ? (
              <tr><td colSpan={8} className="py-14 text-center" style={{ color: 'var(--text-muted)', fontSize: 13 }}>Không tìm thấy ca trực nào</td></tr>
            ) : history.map(sh => (
              <tr key={sh.maGiaoCa}>
                <td className="font-mono-data font-bold text-[12px]" style={{ color: 'var(--accent)' }}>{sh.maGiaoCa}</td>
                <td>
                  <div className="flex items-center gap-1.5">
                    <User style={{ width: 13, height: 13, color: 'var(--text-muted)' }} />
                    <span className="font-semibold" style={{ color: 'var(--text-primary)' }}>{sh.nhanVien?.hoTen}</span>
                  </div>
                </td>
                <td className="text-xs space-y-0.5" style={{ color: 'var(--text-secondary)' }}>
                  <div><span className="font-bold text-[10px]" style={{ color: 'var(--text-muted)' }}>VÀO:</span> {new Date(sh.thoiGianBatDau).toLocaleString('vi-VN')}</div>
                  {sh.thoiGianKetThuc && (
                    <div><span className="font-bold text-[10px]" style={{ color: 'var(--text-muted)' }}>RA:</span> {new Date(sh.thoiGianKetThuc).toLocaleString('vi-VN')}</div>
                  )}
                </td>
                <td className="font-mono-data tabular-nums" style={{ color: 'var(--text-secondary)' }}>{fmt(sh.tienMatDauCa)}đ</td>
                <td className="font-mono-data tabular-nums" style={{ color: 'var(--text-secondary)' }}>{fmt(sh.tienMatThuTrongCa)}đ</td>
                <td className="font-mono-data tabular-nums" style={{ color: 'var(--status-avail-text)' }}>
                  {sh.trangThai === 'CLOSED' ? `${fmt(sh.tienMatBanGiao)}đ` : '—'}
                </td>
                <td className="font-mono-data tabular-nums font-bold"
                  style={{ color: sh.tienMatChenhLech < 0 ? 'var(--status-maint-text)' : sh.tienMatChenhLech > 0 ? 'var(--status-clean-text)' : 'var(--text-secondary)' }}>
                  {sh.trangThai === 'CLOSED' ? `${sh.tienMatChenhLech > 0 ? '+' : ''}${fmt(sh.tienMatChenhLech)}đ` : '—'}
                </td>
                <td>
                  {sh.trangThai === 'OPEN' ? (
                    <span className="badge badge-occupied">
                      <span className="badge-dot" style={{ background: 'var(--status-occup-dot)' }} /> Đang trực
                    </span>
                  ) : (
                    <span className="badge">
                      <span className="badge-dot" style={{ background: 'var(--text-muted)' }} /> Đã chốt
                    </span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Dialog: Open Shift */}
      {showOpenDlg && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setShowOpenDlg(false); }}>
          <div className="modal-panel" style={{ maxWidth: 400 }}>
            <div className="modal-header">
              <h3 className="modal-title">Bắt Đầu Ca Trực Lễ Tân</h3>
              <button onClick={() => setShowOpenDlg(false)} className="row-action-btn"><X style={{ width: 13, height: 13 }} /></button>
            </div>
            <form onSubmit={handleOpenShift} className="modal-body space-y-4">
              <div className="rounded-xl p-3 flex items-start gap-2.5" style={{ background: 'var(--accent-dim)', border: '1px solid var(--accent-border)' }}>
                <Info style={{ width: 16, height: 16, color: 'var(--accent)', flexShrink: 0, marginTop: 2 }} />
                <p className="text-xs leading-relaxed" style={{ color: 'var(--text-secondary)' }}>
                  Nhân viên trực <span className="font-bold" style={{ color: 'var(--text-primary)' }}>{currentUser.nhanVien?.hoTen}</span> sẽ bắt đầu ca làm việc. Vui lòng kiểm tra và nhập số tiền mặt bàn giao đầu ca trong quầy thu ngân.
                </p>
              </div>
              <div>
                <label className="label-style">Số tiền mặt bàn giao đầu ca (Vốn)</label>
                <input type="number" required value={startFloat} onChange={e => setStartFloat(Number(e.target.value))}
                  className="input-style" />
              </div>
              <div className="modal-footer" style={{ padding: 0, paddingTop: 12 }}>
                <button type="button" onClick={() => setShowOpenDlg(false)} className="btn-ghost text-[13px]">Hủy</button>
                <button type="submit" className="btn-primary text-[13px]">Bắt Đầu Ca</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Dialog: Close Shift */}
      {showCloseDlg && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setShowCloseDlg(false); }}>
          <div className="modal-panel" style={{ maxWidth: 460 }}>
            <div className="modal-header">
              <h3 className="modal-title">Chốt Ca &amp; Bàn Giao Quầy</h3>
              <button onClick={() => setShowCloseDlg(false)} className="row-action-btn"><X style={{ width: 13, height: 13 }} /></button>
            </div>
            <form onSubmit={handleCloseShift} className="modal-body space-y-4">
              {/* Summary box */}
              <div className="rounded-xl p-4 space-y-2" style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border)' }}>
                <div className="flex justify-between text-xs">
                  <span style={{ color: 'var(--text-secondary)' }}>Vốn đầu ca:</span>
                  <span className="font-bold" style={{ color: 'var(--text-primary)' }}>{fmt(activeShift?.tienMatDauCa)}đ</span>
                </div>
                <div className="flex justify-between text-xs">
                  <span style={{ color: 'var(--text-secondary)' }}>Doanh thu quầy trong ca:</span>
                  <span className="font-bold" style={{ color: 'var(--text-primary)' }}>{fmt(expectedCash - (activeShift?.tienMatDauCa || 0))}đ</span>
                </div>
                <div className="flex justify-between text-sm font-bold pt-2" style={{ borderTop: '1px solid var(--border)' }}>
                  <span style={{ color: 'var(--accent)' }}>Tổng kỳ vọng trong quầy:</span>
                  <span style={{ color: 'var(--status-avail-text)' }}>{fmt(expectedCash)}đ</span>
                </div>
              </div>

              <div>
                <label className="label-style">Số tiền mặt đếm thực tế (Bàn giao)</label>
                <input type="number" required value={closeForm.tienMatBanGiao}
                  onChange={e => setCloseForm({...closeForm, tienMatBanGiao: Number(e.target.value)})}
                  className="input-style" />
              </div>

              <div>
                <label className="label-style">Nhân viên nhận bàn giao</label>
                <select required value={closeForm.maNhanVienNhan}
                  onChange={e => setCloseForm({...closeForm, maNhanVienNhan: e.target.value})}
                  className="input-style">
                  <option value="">-- Chọn nhân viên nhận --</option>
                  {staffs.filter(s => s.maNhanVien !== currentMaNV).map(s => (
                    <option key={s.maNhanVien} value={s.maNhanVien}>{s.hoTen} ({s.maNhanVien})</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="label-style">Ghi chú đối soát</label>
                <textarea value={closeForm.ghiChu}
                  onChange={e => setCloseForm({...closeForm, ghiChu: e.target.value})} rows={2}
                  placeholder="Ghi chú chênh lệch hoặc tình trạng bàn giao..."
                  className="input-style resize-none" />
              </div>

              <div className="modal-footer" style={{ padding: 0, paddingTop: 12 }}>
                <button type="button" onClick={() => setShowCloseDlg(false)} className="btn-ghost text-[13px]">Hủy</button>
                <button type="submit" className="btn-primary text-[13px]">Chốt Ca &amp; Bàn Giao</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default GiaoCaPage;
