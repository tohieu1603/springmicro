"use client";

import { useCallback, useEffect, useState } from "react";

import { AccountNotificationsApi } from "../services/api";
import type { Notification } from "../types";

export function useAccountNotifications() {
  const [items, setItems] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      setItems(await AccountNotificationsApi.list());
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    // SSE stream — optional. The BE has SSE endpoints; we listen passively and prepend.
    const url = "/api/proxy/api/notifications/stream";
    try {
      const ev = new EventSource(url);
      ev.onmessage = (e) => {
        try {
          const n = JSON.parse(e.data) as Notification;
          setItems((prev) => [n, ...prev]);
        } catch {
          // ignore non-JSON heartbeats
        }
      };
      return () => ev.close();
    } catch {
      // SSE not available → no-op
    }
  }, [load]);

  const markRead = useCallback(async (id: string) => {
    setItems((p) => p.map((x) => (x.id === id ? { ...x, read: true } : x)));
    try {
      await AccountNotificationsApi.markRead(id);
    } catch {
      // best effort
    }
  }, []);

  return { items, loading, markRead };
}

export type AccountNotificationsVM = ReturnType<typeof useAccountNotifications>;
