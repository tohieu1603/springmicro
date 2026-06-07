import { Card, CardContent } from "@/components/ui/card";
import { formatDate, formatVnd } from "@/lib/utils";
import type { Voucher } from "@/lib/api/types";

import { CopyVoucher } from "../copy-voucher";

export function VoucherCard({ voucher: v }: { voucher: Voucher }) {
  return (
    <Card className="overflow-hidden">
      <div className="bg-gradient-to-br from-primary to-primary-dark text-white p-5 flex justify-between items-center">
        <div>
          <p className="text-xs uppercase tracking-widest text-accent font-semibold">
            {v.type === "PERCENTAGE" ? "Giảm %" : "Giảm tiền"}
          </p>
          <p className="text-3xl font-bold mt-1">
            {v.type === "PERCENTAGE" ? `${v.discountValue}%` : formatVnd(v.discountValue)}
          </p>
        </div>
        <CopyVoucher code={v.code} />
      </div>
      <CardContent className="text-sm space-y-1">
        {v.minOrderAmount && (
          <p>Đơn tối thiểu: <b>{formatVnd(v.minOrderAmount)}</b></p>
        )}
        {v.maxDiscountAmount && (
          <p>Giảm tối đa: <b>{formatVnd(v.maxDiscountAmount)}</b></p>
        )}
        {v.endDate && <p className="text-slate">HSD: {formatDate(v.endDate)}</p>}
        {v.description && <p className="text-slate mt-2">{v.description}</p>}
        {v.usageLimit && (
          <div className="mt-2">
            <div className="h-1.5 rounded-full bg-surface-container overflow-hidden">
              <div
                className="h-full bg-accent"
                style={{ width: `${Math.min(100, (v.usedCount / v.usageLimit) * 100)}%` }}
              />
            </div>
            <p className="text-[11px] text-slate mt-1">
              Đã dùng {v.usedCount}/{v.usageLimit}
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
