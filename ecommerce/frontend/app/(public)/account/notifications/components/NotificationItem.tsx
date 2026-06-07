"use client";

import { Bell, MessageSquare, Package, Tag } from "lucide-react";
import { formatDate } from "@/lib/utils";

import type { Notification } from "../types";

const ICONS: Record<string, React.ComponentType<{ className?: string }>> = {
  ORDER: Package,
  VOUCHER: Tag,
  CHAT: MessageSquare,
};

interface NotificationItemProps {
  notification: Notification;
  onClick: () => void;
}

export function NotificationItem({ notification: n, onClick }: NotificationItemProps) {
  const Icon = ICONS[n.referenceType ?? ""] || Bell;
  return (
    <button
      onClick={onClick}
      className={
        "w-full flex items-start gap-3 p-4 rounded border text-left transition-colors " +
        (n.read
          ? "border-border-base hover:bg-surface-soft"
          : "border-primary/30 bg-primary/[0.03]")
      }
    >
      <div className="h-10 w-10 rounded-full bg-primary/10 text-primary flex items-center justify-center shrink-0">
        <Icon className="h-4 w-4" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="font-medium text-sm">{n.title}</p>
        <p className="text-sm text-slate mt-0.5">{n.content}</p>
        <p className="text-xs text-slate mt-1">{formatDate(n.createdAt, true)}</p>
      </div>
      {!n.read && <span className="h-2 w-2 rounded-full bg-accent mt-2" />}
    </button>
  );
}
