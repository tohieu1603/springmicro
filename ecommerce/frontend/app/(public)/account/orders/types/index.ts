export type OrderTone = "success" | "danger" | "warning" | "accent" | "neutral" | "primary";

export const ORDER_STATUS_TONE: Record<string, OrderTone> = {
  PAID: "success",
  CONFIRMED: "success",
  SHIPPED: "primary",
  DELIVERED: "success",
  CANCELLED: "danger",
  FAILED: "danger",
  PAYMENT_PENDING: "warning",
  INVENTORY_RESERVED: "warning",
  PENDING: "warning",
};

export const ORDER_STATUS_LABEL: Record<string, string> = {
  PENDING: "Chờ xử lý",
  INVENTORY_RESERVED: "Đã giữ hàng",
  PAYMENT_PENDING: "Chờ thanh toán",
  PAID: "Đã thanh toán",
  CONFIRMED: "Đã xác nhận",
  SHIPPED: "Đang giao",
  DELIVERED: "Đã giao",
  CANCELLED: "Đã hủy",
  FAILED: "Thất bại",
};
