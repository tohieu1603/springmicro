"use client";

import { useCallback, useState } from "react";
import { toast } from "sonner";

import { ReturnsApi } from "../services/api";
import type { ReturnFormState, ReturnPhase } from "../types";

export function useReturns() {
  const [form, setForm] = useState<ReturnFormState>({
    orderNumber: "",
    reason: "WRONG_SIZE",
    description: "",
    email: "",
  });
  const [phase, setPhase] = useState<ReturnPhase>("idle");

  const submit = useCallback(async () => {
    if (!form.orderNumber || !form.description) {
      toast.error("Vui lòng điền đủ thông tin bắt buộc");
      return;
    }
    setPhase("submitting");
    try {
      const orderId = await ReturnsApi.resolveOrderId(form.orderNumber);
      if (!orderId) {
        toast.error("Không tìm thấy đơn hàng. Kiểm tra lại số đơn.");
        setPhase("idle");
        return;
      }
      await ReturnsApi.submit(orderId, {
        reason: form.reason,
        description: form.description,
        contactEmail: form.email,
      });
      setPhase("submitted");
      toast.success("Đã gửi yêu cầu đổi/trả");
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message ?? "Gửi yêu cầu thất bại");
      setPhase("idle");
    }
  }, [form]);

  return {
    form,
    setForm,
    phase,
    submit,
  };
}

export type ReturnsVM = ReturnType<typeof useReturns>;
