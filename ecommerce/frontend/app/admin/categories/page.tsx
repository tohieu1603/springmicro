import { Card } from "antd";
import { listCategories } from "@/lib/api/catalog";
import { buildCategoryTree } from "@/lib/categories";
import { CategoriesManager } from "./manager";

export default async function AdminCategories() {
  const flat = await listCategories().catch(() => [] as never[]);
  const tree = buildCategoryTree(flat);

  return (
    <Card title="Danh mục sản phẩm">
      <CategoriesManager initialTree={tree} />
    </Card>
  );
}
