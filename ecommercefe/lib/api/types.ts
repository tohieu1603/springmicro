// Shared API contract types. Mirrors backend DTOs so SSR pages and CSR
// hooks consume the same shape; only what the FE actually renders is modelled.

export interface ApiResponse<T> {
  code?: string;
  message?: string;
  data: T;
}

export interface Page<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// Auth ---------------------------------------------------------------
export interface AuthUser {
  id: string;
  username: string;
  email: string;
  fullName?: string;
  roles: string[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

export interface LoginRequest { username: string; password: string }
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  fullName?: string;
}

// Catalog ------------------------------------------------------------
export interface Category {
  id: string;
  name: string;
  slug: string;
  description?: string;
  parentId?: string | null;
  children?: Category[];
  sortOrder?: number;
}

export interface AttrValue {
  id: string;
  code: string;
  val: string;
}

export interface Attr {
  id: string;
  code: string;
  name: string;
  type: "SELECT" | "TEXT" | "NUMBER";
  values: AttrValue[];
}

/**
 * Variant attribute as returned by catalog-service. The "value" side ships
 * as {valId, valText} — keep both legacy field names (`attrValId`, `val`) for
 * code that still references them.
 */
export interface VariantAttr {
  attrId: string;
  attrCode: string;
  attrName: string;
  valId?: string | null;
  valText: string;
  // Aliases (writes from product-form set these; reads from BE include the canon).
  attrValId?: string | null;
  val?: string;
}

export interface Variant {
  id: string;
  sku: string;
  price: string;
  salePrice?: string | null;
  cost?: string | null;
  image?: string | null;
  weight?: number | null;
  quantity: number;
  attrs: VariantAttr[];
}

export type ProductStatus = "DRAFT" | "ACTIVE" | "ARCHIVED" | "DELETED";

export interface Product {
  id: string;
  name: string;
  slug: string;
  description?: string;
  brand?: string;
  categoryId?: string | null;
  thumbnail?: string;
  images?: string[];
  metaTitle?: string;
  metaDescription?: string;
  metaKeywords?: string;
  status: ProductStatus;
  variants: Variant[];
  createdAt?: string;
  updatedAt?: string;
}

// Cart ---------------------------------------------------------------
export interface CartItem {
  id?: string;
  productId: string;
  productName: string;
  variantId: string;
  variantSku: string;
  variantImage?: string;
  unitPrice: string;
  quantity: number;
}

export interface Cart {
  userId: string;
  items: CartItem[];
  subtotal: string;
}

// Order --------------------------------------------------------------
export type OrderStatus =
  | "PENDING"
  | "INVENTORY_RESERVED"
  | "PAYMENT_PENDING"
  | "PAID"
  | "CONFIRMED"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED"
  | "FAILED";

export interface OrderItem {
  productId: string;
  productName: string;
  variantId: string;
  variantSku: string;
  variantImage?: string;
  unitPrice: string;
  quantity: number;
}

export interface Order {
  id: string;
  orderNumber: string;
  userId: string;
  status: OrderStatus;
  subtotalAmount: string;
  discountAmount?: string;
  shippingFee?: string;
  totalAmount: string;
  paymentMethod: string;
  voucherCode?: string;
  recipientName: string;
  recipientPhone: string;
  // BE OrderDTO ships address fields flat (street/ward/district/city/country/postalCode)
  // — not nested under shippingAddress. Components must use these top-level fields.
  street?: string;
  ward?: string;
  district?: string;
  city?: string;
  country?: string;
  postalCode?: string;
  notes?: string;
  items: OrderItem[];
  payUrl?: string;
  qrCodeUrl?: string;
  createdAt: string;
}

// Voucher ------------------------------------------------------------
export interface Voucher {
  id: string;
  code: string;
  type: "PERCENTAGE" | "FIXED_AMOUNT";
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
