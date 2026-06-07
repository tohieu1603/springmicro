"use client";

import { useProductDetail } from "./hooks/useProductDetail";
import { Hero, Info, Related } from "./components";
import type { BeProduct } from "./types";

interface ProductDetailClientProps {
  initialProduct: BeProduct;
  initialRelated: BeProduct[];
}

/**
 * Client-side shell. Seeded from RSC props — no first-load fetch, no loading
 * skeleton. Owns all interactive state (variant picker, qty, gallery pager).
 */
export function ProductDetailClient({ initialProduct, initialRelated }: ProductDetailClientProps) {
  const vm = useProductDetail({ initialProduct, initialRelated });

  return (
    <>
      <Hero
        gallery={vm.gallery}
        productId={vm.product.id}
        galleryIdx={vm.galleryIdx}
        setGalleryIdx={vm.setGalleryIdx}
      />

      <Info
        product={vm.product}
        displayPrice={vm.displayPrice}
        groups={vm.groups}
        picked={vm.picked}
        selVariant={vm.selVariant}
        fullySelected={vm.fullySelected}
        qty={vm.qty}
        qtyWarn={vm.qtyWarn}
        maxQty={vm.maxQty}
        stockLabel={vm.stockLabel}
        adding={vm.adding}
        onPick={vm.pick}
        onQtyChange={vm.changeQty}
        isReachable={vm.isReachable}
        onAddToBag={vm.addToBag}
      />

      <Related items={vm.related} idx={vm.relIdx} setIdx={vm.setRelIdx} />
    </>
  );
}
