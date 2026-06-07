import Link from "next/link";
import { Empty } from "@/components/ui/empty";
import { ProductCard } from "@/components/ui/product-card";
import type { Product } from "@/lib/api/types";

interface SearchResultsProps {
  products: Product[];
  query: string;
}

export function SearchResults({ products, query }: SearchResultsProps) {
  if (products.length === 0) {
    return (
      <Empty
        title="Không tìm thấy sản phẩm"
        description={query ? `Không có kết quả cho “${query}”. Thử từ khóa khác.` : "Nhập từ khóa để tìm kiếm."}
        cta={<Link href="/shop" className="text-accent font-semibold">Xem tất cả sản phẩm</Link>}
        className="mt-12"
      />
    );
  }
  return (
    <div className="mt-6 grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      {products.map((p) => <ProductCard key={p.id} product={p} />)}
    </div>
  );
}
