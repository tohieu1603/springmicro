"use client";

import axios, { AxiosError, type AxiosInstance } from "axios";

/**
 * Browser-side axios.
 *
 * All requests go through Next route handlers (/api/proxy/*) so the HttpOnly
 * access cookie is attached server-side. The browser never sees the JWT;
 * stealing it via XSS therefore doesn't yield a usable token.
 *
 * 401 retry: on a single 401 we hit /api/auth/refresh (also server-side) then
 * replay the original request. Two consecutive 401s = real logout.
 */
let refreshing: Promise<void> | null = null;

export const api: AxiosInstance = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE || "/api/proxy",
  timeout: 12_000,
  withCredentials: true,
  headers: { Accept: "application/json" },
});

api.interceptors.response.use(
  (r) => r,
  async (error: AxiosError) => {
    const original = error.config as (typeof error.config & { _retried?: boolean }) | undefined;
    if (!original || error.response?.status !== 401 || original._retried) {
      return Promise.reject(error);
    }
    original._retried = true;
    try {
      if (!refreshing) {
        refreshing = axios
          .post("/api/auth/refresh", null, { withCredentials: true })
          .then(() => void 0)
          .finally(() => {
            refreshing = null;
          });
      }
      await refreshing;
      return api.request(original);
    } catch {
      // Surface a clean rejection so callers can redirect to /login.
      return Promise.reject(error);
    }
  },
);

/** Helper for components that expect ApiResponse<T> envelopes. */
export async function unwrap<T>(promise: Promise<{ data: T | { data: T } }>): Promise<T> {
  const res = await promise;
  const body = res.data as unknown as { data?: T };
  if (body && typeof body === "object" && "data" in body) return body.data as T;
  return res.data as T;
}
