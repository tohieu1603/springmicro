import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import { env } from "@/lib/env";

/**
 * POST /api/auth/refresh — forwards the refresh cookie to BE; BE returns
 * a fresh ACCESS_TOKEN cookie that we forward back. Used by the client
 * axios interceptor on 401.
 */
export async function POST(req: NextRequest) {
  const ck = await cookies();
  const refresh = ck.get(env.AUTH_COOKIE_REFRESH)?.value;

  // Re-send the request to BE with the refresh cookie attached.
  const cookieHeader = ck
    .getAll()
    .map((c) => `${c.name}=${c.value}`)
    .join("; ");

  const upstream = await fetch(`${env.API_BASE_URL}/api/auth/refresh`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Cookie: cookieHeader,
    },
    body: refresh ? JSON.stringify({ refreshToken: refresh }) : "{}",
  });

  const text = await upstream.text();
  const res = new NextResponse(text, {
    status: upstream.status,
    headers: { "Content-Type": "application/json" },
  });

  // Clear cookies if BE rejected the refresh.
  if (!upstream.ok) {
    res.cookies.delete(env.AUTH_COOKIE_ACCESS);
    res.cookies.delete(env.AUTH_COOKIE_REFRESH);
    return res;
  }

  for (const c of upstream.headers.getSetCookie?.() ?? []) {
    res.headers.append("Set-Cookie", c);
  }
  return res;
}
