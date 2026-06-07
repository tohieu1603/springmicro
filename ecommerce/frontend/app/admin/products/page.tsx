import Link from "next/link";
import { Card, Button as AntButton } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import { listProducts, listCategories } from "@/lib/api/catalog";
import { ProductsTable } from "./products-table";
import { ProductsKpis } from "./products-kpis";

interface PageProps {
  searchParams: Promise<{ q?: string; page?: string; status?: string; categoryId?: string }>;
}

export default async function AdminProducts({ searchParams }: PageProps) {
  const sp = await searchParams;
  const page = Math.max(0, Number(sp.page ?? 0) || 0);
  const [data, allActive, draft, categoriesFlat] = await Promise.all([
    listProducts({
      page, size: 20, q: sp.q, status: sp.status,
      categoryId: sp.categoryId ? sp.categoryId : undefined,
    }).catch(() => ({
      content: [], number: 0, size: 20, totalElements: 0, totalPages: 0,
    })),
    listProducts({ page: 0, size: 1, status: "ACTIVE" }).catch(() => ({ totalElements: 0 } as { totalElements: number })),
    listProducts({ page: 0, size: 1, status: "DRAFT" }).catch(() => ({ totalElements: 0 } as { totalElements: number })),
    listCategories().catch(() => [] as never[]),
  ]);

  const lowStock = data.content.filter((p) =>
    p.variants?.some((v) => (v.quantity ?? 0) <= 5),
  ).length;

  return (
    <div className="space-y-4">
      <ProductsKpis
        total={data.totalElements}
        active={allActive.totalElements}
        lowStock={lowStock}
        draft={draft.totalElements}
      />
      <Card
        title="Danh sách sản phẩm"
        extra={
          <Link href="/admin/products/new">
            <AntButton type="primary" icon={<PlusOutlined />}>Thêm sản phẩm</AntButton>
          </Link>
        }
      >
        <ProductsTable page={data} categories={categoriesFlat} />
      </Card>
    </div>
  );
}
