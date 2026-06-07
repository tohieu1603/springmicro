"use client";

import { hashId } from "@/lib/hashId";
import { useRouter } from "next/navigation";
import { makeBg, IllustStyle } from "@/lib/illustrations";

import type { BeProduct } from "../types";
import { priceVND } from "../utils/format";

interface RelatedProps {
  items: BeProduct[];
  idx: number;
  setIdx: (next: number | ((v: number) => number)) => void;
}

const FALLBACK_STYLES: IllustStyle[] = ["hobo", "topHandle", "bucket", "chevron", "hobo", "hobo", "topHandle"];

/** "YOU MAY ALSO LIKE" rail. Click → /product?id=…. */
export function Related({ items, idx, setIdx }: RelatedProps) {
  const router = useRouter();
  if (items.length === 0) return null;

  const visible = 4;
  const max = Math.max(0, items.length - visible);
  const cardPct = 100 / visible;
  const transform = `translateX(-${idx * cardPct}%)`;

  return (
    <section className="pd-also">
      <h2>YOU MAY ALSO LIKE</h2>
      <div className="pd-also-wrap">
        <div className="pd-also-track" style={{ transform }}>
          {items.map((it, i) => {
            const imgs = [
              it.thumbnail,
              ...(it.images ?? []),
              ...(it.variants ?? []).map((v) => v.image ?? undefined),
            ].filter(Boolean) as string[];
            const cover = imgs[0];
            const pal = (hashId(it.id) * 31) % 8;
            const style = FALLBACK_STYLES[i % FALLBACK_STYLES.length];
            const minPrice = it.variants?.[0]?.price;
            return (
              <div
                key={it.id}
                className="pd-also-card"
                onClick={() => router.push(`/product?id=${it.id}`)}
              >
                {it.brand && <div className="pd-also-tag">{it.brand}</div>}
                <div
                  className="pd-also-img"
                  style={
                    cover
                      ? { backgroundImage: `url(${cover})`, backgroundSize: "cover", backgroundPosition: "center" }
                      : { backgroundImage: makeBg(style, pal), backgroundSize: "cover" }
                  }
                />
                <div className="pd-also-name">{it.name}</div>
                {minPrice && <div className="pd-also-price">{priceVND(minPrice)}</div>}
              </div>
            );
          })}
        </div>
        {items.length > visible && (
          <>
            <button className={`pd-also-arrow left${idx <= 0 ? " disabled" : ""}`} onClick={() => setIdx((v) => Math.max(0, v - 1))}>‹</button>
            <button className={`pd-also-arrow right${idx >= max ? " disabled" : ""}`} onClick={() => setIdx((v) => Math.min(max, v + 1))}>›</button>
          </>
        )}
      </div>
    </section>
  );
}
