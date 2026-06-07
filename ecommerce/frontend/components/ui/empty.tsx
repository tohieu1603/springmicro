import * as React from "react";
import { cn } from "@/lib/utils";

/** Generic empty-state. Pass `cta` for an action button. */
export function Empty({
  title,
  description,
  icon,
  cta,
  className,
}: {
  title: string;
  description?: string;
  icon?: React.ReactNode;
  cta?: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("flex flex-col items-center justify-center text-center py-12 px-4", className)}>
      {icon && <div className="text-slate mb-3">{icon}</div>}
      <h3 className="text-h3-d text-on-surface">{title}</h3>
      {description && <p className="mt-2 text-body-md text-slate max-w-md">{description}</p>}
      {cta && <div className="mt-5">{cta}</div>}
    </div>
  );
}
