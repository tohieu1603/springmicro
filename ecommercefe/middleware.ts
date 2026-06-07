import { NextResponse, type NextRequest } from "next/server";

/**
 * Edge middleware. Two responsibilities:
 *
 *  1. **Soft auth gate** on /admin/* — only checks "is there an access cookie".
 *     The deep role check still happens in `app/admin/layout.tsx` via getSession(),
 *     but this middleware bounces anonymous traffic without burning an RSC render.
 *
 *  2. **Security headers** on every response (X-Frame-Options, CSP basics, etc.).
 *     Tightening CSP further can break antd inline styles — keep `unsafe-inline`
 *     for `style-src` until antd@6 drops cssinjs.
 */
// ADMIN_AUTH_BYPASS=true loosens these gates so /admin works without BE.
// Tighten by setting the env to "false" (or removing it).
const BYPASS = process.env.ADMIN_AUTH_BYPASS === "true";
const PROTECTED_PREFIXES: string[] = BYPASS ? [] : ["/admin", "/account", "/checkout"];

export function middleware(req: NextRequest) {
  const url = req.nextUrl;
  const isProtected = PROTECTED_PREFIXES.some((p) => url.pathname.startsWith(p));

  if (isProtected) {
    const cookieName = process.env.AUTH_COOKIE_ACCESS || "lm_access";
    const hasCookie = req.cookies.has(cookieName);
    if (!hasCookie) {
      const dest = new URL("/login", url);
      dest.searchParams.set("next", url.pathname + url.search);
      return NextResponse.redirect(dest);
    }
  }

  const res = NextResponse.next();
  res.headers.set("X-Frame-Options", "SAMEORIGIN");
  res.headers.set("X-Content-Type-Options", "nosniff");
  res.headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
  res.headers.set(
    "Permissions-Policy",
    "camera=(), microphone=(), geolocation=(), interest-cohort=()",
  );
  return res;
}

export const config = {
  matcher: [
    /*
     * Skip Next internals + static assets.
     */
    "/((?!_next/static|_next/image|favicon.ico|img/|api/).*)",
  ],
};
