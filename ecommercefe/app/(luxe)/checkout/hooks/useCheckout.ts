"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { CheckoutApi } from "../services/api";
import { computeDiscount } from "../utils/format";
import { qk } from "@/lib/query/keys";
import type {
  Address,
  AppliedVoucher,
  CartDto,
  CheckoutPhase,
  PaymentMethod,
  ShippingQuote,
  Voucher,
} from "../types";

/** Used while /api/payments/methods is loading or BE is unreachable. */
const FALLBACK_PAYMENT_METHODS: PaymentMethod[] = [
  { code: "COD",   name: "Thanh toán khi nhận hàng (COD)", description: "Trả tiền mặt khi nhận hàng", icon: "payments",       enabled: true },
  { code: "SEPAY", name: "Chuyển khoản ngân hàng (SePay)", description: "Quét QR/VietQR — tự xác nhận khi tiền về", icon: "qr_code_2", enabled: true },
  { code: "MOMO",  name: "Ví MoMo",                       description: "Thanh toán qua ví MoMo",     icon: "account_balance_wallet", enabled: true },
];

/**
 * Checkout God Hook — now backed by TanStack Query. The cart / addresses /
 * payment-methods queries are the source of truth; mutations (qty change,
 * remove, clear, apply voucher, place order) invalidate the relevant keys so
 * every consumer (cart badge, overlay, checkout page) re-renders with fresh
 * data without manual events.
 *
 * Local state still owns transient pickers — selected address, picked
 * payment method, voucher input draft, in-flight voucher result. Those are
 * UI state, not server state, so they live outside the query cache.
 */
export function useCheckout() {
  const router = useRouter();
  const qc = useQueryClient();

  // ─── server queries (cart / addresses / payment methods) ────────
  const cartQ = useQuery({
    queryKey: qk.cart(),
    queryFn: () => CheckoutApi.getCart(),
    staleTime: 10_000,
    retry: 0,
  });
  const cart = cartQ.data ?? null;

  const phase: CheckoutPhase = (() => {
    if (cartQ.isLoading) return "loading";
    const err = cartQ.error as { response?: { status?: number } } | null;
    if (err?.response?.status === 401) return "auth";
    if (!cart) return "idle";
    return cart.items.length === 0 ? "empty" : "ready";
  })();

  const addressesQ = useQuery({
    queryKey: qk.addresses(),
    queryFn: () => CheckoutApi.listAddresses(),
    enabled: phase === "ready",
    staleTime: 60_000,
    retry: 0,
  });
  const addresses: Address[] = addressesQ.data ?? [];

  const paymentMethodsQ = useQuery({
    queryKey: qk.paymentMethods(),
    queryFn: async () => {
      const list = await CheckoutApi.listPaymentMethods();
      return list.length > 0 ? list : FALLBACK_PAYMENT_METHODS;
    },
    staleTime: 60_000,
    placeholderData: FALLBACK_PAYMENT_METHODS,
    retry: 0,
  });
  const paymentMethods = paymentMethodsQ.data ?? FALLBACK_PAYMENT_METHODS;

  // ─── local UI state (transient) ─────────────────────────────────
  const [selectedAddressId, setSelectedAddressId] = useState<string | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod["code"]>("COD");
  const [voucherCode, setVoucherCode] = useState("");
  const [applied, setApplied] = useState<AppliedVoucher | null>(null);
  const [voucherError, setVoucherError] = useState<string | null>(null);

  // Auto-pick default address (or first) when the list arrives.
  useEffect(() => {
    if (addresses.length === 0 || selectedAddressId != null) return;
    const def = addresses.find((a) => a.isDefault) ?? addresses[0];
    setSelectedAddressId(def.id);
  }, [addresses, selectedAddressId]);

  // If the user-picked code disappears from the catalog (admin disabled it),
  // fall back to the first available one.
  useEffect(() => {
    if (paymentMethods.length === 0) return;
    if (!paymentMethods.some((m) => m.code === paymentMethod)) {
      setPaymentMethod(paymentMethods[0].code);
    }
  }, [paymentMethods, paymentMethod]);

  const selectedAddress = useMemo(
    () => addresses.find((a) => a.id === selectedAddressId) ?? null,
    [addresses, selectedAddressId],
  );

  // ─── live shipping fee query (depends on selectedAddress + cart) ─
  const weightGrams = (cart?.items ?? []).reduce((s, i) => s + 500 * i.quantity, 0);
  const totalValue = Number(cart?.totalAmount) || 0;
  const addrKey = selectedAddress
    ? `${selectedAddress.city}|${selectedAddress.district}|${selectedAddress.ward}|${selectedAddress.street}`
    : "";

  const shippingQ = useQuery({
    queryKey: qk.shipping.fee(addrKey, weightGrams, totalValue),
    queryFn: () => CheckoutApi.calculateShippingFee({
      province: selectedAddress!.city,
      district: selectedAddress!.district ?? "",
      ward: selectedAddress!.ward ?? "",
      address: selectedAddress!.street,
      weightGrams,
      totalValue,
    }),
    enabled: !!selectedAddress && (cart?.items?.length ?? 0) > 0,
    staleTime: 60_000,
    retry: 0,
  });
  const shipping: ShippingQuote | null = shippingQ.data ?? null;
  const shippingLoading = shippingQ.isFetching;

  // ─── voucher revalidation when cart total changes ───────────────
  useEffect(() => {
    if (!applied || !cart) return;
    try {
      const disc = computeDiscount(applied.voucher, Number(cart.totalAmount));
      if (disc !== applied.discountAmount) {
        setApplied({ voucher: applied.voucher, discountAmount: disc });
      }
      setVoucherError(null);
    } catch (reason) {
      setApplied(null);
      setVoucherError(String(reason));
      toast.warning(String(reason));
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cart?.totalAmount]);

  // ─── line mutations ─────────────────────────────────────────────
  const [busyVariant, setBusyVariant] = useState<string | null>(null);

  const changeQtyMut = useMutation({
    mutationFn: ({ variantId, qty }: { variantId: string; qty: number }) =>
      CheckoutApi.updateQty(variantId, qty),
    onMutate: ({ variantId }) => setBusyVariant(variantId),
    onSuccess: (fresh) => {
      qc.setQueryData(qk.cart(), fresh);
    },
    onError: (e: unknown) => {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message ?? "Không cập nhật được số lượng");
      qc.invalidateQueries({ queryKey: qk.cart() });
    },
    onSettled: () => setBusyVariant(null),
  });
  const changeQty = useCallback((variantId: string, qty: number) => {
    if (!Number.isFinite(qty) || qty < 0 || qty > 999) {
      toast.error("Số lượng không hợp lệ"); return;
    }
    changeQtyMut.mutate({ variantId, qty });
  }, [changeQtyMut]);

  const removeMut = useMutation({
    mutationFn: (variantId: string) => CheckoutApi.removeItem(variantId),
    onMutate: (variantId) => setBusyVariant(variantId),
    onSuccess: () => {
      toast.success("Đã xoá khỏi giỏ");
      qc.invalidateQueries({ queryKey: qk.cart() });
    },
    onError: () => toast.error("Không xoá được"),
    onSettled: () => setBusyVariant(null),
  });
  const remove = useCallback((variantId: string) => removeMut.mutate(variantId), [removeMut]);

  const clearAllMut = useMutation({
    mutationFn: () => CheckoutApi.clearCart(),
    onSuccess: () => {
      toast.success("Đã xoá toàn bộ giỏ hàng");
      setApplied(null);
      qc.invalidateQueries({ queryKey: qk.cart() });
    },
    onError: () => toast.error("Không xoá được giỏ hàng"),
  });
  const clearAll = useCallback(() => {
    if (!cart || cart.items.length === 0) return;
    if (!window.confirm(`Xoá toàn bộ ${cart.items.length} sản phẩm khỏi giỏ?`)) return;
    clearAllMut.mutate();
  }, [cart, clearAllMut]);

  // ─── voucher ────────────────────────────────────────────────────
  const applyVoucherMut = useMutation({
    mutationFn: async (code: string): Promise<Voucher> => CheckoutApi.previewVoucher(code),
  });
  const applyVoucher = useCallback(async () => {
    const code = voucherCode.trim().toUpperCase();
    if (!code) { toast.error("Vui lòng nhập mã"); return; }
    if (!cart || Number(cart.totalAmount) <= 0) {
      toast.error("Giỏ hàng trống — không thể áp mã"); return;
    }
    setVoucherError(null);
    try {
      const v = await applyVoucherMut.mutateAsync(code);
      const disc = computeDiscount(v, Number(cart.totalAmount));
      setApplied({ voucher: v, discountAmount: disc });
      toast.success(`Đã áp dụng mã ${v.code}`);
    } catch (e: unknown) {
      if (typeof e === "string") { setVoucherError(e); toast.error(e); }
      else {
        const err = e as { response?: { status?: number } };
        const msg = err.response?.status === 404
          ? "Mã giảm giá không tồn tại"
          : "Không kiểm tra được mã giảm giá";
        setVoucherError(msg); toast.error(msg);
      }
      setApplied(null);
    }
  }, [voucherCode, cart, applyVoucherMut]);

  const clearVoucher = useCallback(() => {
    setApplied(null); setVoucherCode(""); setVoucherError(null);
  }, []);

  // ─── totals ─────────────────────────────────────────────────────
  const subtotal = useMemo(() => (cart ? Number(cart.totalAmount) : 0), [cart]);
  const discount = applied?.discountAmount ?? 0;
  const shippingFee = shipping?.fee ?? 0;
  const total = Math.max(0, subtotal - discount + shippingFee);

  // ─── place order ────────────────────────────────────────────────
  const placeOrderMut = useMutation({
    mutationFn: () => {
      if (!selectedAddress) throw new Error("Vui lòng chọn địa chỉ giao hàng");
      return CheckoutApi.placeOrder({
        recipientName:  selectedAddress.recipientName,
        recipientPhone: selectedAddress.recipientPhone,
        street:         selectedAddress.street,
        ward:           selectedAddress.ward,
        district:       selectedAddress.district,
        city:           selectedAddress.city,
        country:        selectedAddress.country || "VN",
        paymentMethod,
        voucherCode:    applied?.voucher.code,
        notes:          shipping
          ? `Phí vận chuyển ${shipping.carrier}: ${shipping.fee} VND (${shipping.source})`
          : undefined,
      });
    },
    onSuccess: (order) => {
      toast.success(`Đã đặt đơn ${order.orderNumber}`);
      qc.invalidateQueries({ queryKey: qk.cart() });
      qc.invalidateQueries({ queryKey: qk.orders.all() });
      if (order.qrCodeUrl) router.push(`/payment/${encodeURIComponent(order.orderNumber)}`);
      else if (order.payUrl) window.location.href = order.payUrl;
      else router.push(`/track?o=${order.orderNumber}`);
    },
    onError: (e: unknown) => {
      const err = e as { response?: { status?: number; data?: { message?: string } }; message?: string };
      toast.error(err.response?.data?.message ?? err.message ?? "Đặt hàng thất bại");
      if (err.response?.status === 409) qc.invalidateQueries({ queryKey: qk.cart() });
    },
  });
  const placeOrder = useCallback(() => {
    if (!cart || cart.items.length === 0) return;
    placeOrderMut.mutate();
  }, [cart, placeOrderMut]);

  const reloadAddresses = useCallback(async (preferId?: string) => {
    await qc.invalidateQueries({ queryKey: qk.addresses() });
    if (preferId) setSelectedAddressId(preferId);
  }, [qc]);

  return {
    // state
    phase, cart, busyVariant,
    placing: placeOrderMut.isPending,
    addresses, selectedAddressId, setSelectedAddressId,
    paymentMethod, setPaymentMethod, paymentMethods,
    shipping, shippingLoading,
    voucherCode, setVoucherCode,
    voucherChecking: applyVoucherMut.isPending,
    applied, voucherError,

    // derived
    subtotal, discount, shippingFee, total, selectedAddress,

    // actions
    changeQty, remove, clearAll, applyVoucher, clearVoucher, placeOrder,
    reload: () => qc.invalidateQueries({ queryKey: qk.cart() }),
    reloadAddresses,
  };
}

export type CheckoutVM = ReturnType<typeof useCheckout>;
