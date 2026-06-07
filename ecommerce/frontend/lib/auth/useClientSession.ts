"use client";

import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { qk } from "@/lib/query/keys";

export interface ClientSession {
  id: string;
  username: string;
  email: string;
  fullName?: string;
  roles: string[];
}

interface MeRaw {
  id?: string;
  userId?: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  fullName?: string;
  authorities?: string[];
  roles?: string[];
}

async function fetchSession(): Promise<ClientSession | null> {
  const res = await api.get<MeRaw | { data: MeRaw }>("/api/auth/me", {
    validateStatus: (s) => s < 500,
  });
  if (res.status >= 400) return null;
  const body = res.data as { data?: MeRaw };
  const raw = (body?.data ?? res.data) as MeRaw;
  if (!raw || !raw.username) return null;
  const auth = raw.authorities ?? [];
  const roles = Array.from(new Set<string>([
    ...(raw.roles ?? []),
    ...auth.filter((a) => a.startsWith("ROLE_")),
  ]));
  const fullName = raw.fullName
    ?? ([raw.lastName, raw.firstName].filter(Boolean).join(" ").trim() || undefined);
  return {
    id: raw.id ?? raw.userId ?? "",
    username: raw.username,
    email: raw.email,
    fullName,
    roles,
  };
}

/**
 * Browser-side session probe via TanStack Query. Cached for 60s so navigating
 * across pages doesn't refetch /api/auth/me every render. On login/logout the
 * caller should {@code queryClient.invalidateQueries({queryKey: qk.session()})}
 * to refresh immediately.
 */
export function useClientSession() {
  const q = useQuery({
    queryKey: qk.session(),
    queryFn: fetchSession,
    staleTime: 60_000,
    retry: 0,
  });
  return { user: q.data ?? null, loading: q.isLoading };
}
