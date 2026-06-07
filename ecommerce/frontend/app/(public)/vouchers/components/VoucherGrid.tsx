import { Empty } from "@/components/ui/empty";
import type { Voucher } from "@/lib/api/types";
import { VoucherCard } from "./VoucherCard";

export function VoucherGrid({ list }: { list: Voucher[] }) {
  if (list.length === 0) {
    return <Empty title="Hiện chưa có mã nào hoạt động" className="mt-12" />;
  }
  return (
    <div className="mt-8 grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {list.map((v) => <VoucherCard key={v.id} voucher={v} />)}
    </div>
  );
}
