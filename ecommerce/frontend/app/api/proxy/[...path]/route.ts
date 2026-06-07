import { NextRequest, NextResponse } from "next/server";
import { env } from "@/lib/env";
import { cookies } from "next/headers";

/**
 * Browser ⇄ Java api-gateway pass-through.
 *
 * Why route through Next: the access token lives in an HttpOnly cookie that
 * only Next can read. The browser issues `/api/proxy/...` requests against
 * the same origin; we attach the bearer header server-side and stream the
 * gateway's response back. Net effect: zero JWT exposure to JS.
 *
 * Cookie passthrough: Set-Cookie headers from the gateway (e.g. on refresh)
 * are forwarded so the browser can persist any cookie the gateway issues.
 */

const HOP_BY_HOP = new Set([
  "connection",
  "keep-alive",
  "proxy-authenticate",
  "proxy-authorization",
  "te",
  "trailers",
  "transfer-encoding",
  "upgrade",
  "host",
]);

async function handle(req: NextRequest, _ctx: { params: Promise<{ path: string[] }> }) {
  // Rebuild target from pathname so the trailing slash is preserved. The
  // catch-all `[...path]` strips empty segments, so `/api/inventory/` would
  // become `["api","inventory"]` and the slash is lost — but Spring needs
  // `/api/inventory/` (different mapping than `/api/inventory`).
  const PREFIX = "/api/proxy";
  const suffix = req.nextUrl.pathname.startsWith(PREFIX)
    ? req.nextUrl.pathname.slice(PREFIX.length)
    : req.nextUrl.pathname;
  const target = `${env.API_BASE_URL}${suffix}${req.nextUrl.search}`;

  const ck = await cookies();
  const access = ck.get(env.AUTH_COOKIE_ACCESS)?.value;

  const headers = new Headers();
  req.headers.forEach((v, k) => {
    if (!HOP_BY_HOP.has(k.toLowerCase())) headers.set(k, v);
  });
  if (access) headers.set("Authorization", `Bearer ${access}`);
  // Always set host to the upstream so virtual-host routing works.
  headers.delete("host");

  const init: RequestInit = {
    method: req.method,
    headers,
    body: ["GET", "HEAD"].includes(req.method) ? undefined : await req.arrayBuffer(),
    // Keep connection alive — Next will pool to upstream.
    cache: "no-store",
    // `follow` so that gateway's 308/307 trailing-slash redirects resolve
    // transparently. Browser CORS isn't a concern — Next is making the call
    // server-side, not the browser.
    redirect: "follow",
  };

  // Backend not running yet shouldn't take down the FE with a stack trace.
  // Translate ECONNREFUSED / DNS / abort into a clean 503 so callers can fall
  // back to empty state or retry without scaring devs in the console.
  let upstream: Response;
  try {
    upstream = await fetch(target, init);
  } catch (err) {
    const e = err as { cause?: { code?: string }; message?: string };
    const code = e.cause?.code ?? e.message ?? "UPSTREAM_UNREACHABLE";
    return NextResponse.json(
      { message: "Backend not reachable", code, target },
      { status: 503 },
    );
  }
  const resHeaders = new Headers(upstream.headers);
  // Strip hop-by-hop on the response too.
  HOP_BY_HOP.forEach((h) => resHeaders.delete(h));

  return new NextResponse(upstream.body, {
    status: upstream.status,
    headers: resHeaders,
  });
}

export const GET = handle;
export const POST = handle;
export const PUT = handle;
export const PATCH = handle;
export const DELETE = handle;
