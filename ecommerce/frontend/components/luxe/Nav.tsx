"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

type NavProps = { variant?: "default" | "dark" };

export default function Nav({ variant = "default" }: NavProps) {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 40);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const classes = [
    "nav",
    scrolled ? "scrolled" : "",
    variant === "dark" ? "dark" : "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <nav className={classes}>
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
          <svg className="ico" viewBox="0 0 24 24">
            <path d="M6 7h12l-1 13H7L6 7z" />
            <path d="M9 7V5a3 3 0 016 0v2" />
          </svg>
          <svg className="ico" viewBox="0 0 24 24">
            <circle cx="12" cy="8" r="4" />
            <path d="M4 21c0-4 4-7 8-7s8 3 8 7" />
          </svg>
          <svg className="ico" viewBox="0 0 24 24">
            <circle cx="11" cy="11" r="7" />
            <path d="M21 21l-4.3-4.3" />
          </svg>
        </div>
        <Link href="#" className="menu">
          <svg
            className="ico"
            viewBox="0 0 24 24"
            style={{ width: 18, height: 18 }}
          >
            <path d="M4 7h16M4 12h16M4 17h16" />
          </svg>
          <span>MENU</span>
        </Link>
      </div>
    </nav>
  );
}
