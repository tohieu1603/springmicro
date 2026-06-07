"use client";

import { useCallback, useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { NotificationsApi } from "../services/api";
import type { NotificationItem } from "../types";
import { qk } from "@/lib/query/keys";

const POLL_INTERVAL_MS = 60_000;

/**
 * God hook for the bell + dropdown — TanStack Query edition.
 *
 *   – {@code qk.notifications.unread()} polls every 60s and refetches on
 *     any SSE `notification` push (cheap).
 *   – {@code qk.notifications.feed()} hydrates lazily when the dropdown
 *     opens (heavier).
 *   – Mark-read / mark-all-read mutate optimistically and roll back on
 *     error by re-fetching the unread count.
 *
 * SSE is wired here so any push refreshes the badge immediately — the 60s
 * poll is only a fallback when the SSE channel is closed.
 */
export function useNotifications() {
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const [authed, setAuthed] = useState(true);

  const unreadQ = useQuery({
    queryKey: qk.notifications.unread(),
    queryFn: async () => {
      try {
        const c = await NotificationsApi.getUnreadCount();
        setAuthed(true);
        return c;
      } catch (e: unknown) {
        const err = e as { response?: { status?: number } };
        if (err.response?.status === 401) { setAuthed(false); return 0; }
        return 0;
      }
    },
    refetchInterval: POLL_INTERVAL_MS,
    retry: 0,
    staleTime: 30_000,
  });

  const feedQ = useQuery({
    queryKey: qk.notifications.feed(),
    queryFn: async () => {
      try { return await NotificationsApi.getRecent(10); }
      catch (e: unknown) {
        const err = e as { response?: { status?: number } };
        if (err.response?.status === 401) setAuthed(false);
        return [] as NotificationItem[];
      }
    },
    enabled: open,
    staleTime: 30_000,
    retry: 0,
  });

  // SSE live push — invalidate unread on every notification event so the
  // badge bumps without waiting for the 60s poll.
  useEffect(() => {
    let es: EventSource | null = null;
    try {
      es = new EventSource("/api/proxy/api/notifications/stream", { withCredentials: true });
      es.addEventListener("notification", () => {
        qc.invalidateQueries({ queryKey: qk.notifications.unread() });
        if (open) qc.invalidateQueries({ queryKey: qk.notifications.feed() });
      });
      es.onerror = () => {/* browser handles reconnect */};
    } catch { /* SSE unsupported — polling still works */ }
    return () => { if (es) es.close(); };
  }, [qc, open]);

  const markReadMut = useMutation({
    mutationFn: (id: string) => NotificationsApi.markRead(id),
    onMutate: async (id) => {
      qc.setQueryData<NotificationItem[]>(qk.notifications.feed(),
        (cur) => (cur ?? []).map((n) => (n.id === id ? { ...n, isRead: true } : n)));
      qc.setQueryData<number>(qk.notifications.unread(), (n) => Math.max(0, (n ?? 1) - 1));
    },
    onError: () => qc.invalidateQueries({ queryKey: qk.notifications.unread() }),
  });

  const markAllReadMut = useMutation({
    mutationFn: () => NotificationsApi.markAllRead(),
    onMutate: async () => {
      qc.setQueryData<NotificationItem[]>(qk.notifications.feed(),
        (cur) => (cur ?? []).map((n) => ({ ...n, isRead: true })));
      qc.setQueryData<number>(qk.notifications.unread(), 0);
    },
    onError: () => qc.invalidateQueries({ queryKey: qk.notifications.unread() }),
  });

  const toggle = useCallback(() => setOpen((o) => !o), []);
  const close = useCallback(() => setOpen(false), []);

  return {
    open,
    items: feedQ.data ?? [],
    loading: feedQ.isLoading,
    unread: unreadQ.data ?? 0,
    authed,
    toggle,
    close,
    markRead: (id: string) => markReadMut.mutate(id),
    markAllRead: () => markAllReadMut.mutate(),
  };
}

export type NotificationsVM = ReturnType<typeof useNotifications>;
