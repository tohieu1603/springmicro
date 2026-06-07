import * as React from "react";
import { cn } from "@/lib/utils";

/** Max-width wrapper centred to 1200px gutter per the Luxury Mart grid spec. */
export function Container({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn("mx-auto w-full max-w-container px-4 lg:px-gutter", className)} {...props} />
  );
}
