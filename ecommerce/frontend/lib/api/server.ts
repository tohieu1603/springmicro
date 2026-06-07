import "server-only";
import axios, { type AxiosRequestConfig } from "axios";
import { cookies, headers } from "next/headers";
import { env } from "@/lib/env";

/**
 * Server-side axios. Used inside RSC, route handlers, and server actions.
 *
 * Behaviour:
 *  - Targets the Java api-gateway directly so it can attach the user's
 *    HttpOnly access cookie + forward the request id.
 *  - Never reads localStorage / window — keep all auth state on the server.
 *  - Throws plain Error on non-2xx so SSR pages can `notFound()` / `redirect()`.
 */
export const serverApi = axios.create({
  baseURL: env.API_BASE_URL,
  // Generous timeout — Spring lazy-init + first JPA query can take 15s on cold boot.
  // Tighten in prod (BE warmed up by then).
  timeout: 30_000,
  headers: { Accept: "application/json" },
});

serverApi.interceptors.request.use(async (config) => {
  const ck = await cookies();
  const access = ck.get(env.AUTH_COOKIE_ACCESS)?.value;
  if (access) {
    config.headers.set("Authorization", `Bearer ${access}`);
  }
  // Forward x-forwarded-for so the gateway sees the real client.
  const hdrs = await headers();
  const fwd = hdrs.get("x-forwarded-for");
  if (fwd) config.headers.set("x-forwarded-for", fwd);
  return config;
});

/** Convenience wrapper that unwraps ApiResponse<T>.data when present. */
export async function fetchServer<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await serverApi.request<T | { data: T }>({ url, ...config });
  const body = res.data as unknown as { data?: T };
  if (body && typeof body === "object" && "data" in body) return body.data as T;
  return res.data as T;
}

/** Variant that returns null on 404 instead of throwing — good for product/by-slug. */
export async function fetchServerOrNull<T>(url: string, config?: AxiosRequestConfig): Promise<T | null> {
  try {
    return await fetchServer<T>(url, config);
  } catch (e) {
    if (axios.isAxiosError(e) && e.response?.status === 404) return null;
    throw e;
  }
}

/**
 * Like {@link fetchServer} but swallows ALL errors → returns `null`. Use in RSC
 * sections that should degrade gracefully when BE is cold / unreachable / 5xx,
 * instead of throwing an AxiosError whose internals (functions, AxiosHeaders
 * class) crash Next's RSC → CC serializer with confusing secondary errors.
 */
export async function fetchServerSilent<T>(url: string, config?: AxiosRequestConfig): Promise<T | null> {
  try {
    return await fetchServer<T>(url, config);
  } catch {
    return null;
  }
}
