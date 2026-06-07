"use client";

import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import type { Cart } from "@/lib/api/types";
import { qk } from "@/lib/query/keys";

/**
 * Counter overlay for the cart icon in the header. Backed by the shared
 * cart query — any add/remove/clear in the app calls
 * {@code queryClient.invalidateQueries({queryKey: qk.cart()})} and the
 * badge updates automatically without manual events.
 *
 * Returns null on 401 / 5xx / empty cart so the header stays clean.
 */
export function CartBadge() {
  const { data } = useQuery({
    queryKey: qk.cart(),
    queryFn: async () => {
      const res = await api.get<Cart | { data: Cart }>("/api/cart", {
        validateStatus: (s) => s < 500,
      });
      if (res.status >= 400) return null;
      const body = res.data as { data?: Cart };
      return (body?.data ?? (res.data as Cart)) || null;
    },
    staleTime: 10_000,
    retry: 0,
  });

  const count = (data?.items ?? []).reduce((s, i) => s + i.quantity, 0);
  if (!count) return null;
  return (
    <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] rounded-full bg-accent text-white text-[10px] font-bold flex items-center justify-center px-1">
      {count > 99 ? "99+" : count}
    </span>
  );
}
