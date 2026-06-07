import { api } from "@/lib/api/client";
import type { Address } from "../types";

const BASE = "/api/user-profiles/me/addresses";

export class AddressesApi {
  static async list(): Promise<Address[]> {
    const res = await api.get<Address[] | { data: Address[] }>(BASE);
    const body = res.data as { data?: Address[] };
    return body.data ?? (res.data as Address[]);
  }

  static async create(form: Partial<Address>): Promise<Address> {
    const res = await api.post<Address | { data: Address }>(BASE, form);
    const body = res.data as { data?: Address };
    return (body.data ?? (res.data as Address));
  }

  static async update(id: string, form: Partial<Address>): Promise<void> {
    await api.patch(`${BASE}/${id}`, form);
  }

  static async setDefault(id: string): Promise<void> {
    await api.post(`${BASE}/${id}/set-default`);
  }

  static async remove(id: string): Promise<void> {
    await api.delete(`${BASE}/${id}`);
  }
}
