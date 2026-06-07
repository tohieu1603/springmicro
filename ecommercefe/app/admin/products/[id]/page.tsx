import { notFound } from "next/navigation";
import { listCategories, getProductById } from "@/lib/api/catalog";
import { fetchServer } from "@/lib/api/server";
import { ProductForm } from "../_components/product-form";
import type { Attr } from "@/lib/api/types";

interface PageProps {
  params: Promise<{ id: string }>;
}

export default async function EditProduct({ params }: PageProps) {
  const { id } = await params;
  const [product, cats, attrs] = await Promise.all([
    getProductById(id),
    listCategories().catch(() => [] as never[]),
    fetchServer<Attr[]>("/api/attrs").catch(() => [] as Attr[]),
  ]);
  if (!product) notFound();

  return <ProductForm mode="edit" product={product} categories={cats} attrs={attrs} />;
}
