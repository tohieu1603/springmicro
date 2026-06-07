/** Cart + voucher view-model types for the checkout page. */

export interface CartItem {
  id: string;
  productId: string;
  productName: string;
  variantId: string;
  variantSku: string;
  variantImage?: string | null;
  unitPrice: string;
  quantity: number;
  subtotal?: string;
  warning?: string | null;
}

export interface CartDto {
  userId: string;
  items: CartItem[];
  totalItems: number;
  totalAmount: string;
  warnings?: string[];
}

export type VoucherType = "PERCENTAGE" | "FIXED_AMOUNT";

export interface Voucher {
  id: string;
  code: string;
  type: VoucherType;
  discountValue: string;
  minOrderAmount?: string;
  maxDiscountAmount?: string;
  usageLimit?: number;
  usedCount: number;
  startDate?: string;
  endDate?: string;
  active: boolean;
  description?: string;
}

export interface AppliedVoucher {
  voucher: Voucher;
  discountAmount: number;
}

export type CheckoutPhase = "idle" | "loading" | "auth" | "ready" | "empty";

/** Subset of the user-profile-service address — only what the checkout cares about. */
export interface Address {
  id: string;
  recipientName: string;
  recipientPhone: string;
  street: string;
  ward?: string;
  district?: string;
  city: string;
  country: string;
  isDefault: boolean;
}

/** Card we render for each payment method. icon is a Material Symbols name. */
export interface PaymentMethod {
  code: "COD" | "SEPAY" | "MOMO";
  name: string;
  description: string;
  icon: string;
  enabled: boolean;
}

/** GHTK / GHN / etc. shown in the shipping picker. */
export interface ShippingCarrier {
  code: string;
  name: string;
  enabled: boolean;
  supportsCod: boolean;
  etaHours: number;
}

export interface ShippingQuote {
  carrier: string;
  fee: number;
  insuranceFee: number;
  deliveryTimeHours: number;
  source: "GHTK_LIVE" | "LOCAL_FALLBACK";
}
