"use client";

import Link from "next/link";
import { useState } from "react";
import { ChevronDown, Sparkles, Flame, Tag } from "lucide-react";
import type { Category } from "@/lib/api/types";
import { cn } from "@/lib/utils";

interface Props {
  categories: Category[];
}

/**
 * Mega-menu — desktop only. Hover the "Danh mục" trigger to reveal a
 * full-width panel with top-level categories on the left and the active
 * one's children spread across the right.
 */
export function MegaMenu({ categories }: Props) {
  const [open, setOpen] = useState(false);
  const [hovered, setHovered] = useState<string | null>(
    categories[0]?.id ?? null,
  );
  const activeRoot = categories.find((c) => c.id === hovered) ?? categories[0];

  return (
    <nav className="hidden md:flex items-center gap-1 relative">
      <div
        onMouseEnter={() => setOpen(true)}
        onMouseLeave={() => setOpen(false)}
        className="relative"
      >
        <button
          type="button"
          className={cn(
            "h-10 px-3 inline-flex items-center gap-1.5 rounded text-sm font-medium transition-colors",
            open ? "text-primary bg-surface-soft" : "hover:bg-surface-soft",
          )}
        >
          Danh mục
          <ChevronDown
            className={cn("h-4 w-4 transition-transform", open && "rotate-180")}
          />
        </button>

        {open && categories.length > 0 && (
          <div className="absolute left-0 top-full pt-2 w-[720px] -translate-x-2">
            <div className="rounded-lg border border-border-base bg-white shadow-xl overflow-hidden grid grid-cols-[240px,1fr]">
              <ul className="bg-surface-soft py-2">
                {categories.slice(0, 10).map((c) => (
                  <li key={c.id}>
                    <Link
                      href={`/c/${c.slug}`}
                      onMouseEnter={() => setHovered(c.id)}
                      className={cn(
                        "px-4 py-2.5 flex items-center justify-between text-sm transition-colors",
                        hovered === c.id
                          ? "bg-white text-primary font-semibold"
                          : "hover:bg-white",
                      )}
                    >
                      <span>{c.name}</span>
                      <ChevronDown className="h-3.5 w-3.5 -rotate-90" />
                    </Link>
                  </li>
                ))}
              </ul>
              <div className="p-5">
                <div className="flex items-center justify-between mb-3">
                  <h4 className="font-semibold text-on-surface">
                    {activeRoot?.name || "Danh mục con"}
                  </h4>
                  {activeRoot && (
                    <Link
                      href={`/c/${activeRoot.slug}`}
                      className="text-xs text-accent font-medium hover:underline"
                    >
                      Xem tất cả →
                    </Link>
                  )}
                </div>
                {activeRoot?.children && activeRoot.children.length > 0 ? (
                  <div className="grid grid-cols-2 gap-x-6 gap-y-2">
                    {activeRoot.children.slice(0, 12).map((sub) => (
                      <Link
                        key={sub.id}
                        href={`/c/${sub.slug}`}
                        className="text-sm text-on-surface-variant hover:text-primary py-1"
                      >
                        {sub.name}
                      </Link>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-slate">Chưa có danh mục con.</p>
                )}
              </div>
            </div>
          </div>
        )}
      </div>

      <Link
        href="/c/new"
        className="h-10 px-3 inline-flex items-center gap-1.5 rounded text-sm font-medium hover:bg-surface-soft"
      >
        <Sparkles className="h-4 w-4 text-primary" /> Mới về
      </Link>
      <Link
        href="/c/sale"
        className="h-10 px-3 inline-flex items-center gap-1.5 rounded text-sm font-medium hover:bg-surface-soft text-accent"
      >
        <Flame className="h-4 w-4" /> Sale
      </Link>
      <Link
        href="/vouchers"
        className="h-10 px-3 inline-flex items-center gap-1.5 rounded text-sm font-medium hover:bg-surface-soft"
      >
        <Tag className="h-4 w-4" /> Voucher
      </Link>
    </nav>
  );
}
