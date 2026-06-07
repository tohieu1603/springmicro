import { cn } from "@/lib/utils";

/** Tailwind-pulse skeleton — use sparingly, only where SSR can't fill. */
export function Skeleton({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("animate-pulse rounded-md bg-surface-container", className)} {...props} />;
}

export function ProductCardSkeleton() {
  return (
    <div className="rounded-lg border border-border-base bg-white overflow-hidden">
      <Skeleton className="aspect-square rounded-none" />
      <div className="p-3 space-y-2">
        <Skeleton className="h-3 w-1/3" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-2/3" />
        <Skeleton className="h-6 w-1/2 mt-2" />
        <Skeleton className="h-10 w-full mt-2" />
      </div>
    </div>
  );
}
