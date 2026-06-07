"use client";

import type { AppliedVoucher, PaymentMethod, ShippingQuote } from "../types";
import { priceVND } from "../utils/format";
import { VoucherInput } from "./VoucherInput";

interface OrderSummaryProps {
  userId?: string;
  subtotal: number;
  discount: number;
  shippingFee: number;
  total: number;
  itemCount: number;
  placing: boolean;
  onCheckout: () => void;

  // selected payment + shipping context
  paymentMethod: PaymentMethod["code"];
  paymentName: string;
  shipping: ShippingQuote | null;
  hasAddress: boolean;

  // voucher slot
  voucherCode: string;
  setVoucherCode: (v: string) => void;
  voucherChecking: boolean;
  applied: AppliedVoucher | null;
  voucherError: string | null;
  onApply: () => void;
  onClearVoucher: () => void;
}

export function OrderSummary(p: OrderSummaryProps) {
  return (
    <aside className="co-summary">
      <h3>ORDER SUMMARY</h3>
      {p.userId && <div className="order-id">USCART{p.userId.slice(0, 9).toUpperCase()}</div>}

      <div className="row">
        <span>Subtotal</span>
        <span>{priceVND(p.subtotal)}</span>
      </div>

      <VoucherInput
        code={p.voucherCode}
        setCode={p.setVoucherCode}
        checking={p.voucherChecking}
        applied={p.applied}
        error={p.voucherError}
        onApply={p.onApply}
        onClear={p.onClearVoucher}
      />

      {p.discount > 0 && (
        <div className="row">
          <span>Giảm giá</span>
          <span style={{ color: "#28a745" }}>−{priceVND(p.discount)}</span>
        </div>
      )}

      <div className="row">
        <span>Vận chuyển ({p.shipping?.carrier ?? "GHTK"})</span>
        <span style={{ fontSize: 12, color: p.shippingFee > 0 ? "#000" : "#888" }}>
          {!p.hasAddress
            ? "Chọn địa chỉ"
            : p.shipping
              ? priceVND(p.shippingFee)
              : "Đang tính..."}
        </span>
      </div>
      <div className="row">
        <span>Thanh toán</span>
        <span style={{ fontSize: 12, color: "#555" }}>{p.paymentName}</span>
      </div>
      <div className="row bold">
        <span>Tổng cộng</span>
        <span>{priceVND(p.total)}</span>
      </div>

      <p className="pay-info">
        {p.paymentMethod === "COD"
          ? "Bạn sẽ trả tiền mặt khi nhận hàng."
          : "Sau khi đặt đơn, bạn sẽ được chuyển sang trang thanh toán."}
      </p>

      <button
        className="checkout-btn"
        onClick={p.onCheckout}
        disabled={p.placing || p.itemCount === 0 || !p.hasAddress}
      >
        {p.placing ? "ĐANG XỬ LÝ…" : !p.hasAddress ? "VUI LÒNG CHỌN ĐỊA CHỈ" : "ĐẶT HÀNG"}
      </button>

      <div className="co-help">
        <h4>
          <u>MAY WE HELP?</u>
          <span style={{ fontSize: 18, fontWeight: 300 }}>−</span>
        </h4>
        <div className="co-help-row">
          <svg viewBox="0 0 24 24" style={{ width: 14, height: 14, stroke: "#000", fill: "none", strokeWidth: 1.4 }}>
            <path d="M5 4h3l2 5-2 1a11 11 0 006 6l1-2 5 2v3a2 2 0 01-2 2A16 16 0 013 6a2 2 0 012-2z" />
          </svg>
          <u>+1.877.482.2430</u>
        </div>
        <div className="co-help-row">
          <svg viewBox="0 0 24 24" style={{ width: 14, height: 14, stroke: "#000", fill: "none", strokeWidth: 1.4 }}>
            <path d="M3 6h18v12H3zM3 6l9 7 9-7" />
          </svg>
          <u>assistance@hieu.com</u>
        </div>
      </div>
    </aside>
  );
}
