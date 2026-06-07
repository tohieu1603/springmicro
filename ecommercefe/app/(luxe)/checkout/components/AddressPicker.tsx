"use client";

import { useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import type { Address } from "../types";
import { CheckoutApi } from "../services/api";
import { useVnAddress } from "../hooks/useVnAddress";

interface Props {
  addresses: Address[];
  selectedId: string | null;
  onSelect: (id: string) => void;
  onReload: (preferId?: string) => Promise<void>;
}

/**
 * Address picker for the checkout page. Renders all saved addresses as radio
 * cards (Shopee-style); the "Thêm địa chỉ mới" toggle opens an inline form
 * with cascading Vietnamese province/district/ward dropdowns powered by
 * provinces.open-api.vn so the names match the GHTK directory exactly.
 *
 * Full CRUD lives in /account/addresses — this widget keeps the surface
 * minimal (create + select). Edit/delete stays in the dedicated page.
 */
export function AddressPicker({ addresses, selectedId, onSelect, onReload }: Props) {
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);

  const vn = useVnAddress();
  const [provinceCode, setProvinceCode] = useState<number | null>(null);
  const [districtCode, setDistrictCode] = useState<number | null>(null);
  const [wardCode, setWardCode] = useState<number | null>(null);

  const [form, setForm] = useState({
    recipientName: "",
    recipientPhone: "",
    street: "",
    isDefault: false,
  });

  const set = <K extends keyof typeof form>(k: K, v: (typeof form)[K]) =>
    setForm((f) => ({ ...f, [k]: v }));

  const reset = () => {
    setForm({ recipientName: "", recipientPhone: "", street: "", isDefault: false });
    setProvinceCode(null);
    setDistrictCode(null);
    setWardCode(null);
    vn.selectProvince(null);
    vn.selectDistrict(null);
  };

  const handleProvince = (code: number | null) => {
    setProvinceCode(code);
    setDistrictCode(null);
    setWardCode(null);
    vn.selectProvince(code);
  };

  const handleDistrict = (code: number | null) => {
    setDistrictCode(code);
    setWardCode(null);
    vn.selectDistrict(code);
  };

  const submit = async () => {
    if (!form.recipientName.trim()) { toast.error("Vui lòng nhập họ tên"); return; }
    if (!form.recipientPhone.trim()) { toast.error("Vui lòng nhập số điện thoại"); return; }
    if (!form.street.trim()) { toast.error("Vui lòng nhập địa chỉ cụ thể"); return; }
    if (provinceCode == null) { toast.error("Vui lòng chọn Tỉnh/Thành phố"); return; }
    if (districtCode == null) { toast.error("Vui lòng chọn Quận/Huyện"); return; }
    if (wardCode == null) { toast.error("Vui lòng chọn Phường/Xã"); return; }

    const province = vn.provinces.find((p) => p.code === provinceCode)?.name ?? "";
    const district = vn.districts.find((d) => d.code === districtCode)?.name ?? "";
    const ward     = vn.wards.find((w) => w.code === wardCode)?.name ?? "";

    setSaving(true);
    try {
      const created = await CheckoutApi.createAddress({
        recipientName: form.recipientName.trim(),
        recipientPhone: form.recipientPhone.trim(),
        street: form.street.trim(),
        ward,
        district,
        city: province,
        country: "VN",
        isDefault: form.isDefault,
      });
      toast.success("Đã lưu địa chỉ");
      await onReload(created.id);
      setShowForm(false);
      reset();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Không lưu được địa chỉ");
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="co-block">
      <header className="co-block-head">
        <h3>ĐỊA CHỈ NHẬN HÀNG</h3>
        <Link href="/account/addresses" className="co-block-link">Quản lý ›</Link>
      </header>

      {addresses.length === 0 && !showForm && (
        <div className="co-empty-row">
          <div>Bạn chưa có địa chỉ nào.</div>
          <button type="button" className="co-link-btn" onClick={() => setShowForm(true)}>
            + Thêm địa chỉ mới
          </button>
        </div>
      )}

      <div className="co-addr-list">
        {addresses.map((a) => {
          const active = a.id === selectedId;
          return (
            <label key={a.id} className={`co-addr-card ${active ? "active" : ""}`}>
              <input
                type="radio"
                name="address"
                checked={active}
                onChange={() => onSelect(a.id)}
                style={{ marginTop: 4 }}
              />
              <div style={{ flex: 1 }}>
                <div className="co-addr-name">
                  {a.recipientName}
                  <span className="co-addr-phone"> · {a.recipientPhone}</span>
                  {a.isDefault && <span className="co-addr-default">Mặc định</span>}
                </div>
                <div className="co-addr-line">
                  {[a.street, a.ward, a.district, a.city, a.country].filter(Boolean).join(", ")}
                </div>
              </div>
            </label>
          );
        })}
      </div>

      {addresses.length > 0 && !showForm && (
        <button type="button" className="co-link-btn" onClick={() => setShowForm(true)}>
          + Thêm địa chỉ mới
        </button>
      )}

      {showForm && (
        <div className="co-addr-form">
          <div className="row2">
            <input placeholder="Họ và tên *" value={form.recipientName}
                   onChange={(e) => set("recipientName", e.target.value)} />
            <input placeholder="Số điện thoại *" value={form.recipientPhone}
                   onChange={(e) => set("recipientPhone", e.target.value)} />
          </div>

          <div className="row3">
            <select
              value={provinceCode ?? ""}
              onChange={(e) => handleProvince(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Tỉnh/Thành phố *</option>
              {vn.provinces.map((p) => (
                <option key={p.code} value={p.code}>{p.name}</option>
              ))}
            </select>

            <select
              value={districtCode ?? ""}
              disabled={!provinceCode || vn.loading}
              onChange={(e) => handleDistrict(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">
                {!provinceCode ? "Chọn tỉnh trước" : "Quận/Huyện *"}
              </option>
              {vn.districts.map((d) => (
                <option key={d.code} value={d.code}>{d.name}</option>
              ))}
            </select>

            <select
              value={wardCode ?? ""}
              disabled={!districtCode || vn.loading}
              onChange={(e) => setWardCode(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">
                {!districtCode ? "Chọn quận trước" : "Phường/Xã *"}
              </option>
              {vn.wards.map((w) => (
                <option key={w.code} value={w.code}>{w.name}</option>
              ))}
            </select>
          </div>

          <input placeholder="Số nhà, tên đường *" value={form.street}
                 onChange={(e) => set("street", e.target.value)} />

          <label className="co-addr-default-check">
            <input type="checkbox" checked={form.isDefault}
                   onChange={(e) => set("isDefault", e.target.checked)} />
            Đặt làm địa chỉ mặc định
          </label>

          <div style={{ display: "flex", gap: 8 }}>
            <button type="button" className="co-btn-secondary"
                    onClick={() => { setShowForm(false); reset(); }} disabled={saving}>
              Huỷ
            </button>
            <button type="button" className="co-btn-primary"
                    onClick={submit} disabled={saving}>
              {saving ? "Đang lưu..." : "Lưu địa chỉ"}
            </button>
          </div>
        </div>
      )}
    </section>
  );
}
