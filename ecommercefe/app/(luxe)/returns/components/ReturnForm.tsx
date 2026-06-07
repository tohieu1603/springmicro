"use client";

import type { ReturnFormState } from "../types";
import { REASONS } from "../types";

interface ReturnFormProps {
  form: ReturnFormState;
  setForm: (f: ReturnFormState) => void;
  submitting: boolean;
  onSubmit: () => void;
}

export function ReturnForm({ form, setForm, submitting, onSubmit }: ReturnFormProps) {
  return (
    <form className="track-form returns-form" onSubmit={(e) => { e.preventDefault(); onSubmit(); }}>
      <div className="track-field">
        <label>Order number</label>
        <input
          placeholder="ORD-20260512-000123"
          value={form.orderNumber}
          onChange={(e) => setForm({ ...form, orderNumber: e.target.value.toUpperCase() })}
          required
        />
      </div>
      <div className="track-field">
        <label>Contact email</label>
        <input
          type="email"
          placeholder="you@example.com"
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          required
        />
      </div>
      <div className="track-field full">
        <label>Reason</label>
        <select
          value={form.reason}
          onChange={(e) => setForm({ ...form, reason: e.target.value as ReturnFormState["reason"] })}
        >
          {REASONS.map((r) => (
            <option key={r.v} value={r.v}>{r.label}</option>
          ))}
        </select>
      </div>
      <div className="track-field full">
        <label>Tell us more</label>
        <textarea
          rows={5}
          placeholder="Chuyện gì đã xảy ra? Chi tiết giúp chúng tôi xử lý nhanh hơn."
          value={form.description}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
          required
        />
      </div>
      <button type="submit" className="track-submit full" disabled={submitting}>
        {submitting ? "SUBMITTING…" : "SUBMIT REQUEST"}
      </button>
    </form>
  );
}
