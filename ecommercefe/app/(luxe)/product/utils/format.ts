/** Money + small text helpers used by detail page components. */

export function priceVND(v?: number | string | null): string {
  if (v == null) return "";
  const n = typeof v === "string" ? Number(v) : v;
  return Number.isFinite(n)
    ? new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(n)
    : "";
}

export function priceRange(min: number, max: number): string {
  if (min === max) return priceVND(min);
  return `${priceVND(min)} – ${priceVND(max)}`;
}
