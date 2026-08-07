package ui.components;

import java.awt.*;

/**
 * FlowLayout mở rộng: tự động xuống dòng khi hết chiều ngang container.
 * Dùng cho sơ đồ phòng, kết quả tìm kiếm phòng v.v.
 */
public class WrapLayout extends FlowLayout {

    public WrapLayout() { super(); }
    public WrapLayout(int align) { super(align); }
    public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            // Leo ngược toàn bộ parent chain để tìm container có width thực sự > 0.
            // Chuỗi: cardsPanel → section → roomGrid(ScrollablePanel) → JViewport → JScrollPane
            // ScrollablePanel.getScrollableTracksViewportWidth()=true nên nó luôn có width = viewport width.
            int targetWidth = 0;
            Container c = target;
            while (c != null) {
                targetWidth = c.getSize().width;
                if (targetWidth > 0) break;
                c = c.getParent();
            }
            if (targetWidth == 0) targetWidth = 800; // safe fallback khi chưa show lần nào

            int hgap = getHgap(), vgap = getVgap();
            Insets insets = target.getInsets();
            // maxWidth = chiều ngang dùng được, không trừ thêm hgap vì đã tính trong rowWidth
            int maxWidth = targetWidth - insets.left - insets.right;

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0, rowHeight = 0;
            boolean firstInRow = true;

            for (int i = 0; i < target.getComponentCount(); i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    // Kiểm tra wrap: không phải card đầu tiên và thêm vào sẽ vượt maxWidth
                    if (!firstInRow && rowWidth + hgap + d.width > maxWidth) {
                        dim.width   = Math.max(dim.width, rowWidth);
                        dim.height += rowHeight + vgap;
                        rowWidth    = 0;
                        rowHeight   = 0;
                        firstInRow  = true;
                    }
                    rowWidth  += (firstInRow ? 0 : hgap) + d.width;
                    rowHeight  = Math.max(rowHeight, d.height);
                    firstInRow = false;
                }
            }
            dim.width   = Math.max(dim.width, rowWidth);
            dim.height += rowHeight + insets.top + insets.bottom + vgap * 2;
            return dim;
        }
    }
}

