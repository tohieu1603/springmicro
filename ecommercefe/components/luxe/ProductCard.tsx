"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { makeBg, IllustStyle } from "@/lib/illustrations";

export type Product = {
  id: string;
  name: string;
  price?: string;
  tag?: string;
  /** Real BE images (thumbnail + gallery + variant images). When present, rendered as cycling layers. */
  imageUrls?: string[];
  /** SVG fallback when no real images are available. */
  baseStyle?: IllustStyle;
  palette?: number;
};

const STYLE_POOL: IllustStyle[] = ["hobo", "topHandle", "bucket", "chevron"];

/**
 * Storefront product card. Hover-cycles through layers — real photos when the
 * BE has shipped images, SVG illustrations otherwise so a fresh DB still feels
 * curated. The first photo is the resting frame; mouse-enter advances to the
 * next, mouse-leave returns home.
 */
export default function ProductCard({ p }: { p: Product }) {
  const router = useRouter();
  const [cur, setCur] = useState(0);

  const realLayers = (p.imageUrls ?? []).filter(Boolean);
  // Always have at least 2 visual frames: when the BE only ships one image,
  // fold an SVG "model shot" in as the second layer so hover still produces a
  // visible transition (rest → hover → rest) instead of a frozen card.
  const usingReal = realLayers.length > 0;

  const svgLayers: { style: IllustStyle; palette: number }[] = [
    { style: p.baseStyle ?? "hobo", palette: p.palette ?? 0 },
    { style: "model", palette: p.palette ?? 0 },
    {
      style:
        STYLE_POOL[(STYLE_POOL.indexOf(p.baseStyle ?? "hobo") + 1) % STYLE_POOL.length] ?? "hobo",
      palette: p.palette ?? 0,
    },
  ];

  // Compose the active layer set: real images first, then SVG slots to top up
  // to at least 2 frames so the carousel arrows + hover swap always work.
  const composedReal: { kind: "real"; url: string }[] = realLayers.map((url) => ({ kind: "real", url }));
  const composedSvg: { kind: "svg"; style: IllustStyle; palette: number }[] =
    svgLayers.map((s) => ({ kind: "svg", ...s }));
  const composed: Array<typeof composedReal[number] | typeof composedSvg[number]> = usingReal
    ? (realLayers.length >= 2 ? composedReal : [...composedReal, composedSvg[1], composedSvg[2]])
    : composedSvg;

  const layerCount = composed.length;
  const show = (i: number) => setCur(((i % layerCount) + layerCount) % layerCount);

  return (
    <div
      className="product-card"
      data-id={p.id}
      onClick={() => router.push(`/product?id=${p.id}`)}
      onMouseEnter={() => { if (layerCount > 1 && cur === 0) show(1); }}
      onMouseLeave={() => show(0)}
    >
      <div className="pc-img">
        <div className="pc-track">
          {composed.map((l, i) => (
            <div
              key={i}
              className={`pc-layer${i === cur ? " active" : ""}`}
              style={{
                backgroundImage:
                  l.kind === "real" ? `url(${l.url})` : makeBg(l.style, l.palette),
                backgroundSize: "cover",
                backgroundPosition: "center",
              }}
            />
          ))}
        </div>

        {layerCount > 1 && (
          <>
            <div
              className="pc-arrow left"
              onClick={(e) => { e.stopPropagation(); show(cur - 1); }}
            >
              ‹
            </div>
            <div
              className="pc-arrow right"
              onClick={(e) => { e.stopPropagation(); show(cur + 1); }}
            >
              ›
            </div>

            <div className="pc-progress">
              {Array.from({ length: layerCount }).map((_, i) => (
                <div key={i} className={`pp${i === cur ? " active" : ""}`} />
              ))}
            </div>
          </>
        )}

        {p.tag && <div className="pc-tag">{p.tag}</div>}
      </div>

      <div className="pc-info">
        <div className="pc-name">{p.name}</div>
        {p.price && <div className="pc-price">{p.price}</div>}
      </div>
    </div>
  );
}
