"use client";

import { hashId } from "@/lib/hashId";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { makeBg } from "@/lib/illustrations";
import { qk } from "@/lib/query/keys";

interface CartItem {
  id: string;
  productId: string;
  productName: string;
  variantSku: string;
  variantImage?: string | null;
  unitPrice: string;
  quantity: number;
  subtotal?: string;
}
interface CartDto { items: CartItem[]; totalAmount: string; }

function priceVND(v?: number | string | null): string {
  if (v == null) return "";
  const n = typeof v === "string" ? Number(v) : v;
  return Number.isFinite(n)
    ? new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(n)
    : "";
}

/**
 * Cart preview popover triggered from the header bag icon. Shares the
 * {@code qk.cart()} query with CartBadge / useCheckout so any mutation
 * invalidates all three at once.
 */
export default function CartOverlay({ onClose }: { onClose: () => void }) {
  const router = useRouter();
  const { data: cart, isLoading } = useQuery({
    queryKey: qk.cart(),
    queryFn: async () => {
      const res = await api.get<CartDto | { data: CartDto }>("/api/cart", {
        validateStatus: (s) => s < 500,
      });
      if (res.status >= 400) return null;
      const body = res.data as { data?: CartDto };
      return (body?.data ?? (res.data as CartDto)) || null;
    },
    staleTime: 10_000,
    retry: 0,
  });

  const items = cart?.items ?? [];

  return (
    <>
      <div
        style={{ position: "fixed", inset: 0, zIndex: 99, background: "transparent" }}
        onClick={onClose}
      />
      <div className="cart-overlay">
        <div className="cart-modal" onClick={(e) => e.stopPropagation()}>
          <button className="close" onClick={onClose} aria-label="close">×</button>
          <h3>{items.length > 0 ? "GIỎ HÀNG CỦA BẠN" : "GIỎ HÀNG TRỐNG"}</h3>

          {isLoading && (
            <div style={{ padding: "20px 0", color: "#888", fontSize: 13, textAlign: "center" }}>
              Đang tải…
            </div>
          )}

          {!isLoading && items.length === 0 && (
            <div style={{ padding: "20px 0", color: "#888", fontSize: 13, textAlign: "center" }}>
              Bạn chưa có sản phẩm nào trong giỏ.
            </div>
          )}

          {!isLoading && items.length > 0 && (
            <div style={{ maxHeight: 320, overflowY: "auto", margin: "8px 0 16px" }}>
              {items.map((it) => (
                <div className="cart-row" key={it.id}>
                  <div
                    className="cart-img"
                    style={
                      it.variantImage
                        ? {
                            backgroundImage: `url(${it.variantImage})`,
                            backgroundSize: "cover",
                            backgroundPosition: "center",
                          }
                        : { backgroundImage: makeBg("topHandle", (hashId(it.id) * 7) % 8) }
                    }
                  />
                  <div className="cart-info">
                    <div className="name">{it.productName}</div>
                    <div className="price">
                      {priceVND(it.subtotal ?? Number(it.unitPrice) * it.quantity)}
                    </div>
                    <div className="meta">
                      Style: {it.variantSku}
                      <br />
                      Số lượng: {it.quantity}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {!isLoading && items.length > 0 && (
            <div
              style={{
                display: "flex", justifyContent: "space-between",
                fontSize: 13, fontWeight: 500, padding: "8px 0 12px",
                borderTop: "1px solid #eee",
              }}
            >
              <span>Tạm tính</span>
              <span>{priceVND(cart?.totalAmount)}</span>
            </div>
          )}

          <button
            className="full-btn dark"
            disabled={items.length === 0}
            onClick={() => { onClose(); router.push("/checkout"); }}
            style={items.length === 0 ? { opacity: 0.4, cursor: "not-allowed" } : undefined}
          >
            {items.length === 0 ? "GIỎ HÀNG TRỐNG" : "THANH TOÁN"}
          </button>
          <button
            className="full-btn outline"
            onClick={() => { onClose(); router.push(items.length === 0 ? "/shop" : "/checkout"); }}
          >
            {items.length === 0 ? "MUA SẮM NGAY" : "XEM GIỎ HÀNG"}
          </button>
        </div>
      </div>
    </>
  );
}
