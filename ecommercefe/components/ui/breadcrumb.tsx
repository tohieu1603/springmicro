import Link from "next/link";
import { ChevronRight, Home } from "lucide-react";

export interface BreadcrumbItem {
  href?: string;
  label: string;
}

export function Breadcrumb({ items }: { items: BreadcrumbItem[] }) {
  return (
    <nav aria-label="Breadcrumb" className="text-xs text-slate flex items-center gap-1.5 flex-wrap">
      <Link href="/" className="hover:text-primary inline-flex items-center gap-1">
        <Home className="h-3.5 w-3.5" />
        <span>Trang chủ</span>
      </Link>
      {items.map((item, i) => (
        <span key={i} className="inline-flex items-center gap-1.5">
          <ChevronRight className="h-3.5 w-3.5 text-border-base" />
          {item.href ? (
            <Link href={item.href} className="hover:text-primary">
              {item.label}
            </Link>
          ) : (
            <span className="text-on-surface font-medium">{item.label}</span>
          )}
        </span>
      ))}
    </nav>
  );
}
