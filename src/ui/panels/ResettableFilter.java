package ui.panels;

/**
 * Interface cho các Panel có bộ lọc.
 * Cho phép MainFrame tự động xóa bộ lọc khi chuyển trang.
 */
public interface ResettableFilter {
    /**
     * Xóa tất cả các bộ lọc về trạng thái mặc định.
     */
    void resetFilters();
}
