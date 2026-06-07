"use client";

import { toast } from "sonner";
import { Copy, Check } from "lucide-react";
import { useState } from "react";

export function CopyVoucher({ code }: { code: string }) {
  const [copied, setCopied] = useState(false);
  async function copy() {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      toast.success("Đã sao chép mã", { description: code });
      setTimeout(() => setCopied(false), 2500);
    } catch {
      toast.error("Không sao chép được");
    }
  }
  return (
    <button
      onClick={copy}
      className="inline-flex items-center gap-2 px-4 py-2 rounded bg-white text-primary font-mono font-bold hover:bg-accent hover:text-white transition-colors"
    >
      {code}
      {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
    </button>
  );
}
