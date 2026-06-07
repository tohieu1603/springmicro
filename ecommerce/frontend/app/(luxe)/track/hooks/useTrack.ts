"use client";

import { useCallback, useState } from "react";
import { toast } from "sonner";

import { TrackApi } from "../services/api";
import type { TrackPhase, Tracking } from "../types";

export const STEPS = ["PLACED", "PAID", "PACKING", "SHIPPED", "DELIVERED"];

interface UseTrackArgs {
  initialOrderNumber?: string;
  initialPhone?: string;
  initialData?: Tracking | null;
}

/**
 * God hook for /track. Owns form state, lookup phase, and the resolved
 * timeline. When the RSC page pre-fetched the timeline (`?o`+`?phone`
 * present), `initialData` seeds the view — no client-side fetch needed
 * and the SSR HTML already contains the rendered timeline.
 */
export function useTrack({ initialOrderNumber = "", initialPhone = "", initialData = null }: UseTrackArgs = {}) {
  const [form, setForm] = useState({
    orderNumber: initialOrderNumber,
    phone: initialPhone,
  });
  const [phase, setPhase] = useState<TrackPhase>(initialData ? "found" : "idle");
  const [data, setData] = useState<Tracking | null>(initialData);

  const lookup = useCallback(async () => {
    if (!form.orderNumber || !form.phone) {
      toast.error("Vui lòng nhập đủ thông tin");
      return;
    }
    setPhase("loading");
    setData(null);
    try {
      const t = await TrackApi.lookup(form.orderNumber, form.phone);
      setData(t);
      setPhase("found");
    } catch (e: unknown) {
      const err = e as { response?: { status?: number } };
      if (err.response?.status === 404) setPhase("not-found");
      else { setPhase("error"); toast.error("Không tra cứu được đơn"); }
    }
  }, [form.orderNumber, form.phone]);

  const currentStep = data ? Math.max(0, STEPS.indexOf(data.status)) : -1;

  return {
    form,
    setForm,
    phase,
    data,
    currentStep,
    steps: STEPS,
    lookup,
  };
}

export type TrackVM = ReturnType<typeof useTrack>;
