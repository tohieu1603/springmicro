"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

import { useProfile } from "./hooks/useProfile";
import { ProfileForm } from "./components";

export function ProfileClient() {
  const vm = useProfile();
  if (vm.loading) return <div className="h-64 animate-pulse rounded bg-surface-container" />;

  return (
    <Card>
      <CardHeader>
        <CardTitle>Hồ sơ cá nhân</CardTitle>
      </CardHeader>
      <CardContent>
        <ProfileForm
          form={vm.form}
          setForm={vm.setForm}
          saving={vm.saving}
          onSubmit={vm.save}
        />
      </CardContent>
    </Card>
  );
}
