"use client";

import { useEffect, useRef } from "react";
import type { NotificationItem } from "../types";

interface NotificationsPanelProps {
  open: boolean;
  loading: boolean;
  authed: boolean;
  items: NotificationItem[];
  onClose: () => void;
  onMarkRead: (id: string) => void;
  onMarkAllRead: () => void;
}

function timeAgo(iso?: string): string {
  if (!iso) return "";
  const diff = Date.now() - new Date(iso).getTime();
  if (diff < 60_000) return "vừa xong";
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} phút trước`;
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} giờ trước`;
  if (diff < 7 * 86_400_000) return `${Math.floor(diff / 86_400_000)} ngày trước`;
  return new Date(iso).toLocaleDateString("vi-VN");
}

export function NotificationsPanel(p: NotificationsPanelProps) {
  const ref = useRef<HTMLDivElement>(null);

  // Click-outside to close.
  useEffect(() => {
    if (!p.open) return;
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) p.onClose();
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [p.open, p]);

  if (!p.open) return null;

  return (
    <div ref={ref} className="lux-notif-panel">
      <div className="lux-notif-header">
        <span>THÔNG BÁO</span>
        {p.authed && p.items.some((i) => !i.isRead) && (
          <button type="button" className="lux-notif-readall" onClick={p.onMarkAllRead}>
            Đánh dấu đã đọc tất cả
          </button>
        )}
      </div>

      <div className="lux-notif-body">
        {!p.authed && (
          <div className="lux-notif-empty">
            <a href="/login">Đăng nhập</a> để xem thông báo
          </div>
        )}

        {p.authed && p.loading && p.items.length === 0 && (
          <div className="lux-notif-empty">Đang tải…</div>
        )}

        {p.authed && !p.loading && p.items.length === 0 && (
          <div className="lux-notif-empty">Chưa có thông báo</div>
        )}

        {p.items.map((n) => (
          <div
            key={n.id}
            className={`lux-notif-item${n.isRead ? "" : " unread"}`}
            onClick={() => !n.isRead && p.onMarkRead(n.id)}
          >
            {!n.isRead && <span className="lux-notif-dot" />}
            <div className="lux-notif-content">
              <div className="lux-notif-title">{n.title}</div>
              <div className="lux-notif-msg">{n.content}</div>
              <div className="lux-notif-time">{timeAgo(n.createdAt)}</div>
            </div>
          </div>
        ))}
      </div>

      {p.authed && p.items.length > 0 && (
        <a href="/notifications" className="lux-notif-footer">Xem tất cả</a>
      )}
    </div>
  );
}
