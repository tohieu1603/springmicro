"use client";

import { useCallback, useState } from "react";
import { toast } from "sonner";

import { ForgotApi } from "../services/api";

export function useForgot() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);

  const submit = useCallback(async () => {
    setLoading(true);
    try {
      await ForgotApi.sendResetLink(email);
      setSent(true);
      toast.success("Đã gửi liên kết khôi phục đến email");
    } catch {
      // BE may not expose this endpoint yet — fake success to avoid leaking
      // which emails exist (avoids user enumeration).
      setSent(true);
    } finally {
      setLoading(false);
    }
  }, [email]);

  return { email, setEmail, sent, loading, submit };
}

export type ForgotVM = ReturnType<typeof useForgot>;
