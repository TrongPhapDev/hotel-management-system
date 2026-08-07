import React, { useEffect, useState } from 'react';
import { chiTietDatPhongAPI, dichVuAPI, khuyenMaiAPI, thuePhongAPI, phongAPI } from '../../api/api';
import { X, Search, Plus, Calendar, ShieldCheck, DollarSign, Clock, Users, Bookmark, FileText, CheckCircle2, ArrowLeftRight, Hourglass, Printer, Edit, Trash2 } from 'lucide-react';

const fmt = (n) => (n || 0).toLocaleString('vi-VN');

const convertToVietnameseWords = (number) => {
  if (number === 0) return "Không";
  const units = ["", "ngàn", "triệu", "tỷ", "ngàn tỷ", "triệu tỷ"];
  const ones = ["không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"];
  let res = "";
  let ui = 0;
  let num = Math.round(number);

  const convertGroup = (n, isLeading) => {
    let h = Math.floor(n / 100);
    let t = Math.floor((n % 100) / 10);
    let u = n % 10;
    let r = "";

    if (h > 0) {
      r = ones[h] + " trăm";
      if (t === 0 && u !== 0) {
        r += " lẻ " + ones[u];
      } else if (t !== 0) {
        r += " " + (t === 1 ? "mười" : ones[t] + " mươi");
        if (u === 1 && t > 1) r += " mốt";
        else if (u === 5) r += " lăm";
        else if (u !== 0) r += " " + ones[u];
      }
    } else {
      if (isLeading) {
        if (t === 0) {
          r = ones[u];
        } else {
          r = (t === 1 ? "mười" : ones[t] + " mươi");
          if (u === 1 && t > 1) r += " mốt";
          else if (u === 5) r += " lăm";
          else if (u !== 0) r += " " + ones[u];
        }
      } else {
        r = "không trăm";
        if (t === 0 && u !== 0) {
          r += " lẻ " + ones[u];
        } else if (t !== 0) {
          r += " " + (t === 1 ? "mười" : ones[t] + " mươi");
          if (u === 1 && t > 1) r += " mốt";
          else if (u === 5) r += " lăm";
          else if (u !== 0) r += " " + ones[u];
        }
      }
    }
    return r;
  };

  while (num > 0) {
    let part = num % 1000;
    let nextNum = Math.floor(num / 1000);
    if (part > 0) {
      let isLeading = nextNum === 0;
      res = convertGroup(part, isLeading) + " " + units[ui] + " " + res;
    }
    num = nextNum;
    ui++;
  }

  res = res.trim().replace(/\s+/g, " ");
  if (res.length > 0) {
    res = res.charAt(0).toUpperCase() + res.slice(1);
  }
  return res;
};

export const CheckoutDialog = ({ isOpen, onClose, room, onCheckoutSuccess }) => {
  const [stayDetail, setStayDetail] = useState(null);
  const [services, setServices] = useState([]);
  const [promotions, setPromotions] = useState([]);
  const [loading, setLoading] = useState(true);

  // Form Fields / Options
  const [voucherCode, setVoucherCode] = useState('');
  const [customDeposit, setCustomDeposit] = useState(0);
  const [cashReceived, setCashReceived] = useState(0);
  const [paymentMethod, setPaymentMethod] = useState('CASH'); // CASH, CARD, TRANSFER, DEBT
  const [previewData, setPreviewData] = useState(null);

  // Multi-room Booking Support (Gombill)
  const [allStaysOfBooking, setAllStaysOfBooking] = useState([]);
  const [checkoutMode, setCheckoutMode] = useState('SINGLE'); // SINGLE or GOMBILL
  const [previewsMap, setPreviewsMap] = useState({});

  // Operation dialogs nested within checkout
  const [addServiceForm, setAddServiceForm] = useState({ maDV: '', soLuong: 1 });
  const [showTransfer, setShowTransfer] = useState(false);
  const [transferForm, setTransferForm] = useState({ maPhongMoi: '', giuNguyenGia: true });
  const [availableRooms, setAvailableRooms] = useState([]);
  const [showExtend, setShowExtend] = useState(false);
  const [extendForm, setExtendForm] = useState({ ngayTraMoi: '' });

  useEffect(() => {
    if (isOpen && room) {
      loadCheckoutData();
    }
  }, [isOpen, room]);

  const loadCheckoutData = async () => {
    setLoading(true);
    try {
      // 1. Get stays for room
      const staysRes = await chiTietDatPhongAPI.getActive();
      const activeStay = staysRes.data.find(s => s.phong?.maPhong === room.maPhong);
      if (activeStay) {
        setStayDetail(activeStay);
        setCustomDeposit(activeStay.datPhong?.tienDatCoc || 0);

        // Fetch all rooms in this booking
        if (activeStay.datPhong?.maDatPhong) {
          const resAll = await chiTietDatPhongAPI.getAll(activeStay.datPhong.maDatPhong);
          // Only take active stays (daThanhToan is false)
          setAllStaysOfBooking(resAll.data.filter(s => !s.daThanhToan));
        }
      }

      // 2. Get list of services & promotions
      const [sRes, pRes] = await Promise.all([
        dichVuAPI.getAll(),
        khuyenMaiAPI.getActive()
      ]);
      setServices(sRes.data);
      setPromotions(pRes.data);
    } catch (e) { console.error(e); }
    setLoading(false);
  };

  const fetchPreview = async () => {
    if (!stayDetail) return;
    try {
      const res = await thuePhongAPI.previewCheckout(stayDetail.maChiTiet, {
        voucherCode: voucherCode,
        customDeposit: customDeposit
      });
      setPreviewData(res.data);
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    if (stayDetail) {
      fetchPreview();
    }
  }, [stayDetail, voucherCode, customDeposit]);

  // Fetch previews for all rooms in Gombill mode
  useEffect(() => {
    if (allStaysOfBooking.length > 0) {
      const fetchAllPreviews = async () => {
        const temp = {};
        for (const s of allStaysOfBooking) {
          try {
            const res = await thuePhongAPI.previewCheckout(s.maChiTiet, {
              voucherCode: voucherCode,
              customDeposit: 0
            });
            temp[s.maChiTiet] = res.data;
          } catch (e) {
            console.error("Error previewing room stay", s.maChiTiet, e);
          }
        }
        setPreviewsMap(temp);
      };
      fetchAllPreviews();
    }
  }, [allStaysOfBooking, voucherCode]);

  const handleInlineAddService = async (e) => {
    e.preventDefault();
    if (!stayDetail || !addServiceForm.maDV) return;
    try {
      const selected = services.find(s => s.maDichVu === addServiceForm.maDV);
      await thuePhongAPI.themDichVu({
        maChiTiet: stayDetail.maChiTiet,
        maDV: addServiceForm.maDV,
        soLuong: addServiceForm.soLuong,
        donGia: selected ? selected.donGia : 0
      });
      alert("Đã thêm dịch vụ!");
      setAddServiceForm({ maDV: '', soLuong: 1 });
      loadCheckoutData();
      fetchPreview();
    } catch (e) { alert(e.response?.data || "Lỗi thêm dịch vụ"); }
  };

  const handleEditService = async (maSuDung, currentQty) => {
    const promptVal = prompt("Nhập số lượng mới:", currentQty);
    if (promptVal === null) return;
    const newQty = parseInt(promptVal);
    if (isNaN(newQty)) return alert("Số lượng không hợp lệ!");
    if (newQty <= 0) {
      handleDeleteService(maSuDung);
      return;
    }
    try {
      await thuePhongAPI.suaSuDungDichVu(maSuDung, newQty);
      loadCheckoutData();
      fetchPreview();
    } catch (e) {
      alert(e.response?.data || "Lỗi sửa số lượng dịch vụ");
    }
  };

  const handleDeleteService = async (maSuDung) => {
    if (!confirm("Bạn chắc chắn muốn xóa dịch vụ này khỏi hóa đơn?")) return;
    try {
      await thuePhongAPI.xoaSuDungDichVu(maSuDung);
      loadCheckoutData();
      fetchPreview();
    } catch (e) {
      alert(e.response?.data || "Lỗi xóa dịch vụ");
    }
  };

  const handleOpenTransfer = async () => {
    try {
      const res = await phongAPI.getAll('AVAILABLE');
      setAvailableRooms(res.data);
      setShowTransfer(true);
    } catch (e) { console.error(e); }
  };

  const handleTransfer = async (e) => {
    e.preventDefault();
    if (!stayDetail) return;
    try {
      await thuePhongAPI.doiPhong({
        maChiTiet: stayDetail.maChiTiet,
        maPhongMoi: transferForm.maPhongMoi,
        giuNguyenGia: transferForm.giuNguyenGia
      });
      alert("Đã đổi phòng!");
      setShowTransfer(false);
      onCheckoutSuccess();
      onClose();
    } catch (e) { alert(e.response?.data || "Lỗi đổi phòng"); }
  };

  const handleExtend = async (e) => {
    e.preventDefault();
    if (!stayDetail) return;
    try {
      await thuePhongAPI.giaHan({
        maChiTiet: stayDetail.maChiTiet,
        ngayTraMoi: new Date(extendForm.ngayTraMoi).toISOString()
      });
      alert("Gia hạn phòng thành công!");
      setShowExtend(false);
      loadCheckoutData();
    } catch (e) { alert(e.response?.data || "Lỗi gia hạn"); }
  };

  const handleCheckoutSubmit = async () => {
    if (!stayDetail) return;
    try {
      if (checkoutMode === 'GOMBILL') {
        // Combined checkout (Gombill)
        await thuePhongAPI.checkOutMaster({
          maDatPhong: stayDetail.datPhong.maDatPhong,
          maNhanVien: "NV001", // fallback
          voucherCode: voucherCode
        });
        alert("Thanh toán gộp (Gombill) thành công!");
      } else {
        // Single checkout
        await thuePhongAPI.checkOut({
          maChiTiet: stayDetail.maChiTiet,
          maNhanVien: "NV001", // fallback
          trangThai: paymentMethod === 'DEBT' ? 'DEBT' : 'PAID',
          voucherCode: voucherCode,
          customDeposit: Number(customDeposit)
        });
        alert(paymentMethod === 'DEBT' ? "Đã ghi nợ thành công!" : "Trả phòng và thanh toán thành công!");
      }
      onCheckoutSuccess();
      onClose();
    } catch (e) {
      alert(e.response?.data || "Lỗi trong quá trình thanh toán!");
    }
  };

  // Print separate invoice
  const handlePrint = () => {
    if (!previewData || !stayDetail) return;
    const printWindow = window.open('', '_blank', 'width=850,height=800');
    if (!printWindow) return;

    let items = [];

    // Add Room charge
    items.push({
      noiDung: `Tiền phòng (P.${previewData.maPhong} - ${previewData.soNgay} đêm)`,
      donVi: "Đêm",
      soLuong: previewData.soNgay,
      donGia: previewData.donGiaPhong,
      thanhTien: previewData.tienPhong
    });

    if (previewData.phuPhiCheckInEarly > 0) {
      items.push({
        noiDung: "Phụ thu check-in sớm",
        donVi: "Lần",
        soLuong: 1,
        donGia: previewData.phuPhiCheckInEarly,
        thanhTien: previewData.phuPhiCheckInEarly
      });
    }

    if (previewData.phuPhiCheckOutLate > 0) {
      items.push({
        noiDung: "Phụ thu check-out trễ",
        donVi: "Lần",
        soLuong: 1,
        donGia: previewData.phuPhiCheckOutLate,
        thanhTien: previewData.phuPhiCheckOutLate
      });
    }

    if (previewData.phuPhiKhac > 0) {
      items.push({
        noiDung: "Phí phát sinh khác",
        donVi: "Lần",
        soLuong: 1,
        donGia: previewData.phuPhiKhac,
        thanhTien: previewData.phuPhiKhac
      });
    }

    if (previewData.dsDichVu && previewData.dsDichVu.length > 0) {
      previewData.dsDichVu.forEach(dv => {
        items.push({
          noiDung: `[Dịch vụ] ${dv.tenDichVu}`,
          donVi: dv.donViTinh || "Lần",
          soLuong: dv.soLuong,
          donGia: dv.donGia,
          thanhTien: dv.thanhTien
        });
      });
    }

    // Minimum 7 rows
    const originalLen = items.length;
    for (let i = originalLen; i < 7; i++) {
      items.push({
        noiDung: "&nbsp;",
        donVi: "&nbsp;",
        soLuong: "&nbsp;",
        donGia: "",
        thanhTien: ""
      });
    }

    let itemsHtml = "";
    items.forEach((item, idx) => {
      const isDummy = item.noiDung === "&nbsp;";
      itemsHtml += `
        <tr>
          <td class="align-center">${isDummy ? "&nbsp;" : (idx + 1)}</td>
          <td class="align-left">${item.noiDung}</td>
          <td class="align-center">${item.donVi}</td>
          <td class="align-center">${item.soLuong}</td>
          <td class="align-right">${item.donGia !== "" ? fmt(item.donGia) + "đ" : "&nbsp;"}</td>
          <td class="align-right" style="${!isDummy ? 'font-weight: bold;' : ''}">${item.thanhTien !== "" ? fmt(item.thanhTien) + "đ" : "&nbsp;"}</td>
        </tr>
      `;
    });

    const now = new Date();
    const dateStr = `Ngày ${String(now.getDate()).padStart(2, '0')} tháng ${String(now.getMonth() + 1).padStart(2, '0')} năm ${now.getFullYear()}`;
    const dateLabel = `${String(now.getDate()).padStart(2, '0')}/${String(now.getMonth() + 1).padStart(2, '0')}/${now.getFullYear()}`;
    const customerName = stayDetail.khachHang?.hoTen || "Vãng lai";
    const phone = stayDetail.khachHang?.soDienThoai || "";
    const cccd = stayDetail.khachHang?.cccd || "";
    const checkInDate = stayDetail.ngayNhanThucTe ? new Date(stayDetail.ngayNhanThucTe).toLocaleString('vi-VN') : "—";
    const checkOutDate = now.toLocaleString('vi-VN');
    const invoiceNo = `HD-${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}-${previewData.maPhong}`;
    const pthuc = paymentMethod === 'CASH' ? 'TM/CK' : paymentMethod === 'CARD' ? 'Thẻ ngân hàng' : paymentMethod === 'TRANSFER' ? 'Chuyển khoản' : 'Treo nợ';
    const qrData = `OHNO|${invoiceNo}|${now.toISOString().substring(0, 10)}|${totalAmount}`;

    const words = convertToVietnameseWords(totalAmount);

    const tongPhongDV = previewData.tienPhong + previewData.tongDichVu + previewData.phuPhiCheckInEarly + previewData.phuPhiCheckOutLate + previewData.phuPhiKhac;

    const htmlContent = `
      <html>
      <head>
        <title>Hóa đơn thanh toán - Phòng ${previewData.maPhong}</title>
        <style>
          @page {
            size: A4 portrait;
            margin: 12mm 15mm;
          }
          body {
            font-family: 'Times New Roman', Times, serif, Arial;
            color: #1a1a1a;
            background-color: #fff;
            margin: 0;
            padding: 0;
            font-size: 13px;
            line-height: 1.4;
          }
          .outer-border {
            border: 2px solid #969696;
            padding: 3px;
            box-sizing: border-box;
            width: 100%;
            min-height: 270mm;
            display: flex;
            flex-direction: column;
          }
          .inner-container {
            border: 1px solid #969696;
            padding: 18px;
            box-sizing: border-box;
            flex: 1;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
          }
          .header-tbl {
            width: 100%;
            border-collapse: collapse;
            border-bottom: 1px solid #969696;
            margin-bottom: 10px;
          }
          .header-logo {
            width: 30%;
            text-align: center;
            border-right: 1px solid #969696;
            padding: 8px;
          }
          .header-logo-text {
            font-size: 26px;
            font-weight: bold;
            color: #1e50a0;
            line-height: 1.1;
            margin: 0;
          }
          .header-slogan {
            font-size: 8px;
            color: #666;
            margin-top: 5px;
            letter-spacing: 0.5px;
          }
          .header-info {
            padding: 8px 15px;
            text-align: left;
            vertical-align: top;
            font-size: 11px;
          }
          .company-title {
            font-size: 14px;
            font-weight: bold;
            margin: 0 0 5px 0;
          }
          .company-line {
            font-size: 11px;
            margin: 2px 0;
          }
          .company-label {
            font-weight: bold;
          }
          .title-section {
            width: 100%;
            border-collapse: collapse;
            border-bottom: 1px solid #969696;
            margin-bottom: 10px;
          }
          .title-cell {
            text-align: center;
            padding: 8px 0;
            vertical-align: middle;
          }
          .main-title {
            font-size: 18px;
            font-weight: bold;
            margin: 0;
            letter-spacing: 0.5px;
          }
          .sub-title-date {
            font-size: 12px;
            font-style: italic;
            margin: 4px 0 0 0;
          }
          .meta-cell {
            width: 30%;
            padding: 8px 10px;
            vertical-align: top;
            font-size: 11px;
          }
          .meta-line {
            margin: 3px 0;
          }
          .meta-label {
            font-weight: bold;
          }
          .customer-section {
            width: 100%;
            border-collapse: collapse;
            border-bottom: 1px solid #969696;
            margin-bottom: 12px;
          }
          .customer-cell {
            padding: 2px 8px 6px 0;
            vertical-align: middle;
          }
          .underline-field {
            border-bottom: 1px dotted #888;
            padding: 4px 0;
            font-size: 12px;
          }
          .qr-code-cell {
            width: 100px;
            text-align: right;
            vertical-align: middle;
            padding: 0 0 8px 10px;
          }
          .details-tbl {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 12px;
          }
          .details-tbl th {
            background-color: #DCDCDC;
            border: 1px solid #969696;
            padding: 6px 4px;
            font-size: 11px;
            font-weight: bold;
            text-align: center;
          }
          .details-tbl td {
            border: 1px solid #969696;
            padding: 6px 6px;
            font-size: 11px;
          }
          .align-center { text-align: center; }
          .align-left { text-align: left; }
          .align-right { text-align: right; }
          
          .summary-section {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 12px;
          }
          .summary-left {
            width: 50%;
            border: 1px solid #969696;
            vertical-align: top;
            padding: 0;
          }
          .summary-right {
            width: 50%;
            border: 1px solid #969696;
            vertical-align: top;
            padding: 0;
          }
          .tax-tbl, .sum-val-tbl {
            width: 100%;
            border-collapse: collapse;
          }
          .tax-tbl td {
            padding: 6px;
            border-bottom: 1px solid #969696;
            border-right: 1px solid #969696;
            font-size: 11px;
          }
          .tax-tbl tr:last-child td {
            border-bottom: none;
          }
          .tax-tbl td:last-child {
            border-right: none;
          }
          .sum-val-tbl td {
            padding: 6px;
            border-bottom: 1px solid #969696;
            border-right: 1px solid #969696;
            font-size: 11px;
          }
          .sum-val-tbl tr:last-child td {
            border-bottom: none;
          }
          .sum-val-tbl td:last-child {
            border-right: none;
          }
          .sum-total-row {
            font-weight: bold;
            font-size: 12px;
          }
          
          .words-section {
            font-style: italic;
            font-size: 12px;
            padding: 6px 0;
            border-bottom: 1px solid #969696;
            margin-bottom: 15px;
          }
          
          .signature-section {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 25px;
          }
          .signature-cell {
            width: 50%;
            text-align: center;
            vertical-align: top;
            padding: 10px;
          }
          .signature-title {
            font-weight: bold;
            font-size: 13px;
          }
          .signature-sub {
            font-size: 11px;
            font-style: italic;
            color: #555;
            margin-top: 2px;
          }
          .sig-valid-box {
            margin: 10px auto 0 auto;
            width: 180px;
            background-color: #C6EFC6;
            border: 1px solid #468250;
            border-radius: 4px;
            padding: 6px;
            text-align: center;
            font-family: Arial, sans-serif;
          }
          .sig-valid-title {
            font-weight: bold;
            color: #006400;
            font-size: 11px;
            margin-bottom: 2px;
          }
          .sig-valid-line {
            font-size: 9px;
            color: #005000;
            margin: 1px 0;
          }
          
          .footer-section {
            width: 100%;
            border-collapse: collapse;
            margin-top: auto;
          }
          .footer-cell {
            border-top: 1px solid #969696;
            padding-top: 8px;
            text-align: center;
            font-size: 11px;
            color: #666;
          }
          .footer-link {
            color: #1155CC;
            text-decoration: none;
            font-weight: bold;
          }
        </style>
      </head>
      <body>
        <div class="outer-border">
          <div class="inner-container">
            
            <!-- Company Info & Logo -->
            <table class="header-tbl">
              <tr>
                <td class="header-logo">
                  <div class="header-logo-text">OHNO</div>
                  <div class="header-logo-text" style="color: #4a5068;">HOTEL</div>
                  <div class="header-slogan">TÍN NHIỆM – TIỆN ÍCH – TẬN TÌNH</div>
                </td>
                <td class="header-info">
                  <div class="company-title">KHÁCH SẠN OHNO – CÔNG TY CỔ PHẦN OHNO</div>
                  <div class="company-line"><span class="company-label">Mã số thuế:</span> 0101243150-104</div>
                  <div class="company-line"><span class="company-label">Địa chỉ:</span> 12 Cao Lỗ, Phường 4, Quận 8, TP. Hồ Chí Minh</div>
                  <div class="company-line"><span class="company-label">Điện thoại:</span> (028) 1234 5678</div>
                  <div class="company-line"><span class="company-label">Số tài khoản:</span> 0123456789 – Ngân hàng Vietcombank</div>
                </td>
              </tr>
            </table>

            <!-- Title & Metadata -->
            <table class="title-section">
              <tr>
                <td class="title-cell">
                  <div class="main-title">HÓA ĐƠN THANH TOÁN DỊCH VỤ</div>
                  <div class="sub-title-date">${dateStr}</div>
                </td>
                <td class="meta-cell">
                  <div class="meta-line"><span class="meta-label">Ký hiệu:</span> OHN-2026</div>
                  <div class="meta-line"><span class="meta-label">Số:</span> ${invoiceNo}</div>
                </td>
              </tr>
            </table>

            <!-- Customer & Stay Info -->
            <table class="customer-section">
              <tr>
                <td class="customer-cell">
                  <div class="underline-field">Họ tên người mua hàng: <strong>${customerName}</strong></div>
                  <div class="underline-field">Tên đơn vị: &nbsp;</div>
                  <div class="underline-field">Mã số thuế: &nbsp;</div>
                  <div class="underline-field">Địa chỉ: &nbsp;</div>
                  <table style="width: 100%; border-collapse: collapse;">
                    <tr>
                      <td style="width: 55%; padding: 4px 0 0 0;" class="underline-field">Hình thức thanh toán: <strong>${pthuc}</strong></td>
                      <td style="padding: 4px 0 0 0;" class="underline-field">Phòng số: <strong>P.${previewData.maPhong}</strong></td>
                    </tr>
                  </table>
                  <table style="width: 100%; border-collapse: collapse;">
                    <tr>
                      <td style="width: 35%; padding: 4px 0 0 0;" class="underline-field">Ngày đến: <strong>${checkInDate}</strong></td>
                      <td style="width: 35%; padding: 4px 0 0 0;" class="underline-field">Ngày đi: <strong>${checkOutDate}</strong></td>
                      <td style="padding: 4px 0 0 0;" class="underline-field">Số tài khoản: &nbsp;</td>
                    </tr>
                  </table>
                  <div class="underline-field" style="border-bottom: none; padding-bottom: 0;">Đồng tiền thanh toán: <strong>VNĐ</strong></div>
                </td>
                <td class="qr-code-cell">
                  <img src="https://api.qrserver.com/v1/create-qr-code/?size=90x90&data=${encodeURIComponent(qrData)}" alt="QR" style="width: 85px; height: 85px; display: inline-block; border: 1px solid #ccc; padding: 2px;" />
                </td>
              </tr>
            </table>

            <!-- Details Table -->
            <table class="details-tbl">
              <thead>
                <tr>
                  <th style="width: 5%;">STT</th>
                  <th style="width: 48%;">Tên hàng hóa, dịch vụ</th>
                  <th style="width: 12%;">Đơn vị tính</th>
                  <th style="width: 10%;">Số lượng</th>
                  <th style="width: 12%;">Đơn giá</th>
                  <th style="width: 13%;">Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                ${itemsHtml}
              </tbody>
            </table>

            <!-- Summary Section -->
            <table class="summary-section">
              <tr>
                <td class="summary-left">
                  <table class="tax-tbl">
                    <tr>
                      <td style="font-weight: bold; width: 60%;">Thuế TTĐB:</td>
                      <td class="align-center">%</td>
                    </tr>
                    <tr>
                      <td style="font-weight: bold;">Thuế suất GTGT:</td>
                      <td class="align-center">0%</td>
                    </tr>
                  </table>
                </td>
                <td class="summary-right">
                  <table class="sum-val-tbl">
                    <tr>
                      <td style="width: 55%;">Tiền thuế TTĐB:</td>
                      <td class="align-right">0đ</td>
                    </tr>
                    <tr>
                      <td>Cộng tiền hàng:</td>
                      <td class="align-right">${fmt(tongPhongDV)}đ</td>
                    </tr>
                    ${previewData.tienGiam > 0 ? `
                    <tr style="color: #dc2626;">
                      <td>Chiết khấu giảm giá:</td>
                      <td class="align-right">-${fmt(previewData.tienGiam)}đ</td>
                    </tr>
                    ` : ''}
                    ${previewData.tienCoc > 0 ? `
                    <tr>
                      <td>Đặt cọc khấu trừ:</td>
                      <td class="align-right">-${fmt(previewData.tienCoc)}đ</td>
                    </tr>
                    ` : ''}
                    <tr>
                      <td>Tiền thuế GTGT:</td>
                      <td class="align-right">0đ</td>
                    </tr>
                    <tr class="sum-total-row">
                      <td style="color: #1e50a0; font-size: 13px;">Tổng tiền thanh toán:</td>
                      <td class="align-right" style="color: #1e50a0; font-size: 14px;">${fmt(Math.max(0, totalAmount))}đ</td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>

            <!-- Words -->
            <div class="words-section">
              Số tiền viết bằng chữ: <strong>${words} đồng chẵn.</strong>
            </div>

            <!-- Signature Section -->
            <table class="signature-section">
              <tr>
                <td class="signature-cell">
                  <div class="signature-title">Người mua hàng</div>
                  <div class="signature-sub">(Chữ ký số (nếu có))</div>
                </td>
                <td class="signature-cell">
                  <div class="signature-title">Người bán hàng</div>
                  <div class="signature-sub">(Chữ ký điện tử, chữ ký số)</div>
                  <div class="sig-valid-box">
                    <div class="sig-valid-title">Signature Valid</div>
                    <div class="sig-valid-line">Ký bởi: Công ty Cổ phần OHNO</div>
                    <div class="sig-valid-line">Ký ngày: ${dateLabel}</div>
                  </div>
                </td>
              </tr>
            </table>

            <!-- Footer Retrieval -->
            <table class="footer-section">
              <tr>
                <td class="footer-cell">
                  <div>Tra cứu tại Website: <a href="https://ohno.vn/tra-cuu" class="footer-link">https://ohno.vn/tra-cuu</a> &nbsp;–&nbsp; Mã tra cứu: <strong style="text-transform: uppercase;">${invoiceNo}</strong></div>
                  <div style="font-size: 9px; margin-top: 3px; font-style: italic;">(Cần kiểm tra, đối chiếu khi lập, giao, nhận hóa đơn)</div>
                  <div style="font-size: 9px; margin-top: 2px; color: #888;">Phát hành bởi phần mềm quản lý khách sạn OHNO (www.ohno.vn) – MST: 0101243150</div>
                </td>
              </tr>
            </table>

          </div>
        </div>
        <script>
          window.onload = function() {
            window.print();
            setTimeout(function() { window.close(); }, 500);
          }
        </script>
      </body>
      </html>
    `;

    printWindow.document.write(htmlContent);
    printWindow.document.close();
  };

  // Print combined invoice (Gombill)
  const handlePrintGombill = () => {
    if (allStaysOfBooking.length === 0) return;
    const printWindow = window.open('', '_blank', 'width=850,height=800');
    if (!printWindow) return;

    let items = [];
    allStaysOfBooking.forEach(s => {
      const p = previewsMap[s.maChiTiet];
      if (!p) return;

      const prefix = `[P.${s.phong?.maPhong}] `;

      // Add Room charge
      items.push({
        noiDung: `${prefix}Tiền phòng (${p.soNgay} đêm)`,
        donVi: "Đêm",
        soLuong: p.soNgay,
        donGia: p.donGiaPhong,
        thanhTien: p.tienPhong
      });

      if (p.phuPhiCheckInEarly > 0) {
        items.push({
          noiDung: `${prefix}Phụ thu check-in sớm`,
          donVi: "Lần",
          soLuong: 1,
          donGia: p.phuPhiCheckInEarly,
          thanhTien: p.phuPhiCheckInEarly
        });
      }

      if (p.phuPhiCheckOutLate > 0) {
        items.push({
          noiDung: `${prefix}Phụ thu check-out trễ`,
          donVi: "Lần",
          soLuong: 1,
          donGia: p.phuPhiCheckOutLate,
          thanhTien: p.phuPhiCheckOutLate
        });
      }

      if (p.phuPhiKhac > 0) {
        items.push({
          noiDung: `${prefix}Phí phát sinh khác`,
          donVi: "Lần",
          soLuong: 1,
          donGia: p.phuPhiKhac,
          thanhTien: p.phuPhiKhac
        });
      }

      if (p.dsDichVu && p.dsDichVu.length > 0) {
        p.dsDichVu.forEach(dv => {
          items.push({
            noiDung: `${prefix}[Dịch vụ] ${dv.tenDichVu}`,
            donVi: dv.donViTinh || "Lần",
            soLuong: dv.soLuong,
            donGia: dv.donGia,
            thanhTien: dv.thanhTien
          });
        });
      }
    });

    // Minimum 7 rows
    const originalLen = items.length;
    for (let i = originalLen; i < 7; i++) {
      items.push({
        noiDung: "&nbsp;",
        donVi: "&nbsp;",
        soLuong: "&nbsp;",
        donGia: "",
        thanhTien: ""
      });
    }

    let itemsHtml = "";
    items.forEach((item, idx) => {
      const isDummy = item.noiDung === "&nbsp;";
      itemsHtml += `
        <tr>
          <td class="align-center">${isDummy ? "&nbsp;" : (idx + 1)}</td>
          <td class="align-left">${item.noiDung}</td>
          <td class="align-center">${item.donVi}</td>
          <td class="align-center">${item.soLuong}</td>
          <td class="align-right">${item.donGia !== "" ? fmt(item.donGia) + "đ" : "&nbsp;"}</td>
          <td class="align-right" style="${!isDummy ? 'font-weight: bold;' : ''}">${item.thanhTien !== "" ? fmt(item.thanhTien) + "đ" : "&nbsp;"}</td>
        </tr>
      `;
    });

    const now = new Date();
    const dateStr = `Ngày ${String(now.getDate()).padStart(2, '0')} tháng ${String(now.getMonth() + 1).padStart(2, '0')} năm ${now.getFullYear()}`;
    const dateLabel = `${String(now.getDate()).padStart(2, '0')}/${String(now.getMonth() + 1).padStart(2, '0')}/${now.getFullYear()}`;
    const customerName = stayDetail.datPhong?.khachHang?.hoTen || "Vãng lai";
    const phone = stayDetail.datPhong?.khachHang?.soDienThoai || "";
    const cccd = stayDetail.datPhong?.khachHang?.cccd || "";
    const roomList = allStaysOfBooking.map(s => "P." + s.phong?.maPhong).join(', ');
    const invoiceNo = `HD-GRP-${stayDetail.datPhong?.maDatPhong}`;
    const pthuc = paymentMethod === 'CASH' ? 'TM/CK' : paymentMethod === 'CARD' ? 'Thẻ ngân hàng' : paymentMethod === 'TRANSFER' ? 'Chuyển khoản' : 'Treo nợ';
    const qrData = `OHNO|${invoiceNo}|${now.toISOString().substring(0, 10)}|${gombillTotalPay}`;

    const words = convertToVietnameseWords(gombillTotalPay);

    const htmlContent = `
      <html>
      <head>
        <title>Hóa đơn gộp thanh toán - Đặt phòng ${stayDetail.datPhong?.maDatPhong}</title>
        <style>
          @page {
            size: A4 portrait;
            margin: 12mm 15mm;
          }
          body {
            font-family: 'Times New Roman', Times, serif, Arial;
            color: #1a1a1a;
            background-color: #fff;
            margin: 0;
            padding: 0;
            font-size: 13px;
            line-height: 1.4;
          }
          .outer-border {
            border: 2px solid #969696;
            padding: 3px;
            box-sizing: border-box;
            width: 100%;
            min-height: 270mm;
            display: flex;
            flex-direction: column;
          }
          .inner-container {
            border: 1px solid #969696;
            padding: 18px;
            box-sizing: border-box;
            flex: 1;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
          }
          .header-tbl {
            width: 100%;
            border-collapse: collapse;
            border-bottom: 1px solid #969696;
            margin-bottom: 10px;
          }
          .header-logo {
            width: 30%;
            text-align: center;
            border-right: 1px solid #969696;
            padding: 8px;
          }
          .header-logo-text {
            font-size: 26px;
            font-weight: bold;
            color: #1e50a0;
            line-height: 1.1;
            margin: 0;
          }
          .header-slogan {
            font-size: 8px;
            color: #666;
            margin-top: 5px;
            letter-spacing: 0.5px;
          }
          .header-info {
            padding: 8px 15px;
            text-align: left;
            vertical-align: top;
            font-size: 11px;
          }
          .company-title {
            font-size: 14px;
            font-weight: bold;
            margin: 0 0 5px 0;
          }
          .company-line {
            font-size: 11px;
            margin: 2px 0;
          }
          .company-label {
            font-weight: bold;
          }
          .title-section {
            width: 100%;
            border-collapse: collapse;
            border-bottom: 1px solid #969696;
            margin-bottom: 10px;
          }
          .title-cell {
            text-align: center;
            padding: 8px 0;
            vertical-align: middle;
          }
          .main-title {
            font-size: 18px;
            font-weight: bold;
            margin: 0;
            letter-spacing: 0.5px;
          }
          .sub-title-date {
            font-size: 12px;
            font-style: italic;
            margin: 4px 0 0 0;
          }
          .meta-cell {
            width: 30%;
            padding: 8px 10px;
            vertical-align: top;
            font-size: 11px;
          }
          .meta-line {
            margin: 3px 0;
          }
          .meta-label {
            font-weight: bold;
          }
          .customer-section {
            width: 100%;
            border-collapse: collapse;
            border-bottom: 1px solid #969696;
            margin-bottom: 12px;
          }
          .customer-cell {
            padding: 2px 8px 6px 0;
            vertical-align: middle;
          }
          .underline-field {
            border-bottom: 1px dotted #888;
            padding: 4px 0;
            font-size: 12px;
          }
          .qr-code-cell {
            width: 100px;
            text-align: right;
            vertical-align: middle;
            padding: 0 0 8px 10px;
          }
          .details-tbl {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 12px;
          }
          .details-tbl th {
            background-color: #DCDCDC;
            border: 1px solid #969696;
            padding: 6px 4px;
            font-size: 11px;
            font-weight: bold;
            text-align: center;
          }
          .details-tbl td {
            border: 1px solid #969696;
            padding: 6px 6px;
            font-size: 11px;
          }
          .align-center { text-align: center; }
          .align-left { text-align: left; }
          .align-right { text-align: right; }
          
          .summary-section {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 12px;
          }
          .summary-left {
            width: 50%;
            border: 1px solid #969696;
            vertical-align: top;
            padding: 0;
          }
          .summary-right {
            width: 50%;
            border: 1px solid #969696;
            vertical-align: top;
            padding: 0;
          }
          .tax-tbl, .sum-val-tbl {
            width: 100%;
            border-collapse: collapse;
          }
          .tax-tbl td {
            padding: 6px;
            border-bottom: 1px solid #969696;
            border-right: 1px solid #969696;
            font-size: 11px;
          }
          .tax-tbl tr:last-child td {
            border-bottom: none;
          }
          .tax-tbl td:last-child {
            border-right: none;
          }
          .sum-val-tbl td {
            padding: 6px;
            border-bottom: 1px solid #969696;
            border-right: 1px solid #969696;
            font-size: 11px;
          }
          .sum-val-tbl tr:last-child td {
            border-bottom: none;
          }
          .sum-val-tbl td:last-child {
            border-right: none;
          }
          .sum-total-row {
            font-weight: bold;
            font-size: 12px;
          }
          
          .words-section {
            font-style: italic;
            font-size: 12px;
            padding: 6px 0;
            border-bottom: 1px solid #969696;
            margin-bottom: 15px;
          }
          
          .signature-section {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 25px;
          }
          .signature-cell {
            width: 50%;
            text-align: center;
            vertical-align: top;
            padding: 10px;
          }
          .signature-title {
            font-weight: bold;
            font-size: 13px;
          }
          .signature-sub {
            font-size: 11px;
            font-style: italic;
            color: #555;
            margin-top: 2px;
          }
          .sig-valid-box {
            margin: 10px auto 0 auto;
            width: 180px;
            background-color: #C6EFC6;
            border: 1px solid #468250;
            border-radius: 4px;
            padding: 6px;
            text-align: center;
            font-family: Arial, sans-serif;
          }
          .sig-valid-title {
            font-weight: bold;
            color: #006400;
            font-size: 11px;
            margin-bottom: 2px;
          }
          .sig-valid-line {
            font-size: 9px;
            color: #005000;
            margin: 1px 0;
          }
          
          .footer-section {
            width: 100%;
            border-collapse: collapse;
            margin-top: auto;
          }
          .footer-cell {
            border-top: 1px solid #969696;
            padding-top: 8px;
            text-align: center;
            font-size: 11px;
            color: #666;
          }
          .footer-link {
            color: #1155CC;
            text-decoration: none;
            font-weight: bold;
          }
        </style>
      </head>
      <body>
        <div class="outer-border">
          <div class="inner-container">
            
            <!-- Company Info & Logo -->
            <table class="header-tbl">
              <tr>
                <td class="header-logo">
                  <div class="header-logo-text">OHNO</div>
                  <div class="header-logo-text" style="color: #4a5068;">HOTEL</div>
                  <div class="header-slogan">TÍN NHIỆM – TIỆN ÍCH – TẬN TÌNH</div>
                </td>
                <td class="header-info">
                  <div class="company-title">KHÁCH SẠN OHNO – CÔNG TY CỔ PHẦN OHNO</div>
                  <div class="company-line"><span class="company-label">Mã số thuế:</span> 0101243150-104</div>
                  <div class="company-line"><span class="company-label">Địa chỉ:</span> 12 Cao Lỗ, Phường 4, Quận 8, TP. Hồ Chí Minh</div>
                  <div class="company-line"><span class="company-label">Điện thoại:</span> (028) 1234 5678</div>
                  <div class="company-line"><span class="company-label">Số tài khoản:</span> 0123456789 – Ngân hàng Vietcombank</div>
                </td>
              </tr>
            </table>

            <!-- Title & Metadata -->
            <table class="title-section">
              <tr>
                <td class="title-cell">
                  <div class="main-title">HÓA ĐƠN THANH TOÁN GỘP (GOMBILL)</div>
                  <div class="sub-title-date">${dateStr}</div>
                </td>
                <td class="meta-cell">
                  <div class="meta-line"><span class="meta-label">Ký hiệu:</span> OHN-2026</div>
                  <div class="meta-line"><span class="meta-label">Số:</span> ${invoiceNo}</div>
                </td>
              </tr>
            </table>

            <!-- Customer & Stay Info -->
            <table class="customer-section">
              <tr>
                <td class="customer-cell">
                  <div class="underline-field">Họ tên người mua hàng (Đại diện): <strong>${customerName}</strong></div>
                  <div class="underline-field">Tên đơn vị: &nbsp;</div>
                  <div class="underline-field">Mã số thuế: &nbsp;</div>
                  <div class="underline-field">Địa chỉ: &nbsp;</div>
                  <table style="width: 100%; border-collapse: collapse;">
                    <tr>
                      <td style="width: 55%; padding: 4px 0 0 0;" class="underline-field">Hình thức thanh toán: <strong>${pthuc}</strong></td>
                      <td style="padding: 4px 0 0 0;" class="underline-field">Danh sách phòng: <strong>${roomList}</strong></td>
                    </tr>
                  </table>
                  <table style="width: 100%; border-collapse: collapse;">
                    <tr>
                      <td style="width: 70%; padding: 4px 0 0 0;" class="underline-field">Mã đặt phòng: <strong>${stayDetail.datPhong?.maDatPhong}</strong></td>
                      <td style="padding: 4px 0 0 0;" class="underline-field">Số tài khoản: &nbsp;</td>
                    </tr>
                  </table>
                  <div class="underline-field" style="border-bottom: none; padding-bottom: 0;">Đồng tiền thanh toán: <strong>VNĐ</strong></div>
                </td>
                <td class="qr-code-cell">
                  <img src="https://api.qrserver.com/v1/create-qr-code/?size=90x90&data=${encodeURIComponent(qrData)}" alt="QR" style="width: 85px; height: 85px; display: inline-block; border: 1px solid #ccc; padding: 2px;" />
                </td>
              </tr>
            </table>

            <!-- Details Table -->
            <table class="details-tbl">
              <thead>
                <tr>
                  <th style="width: 5%;">STT</th>
                  <th style="width: 48%;">Tên hàng hóa, dịch vụ</th>
                  <th style="width: 12%;">Đơn vị tính</th>
                  <th style="width: 10%;">Số lượng</th>
                  <th style="width: 12%;">Đơn giá</th>
                  <th style="width: 13%;">Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                ${itemsHtml}
              </tbody>
            </table>

            <!-- Summary Section -->
            <table class="summary-section">
              <tr>
                <td class="summary-left">
                  <table class="tax-tbl">
                    <tr>
                      <td style="font-weight: bold; width: 60%;">Thuế TTĐB:</td>
                      <td class="align-center">%</td>
                    </tr>
                    <tr>
                      <td style="font-weight: bold;">Thuế suất GTGT:</td>
                      <td class="align-center">0%</td>
                    </tr>
                  </table>
                </td>
                <td class="summary-right">
                  <table class="sum-val-tbl">
                    <tr>
                      <td style="width: 55%;">Tiền thuế TTĐB:</td>
                      <td class="align-right">0đ</td>
                    </tr>
                    <tr>
                      <td>Cộng tiền hàng:</td>
                      <td class="align-right">${fmt(gombillSubTotal)}đ</td>
                    </tr>
                    ${gombillTienGiam > 0 ? `
                    <tr style="color: #dc2626;">
                      <td>Chiết khấu giảm giá:</td>
                      <td class="align-right">-${fmt(gombillTienGiam)}đ</td>
                    </tr>
                    ` : ''}
                    ${gombillTienCoc > 0 ? `
                    <tr>
                      <td>Đặt cọc khấu trừ:</td>
                      <td class="align-right">-${fmt(gombillTienCoc)}đ</td>
                    </tr>
                    ` : ''}
                    <tr>
                      <td>Tiền thuế GTGT:</td>
                      <td class="align-right">0đ</td>
                    </tr>
                    <tr class="sum-total-row">
                      <td style="color: #1e50a0; font-size: 13px;">Tổng tiền thanh toán:</td>
                      <td class="align-right" style="color: #1e50a0; font-size: 14px;">${fmt(Math.max(0, gombillTotalPay))}đ</td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>

            <!-- Words -->
            <div class="words-section">
              Số tiền viết bằng chữ: <strong>${words} đồng chẵn.</strong>
            </div>

            <!-- Signature Section -->
            <table class="signature-section">
              <tr>
                <td class="signature-cell">
                  <div class="signature-title">Người mua hàng</div>
                  <div class="signature-sub">(Chữ ký số (nếu có))</div>
                </td>
                <td class="signature-cell">
                  <div class="signature-title">Người bán hàng</div>
                  <div class="signature-sub">(Chữ ký điện tử, chữ ký số)</div>
                  <div class="sig-valid-box">
                    <div class="sig-valid-title">Signature Valid</div>
                    <div class="sig-valid-line">Ký bởi: Công ty Cổ phần OHNO</div>
                    <div class="sig-valid-line">Ký ngày: ${dateLabel}</div>
                  </div>
                </td>
              </tr>
            </table>

            <!-- Footer Retrieval -->
            <table class="footer-section">
              <tr>
                <td class="footer-cell">
                  <div>Tra cứu tại Website: <a href="https://ohno.vn/tra-cuu" class="footer-link">https://ohno.vn/tra-cuu</a> &nbsp;–&nbsp; Mã tra cứu: <strong style="text-transform: uppercase;">${invoiceNo}</strong></div>
                  <div style="font-size: 9px; margin-top: 3px; font-style: italic;">(Cần kiểm tra, đối chiếu khi lập, giao, nhận hóa đơn)</div>
                  <div style="font-size: 9px; margin-top: 2px; color: #888;">Phát hành bởi phần mềm quản lý khách sạn OHNO (www.ohno.vn) – MST: 0101243150</div>
                </td>
              </tr>
            </table>

          </div>
        </div>
        <script>
          window.onload = function() {
            window.print();
            setTimeout(function() { window.close(); }, 500);
          }
        </script>
      </body>
      </html>
    `;

    printWindow.document.write(htmlContent);
    printWindow.document.close();
  };

  if (!isOpen) return null;

  // Multi-room calculation sum
  let gombillSubTotal = 0;
  let gombillTienPhong = 0;
  let gombillTongDV = 0;
  let gombillTienCoc = stayDetail?.datPhong?.tienDatCoc || 0;

  allStaysOfBooking.forEach(s => {
    const p = previewsMap[s.maChiTiet];
    if (p) {
      gombillTienPhong += p.tienPhong;
      gombillTongDV += p.tongDichVu + p.phuPhiCheckInEarly + p.phuPhiCheckOutLate + p.phuPhiKhac;
    }
  });

  gombillSubTotal = gombillTienPhong + gombillTongDV;
  const gombillTienGiam = voucherCode ? gombillSubTotal * 0.1 : 0;
  const gombillTotalPay = Math.max(0, gombillSubTotal - gombillTienGiam - gombillTienCoc);

  const totalAmount = checkoutMode === 'GOMBILL' ? gombillTotalPay : (previewData ? previewData.tongThanhToan : 0);
  const changeDue = Math.max(0, cashReceived - totalAmount);

  return (
    <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-panel flex flex-col" style={{ maxWidth: 768, maxHeight: '90vh', backgroundColor: '#FFFFFF', color: '#1E293B', padding: 0, overflow: 'hidden' }}>

        {/* Blue Header Banner */}
        <div className="bg-blue-600 text-white px-6 py-4 flex flex-col md:flex-row md:items-center md:justify-between relative select-none">
          <div>
            <h3 className="font-extrabold text-base uppercase tracking-wider">HÓA ĐƠN THANH TOÁN</h3>
            <p className="text-xs text-blue-100 font-semibold mt-0.5">
              P.{room?.maPhong} · {room?.loaiPhong?.tenLoaiPhong || 'Phòng'}
            </p>
          </div>
          <div className="text-left md:text-right mt-2 md:mt-0 text-xs">
            <div className="font-bold">Mã HĐ: HD-{new Date().toISOString().substring(0, 10).replace(/-/g, '')}-P{room?.maPhong}</div>
            <div className="text-blue-100 mt-0.5">Xuất: {new Date().toLocaleDateString('vi-VN')} {new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</div>
          </div>
          <button onClick={onClose} className="absolute right-4 top-4 text-blue-200 hover:text-white transition-colors">
            <X size={18} />
          </button>
        </div>

        {loading ? (
          <div className="p-20 text-center text-slate-500 font-semibold flex-1">Đang tải thông tin hóa đơn...</div>
        ) : !stayDetail ? (
          <div className="p-20 text-center text-slate-500 font-semibold flex-1">Không tìm thấy thông tin lượt thuê của phòng này.</div>
        ) : (
          <div className="p-6 overflow-y-auto space-y-4 flex-1 custom-scrollbar">

            {/* Top operational buttons (Transfer & Extend) */}
            <div className="flex gap-2">
              <button onClick={handleOpenTransfer}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-50 hover:bg-slate-100 text-slate-700 text-xs font-bold rounded-lg border border-slate-200 transition-all">
                <ArrowLeftRight size={13} /> Đổi Phòng
              </button>
              <button onClick={() => { setExtendForm({ ngayTraMoi: new Date(Date.now() + 24 * 3600 * 1000).toISOString().substring(0, 16) }); setShowExtend(true); }}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-50 hover:bg-slate-100 text-slate-700 text-xs font-bold rounded-lg border border-slate-200 transition-all">
                <Hourglass size={13} /> Gia Hạn
              </button>
            </div>

            {/* Toggle GOMBILL/SINGLE checkout if booking has multiple rooms */}
            {allStaysOfBooking.length > 1 && (
              <div className="flex bg-slate-100 p-1 rounded-xl border border-slate-200 gap-1">
                <button type="button" onClick={() => setCheckoutMode('SINGLE')}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-lg transition-all ${checkoutMode === 'SINGLE' ? 'bg-blue-600 text-white shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}>
                  Thanh toán riêng (P.{room?.maPhong})
                </button>
                <button type="button" onClick={() => setCheckoutMode('GOMBILL')}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-lg transition-all ${checkoutMode === 'GOMBILL' ? 'bg-blue-600 text-white shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}>
                  Thanh toán gộp (Gombill - {allStaysOfBooking.length} phòng)
                </button>
              </div>
            )}

            {/* Customer & Stay Info columns */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-slate-50 p-4 rounded-xl border border-slate-100 text-xs text-slate-500 font-semibold">
              <div>
                <h4 className="text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-2 border-b border-slate-200/60 pb-1">
                  {checkoutMode === 'GOMBILL' ? 'Khách hàng đại diện' : 'Khách hàng'}
                </h4>
                <div className="space-y-1.5">
                  <div className="text-slate-800 text-sm font-extrabold">{checkoutMode === 'GOMBILL' ? (stayDetail?.datPhong?.khachHang?.hoTen || 'Khách lẻ') : (stayDetail?.khachHang?.hoTen || 'Khách lẻ')}</div>
                  <div>SĐT: <span className="text-slate-700">{checkoutMode === 'GOMBILL' ? (stayDetail?.datPhong?.khachHang?.soDienThoai || '—') : (stayDetail?.khachHang?.soDienThoai || '—')}</span></div>
                  <div>CCCD: <span className="text-slate-700">{checkoutMode === 'GOMBILL' ? (stayDetail?.datPhong?.khachHang?.cccd || '—') : (stayDetail?.khachHang?.cccd || '—')}</span></div>
                </div>
              </div>
              <div>
                <h4 className="text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-2 border-b border-slate-200/60 pb-1">Chi tiết lưu trú</h4>
                {checkoutMode === 'GOMBILL' ? (
                  <div className="space-y-1.5">
                    <div>Số phòng: <strong className="text-blue-600">{allStaysOfBooking.map(s => s.phong?.maPhong).join(', ')}</strong></div>
                    <div>Ngày nhận phòng: <span className="text-slate-700">{new Date(stayDetail?.datPhong?.ngayNhanDuKien).toLocaleDateString('vi-VN')}</span></div>
                    <div>Ngày trả dự kiến: <span className="text-slate-700">{new Date(stayDetail?.datPhong?.ngayTraDuKien).toLocaleDateString('vi-VN')}</span></div>
                  </div>
                ) : (
                  <div className="space-y-1.5">
                    <div className="grid grid-cols-2 gap-x-2">
                      <div>Nhận phòng: <span className="text-slate-700">{stayDetail?.ngayNhanThucTe ? new Date(stayDetail.ngayNhanThucTe).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : '—'}</span></div>
                      <div>Trả phòng: <span className="text-slate-700">{new Date().toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' })}</span></div>
                    </div>
                    <div className="grid grid-cols-2 gap-x-2 mt-1">
                      <div>Số đêm: <strong className="text-blue-600">{previewData?.soNgay || 1} đêm</strong></div>
                      <div>Giá/đêm: <strong className="text-slate-800">{previewData ? fmt(previewData.donGiaPhong) : '0'}đ</strong></div>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Detailed Table */}
            <div className="border border-slate-200 rounded-xl overflow-hidden bg-white shadow-sm">
              <table className="w-full text-xs border-collapse">
                <thead>
                  <tr className="bg-slate-50 border-b border-slate-200 text-slate-500 font-extrabold uppercase text-[10px] tracking-wider select-none">
                    <th className="px-4 py-2.5 text-left">Khoản mục</th>
                    <th className="px-4 py-2.5 text-right w-28">Đơn giá</th>
                    <th className="px-4 py-2.5 text-center w-20">Số lượng</th>
                    <th className="px-4 py-2.5 text-right w-28">Thành tiền</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-slate-800 font-semibold">
                  {checkoutMode === 'GOMBILL' ? (
                    allStaysOfBooking.map(s => {
                      const p = previewsMap[s.maChiTiet];
                      if (!p) return null;
                      return (
                        <React.Fragment key={s.maChiTiet}>
                          <tr className="bg-slate-50/50">
                            <td colSpan={4} className="px-4 py-1.5 text-blue-600 font-extrabold text-[10px] uppercase tracking-wider">
                              Phòng P.{s.phong?.maPhong} ({s.phong?.loaiPhong?.tenLoaiPhong})
                            </td>
                          </tr>
                          <tr>
                            <td className="px-4 py-2 pl-6 text-slate-500">Tiền phòng P.{s.phong?.maPhong} ({p.soNgay} đêm)</td>
                            <td className="px-4 py-2 text-right">{fmt(p.donGiaPhong)}đ</td>
                            <td className="px-4 py-2 text-center">{p.soNgay} đêm</td>
                            <td className="px-4 py-2 text-right font-bold text-emerald-600">{fmt(p.tienPhong)}đ</td>
                          </tr>
                          {p.phuPhiCheckInEarly > 0 && (
                            <tr className="text-orange-600">
                              <td className="px-4 py-2 pl-6">Phụ thu check-in sớm (P.{s.phong?.maPhong})</td>
                              <td className="px-4 py-2 text-right">{fmt(p.phuPhiCheckInEarly)}đ</td>
                              <td className="px-4 py-2 text-center">1</td>
                              <td className="px-4 py-2 text-right font-bold">{fmt(p.phuPhiCheckInEarly)}đ</td>
                            </tr>
                          )}
                          {p.phuPhiCheckOutLate > 0 && (
                            <tr className="text-orange-600">
                              <td className="px-4 py-2 pl-6">Phụ thu check-out trễ (P.{s.phong?.maPhong})</td>
                              <td className="px-4 py-2 text-right">{fmt(p.phuPhiCheckOutLate)}đ</td>
                              <td className="px-4 py-2 text-center">1</td>
                              <td className="px-4 py-2 text-right font-bold">{fmt(p.phuPhiCheckOutLate)}đ</td>
                            </tr>
                          )}
                          {p.phuPhiKhac > 0 && (
                            <tr className="text-orange-600">
                              <td className="px-4 py-2 pl-6">Phí phát sinh khác (P.{s.phong?.maPhong})</td>
                              <td className="px-4 py-2 text-right">{fmt(p.phuPhiKhac)}đ</td>
                              <td className="px-4 py-2 text-center">1</td>
                              <td className="px-4 py-2 text-right font-bold">{fmt(p.phuPhiKhac)}đ</td>
                            </tr>
                          )}
                          {p.dsDichVu && p.dsDichVu.length > 0 && p.dsDichVu.map((dv, i) => (
                            <tr key={i} className="group/row">
                              <td className="px-4 py-2 pl-6 text-slate-500 flex items-center justify-between">
                                <span>[Dịch vụ] {dv.tenDichVu}</span>
                                <div className="opacity-0 group-hover/row:opacity-100 transition-opacity flex gap-1 select-none">
                                  <button type="button" onClick={() => handleEditService(dv.maSuDung, dv.soLuong)} className="text-[10px] text-blue-500 hover:underline px-1">Sửa</button>
                                  <button type="button" onClick={() => handleDeleteService(dv.maSuDung)} className="text-[10px] text-red-500 hover:underline px-1">Xóa</button>
                                </div>
                              </td>
                              <td className="px-4 py-2 text-right text-slate-500">{fmt(dv.donGia)}đ</td>
                              <td className="px-4 py-2 text-center text-slate-500">{dv.soLuong}</td>
                              <td className="px-4 py-2 text-right font-bold text-slate-500">{fmt(dv.thanhTien)}đ</td>
                            </tr>
                          ))}
                        </React.Fragment>
                      );
                    })
                  ) : (
                    <>
                      {previewData ? (
                        <>
                          <tr>
                            <td className="px-4 py-2.5 text-slate-600">Tiền phòng – Phòng P.{previewData.maPhong} ({previewData.soNgay} đêm)</td>
                            <td className="px-4 py-2.5 text-right">{fmt(previewData.donGiaPhong)}đ/đêm</td>
                            <td className="px-4 py-2.5 text-center">{previewData.soNgay} đêm</td>
                            <td className="px-4 py-2.5 text-right font-bold text-emerald-600">{fmt(previewData.tienPhong)}đ</td>
                          </tr>
                          {previewData.phuPhiCheckInEarly > 0 && (
                            <tr className="text-orange-600">
                              <td className="px-4 py-2.5">Phụ thu check-in sớm</td>
                              <td className="px-4 py-2.5 text-right">{fmt(previewData.phuPhiCheckInEarly)}đ</td>
                              <td className="px-4 py-2.5 text-center">1</td>
                              <td className="px-4 py-2.5 text-right font-bold">{fmt(previewData.phuPhiCheckInEarly)}đ</td>
                            </tr>
                          )}
                          {previewData.phuPhiCheckOutLate > 0 && (
                            <tr className="text-orange-600">
                              <td className="px-4 py-2.5">Phụ thu check-out trễ</td>
                              <td className="px-4 py-2.5 text-right">{fmt(previewData.phuPhiCheckOutLate)}đ</td>
                              <td className="px-4 py-2.5 text-center">1</td>
                              <td className="px-4 py-2.5 text-right font-bold">{fmt(previewData.phuPhiCheckOutLate)}đ</td>
                            </tr>
                          )}
                          {previewData.phuPhiKhac > 0 && (
                            <tr className="text-orange-600">
                              <td className="px-4 py-2.5">Phí phát sinh khác</td>
                              <td className="px-4 py-2.5 text-right">{fmt(previewData.phuPhiKhac)}đ</td>
                              <td className="px-4 py-2.5 text-center">1</td>
                              <td className="px-4 py-2.5 text-right font-bold">{fmt(previewData.phuPhiKhac)}đ</td>
                            </tr>
                          )}
                          {previewData.dsDichVu && previewData.dsDichVu.length > 0 && previewData.dsDichVu.map((dv, i) => (
                            <tr key={i} className="group/row hover:bg-slate-50/50">
                              <td className="px-4 py-2 text-slate-600 flex items-center justify-between">
                                <span>[Dịch vụ] {dv.tenDichVu}</span>
                                <div className="opacity-0 group-hover/row:opacity-100 transition-opacity flex gap-2 select-none">
                                  <button type="button" onClick={() => handleEditService(dv.maSuDung, dv.soLuong)} className="text-[10px] text-blue-500 hover:underline px-1 flex items-center gap-0.5"><Edit size={10} />Sửa</button>
                                  <button type="button" onClick={() => handleDeleteService(dv.maSuDung)} className="text-[10px] text-red-500 hover:underline px-1 flex items-center gap-0.5"><Trash2 size={10} />Xóa</button>
                                </div>
                              </td>
                              <td className="px-4 py-2 text-right text-slate-500">{fmt(dv.donGia)}đ</td>
                              <td className="px-4 py-2 text-center text-slate-500">{dv.soLuong}</td>
                              <td className="px-4 py-2 text-right font-bold text-slate-500">{fmt(dv.thanhTien)}đ</td>
                            </tr>
                          ))}
                        </>
                      ) : (
                        <tr>
                          <td colSpan={4} className="text-center py-6 text-slate-500">Đang tải chi tiết hóa đơn...</td>
                        </tr>
                      )}
                    </>
                  )}
                </tbody>
              </table>
            </div>

            {/* Note and Inline Add Service form */}
            {checkoutMode === 'SINGLE' && (
              <div className="space-y-3.5">
                <div className="text-[10px] text-slate-400 italic">
                  (*) Rê chuột vào dòng dịch vụ &rarr; hiện nút [Sửa] [Xóa] để điều chỉnh nhanh hóa đơn
                </div>

                <form onSubmit={handleInlineAddService} className="border border-slate-200 rounded-xl p-3.5 bg-slate-50/50 space-y-2 text-xs font-semibold">
                  <div className="font-extrabold uppercase text-[9px] text-slate-400 tracking-wider">THÊM DỊCH VỤ (nếu có)</div>
                  <div className="flex gap-2">
                    <select required value={addServiceForm.maDV} onChange={e => setAddServiceForm({ ...addServiceForm, maDV: e.target.value })}
                      className="flex-1 bg-white border border-slate-200 text-slate-800 rounded-lg px-2.5 py-1.5 focus:border-blue-500 outline-none">
                      <option value="">-- Chọn dịch vụ cần thêm --</option>
                      {services.map(s => <option key={s.maDichVu} value={s.maDichVu}>{s.tenDichVu} ({fmt(s.donGia)}đ/{s.donViTinh || 'lượt'})</option>)}
                    </select>
                    <div className="flex items-center gap-1.5 border border-slate-200 rounded-lg px-2 bg-white">
                      <span className="text-slate-400 text-[10px] font-bold uppercase">SL:</span>
                      <input type="number" min={1} value={addServiceForm.soLuong} onChange={e => setAddServiceForm({ ...addServiceForm, soLuong: Number(e.target.value) })}
                        className="w-10 bg-transparent text-slate-800 text-center focus:outline-none font-bold" />
                    </div>
                    <button type="submit" className="px-4 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white font-extrabold rounded-lg shadow-sm flex items-center gap-1 transition-all">
                      <Plus size={14} />Thêm
                    </button>
                  </div>
                </form>
              </div>
            )}

            {/* Surcharge details & Payment forms */}
            <div className="grid grid-cols-1 md:grid-cols-12 gap-5 pt-2">

              {/* Left Column: Payment selector */}
              <div className="md:col-span-7 space-y-3">
                <div className="bg-slate-50 p-4 rounded-xl border border-slate-100 space-y-3 font-semibold">
                  <div className="font-bold text-[10px] text-slate-400 uppercase tracking-wider">Hình thức thanh toán</div>

                  <div className="grid grid-cols-4 gap-2">
                    {[
                      { key: 'CASH', label: 'Tiền mặt' },
                      { key: 'CARD', label: 'Thẻ' },
                      { key: 'TRANSFER', label: 'Chuyển khoản' },
                      { key: 'DEBT', label: 'Treo nợ' }
                    ].map(m => (
                      <button key={m.key} type="button" onClick={() => setPaymentMethod(m.key)}
                        className={`py-2 text-xs font-bold rounded-lg border transition-all ${paymentMethod === m.key ? 'bg-blue-600 text-white border-blue-600 shadow-sm' : 'bg-white text-slate-600 border-slate-200 hover:border-blue-500'}`}>
                        {m.label}
                      </button>
                    ))}
                  </div>

                  {paymentMethod === 'CASH' && (
                    <div className="grid grid-cols-2 gap-3 text-xs pt-1">
                      <div>
                        <label className="text-[10px] text-slate-400 font-bold uppercase">Tiền khách đưa</label>
                        <input type="number" value={cashReceived} onChange={e => setCashReceived(Number(e.target.value))}
                          className="w-full bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 text-slate-800 focus:border-blue-500 outline-none font-bold mt-1" />
                      </div>
                      <div>
                        <label className="text-[10px] text-slate-400 font-bold uppercase">Tiền trả lại</label>
                        <div className="w-full bg-slate-100 border border-slate-200 rounded-lg px-2.5 py-1.5 text-blue-600 font-extrabold mt-1 text-sm">
                          {fmt(changeDue)}đ
                        </div>
                      </div>
                    </div>
                  )}

                  {paymentMethod === 'TRANSFER' && (
                    <div className="flex flex-col items-center p-2 bg-white rounded-lg border border-slate-200 space-y-1.5">
                      <div className="text-[10px] font-bold text-slate-400 uppercase">VietQR Chuyển Khoản</div>
                      <img
                        src={`https://img.vietqr.io/image/mbbank-0971234567-compact2.png?amount=${Math.max(0, totalAmount)}&addInfo=ThanhToanPhong${room?.maPhong}`}
                        alt="VietQR Payment"
                        className="w-32 h-32 rounded-lg border border-slate-200"
                      />
                      <div className="text-[9px] text-slate-500 text-center">
                        Quét mã QR để chuyển khoản nhanh số tiền <strong className="text-emerald-600">{fmt(Math.max(0, totalAmount))}đ</strong>
                      </div>
                    </div>
                  )}

                  {paymentMethod === 'DEBT' && (
                    <div className="p-3 bg-amber-500/10 border border-amber-500/20 text-amber-600 rounded-lg text-[10px] font-semibold">
                      Ghi chú: Lượt lưu trú này sẽ được đóng lại và số tiền chưa thanh toán sẽ được lưu dưới dạng nợ (Unpaid bill) để thanh toán sau.
                    </div>
                  )}
                </div>
              </div>

              {/* Right Column: Summaries */}
              <div className="md:col-span-5 bg-slate-50 p-4 rounded-xl border border-slate-100 flex flex-col justify-between font-semibold">
                <div className="space-y-2.5 text-xs text-slate-500">
                  <div className="flex justify-between">
                    <span>Tiền phòng:</span>
                    <span className="text-slate-800">{fmt(checkoutMode === 'GOMBILL' ? gombillTienPhong : (previewData?.tienPhong || 0))}đ</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Dịch vụ & phụ thu:</span>
                    <span className="text-slate-800">{fmt(checkoutMode === 'GOMBILL' ? gombillTongDV : (previewData ? (previewData.tongDichVu + previewData.phuPhiCheckInEarly + previewData.phuPhiCheckOutLate + previewData.phuPhiKhac) : 0))}đ</span>
                  </div>
                  {(checkoutMode === 'GOMBILL' ? gombillTienGiam : (previewData?.tienGiam || 0)) > 0 && (
                    <div className="flex justify-between text-red-500">
                      <span>Khuyến mãi giảm giá:</span>
                      <span>-{fmt(checkoutMode === 'GOMBILL' ? gombillTienGiam : previewData.tienGiam)}đ</span>
                    </div>
                  )}
                  {(checkoutMode === 'GOMBILL' ? gombillTienCoc : (previewData?.tienCoc || 0)) > 0 && (
                    <div className="flex justify-between">
                      <span>Tiền đặt cọc khấu trừ:</span>
                      <span>-{fmt(checkoutMode === 'GOMBILL' ? gombillTienCoc : previewData.tienCoc)}đ</span>
                    </div>
                  )}

                  <div className="border-t border-slate-200 pt-2.5 flex justify-between text-xs font-black text-slate-800">
                    <span>THỰC THANH TOÁN:</span>
                    <span className="text-emerald-600 text-base">{fmt(totalAmount)}đ</span>
                  </div>
                </div>
              </div>

            </div>

            {/* Footer Bottom Actions */}
            <div className="flex justify-between items-center border-t border-slate-200 pt-4 shrink-0 mt-4">
              <div className="flex items-center gap-2">
                <button type="button" onClick={checkoutMode === 'GOMBILL' ? handlePrintGombill : handlePrint}
                  className="px-4 py-2 bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 rounded-xl font-bold transition-all text-xs flex items-center gap-1.5 shadow-sm">
                  <Printer size={13} /> In hóa đơn {checkoutMode === 'GOMBILL' ? 'chung' : 'riêng'}
                </button>
                {checkoutMode === 'SINGLE' && (
                  <input type="text" placeholder="Nhập mã giảm giá..." value={voucherCode} onChange={e => setVoucherCode(e.target.value)}
                    className="bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs text-slate-800 focus:border-blue-500 outline-none w-28" />
                )}
              </div>

              <div className="flex gap-2">
                <button type="button" onClick={onClose}
                  className="px-4 py-2 bg-slate-50 hover:bg-slate-100 text-slate-500 rounded-xl font-bold transition-all text-xs border border-slate-200">
                  Hủy
                </button>
                <button type="button" onClick={handleCheckoutSubmit}
                  className="px-5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl font-extrabold transition-all text-xs shadow-lg shadow-emerald-600/20 flex items-center gap-1.5">
                  <CheckCircle2 size={13} /> Xác nhận trả phòng
                </button>
              </div>
            </div>

          </div>
        )}

      </div>

      {showTransfer && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setShowTransfer(false); }}>
          <div className="modal-panel" style={{ maxWidth: 384, padding: 20 }}>
            <h4 className="font-bold text-[var(--text-primary)] text-sm">Chuyển phòng nghỉ</h4>
            <form onSubmit={handleTransfer} className="space-y-3.5 text-xs font-semibold">
              <div>
                <label className="text-[10px] text-[var(--text-secondary)] uppercase">Phòng trống khả dụng</label>
                <select required value={transferForm.maPhongMoi} onChange={e => setTransferForm({ ...transferForm, maPhongMoi: e.target.value })}
                  className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-[var(--text-primary)] mt-1">
                  <option value="">-- Chọn phòng trống --</option>
                  {availableRooms.map(r => <option key={r.maPhong} value={r.maPhong}>P.{r.maPhong} ({r.loaiPhong?.tenLoaiPhong})</option>)}
                </select>
              </div>
              <div>
                <label className="flex items-center gap-2 cursor-pointer mt-2">
                  <input type="checkbox" checked={transferForm.giuNguyenGia} onChange={e => setTransferForm({ ...transferForm, giuNguyenGia: e.target.checked })}
                    className="w-4 h-4 text-blue-600 bg-[var(--bg-main)] border-[var(--border-color)] rounded focus:ring-blue-500" />
                  <span className="text-[var(--text-secondary)]">Giữ nguyên giá phòng cũ</span>
                </label>
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" onClick={() => setShowTransfer(false)} className="px-3.5 py-1.5 bg-[var(--bg-sidebar)] hover:bg-[var(--bg-main)] text-[var(--text-secondary)] rounded-lg">Hủy</button>
                <button type="submit" className="px-4 py-1.5 bg-blue-600 hover:bg-blue-500 text-white rounded-lg">Chuyển</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showExtend && (
        <div className="modal-backdrop" onClick={e => { if (e.target === e.currentTarget) setShowExtend(false); }}>
          <div className="modal-panel" style={{ maxWidth: 384, padding: 20 }}>
            <h4 className="font-bold text-[var(--text-primary)] text-sm">Gia hạn thời gian trả phòng</h4>
            <form onSubmit={handleExtend} className="space-y-3.5 text-xs font-semibold">
              <div>
                <label className="text-[10px] text-[var(--text-secondary)] uppercase">Ngày trả mới dự kiến</label>
                <input type="datetime-local" required value={extendForm.ngayTraMoi} onChange={e => setExtendForm({ ...extendForm, ngayTraMoi: e.target.value })}
                  className="w-full bg-[var(--input-bg)] border border-[var(--input-border)] rounded-xl px-3 py-2 text-[var(--text-primary)] mt-1" />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" onClick={() => setShowExtend(false)} className="px-3.5 py-1.5 bg-[var(--bg-sidebar)] hover:bg-[var(--bg-main)] text-[var(--text-secondary)] rounded-lg">Hủy</button>
                <button type="submit" className="px-4 py-1.5 bg-blue-600 hover:bg-blue-500 text-white rounded-lg">Gia Hạn</button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};
