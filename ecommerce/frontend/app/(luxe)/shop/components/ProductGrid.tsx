"use client";

import ProductCard from "@/components/luxe/ProductCard";
import type { LuxeProduct } from "../types";

interface ProductGridProps {
  loaded: boolean;
  products: LuxeProduct[];
}

export function ProductGrid({ loaded, products }: ProductGridProps) {
  return (
    <div className="product-grid">
      {!loaded && (
        <div style={{ gridColumn: "1/-1", padding: 80, textAlign: "center", color: "#888" }}>
          Loading…
        </div>
      )}
      {loaded && products.length === 0 && (
        <div style={{ gridColumn: "1/-1", padding: 80, textAlign: "center", color: "#888" }}>
          Chưa có sản phẩm — hãy chạy seed script.
        </div>
      )}
      {products.map((p) => (
        <ProductCard key={p.id} p={p} />
      ))}
    </div>
  );
}
