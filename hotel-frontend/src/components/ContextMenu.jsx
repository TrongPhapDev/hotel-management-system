/**
 * Shared ContextMenu component for all pages
 * Pattern: Right-click → context menu | Double-click → view/edit detail
 */
import React, { useEffect, useRef } from 'react';

export const RowContextMenu = ({ menu, items, onAction, onClose, title, subtitle }) => {
  const ref = useRef(null);

  useEffect(() => {
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) onClose();
    };
    const keyHandler = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('mousedown', handler);
    document.addEventListener('keydown', keyHandler);
    return () => {
      document.removeEventListener('mousedown', handler);
      document.removeEventListener('keydown', keyHandler);
    };
  }, [onClose]);

  const x = menu.x + 220 > window.innerWidth ? menu.x - 220 : menu.x;
  const y = menu.y + (items.length * 38 + 64) > window.innerHeight
    ? menu.y - (items.length * 38 + 64)
    : menu.y;

  return (
    <div
      ref={ref}
      style={{ position: 'fixed', top: y, left: x, zIndex: 200 }}
      className="dropdown-menu min-w-[210px] animate-fade-in"
      onContextMenu={e => e.preventDefault()}
    >
      {(title || subtitle) && (
        <div
          className="px-4 py-3"
          style={{ borderBottom: '1px solid var(--border)' }}
        >
          {title && (
            <div className="text-[12px] font-bold truncate" style={{ color: 'var(--text-primary)' }}>
              {title}
            </div>
          )}
          {subtitle && (
            <div className="font-mono-data text-[10px] mt-0.5 truncate" style={{ color: 'var(--text-muted)' }}>
              {subtitle}
            </div>
          )}
        </div>
      )}
      <div className="py-1.5">
        {items.map((item, idx) =>
          item.separator ? (
            <div key={idx} className="my-1" style={{ borderTop: '1px solid var(--border)', margin: '4px 0' }} />
          ) : (
            <button
              key={idx}
              disabled={item.disabled}
              onClick={() => { if (!item.disabled) { onAction(item.action, menu.item); onClose(); } }}
              className={`dropdown-item text-[13px] ${item.danger ? 'danger' : ''} ${item.disabled ? 'opacity-40 cursor-not-allowed pointer-events-none' : ''}`}
              style={!item.danger && !item.disabled ? { color: 'var(--text-primary)' } : undefined}
            >
              {item.label}
            </button>
          )
        )}
      </div>
    </div>
  );
};

export const useContextMenu = () => {
  const [ctxMenu, setCtxMenu] = React.useState(null);

  const openCtxMenu = (e, item) => {
    e.preventDefault();
    e.stopPropagation();
    setCtxMenu({ x: e.clientX, y: e.clientY, item });
  };

  const closeCtxMenu = () => setCtxMenu(null);

  return { ctxMenu, openCtxMenu, closeCtxMenu };
};

export default RowContextMenu;
