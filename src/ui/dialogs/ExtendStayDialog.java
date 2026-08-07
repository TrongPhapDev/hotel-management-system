package ui.dialogs;

import entity.ChiTietDatPhong;
import entity.DatPhong;
import entity.Phong;
import service.ThuePhongService;
import service.DatPhongService;
import ui.components.UIConstants;
import ui.components.RoundedComponents;
import static ui.components.RoundedComponents.*;
import ui.components.DateTimePicker;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ExtendStayDialog extends JDialog {

    private final ChiTietDatPhong chiTietDatPhong;
    private final DatPhong datPhong;
    private final ThuePhongService thuePhongService = new ThuePhongService();
    private final DatPhongService datPhongService = new DatPhongService();

    private DateTimePicker dtpNewCheckout;
    private JLabel lblCurrentStay, lblExtractedInfo, lblExtraCost, lblTotalEstimate;
    private JLabel lblAvailability;
    
    private boolean confirmed = false;
    private LocalDateTime newCheckoutTime;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ExtendStayDialog(Frame parent, ChiTietDatPhong ctdp) {
        super(parent, "Gia hạn lưu trú - EXTEND STAY", true);
        this.chiTietDatPhong = ctdp;
        this.datPhong = ctdp.getDatPhong();

        setSize(600, 520);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        buildUI();
        calculateImpact();
    }

    private void buildUI() {
        // --- Header ---
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 80));
        header.setBorder(new EmptyBorder(15, 30, 10, 30));

        JLabel lblTitle = new JLabel("Gia hạn lưu trú (Stay Extension)");
        lblTitle.setFont(UIConstants.FONT_HEADER);
        lblTitle.setForeground(UIConstants.PRIMARY);
        header.add(lblTitle);
        
        Phong p = chiTietDatPhong.getPhong();
        JLabel lblRoom = new JLabel("Phòng " + (p != null ? p.getSoPhong() : "N/A") + " • Khách: " + 
                                   (datPhong.getKhachHang() != null ? datPhong.getKhachHang().getHoTen() : "Khách lẻ"));
        lblRoom.setFont(UIConstants.FONT_SMALL);
        lblRoom.setForeground(UIConstants.TEXT_MUTED);
        header.add(lblRoom);
        
        add(header, BorderLayout.NORTH);

        // --- Content ---
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(20, 30, 20, 30));

        // 1. Current Stay Info
        JPanel currentPanel = new JPanel(new BorderLayout());
        currentPanel.setOpaque(false);
        currentPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        lblCurrentStay = new JLabel("Ngày trả hiện tại: " + datPhong.getNgayTraDuKien().format(dtf));
        lblCurrentStay.setFont(UIConstants.FONT_BODY_BOLD);
        lblCurrentStay.setForeground(new Color(0xD97706)); // Amber color for emphasis
        currentPanel.add(lblCurrentStay, BorderLayout.WEST);
        main.add(currentPanel);
        main.add(Box.createVerticalStrut(15));

        // 2. New Checkout Picker
        JLabel lblLabelPicker = new JLabel("Chọn ngày trả mới:");
        lblLabelPicker.setFont(UIConstants.FONT_SMALL_BOLD);
        lblLabelPicker.setForeground(UIConstants.TEXT_SECONDARY);
        main.add(lblLabelPicker);
        main.add(Box.createVerticalStrut(8));
        
        // Mặc định gia hạn thêm 1 ngày và đặt giờ là 12:00 (chuẩn khách sạn)
        LocalDateTime defaultExt = datPhong.getNgayTraDuKien().plusDays(1)
                                            .withHour(12).withMinute(0).withSecond(0);
        dtpNewCheckout = new DateTimePicker(java.sql.Timestamp.valueOf(defaultExt));
        dtpNewCheckout.addChangeListener(e -> calculateImpact());
        main.add(dtpNewCheckout);
        main.add(Box.createVerticalStrut(12));
        
        // 3. Availability Check
        lblAvailability = new JLabel("Phòng sẵn sàng cho khoảng thời gian này");
        lblAvailability.setFont(UIConstants.FONT_SMALL_BOLD);
        lblAvailability.setForeground(UIConstants.SUCCESS);
        main.add(lblAvailability);
        main.add(Box.createVerticalStrut(20));

        // 4. Summary Box - Using RoundedPanel for Premium look
        RoundedPanel summaryBox = new RoundedPanel(UIConstants.CARD_RADIUS);
        summaryBox.setLayout(new BoxLayout(summaryBox, BoxLayout.Y_AXIS));
        summaryBox.setBackground(new Color(0xF8FAFC));
        summaryBox.setBorder(new EmptyBorder(20, 20, 20, 20));
        summaryBox.setShadow(true);
        
        lblExtractedInfo = new JLabel("Gia hạn thêm: 0 đêm");
        lblExtraCost = new JLabel("Phí phát sinh dự kiến: 0đ");
        lblTotalEstimate = new JLabel("Tổng tiền phòng ước tính: 0đ");
        
        lblExtractedInfo.setFont(UIConstants.FONT_BODY);
        lblExtraCost.setFont(UIConstants.FONT_BODY);
        lblTotalEstimate.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTotalEstimate.setForeground(UIConstants.PRIMARY);

        summaryBox.add(lblExtractedInfo);
        summaryBox.add(Box.createVerticalStrut(10));
        summaryBox.add(lblExtraCost);
        summaryBox.add(Box.createVerticalStrut(15));
        
        // Custom separator
        JPanel separator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0xE2E8F0));
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setPreferredSize(new Dimension(0, 1));
        summaryBox.add(separator);
        
        summaryBox.add(Box.createVerticalStrut(12));
        summaryBox.add(lblTotalEstimate);
        
        main.add(summaryBox);
        add(main, BorderLayout.CENTER);

        // --- Footer Buttons ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        footer.setBackground(new Color(0xF8FAFC));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xF1F5F9)));
        
        RoundedButton btnCancel = grayButton("Hủy bỏ");
        RoundedButton btnConfirm = primaryButton("Xác nhận gia hạn");
        
        btnCancel.addActionListener(e -> dispose());
        btnConfirm.addActionListener(e -> handleConfirm());
        
        footer.add(btnCancel);
        footer.add(btnConfirm);
        add(footer, BorderLayout.SOUTH);
    }

    private void calculateImpact() {
        java.util.Date selectedDate = dtpNewCheckout.getDate();
        LocalDateTime newDate = selectedDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime oldDate = datPhong.getNgayTraDuKien();
        LocalDateTime arrival = datPhong.getNgayNhanDuKien();

        if (newDate.isBefore(oldDate)) {
            lblAvailability.setText("Ngày trả mới phải sau ngày trả hiện tại!");
            lblAvailability.setForeground(Color.RED);
            return;
        }

        // 1. Check Availability (Real check)
        boolean isAvailable = new dao.PhongDAO().isRoomAvailable(
                chiTietDatPhong.getPhong().getMaPhong(), 
                oldDate, 
                newDate, 
                datPhong.getMaDatPhong());
                
        if (!isAvailable) {
            lblAvailability.setText("XUNG ĐỘT: Phòng đã có lịch đặt trong khoảng thời gian này!");
            lblAvailability.setForeground(java.awt.Color.RED);
            this.newCheckoutTime = null; // Chặn xác nhận
            return;
        } else {
            lblAvailability.setText("Phòng trống sẵn sàng cho gia hạn");
            lblAvailability.setForeground(UIConstants.SUCCESS);
        }

        // 2. Calculate nights
        long oldNights = ChronoUnit.DAYS.between(arrival.toLocalDate(), oldDate.toLocalDate());
        long newNights = ChronoUnit.DAYS.between(arrival.toLocalDate(), newDate.toLocalDate());
        if (newNights == oldNights && newDate.isAfter(oldDate)) newNights++; // Handle same-day extension logic if needed

        long extraNights = newNights - oldNights;
        double rate = chiTietDatPhong.getGiaThucTeChot();
        double extraCost = extraNights * rate;
        double totalCost = newNights * rate;

        lblExtractedInfo.setText("Gia hạn thêm: " + extraNights + " đêm (Tổng " + newNights + " đêm)");
        lblExtraCost.setText("Phí phát sinh dự kiến: " + String.format("%,.0fđ", extraCost));
        lblTotalEstimate.setText("Tổng tiền phòng ước tính: " + String.format("%,.0fđ", totalCost));
        
        this.newCheckoutTime = newDate;
    }

    private void handleConfirm() {
        if (newCheckoutTime == null || newCheckoutTime.isBefore(datPhong.getNgayTraDuKien())) {
            JOptionPane.showMessageDialog(this, "Ngày gia hạn không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() { return confirmed; }
    public LocalDateTime getNewCheckoutTime() { return newCheckoutTime; }
}
