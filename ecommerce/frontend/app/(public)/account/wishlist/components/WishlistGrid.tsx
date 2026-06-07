"use client";

import Link from "next/link";
import { Heart } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Empty } from "@/components/ui/empty";
import { ProductCard } from "@/components/ui/product-card";
import type { Product } from "@/lib/api/types";

interface WishlistGridProps {
  loading: boolean;
  items: Product[];
}

export function WishlistGrid({ loading, items }: WishlistGridProps) {
  if (loading) {
    return (
      <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="h-64 rounded animate-pulse bg-surface-container" />
        ))}
      </div>
    );
  }
  if (items.length === 0) {
    return (
      <Empty
        icon={<Heart className="h-12 w-12" />}
        title="Chưa có sản phẩm yêu thích"
        description="Lưu các sản phẩm bạn quan tâm để theo dõi giá."
        cta={
          <Button asChild variant="cta">
            <Link href="/shop">Khám phá sản phẩm</Link>
          </Button>
        }
      />
    );
  }
  return (
    <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
      {items.map((p) => <ProductCard key={p.id} product={p} />)}
    </div>
  );
}
