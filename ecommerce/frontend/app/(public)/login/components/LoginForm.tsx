"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input, Field } from "@/components/ui/input";

import type { LoginForm as LoginFormState } from "../hooks/useLogin";

interface LoginFormProps {
  form: LoginFormState;
  setForm: (f: LoginFormState) => void;
  submitting: boolean;
  onSubmit: () => void;
}

export function LoginForm({ form, setForm, submitting, onSubmit }: LoginFormProps) {
  return (
    <form onSubmit={(e) => { e.preventDefault(); onSubmit(); }} className="mt-6 space-y-4">
      <Field label="Tên đăng nhập hoặc email" required>
        <Input
          autoComplete="username"
          value={form.usernameOrEmail}
          onChange={(e) => setForm({ ...form, usernameOrEmail: e.target.value })}
        />
      </Field>
      <Field label="Mật khẩu" required>
        <Input
          type="password"
          autoComplete="current-password"
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
        />
      </Field>

      <div className="flex justify-between text-sm">
        <label className="flex items-center gap-2">
          <input type="checkbox" className="accent-primary" /> Ghi nhớ
        </label>
        <Link href="/forgot" className="text-primary hover:text-accent">Quên mật khẩu?</Link>
      </div>

      <Button type="submit" variant="cta" size="lg" className="w-full" loading={submitting}>
        Đăng nhập
      </Button>
    </form>
  );
}
