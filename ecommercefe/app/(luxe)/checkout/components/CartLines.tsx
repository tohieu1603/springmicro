"use client";

import { hashId } from "@/lib/hashId";
import { useRouter } from "next/navigation";
import { makeBg } from "@/lib/illustrations";

import type { CartItem } from "../types";
import { priceVND } from "../utils/format";

interface CartLinesProps {
  items: CartItem[];
  busyVariant: string | null;
  warnings: string[];
  onChangeQty: (variantId: string, qty: number) => void;
  onRemove: (variantId: string) => void;
  onClearAll?: () => void;
}

const qtyBtn = (disabled: boolean): React.CSSProperties => ({
  width: 28,
  border: "none",
  background: disabled ? "#f5f5f5" : "#fff",
  cursor: disabled ? "not-allowed" : "pointer",
  fontSize: 16,
  lineHeight: 1,
  color: disabled ? "#999" : "#000",
});

export function CartLines({ items, busyVariant, warnings, onChangeQty, onRemove, onClearAll }: CartLinesProps) {
  const router = useRouter();
  return (
    <>
      {onClearAll && items.length > 0 && (
        <div className="co-cart-toolbar">
          <span>{items.length} sản phẩm</span>
          <button type="button" className="co-clear-all" onClick={onClearAll}>
            Xoá tất cả
          </button>
        </div>
      )}
      {items.map((it) => {
        const busy = busyVariant === it.variantId;
        return (
          <div key={it.id} className="co-item">
            <div
              className="co-item-img"
              style={
                it.variantImage
                  ? { backgroundImage: `url(${it.variantImage})`, backgroundSize: "cover", backgroundPosition: "center" }
                  : { backgroundImage: makeBg("topHandle", (hashId(it.variantId) * 13) % 8) }
              }
            />
            <div className="co-item-info">
              <div
                className="co-item-name"
                style={{ cursor: "pointer" }}
                onClick={() => router.push(`/product?id=${it.productId}`)}
              >
                {it.productName}
              </div>
              <div className="co-item-meta">
                <div>Style# {it.variantSku}</div>
              </div>
              <div className="co-availability">{it.warning ? it.warning : "AVAILABLE"}</div>
              <div>Enjoy complimentary delivery or Collect In Store.</div>
              <div className="co-item-actions">
                <a onClick={() => router.push(`/product?id=${it.productId}`)} style={{ cursor: "pointer" }}>
                  <u>Edit</u>
                </a>
                <span>|</span>
                <a onClick={() => !busy && onRemove(it.variantId)} style={{ cursor: busy ? "wait" : "pointer" }}>
                  <u>Remove</u>
                </a>
              </div>
            </div>
            <div className="co-qty">
              <span>QTY:</span>
              <div
                role="group"
                aria-label="Số lượng"
                style={{
                  display: "inline-flex",
                  alignItems: "stretch",
                  border: "1px solid #ccc",
                  borderRadius: 2,
                  overflow: "hidden",
                  height: 30,
                  opacity: busy ? 0.5 : 1,
                  pointerEvents: busy ? "none" : "auto",
                }}
              >
                <button
                  type="button"
                  aria-label="Giảm số lượng"
                  onClick={() => onChangeQty(it.variantId, Math.max(1, it.quantity - 1))}
                  disabled={busy || it.quantity <= 1}
                  style={qtyBtn(busy || it.quantity <= 1)}
                >
                  −
                </button>
                <input
                  type="number"
                  min={1}
                  max={999}
                  value={it.quantity}
                  disabled={busy}
                  onChange={(e) => {
                    const n = Number(e.target.value);
                    if (Number.isFinite(n) && n > 0 && n !== it.quantity) onChangeQty(it.variantId, n);
                  }}
                  style={{
                    width: 44, border: "none", borderLeft: "1px solid #ccc",
                    borderRight: "1px solid #ccc", textAlign: "center",
                    fontSize: 13, outline: "none", appearance: "textfield",
                  }}
                />
                <button
                  type="button"
                  aria-label="Tăng số lượng"
                  onClick={() => onChangeQty(it.variantId, it.quantity + 1)}
                  disabled={busy}
                  style={qtyBtn(busy)}
                >
                  +
                </button>
              </div>
            </div>
            <div className="co-item-price">{priceVND(it.subtotal ?? Number(it.unitPrice) * it.quantity)}</div>
          </div>
        );
      })}

      {warnings.length > 0 && (
        <div style={{ padding: "12px 20px", color: "#a05" }}>
          {warnings.map((w, i) => <div key={i}>• {w}</div>)}
        </div>
      )}
    </>
  );
}
