package ui.dialogs;

import entity.ChiTietDatPhong;
import entity.LoaiPhong;
import entity.Phong;
import entity.enums.TrangThaiPhong;
import service.PhongService;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class RoomTransferDialog extends JDialog {

    private final ChiTietDatPhong chiTietDatPhong;
    private final Phong oldRoom;
    private final PhongService phongService = new PhongService();
    private final service.BangGiaService bangGiaService = new service.BangGiaService();

    private JComboBox<LoaiPhong> cboRoomType;
    private JList<Phong> listRooms;
    private DefaultListModel<Phong> listModel;
    
    private JLabel lblOldPrice, lblNewPrice, lblDiff;
    private JCheckBox chkKeepPrice;
    
    private boolean confirmed = false;
    private Phong selectedRoom;
    private boolean useNewPrice = true;

    public RoomTransferDialog(Frame parent, ChiTietDatPhong ctdp) {
        super(parent, "Đổi phòng - Move Room", true);
        this.chiTietDatPhong = ctdp;
        this.oldRoom = ctdp.getPhong();

        setSize(850, 600);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- Header ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x0F172A));
        header.setPreferredSize(new Dimension(0, 80));
        header.setBorder(new EmptyBorder(0, 25, 0, 25));

        JLabel lblTitle = new JLabel("QUY TRÌNH ĐỔI PHÒNG (ROOM MOVE)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle, BorderLayout.CENTER);
        
        JLabel lblRoom = new JLabel("Đang ở: Phòng " + oldRoom.getSoPhong() + " (" + oldRoom.getTenLoaiPhong() + ")");
        lblRoom.setFont(UIConstants.FONT_BODY);
        lblRoom.setForeground(new Color(0x94A3B8));
        header.add(lblRoom, BorderLayout.SOUTH);
        
        add(header, BorderLayout.NORTH);

        // --- Main Content ---
        JPanel main = new JPanel(new GridLayout(1, 2, 20, 0));
        main.setBackground(Color.WHITE);
        main.setBorder(new EmptyBorder(25, 25, 25, 25));

        // LEFT: Selection
        JPanel left = new JPanel(new BorderLayout(0, 15));
        left.setOpaque(false);
        
        JPanel filters = new JPanel(new BorderLayout(0, 5));
        filters.setOpaque(false);
        filters.add(new JLabel("Chọn loại phòng mới:"), BorderLayout.NORTH);
        cboRoomType = new JComboBox<>();
        cboRoomType.setFont(UIConstants.FONT_BODY);
        cboRoomType.addActionListener(e -> updateRoomList());
        filters.add(cboRoomType, BorderLayout.CENTER);
        left.add(filters, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        listRooms = new JList<>(listModel);
        listRooms.setCellRenderer(new RoomListRenderer());
        listRooms.setFixedCellHeight(60);
        listRooms.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listRooms.addListSelectionListener(e -> updateFinancials());
        JScrollPane scrollRooms = new JScrollPane(listRooms);
        scrollRooms.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        left.add(scrollRooms, BorderLayout.CENTER);
        
        main.add(left);

        // RIGHT: Financials & Action
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(new Color(0xF8FAFC));
        right.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel detailHeader = new JPanel(new BorderLayout());
        detailHeader.setOpaque(false);
        JLabel lblDet = new JLabel("Chi tiết thay đổi");
        lblDet.setFont(UIConstants.FONT_HEADER);
        detailHeader.add(lblDet, BorderLayout.NORTH);
        detailHeader.add(new JSeparator(), BorderLayout.CENTER);
        right.add(detailHeader, BorderLayout.NORTH);

        JPanel info = new JPanel(new GridLayout(6, 1, 0, 10));
        info.setOpaque(false);
        info.setBorder(new EmptyBorder(20, 0, 20, 0));

        lblOldPrice = new JLabel("Giá phòng hiện tại: " + String.format("%,.0fđ", chiTietDatPhong.getGiaThucTeChot()));
        lblNewPrice = new JLabel("Giá phòng mới: 0đ");
        lblDiff = new JLabel("Chênh lệch: 0đ");
        
        lblOldPrice.setFont(UIConstants.FONT_BODY);
        lblNewPrice.setFont(UIConstants.FONT_BODY);
        lblDiff.setFont(UIConstants.FONT_BODY_BOLD);

        chkKeepPrice = new JCheckBox("Giữ nguyên giá cũ (Upgrade miễn phí)");
        chkKeepPrice.setFont(UIConstants.FONT_BODY);
        chkKeepPrice.setOpaque(false);
        chkKeepPrice.addActionListener(e -> updateFinancials());

        info.add(lblOldPrice);
        info.add(lblNewPrice);
        info.add(new JSeparator());
        info.add(lblDiff);
        info.add(chkKeepPrice);
        
        right.add(info, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttons.setOpaque(false);
        RoundedButton btnCancel = new RoundedButton("Hủy bỏ", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnConfirm = new RoundedButton("Xác nhận đổi phòng", UIConstants.PRIMARY, Color.WHITE);
        
        btnCancel.addActionListener(e -> dispose());
        btnConfirm.addActionListener(e -> handleConfirm());
        
        buttons.add(btnCancel);
        buttons.add(btnConfirm);
        right.add(buttons, BorderLayout.SOUTH);

        main.add(right);
        add(main, BorderLayout.CENTER);
    }

    private void loadData() {
        List<LoaiPhong> lps = phongService.getAllLoaiPhong();
        for (LoaiPhong lp : lps) {
            cboRoomType.addItem(lp);
            if (lp.getMaLoaiPhong().equals(oldRoom.getLoaiPhong().getMaLoaiPhong())) {
                cboRoomType.setSelectedItem(lp);
            }
        }
        updateRoomList();
    }

    private void updateRoomList() {
        listModel.clear();
        LoaiPhong selected = (LoaiPhong) cboRoomType.getSelectedItem();
        if (selected == null) return;

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime departure = (chiTietDatPhong.getDatPhong() != null && chiTietDatPhong.getDatPhong().getNgayTraDuKien() != null)
                ? chiTietDatPhong.getDatPhong().getNgayTraDuKien()
                : now.plusHours(24);

        List<Phong> rooms = phongService.getAllPhong().stream()
                .filter(p -> p.getTrangThai() == TrangThaiPhong.AVAILABLE 
                    && p.getLoaiPhong().getMaLoaiPhong().equals(selected.getMaLoaiPhong()))
                .filter(p -> {
                    // KIỂM TRA XUNG ĐỘT RESERVATION TRONG TOÀN BỘ THỜI GIAN CÒN LẠI (Chuẩn Opera PMS)
                    return phongService.isRoomAvailable(p.getMaPhong(), now, departure);
                })
                .collect(Collectors.toList());

        for (Phong p : rooms) listModel.addElement(p);
    }

    private void updateFinancials() {
        selectedRoom = listRooms.getSelectedValue();
        if (selectedRoom == null) {
            lblNewPrice.setText("Giá phòng mới: 0đ");
            lblDiff.setText("Chênh lệch: 0đ");
            return;
        }

        double oldPrice = chiTietDatPhong.getGiaThucTeChot();
        
        // PRIORITY: Active Price Table > Base Price
        double newPrice = bangGiaService.layGiaHienHanh(selectedRoom.getLoaiPhong() != null ? selectedRoom.getLoaiPhong().getMaLoaiPhong() : "");
        
        if (chkKeepPrice.isSelected()) {
            lblNewPrice.setText("Giá áp dụng: " + String.format("%,.0fđ", oldPrice) + " (Gốc: " + String.format("%,.0fđ", newPrice) + ")");
            lblDiff.setText("Chênh lệch: 0đ (Free)");
            lblDiff.setForeground(UIConstants.SUCCESS);
            useNewPrice = false;
        } else {
            lblNewPrice.setText("Giá phòng mới: " + String.format("%,.0fđ", newPrice));
            double diff = newPrice - oldPrice;
            lblDiff.setText("Chênh lệch: " + (diff > 0 ? "+" : "") + String.format("%,.0fđ", diff));
            lblDiff.setForeground(diff > 0 ? new Color(0xDC2626) : UIConstants.SUCCESS);
            useNewPrice = true;
        }
    }

    private void handleConfirm() {
        if (selectedRoom == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn phòng để đổi!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime departure = (chiTietDatPhong.getDatPhong() != null && chiTietDatPhong.getDatPhong().getNgayTraDuKien() != null)
                ? chiTietDatPhong.getDatPhong().getNgayTraDuKien()
                : now.plusHours(24);

        // --- HỆ THỐNG CẢNH BÁO (WARNING SYSTEM - KHÔNG CHẶN CỨNG) ---
        StringBuilder warningMsg = new StringBuilder();
        
        // 1. Cảnh báo giờ muộn
        if (now.getHour() >= 18) {
            warningMsg.append("- Giờ đã muộn (sau 18:00), xác nhận vẫn muốn đổi phòng?\n");
        }

        // 2. Cảnh báo ngày checkout
        if (departure.toLocalDate().isEqual(now.toLocalDate())) {
            warningMsg.append("- Khách dự kiến trả phòng ngay trong hôm nay, xác nhận đổi?\n");
        }

        // 3. Cảnh báo có reservation sát giờ (trong 24h tới)
        // Lưu ý: isRoomAvailable đã lọc conflict cứng, đây là cảnh báo "sắp có người vào"
        if (!phongService.isRoomAvailable(selectedRoom.getMaPhong(), now, now.plusHours(24))) {
             // (Logic này thực tế sẽ ít gặp vì isRoomAvailable ở updateRoomList đã lọc nếu departure > 24h)
             // Nhưng nếu departure ngắn, ta vẫn cảnh báo nếu có ai đó đặt sát giờ sau khi khách checkout
        }

        if (warningMsg.length() > 0) {
            int ok = JOptionPane.showConfirmDialog(this, 
                "Cảnh báo nghiệp vụ:\n" + warningMsg.toString() + "\nBạn vẫn muốn tiếp tục?",
                "Xác nhận đổi phòng", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
        }

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() { return confirmed; }
    public Phong getSelectedRoom() { return selectedRoom; }
    public boolean isUseNewPrice() { return useNewPrice; }

    // --- Renderer ---
    static class RoomListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel p = new JPanel(new BorderLayout(15, 0));
            p.setBorder(new EmptyBorder(10, 15, 10, 15));
            p.setBackground(isSelected ? new Color(0xEFF6FF) : Color.WHITE);
            
            if (value instanceof Phong room) {
                JLabel lblTitle = new JLabel("Phòng " + room.getSoPhong());
                lblTitle.setFont(UIConstants.FONT_BODY_BOLD);
                lblTitle.setForeground(isSelected ? UIConstants.PRIMARY : UIConstants.TEXT_PRIMARY);
                
                JLabel lblPrice = new JLabel(String.format("%,.0fđ/đêm", room.getGiaTheoNgay()));
                lblPrice.setFont(UIConstants.FONT_SMALL_BOLD);
                lblPrice.setForeground(UIConstants.SUCCESS);
                
                JLabel lblView = new JLabel("Tầng " + room.getTang() + " • " + room.getView());
                lblView.setFont(UIConstants.FONT_SMALL);
                lblView.setForeground(UIConstants.TEXT_SECONDARY);
                
                p.add(lblTitle, BorderLayout.WEST);
                p.add(lblPrice, BorderLayout.EAST);
                p.add(lblView, BorderLayout.SOUTH);
            }
            return p;
        }
    }
}
