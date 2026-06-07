"use client";

import { Button } from "@/components/ui/button";
import { Input, Field } from "@/components/ui/input";

import type { Address, EditMode } from "../types";

interface AddressFormProps {
  mode: EditMode;
  form: Partial<Address>;
  setForm: (f: Partial<Address>) => void;
  onSubmit: () => void;
  onCancel: () => void;
}

export function AddressForm({ mode, form, setForm, onSubmit, onCancel }: AddressFormProps) {
  if (!mode) return null;

  return (
    <form
      onSubmit={(e) => { e.preventDefault(); onSubmit(); }}
      className="mt-6 rounded border border-border-base p-4 grid grid-cols-1 sm:grid-cols-2 gap-3 bg-surface-soft"
    >
      <h3 className="sm:col-span-2 font-semibold">
        {mode === "new" ? "Thêm địa chỉ mới" : "Sửa địa chỉ"}
      </h3>
      <Field label="Người nhận" required>
        <Input
          value={form.recipientName || ""}
          onChange={(e) => setForm({ ...form, recipientName: e.target.value })}
          required
        />
      </Field>
      <Field label="Số điện thoại" required>
        <Input
          value={form.recipientPhone || ""}
          onChange={(e) => setForm({ ...form, recipientPhone: e.target.value })}
          required
        />
      </Field>
      <div className="sm:col-span-2">
        <Field label="Đường / Số nhà" required>
          <Input
            value={form.street || ""}
            onChange={(e) => setForm({ ...form, street: e.target.value })}
            required
          />
        </Field>
      </div>
      <Field label="Phường/Xã">
        <Input value={form.ward || ""} onChange={(e) => setForm({ ...form, ward: e.target.value })} />
      </Field>
      <Field label="Quận/Huyện">
        <Input value={form.district || ""} onChange={(e) => setForm({ ...form, district: e.target.value })} />
      </Field>
      <Field label="Tỉnh/Thành" required>
        <Input
          value={form.city || ""}
          onChange={(e) => setForm({ ...form, city: e.target.value })}
          required
        />
      </Field>
      <Field label="Quốc gia">
        <Input
          value={form.country || "Vietnam"}
          onChange={(e) => setForm({ ...form, country: e.target.value })}
        />
      </Field>
      <div className="sm:col-span-2 flex gap-2">
        <Button variant="cta" type="submit">Lưu</Button>
        <Button type="button" variant="ghost" onClick={onCancel}>Hủy</Button>
      </div>
    </form>
  );
}
