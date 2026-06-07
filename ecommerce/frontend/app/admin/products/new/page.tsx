import { listCategories } from "@/lib/api/catalog";
import { fetchServer } from "@/lib/api/server";
import { ProductForm } from "../_components/product-form";
import type { Attr } from "@/lib/api/types";

/**
 * "New product" page. SSR loads the supporting reference data (categories +
 * attributes) once so the client form doesn't burn a round-trip waiting for
 * dropdowns to populate.
 */
export default async function NewProductPage() {
  const [cats, attrs] = await Promise.all([
    listCategories().catch(() => [] as never[]),
    fetchServer<Attr[]>("/api/attrs").catch(() => [] as Attr[]),
  ]);

  return <ProductForm mode="create" categories={cats} attrs={attrs} />;
}
