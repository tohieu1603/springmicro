"use client";

import { useCallback, useEffect, useState } from "react";
import type { Product } from "@/lib/api/types";

import { WishlistApi } from "../services/api";

export function useWishlist() {
  const [items, setItems] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      setItems(await WishlistApi.list());
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  return { items, loading, reload: load };
}

export type WishlistVM = ReturnType<typeof useWishlist>;
