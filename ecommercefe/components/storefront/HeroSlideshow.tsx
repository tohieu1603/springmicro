"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api/client";

interface Banner {
  id: string;
  title: string;
  subtitle?: string;
  imageUrl: string;
  targetUrl?: string;
  ctaLabel?: string;
}

/**
 * Storefront hero slideshow. Pulls active banners from
 * {@code GET /api/banners/active} (sorted server-side by displayOrder) and
 * cycles every 5s. Falls back to silently rendering nothing when no banners
 * are configured so home pages without admin setup don't show a broken slot.
 */
const ROTATE_MS = 5_000;

export default function HeroSlideshow({ className }: { className?: string }) {
  const [banners, setBanners] = useState<Banner[]>([]);
  const [idx, setIdx] = useState(0);

  useEffect(() => {
    api.get<Banner[] | { data: Banner[] }>("/api/banners/active", {
      validateStatus: (s) => s < 500,
    })
      .then((res) => {
        if (res.status >= 400) return;
        const body = res.data as { data?: Banner[] };
        setBanners((body.data ?? (res.data as Banner[])) || []);
      })
      .catch(() => {/* silent — homepage degrades gracefully */});
  }, []);

  useEffect(() => {
    if (banners.length < 2) return;
    const t = window.setInterval(() => setIdx((i) => (i + 1) % banners.length), ROTATE_MS);
    return () => window.clearInterval(t);
  }, [banners.length]);

  if (banners.length === 0) return null;

  return (
    <div
      className={className}
      style={{
        position: "relative", width: "100%", aspectRatio: "21/9",
        overflow: "hidden", background: "#000",
      }}
    >
      {banners.map((b, i) => {
        const active = i === idx;
        const inner = (
          <>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={b.imageUrl}
              alt={b.title}
              style={{
                position: "absolute", inset: 0,
                width: "100%", height: "100%", objectFit: "cover",
                opacity: active ? 1 : 0,
                transition: "opacity .8s cubic-bezier(.6,.05,.3,1)",
              }}
            />
            <div
              style={{
                position: "absolute", inset: 0,
                display: "flex", flexDirection: "column", justifyContent: "flex-end",
                padding: "32px 48px",
                background: "linear-gradient(to top, rgba(0,0,0,.45) 0%, transparent 50%)",
                color: "#fff",
                opacity: active ? 1 : 0,
                transition: "opacity .8s cubic-bezier(.6,.05,.3,1)",
              }}
            >
              <h2 style={{ fontSize: 32, fontWeight: 300, letterSpacing: 2, marginBottom: 8 }}>
                {b.title}
              </h2>
              {b.subtitle && (
                <p style={{ fontSize: 14, opacity: 0.85, marginBottom: 12 }}>{b.subtitle}</p>
              )}
              {b.ctaLabel && (
                <span style={{
                  display: "inline-block", padding: "10px 24px",
                  background: "#fff", color: "#000",
                  fontSize: 11, letterSpacing: 2, textTransform: "uppercase",
                  width: "fit-content",
                }}>
                  {b.ctaLabel}
                </span>
              )}
            </div>
          </>
        );
        return b.targetUrl ? (
          <Link key={b.id} href={b.targetUrl} style={{ position: "absolute", inset: 0 }}>
            {inner}
          </Link>
        ) : (
          <div key={b.id} style={{ position: "absolute", inset: 0 }}>{inner}</div>
        );
      })}

      {/* dots */}
      {banners.length > 1 && (
        <div
          style={{
            position: "absolute", bottom: 16, left: "50%", transform: "translateX(-50%)",
            display: "flex", gap: 6, zIndex: 2,
          }}
        >
          {banners.map((_, i) => (
            <button
              key={i}
              aria-label={`Slide ${i + 1}`}
              onClick={() => setIdx(i)}
              style={{
                width: i === idx ? 28 : 8, height: 8,
                borderRadius: 4, border: "none",
                background: i === idx ? "#fff" : "rgba(255,255,255,.5)",
                cursor: "pointer", transition: "all .25s ease",
              }}
            />
          ))}
        </div>
      )}
    </div>
  );
}
