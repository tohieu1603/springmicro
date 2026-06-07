"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

/**
 * Storefront sort picker. Pushes the selected key into the URL (`?sort=`)
 * which re-triggers the /shop RSC fetch. Default key is "newest" so it lines
 * up with the BE handler's keyset-pagination path; price + name sorts fall
 * back to offset pagination server-side.
 */
const OPTIONS = [
  { label: "Mới nhất",         key: "newest" },
  { label: "Giá: Thấp → Cao",  key: "priceAsc" },
  { label: "Giá: Cao → Thấp",  key: "priceDesc" },
  { label: "Tên: A → Z",       key: "nameAsc" },
  { label: "Tên: Z → A",       key: "nameDesc" },
];

export default function SortDropdown() {
  const router = useRouter();
  const pathname = usePathname();
  const sp = useSearchParams();
  const current = sp.get("sort") ?? "newest";

  const [open, setOpen] = useState(false);
  const wrapRef = useRef<HTMLDivElement>(null);

  const currentLabel = useMemo(
    () => OPTIONS.find((o) => o.key === current)?.label ?? OPTIONS[0].label,
    [current],
  );

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, []);

  const pick = (key: string) => {
    setOpen(false);
    const next = new URLSearchParams(sp.toString());
    if (key === "newest") next.delete("sort");
    else next.set("sort", key);
    const qs = next.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname);
  };

  return (
    <div className="sort-wrap" ref={wrapRef}>
      <div className="sort" onClick={() => setOpen((v) => !v)} style={{ cursor: "pointer" }}>
        <span>&#9776;</span> <u>Sắp xếp: {currentLabel}</u>
      </div>
      {open && (
        <div className="sort-menu">
          {OPTIONS.map((o) => (
            <div
              key={o.key}
              className={`opt${o.key === current ? " active" : ""}`}
              onClick={() => pick(o.key)}
            >
              {o.label}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
