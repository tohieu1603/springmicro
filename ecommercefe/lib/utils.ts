import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

/** Conditional class concatenation with tailwind-merge to drop duplicates. */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/** VND formatter — sticks to integer dong (no decimals shown in retail). */
export function formatVnd(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === "") return "";
  const n = typeof value === "string" ? Number(value) : value;
  if (!Number.isFinite(n)) return "";
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(n);
}

export function formatNumber(n: number): string {
  return new Intl.NumberFormat("vi-VN").format(n);
}

export function formatDate(iso: string | Date | null | undefined, withTime = false): string {
  if (iso == null || iso === "") return "";
  const d = typeof iso === "string" ? new Date(iso) : iso;
  // Treat any non-Date or invalid date as empty — fixes antd Table cell crashes
  // when BE returns null timestamps ("d.getTime is not a function").
  if (!(d instanceof Date) || !Number.isFinite(d.getTime())) return "";
  const opts: Intl.DateTimeFormatOptions = withTime
    ? { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" }
    : { day: "2-digit", month: "2-digit", year: "numeric" };
  return new Intl.DateTimeFormat("vi-VN", opts).format(d);
}

export function slugify(input: string): string {
  return input
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/đ/gi, "d")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/(^-|-$)/g, "")
    .slice(0, 128);
}

export function truncate(text: string, max = 80): string {
  return text.length <= max ? text : `${text.slice(0, max - 1)}…`;
}

/** Stable empty-array literal for default props to keep referential equality. */
export const EMPTY_ARRAY = Object.freeze([]) as never[];
