"use client";

import { useCallback, useEffect, useState } from "react";
import useEmblaCarousel from "embla-carousel-react";
import Autoplay from "embla-carousel-autoplay";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { ProductCard } from "@/components/ui/product-card";
import type { Product } from "@/lib/api/types";

/**
 * Embla-backed horizontal carousel of ProductCards. Autoplay on a slow cadence
 * pauses when the user hovers / interacts — Embla handles that automatically.
 */
export function HomeProductCarousel({
  products,
  idPrefix,
}: {
  products: Product[];
  idPrefix: string;
}) {
  const [ref, api] = useEmblaCarousel(
    { align: "start", loop: products.length > 4, slidesToScroll: 1 },
    [Autoplay({ delay: 5000, stopOnInteraction: true })],
  );
  const [canPrev, setCanPrev] = useState(false);
  const [canNext, setCanNext] = useState(false);

  const sync = useCallback(() => {
    if (!api) return;
    setCanPrev(api.canScrollPrev());
    setCanNext(api.canScrollNext());
  }, [api]);

  useEffect(() => {
    if (!api) return;
    sync();
    api.on("select", sync);
    api.on("reInit", sync);
  }, [api, sync]);

  return (
    <div className="relative mt-8">
      <div className="overflow-hidden -mx-2" ref={ref}>
        <div className="flex gap-4 px-2">
          {products.map((p) => (
            <div
              key={`${idPrefix}-${p.id}`}
              className="basis-[80%] sm:basis-[45%] md:basis-[30%] lg:basis-[23%] shrink-0"
            >
              <ProductCard product={p} className="h-full" />
            </div>
          ))}
        </div>
      </div>

      <button
        type="button"
        onClick={() => api?.scrollPrev()}
        disabled={!canPrev}
        className="hidden md:flex h-11 w-11 absolute left-0 -translate-x-1/2 top-1/2 -translate-y-1/2 rounded-full bg-white border border-border-base shadow-soft items-center justify-center hover:bg-primary hover:text-white disabled:opacity-30 disabled:hover:bg-white disabled:hover:text-on-surface transition-colors"
        aria-label="Trước"
      >
        <ChevronLeft className="h-5 w-5" />
      </button>
      <button
        type="button"
        onClick={() => api?.scrollNext()}
        disabled={!canNext}
        className="hidden md:flex h-11 w-11 absolute right-0 translate-x-1/2 top-1/2 -translate-y-1/2 rounded-full bg-white border border-border-base shadow-soft items-center justify-center hover:bg-primary hover:text-white disabled:opacity-30 disabled:hover:bg-white disabled:hover:text-on-surface transition-colors"
        aria-label="Sau"
      >
        <ChevronRight className="h-5 w-5" />
      </button>
    </div>
  );
}
