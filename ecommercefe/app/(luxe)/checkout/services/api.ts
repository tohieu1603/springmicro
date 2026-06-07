/** Data layer for the checkout page. */

import { api } from "@/lib/api/client";
import type { Address, CartDto, PaymentMethod, ShippingCarrier, ShippingQuote, Voucher } from "../types";

export class CheckoutApi {
  // ── Address book (user-profile-service) ─────────────────────────────
  static async listAddresses(): Promise<Address[]> {
    const res = await api.get<Address[] | { data: Address[] }>("/api/user-profiles/me/addresses");
    const body = res.data as { data?: Address[] };
    return body.data ?? (res.data as Address[]);
  }

  static async createAddress(addr: Omit<Address, "id" | "isDefault"> & { isDefault?: boolean }): Promise<Address> {
    const res = await api.post<Address | { data: Address }>("/api/user-profiles/me/addresses", addr);
    const body = res.data as { data?: Address };
    return (body.data ?? (res.data as Address));
  }

  // ── Shipping fee quote (shipping-service) ────────────────────────────
  static async listCarriers(): Promise<ShippingCarrier[]> {
    const res = await api.get<ShippingCarrier[] | { data: ShippingCarrier[] }>("/api/shipping/carriers");
    const body = res.data as { data?: ShippingCarrier[] };
    return body.data ?? (res.data as ShippingCarrier[]);
  }

  static async calculateShippingFee(payload: {
    province: string; district: string; ward: string; address: string;
    weightGrams: number; totalValue: number;
  }): Promise<ShippingQuote> {
    const res = await api.post<ShippingQuote | { data: ShippingQuote }>(
      "/api/shipping/calculate-fee",
      { ...payload, transport: "road" },
    );
    const body = res.data as { data?: ShippingQuote };
    return body.data ?? (res.data as ShippingQuote);
  }

  static async getCart(): Promise<CartDto> {
    const res = await api.get<CartDto>("/api/cart");
    return res.data;
  }

  static async updateQty(variantId: string, quantity: number): Promise<CartDto> {
    const res = await api.put<CartDto>(`/api/cart/items/${variantId}`, { quantity });
    return res.data;
  }

  static async removeItem(variantId: string): Promise<void> {
    await api.delete(`/api/cart/items/${variantId}`);
  }

  /** Empty the entire cart (DELETE /api/cart). */
  static async clearCart(): Promise<void> {
    await api.delete("/api/cart");
  }

  /**
   * Storefront-facing list of enabled payment methods. Returns whatever
   * payment-service exposes — keeps the FE provider catalog in sync with
   * yaml/DB instead of a hardcoded array in useCheckout.
   */
  static async listPaymentMethods(): Promise<PaymentMethod[]> {
    const res = await api.get<PaymentMethod[] | { data: PaymentMethod[] }>(
      "/api/payments/methods",
      { validateStatus: (s) => s < 500 },
    );
    if (res.status >= 400) return [];
    const body = res.data as { data?: PaymentMethod[] };
    return body.data ?? (res.data as PaymentMethod[]);
  }

  /**
   * Voucher preview: GET by code returns the raw voucher record. Client-side
   * we still need to check active / window / min-order locally because the
   * per-user usage limit only enforces on actual checkout.
   */
  static async previewVoucher(code: string): Promise<Voucher> {
    const res = await api.get<{ data?: Voucher } & Voucher>(`/api/vouchers/code/${encodeURIComponent(code)}`);
    // BE wraps in ApiResponse<T> envelope; unwrap if present.
    const body = res.data as { data?: Voucher } & Voucher;
    return (body.data ?? body) as Voucher;
  }

  static async placeOrder(payload: {
    recipientName: string;
    recipientPhone: string;
    street: string;
    ward?: string;
    district?: string;
    city: string;
    country: string;
    postalCode?: string;
    paymentMethod: string;
    notes?: string;
    voucherCode?: string;
  }): Promise<{ id: string; orderNumber: string; payUrl?: string; qrCodeUrl?: string }> {
    // order-service wraps every response in {success, data, message, ...} via
    // ApiResponseBodyAdvice, so the actual order lives at res.data.data.
    // Unwrap if envelope present; fall back to bare body for legacy callers.
    type OrderShape = { id: string; orderNumber: string; payUrl?: string; qrCodeUrl?: string };
    const res = await api.post<OrderShape | { data: OrderShape }>(
      "/api/orders/from-cart",
      payload,
    );
    const body = res.data as { data?: OrderShape };
    return (body.data ?? (res.data as OrderShape));
  }
}
