package ui.components;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * DatePicker – Component chọn ngày có calendar popup.
 *
 * Cách dùng:
 *   DatePicker picker = new DatePicker(new Date());
 *   panel.add(picker);
 *   Date selectedDate = picker.getDate();        // lấy Date
 *   String text = picker.getFormattedDate();     // lấy String "dd/MM/yyyy"
 */
public class DatePicker extends JPanel {

    private final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    private Calendar selectedCal;
    private JTextField txtDisplay;
    private JPopupMenu popup;
    private JPanel calendarGrid;
    private JLabel lblMonthYear;
    private java.util.function.Consumer<Date> onDateSelected;
    private boolean isDateNull = true;

    // Màu sắc
    private static final Color PRIMARY      = new Color(0x4361EE);
    private static final Color TEXT_MAIN    = new Color(0x1E293B);
    private static final Color TEXT_MUTED   = new Color(0x94A3B8);
    private static final Color BORDER_COLOR = new Color(0xE2E8F0);
    private static final Color HOVER_COLOR  = new Color(0xF1F5F9);
    private static final Color TODAY_COLOR  = new Color(0xFEF3C7);

    public DatePicker(Date initialDate) {
        this(initialDate, null);
    }

    public DatePicker(Date initialDate, java.util.function.Consumer<Date> onDateSelected) {
        setLayout(new BorderLayout());
        setOpaque(false);
        this.onDateSelected = onDateSelected;

        selectedCal = Calendar.getInstance();
        if (initialDate != null) {
            selectedCal.setTime(initialDate);
            isDateNull = false;
        } else {
            isDateNull = true;
        }

        buildTextField();
        buildPopup();
    }

    // ---- Text field với icon lịch ----
    private void buildTextField() {
        txtDisplay = new JTextField(SDF.format(selectedCal.getTime()));
        txtDisplay.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtDisplay.setEditable(true);
        txtDisplay.setBorder(null);
        txtDisplay.setBackground(Color.WHITE);

        // Wrapper với viền bo góc hiện đại giống ModernTextField
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), UIConstants.BTN_RADIUS, UIConstants.BTN_RADIUS);
                g2.setColor(UIConstants.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UIConstants.BTN_RADIUS, UIConstants.BTN_RADIUS);

                // Icon 📅 đơn giản
                int x = getWidth() - 28, y = getHeight() / 2 - 8;
                g2.setColor(TEXT_MUTED);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x, y, 16, 14, 3, 3);
                g2.drawLine(x + 4,  y - 2, x + 4,  y + 2);
                g2.drawLine(x + 12, y - 2, x + 12, y + 2);
                g2.drawLine(x, y + 4, x + 16, y + 4);
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 32));
        wrapper.add(txtDisplay, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
        setPreferredSize(new Dimension(140, 40));

        // Click wrapper/icon area → hiện popup (không click trực tiếp vào text để cho phép focus nhập liệu)
        wrapper.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { togglePopup(); }
        });

        // Xử lý nhập tay
        txtDisplay.addActionListener(e -> parseManualInput());
        txtDisplay.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) { parseManualInput(); }
        });
    }

    private void parseManualInput() {
        String input = txtDisplay.getText().trim();
        if (input.isEmpty()) {
            setDate(null);
            return;
        }

        try {
            SDF.setLenient(false);
            Date date = SDF.parse(input);
            setDate(date);
            if (onDateSelected != null) {
                onDateSelected.accept(date);
            }
        } catch (java.text.ParseException e) {
            // Revert về giá trị hợp lệ gần nhất
            updateDisplay();
        }
    }

    // ---- Popup calendar ----
    private void buildPopup() {
        popup = new JPopupMenu();
        popup.setBorder(new LineBorder(BORDER_COLOR, 1));
        popup.setBackground(Color.WHITE);

        JPanel container = new JPanel(new BorderLayout(0, 0));
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // Header: nút prev/next + tháng năm
        JPanel header = buildCalendarHeader();
        container.add(header, BorderLayout.NORTH);

        // Lưới ngày
        calendarGrid = new JPanel(new GridLayout(7, 7, 4, 4));
        calendarGrid.setOpaque(false);
        calendarGrid.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));
        buildCalendarGrid();
        container.add(calendarGrid, BorderLayout.CENTER);

        popup.add(container);
    }

    private JPanel buildCalendarHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(4, 4, 12, 4));

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftBtns.setOpaque(false);
        leftBtns.add(arrowBtn("«", e -> { selectedCal.add(Calendar.YEAR, -1); syncAndRebuild(); }));
        leftBtns.add(arrowBtn("‹", e -> { selectedCal.add(Calendar.MONTH, -1); syncAndRebuild(); }));

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightBtns.setOpaque(false);
        rightBtns.add(arrowBtn("›", e -> { selectedCal.add(Calendar.MONTH, 1); syncAndRebuild(); }));
        rightBtns.add(arrowBtn("»", e -> { selectedCal.add(Calendar.YEAR, 1); syncAndRebuild(); }));

        lblMonthYear = new JLabel("", SwingConstants.CENTER);
        lblMonthYear.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblMonthYear.setForeground(TEXT_MAIN);
        updateMonthYearLabel();

        header.add(leftBtns, BorderLayout.WEST);
        header.add(lblMonthYear, BorderLayout.CENTER);
        header.add(rightBtns, BorderLayout.EAST);
        return header;
    }

    private void syncAndRebuild() {
        updateMonthYearLabel();
        buildCalendarGrid();
        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private void buildCalendarGrid() {
        calendarGrid.removeAll();

        // Tên các ngày trong tuần
        String[] days = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        for (String d : days) {
            JLabel lbl = new JLabel(d, SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setForeground(TEXT_MUTED);
            calendarGrid.add(lbl);
        }

        // Xác định ngày đầu tháng
        Calendar cal = (Calendar) selectedCal.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int startDow = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=CN
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int today      = -1;
        int selectedDay = selectedCal.get(Calendar.DAY_OF_MONTH);
        Calendar now   = Calendar.getInstance();
        boolean sameMonth = now.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH)
                         && now.get(Calendar.YEAR)  == selectedCal.get(Calendar.YEAR);
        if (sameMonth) today = now.get(Calendar.DAY_OF_MONTH);

        // Ô trống đầu
        for (int i = 0; i < startDow; i++) calendarGrid.add(new JLabel());

        // Các ngày
        for (int day = 1; day <= daysInMonth; day++) {
            final int d = day;
            boolean isSelected = (d == selectedDay);
            boolean isToday    = (d == today);

            JLabel lbl = new JLabel(String.valueOf(d), SwingConstants.CENTER) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (isSelected) {
                        g2.setColor(PRIMARY);
                        g2.fillOval(2, 2, getWidth()-4, getHeight()-4);
                    } else if (isToday) {
                        g2.setColor(TODAY_COLOR);
                        g2.fillOval(2, 2, getWidth()-4, getHeight()-4);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lbl.setFont(new Font("Segoe UI", isSelected ? Font.BOLD : Font.PLAIN, 12));
            lbl.setForeground(isSelected ? Color.WHITE : TEXT_MAIN);
            lbl.setOpaque(false);
            lbl.setPreferredSize(new Dimension(28, 28));
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Hover
            lbl.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (!isSelected) { lbl.setOpaque(true); lbl.setBackground(HOVER_COLOR); }
                }
                public void mouseExited(MouseEvent e) {
                    lbl.setOpaque(false); lbl.repaint();
                }
                public void mouseClicked(MouseEvent e) {
                    isDateNull = false;
                    selectedCal.set(Calendar.DAY_OF_MONTH, d);
                    buildCalendarGrid();
                    calendarGrid.revalidate();
                    calendarGrid.repaint();
                    updateDisplay();
                    popup.setVisible(false);
                    if (onDateSelected != null) {
                        onDateSelected.accept(getDate());
                    }
                }
            });

            calendarGrid.add(lbl);
        }

        // Ô trống cuối để đủ 6 hàng
        int total = startDow + daysInMonth;
        int remaining = (total % 7 == 0) ? 0 : 7 - (total % 7);
        for (int i = 0; i < remaining; i++) calendarGrid.add(new JLabel());
    }

    private JButton arrowBtn(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        btn.setForeground(new Color(100, 116, 139));
        btn.setBackground(Color.WHITE);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(30, 30));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setContentAreaFilled(true); btn.setBackground(HOVER_COLOR); }
            public void mouseExited(MouseEvent e)  { btn.setContentAreaFilled(false); }
        });
        btn.addActionListener(action);
        return btn;
    }

    private void updateMonthYearLabel() {
        String[] months = {"Tháng 1","Tháng 2","Tháng 3","Tháng 4","Tháng 5","Tháng 6",
                           "Tháng 7","Tháng 8","Tháng 9","Tháng 10","Tháng 11","Tháng 12"};
        lblMonthYear.setText(months[selectedCal.get(Calendar.MONTH)]
                             + " " + selectedCal.get(Calendar.YEAR));
    }

    private void updateDisplay() {
        if (isDateNull) {
            txtDisplay.setText("");
        } else {
            txtDisplay.setText(SDF.format(selectedCal.getTime()));
        }
    }

    private void togglePopup() {
        if (!isEnabled()) return;
        if (popup.isVisible()) {
            popup.setVisible(false);
        } else {
            popup.show(this, 0, getHeight());
        }
    }

    // ---- Public API ----
    /** Lấy ngày đã chọn dưới dạng Date, trả về null nếu chưa chọn/đã xóa */
    public Date getDate() {
        if (isDateNull) return null;
        selectedCal.set(Calendar.HOUR_OF_DAY, 0);
        selectedCal.set(Calendar.MINUTE, 0);
        selectedCal.set(Calendar.SECOND, 0);
        selectedCal.set(Calendar.MILLISECOND, 0);
        return selectedCal.getTime();
    }

    /** Lấy ngày đã chọn dưới dạng String "dd/MM/yyyy", trả về "" nếu null */
    public String getFormattedDate() {
        Date d = getDate();
        return (d == null) ? "" : SDF.format(d);
    }

    /** Set ngày từ bên ngoài, truyền null để xóa trắng */
    public void setDate(Date date) {
        if (date != null) {
            isDateNull = false;
            selectedCal.setTime(date);
        } else {
            isDateNull = true;
        }
        updateDisplay();
        syncAndRebuild();
    }

    public void setOnDateSelected(java.util.function.Consumer<Date> onDateSelected) {
        this.onDateSelected = onDateSelected;
    }
}

