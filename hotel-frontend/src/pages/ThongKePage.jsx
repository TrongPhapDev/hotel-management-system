import React, { useEffect, useState } from 'react';
import { BarChart3, RefreshCw, Download } from 'lucide-react';
import { thongKeAPI } from '../api/api';

const fmt = (n) => (n||0).toLocaleString('vi-VN');
const fmtM = (n) => n >= 1000000 ? `${(n/1000000).toFixed(1)}M` : n >= 1000 ? `${(n/1000).toFixed(0)}K` : String(n);

const KY_OPTIONS = [
  { key: '7ngay', label: '7 ngày' },
  { key: 'thang', label: 'Tháng này' },
  { key: 'quy',   label: 'Quý này' },
  { key: 'nam',   label: 'Năm nay' },
];

const KpiCard = ({ label, value, sub, trend, color }) => (
  <div className="bg-[var(--bg-card)] rounded-[10px] p-[14px_16px] border-[0.5px] border-[var(--border-tertiary)] flex flex-col font-semibold shadow-sm">
    <div className="text-[10px] font-medium uppercase tracking-[0.6px]" style={{ color: 'var(--text-tertiary)' }}>{label}</div>
    <div className={`text-2xl font-medium mt-1 ${color || 'text-[var(--text-primary)]'}`}>{value}</div>
    <div className="text-[11px] font-normal mt-[5px]" style={{ color: 'var(--text-tertiary)' }}>{sub}</div>
    {trend != null && trend !== 0 && (
      <div className={`text-xs font-bold mt-1 ${trend > 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-600 dark:text-red-400'}`}>
        {trend > 0 ? '↑' : '↓'} {Math.abs(trend)}% vs kỳ trước
      </div>
    )}
  </div>
);

const BarChart = ({ data, maxVal }) => {
  const max = maxVal || Math.max(...data.map(d => d.val), 1);

  return (
    <div className="flex items-end gap-1 h-32 px-2">
      {data.map((d, i) => (
        <div key={i} className="flex flex-col items-center gap-1 flex-1">
          <div className="text-[9px] text-[var(--text-secondary)] font-semibold">{d.val > 0 ? fmtM(d.val) : ''}</div>
          <div className="w-full rounded-t-md transition-all"
            style={{ height: `${Math.max(4, (d.val / max) * 96)}px`, backgroundColor: d.today ? '#4361EE' : '#4361EE30' }}/>
          <div className={`text-[9px] font-semibold ${d.today ? 'text-blue-600 dark:text-blue-400' : 'text-[var(--text-secondary)]'}`}>{d.label}</div>
        </div>
      ))}
    </div>
  );
};

const ThongKePage = () => {
  const [ky, setKy] = useState('thang');
  const [kpiData, setKpiData] = useState({});
  const [revenue7, setRevenue7] = useState([]);
  const [revenueByDay, setRevenueByDay] = useState([]);
  const [topPhong, setTopPhong] = useState([]);
  const [topDichVu, setTopDichVu] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [kpiRes, rev7Res, revByDayRes, topPRes, topDVRes] = await Promise.all([
        thongKeAPI.getKy(ky),
        thongKeAPI.getDoanhThu7Ngay(),
        thongKeAPI.getDoanhThuTheoNgay(ky),
        thongKeAPI.getTopPhong(5, ky),
        thongKeAPI.getTopDichVu(5, ky),
      ]);
      setKpiData(kpiRes.data || {});
      setRevenue7(rev7Res.data || []);
      setRevenueByDay(revByDayRes.data || []);
      setTopPhong(topPRes.data || []);
      setTopDichVu(topDVRes.data || []);
    } catch (e) { console.error(e); }
    setLoading(false);
  };

  useEffect(() => { fetchData(); }, [ky]);

  const calcTrend = (cur, prev) => {
    if (!prev) return cur > 0 ? 100 : 0;
    return Math.round((cur - prev) * 100 / prev);
  };

  // Build 7-day bar chart data
  const today = new Date();
  const DOW = ['CN','T2','T3','T4','T5','T6','T7'];
  const chart7 = revenue7.map((r, i) => {
    const d = new Date(today); d.setDate(d.getDate() - (6 - i));
    return { label: DOW[d.getDay()], val: r[1] || 0, today: i === 6 };
  });

  const dt = kpiData.doanhThu || 0;
  const dtTruoc = kpiData.doanhThuTruoc || 0;
  const tongPhong = kpiData.phongDangThue || 0;

  return (
    <div className="page-shell">
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="page-label">Báo cáo</div>
          <h1 className="page-title flex items-center gap-2">
            <BarChart3 style={{ width: 20, height: 20, color: 'var(--text-secondary)' }} strokeWidth={2} />Báo cáo &amp; Thống kê
          </h1>
          <p className="page-subtitle">Tổng quan hiệu quả kinh doanh khách sạn theo kỳ</p>
        </div>
        <div className="flex items-center gap-2">
          {KY_OPTIONS.map(k => (
            <button key={k.key} onClick={() => setKy(k.key)}
              className="px-3.5 py-1.5 text-xs font-bold rounded-lg transition-all"
              style={ky===k.key
                ? { background: 'var(--accent)', color: '#fff', boxShadow: 'var(--shadow-accent)' }
                : { background: 'var(--bg-sidebar)', border: '1px solid var(--border)', color: 'var(--text-secondary)' }}>
              {k.label}
            </button>
          ))}
        </div>
      </div>

      {/* KPI Row */}
      <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-[10px]">
        <KpiCard label="Doanh thu" value={`${fmtM(dt)}đ`} sub={KY_OPTIONS.find(k=>k.key===ky)?.label} trend={calcTrend(dt,dtTruoc)} color="text-emerald-400"/>
        <KpiCard label="Công suất" value={`${tongPhong ? Math.round(tongPhong*100/(tongPhong+1)):0}%`} sub={`${tongPhong} phòng đang thuê`}/>
        <KpiCard label="Khách mới" value={kpiData.khachMoi||0} sub="Trong kỳ" trend={calcTrend(kpiData.khachMoi||0, kpiData.khachMoiTruoc||0)} color="text-sky-400"/>
        <KpiCard label="Lượt check-in" value={kpiData.luotDatPhong||0} sub="Trong kỳ" trend={calcTrend(kpiData.luotDatPhong||0, kpiData.luotDatPhongTruoc||0)}/>
        <KpiCard label="Phòng thuê" value={tongPhong} sub="Hiện tại" color="text-orange-400"/>
        <KpiCard label="DV bán thêm" value={`${fmtM(kpiData.doanhThuDV||0)}đ`} sub="Trong kỳ" trend={calcTrend(kpiData.doanhThuDV||0, kpiData.doanhThuDVTruoc||0)} color="text-violet-400"/>
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Revenue 7 days */}
        <div className="bg-[var(--bg-sidebar)] rounded-2xl border border-[var(--border-color)] p-5">
          <div className="flex items-center justify-between mb-4">
            <div>
              <div className="text-sm font-bold text-[var(--text-primary)]">Doanh thu 7 ngày gần nhất</div>
              <div className="text-xs text-[var(--text-secondary)] mt-0.5 font-semibold">Đơn vị: VNĐ</div>
            </div>
          </div>
          {loading ? <div className="h-32 flex items-center justify-center text-[var(--text-secondary)] text-sm font-semibold">Đang tải...</div>
            : <BarChart data={chart7} />}
        </div>

        {/* Donut-style breakdown */}
        <div className="bg-[var(--bg-sidebar)] rounded-2xl border border-[var(--border-color)] p-5">
          <div className="text-sm font-bold text-[var(--text-primary)] mb-4">Nguồn đặt phòng <span className="text-xs text-[var(--text-secondary)] font-normal">(Demo)</span></div>
          <div className="space-y-3 font-semibold">
            {[
              { name:'Trực tiếp', pct:38, color:'#6366f1' },
              { name:'Booking.com', pct:28, color:'#10b981' },
              { name:'Agoda', pct:18, color:'#f59e0b' },
              { name:'Airbnb', pct:10, color:'#06b6d4' },
              { name:'Khác', pct:6, color:'#94a3b8' },
            ].map(s => (
              <div key={s.name} className="flex items-center gap-3">
                <div className="w-24 text-xs text-[var(--text-secondary)] font-medium shrink-0">{s.name}</div>
                <div className="flex-1 bg-[var(--bg-main)] rounded-full h-2.5 overflow-hidden border border-[var(--border-color)]">
                  <div style={{ width:`${s.pct}%`, backgroundColor: s.color }} className="h-full rounded-full transition-all"/>
                </div>
                <div style={{ color: s.color }} className="w-8 text-xs font-bold text-right">{s.pct}%</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Bottom Row: Top phòng + Top dịch vụ */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Top phòng */}
        <div className="bg-[var(--bg-sidebar)] rounded-2xl border border-[var(--border-color)] overflow-hidden">
          <div className="px-5 py-4 border-b border-[var(--border-color)]">
            <div className="text-sm font-bold text-[var(--text-primary)]">Top phòng doanh thu</div>
          </div>
          <div className="p-5 font-semibold">
            {topPhong.length === 0
              ? <div className="text-center py-6 text-[var(--text-secondary)] text-sm">Chưa có dữ liệu</div>
              : topPhong.map((p, i) => (
                <div key={i} className="flex items-center justify-between py-2 border-b border-[var(--border-color)]/50 last:border-0">
                  <div className="text-sm text-[var(--text-primary)]">{i+1}. P.{p.soPhong} — {p.tenLoai}</div>
                  <div className="text-sm font-bold text-emerald-600 dark:text-emerald-400">{fmtM(p.doanhThu)}đ</div>
                </div>
              ))}
          </div>
        </div>

        {/* Top dịch vụ */}
        <div className="bg-[var(--bg-sidebar)] rounded-2xl border border-[var(--border-color)] overflow-hidden">
          <div className="px-5 py-4 border-b border-[var(--border-color)]">
            <div className="text-sm font-bold text-[var(--text-primary)]">Dịch vụ bán chạy</div>
          </div>
          <div className="p-5 font-semibold">
            {topDichVu.length === 0
              ? <div className="text-center py-6 text-[var(--text-secondary)] text-sm">Chưa có dữ liệu</div>
              : topDichVu.map((d, i) => (
                <div key={i} className="flex items-center justify-between py-2 border-b border-[var(--border-color)]/50 last:border-0">
                  <div className="text-sm text-[var(--text-primary)]">{d.tenDV}</div>
                  <div className="text-xs text-[var(--text-secondary)]">{d.soLan} lần · <span className="text-blue-600 dark:text-blue-400 font-bold">{fmtM(d.doanhThu)}đ</span></div>
                </div>
              ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ThongKePage;
