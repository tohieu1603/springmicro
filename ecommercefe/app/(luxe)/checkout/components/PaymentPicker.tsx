"use client";

import type { PaymentMethod } from "../types";

interface Props {
  methods: PaymentMethod[];
  selected: PaymentMethod["code"];
  onSelect: (code: PaymentMethod["code"]) => void;
}

/**
 * Radio-card picker for payment methods. Mirrors Shopee/Lazada: one card per
 * method, big icon, name + short description. Disabled methods render greyed
 * out — kept visible so the user can ask the merchant to enable them later.
 */
export function PaymentPicker({ methods, selected, onSelect }: Props) {
  return (
    <section className="co-block">
      <header className="co-block-head">
        <h3>PHƯƠNG THỨC THANH TOÁN</h3>
      </header>

      <div className="co-pay-list">
        {methods.map((m) => {
          const active = m.code === selected;
          const disabled = !m.enabled;
          return (
            <button
              key={m.code}
              type="button"
              disabled={disabled}
              onClick={() => onSelect(m.code)}
              className={`co-pay-card ${active ? "active" : ""} ${disabled ? "disabled" : ""}`}
            >
              <span className="material-symbols-outlined co-pay-icon">{m.icon}</span>
              <span className="co-pay-text">
                <span className="co-pay-name">{m.name}</span>
                <span className="co-pay-desc">{m.description}</span>
              </span>
              {active && <span className="material-symbols-outlined co-pay-check">check_circle</span>}
            </button>
          );
        })}
      </div>
    </section>
  );
}
