import type { IllustStyle } from "@/lib/illustrations";

export interface BeVariant {
  id: string;
  sku: string;
  price: string;
  salePrice?: string | null;
  image?: string | null;
}

export interface BeProduct {
  id: string;
  name: string;
  slug: string;
  thumbnail?: string;
  images?: string[];
  /** New: BE list endpoint now includes every variant image URL (deduped). */
  variantImages?: string[];
  brand?: string;
  minPrice?: number | string;
  maxPrice?: number | string;
  variants?: BeVariant[];
}

/** ProductCard view-model (mirrors the existing ProductCard type). */
export interface LuxeProduct {
  id: string;
  name: string;
  price?: string;
  tag?: string;
  imageUrls?: string[];
  baseStyle?: IllustStyle;
  palette?: number;
}

export interface HeroSlot {
  style: IllustStyle;
  palette: number;
  title: string;
}
