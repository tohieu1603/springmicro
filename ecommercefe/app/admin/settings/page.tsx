"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Card, InputNumber, Switch, Tabs, Tag } from "antd";
import { toast } from "sonner";

import { api } from "@/lib/api/client";
import { qk } from "@/lib/query/keys";

interface PaymentMethodRow {
  code: string;
  name: string;
  description?: string;
  enabled: boolean;
  displayOrder: number;
}

interface CarrierRow {
  code: string;
  name: string;
  enabled: boolean;
  supportsCod: boolean;
  etaHours: number;
  displayOrder: number;
}

/**
 * Admin platform settings — every row is a useMutation that optimistically
 * patches the cache via {@code setQueryData} and rolls back on error. The
 * storefront's payment / carrier queries share the same keys so toggling
 * here reflects on the next checkout fetch.
 */
export default function SettingsAdmin() {
  return (
    <Tabs
      items={[
        { key: "pm", label: "Phương thức thanh toán", children: <PaymentMethodsTab /> },
        { key: "sh", label: "Đơn vị vận chuyển",      children: <CarriersTab /> },
      ]}
    />
  );
}

async function unwrap<T>(res: { data: T | { data: T } }): Promise<T> {
  const body = res.data as { data?: T };
  return (body.data ?? (res.data as T));
}

function PaymentMethodsTab() {
  const qc = useQueryClient();

  const { data: rows = [], isLoading } = useQuery({
    queryKey: qk.paymentMethodsAdmin(),
    queryFn: async () => {
      const res = await api.get<PaymentMethodRow[] | { data: PaymentMethodRow[] }>(
        "/api/payments/methods/admin",
      );
      return (await unwrap<PaymentMethodRow[]>(res)) || [];
    },
    staleTime: 30_000,
  });

  const patchMut = useMutation({
    mutationFn: ({ code, patch }: { code: string; patch: Partial<PaymentMethodRow> }) =>
      api.patch(`/api/payments/methods/admin/${code}`, patch),
    onMutate: async ({ code, patch }) => {
      await qc.cancelQueries({ queryKey: qk.paymentMethodsAdmin() });
      const prev = qc.getQueryData<PaymentMethodRow[]>(qk.paymentMethodsAdmin()) ?? [];
      qc.setQueryData<PaymentMethodRow[]>(qk.paymentMethodsAdmin(),
        prev.map((r) => (r.code === code ? { ...r, ...patch } : r)));
      // Also invalidate the storefront catalog so checkout reflects the change.
      qc.invalidateQueries({ queryKey: qk.paymentMethods() });
      return { prev };
    },
    onError: (_e, _v, ctx) => {
      if (ctx?.prev) qc.setQueryData(qk.paymentMethodsAdmin(), ctx.prev);
      toast.error("Cập nhật thất bại");
    },
    onSuccess: (_d, { code }) => toast.success(`Đã cập nhật ${code}`),
  });

  return (
    <Card title={isLoading ? "Đang tải..." : `${rows.length} phương thức`}>
      <div className="space-y-3">
        {rows.map((m) => (
          <div key={m.code} className="flex items-center gap-3 py-2 border-b last:border-0">
            <div className="flex-1">
              <div className="flex items-center gap-2">
                <Tag color={m.enabled ? "green" : "default"}>{m.code}</Tag>
                <span className="font-medium">{m.name}</span>
              </div>
              {m.description && <p className="text-xs text-slate mt-1">{m.description}</p>}
            </div>
            <span className="text-xs text-slate">Thứ tự:</span>
            <InputNumber
              size="small"
              min={0}
              max={9999}
              value={m.displayOrder}
              onBlur={(e) => {
                const n = Number((e.target as HTMLInputElement).value);
                if (Number.isFinite(n) && n !== m.displayOrder) {
                  patchMut.mutate({ code: m.code, patch: { displayOrder: n } });
                }
              }}
              style={{ width: 80 }}
            />
            <Switch
              checked={m.enabled}
              onChange={(v) => patchMut.mutate({ code: m.code, patch: { enabled: v } })}
            />
          </div>
        ))}
        {!isLoading && rows.length === 0 && (
          <p className="text-sm text-slate">Chưa có phương thức nào trong cấu hình yaml.</p>
        )}
      </div>
    </Card>
  );
}

function CarriersTab() {
  const qc = useQueryClient();

  const { data: rows = [], isLoading } = useQuery({
    queryKey: qk.shipping.carriersAdmin(),
    queryFn: async () => {
      const res = await api.get<CarrierRow[] | { data: CarrierRow[] }>(
        "/api/shipping/carriers/admin",
      );
      return (await unwrap<CarrierRow[]>(res)) || [];
    },
    staleTime: 30_000,
  });

  const patchMut = useMutation({
    mutationFn: ({ code, patch }: { code: string; patch: Partial<CarrierRow> }) =>
      api.patch(`/api/shipping/carriers/admin/${code}`, patch),
    onMutate: async ({ code, patch }) => {
      await qc.cancelQueries({ queryKey: qk.shipping.carriersAdmin() });
      const prev = qc.getQueryData<CarrierRow[]>(qk.shipping.carriersAdmin()) ?? [];
      qc.setQueryData<CarrierRow[]>(qk.shipping.carriersAdmin(),
        prev.map((r) => (r.code === code ? { ...r, ...patch } : r)));
      qc.invalidateQueries({ queryKey: qk.shipping.carriers() });
      return { prev };
    },
    onError: (_e, _v, ctx) => {
      if (ctx?.prev) qc.setQueryData(qk.shipping.carriersAdmin(), ctx.prev);
      toast.error("Cập nhật thất bại");
    },
    onSuccess: (_d, { code }) => toast.success(`Đã cập nhật ${code}`),
  });

  const addCarrier = async () => {
    const code = window.prompt("Mã carrier mới (vd: VIETTELPOST, JNT_EXPRESS):");
    if (!code) return;
    const name = window.prompt("Tên hiển thị:", code) || code;
    try {
      await api.patch(`/api/shipping/carriers/admin/${code}`, {
        name, enabled: false, supportsCod: true, etaHours: 48, displayOrder: 100,
      });
      toast.success(`Đã thêm ${code}`);
      qc.invalidateQueries({ queryKey: qk.shipping.carriersAdmin() });
      qc.invalidateQueries({ queryKey: qk.shipping.carriers() });
    } catch {
      toast.error("Thêm thất bại");
    }
  };

  return (
    <Card
      title={isLoading ? "Đang tải..." : `${rows.length} đơn vị vận chuyển`}
      extra={<Button onClick={addCarrier}>+ Thêm carrier</Button>}
    >
      <div className="space-y-3">
        {rows.map((c) => (
          <div key={c.code} className="flex flex-wrap items-center gap-3 py-2 border-b last:border-0">
            <div className="min-w-[200px]">
              <Tag color={c.enabled ? "green" : "default"}>{c.code}</Tag>
              <span className="font-medium ml-2">{c.name}</span>
            </div>
            <div className="flex items-center gap-2 text-sm">
              <span className="text-slate">ETA (giờ):</span>
              <InputNumber
                size="small" min={1} max={720}
                value={c.etaHours}
                onBlur={(e) => {
                  const n = Number((e.target as HTMLInputElement).value);
                  if (Number.isFinite(n) && n !== c.etaHours) {
                    patchMut.mutate({ code: c.code, patch: { etaHours: n } });
                  }
                }}
                style={{ width: 80 }}
              />
            </div>
            <div className="flex items-center gap-2 text-sm">
              <span className="text-slate">Thứ tự:</span>
              <InputNumber
                size="small" min={0} max={9999}
                value={c.displayOrder}
                onBlur={(e) => {
                  const n = Number((e.target as HTMLInputElement).value);
                  if (Number.isFinite(n) && n !== c.displayOrder) {
                    patchMut.mutate({ code: c.code, patch: { displayOrder: n } });
                  }
                }}
                style={{ width: 80 }}
              />
            </div>
            <div className="flex items-center gap-2 text-sm">
              <span className="text-slate">COD:</span>
              <Switch
                checked={c.supportsCod}
                onChange={(v) => patchMut.mutate({ code: c.code, patch: { supportsCod: v } })}
              />
            </div>
            <div className="ml-auto flex items-center gap-2">
              <span className="text-xs text-slate">Bật:</span>
              <Switch
                checked={c.enabled}
                onChange={(v) => patchMut.mutate({ code: c.code, patch: { enabled: v } })}
              />
            </div>
          </div>
        ))}
        {!isLoading && rows.length === 0 && (
          <p className="text-sm text-slate">Chưa có carrier nào. Click "+ Thêm carrier" để bắt đầu.</p>
        )}
      </div>
    </Card>
  );
}
