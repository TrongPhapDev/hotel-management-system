import React, { useEffect, useState } from 'react';
import { hoaDonAPI } from '../api/api';
import { Receipt, CreditCard, X, Search, Printer, Eye, Phone } from 'lucide-react';
import { RowContextMenu, useContextMenu } from '../components/ContextMenu';

const fmt = (n) => (n || 0).toLocaleString('vi-VN') + ' ₫';
const fmtDate = (dt) => {
  if (!dt) return '—';
  const d = new Date(dt);
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const STATUS_BADGE = {
  PAID:      'badge-available',
  CANCELLED: '',
  REFUNDED:  'badge-occupied',
  DEFAULT:   'badge-cleaning',
};

const getStatusLabel = (t) => {
  switch (t) {
    case 'PAID': return 'Đã thanh toán';
    case 'CANCELLED': return 'Đã hủy';
    case 'REFUNDED': return 'Đã hoàn tiền';
    default: return 'Chưa thanh toán';
  }
};

const HoaDonPage = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [payModal, setPayModal] = useState(null);
  const [phuongThuc, setPhuongThuc] = useState('CASH');
  const [viewDetail, setViewDetail] = useState(null);
  const { ctxMenu, openCtxMenu, closeCtxMenu } = useContextMenu();

  const [searchKw, setSearchKw] = useState('');
  const [timeRange, setTimeRange] = useState('all');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('ALL');

  const fetchData = () => {
    setLoading(true);
    hoaDonAPI.getAll()
      .then(r => setItems(r.data))
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(); }, []);

  const handleThanhToan = async () => {
    try {
      await hoaDonAPI.thanhToan(payModal.maHoaDon, phuongThuc);
      setPayModal(null);
      fetchData();
    } catch (e) {
      alert(e.response?.data || 'Lỗi!');
    }
  };

  const handleCtxAction = (action, hd) => {
    switch (action) {
      case 'view': setViewDetail(hd); break;
      case 'pay': setPayModal(hd); setPhuongThuc('CASH'); break;
      case 'print': alert(`In hóa đơn ${hd.maHoaDon} thành công!`); break;
    }
  };

  const getCtxItems = (hd) => {
    const its = [
      { label: 'Xem chi tiết hóa đơn', action: 'view' },
      { label: 'In hóa đơn (PDF/Print)', action: 'print' },
    ];
    if (hd.trangThai !== 'PAID') {
      its.push({ separator: true });
      its.push({ label: 'Thanh toán ngay', action: 'pay', bold: true });
    }
    return its;
  };

  const getDatesByRange = (rangeType) => {
    const today = new Date(); today.setHours(0,0,0,0);
    const getStart = (d) => { d.setHours(0,0,0,0); return d; };
    const getEnd = (d) => { d.setHours(23,59,59,999); return d; };
    if (rangeType === 'today') return { from: getStart(new Date()), to: getEnd(new Date()) };
    if (rangeType === 'yesterday') { const y=new Date(); y.setDate(y.getDate()-1); return { from: getStart(y), to: getEnd(new Date(y)) }; }
    if (rangeType === '7days') { const d=new Date(); d.setDate(d.getDate()-7); return { from: getStart(d), to: getEnd(new Date()) }; }
    if (rangeType === '30days') { const d=new Date(); d.setDate(d.getDate()-30); return { from: getStart(d), to: getEnd(new Date()) }; }
    if (rangeType === 'thisMonth') return { from: new Date(today.getFullYear(), today.getMonth(), 1), to: getEnd(new Date(today.getFullYear(), today.getMonth()+1, 0)) };
    if (rangeType === 'prevMonth') return { from: new Date(today.getFullYear(), today.getMonth()-1, 1), to: getEnd(new Date(today.getFullYear(), today.getMonth(), 0)) };
    return { from: null, to: null };
  };

  const clearFilters = () => { setSearchKw(''); setTimeRange('all'); setStartDate(''); setEndDate(''); setSelectedStatus('ALL'); };

  const filteredItems = items.filter(hd => {
    const kw = searchKw.trim().toLowerCase();
    const matchKw = !kw ||
      hd.maHoaDon?.toLowerCase().includes(kw) ||
      hd.datPhong?.khachHang?.hoTen?.toLowerCase().includes(kw) ||
      hd.datPhong?.khachHang?.sdt?.includes(kw) ||
      (hd.datPhong?.tenDoan && hd.datPhong.tenDoan.toLowerCase().includes(kw));

    let matchStatus = true;
    if (selectedStatus === 'PAID') matchStatus = (hd.trangThai === 'PAID');
    else if (selectedStatus === 'UNPAID') matchStatus = (hd.trangThai !== 'PAID');

    let matchDate = true;
    if (timeRange !== 'all' && timeRange !== 'custom') {
      const { from, to } = getDatesByRange(timeRange);
      const lap = new Date(hd.ngayLap);
      if (from && lap < from) matchDate = false;
      if (to && lap > to) matchDate = false;
    } else if (timeRange === 'custom') {
      const lap = new Date(hd.ngayLap);
      if (startDate) { const f = new Date(startDate); f.setHours(0,0,0,0); if (lap < f) matchDate = false; }
      if (endDate) { const t = new Date(endDate); t.setHours(23,59,59,999); if (lap > t) matchDate = false; }
    }

    return matchKw && matchStatus && matchDate;
  });

  const totalCount = filteredItems.length;
  const totalRevenue = filteredItems.filter(x => x.trangThai === 'PAID').reduce((s, x) => s + (x.tongThanhToan || 0), 0);
  const hasFilter = searchKw || timeRange !== 'all' || selectedStatus !== 'ALL';

  return (
    <div className="page-shell" onClick={() => closeCtxMenu()}>
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="page-label">Thu ngân</div>
          <h1 className="page-title flex items-center gap-2">
            <Receipt style={{ width: 20, height: 20, color: 'var(--text-secondary)' }} strokeWidth={2} />
            Lịch sử Hóa đơn
          </h1>
          <p className="page-subtitle">Tra cứu toàn bộ hóa đơn đã xuất với công cụ lọc thông minh · Double-click để xem chi tiết</p>
        </div>
      </div>

      {/* KPI summary */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-[10px]">
        <div className="kpi-card">
          <span className="kpi-label">Tổng hóa đơn</span>
          <span className="kpi-value">{items.length}</span>
        </div>
        <div className="kpi-card">
          <span className="kpi-label">Đã thanh toán</span>
          <span className="kpi-value">{items.filter(x => x.trangThai === 'PAID').length}</span>
        </div>
        <div className="kpi-card">
          <span className="kpi-label">Chưa thanh toán</span>
          <span className="kpi-value">{items.filter(x => x.trangThai !== 'PAID').length}</span>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="filter-bar">
        <div className="filter-search">
          <Search className="filter-search-icon" style={{ width: 14, height: 14 }} />
          <input type="text" placeholder="Tìm Mã HĐ, Tên KH, SĐT..."
            value={searchKw} onChange={e => setSearchKw(e.target.value)} />
          {searchKw && (
            <button onClick={() => setSearchKw('')} className="filter-clear-btn">
              <X style={{ width: 10, height: 10 }} />
            </button>
          )}
        </div>

        <div className={`filter-select-wrap ${timeRange !== 'all' ? 'active' : ''}`}>
          <select value={timeRange} onChange={e => setTimeRange(e.target.value)}>
            <option value="all">Tất cả thời gian</option>
            <option value="today">Hôm nay</option>
            <option value="yesterday">Hôm qua</option>
            <option value="7days">7 ngày qua</option>
            <option value="30days">30 ngày qua</option>
            <option value="thisMonth">Tháng này</option>
            <option value="prevMonth">Tháng trước</option>
            <option value="custom">Tùy chỉnh...</option>
          </select>
        </div>

        {timeRange === 'custom' && (
          <div className="flex items-center gap-2">
            <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)} className="input-style" style={{ height: 40, padding: '0 12px', width: 'auto' }} />
            <span style={{ color: 'var(--text-muted)', fontSize: 12 }}>đến</span>
            <input type="date" value={endDate} onChange={e => setEndDate(e.target.value)} className="input-style" style={{ height: 40, padding: '0 12px', width: 'auto' }} />
          </div>
        )}

        <div className={`filter-select-wrap ${selectedStatus !== 'ALL' ? 'active' : ''}`}>
          <select value={selectedStatus} onChange={e => setSelectedStatus(e.target.value)}>
            <option value="ALL">Tất cả trạng thái</option>
            <option value="PAID">Đã thanh toán</option>
            <option value="UNPAID">Chưa thanh toán</option>
          </select>
        </div>

        {hasFilter && (
          <button onClick={clearFilters} className="filter-reset-btn">Xoá lọc</button>
        )}
        <span className="filter-result-count">{filteredItems.length}/{items.length} hóa đơn</span>
      </div>

      {/* Table */}
      <div className="section-box overflow-x-auto">
        <table className="data-table">
          <thead>
            <tr>
              {['Mã HĐ', 'Loại', 'Khách hàng', 'Ngày lập', 'Tiền phòng', 'Dịch vụ', 'Tổng thanh toán', 'Trạng thái'].map(h => (
                <th key={h}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={8} className="py-16 text-center">
                <div className="spinner mx-auto mb-2" />
                <div style={{ color: 'var(--text-muted)', fontSize: 12 }}>Đang tải...</div>
              </td></tr>
            ) : filteredItems.length === 0 ? (
              <tr><td colSpan={8} className="py-14 text-center" style={{ color: 'var(--text-muted)', fontSize: 13 }}>Không có hóa đơn thỏa mãn bộ lọc</td></tr>
            ) : filteredItems.map(hd => {
              const isDoan = hd.datPhong?.loaiKhach === 'DOAN';
              let tenKh = hd.datPhong?.khachHang?.hoTen || 'Khách vãng lai';
              if (isDoan && hd.datPhong?.tenDoan) tenKh = `${hd.datPhong.tenDoan} (${tenKh})`;
              const sdt = hd.datPhong?.khachHang?.sdt || '';
              const badgeCls = STATUS_BADGE[hd.trangThai] || STATUS_BADGE.DEFAULT;

              return (
                <tr key={hd.maHoaDon}
                  className="cursor-pointer group"
                  onDoubleClick={() => setViewDetail(hd)}
                  onContextMenu={e => { e.stopPropagation(); openCtxMenu(e, hd); }}>
                  <td className="font-mono-data font-bold text-[12px]" style={{ color: 'var(--accent)' }}>{hd.maHoaDon}</td>
                  <td className="font-bold text-[12px]" style={{ color: isDoan ? 'var(--status-clean-text)' : 'var(--status-occup-text)' }}>
                    {isDoan ? 'Đoàn' : 'Khách lẻ'}
                  </td>
                  <td>
                    <div className="flex items-center gap-2.5">
                      <div className="w-[26px] h-[26px] rounded-full flex items-center justify-center text-[10px] font-black shrink-0"
                        style={{ background: 'var(--accent-dim)', color: 'var(--accent)', border: '1px solid var(--accent-border)' }}>
                        {tenKh.charAt(0)}
                      </div>
                      <div>
                        <div className="font-semibold text-[13px]" style={{ color: 'var(--text-primary)' }}>{tenKh}</div>
                        <div className="text-[11px] flex items-center gap-0.5" style={{ color: 'var(--text-secondary)' }}>
                          {sdt ? <span className="flex items-center gap-0.5"><Phone size={9} />{sdt}</span> : '—'}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td className="font-mono-data text-[11px]" style={{ color: 'var(--text-muted)' }}>{fmtDate(hd.ngayLap)}</td>
                  <td style={{ color: 'var(--text-secondary)' }}>{fmt(hd.tongTienPhong)}</td>
                  <td style={{ color: 'var(--text-secondary)' }}>{fmt(hd.tongTienDichVu)}</td>
                  <td className="font-bold" style={{ color: 'var(--status-avail-text)' }}>{fmt(hd.tongThanhToan)}</td>
                  <td className="row-actions-cell">
                    <span className={`badge ${badgeCls}`}>
                      {getStatusLabel(hd.trangThai)}
                    </span>
                    {/* Hover Actions */}
                    <div className="row-hover-actions">
                      <button onClick={e => { e.stopPropagation(); setViewDetail(hd); }} className="row-action-btn" title="Xem chi tiết">
                        <Eye style={{ width: 12, height: 12 }} />
                      </button>
                      {hd.trangThai !== 'PAID' && (
                        <button onClick={e => { e.stopPropagation(); setPayModal(hd); setPhuongThuc('CASH'); }}
                          className="row-action-btn" title="Thanh toán" style={{ color: 'var(--status-avail-text)' }}>
                          <CreditCard style={{ width: 11, height: 11 }} />
                        </button>
                      )}
                      <button onClick={e => { e.stopPropagation(); alert(`In hóa đơn ${hd.maHoaDon}!`); }} className="row-action-btn" title="In hóa đơn">
                        <Printer style={{ width: 11, height: 11 }} />
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {!loading && filteredItems.length > 0 && (
          <div className="table-footer">
            <span>{totalCount} hóa đơn — Tổng doanh thu đã thu: <strong style={{ color: 'var(--status-avail-text)' }}>{fmt(totalRevenue)}</strong></span>
            <span>Double-click để xem · Chuột phải để tùy chọn</span>
          </div>
        )}
      </div>

      {/* Context Menu */}
      {ctxMenu && (
        <RowContextMenu menu={ctxMenu} items={getCtxItems(ctxMenu.item)} onAction={handleCtxAction} onClose={closeCtxMenu}
          title={`Hóa đơn ${ctxMenu.item?.maHoaDon}`} subtitle={ctxMenu.item?.datPhong?.khachHang?.hoTen} />
      )}

      {/* Pay Modal */}
      {payModal && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setPayModal(null); }}>
          <div className="modal-panel" style={{ maxWidth: 440 }}>
            <div className="modal-header">
              <h3 className="modal-title">Thanh toán Hóa đơn</h3>
              <button onClick={() => setPayModal(null)} className="row-action-btn"><X style={{ width: 13, height: 13 }} /></button>
            </div>
            <div className="modal-body space-y-4">
              <div className="rounded-xl space-y-2.5 p-4" style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border)' }}>
                <div className="flex justify-between text-sm"><span style={{ color: 'var(--text-secondary)' }}>Mã hóa đơn</span><span className="font-mono font-bold" style={{ color: 'var(--text-primary)' }}>{payModal.maHoaDon}</span></div>
                <div className="flex justify-between text-sm"><span style={{ color: 'var(--text-secondary)' }}>Tiền phòng</span><span style={{ color: 'var(--text-primary)' }}>{fmt(payModal.tongTienPhong)}</span></div>
                <div className="flex justify-between text-sm"><span style={{ color: 'var(--text-secondary)' }}>Tiền dịch vụ</span><span style={{ color: 'var(--text-primary)' }}>{fmt(payModal.tongTienDichVu)}</span></div>
                {payModal.tienGiamKhuyenMai > 0 && <div className="flex justify-between text-sm"><span style={{ color: 'var(--text-secondary)' }}>Giảm giá</span><span style={{ color: 'var(--status-maint-text)' }}>-{fmt(payModal.tienGiamKhuyenMai)}</span></div>}
                <div className="flex justify-between text-base font-bold pt-2" style={{ borderTop: '1px solid var(--border)' }}>
                  <span style={{ color: 'var(--text-primary)' }}>Tổng thanh toán</span>
                  <span style={{ color: 'var(--status-avail-text)', fontSize: 16 }}>{fmt(payModal.tongThanhToan)}</span>
                </div>
              </div>
              <div>
                <label className="label-style">Phương thức thanh toán</label>
                <div className="grid grid-cols-3 gap-2 mt-2">
                  {[{ val: 'CASH', label: 'Tiền mặt' }, { val: 'CARD', label: 'Thẻ ngân hàng' }, { val: 'TRANSFER', label: 'Chuyển khoản' }].map(p => (
                    <button key={p.val} type="button" onClick={() => setPhuongThuc(p.val)}
                      className="py-2.5 text-xs font-bold rounded-xl border transition-all"
                      style={phuongThuc === p.val
                        ? { background: 'var(--accent)', color: '#fff', borderColor: 'var(--accent)', boxShadow: 'var(--shadow-accent)' }
                        : { background: 'var(--bg-main)', color: 'var(--text-secondary)', borderColor: 'var(--border)' }}>
                      {p.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button onClick={() => setPayModal(null)} className="btn-ghost text-[13px]">Hủy</button>
              <button onClick={handleThanhToan} className="btn-primary text-[13px]">Xác nhận thanh toán</button>
            </div>
          </div>
        </div>
      )}

      {/* Detail Modal */}
      {viewDetail && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setViewDetail(null); }}>
          <div className="modal-panel" style={{ maxWidth: 500 }}>
            <div className="modal-header">
              <h3 className="modal-title flex items-center gap-2">
                <Receipt style={{ width: 16, height: 16, color: 'var(--accent)' }} />
                Chi tiết Hóa đơn {viewDetail.maHoaDon}
              </h3>
              <button onClick={() => setViewDetail(null)} className="row-action-btn"><X style={{ width: 13, height: 13 }} /></button>
            </div>
            <div className="modal-body space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-xl p-3" style={{ background: 'var(--bg-elevated)' }}>
                  <div className="text-[9px] font-bold uppercase tracking-wider mb-1" style={{ color: 'var(--text-muted)' }}>Khách hàng</div>
                  <div className="font-bold" style={{ color: 'var(--text-primary)' }}>{viewDetail.datPhong?.khachHang?.hoTen || 'Khách vãng lai'}</div>
                  <div className="text-[11px] mt-0.5" style={{ color: 'var(--text-secondary)' }}>{viewDetail.datPhong?.khachHang?.sdt}</div>
                </div>
                <div className="rounded-xl p-3" style={{ background: 'var(--bg-elevated)' }}>
                  <div className="text-[9px] font-bold uppercase tracking-wider mb-1" style={{ color: 'var(--text-muted)' }}>Nhân viên lập</div>
                  <div className="font-bold" style={{ color: 'var(--text-primary)' }}>{viewDetail.nhanVien?.hoTen || 'Lễ tân'}</div>
                  <div className="text-[11px] mt-0.5 font-mono" style={{ color: 'var(--text-secondary)' }}>{viewDetail.nhanVien?.maNhanVien}</div>
                </div>
              </div>

              <div className="rounded-xl p-4 space-y-2.5" style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border)' }}>
                <div className="flex justify-between text-sm"><span style={{ color: 'var(--text-secondary)' }}>Ngày lập:</span><span className="font-bold" style={{ color: 'var(--text-primary)' }}>{fmtDate(viewDetail.ngayLap)}</span></div>
                <div className="flex justify-between text-sm"><span style={{ color: 'var(--text-secondary)' }}>Tiền phòng:</span><span className="font-bold" style={{ color: 'var(--text-primary)' }}>{fmt(viewDetail.tongTienPhong)}</span></div>
                <div className="flex justify-between text-sm"><span style={{ color: 'var(--text-secondary)' }}>Tiền dịch vụ:</span><span className="font-bold" style={{ color: 'var(--text-primary)' }}>{fmt(viewDetail.tongTienDichVu)}</span></div>
                {viewDetail.tienDatCoc > 0 && <div className="flex justify-between text-sm"><span style={{ color: 'var(--text-secondary)' }}>Đã đặt cọc:</span><span>-{fmt(viewDetail.tienDatCoc)}</span></div>}
                {viewDetail.tienGiamKhuyenMai > 0 && <div className="flex justify-between text-sm"><span style={{ color: 'var(--text-secondary)' }}>Khuyến mãi giảm:</span><span style={{ color: 'var(--status-maint-text)' }}>-{fmt(viewDetail.tienGiamKhuyenMai)}</span></div>}
                <div className="flex justify-between text-base font-black pt-2" style={{ borderTop: '1px solid var(--border)' }}>
                  <span style={{ color: 'var(--text-primary)' }}>Tổng thanh toán:</span>
                  <span style={{ color: 'var(--status-avail-text)' }}>{fmt(viewDetail.tongThanhToan)}</span>
                </div>
              </div>

              <div className="flex justify-between items-center text-xs rounded-xl px-3 py-2.5" style={{ background: 'var(--bg-main)', border: '1px solid var(--border)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Phương thức: <strong style={{ color: 'var(--accent)' }}>{viewDetail.phuongThucThanhToan || 'CASH'}</strong></span>
                <span className={`badge ${STATUS_BADGE[viewDetail.trangThai] || STATUS_BADGE.DEFAULT}`}>{getStatusLabel(viewDetail.trangThai)}</span>
              </div>
            </div>
            <div className="modal-footer">
              <button onClick={() => setViewDetail(null)} className="btn-ghost text-[13px]">Đóng</button>
              <button onClick={() => alert(`In hóa đơn ${viewDetail.maHoaDon}!`)} className="btn-primary text-[13px]">
                <Printer style={{ width: 13, height: 13 }} /> In hóa đơn
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default HoaDonPage;
