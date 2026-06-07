"use client";

import { makeBg } from "@/lib/illustrations";
import type { HeroSlot } from "../types";

export function CategoryHero({ hero }: { hero: HeroSlot }) {
  return (
    <section className="cat-hero">
      <div
        className="ch-bg"
        style={{
          backgroundImage: makeBg(hero.style, hero.palette),
          backgroundSize: "cover",
          backgroundPosition: "center",
        }}
      />
      <div className="ch-title">{hero.title}</div>
    </section>
  );
}
