import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import { env } from "@/lib/env";

export async function POST(_req: NextRequest) {
  const ck = await cookies();
  const cookieHeader = ck.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  try {
    await fetch(`${env.API_BASE_URL}/api/auth/logout`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Cookie: cookieHeader },
    });
  } catch {
    // ignore — clear local cookies regardless
  }

  const res = NextResponse.json({ ok: true });
  res.cookies.delete(env.AUTH_COOKIE_ACCESS);
  res.cookies.delete(env.AUTH_COOKIE_REFRESH);
  return res;
}
