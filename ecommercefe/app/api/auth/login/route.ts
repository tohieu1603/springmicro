import { NextRequest, NextResponse } from "next/server";
import { env } from "@/lib/env";

/**
 * POST /api/auth/login — proxies to BE auth-service.
 *
 * BE puts the JWT into `Set-Cookie: ACCESS_TOKEN=...; HttpOnly; ...`. We
 * forward those Set-Cookie headers verbatim so they land on the Next domain
 * (same-origin as the browser). Subsequent `/api/proxy/*` calls then read
 * `ACCESS_TOKEN` cookie and attach Bearer header server-side.
 *
 * Body comes through unchanged (BE returns user profile + roles).
 */
export async function POST(req: NextRequest) {
  const body = await req.text();
  const upstream = await fetch(`${env.API_BASE_URL}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body,
  });

  const text = await upstream.text();
  const res = new NextResponse(text, {
    status: upstream.status,
    headers: { "Content-Type": "application/json" },
  });

  // Forward every Set-Cookie header from BE so the browser stores them
  // on the Next domain. `headers.getSetCookie()` returns them as an array;
  // appending preserves multiple cookies.
  const setCookies = upstream.headers.getSetCookie?.() ?? [];
  for (const c of setCookies) {
    res.headers.append("Set-Cookie", c);
  }
  return res;
}
