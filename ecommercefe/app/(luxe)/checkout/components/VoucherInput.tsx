"use client";

import type { AppliedVoucher } from "../types";
import { priceVND } from "../utils/format";

interface VoucherInputProps {
  code: string;
  setCode: (v: string) => void;
  checking: boolean;
  applied: AppliedVoucher | null;
  error: string | null;
  onApply: () => void;
  onClear: () => void;
}

/** "Promo code" row in the order summary. */
export function VoucherInput({ code, setCode, checking, applied, error, onApply, onClear }: VoucherInputProps) {
  if (applied) {
    return (
      <div className="voucher-applied">
        <div className="vc-row">
          <span className="vc-code">{applied.voucher.code}</span>
          <span className="vc-amount">−{priceVND(applied.discountAmount)}</span>
        </div>
        {applied.voucher.description && <div className="vc-desc">{applied.voucher.description}</div>}
        <button type="button" className="vc-clear" onClick={onClear}>Bỏ áp dụng</button>
      </div>
    );
  }

  return (
    <div className="voucher-input">
      <div className="vc-row">
        <input
          type="text"
          placeholder="MÃ GIẢM GIÁ"
          value={code}
          onChange={(e) => setCode(e.target.value.toUpperCase())}
          onKeyDown={(e) => { if (e.key === "Enter") onApply(); }}
          disabled={checking}
        />
        <button type="button" onClick={onApply} disabled={checking || !code.trim()}>
          {checking ? "ĐANG KIỂM…" : "ÁP DỤNG"}
        </button>
      </div>
      {error && <div className="vc-error">{error}</div>}
    </div>
  );
}
