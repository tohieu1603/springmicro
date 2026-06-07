import { api } from "@/lib/api/client";

export interface ProfileForm {
  fullName: string;
  email: string;
  phone: string;
  dob: string;
}

interface BeMe {
  fullName?: string;
  email?: string;
  phone?: string;
  dob?: string;
}

export class ProfileApi {
  static async fetch(): Promise<ProfileForm> {
    const res = await api.get<{ data?: BeMe } & BeMe>("/api/auth/me");
    const body = res.data as { data?: BeMe } & BeMe;
    const me = body.data ?? body;
    return {
      fullName: me.fullName ?? "",
      email: me.email ?? "",
      phone: me.phone ?? "",
      dob: me.dob ?? "",
    };
  }

  static async save(form: ProfileForm): Promise<void> {
    await api.patch("/api/auth/me", form);
  }
}
