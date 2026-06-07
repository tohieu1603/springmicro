/**
 * Single source of truth for env access. Reading `process.env` directly all over
 * the codebase risks typos and undefined-at-runtime surprises; centralising it
 * here forces a fail-fast at module load when something is missing in prod.
 */

const isServer = typeof window === "undefined";

function required(key: string): string {
  const v = process.env[key];
  if (!v) {
    // Server-only: throw loud. Browser: fall back so a missing public var
    // doesn't blank the whole app.
    if (isServer && process.env.NODE_ENV === "production") {
      throw new Error(`Missing required env: ${key}`);
    }
    return "";
  }
  return v;
}

export const env = {
  // Server-side
  API_BASE_URL: required("API_BASE_URL") || "http://localhost:8080",
  AUTH_COOKIE_ACCESS: process.env.AUTH_COOKIE_ACCESS || "lm_access",
  AUTH_COOKIE_REFRESH: process.env.AUTH_COOKIE_REFRESH || "lm_refresh",
  REVALIDATE_SECRET: process.env.REVALIDATE_SECRET || "",

  // Public (NEXT_PUBLIC_ inlined at build time)
  PUBLIC_API_BASE: process.env.NEXT_PUBLIC_API_BASE || "",
  BRAND_NAME: process.env.NEXT_PUBLIC_BRAND_NAME || "Luxury Mart",
  BRAND_TAGLINE: process.env.NEXT_PUBLIC_BRAND_TAGLINE || "Mua sắm đẳng cấp",
  /** Canonical absolute base URL for JSON-LD, OG, sitemap, robots. */
  SITE_URL:
    (process.env.NEXT_PUBLIC_SITE_URL || "").replace(/\/+$/, "") || "http://localhost:3000",
};
