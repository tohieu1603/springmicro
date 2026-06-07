"use client";

import { useCallback, useState } from "react";
import { toast } from "sonner";

import { PasswordApi } from "../services/api";

export interface PasswordForm {
  current: string;
  next: string;
  confirm: string;
}

export function usePassword() {
  const [form, setForm] = useState<PasswordForm>({ current: "", next: "", confirm: "" });
  const [saving, setSaving] = useState(false);

  const submit = useCallback(async () => {
    if (form.next !== form.confirm) {
      toast.error("Mật khẩu xác nhận không khớp");
      return;
    }
    if (form.next.length < 8) {
      toast.error("Mật khẩu tối thiểu 8 ký tự");
      return;
    }
    setSaving(true);
    try {
      await PasswordApi.change(form.current, form.next);
      toast.success("Đã đổi mật khẩu — vui lòng đăng nhập lại");
      setForm({ current: "", next: "", confirm: "" });
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Đổi mật khẩu thất bại");
    } finally {
      setSaving(false);
    }
  }, [form]);

  return { form, setForm, saving, submit };
}

export type PasswordVM = ReturnType<typeof usePassword>;
