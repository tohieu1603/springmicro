"use client";

import { Button } from "@/components/ui/button";
import { Input, Field } from "@/components/ui/input";

import type { PasswordForm as PasswordFormState } from "../hooks/usePassword";

interface PasswordFormProps {
  form: PasswordFormState;
  setForm: (f: PasswordFormState) => void;
  saving: boolean;
  onSubmit: () => void;
}

export function PasswordForm({ form, setForm, saving, onSubmit }: PasswordFormProps) {
  return (
    <form onSubmit={(e) => { e.preventDefault(); onSubmit(); }} className="space-y-4 max-w-md">
      <Field label="Mật khẩu hiện tại" required>
        <Input
          type="password"
          autoComplete="current-password"
          value={form.current}
          onChange={(e) => setForm({ ...form, current: e.target.value })}
        />
      </Field>
      <Field label="Mật khẩu mới" required hint="Tối thiểu 8 ký tự">
        <Input
          type="password"
          autoComplete="new-password"
          value={form.next}
          onChange={(e) => setForm({ ...form, next: e.target.value })}
        />
      </Field>
      <Field label="Xác nhận mật khẩu mới" required>
        <Input
          type="password"
          autoComplete="new-password"
          value={form.confirm}
          onChange={(e) => setForm({ ...form, confirm: e.target.value })}
        />
      </Field>
      <Button type="submit" variant="cta" loading={saving}>Đổi mật khẩu</Button>
    </form>
  );
}
