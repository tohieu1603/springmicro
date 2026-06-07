"use client";

import Link from "next/link";
import { Container } from "@/components/ui/container";

import { useLogin } from "./hooks/useLogin";
import { LoginForm } from "./components";

export function LoginClient() {
  const vm = useLogin();
  return (
    <Container className="py-12 max-w-md">
      <div className="rounded border border-border-base bg-white p-8 shadow-soft">
        <h1 className="text-h2-d text-center">Đăng nhập</h1>
        <p className="text-center text-sm text-slate mt-1">
          Chào mừng bạn quay lại Luxury Mart
        </p>

        <LoginForm
          form={vm.form}
          setForm={vm.setForm}
          submitting={vm.submitting}
          onSubmit={vm.submit}
        />

        <p className="text-center text-sm mt-6">
          Chưa có tài khoản?{" "}
          <Link href="/register" className="font-semibold text-primary hover:text-accent">
            Đăng ký
          </Link>
        </p>
      </div>
    </Container>
  );
}
