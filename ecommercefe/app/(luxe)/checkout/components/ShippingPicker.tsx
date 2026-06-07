"use client";

import type { ShippingQuote } from "../types";
import { priceVND } from "../utils/format";

interface Props {
  quote: ShippingQuote | null;
  loading: boolean;
  hasAddress: boolean;
}

/**
 * Shows the live shipping quote for the selected address. Currently single-
 * carrier (GHTK live) — the {@code source} field reveals whether the price
 * came from the real API or the local fallback so devs can spot rate-card
 * drift in staging.
 *
 * When the user has no address yet, prompts them to pick one before a quote
 * can be calculated.
 */
export function ShippingPicker({ quote, loading, hasAddress }: Props) {
  return (
    <section className="co-block">
      <header className="co-block-head">
        <h3>VẬN CHUYỂN</h3>
      </header>

      {!hasAddress && (
        <div className="co-empty-row">
          Chọn địa chỉ giao hàng để xem phí vận chuyển.
        </div>
      )}

      {hasAddress && loading && (
        <div className="co-empty-row">Đang tính phí vận chuyển…</div>
      )}

      {hasAddress && !loading && !quote && (
        <div className="co-empty-row" style={{ color: "#a05" }}>
          Không tính được phí — vui lòng thử lại sau.
        </div>
      )}

      {hasAddress && !loading && quote && (
        <div className="co-ship-card active">
          <span className="material-symbols-outlined co-ship-icon">local_shipping</span>
          <span className="co-ship-text">
            <span className="co-ship-name">
              {quote.carrier}
              {quote.source === "LOCAL_FALLBACK" && (
                <span className="co-ship-tag" title="Ước tính cục bộ, GHTK đang gián đoạn">
                  Ước tính
                </span>
              )}
            </span>
            <span className="co-ship-desc">
              Dự kiến {quote.deliveryTimeHours > 0
                ? `${Math.ceil(quote.deliveryTimeHours / 24)} ngày`
                : "1-3 ngày"} làm việc
            </span>
          </span>
          <span className="co-ship-fee">{priceVND(quote.fee)}</span>
        </div>
      )}
    </section>
  );
}
