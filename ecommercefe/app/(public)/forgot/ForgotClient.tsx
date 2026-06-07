"use client";

import Link from "next/link";
import { Container } from "@/components/ui/container";

import { useForgot } from "./hooks/useForgot";
import { ForgotForm, SentNotice } from "./components";

export function ForgotClient() {
  const vm = useForgot();
  return (
    <Container className="py-12 max-w-md">
      <div className="rounded-lg border border-border-base bg-white p-8 shadow-soft">
        <h1 className="text-h2-d text-center">Quên mật khẩu?</h1>
        <p className="text-center text-sm text-slate mt-1">
          Nhập email đăng ký để nhận liên kết khôi phục
        </p>

        {vm.sent ? (
          <SentNotice email={vm.email} />
        ) : (
          <ForgotForm
            email={vm.email}
            setEmail={vm.setEmail}
            loading={vm.loading}
            onSubmit={vm.submit}
          />
        )}

        <p className="text-center text-sm mt-6">
          Nhớ ra rồi?{" "}
          <Link href="/login" className="font-semibold text-primary hover:text-accent">
            Đăng nhập
          </Link>
        </p>
      </div>
    </Container>
  );
}
