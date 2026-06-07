"use client";

import { useState } from "react";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

export function HelpAccordion({ items }: { items: { q: string; a: string }[] }) {
  const [open, setOpen] = useState<number | null>(0);
  return (
    <div className="divide-y divide-border-base">
      {items.map((it, i) => (
        <div key={i}>
          <button
            type="button"
            onClick={() => setOpen(open === i ? null : i)}
            className="w-full flex justify-between items-center py-4 text-left"
          >
            <span className="font-semibold">{it.q}</span>
            <ChevronDown className={cn("h-4 w-4 transition-transform", open === i && "rotate-180")} />
          </button>
          {open === i && (
            <div className="pb-4 text-sm text-slate leading-relaxed">{it.a}</div>
          )}
        </div>
      ))}
    </div>
  );
}
