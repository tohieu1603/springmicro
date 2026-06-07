import type { Voucher } from "../types";

export function priceVND(v?: number | string | null): string {
  if (v == null) return "";
  const n = typeof v === "string" ? Number(v) : v;
  return Number.isFinite(n)
    ? new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(n)
    : "";
}

/**
 * Compute the discount a voucher would yield against an order subtotal.
 * Throws a string reason when the voucher cannot be applied so the caller
 * can show a precise toast.
 */
export function computeDiscount(voucher: Voucher, subtotal: number): number {
  if (!voucher.active) throw "Mã giảm giá không còn hoạt động";

  const now = Date.now();
  if (voucher.startDate && new Date(voucher.startDate).getTime() > now) {
    throw "Mã giảm giá chưa đến hạn sử dụng";
  }
  if (voucher.endDate && new Date(voucher.endDate).getTime() < now) {
    throw "Mã giảm giá đã hết hạn";
  }
  if (voucher.usageLimit != null && voucher.usedCount >= voucher.usageLimit) {
    throw "Mã giảm giá đã hết lượt sử dụng";
  }
  const min = Number(voucher.minOrderAmount ?? 0);
  if (min > 0 && subtotal < min) {
    throw `Đơn tối thiểu ${priceVND(min)} để áp dụng mã này`;
  }

  const value = Number(voucher.discountValue);
  if (!Number.isFinite(value) || value <= 0) throw "Mã giảm giá không hợp lệ";

  let discount = voucher.type === "PERCENTAGE" ? subtotal * (value / 100) : value;
  const cap = voucher.maxDiscountAmount ? Number(voucher.maxDiscountAmount) : null;
  if (cap != null && cap > 0 && discount > cap) discount = cap;
  return Math.min(discount, subtotal);
}
