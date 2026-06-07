import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeStyles = cva(
  "inline-flex items-center gap-1 rounded-sm px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wide",
  {
    variants: {
      tone: {
        neutral: "bg-surface-container text-on-surface-variant",
        success: "bg-emerald-50 text-emerald-700 border border-emerald-200",
        danger: "bg-red-50 text-red-700 border border-red-200",
        warning: "bg-amber-50 text-amber-700 border border-amber-200",
        accent: "bg-orange-50 text-accent border border-orange-200",
        primary: "bg-primary text-white",
      },
    },
    defaultVariants: { tone: "neutral" },
  },
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeStyles> {}

export function Badge({ className, tone, ...props }: BadgeProps) {
  return <span className={cn(badgeStyles({ tone }), className)} {...props} />;
}
