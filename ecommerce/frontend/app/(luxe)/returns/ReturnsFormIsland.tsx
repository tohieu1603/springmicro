"use client";

import { useReturns } from "./hooks/useReturns";
import { ReturnForm, SuccessState } from "./components";

/**
 * CSR island for the form interaction only. The surrounding hero text +
 * policy block are SSR'd by page.tsx so the static content is in the
 * initial HTML for SEO and faster paint.
 */
export function ReturnsFormIsland() {
  const vm = useReturns();
  if (vm.phase === "submitted") return <SuccessState />;
  return (
    <ReturnForm
      form={vm.form}
      setForm={vm.setForm}
      submitting={vm.phase === "submitting"}
      onSubmit={vm.submit}
    />
  );
}
