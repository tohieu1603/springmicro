"use client";

interface LookupFormProps {
  form: { orderNumber: string; phone: string };
  setForm: (f: { orderNumber: string; phone: string }) => void;
  loading: boolean;
  onSubmit: () => void;
}

export function LookupForm({ form, setForm, loading, onSubmit }: LookupFormProps) {
  return (
    <form className="track-form" onSubmit={(e) => { e.preventDefault(); onSubmit(); }}>
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
        <label>Recipient phone</label>
        <input
          placeholder="09xxxxxxxx"
          value={form.phone}
          onChange={(e) => setForm({ ...form, phone: e.target.value })}
          required
        />
      </div>
      <button type="submit" className="track-submit" disabled={loading}>
        {loading ? "LOOKING UP…" : "TRACK ORDER"}
      </button>
    </form>
  );
}
