import { api } from "@/lib/api/client";

export class PasswordApi {
  static async change(currentPassword: string, newPassword: string): Promise<void> {
    await api.post("/api/auth/change-password", { currentPassword, newPassword });
  }
}
