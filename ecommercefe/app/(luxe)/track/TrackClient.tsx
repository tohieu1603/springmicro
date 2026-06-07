"use client";

import { useTrack } from "./hooks/useTrack";
import { EmptyState, LookupForm, Timeline } from "./components";
import type { Tracking } from "./types";

interface TrackClientProps {
  initialOrderNumber?: string;
  initialPhone?: string;
  initialData?: Tracking | null;
}

export function TrackClient(props: TrackClientProps) {
  const vm = useTrack(props);

  return (
    <section className="track-section">
      <div className="track-hero">
        <h1>TRACK YOUR ORDER</h1>
        <p>Nhập số đơn hàng và số điện thoại nhận hàng để theo dõi hành trình.</p>
      </div>

      <LookupForm
        form={vm.form}
        setForm={vm.setForm}
        loading={vm.phase === "loading"}
        onSubmit={vm.lookup}
      />

      {vm.phase === "not-found" && <EmptyState />}
      {vm.phase === "found" && vm.data && (
        <Timeline data={vm.data} currentStep={vm.currentStep} steps={vm.steps} />
      )}
    </section>
  );
}
