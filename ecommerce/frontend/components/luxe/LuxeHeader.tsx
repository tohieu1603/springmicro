"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { useUI } from "./UIProvider";
import { BellNotifications } from "./notifications/BellNotifications";
import { useClientSession } from "@/lib/auth/useClientSession";
import { CartBadge } from "@/components/layout/cart-badge";

/**
 * Unified site header.
 * - Renders the appropriate announcement strip per route (home vs catalog vs product)
 * - Sticks the nav and toggles its color scheme on scroll
 * - On the product detail page, forces the dark variant
 */
export default function Header() {
  const pathname = usePathname() || "/";
  const [scrolled, setScrolled] = useState(false);
  const { open } = useUI();
  const { user } = useClientSession();

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 40);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const isHome = pathname === "/";
  const isShop = pathname.startsWith("/shop");

  const navClasses = ["nav", scrolled ? "scrolled" : ""]
    .filter(Boolean)
    .join(" ");

  return (
    <>
      {isHome && (
        <div className={`announce${scrolled ? " hide" : ""}`}>
          <div className="left">
            <div
              className="dot"
              style={{ ["--rot" as string]: "240deg" } as React.CSSProperties}
            />
            <span>3/3</span>
          </div>
          <div className="center">
            <a href="#">Introducing Generation Hieu</a>
          </div>
          <div className="right">
            <span className="pause" aria-label="pause" />
          </div>
        </div>
      )}

      {isShop && (
        <div className="cat-announce">
          <span className="arrow">&lsaquo;</span>
          <div className="center">Visit a Store for Last-Minute Gifts</div>
          <span className="arrow">&rsaquo;</span>
        </div>
      )}

      <nav className={navClasses}>
        <div className="left">
          <Link href="#" className="contact">
            <span className="plus">+</span>
            <span>Contact Us</span>
          </Link>
        </div>
        <Link href="/" className="brand">
          HIEU
        </Link>
        <div className="right">
          <div className="icons">
            <button
              type="button"
              aria-label="cart"
              onClick={() => open("cart")}
              style={{ background: "none", border: "none", padding: 0, cursor: "pointer", color: "inherit", position: "relative" }}
            >
              <svg className="ico" viewBox="0 0 24 24">
                <path d="M6 7h12l-1 13H7L6 7z" />
                <path d="M9 7V5a3 3 0 016 0v2" />
              </svg>
              <CartBadge />
            </button>
            {user ? (
              <Link
                href="/account"
                aria-label={`Tài khoản ${user.fullName || user.username}`}
                title={user.fullName || user.username}
                style={{ display: "inline-flex", alignItems: "center", gap: 6, color: "inherit", textDecoration: "none" }}
              >
                <span
                  aria-hidden
                  style={{
                    width: 26, height: 26, borderRadius: "50%",
                    background: "currentColor", color: "var(--luxe-bg, #fff)",
                    fontSize: 11, fontWeight: 700,
                    display: "inline-flex", alignItems: "center", justifyContent: "center",
                  }}
                >
                  {(user.fullName || user.username).charAt(0).toUpperCase()}
                </span>
              </Link>
            ) : (
              <Link href="/login" aria-label="Đăng nhập" style={{ color: "inherit" }}>
                <svg className="ico" viewBox="0 0 24 24">
                  <circle cx="12" cy="8" r="4" />
                  <path d="M4 21c0-4 4-7 8-7s8 3 8 7" />
                </svg>
              </Link>
            )}
            <button
              type="button"
              aria-label="search"
              onClick={() => open("search")}
              style={{ background: "none", border: "none", padding: 0, cursor: "pointer", color: "inherit" }}
            >
              <svg className="ico" viewBox="0 0 24 24">
                <circle cx="11" cy="11" r="7" />
                <path d="M21 21l-4.3-4.3" />
              </svg>
            </button>
            <BellNotifications />
          </div>
          <button
            type="button"
            className="menu"
            onClick={() => open("menu")}
            style={{ background: "none", border: "none", padding: 0, cursor: "pointer", color: "inherit", display: "flex", alignItems: "center", gap: 8 }}
          >
            <svg
              className="ico"
              viewBox="0 0 24 24"
              style={{ width: 18, height: 18 }}
            >
              <path d="M4 7h16M4 12h16M4 17h16" />
            </svg>
            <span>MENU</span>
          </button>
        </div>
      </nav>
    </>
  );
}
