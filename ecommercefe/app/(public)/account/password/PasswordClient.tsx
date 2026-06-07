"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

import { usePassword } from "./hooks/usePassword";
import { PasswordForm } from "./components";

export function PasswordClient() {
  const vm = usePassword();
  return (
    <Card>
      <CardHeader>
        <CardTitle>Đổi mật khẩu</CardTitle>
      </CardHeader>
      <CardContent>
        <PasswordForm
          form={vm.form}
          setForm={vm.setForm}
          saving={vm.saving}
          onSubmit={vm.submit}
        />
      </CardContent>
    </Card>
  );
}
