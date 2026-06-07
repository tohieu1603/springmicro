"use client";

import Link from "next/link";
import Image from "next/image";
import { useState } from "react";
import { toast } from "sonner";
import { ShoppingCart, Heart, Eye } from "lucide-react";
import { api } from "@/lib/api/client";
import { useInvalidate } from "@/lib/query/invalidate";
import { formatVnd, cn } from "@/lib/utils";
import type { Product } from "@/lib/api/types";

interface ProductCardProps {
  product: Product;
  className?: string;
}

/**
 * Storefront product tile. Mirrors the Luxury Mart mock — square media zone
 * with hover zoom + side action rail, badge stack (HOT / SALE / NEW), bottom
 * info block with category, name, price, and a full-width add-to-cart CTA.
 *
 * Quick-add path uses the user's first variant. Multi-variant products bounce
 * to the PDP so the buyer can pick (BuyBox handles the matrix).
 */
export function ProductCard({ product, className }: ProductCardProps) {
  const invalidate = useInvalidate();
  const [adding, setAdding] = useState(false);
  const [wished, setWished] = useState(false);

  const firstVariant = product.variants[0];
  const minPriceVariant = product.variants.reduce<typeof firstVariant | undefined>((best, v) => {
    const cur = Number(v.salePrice ?? v.price);
    const prev = best ? Number(best.salePrice ?? best.price) : Number.POSITIVE_INFINITY;
    return Number.isFinite(cur) && cur < prev ? v : best;
  }, undefined);
  const display = minPriceVariant ?? firstVariant;

  const price = display ? Number(display.salePrice ?? display.price) : null;
  const original = display ? Number(display.price) : null;
  const hasSale = display?.salePrice && Number(display.salePrice) < Number(display.price);
  const discountPct = hasSale && original ? Math.round(((original - price!) / original) * 100) : 0;
  const outOfStock = product.variants.every((v) => v.quantity <= 0);
  const isNew = product.createdAt
    ? Date.now() - new Date(product.createdAt).getTime() < 14 * 86_400_000
    : false;
  const thumb = product.thumbnail || product.variants[0]?.image || "/img/placeholder.svg";

  async function quickAdd(e: React.MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    if (!display || outOfStock) return;
    // If product has > 1 variant, route to PDP so user can pick.
    if (product.variants.length > 1) {
      window.location.href = `/p/${product.slug}`;
      return;
    }
    setAdding(true);
    try {
      await api.post("/api/cart/items", {
        productId: product.id,
        variantId: display.id,
        quantity: 1,
      });
      invalidate.cart();
      toast.success("Đã thêm vào giỏ", {
        description: product.name,
      });
    } catch (err: unknown) {
      const e = err as { response?: { status?: number; data?: { message?: string } } };
      if (e.response?.status === 401) {
        window.location.href = `/login?next=${encodeURIComponent(window.location.pathname)}`;
        return;
      }
      toast.error(e.response?.data?.message || "Không thêm được vào giỏ");
    } finally {
      setAdding(false);
    }
  }

  function toggleWish(e: React.MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    setWished((w) => !w);
    toast(wished ? "Đã bỏ yêu thích" : "Đã thêm vào yêu thích");
  }

  return (
    <Link
      href={`/p/${product.slug}`}
      className={cn(
        "group block rounded-lg border border-border-base bg-white overflow-hidden flex flex-col",
        "transition-all duration-300 hover:shadow-soft hover:-translate-y-0.5 hover:border-primary/30",
        className,
      )}
    >
      <div className="relative aspect-square bg-surface-soft overflow-hidden">
        <Image
          src={thumb}
          alt={product.name}
          fill
          sizes="(max-width: 768px) 50vw, (max-width: 1200px) 33vw, 280px"
          className="object-cover transition-transform duration-500 group-hover:scale-110"
        />

        {/* Badge stack */}
        <div className="absolute top-2 left-2 flex flex-col gap-1">
          {hasSale && (
            <span className="bg-accent text-white text-[10px] font-bold uppercase px-2 py-1 rounded shadow">
              −{discountPct}%
            </span>
          )}
          {isNew && (
            <span className="bg-primary text-white text-[10px] font-bold uppercase px-2 py-1 rounded">
              Mới
            </span>
          )}
        </div>

        {outOfStock && (
          <div className="absolute inset-0 bg-white/70 backdrop-blur-sm flex items-center justify-center">
            <span className="bg-on-surface text-white text-xs font-semibold px-3 py-1.5 rounded">
              Hết hàng
            </span>
          </div>
        )}

        {/* Action rail (right) */}
        <div className="absolute top-2 right-2 flex flex-col gap-1.5 opacity-0 -translate-x-2 group-hover:opacity-100 group-hover:translate-x-0 transition-all duration-300">
          <button
            type="button"
            onClick={toggleWish}
            className={cn(
              "h-9 w-9 rounded-full bg-white text-on-surface shadow-soft flex items-center justify-center hover:bg-accent hover:text-white transition-colors",
              wished && "bg-accent text-white",
            )}
            aria-label="Yêu thích"
          >
            <Heart className="h-4 w-4" fill={wished ? "currentColor" : "none"} />
          </button>
          <Link
            href={`/p/${product.slug}`}
            onClick={(e) => e.stopPropagation()}
            className="h-9 w-9 rounded-full bg-white text-on-surface shadow-soft flex items-center justify-center hover:bg-primary hover:text-white transition-colors"
            aria-label="Xem nhanh"
          >
            <Eye className="h-4 w-4" />
          </Link>
        </div>
      </div>

      <div className="p-3 flex flex-col flex-grow">
        {product.brand && (
          <p className="text-[11px] uppercase tracking-wider text-slate font-medium truncate">
            {product.brand}
          </p>
        )}
        <h3 className="mt-1 text-sm font-medium text-on-surface line-clamp-2 min-h-[2.5em]">
          {product.name}
        </h3>

        <div className="mt-2 flex items-baseline gap-2">
          <span className="text-lg font-bold text-accent">
            {price !== null ? formatVnd(price) : "Liên hệ"}
          </span>
          {hasSale && original !== null && (
            <span className="text-xs text-slate line-through">{formatVnd(original)}</span>
          )}
        </div>

        <button
          type="button"
          onClick={quickAdd}
          disabled={adding || outOfStock}
          className={cn(
            "mt-3 h-10 w-full rounded font-semibold text-sm inline-flex items-center justify-center gap-1.5 transition-all",
            outOfStock
              ? "bg-surface-container text-slate cursor-not-allowed"
              : "bg-primary text-white hover:bg-primary-dark active:scale-[0.98]",
          )}
        >
          <ShoppingCart className="h-4 w-4" />
          {outOfStock ? "Hết hàng" : product.variants.length > 1 ? "Xem & chọn" : "Thêm vào giỏ"}
        </button>
      </div>
    </Link>
  );
}
