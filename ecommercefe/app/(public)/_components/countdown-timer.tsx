"use client";

import { useEffect, useState } from "react";

/**
 * Generic countdown until end-of-day. Real flash-sale times come from
 * backend — drop them in via props when wiring the homepage to the
 * flash-sale-service API.
 */
export function CountdownTimer({ targetIso }: { targetIso?: string }) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(t);
  }, []);

  // Default: end of today at midnight
  const target = targetIso
    ? new Date(targetIso).getTime()
    : (() => {
        const d = new Date();
        d.setHours(23, 59, 59, 999);
        return d.getTime();
      })();

  const diff = Math.max(0, target - now);
  const h = Math.floor(diff / 3_600_000);
  const m = Math.floor((diff % 3_600_000) / 60_000);
  const s = Math.floor((diff % 60_000) / 1000);

  return (
    <div className="flex items-center gap-2">
      <span className="text-xs uppercase tracking-widest text-slate font-semibold">Kết thúc sau</span>
      <Box value={String(h).padStart(2, "0")} />
      <span className="font-bold text-accent">:</span>
      <Box value={String(m).padStart(2, "0")} />
      <span className="font-bold text-accent">:</span>
      <Box value={String(s).padStart(2, "0")} />
    </div>
  );
}

function Box({ value }: { value: string }) {
  return (
    <span className="inline-flex h-10 min-w-[44px] items-center justify-center rounded bg-primary text-white font-bold text-lg tabular-nums px-2">
      {value}
    </span>
  );
}
