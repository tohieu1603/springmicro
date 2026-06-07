import type { BeProduct } from "../types";

/**
 * schema.org Product JSON-LD for Google rich results.
 * Renders inline `<script type="application/ld+json">` in the SSR HTML so
 * crawlers can pick up price/availability/image without executing JS.
 */
export function ProductJsonLd({ product, url }: { product: BeProduct; url: string }) {
  const variants = product.variants ?? [];
  const prices = variants.map((v) => Number(v.salePrice ?? v.price)).filter(Number.isFinite);
  const minPrice = prices.length ? Math.min(...prices) : null;
  const maxPrice = prices.length ? Math.max(...prices) : null;
  const inStock = variants.some((v) => (v.quantity ?? 0) > 0);

  const images = [
    product.thumbnail,
    ...(product.images ?? []),
    ...variants.map((v) => v.image ?? undefined),
  ].filter(Boolean) as string[];

  const data: Record<string, unknown> = {
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.name,
    description: product.description,
    sku: variants[0]?.sku,
    image: Array.from(new Set(images)).slice(0, 6),
    brand: product.brand ? { "@type": "Brand", name: product.brand } : undefined,
    url,
    offers: minPrice != null
      ? {
          "@type": maxPrice && maxPrice !== minPrice ? "AggregateOffer" : "Offer",
          priceCurrency: "VND",
          ...(maxPrice && maxPrice !== minPrice
            ? { lowPrice: minPrice, highPrice: maxPrice, offerCount: variants.length }
            : { price: minPrice }),
          availability: inStock
            ? "https://schema.org/InStock"
            : "https://schema.org/OutOfStock",
        }
      : undefined,
  };

  return (
    <script
      type="application/ld+json"
      // Render once, server-only — never read this prop client-side.
      dangerouslySetInnerHTML={{ __html: JSON.stringify(data) }}
    />
  );
}
