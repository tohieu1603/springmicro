"use client";

import { useRouter } from "next/navigation";
import { useUI } from "./UIProvider";
import { makeBg, IllustStyle } from "@/lib/illustrations";
import CartOverlay from "./CartOverlay";

export default function Overlays() {
  const { overlay, close } = useUI();
  const router = useRouter();

  if (!overlay) return null;

  if (overlay === "cart") {
    return <CartOverlay onClose={close} />;
  }

  if (overlay === "search") {
    const trending: IllustStyle[] = ["shoe", "hobo", "hobo"];
    return (
      <div className="search-overlay">
        <div className="search-top">
          <span className="brand">HIEU</span>
          <button className="close-btn" onClick={close}>
            Close
          </button>
        </div>
        <div className="search-body">
          <aside className="search-side">
            <h4>NEW IN</h4>
            <ul>
              <li>
                <a href="#">Women</a>
              </li>
              <li>
                <a href="#">Men</a>
              </li>
            </ul>
            <h4>SUGGESTIONS</h4>
            <ul>
              <li>
                <a href="#">Personalization</a>
              </li>
              <li>
                <a href="#">Store Locator</a>
              </li>
            </ul>
          </aside>

          <div className="search-main">
            <div className="search-input-wrap">
              <div className="search-input-label">
                Search for:<span className="term">Handbags</span>
              </div>
            </div>
            <div className="search-tags">
              <span className="tag-label">Trending Searches</span>
              <a href="#">Handbags</a>
              <a href="#">Shoes</a>
              <a href="#">Belts</a>
              <a href="#">Wallets</a>
            </div>

            <div className="search-section-title">MOST COVETED</div>
            <div className="search-results">
              {trending.map((style, i) => (
                <div
                  key={i}
                  className="search-result-card"
                  onClick={() => {
                    close();
                    router.push(`/product?id=${i + 1}`);
                  }}
                >
                  {i > 0 && <div className="search-result-tag">Personalize with initials</div>}
                  <div
                    className="search-result-img"
                    style={{
                      backgroundImage: makeBg(style, i === 0 ? 4 : i === 1 ? 3 : 4),
                    }}
                  />
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (overlay === "menu") {
    const main = [
      "Handbags",
      "Women",
      "Men",
      "New In",
      "Children",
      "Travel",
      "Jewelry & Watches",
      "Décor & Lifestyle",
      "Fragrances & Make-Up",
      "Gifts",
    ];
    const sub = ["Hieu Services", "World of Hieu", "Store Locator"];

    return (
      <div className="menu-overlay" onClick={close}>
        <div className="menu-drawer" onClick={(e) => e.stopPropagation()}>
          <button className="close-x" onClick={close} aria-label="close">
            ×
          </button>
          <ul>
            {main.map((m, i) => (
              <li key={i}>
                <a
                  href="#"
                  onClick={(e) => {
                    e.preventDefault();
                    close();
                    if (m === "Handbags") router.push("/shop?cat=0");
                  }}
                >
                  {m}
                </a>
              </li>
            ))}
          </ul>
          <div className="menu-divider" />
          <ul className="menu-sub">
            {sub.map((s, i) => (
              <li key={i}>
                <a href="#">{s}</a>
              </li>
            ))}
          </ul>
        </div>
      </div>
    );
  }

  return null;
}
