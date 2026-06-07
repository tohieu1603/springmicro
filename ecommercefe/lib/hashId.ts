/**
 * Deterministic string→number hash for UI seeding (palettes, fallback styles,
 * placeholder backgrounds). Ids are UUID strings now, so callers that used to
 * do `id * N % M` for visual variety route through this instead.
 */
export function hashId(v: string | number | null | undefined): number {
  const s = String(v ?? "");
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0;
  return h;
}
