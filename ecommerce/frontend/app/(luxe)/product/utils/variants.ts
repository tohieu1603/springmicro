/**
 * Pure variant-matrix utilities. Used by the hook + components but no React
 * here, so these can be unit-tested in isolation.
 */

import type { AttrGroup, BeAttr, BeVariant, PickedAttrs } from "../types";

export function attrValId(a: BeAttr): string | null {
  return a.attrValId ?? a.valId ?? null;
}
export function attrVal(a: BeAttr): string {
  return a.val ?? a.valText ?? "";
}

/**
 * Collapse a variant list into per-attribute groups. The COLOR attribute
 * additionally carries a representative image per value (taken from the first
 * variant that uses that color) so the picker can render image-swatches.
 *
 * Order: COLOR, SIZE, then everything else by their first-seen attrId.
 */
export function buildGroups(variants: BeVariant[]): AttrGroup[] {
  const byAttr = new Map<string, AttrGroup>();
  for (const v of variants) {
    for (const a of v.attrs ?? []) {
      const aid = a.attrId;
      const vid = attrValId(a);
      if (aid == null || vid == null) continue;
      if (!byAttr.has(aid)) {
        byAttr.set(aid, {
          attrId: aid,
          attrCode: (a.attrCode ?? "").toUpperCase(),
          attrName: a.attrName ?? a.attrCode ?? `Attr ${aid}`,
          values: [],
        });
      }
      const g = byAttr.get(aid)!;
      const existing = g.values.find((x) => x.valId === vid);
      if (!existing) {
        g.values.push({
          valId: vid,
          valText: attrVal(a),
          image: g.attrCode === "COLOR" ? (v.image ?? undefined) : undefined,
        });
      } else if (g.attrCode === "COLOR" && v.image && !existing.image) {
        existing.image = v.image;
      }
    }
  }
  const order = (code: string) => (code === "COLOR" ? 0 : code === "SIZE" ? 1 : 2);
  return Array.from(byAttr.values()).sort((a, b) => order(a.attrCode) - order(b.attrCode));
}

/** Variant whose attrs exactly match every (and only) selected attr. */
export function findVariant(variants: BeVariant[], picked: PickedAttrs): BeVariant | null {
  const required = Object.keys(picked).length;
  if (required === 0) return null;
  for (const v of variants) {
    const av = v.attrs ?? [];
    if (av.length !== required) continue;
    let matched = 0;
    for (const a of av) {
      const aid = a.attrId;
      const vid = attrValId(a);
      if (aid != null && vid != null && picked[aid] === vid) matched++;
    }
    if (matched === required) return v;
  }
  return null;
}

/**
 * Values still reachable for a given attribute, holding everything else fixed.
 * Used to grey out "Đỏ size XL" if no XL exists for crimson colorway.
 */
export function reachableValues(
  variants: BeVariant[],
  picked: PickedAttrs,
  attrId: string,
): Set<string> {
  const out = new Set<string>();
  for (const v of variants) {
    if ((v.quantity ?? 0) <= 0) continue;
    let ok = true;
    let mine: string | null = null;
    for (const a of v.attrs ?? []) {
      const aid = a.attrId;
      const vid = attrValId(a);
      if (aid == null || vid == null) continue;
      if (aid === attrId) { mine = vid; continue; }
      if (picked[aid] != null && picked[aid] !== vid) { ok = false; break; }
    }
    if (ok && mine != null) out.add(mine);
  }
  return out;
}

/**
 * Gallery image list with the picked-colorway photo first. Falls back to
 * thumbnail + gallery + every variant image, deduped, when no color is picked.
 */
export function buildGallery(product: { thumbnail?: string; images?: string[]; variants?: BeVariant[] }, pickedColorImage: string | null): string[] {
  const out: string[] = [];
  if (pickedColorImage) out.push(pickedColorImage);
  if (product.thumbnail && !out.includes(product.thumbnail)) out.push(product.thumbnail);
  for (const img of product.images ?? []) if (img && !out.includes(img)) out.push(img);
  for (const v of product.variants ?? []) {
    if (v.image && !out.includes(v.image)) out.push(v.image);
  }
  return out;
}

/** Picked color image (or null when no color attr / not yet picked). */
export function pickedColorImageOf(groups: AttrGroup[], picked: PickedAttrs): string | null {
  const colorGroup = groups.find((g) => g.attrCode === "COLOR");
  if (!colorGroup) return null;
  const v = picked[colorGroup.attrId];
  if (v == null) return null;
  return colorGroup.values.find((x) => x.valId === v)?.image ?? null;
}
