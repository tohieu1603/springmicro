import { api } from "@/lib/api/client";
import type { NotificationItem, NotificationsPage } from "../types";

export class NotificationsApi {
  static async getRecent(size = 10): Promise<NotificationItem[]> {
    const res = await api.get<NotificationsPage>(`/api/notifications/my?page=0&size=${size}`);
    return res.data.content ?? [];
  }

  static async getUnreadCount(): Promise<number> {
    const res = await api.get<{ count: number }>(`/api/notifications/my/unread-count`);
    return Number(res.data.count ?? 0);
  }

  static async markRead(id: string): Promise<void> {
    await api.put(`/api/notifications/${id}/read`, {});
  }

  static async markAllRead(): Promise<void> {
    await api.put(`/api/notifications/my/read-all`, {});
  }
}
