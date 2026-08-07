import React, { useEffect, useState, useMemo } from 'react';
import { phongAPI, chiTietDatPhongAPI, loaiPhongAPI } from '../api/api';
import { Calendar, Search, RefreshCw, Layers, SlidersHorizontal, ChevronLeft, ChevronRight, Info, Eye, X } from 'lucide-react';

const fmt = (n) => (n || 0).toLocaleString('vi-VN') + 'đ';

const STATUS_LABELS = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  PARTIALLY_CHECKED_IN: 'Đang check-in',
  CHECKED_IN: 'Đã nhận phòng',
  CHECKED_OUT: 'Đã trả phòng',
  CANCELLED: 'Đã hủy',
  NO_SHOW: 'Không đến',
  WAITLIST: 'Chờ xếp phòng',
};

const KeHoachPage = ({ setTab }) => {
  const [rooms, setRooms] = useState([]);
  const [stays, setStays] = useState([]);
  const [loaiPhongs, setLoaiPhongs] = useState([]);
  const [loading, setLoading] = useState(false);

  // Filters & State
  const [startDate, setStartDate] = useState(new Date().toISOString().substring(0, 10));
  const [numDays, setNumDays] = useState(7);
  const [searchKw, setSearchKw] = useState('');
  const [selectedLoai, setSelectedLoai] = useState('ALL');
  const [selectedTang, setSelectedTang] = useState('ALL');
  const [hoveredStay, setHoveredStay] = useState(null);
  const [activeDetailStay, setActiveDetailStay] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [rRes, sRes, lpRes] = await Promise.all([
        phongAPI.getAll(),
        chiTietDatPhongAPI.getActive(),
        loaiPhongAPI.getAll()
      ]);
      setRooms(rRes.data);
      setStays(sRes.data);
      setLoaiPhongs(lpRes.data);
    } catch (e) { console.error(e); }
    setLoading(false);
  };

  // Generate date columns
  const dates = useMemo(() => {
    const dateList = [];
    const start = new Date(startDate);
    for (let i = 0; i < numDays; i++) {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      dateList.push(d);
    }
    return dateList;
  }, [startDate, numDays]);

  // Shift start date
  const shiftDate = (days) => {
    const d = new Date(startDate);
    d.setDate(d.getDate() + days);
    setStartDate(d.toISOString().substring(0, 10));
  };

  // Filtered rooms
  const filteredRooms = rooms.filter(p => {
    const matchKw = p.maPhong.toLowerCase().includes(searchKw.toLowerCase()) ||
                    (p.loaiPhong?.tenLoaiPhong || '').toLowerCase().includes(searchKw.toLowerCase());
    const matchLoai = selectedLoai === 'ALL' || p.loaiPhong?.maLoaiPhong === selectedLoai;
    const matchTang = selectedTang === 'ALL' || String(p.tang) === selectedTang;
    return matchKw && matchLoai && matchTang;
  });

  const floors = useMemo(() => {
    return ['ALL', ...new Set(rooms.map(r => String(r.tang)).filter(Boolean))];
  }, [rooms]);

  // Helper: check if stay overlaps with the date range
  const getStayPosition = (stay, room, dateList) => {
    if (!stay.phong || stay.phong.maPhong !== room.maPhong) return null;

    const start = new Date(stay.ngayNhanThucTe || stay.datPhong?.ngayNhanDuKien);
    const end = new Date(stay.ngayTraThucTe || stay.datPhong?.ngayTraDuKien || new Date());

    // Find start column index (clamp to timeline boundaries)
    let startIndex = -1;
    let endIndex = -1;

    for (let i = 0; i < dateList.length; i++) {
      const d = dateList[i];
      const nextDay = new Date(d);
      nextDay.setDate(d.getDate() + 1);

      if (start < nextDay && startIndex === -1) {
        startIndex = i;
      }
      if (end >= d) {
        endIndex = i;
      }
    }

    if (startIndex === -1 || endIndex === -1 || startIndex > endIndex) return null;

    return {
      left: startIndex,
      width: (endIndex - startIndex) + 1,
      stay
    };
  };

  // Get color classes for Gantt stay bars according to reservation status
  const getStayStatusClass = (stay) => {
    const status = stay.datPhong?.trangThai || 'WAITLIST';
    switch (status) {
      case 'CHECKED_IN':
        return 'bg-blue-100 dark:bg-blue-950/40 text-blue-800 dark:text-blue-300 border-blue-200 dark:border-blue-900/40';
      case 'CONFIRMED':
      case 'PARTIALLY_CHECKED_IN':
        return 'bg-emerald-100 dark:bg-emerald-950/40 text-emerald-800 dark:text-emerald-300 border-emerald-200 dark:border-emerald-900/40';
      case 'CHECKED_OUT':
        return 'bg-gray-100 dark:bg-gray-800/60 text-gray-700 dark:text-gray-400 border-gray-200 dark:border-gray-700/50';
      case 'WAITLIST':
      case 'PENDING':
        return 'bg-amber-100 dark:bg-amber-950/40 text-amber-800 dark:text-amber-300 border-amber-200 dark:border-amber-900/40';
      case 'NO_SHOW':
      case 'CANCELLED':
        return 'bg-red-100 dark:bg-red-950/40 text-red-800 dark:text-red-300 border-red-200 dark:border-red-900/40';
      default:
        return 'bg-blue-100 dark:bg-blue-950/40 text-blue-800 dark:text-blue-300 border-blue-200 dark:border-blue-900/40';
    }
  };

  return (
    <div className="page-shell">
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="page-label">Lịch trình</div>
          <h1 className="page-title flex items-center gap-2">
            <Calendar style={{ width: 20, height: 20, color: 'var(--text-secondary)' }} strokeWidth={2} />
            Lịch Trình Sử Dụng Phòng
          </h1>
          <p className="page-subtitle">Theo dõi trạng thái và kế hoạch sử dụng phòng dạng timeline (Gantt Chart)</p>
        </div>
      </div>

      {/* ── Content Layout: Sidebar Filter Left, Gantt Chart Right ── */}
      <div className="grid grid-cols-1 xl:grid-cols-12 gap-5 items-start">
        
        {/* Left Column: Filter Sidebar (xl:col-span-3) */}
        <div className="xl:col-span-3 space-y-4">
          <div className="bg-[var(--bg-sidebar)] p-5 rounded-2xl border border-[var(--border-color)] space-y-4">
            <h3 className="text-xs font-bold text-[var(--text-secondary)] uppercase tracking-[0.7px] border-b border-[var(--border-color)] pb-2.5">
              Bộ lọc tìm kiếm
            </h3>
            
            {/* Date Navigator */}
            <div className="space-y-1.5">
              <label className="label-style">Khoảng thời gian</label>
              <div className="flex items-center gap-1 bg-[var(--bg-main)] border border-[var(--border-color)] rounded-xl px-2 py-1.5">
                <button onClick={() => shiftDate(-numDays)} className="p-1 text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-sidebar)] rounded-lg transition-colors"><ChevronLeft size={14}/></button>
                <input type="date" value={startDate} onChange={(e)=>setStartDate(e.target.value)}
                  className="bg-transparent border-none text-[11px] font-bold text-[var(--text-primary)] outline-none w-full text-center"/>
                <button onClick={() => shiftDate(numDays)} className="p-1 text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-sidebar)] rounded-lg transition-colors"><ChevronRight size={14}/></button>
              </div>
            </div>

            {/* Room Search */}
            <div className="space-y-1.5">
              <label className="label-style">Số phòng / Loại phòng</label>
              <div className="relative">
                <Search size={13} className="absolute left-3 top-3.5 text-[var(--text-muted)]"/>
                <input type="text" placeholder="Tìm số phòng, loại..." value={searchKw} onChange={(e)=>setSearchKw(e.target.value)}
                  className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl pl-9 pr-4 py-2.5 text-xs text-[var(--text-primary)] focus:border-blue-500 outline-none font-semibold"/>
              </div>
            </div>

            {/* Display Days */}
            <div className="space-y-1.5">
              <label className="label-style">Số ngày hiển thị</label>
              <select value={numDays} onChange={(e)=>setNumDays(Number(e.target.value))}
                className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2.5 text-xs text-[var(--text-primary)] focus:border-blue-500 outline-none font-semibold">
                <option value={7}>7 ngày</option>
                <option value={14}>14 ngày</option>
                <option value={30}>30 ngày</option>
              </select>
            </div>

            {/* Room Type */}
            <div className="space-y-1.5">
              <label className="label-style">Hạng phòng</label>
              <select value={selectedLoai} onChange={(e)=>setSelectedLoai(e.target.value)}
                className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2.5 text-xs text-[var(--text-primary)] focus:border-blue-500 outline-none font-semibold">
                <option value="ALL">Tất cả loại phòng</option>
                {loaiPhongs.map(lp => (
                  <option key={lp.maLoaiPhong} value={lp.maLoaiPhong}>{lp.tenLoaiPhong}</option>
                ))}
              </select>
            </div>

            {/* Floor Filter */}
            <div className="space-y-1.5">
              <label className="label-style">Tầng</label>
              <select value={selectedTang} onChange={(e)=>setSelectedTang(e.target.value)}
                className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2.5 text-xs text-[var(--text-primary)] focus:border-blue-500 outline-none font-semibold">
                <option value="ALL">Tất cả tầng</option>
                {floors.filter(f=>f!=='ALL').map(f => (
                  <option key={f} value={f}>Tầng {f}</option>
                ))}
              </select>
            </div>

            {/* Color Legend (CHÚ THÍCH MÀU SẮC) */}
            <div className="pt-4 border-t border-[var(--border-color)] space-y-3">
              <h4 className="text-[10px] font-bold text-[var(--text-secondary)] uppercase tracking-[0.7px]">
                Chú thích màu sắc
              </h4>
              <div className="space-y-2.5 text-xs font-semibold">
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-blue-500" />
                  <span style={{ color: 'var(--text-primary)' }}>Đang thuê (Checked-in)</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
                  <span style={{ color: 'var(--text-primary)' }}>Đã đặt trước (Confirmed)</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-gray-400" />
                  <span style={{ color: 'var(--text-primary)' }}>Đã trả phòng (Checked-out)</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-amber-500" />
                  <span style={{ color: 'var(--text-primary)' }}>Chờ xếp phòng (Waitlist)</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-red-500" />
                  <span style={{ color: 'var(--text-primary)' }}>Quá hạn / No-show</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Timeline Gantt Grid (xl:col-span-9) */}
        <div className="xl:col-span-9 bg-[var(--bg-sidebar)]/60 rounded-2xl border border-[var(--border-color)] overflow-hidden flex flex-col shadow-sm">
          {/* Dates Header */}
          <div className="flex border-b border-[var(--border-color)] bg-[var(--bg-sidebar)]/60 sticky top-0 z-10 select-none">
            <div className="w-44 shrink-0 px-4 py-3 border-r border-[var(--border-color)] text-[10px] font-bold text-[var(--text-secondary)] uppercase tracking-wider flex items-center gap-1.5">
              <Layers size={13}/> Phòng
            </div>
            <div className="flex-1 flex overflow-hidden">
              {dates.map((date, idx) => {
                const isToday = date.toDateString() === new Date().toDateString();
                return (
                  <div key={idx} className={`flex-1 text-center py-2.5 border-r border-[var(--border-color)]/60 flex flex-col justify-center min-w-[70px] ${isToday ? 'bg-blue-600/10' : ''}`}>
                    <div className={`text-[10px] uppercase font-bold ${isToday ? 'text-blue-600 dark:text-blue-400' : 'text-[var(--text-secondary)]'}`}>
                      {date.toLocaleDateString('vi-VN', { weekday: 'short' })}
                    </div>
                    <div className={`text-sm font-extrabold mt-0.5 ${isToday ? 'text-blue-600 dark:text-blue-400' : 'text-[var(--text-secondary)]'}`}>
                      {date.getDate()}
                    </div>
                    <div className="text-[9px] text-[var(--text-secondary)] font-semibold mt-0.5">
                      Thg {date.getMonth() + 1}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Rooms Rows */}
          <div className="divide-y divide-[var(--border-color)] max-h-[600px] overflow-y-auto custom-scrollbar">
            {loading ? (
              <div className="py-20 text-center text-[var(--text-secondary)] font-semibold">Đang tải biểu đồ kế hoạch...</div>
            ) : filteredRooms.length === 0 ? (
              <div className="py-20 text-center text-[var(--text-secondary)] font-semibold">Không có phòng nào thỏa mãn điều kiện lọc</div>
            ) : (
              filteredRooms.map((room) => (
                <div key={room.maPhong} className="flex relative hover:bg-[var(--bg-main)] min-h-[52px]">
                  
                  {/* Room Info Block */}
                  <div className="w-44 shrink-0 px-4 py-2 border-r border-[var(--border-color)] flex flex-col justify-center bg-[var(--bg-main)]/40 z-10 select-none">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-extrabold text-[var(--text-primary)]">P.{room.maPhong}</span>
                      <span className={`w-2 h-2 rounded-full ${
                        room.trangThai === 'AVAILABLE' ? 'bg-emerald-500' :
                        room.trangThai === 'OCCUPIED' ? 'bg-blue-500' :
                        room.trangThai === 'CLEANING' ? 'bg-amber-500' : 'bg-red-500'
                      }`}/>
                    </div>
                    <span className="text-[9px] text-[var(--text-secondary)] font-semibold uppercase truncate mt-0.5">{room.loaiPhong?.tenLoaiPhong}</span>
                  </div>

                  {/* Timeline cells */}
                  <div className="flex-1 flex relative overflow-hidden">
                    {/* Grid background lines */}
                    {dates.map((_, idx) => (
                      <div key={idx} className="flex-1 border-r border-[var(--border-color)]/30 min-w-[70px]" />
                    ))}

                    {/* Absolute booking bars */}
                    {stays.map((stay) => {
                      const pos = getStayPosition(stay, room, dates);
                      if (!pos) return null;

                      const widthPercent = (pos.width / numDays) * 100;
                      const leftPercent = (pos.left / numDays) * 100;

                      const barClass = getStayStatusClass(stay);

                      return (
                        <div
                          key={stay.maChiTiet}
                          onMouseEnter={() => setHoveredStay(stay)}
                          onMouseLeave={() => setHoveredStay(null)}
                          onClick={() => setActiveDetailStay(stay)}
                          className={`absolute top-2 h-8 rounded-lg flex items-center px-3 cursor-pointer border select-none transition-all shadow-sm group ${barClass}`}
                          style={{
                            left: `${leftPercent}%`,
                            width: `calc(${widthPercent}% - 6px)`,
                            marginLeft: '3px'
                          }}
                        >
                          <div className="text-xs font-bold truncate">
                            {stay.khachHang?.hoTen || stay.datPhong?.tenDoan || 'Khách'}
                          </div>
                          {/* Interactive highlight effect */}
                          <div className="absolute inset-0 rounded-lg group-hover:bg-white/5 pointer-events-none"/>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

      </div>

      {/* Bottom Tooltip hover detail bar */}
      {hoveredStay && !activeDetailStay && (
        <div className="mt-4 bg-blue-50 dark:bg-blue-950/10 p-4 rounded-xl border border-blue-200 dark:border-blue-500/30 flex items-start gap-3 animate-fadeIn">
          <Info className="w-5 h-5 text-blue-400 shrink-0 mt-0.5"/>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-xs">
            <div>
              <div className="text-[var(--text-secondary)] font-bold uppercase tracking-wider text-[10px]">Tên khách / Đoàn</div>
              <div className="text-[var(--text-primary)] font-extrabold text-sm mt-0.5">{hoveredStay.khachHang?.hoTen || hoveredStay.datPhong?.tenDoan || 'N/A'}</div>
            </div>
            <div>
              <div className="text-[var(--text-secondary)] font-bold uppercase tracking-wider text-[10px]">Mã đặt phòng</div>
              <div className="text-[var(--text-secondary)] font-mono mt-0.5 font-semibold">{hoveredStay.datPhong?.maDatPhong || 'N/A'}</div>
            </div>
            <div>
              <div className="text-[var(--text-secondary)] font-bold uppercase tracking-wider text-[10px]">Ngày Nhận</div>
              <div className="text-[var(--text-secondary)] mt-0.5 font-semibold">
                {new Date(hoveredStay.ngayNhanThucTe || hoveredStay.datPhong?.ngayNhanDuKien).toLocaleDateString('vi-VN')}
              </div>
            </div>
            <div>
              <div className="text-[var(--text-secondary)] font-bold uppercase tracking-wider text-[10px]">Ngày Trả (Dự Kiến)</div>
              <div className="text-[var(--text-secondary)] mt-0.5 font-semibold">
                {new Date(hoveredStay.ngayTraThucTe || hoveredStay.datPhong?.ngayTraDuKien).toLocaleDateString('vi-VN')}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Left-Click Detailed Information Modal Popup */}
      {activeDetailStay && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setActiveDetailStay(null); }}>
          <div className="modal-panel" style={{ maxWidth: 460 }}>
            <div className="modal-header">
              <h3 className="modal-title">Chi tiết lưu trú</h3>
              <button onClick={() => setActiveDetailStay(null)} className="row-action-btn">
                <X style={{ width: 13, height: 13 }} />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <div className="rounded-xl p-4 flex items-center justify-between" style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border)' }}>
                <div>
                  <h4 className="text-[16px] font-bold" style={{ color: 'var(--text-primary)' }}>
                    {activeDetailStay.khachHang?.hoTen || 'Khách lẻ'}
                  </h4>
                  <div className="font-mono-data text-[10px] mt-0.5" style={{ color: 'var(--text-secondary)' }}>
                    Mã đơn đặt: {activeDetailStay.datPhong?.maDatPhong} | Phòng: {activeDetailStay.phong?.maPhong}
                  </div>
                </div>
                <span className={`badge ${
                  activeDetailStay.datPhong?.trangThai === 'CHECKED_IN' ? 'badge-occupied' :
                  activeDetailStay.datPhong?.trangThai === 'CONFIRMED' ? 'badge-occupied' :
                  activeDetailStay.datPhong?.trangThai === 'CHECKED_OUT' ? 'badge-available' : 'badge-maintenance'
                }`}>
                  {STATUS_LABELS[activeDetailStay.datPhong?.trangThai] || activeDetailStay.datPhong?.trangThai}
                </span>
              </div>

              <div className="grid grid-cols-2 gap-2.5 text-xs">
                {[
                  ['Số điện thoại', activeDetailStay.khachHang?.sdt || '—'],
                  ['Số CCCD/Hộ chiếu', activeDetailStay.khachHang?.cccd || '—'],
                  ['Hạng phòng', activeDetailStay.phong?.loaiPhong?.tenLoaiPhong || '—'],
                  ['Giá chốt', activeDetailStay.giaThucTeChot ? `${fmt(activeDetailStay.giaThucTeChot)} / đêm` : '—'],
                  ['Thời gian nhận', activeDetailStay.ngayNhanThucTe ? new Date(activeDetailStay.ngayNhanThucTe).toLocaleString('vi-VN') : (activeDetailStay.datPhong?.ngayNhanDuKien ? new Date(activeDetailStay.datPhong.ngayNhanDuKien).toLocaleString('vi-VN') : '—')],
                  ['Thời gian trả', activeDetailStay.ngayTraThucTe ? new Date(activeDetailStay.ngayTraThucTe).toLocaleString('vi-VN') : (activeDetailStay.datPhong?.ngayTraDuKien ? `${new Date(activeDetailStay.datPhong.ngayTraDuKien).toLocaleString('vi-VN')} (Dự kiến)` : '—')],
                  ['Tiền đặt cọc', activeDetailStay.datPhong?.tienDatCoc ? fmt(activeDetailStay.datPhong.tienDatCoc) : '0đ'],
                  ['Tổng tạm tính', activeDetailStay.datPhong?.tongTien ? `${fmt(activeDetailStay.datPhong.tongTien)} (Tạm tính)` : '—'],
                ].map(([k, v]) => (
                  <div key={k} className="rounded-xl p-2.5" style={{ background: 'var(--bg-elevated)' }}>
                    <div className="text-[9px] font-bold uppercase tracking-wider" style={{ color: 'var(--text-muted)' }}>{k}</div>
                    <div className="text-[12px] font-semibold mt-0.5 truncate" style={{ color: 'var(--text-primary)' }}>{v}</div>
                  </div>
                ))}
              </div>
            </div>
            <div className="modal-footer">
              <button onClick={() => setActiveDetailStay(null)} className="btn-ghost text-[13px]">Đóng</button>
              <button
                onClick={() => {
                  setTab('datphong');
                  setActiveDetailStay(null);
                }}
                className="btn-primary text-[13px]"
              >
                Xem đơn đặt
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default KeHoachPage;
