"use client";

import FilterDrawer from "@/components/luxe/FilterDrawer";

import { useShop } from "./hooks/useShop";
import {
  CategoryHero,
  CategoryTabs,
  ProductGrid,
  SeoBlock,
  Toolbar,
} from "./components";
import type { BeProduct } from "./types";

interface ShopClientProps {
  initialItems: BeProduct[];
  initialTotal: number;
  catIdx: number;
}

/**
 * /shop client shell. Seeded from RSC props — no first-load fetch. Owns tab
 * + filter drawer interactivity only.
 */
export function ShopClient({ initialItems, initialTotal, catIdx }: ShopClientProps) {
  const vm = useShop({ initialItems, initialTotal, catIdx });
  return (
    <>
      <CategoryHero hero={vm.hero} />

      <section className="cat-section">
        <CategoryTabs tabs={vm.tabs} activeTab={vm.activeTab} setActiveTab={vm.setActiveTab} />
        <Toolbar total={vm.total} loaded={vm.loaded} onOpenFilter={vm.openFilter} />
        <ProductGrid loaded={vm.loaded} products={vm.products} />
      </section>

      <SeoBlock />

      {vm.filterOpen && <FilterDrawer onClose={vm.closeFilter} />}
    </>
  );
}
