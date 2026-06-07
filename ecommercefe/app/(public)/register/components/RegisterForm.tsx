"use client";

import { Button } from "@/components/ui/button";
import { Input, Field } from "@/components/ui/input";

import type { RegisterForm as RegisterFormState } from "../hooks/useRegister";

interface RegisterFormProps {
  form: RegisterFormState;
  setForm: (f: RegisterFormState) => void;
  submitting: boolean;
  onSubmit: () => void;
}

export function RegisterForm({ form, setForm, submitting, onSubmit }: RegisterFormProps) {
  return (
    <form onSubmit={(e) => { e.preventDefault(); onSubmit(); }} className="mt-6 space-y-4">
      <Field label="Họ và tên" required>
        <Input
          value={form.fullName}
          onChange={(e) => setForm({ ...form, fullName: e.target.value })}
        />
      </Field>
      <Field label="Tên đăng nhập" required hint="Tối thiểu 4 ký tự, không dấu">
        <Input
          autoComplete="username"
          value={form.username}
          onChange={(e) => setForm({ ...form, username: e.target.value })}
        />
      </Field>
      <Field label="Email" required>
        <Input
          type="email"
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
        />
      </Field>
      <Field label="Mật khẩu" required hint="Tối thiểu 8 ký tự">
        <Input
          type="password"
          autoComplete="new-password"
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
        />
      </Field>
      <Field label="Xác nhận mật khẩu" required>
        <Input
          type="password"
          autoComplete="new-password"
          value={form.confirm}
          onChange={(e) => setForm({ ...form, confirm: e.target.value })}
        />
      </Field>

      <Button type="submit" variant="cta" size="lg" className="w-full" loading={submitting}>
        Đăng ký
      </Button>
    </form>
  );
}
