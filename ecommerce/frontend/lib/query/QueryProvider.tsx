"use client";

import { ReactNode, useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";

/**
 * Mounts a single QueryClient for the whole app. We instantiate it inside
 * a {@code useState} initializer so Next's RSC → CC hydration boundary
 * doesn't recreate it on every render — the client must be stable to
 * preserve in-flight queries across re-renders.
 *
 * Defaults:
 *   – {@code staleTime: 30s} — keeps tab-switch refetches cheap while still
 *     refreshing on focus after half a minute.
 *   – {@code refetchOnWindowFocus: true} — what users intuitively expect.
 *   – {@code retry: 1} — one retry, no exponential thrash on dev BE that's
 *     still warming up.
 */
export function QueryProvider({ children }: { children: ReactNode }) {
  const [client] = useState(() => new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        gcTime: 5 * 60_000,
        retry: 1,
        refetchOnWindowFocus: true,
        // 401 / 403 / 404 are deterministic — retrying just spams the server.
        // axios interceptor in lib/api/client already does the 1 refresh dance.
      },
      mutations: { retry: 0 },
    },
  }));
  return (
    <QueryClientProvider client={client}>
      {children}
      {process.env.NODE_ENV !== "production" && (
        <ReactQueryDevtools initialIsOpen={false} buttonPosition="bottom-right" />
      )}
    </QueryClientProvider>
  );
}
