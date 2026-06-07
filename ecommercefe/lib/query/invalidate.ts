"use client";

import { useQueryClient } from "@tanstack/react-query";
import { qk } from "./keys";

/**
 * Convenience hook returning bound invalidators for the highest-frequency
 * server-state buckets. Callers don't need to know exact query keys —
 * they just describe what they changed:
 *
 *   const invalidate = useInvalidate();
 *   await api.post("/api/cart/items", payload);
 *   invalidate.cart();
 */
export function useInvalidate() {
  const qc = useQueryClient();
  return {
    cart:           () => qc.invalidateQueries({ queryKey: qk.cart() }),
    addresses:      () => qc.invalidateQueries({ queryKey: qk.addresses() }),
    orders:         () => qc.invalidateQueries({ queryKey: qk.orders.all() }),
    session:        () => qc.invalidateQueries({ queryKey: qk.session() }),
    notifications:  () => qc.invalidateQueries({ queryKey: ["notifications"] }),
    paymentMethods: () => qc.invalidateQueries({ queryKey: qk.paymentMethods() }),
    carriers:       () => qc.invalidateQueries({ queryKey: qk.shipping.carriers() }),
    banners:        () => qc.invalidateQueries({ queryKey: ["banners"] }),
    products:       () => qc.invalidateQueries({ queryKey: ["products"] }),
  };
}
