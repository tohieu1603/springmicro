"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { qk } from "@/lib/query/keys";

interface FlashSaleItem {
  id: string;
  productId: string;
  productName: string;
  variantId?: string;
  imageUrl?: string;
  originalPrice?: string | number;
  flashPrice: string | number;
  quantity: number;
  sold: number;
}

interface FlashSale {
  id: string;
  name: string;
  endsAt: string;
  items: FlashSaleItem[];
}

function priceVND(v: string | number) {
  const n = typeof v === "string" ? Number(v) : v;
  if (!Number.isFinite(n)) return "";
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(n);
}

/**
 * Storefront flash-sale strip. Pulls the first active sale via TanStack
 * Query (cached 60s, refetch on focus) and renders item tiles with a live
 * countdown + sold progress. Returns null when nothing is live so the
 * homepage doesn't render an empty block.
 *
 * The countdown is a separate {@code useState(Date.now())} that ticks every
 * second — it's pure presentation, no need to involve the query cache.
 */
export default function FlashSaleStrip({ className }: { className?: string }) {
  const { data: list = [] } = useQuery({
    queryKey: qk.flashSales.active(),
    queryFn: async () => {
      const res = await api.get<FlashSale[] | { data: FlashSale[] }>("/api/flash-sales/active", {
        validateStatus: (s) => s < 500,
      });
      if (res.status >= 400) return [] as FlashSale[];
      const body = res.data as { data?: FlashSale[] };
      return (body.data ?? (res.data as FlashSale[])) || [];
    },
    staleTime: 60_000,
    retry: 0,
  });
  const sale = list[0] ?? null;

  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    if (!sale) return;
    const t = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(t);
  }, [sale]);

  if (!sale) return null;

  const remainingMs = Math.max(0, new Date(sale.endsAt).getTime() - now);
  const hh = Math.floor(remainingMs / 3_600_000);
  const mm = Math.floor((remainingMs % 3_600_000) / 60_000);
  const ss = Math.floor((remainingMs % 60_000) / 1000);

  if (remainingMs === 0) return null;

  return (
    <section className={className} style={{ padding: "32px 0", background: "#fff" }}>
      <div style={{ maxWidth: 1280, margin: "0 auto", padding: "0 24px" }}>
        <header style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: 16 }}>
          <h2 style={{ fontSize: 22, fontWeight: 300, letterSpacing: 3, textTransform: "uppercase" }}>
            <span style={{ color: "#c00", marginRight: 8 }}>⚡</span>
            Flash Sale — {sale.name}
          </h2>
          <span style={{
            fontVariantNumeric: "tabular-nums",
            background: "#000", color: "#fff",
            padding: "6px 14px", fontSize: 13, letterSpacing: 1,
          }}>
            Còn {String(hh).padStart(2, "0")}:{String(mm).padStart(2, "0")}:{String(ss).padStart(2, "0")}
          </span>
        </header>

        <div style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
          gap: 16,
        }}>
          {sale.items.slice(0, 6).map((it) => {
            const soldPct = it.quantity > 0
              ? Math.min(100, Math.round((it.sold / it.quantity) * 100))
              : 0;
            return (
              <Link
                key={it.id}
                href={`/product?id=${it.productId}`}
                style={{ display: "block", textDecoration: "none", color: "inherit" }}
              >
                <div style={{
                  aspectRatio: "1/1",
                  background: it.imageUrl ? `url(${it.imageUrl}) center/cover no-repeat` : "#f5f5f5",
                  marginBottom: 8,
                }} />
                <div style={{ fontSize: 13, marginBottom: 4, height: 36, overflow: "hidden" }}>
                  {it.productName}
                </div>
                <div style={{ display: "flex", alignItems: "baseline", gap: 6 }}>
                  <span style={{ color: "#c00", fontWeight: 600 }}>{priceVND(it.flashPrice)}</span>
                  {it.originalPrice && (
                    <span style={{ textDecoration: "line-through", color: "#888", fontSize: 11 }}>
                      {priceVND(it.originalPrice)}
                    </span>
                  )}
                </div>
                <div style={{
                  marginTop: 6, height: 14,
                  background: "#fae0e0", borderRadius: 7, overflow: "hidden", position: "relative",
                }}>
                  <div style={{
                    width: `${soldPct}%`, height: "100%",
                    background: "linear-gradient(90deg,#ff5252,#c00)",
                  }} />
                  <span style={{
                    position: "absolute", inset: 0, display: "flex",
                    alignItems: "center", justifyContent: "center",
                    color: "#fff", fontSize: 10, fontWeight: 600, letterSpacing: .5,
                  }}>
                    Đã bán {it.sold}/{it.quantity}
                  </span>
                </div>
              </Link>
            );
          })}
        </div>
      </div>
    </section>
  );
}
