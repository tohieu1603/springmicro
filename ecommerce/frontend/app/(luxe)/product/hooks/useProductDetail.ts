"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { ProductDetailApi } from "../services/api";
import { useInvalidate } from "@/lib/query/invalidate";
import type { AddToBagResult, BeProduct, BeVariant, PickedAttrs } from "../types";
import {
  buildGallery,
  buildGroups,
  findVariant,
  pickedColorImageOf,
  reachableValues,
} from "../utils/variants";
import { priceRange, priceVND } from "../utils/format";

interface UseProductDetailArgs {
  /** Server-fetched product. Hook seeds state from this — no initial fetch. */
  initialProduct: BeProduct;
  initialRelated?: BeProduct[];
}

/**
 * Single God Hook for the product detail page.
 *
 * Owns: client-side state (variant + qty selection, gallery pager), derived
 * view-model (price label, stock label, gallery, picker groups), and the
 * add-to-bag action (with a pre-flight re-check against catalog right before
 * the POST).
 *
 * Data fetching is done by the RSC `page.tsx` and seeded via props — no
 * client-side initial fetch, so the first paint already has full HTML.
 * Re-seeds when `initialProduct.id` changes (client-side nav between products).
 *
 * Returns one object — the client wrapper destructures it and prop-drills.
 */
export function useProductDetail({ initialProduct, initialRelated = [] }: UseProductDetailArgs) {
  const router = useRouter();
  const invalidate = useInvalidate();

  const [product, setProduct] = useState<BeProduct>(initialProduct);
  const [related, setRelated] = useState<BeProduct[]>(initialRelated);

  const [picked, setPicked] = useState<PickedAttrs>({});
  const [qty, setQty] = useState(1);
  const [qtyWarn, setQtyWarn] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);

  const [galleryIdx, setGalleryIdx] = useState(0);
  const [relIdx, setRelIdx] = useState(0);

  // ─── re-seed on client-side nav between products ────────────────
  useEffect(() => {
    setProduct(initialProduct);
    setRelated(initialRelated);
    setPicked({});
    setQty(1);
    setQtyWarn(null);
    setGalleryIdx(0);
    setRelIdx(0);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialProduct.id]);

  // ─── derived ─────────────────────────────────────────────────────
  const groups = useMemo(() => (product ? buildGroups(product.variants ?? []) : []), [product]);

  const selVariant: BeVariant | null = useMemo(
    () => (product ? findVariant(product.variants ?? [], picked) : null),
    [product, picked],
  );

  const fullySelected = groups.length > 0 && groups.every((g) => picked[g.attrId] != null);

  const pickedColorImage = useMemo(() => pickedColorImageOf(groups, picked), [groups, picked]);
  const gallery = useMemo(() => (product ? buildGallery(product, pickedColorImage) : []), [product, pickedColorImage]);

  const displayPrice = useMemo(() => {
    if (selVariant) {
      const sale = selVariant.salePrice ? Number(selVariant.salePrice) : null;
      const base = Number(selVariant.price);
      if (sale && sale < base) return priceVND(sale);
      return priceVND(base);
    }
    const prices = (product.variants ?? []).map((v) => Number(v.salePrice ?? v.price)).filter(Number.isFinite);
    if (prices.length === 0) return "";
    return priceRange(Math.min(...prices), Math.max(...prices));
  }, [product, selVariant]);

  const maxQty = selVariant?.quantity ?? 0;
  const stockLabel = selVariant
    ? maxQty > 0
      ? `Còn ${maxQty} sản phẩm`
      : "Hết hàng"
    : null;

  // ─── actions ─────────────────────────────────────────────────────
  const pick = useCallback((attrId: string, valId: string) => {
    setPicked((cur) => {
      if (cur[attrId] === valId) {
        const { [attrId]: _drop, ...rest } = cur;
        return rest;
      }
      return { ...cur, [attrId]: valId };
    });
    setGalleryIdx(0);
    setQty(1);
    setQtyWarn(null);
  }, []);

  const changeQty = useCallback((next: number) => {
    if (!Number.isFinite(next)) return;
    if (next < 1) { setQty(1); setQtyWarn(null); return; }
    if (selVariant && next > (selVariant.quantity ?? 0)) {
      setQty(selVariant.quantity ?? 1);
      setQtyWarn(`Chỉ còn ${selVariant.quantity} sản phẩm`);
      return;
    }
    if (next > 999) {
      setQty(999);
      setQtyWarn("Tối đa 999 sản phẩm");
      return;
    }
    setQty(next);
    setQtyWarn(null);
  }, [selVariant]);

  /**
   * Add-to-bag with two safety nets:
   *  1. Pre-flight: re-GET the product right before POST so we don't try to
   *     add a deleted/inactive/out-of-stock variant from a stale tab.
   *  2. Server enforces stock + status independently — UI returns the
   *     translated error toast based on HTTP status.
   */
  const addToBag = useCallback(async (): Promise<AddToBagResult> => {
    if (groups.length > 0 && !fullySelected) {
      toast.error("Vui lòng chọn đầy đủ phân loại");
      return { ok: false, reason: "validation", message: "incomplete-selection" };
    }
    const variant = selVariant ?? product.variants?.[0];
    if (!variant) {
      toast.error("Sản phẩm chưa có biến thể");
      return { ok: false, reason: "validation", message: "no-variant" };
    }
    setAdding(true);
    try {
      // ── Pre-flight check ─────────────────────────────────────────
      const fresh = await ProductDetailApi.preflight(product.id);
      if (fresh.status && fresh.status !== "ACTIVE") {
        toast.error("Sản phẩm vừa bị gỡ khỏi cửa hàng");
        setProduct(fresh);
        return { ok: false, reason: "inactive", message: "product-inactive" };
      }
      const freshVariant = fresh.variants?.find((v) => v.id === variant.id);
      if (!freshVariant) {
        toast.error("Phân loại không còn tồn tại");
        setProduct(fresh);
        return { ok: false, reason: "deleted", message: "variant-removed" };
      }
      if (freshVariant.status && freshVariant.status !== "ACTIVE") {
        toast.error("Phân loại đã ngưng kinh doanh");
        setProduct(fresh);
        return { ok: false, reason: "inactive", message: "variant-inactive" };
      }
      const available = freshVariant.quantity ?? 0;
      if (qty > available) {
        toast.error(available > 0 ? `Chỉ còn ${available} sản phẩm` : "Phân loại đã hết hàng");
        setProduct(fresh);
        setQty(Math.max(1, available));
        return { ok: false, reason: "stock", message: "stock-decreased" };
      }
      if (Number(freshVariant.price) !== Number(variant.price)) {
        toast.warning("Giá vừa được cập nhật, vui lòng kiểm tra lại");
        setProduct(fresh);
        return { ok: false, reason: "validation", message: "price-changed" };
      }

      await ProductDetailApi.addToCart({
        productId: product.id,
        variantId: variant.id,
        quantity: qty,
      });
      // Refresh the cart query so the header badge + overlay + checkout page
      // all reflect the new count without a manual page reload.
      invalidate.cart();
      toast.success(qty > 1 ? `Đã thêm ${qty} sản phẩm vào giỏ` : "Đã thêm vào giỏ");
      return { ok: true };
    } catch (e: unknown) {
      const err = e as { response?: { status?: number; data?: { message?: string } } };
      const status = err.response?.status;
      const msg = err.response?.data?.message;
      if (status === 401) {
        toast.error("Vui lòng đăng nhập để thêm vào giỏ");
        router.push("/login");
        return { ok: false, reason: "auth", message: "unauthenticated" };
      }
      if (status === 404) {
        toast.error(msg ?? "Sản phẩm/phân loại không còn tồn tại");
        return { ok: false, reason: "deleted", message: msg ?? "not-found" };
      }
      if (status === 409) {
        toast.error(msg ?? "Không đủ tồn kho");
        return { ok: false, reason: "stock", message: msg ?? "conflict" };
      }
      toast.error(msg ?? "Không thể thêm vào giỏ");
      return { ok: false, reason: "unknown", message: msg ?? "unknown" };
    } finally {
      setAdding(false);
    }
  }, [product, groups, fullySelected, selVariant, qty, router]);

  // ─── helpers exposed to components ───────────────────────────────
  const isReachable = useCallback(
    (attrId: string, valId: string): boolean => {
      const reachable = reachableValues(product.variants ?? [], picked, attrId);
      return reachable.has(valId);
    },
    [product, picked],
  );

  return {
    // state (no loading/error — RSC handled that)
    product,
    related,
    picked,
    qty,
    qtyWarn,
    adding,
    galleryIdx,
    setGalleryIdx,
    relIdx,
    setRelIdx,

    // derived
    groups,
    selVariant,
    fullySelected,
    gallery,
    displayPrice,
    maxQty,
    stockLabel,

    // actions
    pick,
    changeQty,
    addToBag,
    isReachable,
  };
}

export type ProductDetailVM = ReturnType<typeof useProductDetail>;
