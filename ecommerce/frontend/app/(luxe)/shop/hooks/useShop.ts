"use client";

import { useEffect, useState } from "react";

import { HERO_SLOTS, TABS, toLuxeProduct } from "../utils/mapper";
import type { BeProduct, LuxeProduct } from "../types";

interface UseShopArgs {
  /** SSR-fetched products in BE shape. */
  initialItems: BeProduct[];
  initialTotal: number;
  /** Hero band index — derived from `?cat=` server-side. */
  catIdx: number;
}

/**
 * Client-side state for /shop. Tab + filter drawer interactivity only;
 * the product grid is seeded from RSC props and never re-fetches on the
 * client. URL-driven filters re-trigger the RSC fetch on navigation.
 */
export function useShop({ initialItems, initialTotal, catIdx }: UseShopArgs) {
  const hero = HERO_SLOTS[catIdx] ?? HERO_SLOTS[0];

  const [products, setProducts] = useState<LuxeProduct[]>(() => initialItems.map(toLuxeProduct));
  const [total, setTotal] = useState(initialTotal);
  const [activeTab, setActiveTab] = useState(0);
  const [filterOpen, setFilterOpen] = useState(false);

  // Re-seed on client-side nav between categories (RSC delivers new initial data).
  useEffect(() => {
    setProducts(initialItems.map(toLuxeProduct));
    setTotal(initialTotal);
  }, [initialItems, initialTotal]);

  return {
    hero,
    tabs: TABS,
    activeTab,
    setActiveTab,
    products,
    total,
    loaded: true,
    filterOpen,
    openFilter: () => setFilterOpen(true),
    closeFilter: () => setFilterOpen(false),
  };
}

export type ShopVM = ReturnType<typeof useShop>;
