package ui.components;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * DateTimePicker – Component chọn ngày giờ có calendar popup.
 * Dùng thay thế JTextField nhập tay ngày trong CheckinDialog.
 *
 * Cách dùng:
 *   DateTimePicker picker = new DateTimePicker(new Date());
 *   panel.add(picker);
 *   Date selectedDate = picker.getDate();        // lấy Date
 *   String text = picker.getFormattedDate();     // lấy String "dd/MM/yyyy HH:mm"
 */
public class DateTimePicker extends JPanel {

    private final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private Calendar selectedCal;
    private final List<ChangeListener> listeners = new ArrayList<>();
    private JTextField txtDisplay;
    private JPopupMenu popup;
    private JLabel lblMonthYear;
    private JPanel calendarGrid;
    private JSpinner spnHour, spnMin;

    // Màu sắc
    private static final Color PRIMARY      = new Color(0x4361EE);
    private static final Color TEXT_MAIN    = new Color(0x1E293B);
    private static final Color TEXT_MUTED   = new Color(0x94A3B8);
    private static final Color BORDER_COLOR = new Color(0xE2E8F0);
    private static final Color HOVER_COLOR  = new Color(0xF1F5F9);
    private static final Color TODAY_COLOR  = new Color(0xFEF3C7);

    public DateTimePicker(Date initialDate) {
        setLayout(new BorderLayout());
        setOpaque(false);

        selectedCal = Calendar.getInstance();
        if (initialDate != null) selectedCal.setTime(initialDate);

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
            return;
        }

        try {
            SDF.setLenient(false);
            Date date = SDF.parse(input);
            setDate(date);
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

        // Chọn giờ phút
        JPanel timeRow = buildTimeRow();
        container.add(timeRow, BorderLayout.SOUTH);

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
                    selectedCal.set(Calendar.DAY_OF_MONTH, d);
                    syncTimeFromSpinner();
                    buildCalendarGrid();
                    calendarGrid.revalidate();
                    calendarGrid.repaint();
                    updateDisplay();
                }
            });

            calendarGrid.add(lbl);
        }

        // Ô trống cuối để đủ 6 hàng
        int total = startDow + daysInMonth;
        int remaining = (total % 7 == 0) ? 0 : 7 - (total % 7);
        for (int i = 0; i < remaining; i++) calendarGrid.add(new JLabel());
    }

    private JPanel buildTimeRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 0, 0, 0)
        ));

        JLabel lblTime = new JLabel("⏰  Giờ:");
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTime.setForeground(TEXT_MUTED);

        spnHour = timeSpinner(0, 23, selectedCal.get(Calendar.HOUR_OF_DAY));
        JLabel sep = new JLabel(":");
        sep.setFont(new Font("Segoe UI", Font.BOLD, 14));
        spnMin  = timeSpinner(0, 59, selectedCal.get(Calendar.MINUTE));

        JButton btnOk = new JButton("OK");
        btnOk.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnOk.setBackground(PRIMARY);
        btnOk.setForeground(Color.WHITE);
        btnOk.setBorderPainted(false);
        btnOk.setFocusPainted(false);
        btnOk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnOk.setPreferredSize(new Dimension(60, 28));
        btnOk.addActionListener(e -> {
            syncTimeFromSpinner();
            updateDisplay();
            popup.setVisible(false);
            fireStateChanged();
        });

        row.add(lblTime);
        row.add(spnHour);
        row.add(sep);
        row.add(spnMin);
        row.add(Box.createHorizontalStrut(10));
        row.add(btnOk);
        return row;
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

    private JSpinner timeSpinner(int min, int max, int val) {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(val, min, max, 1));
        sp.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sp.setPreferredSize(new Dimension(54, 30));
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        try {
            sp.setUI(new javax.swing.plaf.basic.BasicSpinnerUI() {
                protected Component createNextButton() {
                    JButton b = new JButton("▴"); b.setBorder(null); b.setBackground(Color.WHITE); b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    b.addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e){b.setBackground(HOVER_COLOR);} public void mouseExited(MouseEvent e){b.setBackground(Color.WHITE);} });
                    return b;
                }
                protected Component createPreviousButton() {
                    JButton b = new JButton("▾"); b.setBorder(null); b.setBackground(Color.WHITE); b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    b.addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e){b.setBackground(HOVER_COLOR);} public void mouseExited(MouseEvent e){b.setBackground(Color.WHITE);} });
                    return b;
                }
            });
        } catch(Exception ignored) {}
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(sp, "00");
        editor.getTextField().setForeground(TEXT_MAIN);
        editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
        editor.getTextField().setBorder(null);
        sp.setEditor(editor);
        return sp;
    }

    private void updateMonthYearLabel() {
        String[] months = {"Tháng 1","Tháng 2","Tháng 3","Tháng 4","Tháng 5","Tháng 6",
                           "Tháng 7","Tháng 8","Tháng 9","Tháng 10","Tháng 11","Tháng 12"};
        lblMonthYear.setText(months[selectedCal.get(Calendar.MONTH)]
                             + " " + selectedCal.get(Calendar.YEAR));
    }

    private void syncTimeFromSpinner() {
        if (spnHour != null) selectedCal.set(Calendar.HOUR_OF_DAY, (int) spnHour.getValue());
        if (spnMin  != null) selectedCal.set(Calendar.MINUTE,      (int) spnMin.getValue());
        selectedCal.set(Calendar.SECOND, 0);
    }

    private void updateDisplay() {
        txtDisplay.setText(SDF.format(selectedCal.getTime()));
    }

    private void togglePopup() {
        if (popup.isVisible()) {
            popup.setVisible(false);
        } else {
            popup.show(this, 0, getHeight());
        }
    }

    // ---- Public API ----
    /** Lấy ngày đã chọn dưới dạng Date */
    public Date getDate() {
        syncTimeFromSpinner();
        return selectedCal.getTime();
    }

    /** Lấy ngày đã chọn dưới dạng String "dd/MM/yyyy HH:mm" */
    public String getFormattedDate() {
        return SDF.format(getDate());
    }

    /** Lấy ngày đã chọn dưới dạng LocalDateTime */
    public LocalDateTime getLocalDateTime() {
        return getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /** Set ngày từ bên ngoài */
    public void setDate(Date date) {
        if (date != null) {
            selectedCal.setTime(date);
            if (spnHour != null) spnHour.setValue(selectedCal.get(Calendar.HOUR_OF_DAY));
            if (spnMin  != null) spnMin.setValue(selectedCal.get(Calendar.MINUTE));
            updateDisplay();
            syncAndRebuild();
            fireStateChanged();
        }
    }

    public void addChangeListener(ChangeListener l) {
        listeners.add(l);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (txtDisplay != null) {
            txtDisplay.setEnabled(enabled);
            // Change background of wrapper via txtDisplay background
            txtDisplay.getParent().setBackground(enabled ? Color.WHITE : new Color(0xF1F5F9));
        }
    }

    private void fireStateChanged() {
        ChangeEvent ce = new ChangeEvent(this);
        for (ChangeListener l : listeners) l.stateChanged(ce);
    }
}

