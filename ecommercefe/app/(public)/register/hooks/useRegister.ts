"use client";

import { useCallback, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { RegisterApi, splitFullName } from "../services/api";

export interface RegisterForm {
  fullName: string;
  username: string;
  email: string;
  password: string;
  confirm: string;
}

export function useRegister() {
  const router = useRouter();
  const [form, setForm] = useState<RegisterForm>({
    fullName: "",
    username: "",
    email: "",
    password: "",
    confirm: "",
  });
  const [submitting, setSubmitting] = useState(false);

  const submit = useCallback(async () => {
    if (form.password !== form.confirm) {
      toast.error("Mật khẩu xác nhận không khớp");
      return;
    }
    setSubmitting(true);
    try {
      const { firstName, lastName } = splitFullName(form.fullName, form.username);
      await RegisterApi.register({
        username: form.username,
        email: form.email,
        password: form.password,
        firstName,
        lastName,
      });
      toast.success("Đăng ký thành công, mời đăng nhập");
      router.replace(`/login?next=${encodeURIComponent("/")}`);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Đăng ký thất bại");
    } finally {
      setSubmitting(false);
    }
  }, [form, router]);

  return { form, setForm, submitting, submit };
}

export type RegisterVM = ReturnType<typeof useRegister>;
