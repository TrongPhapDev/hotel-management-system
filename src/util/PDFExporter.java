package util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.itextpdf.text.pdf.qrcode.EncodeHintType;
import entity.HoaDon;
import entity.ChiTietHoaDon;

import javax.swing.*;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Xuất hóa đơn PDF chuẩn theo mẫu hóa đơn GTGT Việt Nam (layout MISA).
 * Áp dụng cho hệ thống quản lý khách sạn OHNO.
 */
public class PDFExporter {

    // ── Màu sắc ─────────────────────────────────────────────────────────────
    private static final BaseColor CLR_BORDER     = new BaseColor(150, 150, 150);
    private static final BaseColor CLR_TBL_HEADER = new BaseColor(220, 220, 220);
    private static final BaseColor CLR_SIG_VALID  = new BaseColor(198, 239, 206);
    private static final BaseColor CLR_SIG_BORDER = new BaseColor( 70, 130,  80);
    private static final BaseColor CLR_LINK       = new BaseColor( 17,  85, 204);

    // ── Font paths ───────────────────────────────────────────────────────────
    private static final String FONT_REGULAR = "c:/windows/fonts/arial.ttf";
    private static final String FONT_BOLD    = "c:/windows/fonts/arialbd.ttf";
    private static final String FONT_ITALIC  = "c:/windows/fonts/ariali.ttf";

    // ════════════════════════════════════════════════════════════════════════
    public static void exportHoaDon(Component parent,
                                    HoaDon hoaDon,
                                    List<ChiTietHoaDon> chiTietList) {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu hóa đơn PDF");
        chooser.setSelectedFile(new File("HoaDon_" + hoaDon.getMaHoaDon() + ".pdf"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf"))
            file = new File(file.getAbsolutePath() + ".pdf");

        try {
            Document doc = new Document(PageSize.A4, 28, 28, 28, 28);
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            // ── Fonts ──────────────────────────────────────────────────────
            BaseFont bf  = BaseFont.createFont(FONT_REGULAR, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfb = BaseFont.createFont(FONT_BOLD,    BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfi = BaseFont.createFont(FONT_ITALIC,  BaseFont.IDENTITY_H, BaseFont.EMBEDDED);

            Font fNorm      = new Font(bf,   10, Font.NORMAL, BaseColor.BLACK);
            Font fSmall     = new Font(bf,    9, Font.NORMAL, BaseColor.BLACK);
            Font fSmallGray = new Font(bf,    8, Font.NORMAL, BaseColor.GRAY);
            Font fSmallIt   = new Font(bfi,   8, Font.ITALIC, BaseColor.GRAY);
            Font fBold      = new Font(bfb,  10, Font.BOLD,   BaseColor.BLACK);
            Font fBoldSm    = new Font(bfb,   9, Font.BOLD,   BaseColor.BLACK);
            Font fTitle     = new Font(bfb,  15, Font.BOLD,   BaseColor.BLACK);
            Font fSubTitle  = new Font(bf,   10, Font.ITALIC, BaseColor.BLACK);
            Font fCompany   = new Font(bfb,  13, Font.BOLD,   BaseColor.BLACK);
            Font fLink      = new Font(bf,    8, Font.NORMAL, CLR_LINK);
            Font fFooterIt  = new Font(bfi,   8, Font.ITALIC, BaseColor.BLACK);
            Font fSigValid  = new Font(bfb,   8, Font.BOLD,   new BaseColor(0, 100, 0));
            Font fTotal     = new Font(bfb,  10, Font.BOLD,   BaseColor.BLACK);

            // ================================================================
            // OUTER BORDER KÉP – bọc toàn bộ nội dung
            // ================================================================
            PdfPTable outerTbl = new PdfPTable(1);
            outerTbl.setWidthPercentage(100);

            PdfPCell outerCell = new PdfPCell();
            outerCell.setBorderColor(CLR_BORDER);
            outerCell.setBorderWidth(2.5f);
            outerCell.setPadding(3f);

            // Inner container
            PdfPTable inner = new PdfPTable(1);
            inner.setWidthPercentage(100);

            // ================================================================
            // PHẦN 1 – HEADER: Logo trái | Thông tin công ty phải
            // ================================================================
            PdfPTable hdr = new PdfPTable(2);
            hdr.setWidthPercentage(100);
            hdr.setWidths(new float[]{1.8f, 3.5f});

            // Logo
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.RIGHT);
            logoCell.setBorderColor(CLR_BORDER);
            logoCell.setBorderWidthRight(1f);
            logoCell.setPadding(8f);
            Paragraph logoTxt = new Paragraph("OHNO\nHOTEL",
                    new Font(bfb, 22, Font.BOLD, new BaseColor(30, 80, 160)));
            logoTxt.setAlignment(Element.ALIGN_CENTER);
            logoCell.addElement(logoTxt);
            Paragraph sloganP = new Paragraph("TÍN NHIỆM – TIỆN ÍCH – TẬN TÌNH",
                    new Font(bf, 7, Font.NORMAL, BaseColor.GRAY));
            sloganP.setAlignment(Element.ALIGN_CENTER);
            logoCell.addElement(sloganP);
            hdr.addCell(logoCell);

            // Thông tin công ty
            PdfPCell compCell = new PdfPCell();
            compCell.setBorder(Rectangle.NO_BORDER);
            compCell.setPadding(8f);
            compCell.addElement(new Paragraph("KHÁCH SẠN OHNO – CÔNG TY CỔ PHẦN OHNO", fCompany));
            compCell.addElement(mkLine("Mã số thuế: ", "0101243150-104", fSmall, fBoldSm));
            compCell.addElement(mkLine("Địa chỉ: ", "12 Cao Lỗ, Phường 4, Quận 8, TP. Hồ Chí Minh", fSmall, fSmall));
            compCell.addElement(mkLine("Điện thoại: ", "(028) 1234 5678", fSmall, fSmall));
            compCell.addElement(mkLine("Số tài khoản: ", "0123456789 – Ngân hàng Vietcombank", fSmall, fSmall));
            hdr.addCell(compCell);

            wrapRow(inner, hdr, Rectangle.BOTTOM, CLR_BORDER, 1f);

            // ================================================================
            // PHẦN 2 – TIÊU ĐỀ + KÝ HIỆU / SỐ
            // ================================================================
            java.time.LocalDateTime now = hoaDon.getNgayLap() != null
                    ? hoaDon.getNgayLap() : java.time.LocalDateTime.now();

            PdfPTable titleRow = new PdfPTable(2);
            titleRow.setWidthPercentage(100);
            titleRow.setWidths(new float[]{4f, 1.5f});

            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setPaddingTop(10f);
            titleCell.setPaddingBottom(4f);
            Paragraph mainTitleP = new Paragraph("HÓA ĐƠN THANH TOÁN DỊCH VỤ", fTitle);
            mainTitleP.setAlignment(Element.ALIGN_CENTER);
            titleCell.addElement(mainTitleP);
            Paragraph dateP = new Paragraph(
                    String.format("Ngày %02d tháng %02d năm %d",
                            now.getDayOfMonth(), now.getMonthValue(), now.getYear()),
                    fSubTitle);
            dateP.setAlignment(Element.ALIGN_CENTER);
            titleCell.addElement(dateP);
            titleRow.addCell(titleCell);

            PdfPCell kyHieuCell = new PdfPCell();
            kyHieuCell.setBorder(Rectangle.NO_BORDER);
            kyHieuCell.setPaddingTop(10f);
            kyHieuCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            kyHieuCell.addElement(mkLine("Ký hiệu: ", "OHN-2025", fSmall, fBoldSm));
            kyHieuCell.addElement(mkLine("Số: ", hoaDon.getMaHoaDon(), fSmall, fBoldSm));
            titleRow.addCell(kyHieuCell);

            wrapRow(inner, titleRow, Rectangle.BOTTOM, CLR_BORDER, 1f);

            // ================================================================
            // PHẦN 3 – THÔNG TIN KHÁCH HÀNG + QR CODE
            // ================================================================
            String customerName = "Vãng lai";
            String donVi = "", maSoThue = "", diaChi = "";
            String roomNo = "N/A", checkIn = "", checkOut = "";

            if (hoaDon.getDatPhong() != null) {
                if (hoaDon.getDatPhong().getKhachHang() != null)
                    customerName = hoaDon.getDatPhong().getKhachHang().getHoTen();
                roomNo = hoaDon.getDatPhong().getSoPhong();
                if (hoaDon.getDatPhong().getNgayNhan() != null)
                    checkIn = hoaDon.getDatPhong().getNgayNhan().toString();
                if (hoaDon.getDatPhong().getNgayTra() != null)
                    checkOut = hoaDon.getDatPhong().getNgayTra().toString();
            } else if (hoaDon.getKhachHang() != null) {
                customerName = hoaDon.getKhachHang().getHoTen();
            }

            String pthuc = hoaDon.getPhuongThucThanhToan();
            if      ("CASH".equalsIgnoreCase(pthuc))     pthuc = "TM/CK";
            else if ("CARD".equalsIgnoreCase(pthuc))     pthuc = "Thẻ ngân hàng";
            else if ("TRANSFER".equalsIgnoreCase(pthuc)) pthuc = "Chuyển khoản";
            if (pthuc == null) pthuc = "TM/CK";

            PdfPTable custQrTbl = new PdfPTable(2);
            custQrTbl.setWidthPercentage(100);
            custQrTbl.setWidths(new float[]{3.5f, 1f});

            PdfPCell custCell = new PdfPCell();
            custCell.setBorder(Rectangle.NO_BORDER);
            custCell.setPadding(5f);

            // Dòng gạch dưới từng trường
            addUnderlineRow(custCell, "Họ tên người mua hàng: " + customerName, fSmall);
            addUnderlineRow(custCell, "Tên đơn vị: " + (donVi.isEmpty() ? " " : donVi), fSmall);
            addUnderlineRow(custCell, "Mã số thuế: " + (maSoThue.isEmpty() ? " " : maSoThue), fSmall);
            addUnderlineRow(custCell, "Địa chỉ: " + (diaChi.isEmpty() ? " " : diaChi), fSmall);

            // Hàng 2 cột: HTT | Phòng
            PdfPTable row2 = new PdfPTable(2);
            row2.setWidthPercentage(100);
            addUnderlineCellTbl(row2, "Hình thức thanh toán: " + pthuc, fSmall);
            addUnderlineCellTbl(row2, "Phòng số: " + roomNo, fSmall);
            custCell.addElement(row2);

            // Ngày nhận – Ngày trả – Số tài khoản
            PdfPTable row3 = new PdfPTable(3);
            row3.setWidthPercentage(100);
            addUnderlineCellTbl(row3, "Ngày đến: " + checkIn, fSmall);
            addUnderlineCellTbl(row3, "Ngày đi: " + checkOut, fSmall);
            addUnderlineCellTbl(row3, "Số tài khoản: ", fSmall);
            custCell.addElement(row3);

            addUnderlineRow(custCell, "Đồng tiền thanh toán: VNĐ", fSmall);
            custQrTbl.addCell(custCell);

            // QR code
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            qrCell.setPadding(4f);
            try {
                String qrData = "OHNO|" + hoaDon.getMaHoaDon()
                        + "|" + now.toLocalDate()
                        + "|" + String.format("%.0f", hoaDon.getTongThanhToan());
                Map<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                BarcodeQRCode qr = new BarcodeQRCode(qrData, 85, 85, hints);
                Image qrImg = qr.getImage();
                qrImg.scaleToFit(85, 85);
                qrCell.addElement(qrImg);
            } catch (Exception ignored) {
                qrCell.addElement(new Paragraph("[QR]", fSmallGray));
            }
            custQrTbl.addCell(qrCell);

            wrapRow(inner, custQrTbl, Rectangle.BOTTOM, CLR_BORDER, 1f);

            // ================================================================
            // PHẦN 4 – BẢNG CHI TIẾT 6 CỘT
            // ================================================================
            PdfPTable detailTbl = new PdfPTable(6);
            detailTbl.setWidthPercentage(100);
            detailTbl.setWidths(new float[]{0.5f, 3.2f, 1.2f, 0.8f, 1.5f, 1.7f});
            detailTbl.setHeaderRows(1);

            String[] headers = {"STT", "Tên hàng hóa, dịch vụ", "Đơn vị tính", "Số lượng", "Đơn giá", "Thành tiền"};
            int[]    haligns = {Element.ALIGN_CENTER, Element.ALIGN_CENTER, Element.ALIGN_CENTER,
                                Element.ALIGN_CENTER, Element.ALIGN_CENTER, Element.ALIGN_CENTER};
            for (int i = 0; i < headers.length; i++)
                addTblHeader(detailTbl, headers[i], haligns[i], fBoldSm, CLR_TBL_HEADER);

            // Data rows (tối thiểu 7 dòng)
            int stt = 1;
            for (ChiTietHoaDon ct : chiTietList) {
                addTblCell(detailTbl, String.valueOf(stt++),      fSmall, Element.ALIGN_CENTER);
                addTblCell(detailTbl, ct.getNoiDung(),            fSmall, Element.ALIGN_LEFT);
                String dvt = ct.getDonViTinh() != null ? ct.getDonViTinh() : "Lần";
                addTblCell(detailTbl, dvt,                        fSmall, Element.ALIGN_CENTER);
                addTblCell(detailTbl, String.valueOf(ct.getSoLuong()), fSmall, Element.ALIGN_CENTER);
                addTblCell(detailTbl, fmtMoney(ct.getDonGia()),   fSmall, Element.ALIGN_RIGHT);
                addTblCell(detailTbl, fmtMoney(ct.getThanhTien()),fSmall, Element.ALIGN_RIGHT);
            }
            for (int i = chiTietList.size(); i < 7; i++)
                for (int j = 0; j < 6; j++) addTblCell(detailTbl, " ", fSmall, Element.ALIGN_LEFT);

            wrapRow(inner, detailTbl, Rectangle.NO_BORDER, CLR_BORDER, 0f);

            // ================================================================
            // PHẦN 5 – KHU VỰC TỔNG TIỀN (chuẩn 2 cột như MISA)
            // ================================================================
            double tongPhong     = hoaDon.getTongTienPhong();
            double tongDV        = hoaDon.getTongTienDichVu();
            double giam          = hoaDon.getTienGiamKhuyenMai();
            double tongHang      = tongPhong + tongDV - giam;
            double thueTTDB      = 0;
            double phiPhuVu      = 0;
            double thueGTGT      = 0;
            double tongThanhToan = hoaDon.getTongThanhToan();

            // Bảng tổng: 2 cột lớn
            PdfPTable sumOuter = new PdfPTable(2);
            sumOuter.setWidthPercentage(100);
            sumOuter.setWidths(new float[]{1f, 1f});

            // Cột trái: Thuế TTĐB + Thuế GTGT
            PdfPTable leftTax = new PdfPTable(2);
            leftTax.setWidthPercentage(100);
            leftTax.setWidths(new float[]{2f, 1f});
            addTaxCell(leftTax, "Thuế TTĐB:",     true,  fSmall);
            addTaxCell(leftTax, "%",               false, fSmall);
            addTaxCell(leftTax, "Thuế suất GTGT:", true,  fSmall);
            addTaxCell(leftTax, "0%",              false, fSmall);
            PdfPCell ltWrap = new PdfPCell(leftTax);
            ltWrap.setBorder(Rectangle.BOX);
            ltWrap.setBorderColor(CLR_BORDER);
            ltWrap.setBorderWidth(0.5f);
            ltWrap.setPadding(0f);
            sumOuter.addCell(ltWrap);

            // Cột phải: 5 dòng tổng
            PdfPTable rightSum = new PdfPTable(2);
            rightSum.setWidthPercentage(100);
            rightSum.setWidths(new float[]{1.6f, 1.2f});
            addSumRow(rightSum, "Tiền thuế TTĐB:",       fmtMoney(thueTTDB),      fSmall, fSmall);
            addSumRow(rightSum, "Cộng tiền hàng:",       fmtMoney(tongHang),      fSmall, fSmall);
            addSumRow(rightSum, "Tiền phí phục vụ:",     fmtMoney(phiPhuVu),      fSmall, fSmall);
            addSumRow(rightSum, "Tiền thuế GTGT:",       fmtMoney(thueGTGT),      fSmall, fSmall);
            addSumRow(rightSum, "Tổng tiền thanh toán:", fmtMoney(tongThanhToan), fTotal, fTotal);
            PdfPCell rtWrap = new PdfPCell(rightSum);
            rtWrap.setBorder(Rectangle.BOX);
            rtWrap.setBorderColor(CLR_BORDER);
            rtWrap.setBorderWidth(0.5f);
            rtWrap.setPadding(0f);
            sumOuter.addCell(rtWrap);

            wrapRow(inner, sumOuter, Rectangle.BOX, CLR_BORDER, 0.5f);

            // ================================================================
            // PHẦN 6 – SỐ TIỀN BẰNG CHỮ
            // ================================================================
            PdfPTable wordsTbl = new PdfPTable(1);
            wordsTbl.setWidthPercentage(100);
            PdfPCell wordsCell = new PdfPCell();
            wordsCell.setBorder(Rectangle.BOTTOM);
            wordsCell.setBorderColor(CLR_BORDER);
            wordsCell.setBorderWidthBottom(0.5f);
            wordsCell.setPadding(5f);
            wordsCell.addElement(new Paragraph(
                    "Số tiền viết bằng chữ: "
                            + convertToVietnameseWords((long) tongThanhToan)
                            + " đồng chẵn.", fSmall));
            wordsTbl.addCell(wordsCell);
            wrapRow(inner, wordsTbl, Rectangle.NO_BORDER, CLR_BORDER, 0f);

            // ================================================================
            // PHẦN 7 – CHỮ KÝ
            // ================================================================
            PdfPTable sigTbl = new PdfPTable(2);
            sigTbl.setWidthPercentage(100);

            // Người mua hàng
            PdfPCell buyerCell = new PdfPCell();
            buyerCell.setBorder(Rectangle.NO_BORDER);
            buyerCell.setPadding(6f);
            Paragraph buyerTitle = new Paragraph("Người mua hàng", fBoldSm);
            buyerTitle.setAlignment(Element.ALIGN_CENTER);
            buyerCell.addElement(buyerTitle);
            Paragraph buyerSub = new Paragraph("(Chữ ký số (nếu có))", fSmallIt);
            buyerSub.setAlignment(Element.ALIGN_CENTER);
            buyerCell.addElement(buyerSub);
            buyerCell.addElement(new Paragraph("\n\n\n\n"));
            sigTbl.addCell(buyerCell);

            // Người bán hàng + Signature Valid box
            PdfPCell sellerCell = new PdfPCell();
            sellerCell.setBorder(Rectangle.NO_BORDER);
            sellerCell.setPadding(6f);
            Paragraph sellerTitle = new Paragraph("Người bán hàng", fBoldSm);
            sellerTitle.setAlignment(Element.ALIGN_CENTER);
            sellerCell.addElement(sellerTitle);
            Paragraph sellerSub = new Paragraph("(Chữ ký điện tử, chữ ký số)", fSmallIt);
            sellerSub.setAlignment(Element.ALIGN_CENTER);
            sellerCell.addElement(sellerSub);
            sellerCell.addElement(new Paragraph("\n"));

            // Khung Signature Valid (góc phải, giống MISA)
            PdfPTable svTbl = new PdfPTable(1);
            svTbl.setWidthPercentage(65);
            svTbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
            PdfPCell svCell = new PdfPCell();
            svCell.setBackgroundColor(CLR_SIG_VALID);
            svCell.setBorderColor(CLR_SIG_BORDER);
            svCell.setBorderWidth(1f);
            svCell.setPadding(5f);
            Paragraph sv1 = new Paragraph("Signature Valid", fSigValid);
            sv1.setAlignment(Element.ALIGN_CENTER);
            svCell.addElement(sv1);
            Paragraph sv2 = new Paragraph("Ký bởi: Công ty Cổ phần OHNO",
                    new Font(bf, 7, Font.NORMAL, new BaseColor(0, 80, 0)));
            sv2.setAlignment(Element.ALIGN_CENTER);
            svCell.addElement(sv2);
            Paragraph sv3 = new Paragraph(
                    "Ký ngày: " + String.format("%02d/%02d/%d",
                            now.getDayOfMonth(), now.getMonthValue(), now.getYear()),
                    new Font(bf, 7, Font.NORMAL, new BaseColor(0, 80, 0)));
            sv3.setAlignment(Element.ALIGN_CENTER);
            svCell.addElement(sv3);
            svTbl.addCell(svCell);
            sellerCell.addElement(svTbl);
            sellerCell.addElement(new Paragraph("\n"));
            sigTbl.addCell(sellerCell);

            wrapRow(inner, sigTbl, Rectangle.NO_BORDER, CLR_BORDER, 0f);

            // ================================================================
            // PHẦN 8 – FOOTER
            // ================================================================
            PdfPTable footerTbl = new PdfPTable(1);
            footerTbl.setWidthPercentage(100);
            PdfPCell ftCell = new PdfPCell();
            ftCell.setBorderColor(CLR_BORDER);
            ftCell.setBorder(Rectangle.TOP);
            ftCell.setBorderWidthTop(0.5f);
            ftCell.setPadding(5f);

            // Dòng tra cứu
            Phrase ftPhrase = new Phrase();
            ftPhrase.add(new Chunk("Tra cứu tại Website: ", fSmallGray));
            ftPhrase.add(new Chunk("https://ohno.vn/tra-cuu", fLink));
            ftPhrase.add(new Chunk("  –  Mã tra cứu: " + hoaDon.getMaHoaDon().toUpperCase(), fSmallGray));
            ftCell.addElement(new Paragraph(ftPhrase));

            Paragraph ft2 = new Paragraph("(Cần kiểm tra, đối chiếu khi lập, giao, nhận hóa đơn)", fFooterIt);
            ft2.setAlignment(Element.ALIGN_CENTER);
            ftCell.addElement(ft2);

            Paragraph ft3 = new Paragraph(
                    "Phát hành bởi phần mềm quản lý khách sạn OHNO (www.ohno.vn) – MST: 0101243150",
                    fSmallGray);
            ft3.setAlignment(Element.ALIGN_CENTER);
            ftCell.addElement(ft3);
            footerTbl.addCell(ftCell);
            wrapRow(inner, footerTbl, Rectangle.NO_BORDER, CLR_BORDER, 0f);

            // ── Gắn inner vào outer và thêm vào doc ────────────────────────
            PdfPCell innerCell = new PdfPCell(inner);
            innerCell.setBorder(Rectangle.BOX);
            innerCell.setBorderColor(CLR_BORDER);
            innerCell.setBorderWidth(1f);
            innerCell.setPadding(0f);
            outerCell.addElement(inner);
            outerTbl.addCell(outerCell);
            doc.add(outerTbl);

            doc.close();
            JOptionPane.showMessageDialog(parent,
                    "Xuất hóa đơn thành công!\nFile: " + file.getAbsolutePath(),
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    "Lỗi xuất file PDF: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ════════════════════════════════════════════════════════════════════════

    /** Bọc một PdfPTable con thành 1 row trong inner */
    private static void wrapRow(PdfPTable inner, PdfPTable content,
                                int border, BaseColor borderColor, float bw) {
        PdfPCell c = new PdfPCell(content);
        c.setPadding(0f);
        c.setBorderWidth(bw);
        if (border == Rectangle.NO_BORDER) {
            c.setBorder(Rectangle.NO_BORDER);
        } else if (border == Rectangle.BOTTOM) {
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(borderColor);
            c.setBorderWidthBottom(bw);
        } else {
            c.setBorder(border);
            c.setBorderColor(borderColor);
        }
        inner.addCell(c);
    }

    /** Tạo Paragraph label + value */
    private static Paragraph mkLine(String label, String value, Font fLabel, Font fValue) {
        Phrase ph = new Phrase();
        ph.add(new Chunk(label, fLabel));
        ph.add(new Chunk(value, fValue));
        return new Paragraph(ph);
    }

    /** Thêm dòng gạch chân vào cell (thông tin khách hàng) */
    private static void addUnderlineRow(PdfPCell parent, String text, Font font) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(BaseColor.LIGHT_GRAY);
        c.setBorderWidthBottom(0.5f);
        c.setPaddingBottom(3f);
        c.setPaddingTop(3f);
        t.addCell(c);
        parent.addElement(t);
    }

    /** Ô gạch dưới trong bảng nhỏ (thông tin khách) */
    private static void addUnderlineCellTbl(PdfPTable t, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(BaseColor.LIGHT_GRAY);
        c.setBorderWidthBottom(0.5f);
        c.setPaddingBottom(3f);
        c.setPaddingTop(3f);
        t.addCell(c);
    }

    /** Header cột bảng chi tiết */
    private static void addTblHeader(PdfPTable t, String text, int align,
                                     Font font, BaseColor bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorderColor(CLR_BORDER);
        c.setBorderWidth(0.5f);
        c.setPadding(5f);
        t.addCell(c);
    }

    /** Ô dữ liệu bảng chi tiết */
    private static void addTblCell(PdfPTable t, String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(align);
        c.setBorderColor(CLR_BORDER);
        c.setBorderWidth(0.5f);
        c.setPadding(4f);
        t.addCell(c);
    }

    /** Ô bảng thuế (trái: label bold, phải: %) */
    private static void addTaxCell(PdfPTable t, String text, boolean isLabel, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorderColor(CLR_BORDER);
        c.setBorderWidth(0.5f);
        c.setPadding(4f);
        c.setHorizontalAlignment(isLabel ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
        t.addCell(c);
    }

    /** Một dòng trong bảng tổng tiền */
    private static void addSumRow(PdfPTable t, String label, String value,
                                  Font fLabel, Font fValue) {
        PdfPCell lc = new PdfPCell(new Phrase(label, fLabel));
        lc.setBorderColor(CLR_BORDER);
        lc.setBorderWidth(0.5f);
        lc.setPadding(4f);
        lc.setHorizontalAlignment(Element.ALIGN_LEFT);
        t.addCell(lc);
        PdfPCell vc = new PdfPCell(new Phrase(value, fValue));
        vc.setBorderColor(CLR_BORDER);
        vc.setBorderWidth(0.5f);
        vc.setPadding(4f);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(vc);
    }

    /** Định dạng số tiền */
    private static String fmtMoney(double v) {
        return String.format("%,.0f", v);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ĐỌC SỐ TIỀN BẰNG CHỮ
    // ════════════════════════════════════════════════════════════════════════
    public static String convertToVietnameseWords(long number) {
        if (number == 0) return "Không";
        String[] units = {"", "ngàn", "triệu", "tỷ", "ngàn tỷ", "triệu tỷ"};
        String res = "";
        int ui = 0;
        do {
            long part = number % 1000;
            if (part > 0) res = convertHundreds((int) part) + " " + units[ui] + " " + res;
            number /= 1000;
            ui++;
        } while (number > 0);
        res = res.trim().replaceAll("\\s+", " ");
        if (!res.isEmpty()) res = Character.toUpperCase(res.charAt(0)) + res.substring(1);
        return res;
    }

    private static String convertHundreds(int n) {
        String[] ones = {"không","một","hai","ba","bốn","năm","sáu","bảy","tám","chín"};
        int h = n / 100, t = (n % 100) / 10, u = n % 10;
        String res = ones[h] + " trăm";
        if      (t == 0 && u != 0) res += " lẻ " + ones[u];
        else if (t != 0) {
            res += (t == 1) ? " mười" : " " + ones[t] + " mươi";
            if      (u == 1 && t > 1) res += " mốt";
            else if (u == 5)          res += " lăm";
            else if (u != 0)          res += " " + ones[u];
        }
        return res;
    }
}