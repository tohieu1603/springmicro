"use client";

import { useCallback, useState } from "react";
import { useSearchParams } from "next/navigation";
import { toast } from "sonner";

import { LoginApi } from "../services/api";

export interface LoginForm {
  usernameOrEmail: string;
  password: string;
}

export function useLogin() {
  const params = useSearchParams();
  const next = params.get("next");

  const [form, setForm] = useState<LoginForm>({ usernameOrEmail: "", password: "" });
  const [submitting, setSubmitting] = useState(false);

  const submit = useCallback(async () => {
    setSubmitting(true);
    try {
      const roles = await LoginApi.login(form.usernameOrEmail, form.password);
      const isAdmin = roles.some((r) => r === "ADMIN" || r === "ROLE_ADMIN");
      toast.success("Đăng nhập thành công");
      const target = isAdmin ? "/admin" : next || "/";
      // Hard nav — `router.replace` waits for the destination's RSC payload to
      // stream back; on a cold backend that's 5–10s of frozen page. Hard
      // navigation lets the browser immediately render loading.tsx.
      window.location.assign(target);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Đăng nhập thất bại");
      setSubmitting(false);
    }
  }, [form, next]);

  return { form, setForm, submitting, submit };
}

export type LoginVM = ReturnType<typeof useLogin>;
