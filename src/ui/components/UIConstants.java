package ui.components;

import entity.enums.*;
import java.awt.*;

/**
 * Hằng số màu sắc, font và kích thước dùng chung toàn bộ UI.
 */
public class UIConstants {

    // ---- Màu chính (Vibrant Blue) ----
    public static final Color PRIMARY       = new Color(0x4361EE);   
    public static final Color PRIMARY_LIGHT = new Color(0xF0F3FF);   
    public static final Color PRIMARY_DARK  = new Color(0x3046C9);   

    // ---- Màu nền (Soft Modern Background) ----
    public static final Color BG_MAIN       = new Color(0xF4F7FE);   
    public static final Color BG_SIDEBAR    = new Color(0xFFFFFF);   
    public static final Color BG_CARD       = new Color(0xFFFFFF);   
    public static final Color BG_TABLE_HEADER = new Color(0xF8FAFC); 

    // ---- Màu chữ ----
    public static final Color TEXT_PRIMARY   = new Color(0x1B2559);  
    public static final Color TEXT_SECONDARY = new Color(0xA3AED0);  
    public static final Color TEXT_MUTED     = new Color(0xCBD5E1);  

    // ---- Màu trạng thái (Modern Palette) ----
    public static final Color SUCCESS        = new Color(0x05CD99);  // Teal-Green
    public static final Color SUCCESS_LIGHT  = new Color(0xE6F9F4);
    public static final Color WARNING        = new Color(0xFFB547);  // Soft Amber
    public static final Color WARNING_LIGHT  = new Color(0xFFF7EC);
    public static final Color DANGER         = new Color(0xEE5D50);  // Soft Red
    public static final Color DANGER_LIGHT   = new Color(0xFFF2F1);
    public static final Color INFO           = new Color(0x7551FF);  // Purple-Indigo
    public static final Color INFO_LIGHT     = new Color(0xF4F2FF);
    public static final Color ORANGE         = new Color(0xF97316);
    public static final Color ORANGE_LIGHT   = new Color(0xFED7AA);

    // ---- Border ----
    public static final Color BORDER         = new Color(0xE2E8F0);
    public static final Color BORDER_FOCUS   = new Color(0x2563EB);

    // ---- Sidebar ----
    public static final Color SIDEBAR_ACTIVE = new Color(0xEFF6FF);
    public static final Color SIDEBAR_HOVER  = new Color(0xF8FAFC);
    public static final int   SIDEBAR_WIDTH  = 220;

    // ---- Dimensions ----
    public static final int HEADER_HEIGHT     = 60;
    public static final int CARD_RADIUS       = 10;
    public static final int BTN_RADIUS        = 8;

    // ---- Fonts ----
    public static final Font FONT_TITLE       = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_HEADER      = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY        = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD   = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL       = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_SMALL_BOLD  = new Font("Segoe UI", Font.BOLD, 11);
    public static final Font FONT_TINY        = new Font("Segoe UI", Font.PLAIN, 10);
    public static final Font FONT_CARD_NUM    = new Font("Segoe UI", Font.BOLD, 28);

    // ---- Trạng thái phòng -> màu ----
    public static String getTrangThaiPhongLabel(TrangThaiPhong tt) {
        if (tt == null) return "Không xác định";
        switch (tt) {
            case AVAILABLE:   return "Có sẵn";
            case OCCUPIED:    return "Đang thuê";
            case MAINTENANCE: return "Bảo trì";
            case CLEANING:    return "Vệ sinh";
            default:          return tt.toString();
        }
    }

    public static Color getTrangThaiPhongColor(TrangThaiPhong tt) {
        if (tt == null) return TEXT_MUTED;
        switch (tt) {
            case AVAILABLE:   return SUCCESS;
            case OCCUPIED:    return PRIMARY;
            case MAINTENANCE: return DANGER;
            case CLEANING:    return new Color(0x06B6D4);
            default:          return TEXT_MUTED;
        }
    }

    public static Color getTrangThaiPhongBg(TrangThaiPhong tt) {
        if (tt == null) return BG_CARD;
        switch (tt) {
            case AVAILABLE:   return SUCCESS_LIGHT;
            case OCCUPIED:    return PRIMARY_LIGHT;
            case MAINTENANCE: return DANGER_LIGHT;
            case CLEANING:    return new Color(0xCFFAFE);
            default:          return BG_CARD;
        }
    }

    // ---- Trạng thái đặt phòng ----
    public static String getTrangThaiDatPhongLabel(TrangThaiDatPhong tt) {
        if (tt == null) return "—";
        switch (tt) {
            case PENDING:     return "Chờ xác nhận";
            case CONFIRMED:   return "Đã xác nhận";
            case PARTIALLY_CHECKED_IN: return "Đang check-in";
            case CHECKED_IN:  return "Đã check-in";
            case CHECKED_OUT: return "Đã trả phòng";
            case CANCELLED:   return "Đã hủy";
            case NO_SHOW:     return "Khách không đến";
            case WAITLIST:    return "Chờ xếp phòng";
            default:          return tt.toString();
        }
    }

    public static Color getTrangThaiDatPhongColor(TrangThaiDatPhong tt) {
        if (tt == null) return TEXT_MUTED;
        switch (tt) {
            case PENDING:     return WARNING;
            case CONFIRMED:   return SUCCESS;
            case PARTIALLY_CHECKED_IN: return INFO;
            case CHECKED_IN:  return PRIMARY;
            case CHECKED_OUT: return TEXT_SECONDARY;
            case CANCELLED:   return DANGER;
            case NO_SHOW:     return new Color(0x991B1B); // Dark red
            case WAITLIST:    return ORANGE;
            default:          return TEXT_MUTED;
        }
    }

    // ---- Vai trò ----
    public static String getVaiTroLabel(VaiTro v) {
        if (v == null) return "—";
        switch (v) {
            case ADMIN:        return "Quản trị viên";
            case MANAGER:      return "Quản lý";
            case RECEPTIONIST: return "Lễ tân";
            default:           return v.toString();
        }
    }

    // ---- Trạng thái thanh toán ----
    public static String getTrangThaiThanhToanLabel(TrangThaiThanhToan tt) {
        if (tt == null) return "Chưa thanh toán";
        switch (tt) {
            case PAID:           return "Đã thanh toán";
            case PARTIALLY_PAID: return "Thanh toán một phần";
            case UNPAID:         return "Chưa thanh toán";
            case REFUNDED:       return "Đã hoàn tiền";
            default:             return tt.toString();
        }
    }

    // Legacy support (to be removed gradually)
    public static Color getTrangThaiPhongColor(String tt) {
        try { return getTrangThaiPhongColor(TrangThaiPhong.valueOf(tt)); } catch(Exception e) {
            if ("Có sẵn".equals(tt))    return SUCCESS;
            if ("Đang thuê".equals(tt)) return PRIMARY;
            if ("Đã đặt".equals(tt))    return WARNING;
            if ("Vệ sinh".equals(tt))   return new Color(0x06B6D4);
            if ("Bảo trì".equals(tt))   return DANGER;
            return TEXT_MUTED;
        }
    }

    public static Color getTrangThaiPhongBg(String tt) {
        try { return getTrangThaiPhongBg(TrangThaiPhong.valueOf(tt)); } catch(Exception e) {
            if ("Có sẵn".equals(tt))    return SUCCESS_LIGHT;
            if ("Đang thuê".equals(tt)) return PRIMARY_LIGHT;
            if ("Đã đặt".equals(tt))    return WARNING_LIGHT;
            if ("Vệ sinh".equals(tt))   return new Color(0xCFFAFE);
            if ("Bảo trì".equals(tt))   return DANGER_LIGHT;
            return BG_CARD;
        }
    }

    public static Color getHangKhachColor(String hang) {
        if (hang == null)          return TEXT_MUTED;
        if ("VIP".equals(hang))    return new Color(0x7C3AED);
        if ("Gold".equals(hang))   return new Color(0xD97706);
        if ("Silver".equals(hang)) return new Color(0x475569);
        return TEXT_MUTED;
    }
}

