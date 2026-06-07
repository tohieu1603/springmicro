"use client";

import Link from "next/link";
import { Container } from "@/components/ui/container";

import { useRegister } from "./hooks/useRegister";
import { RegisterForm } from "./components";

export function RegisterClient() {
  const vm = useRegister();
  return (
    <Container className="py-12 max-w-md">
      <div className="rounded border border-border-base bg-white p-8 shadow-soft">
        <h1 className="text-h2-d text-center">Tạo tài khoản</h1>
        <p className="text-center text-sm text-slate mt-1">Mất chưa đến 30 giây</p>

        <RegisterForm
          form={vm.form}
          setForm={vm.setForm}
          submitting={vm.submitting}
          onSubmit={vm.submit}
        />

        <p className="text-center text-sm mt-6">
          Đã có tài khoản?{" "}
          <Link href="/login" className="font-semibold text-primary hover:text-accent">
            Đăng nhập
          </Link>
        </p>
      </div>
    </Container>
  );
}
