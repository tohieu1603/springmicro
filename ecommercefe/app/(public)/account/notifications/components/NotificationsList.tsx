"use client";

import { Bell } from "lucide-react";
import { Empty } from "@/components/ui/empty";

import type { Notification } from "../types";
import { NotificationItem } from "./NotificationItem";

interface NotificationsListProps {
  loading: boolean;
  items: Notification[];
  onMarkRead: (id: string) => void;
}

export function NotificationsList({ loading, items, onMarkRead }: NotificationsListProps) {
  if (loading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="h-16 rounded bg-surface-container animate-pulse" />
        ))}
      </div>
    );
  }
  if (items.length === 0) {
    return (
      <Empty
        icon={<Bell className="h-12 w-12" />}
        title="Chưa có thông báo"
        description="Hoạt động đơn hàng, khuyến mãi sẽ xuất hiện ở đây."
      />
    );
  }
  return (
    <div className="space-y-2">
      {items.map((n) => (
        <NotificationItem key={n.id} notification={n} onClick={() => onMarkRead(n.id)} />
      ))}
    </div>
  );
}
