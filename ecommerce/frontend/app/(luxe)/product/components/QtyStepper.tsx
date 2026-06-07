"use client";

import { useEffect, useState } from "react";

interface QtyStepperProps {
  value: number;
  max: number;                              // 0 = no variant selected, stepper disabled
  warning: string | null;
  onChange: (next: number) => void;
}

/**
 * Shopee-style − N + stepper. Typing is allowed; out-of-range values are
 * clamped + a warning is surfaced via `onChange` (parent owns the warning
 * string so server-side messages can override it).
 */
export function QtyStepper({ value, max, warning, onChange }: QtyStepperProps) {
  const [draft, setDraft] = useState(String(value));

  // Sync external value changes (e.g. after picking a different variant).
  useEffect(() => { setDraft(String(value)); }, [value]);

  const disabled = max <= 0;
  const atMin = value <= 1;
  const atMax = max > 0 && value >= max;

  const commit = (s: string) => {
    if (s === "") { onChange(1); return; }
    const n = Number(s.replace(/\D+/g, ""));
    if (!Number.isFinite(n) || n < 1) { onChange(1); return; }
    onChange(n);
  };

  return (
    <div className="qty-stepper-wrap">
      <div className="pd-attr-label">Số lượng</div>
      <div className={`qty-stepper${disabled ? " disabled" : ""}`}>
        <button
          type="button"
          className="qty-btn"
          aria-label="Giảm số lượng"
          disabled={disabled || atMin}
          onClick={() => onChange(Math.max(1, value - 1))}
        >−</button>
        <input
          type="text"
          inputMode="numeric"
          className="qty-input"
          aria-label="Số lượng"
          value={draft}
          disabled={disabled}
          onChange={(e) => setDraft(e.target.value)}
          onBlur={(e) => commit(e.currentTarget.value)}
          onKeyDown={(e) => { if (e.key === "Enter") (e.currentTarget as HTMLInputElement).blur(); }}
        />
        <button
          type="button"
          className="qty-btn"
          aria-label="Tăng số lượng"
          disabled={disabled || atMax}
          onClick={() => onChange(value + 1)}
        >+</button>
        {max > 0 && <span className="qty-max">/ {max} có sẵn</span>}
      </div>
      {warning && <div className="qty-warn">{warning}</div>}
    </div>
  );
}
