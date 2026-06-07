import type { Category } from "@/lib/api/types";

/**
 * Flat → tree converter used by both the public mega-menu and the admin
 * category manager. Lives in lib (not server-only) because the admin client
 * page also needs it after mutations.
 */
export function buildCategoryTree(flat: Category[]): Category[] {
  const byId = new Map<string, Category>();
  flat.forEach((c) => byId.set(c.id, { ...c, children: [] }));

  const roots: Category[] = [];
  for (const c of byId.values()) {
    if (c.parentId && byId.has(c.parentId)) {
      byId.get(c.parentId)!.children!.push(c);
    } else {
      roots.push(c);
    }
  }
  // Sort each level by sortOrder then name.
  const sort = (arr: Category[]) => {
    arr.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.name.localeCompare(b.name, "vi"));
    arr.forEach((n) => n.children && sort(n.children));
  };
  sort(roots);
  return roots;
}

export function flattenTree(roots: Category[]): Category[] {
  const out: Category[] = [];
  const walk = (n: Category) => {
    out.push(n);
    n.children?.forEach(walk);
  };
  roots.forEach(walk);
  return out;
}
