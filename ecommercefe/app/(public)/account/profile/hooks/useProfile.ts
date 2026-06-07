"use client";

import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";

import { ProfileApi, type ProfileForm } from "../services/api";

export function useProfile() {
  const [form, setForm] = useState<ProfileForm>({ fullName: "", email: "", phone: "", dob: "" });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const me = await ProfileApi.fetch();
        setForm(me);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const save = useCallback(async () => {
    setSaving(true);
    try {
      await ProfileApi.save(form);
      toast.success("Cập nhật hồ sơ thành công");
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Cập nhật thất bại");
    } finally {
      setSaving(false);
    }
  }, [form]);

  return { form, setForm, loading, saving, save };
}

export type ProfileVM = ReturnType<typeof useProfile>;
