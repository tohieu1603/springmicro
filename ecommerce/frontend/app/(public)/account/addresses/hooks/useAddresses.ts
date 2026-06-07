"use client";

import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";

import { AddressesApi } from "../services/api";
import type { Address, EditMode } from "../types";

export function useAddresses() {
  const [list, setList] = useState<Address[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<EditMode>(null);
  const [form, setForm] = useState<Partial<Address>>({});

  const load = useCallback(async () => {
    try {
      setList(await AddressesApi.list());
    } catch {
      // BE may not expose this — render empty.
      setList([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const openForm = useCallback((addr?: Address) => {
    setEditing(addr ?? "new");
    setForm(addr ?? { country: "Vietnam" });
  }, []);

  const closeForm = useCallback(() => {
    setEditing(null);
    setForm({});
  }, []);

  const save = useCallback(async () => {
    try {
      if (editing === "new") {
        await AddressesApi.create(form);
        toast.success("Đã thêm địa chỉ");
      } else if (editing) {
        await AddressesApi.update(editing.id, form);
        toast.success("Đã cập nhật");
      }
      closeForm();
      load();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Lưu thất bại");
    }
  }, [editing, form, load, closeForm]);

  const setDefault = useCallback(async (id: string) => {
    try {
      await AddressesApi.setDefault(id);
      load();
    } catch {
      toast.error("Không đặt được mặc định");
    }
  }, [load]);

  const remove = useCallback(async (id: string) => {
    if (!confirm("Xóa địa chỉ này?")) return;
    try {
      await AddressesApi.remove(id);
      toast.success("Đã xóa");
      load();
    } catch {
      toast.error("Không xóa được");
    }
  }, [load]);

  return {
    list,
    loading,
    editing,
    form,
    setForm,
    openForm,
    closeForm,
    save,
    setDefault,
    remove,
  };
}

export type AddressesVM = ReturnType<typeof useAddresses>;
