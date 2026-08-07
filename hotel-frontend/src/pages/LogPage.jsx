import React, { useEffect, useState, useCallback } from 'react';
import { logAPI } from '../api/api';
import { ClipboardList, Search, Clock, User, X } from 'lucide-react';
import { RowContextMenu, useContextMenu } from '../components/ContextMenu';

const ACTION_COLORS = {
  'Check-in': { bg: 'var(--color-occupied-bg)', text: 'var(--color-occupied-text)', border: 'var(--color-occupied-border)' },
  'Check-out': { bg: 'var(--color-available-bg)', text: 'var(--color-available-text)', border: 'var(--color-available-border)' },
  'Đăng nhập': { bg: 'var(--color-occupied-bg)', text: 'var(--color-occupied-text)', border: 'var(--color-occupied-border)' },
  'Đăng xuất': { bg: 'var(--bg-main)', text: 'var(--text-secondary)', border: 'var(--border-color)' },
  'Thêm mới': { bg: 'var(--color-available-bg)', text: 'var(--color-available-text)', border: 'var(--color-available-border)' },
  'Cập nhật': { bg: 'var(--color-cleaning-bg)', text: 'var(--color-cleaning-text)', border: 'var(--color-cleaning-border)' },
  'Xóa': { bg: 'var(--color-maintenance-bg)', text: 'var(--color-maintenance-text)', border: 'var(--color-maintenance-border)' },
  'Hủy': { bg: 'var(--color-maintenance-bg)', text: 'var(--color-maintenance-text)', border: 'var(--color-maintenance-border)' },
};

const ActionBadge = ({ action }) => {
  const cfg = Object.entries(ACTION_COLORS).find(([key]) => action?.includes(key))?.[1] ||
    { bg: 'var(--bg-main)', text: 'var(--text-secondary)', border: 'var(--border-color)' };
  return (
    <span style={{ backgroundColor: cfg.bg, color: cfg.text, borderColor: cfg.border }}
      className="px-2 py-0.5 text-[10px] font-extrabold rounded-full border uppercase whitespace-nowrap">{action}</span>
  );
};

const LogDetailModal = ({ log, onClose }) => {
  if (!log) return null;
  return (
    <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-panel" style={{ maxWidth: 448 }}>
        <div className="px-5 py-4 border-b border-[var(--border-color)] flex items-center justify-between">
          <h3 className="font-bold text-[var(--text-primary)] flex items-center gap-2">
            <ClipboardList size={16} className="text-indigo-500"/>Chi tiết nhật ký
          </h3>
          <button onClick={onClose}><X size={18} className="text-[var(--text-secondary)] hover:text-[var(--text-primary)]"/></button>
        </div>
        <div className="p-5 space-y-3">
          <div className="grid grid-cols-2 gap-2 text-xs">
            {[
              ['Mã log', log.maLog],
              ['Thời gian', new Date(log.thoiGian).toLocaleString('vi-VN')],
              ['Tài khoản', log.tenDangNhap || '—'],
              ['Hành động', log.hanhDong || '—'],
              ['Đối tượng', log.doiTuong || '—'],
            ].map(([k, v]) => (
              <div key={k} className="bg-[var(--bg-main)] rounded-xl p-2.5">
                <div className="text-[10px] font-bold text-[var(--text-secondary)] uppercase">{k}</div>
                <div className="font-semibold text-[var(--text-primary)] mt-0.5">{v}</div>
              </div>
            ))}
          </div>
          {log.chiTiet && (
            <div className="bg-[var(--bg-main)] rounded-xl p-3">
              <div className="text-[10px] font-bold text-[var(--text-secondary)] uppercase mb-1">Chi tiết</div>
              <div className="text-xs text-[var(--text-primary)] leading-relaxed">{log.chiTiet}</div>
            </div>
          )}
        </div>
        <div className="px-5 pb-4 flex justify-end">
          <button onClick={onClose} className="px-4 py-2 bg-[var(--bg-main)] hover:bg-[var(--border-color)] text-[var(--text-secondary)] text-xs font-semibold rounded-xl transition-colors">Đóng</button>
        </div>
      </div>
    </div>
  );
};

const LogPage = () => {
  const [logs, setLogs] = useState([]);
  const [kw, setKw] = useState('');
  const [actionFilter, setActionFilter] = useState('ALL');
  const [loading, setLoading] = useState(false);
  const [detailLog, setDetailLog] = useState(null);
  const { ctxMenu, openCtxMenu, closeCtxMenu } = useContextMenu();

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    try {
      const res = await logAPI.getAll();
      setLogs(res.data);
    } catch (e) { console.error(e); }
    setLoading(false);
  }, []);

  useEffect(() => { fetchLogs(); }, [fetchLogs]);

  const actions = ['ALL', ...new Set(logs.map(l => l.hanhDong).filter(Boolean))];

  const filtered = logs.filter(l => {
    const matchKw = !kw || (l.chiTiet || '').toLowerCase().includes(kw.toLowerCase()) ||
      (l.doiTuong || '').toLowerCase().includes(kw.toLowerCase()) ||
      (l.tenDangNhap || '').toLowerCase().includes(kw.toLowerCase());
    const matchAction = actionFilter === 'ALL' || l.hanhDong === actionFilter;
    return matchKw && matchAction;
  });

  // KPI stats
  const today = new Date().toDateString();
  const kpiData = [
    { label: 'Tổng nhật ký', value: logs.length, color: '#6366F1', bg: '#EEF2FF' },
    { label: 'Hôm nay', value: logs.filter(l => new Date(l.thoiGian).toDateString() === today).length, color: '#10B981', bg: '#ECFDF5' },
    { label: 'Cảnh báo', value: logs.filter(l => l.hanhDong?.toLowerCase().includes('xóa') || l.hanhDong?.toLowerCase().includes('hủy')).length, color: '#EF4444', bg: '#FEF2F2' },
  ];

  const ctxItems = [
    { label: 'Xem chi tiết', action: 'view', color: 'text-[var(--text-secondary)]' },
  ];

  return (
    <div className="page-shell" onClick={closeCtxMenu}>
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="page-label">
            Hệ thống
          </div>
          <h1 className="page-title flex items-center gap-2">
            <ClipboardList style={{ width: 20, height: 20, color: 'var(--text-secondary)' }} strokeWidth={2} />
            Nhật ký Hệ thống
          </h1>
          <p className="page-subtitle">Double-click hoặc chuột phải để xem chi tiết</p>
        </div>
      </div>

      {/* KPI Stats */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-[10px]">
        {kpiData.map(kpi => (
          <div key={kpi.label} className="bg-[var(--bg-card)] rounded-[10px] p-[14px_16px] border-[0.5px] border-[var(--border-tertiary)] flex flex-col font-semibold shadow-sm">
            <span className="text-[10px] font-medium uppercase tracking-[0.6px]" style={{ color: 'var(--text-tertiary)' }}>{kpi.label}</span>
            <span className="text-2xl font-medium mt-1" style={{ color: 'var(--text-primary)' }}>{kpi.value}</span>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="filter-bar">
        {/* Search */}
        <div className="filter-search">
          <Search size={14} className="filter-search-icon" />
          <input type="text" placeholder="Tìm tài khoản, đối tượng, chi tiết..." value={kw} onChange={e => setKw(e.target.value)} />
          {kw && (
            <button onClick={() => setKw('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors">
              <X size={12}/>
            </button>
          )}
        </div>

        {/* Action filter */}
        <div className={`filter-select-wrap ${actionFilter !== 'ALL' ? 'active' : ''}`}>
          <select value={actionFilter} onChange={e => setActionFilter(e.target.value)}>
            {actions.map(act => <option key={act} value={act}>{act === 'ALL' ? 'Tất cả hành động' : act}</option>)}
          </select>
        </div>

        {/* Result count */}
        <span className="filter-result-count">
          {filtered.length} / {logs.length} bản ghi
        </span>
      </div>

      {/* Table */}
      <div className="section-box">
        <table className="data-table">
          <thead>
            <tr>
              {['Thời gian', 'Tài khoản', 'Hành động', 'Đối tượng', 'Chi tiết'].map(h => (
                <th key={h}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={5} className="py-16 text-center">
                  <div className="spinner mx-auto mb-2" />
                  <div style={{ color: 'var(--text-muted)', fontSize: 12 }}>Đang tải...</div>
                </td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan={5} className="py-14 text-center" style={{ color: 'var(--text-muted)', fontSize: 13 }}>
                  Không có nhật ký nào
                </td>
              </tr>
            ) : filtered.map(log => (
              <tr key={log.maLog}
                className="cursor-pointer group"
                onDoubleClick={() => setDetailLog(log)}
                onContextMenu={(e) => { e.stopPropagation(); openCtxMenu(e, log); }}>
                <td className="font-mono-data text-[12px]" style={{ color: 'var(--text-secondary)' }}>
                  <div className="flex items-center gap-1.5">
                    <Clock size={11}/>
                    {new Date(log.thoiGian).toLocaleString('vi-VN')}
                  </div>
                </td>
                <td style={{ color: 'var(--text-secondary)' }}>
                  <div className="flex items-center gap-1.5">
                    <User size={12} className="text-[var(--text-secondary)]"/>
                    {log.tenDangNhap || '—'}
                  </div>
                </td>
                <td><ActionBadge action={log.hanhDong}/></td>
                <td style={{ color: 'var(--text-primary)' }}>{log.doiTuong || '—'}</td>
                <td className="text-xs max-w-xs truncate" style={{ color: 'var(--text-secondary)' }}>{log.chiTiet || '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && filtered.length > 0 && (
          <div className="table-footer">
            <span>Tổng: <strong className="text-[var(--text-primary)]">{filtered.length} bản ghi</strong></span>
            <span>Double-click để xem chi tiết</span>
          </div>
        )}
      </div>

      {/* Context Menu */}
      {ctxMenu && (
        <RowContextMenu menu={ctxMenu} items={ctxItems}
          onAction={(action, log) => { if (action === 'view') setDetailLog(log); }}
          onClose={closeCtxMenu}
          title={ctxMenu.item?.hanhDong}
          subtitle={new Date(ctxMenu.item?.thoiGian).toLocaleString('vi-VN')}/>
      )}

      {/* Detail Modal */}
      <LogDetailModal log={detailLog} onClose={() => setDetailLog(null)}/>
    </div>
  );
};

export default LogPage;
