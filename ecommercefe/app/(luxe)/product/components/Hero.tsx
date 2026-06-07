"use client";

import { hashId } from "@/lib/hashId";
import { useMemo } from "react";
import { makeBg, IllustStyle } from "@/lib/illustrations";

interface HeroProps {
  gallery: string[];
  productId: string;
  galleryIdx: number;
  setGalleryIdx: (next: number | ((v: number) => number)) => void;
}

const FALLBACK_STYLES: IllustStyle[] = ["hobo", "topHandle", "bucket", "chevron", "hobo", "hobo"];

/**
 * Single/dual-page hero stage with bottom thumb strip + arrows. Pure
 * presentational — receives the gallery + index from the parent.
 */
export function Hero({ gallery, productId, galleryIdx, setGalleryIdx }: HeroProps) {
  const palette = (hashId(productId) * 31) % 8;
  const heroImages = gallery.length > 0
    ? gallery.slice(0, 6).map((url) => ({ url }))
    : FALLBACK_STYLES.map(() => ({ url: undefined as string | undefined }));

  const total = heroImages.length;
  const maxPage = Math.ceil(total / 2);

  const transform = useMemo(() => {
    if (galleryIdx === 0) return "translateX(25vw)";
    return `translateX(-${(galleryIdx - 1) * 100}vw)`;
  }, [galleryIdx]);

  const visible: number[] = useMemo(() => {
    if (galleryIdx === 0) return [0];
    const o = (galleryIdx - 1) * 2;
    return [o, o + 1].filter((i) => i < total);
  }, [galleryIdx, total]);

  return (
    <section className="detail-hero">
      <div className={`detail-stage${galleryIdx === 0 ? " single" : " dual"}`}>
        <div className="ds-track" style={{ transform }}>
          {heroImages.map((img, i) => (
            <div key={i} className={`ds-item${visible.includes(i) ? "" : " hidden"}`}>
              <div
                className="ds-img"
                style={
                  img.url
                    ? { backgroundImage: `url(${img.url})`, backgroundSize: "cover", backgroundPosition: "center" }
                    : { backgroundImage: makeBg(FALLBACK_STYLES[i % FALLBACK_STYLES.length], palette) }
                }
              />
            </div>
          ))}
        </div>

        <div className="view-3d">View In 3D</div>

        <div className="detail-thumbs">
          <div className="thumbs">
            {heroImages.map((img, i) => (
              <div
                key={i}
                className={`th${visible.includes(i) ? " active" : ""}`}
                style={
                  img.url
                    ? { backgroundImage: `url(${img.url})`, backgroundSize: "cover", backgroundPosition: "center" }
                    : { backgroundImage: makeBg(FALLBACK_STYLES[i % FALLBACK_STYLES.length], palette) }
                }
                onClick={() => setGalleryIdx(i === 0 ? 0 : Math.floor(i / 2) + 1)}
              />
            ))}
          </div>
          <div
            className={`arrow left${galleryIdx <= 0 ? " disabled" : ""}`}
            onClick={() => setGalleryIdx((v: number) => Math.max(0, v - 1))}
          >‹</div>
          <div
            className={`arrow right${galleryIdx >= maxPage ? " disabled" : ""}`}
            onClick={() => setGalleryIdx((v: number) => Math.min(maxPage, v + 1))}
          >›</div>
        </div>
      </div>
    </section>
  );
}
