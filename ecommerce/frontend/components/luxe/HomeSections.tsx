"use client";

import { useRouter } from "next/navigation";
import { makeBg, IllustStyle } from "@/lib/illustrations";

type CategoryItem = { style: IllustStyle; palette: number; label: string };

const CATEGORIES: CategoryItem[] = [
  { style: "wallet",    palette: 1, label: "Women's Small Leather Goods" },
  { style: "shoe",      palette: 2, label: "Women's Shoes" },
  { style: "topHandle", palette: 6, label: "Borsetto Handbag" },
  { style: "chevron",   palette: 4, label: "Men's Bags" },
];

type Service = { style: IllustStyle; palette: number; title: string; cta: string };

const SERVICES: Service[] = [
  { style: "bucket",    palette: 5, title: "BOOK AN APPOINTMENT", cta: "Find a Store" },
  { style: "hobo",      palette: 7, title: "PERSONALIZATION",     cta: "Explore Options" },
  { style: "topHandle", palette: 3, title: "COLLECT IN STORE",    cta: "Discover How" },
];

export function CategoryGrid() {
  const router = useRouter();
  return (
    <section className="cat-grid">
      {CATEGORIES.map((c, i) => (
        <div key={i} className="cat-item" onClick={() => router.push(`/shop?cat=${i}`)}>
          <div
            className="ci-bg"
            style={{
              backgroundImage: makeBg(c.style, c.palette),
              backgroundSize: "cover",
              backgroundPosition: "center",
            }}
          />
          <div className="ci-label">{c.label}</div>
        </div>
      ))}
    </section>
  );
}

export function ServicesSection() {
  return (
    <section className="services">
      <h2>HIEU SERVICES</h2>
      <div className="services-grid">
        {SERVICES.map((s, i) => (
          <div key={i} className="service-card">
            <div
              className="sc-bg"
              style={{
                backgroundImage: makeBg(s.style, s.palette),
                backgroundSize: "cover",
                backgroundPosition: "center",
              }}
            />
            <div className="sc-pause" />
            <div className="sc-info">
              <h3>{s.title}</h3>
              <a href="#">{s.cta}</a>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

export function SubscribeSection() {
  return (
    <section className="subscribe">
      <div className="label">SIGN UP FOR HIEU UPDATES</div>
      <h2>
        Get exclusive updates on the collection&apos;s launch,
        <br />
        personalized communication and the House&apos;s latest news.
      </h2>
      <a href="#" className="sub-btn">
        <span className="plus">+</span> Subscribe
      </a>
    </section>
  );
}
