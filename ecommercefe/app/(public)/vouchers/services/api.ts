import "server-only";
import { fetchServer } from "@/lib/api/server";
import type { Page, Voucher } from "@/lib/api/types";

export class PublicVouchersApi {
  static async listActive(size = 24): Promise<Voucher[]> {
    const res = await fetchServer<Page<Voucher>>(`/api/vouchers/active?page=0&size=${size}`).catch(
      () => null,
    );
    return res?.content ?? [];
  }
}
