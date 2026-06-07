import { api } from "@/lib/api/client";
import type { Notification } from "../types";

interface ListResp {
  content?: Notification[];
  data?: { content?: Notification[] };
}

export class AccountNotificationsApi {
  static async list(size = 50): Promise<Notification[]> {
    const res = await api.get<ListResp>(`/api/notifications/my?size=${size}`);
    const body = res.data;
    return body.data?.content ?? body.content ?? [];
  }

  static async markRead(id: string): Promise<void> {
    await api.post(`/api/notifications/${id}/read`);
  }
}
