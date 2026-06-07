import "server-only";
import { cookies } from "next/headers";
import { serverApi } from "@/lib/api/server";
import { env } from "@/lib/env";
import type { AuthUser } from "@/lib/api/types";

/**
 * Raw shape returned by auth-service `/api/auth/me`. Differs from the login
 * response: no `roles` field, but `authorities` carries the same data plus
 * permissions (e.g. ["ROLE_ADMIN", "USER_READ", ...]).
 */
interface MeResponse {
  userId?: string;
  id?: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  fullName?: string;
  tokenVersion?: number;
  authorities?: string[];
  roles?: string[];
  permissions?: string[];
}

/**
 * Read the current user from /api/auth/me using the cookie's access token.
 * Returns null if anonymous — never throws so layouts/pages can `getSession()
 * ?? guestState` without try/catch noise.
 *
 * Normalises the response into {@link AuthUser}: extracts role-shaped entries
 * (start with `ROLE_`) from `authorities` so `isAdmin()` works uniformly.
 */
export async function getSession(): Promise<AuthUser | null> {
  const ck = await cookies();
  if (!ck.get(env.AUTH_COOKIE_ACCESS)) return null;
  try {
    const res = await serverApi.get<{ data?: MeResponse } | MeResponse>("/api/auth/me");
    const body = res.data as unknown as { data?: MeResponse };
    const raw: MeResponse | undefined = body?.data ?? (res.data as MeResponse);
    if (!raw) return null;
    // Roles can ship as `roles` (login response) OR `authorities` (me endpoint).
    // Authorities also carry permissions — keep only ROLE_* for the role list.
    const auth = raw.authorities ?? [];
    const roleSet = new Set<string>([
      ...(raw.roles ?? []),
      ...auth.filter((a) => a.startsWith("ROLE_")),
    ]);
    return {
      id: raw.id ?? raw.userId ?? "",
      username: raw.username,
      email: raw.email,
      fullName:
        raw.fullName ?? [raw.lastName, raw.firstName].filter(Boolean).join(" ").trim(),
      roles: Array.from(roleSet),
    };
  } catch {
    return null;
  }
}

/** Match both `ADMIN` and `ROLE_ADMIN` because callers pass either form. */
export function hasRole(user: AuthUser | null, role: string): boolean {
  const want = role.startsWith("ROLE_") ? role : `ROLE_${role}`;
  return Boolean(user?.roles?.some((r) => r === role || r === want));
}

export function isAdmin(user: AuthUser | null): boolean {
  return hasRole(user, "ADMIN");
}
