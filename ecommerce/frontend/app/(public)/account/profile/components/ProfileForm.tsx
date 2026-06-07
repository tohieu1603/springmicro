"use client";

import { Button } from "@/components/ui/button";
import { Input, Field } from "@/components/ui/input";

import type { ProfileForm as ProfileFormState } from "../services/api";

interface ProfileFormProps {
  form: ProfileFormState;
  setForm: (f: ProfileFormState) => void;
  saving: boolean;
  onSubmit: () => void;
}

export function ProfileForm({ form, setForm, saving, onSubmit }: ProfileFormProps) {
  return (
    <form onSubmit={(e) => { e.preventDefault(); onSubmit(); }} className="grid sm:grid-cols-2 gap-4">
      <Field label="Họ và tên">
        <Input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
      </Field>
      <Field label="Email">
        <Input type="email" value={form.email} disabled />
      </Field>
      <Field label="Số điện thoại">
        <Input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
      </Field>
      <Field label="Ngày sinh">
        <Input type="date" value={form.dob} onChange={(e) => setForm({ ...form, dob: e.target.value })} />
      </Field>
      <div className="sm:col-span-2">
        <Button variant="cta" type="submit" loading={saving}>Lưu thay đổi</Button>
      </div>
    </form>
  );
}
