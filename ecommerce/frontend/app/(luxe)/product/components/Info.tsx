"use client";

import { VariantPicker } from "./VariantPicker";
import { QtyStepper } from "./QtyStepper";
import type { AttrGroup, BeProduct, BeVariant, PickedAttrs } from "../types";

interface InfoProps {
  product: BeProduct;
  displayPrice: string;
  groups: AttrGroup[];
  picked: PickedAttrs;
  selVariant: BeVariant | null;
  fullySelected: boolean;
  qty: number;
  qtyWarn: string | null;
  maxQty: number;
  stockLabel: string | null;
  adding: boolean;
  onPick: (attrId: string, valId: string) => void;
  onQtyChange: (next: number) => void;
  isReachable: (attrId: string, valId: string) => boolean;
  onAddToBag: () => void;
}

/**
 * The right-hand info panel: brand tag, name, price, variant picker, qty
 * stepper, stock label, Add-to-Bag CTA. Pure presentational — every action
 * is a callback prop, every value comes from the hook via the parent.
 */
export function Info(p: InfoProps) {
  const outOfStock = p.selVariant != null && (p.selVariant.quantity ?? 0) <= 0;
  const cannotAdd = p.adding
    || (p.groups.length > 0 && !p.fullySelected)
    || outOfStock;

  return (
    <section className="pd-info">
      <div className="pd-info-grid">
        <div className="pd-left">
          {p.product.brand && <div className="pd-tag">{p.product.brand.toUpperCase()}</div>}
          <h1 className="pd-name">{p.product.name}</h1>
          <div className="pd-price">{p.displayPrice}</div>

          <VariantPicker
            groups={p.groups}
            picked={p.picked}
            isReachable={p.isReachable}
            onPick={p.onPick}
          />

          <QtyStepper
            value={p.qty}
            max={p.maxQty}
            warning={p.qtyWarn}
            onChange={p.onQtyChange}
          />

          {p.stockLabel && (
            <div className={`pd-stock${(p.selVariant?.quantity ?? 0) > 0 ? "" : " out"}`}>{p.stockLabel}</div>
          )}
        </div>

        <div className="pd-right">
          <button className="pd-add" onClick={p.onAddToBag} disabled={cannotAdd}>
            <svg
              className="ico"
              viewBox="0 0 24 24"
              style={{ width: 16, height: 16, stroke: "#fff", fill: "none", strokeWidth: 1.6 }}
            >
              <path d="M6 7h12l-1 13H7L6 7z" />
              <path d="M9 7V5a3 3 0 016 0v2" />
            </svg>
            {p.adding ? "ADDING…" : outOfStock ? "OUT OF STOCK" : "ADD TO BAG"}
          </button>

          <p className="pd-shipping">
            <svg viewBox="0 0 24 24" className="pd-icon">
              <path d="M3 7h13l3 4v6h-2a2 2 0 01-4 0H8a2 2 0 01-4 0H3V7z" stroke="currentColor" fill="none" strokeWidth="1.4" />
            </svg>
            Complimentary Collect in Store available in 1–2 business days.
          </p>

          <a href="#" className="pd-link">
            <svg viewBox="0 0 24 24" className="pd-icon">
              <path d="M5 4h3l2 5-2 1a11 11 0 006 6l1-2 5 2v3a2 2 0 01-2 2A16 16 0 013 6a2 2 0 012-2z" stroke="currentColor" fill="none" strokeWidth="1.4" />
            </svg>
            <u>Order by Phone</u>
          </a>
          <a href="#" className="pd-link">
            <svg viewBox="0 0 24 24" className="pd-icon">
              <path d="M12 21s-7-7.5-7-12a7 7 0 0114 0c0 4.5-7 12-7 12z" stroke="currentColor" fill="none" strokeWidth="1.4" />
              <circle cx="12" cy="9" r="2.5" stroke="currentColor" fill="none" strokeWidth="1.4" />
            </svg>
            <u>Find in store and Book an appointment</u>
          </a>

          <p className="pd-foot">
            Complimentary Shipping &amp; Collect in Store, Complimentary Exchanges
            &amp; Returns, Secure Payments and Signature Packaging
          </p>
        </div>
      </div>

      {p.product.description && (
        <div className="pd-desc">
          <h2>PRODUCT DESCRIPTION</h2>
          <div className="pd-style">Style {p.product.slug.toUpperCase()}</div>
          <p>{p.product.description}</p>
        </div>
      )}
    </section>
  );
}
