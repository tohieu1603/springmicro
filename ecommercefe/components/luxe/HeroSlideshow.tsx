"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { makeBg, IllustStyle } from "@/lib/illustrations";
import { qk } from "@/lib/query/keys";

interface BackendBanner {
  id: string;
  title: string;
  subtitle?: string;
  imageUrl: string;
  targetUrl?: string;
  ctaLabel?: string;
}

type IllustrationSlide = { style: IllustStyle; palette: number; label: string };

/**
 * Homepage hero slideshow. Tries to pull admin-managed banners from
 * {@code GET /api/banners/active}; if the BE is empty / cold / down, falls
 * back to the original illustration-driven slides so the home page never
 * shows an empty hero. Shares {@link qk.banners.active} with the admin
 * page so edits in /admin/banners propagate without a hard reload.
 */
const FALLBACK_SLIDES: IllustrationSlide[] = [
  { style: "hobo",    palette: 0, label: "Handbags" },
  { style: "shoe",    palette: 2, label: "Shoes" },
  { style: "chevron", palette: 4, label: "Men's Collection" },
];

const ROTATE_MS = 5_000;

export default function HeroSlideshow() {
  const router = useRouter();
  const [idx, setIdx] = useState(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const { data: banners = [] } = useQuery({
    queryKey: qk.banners.active(),
    queryFn: async () => {
      const res = await api.get<BackendBanner[] | { data: BackendBanner[] }>("/api/banners/active", {
        validateStatus: (s) => s < 500,
      });
      if (res.status >= 400) return [];
      const body = res.data as { data?: BackendBanner[] };
      return (body.data ?? (res.data as BackendBanner[])) || [];
    },
    staleTime: 60_000,
    retry: 0,
  });

  const slideCount = banners.length > 0 ? banners.length : FALLBACK_SLIDES.length;

  useEffect(() => {
    if (slideCount < 2) return;
    timerRef.current = setInterval(() => setIdx((i) => (i + 1) % slideCount), ROTATE_MS);
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, [slideCount]);

  const restartTimer = () => {
    if (timerRef.current) clearInterval(timerRef.current);
    if (slideCount < 2) return;
    timerRef.current = setInterval(() => setIdx((i) => (i + 1) % slideCount), ROTATE_MS);
  };

  return (
    <section className="hero">
      <div className="slides">
        {banners.length > 0
          ? banners.map((b, i) => (
              <div key={b.id} className={`slide${i === idx ? " active" : ""}`} data-idx={i}>
                <div
                  className="slide-bg"
                  style={{
                    backgroundImage: `url(${b.imageUrl})`,
                    backgroundSize: "cover",
                    backgroundPosition: "center",
                  }}
                />
                <div className="slide-overlay">
                  <div className="slide-label">{b.title}</div>
                  {b.subtitle && <div className="slide-subtitle">{b.subtitle}</div>}
                  <a
                    href={b.targetUrl ?? "#"}
                    className="shop-now-btn"
                    onClick={(e) => {
                      if (b.targetUrl) {
                        e.preventDefault();
                        router.push(b.targetUrl);
                      }
                    }}
                  >
                    {b.ctaLabel ?? "SHOP NOW"}
                  </a>
                </div>
              </div>
            ))
          : FALLBACK_SLIDES.map((s, i) => (
              <div key={i} className={`slide${i === idx ? " active" : ""}`} data-idx={i}>
                <div
                  className="slide-bg"
                  style={{
                    backgroundImage: makeBg(s.style, s.palette),
                    backgroundSize: "cover",
                    backgroundPosition: "center",
                  }}
                />
                <div className="slide-overlay">
                  <div className="slide-label">{s.label}</div>
                  <a
                    href="#"
                    className="shop-now-btn"
                    onClick={(e) => { e.preventDefault(); router.push(`/shop?cat=${i}`); }}
                  >
                    SHOP NOW
                  </a>
                </div>
              </div>
            ))}
      </div>

      <div className="slide-nav">
        {Array.from({ length: slideCount }).map((_, i) => (
          <div
            key={i}
            className={`sn${i === idx ? " active" : ""}`}
            onClick={() => { setIdx(i); restartTimer(); }}
          />
        ))}
      </div>
    </section>
  );
}
