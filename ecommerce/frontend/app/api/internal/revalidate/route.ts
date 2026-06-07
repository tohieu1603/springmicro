import { NextResponse, type NextRequest } from "next/server";
import { revalidatePath, revalidateTag } from "next/cache";

import { env } from "@/lib/env";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

interface RevalidateBody {
  /** Specific path to revalidate (e.g. "/product"). Multiple via comma-separated string. */
  path?: string;
  /** One or more cache tags to bust (e.g. "catalog:list"). */
  tag?: string | string[];
  /** Product id — convenience: revalidates `/product` + `catalog:product:<id>` tag. */
  productId?: string | string;
}

/**
 * POST /api/internal/revalidate — busts Next's Data Cache after BE events.
 *
 * Auth: `X-Revalidate-Secret` header must equal `REVALIDATE_SECRET`. The
 * intended caller is a Spring Kafka consumer (catalog-service publishes
 * `catalog.product-status-changed` / `catalog.product-deleted`) that POSTs
 * here with the affected productId.
 *
 * Body examples:
 *   { "productId": 42 }                        → /product cache busted
 *   { "path": "/shop" }                        → /shop cache busted
 *   { "tag": ["catalog:list", "catalog:product:42"] }
 *
 * Returns 401 on bad secret, 400 on malformed body, 200 with the list of
 * paths/tags actually revalidated.
 */
export async function POST(req: NextRequest) {
  const supplied = req.headers.get("x-revalidate-secret");
  if (!env.REVALIDATE_SECRET || supplied !== env.REVALIDATE_SECRET) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }

  let body: RevalidateBody;
  try {
    body = (await req.json()) as RevalidateBody;
  } catch {
    return NextResponse.json({ error: "invalid json body" }, { status: 400 });
  }

  const revalidatedPaths: string[] = [];
  const revalidatedTags: string[] = [];

  // Specific paths
  if (body.path) {
    const paths = body.path.split(",").map((s) => s.trim()).filter(Boolean);
    for (const p of paths) {
      revalidatePath(p);
      revalidatedPaths.push(p);
    }
  }

  // Tags (one or many)
  if (body.tag) {
    const tags = Array.isArray(body.tag) ? body.tag : [body.tag];
    for (const t of tags) {
      revalidateTag(t);
      revalidatedTags.push(t);
    }
  }

  // Convenience: productId → bust product + listing caches.
  if (body.productId != null) {
    const pid = String(body.productId);
    revalidatePath("/product", "page");
    revalidatePath("/shop", "page");
    revalidatePath("/", "page");
    revalidatedPaths.push("/product", "/shop", "/");
    revalidateTag(`catalog:product:${pid}`);
    revalidateTag("catalog:list");
    revalidatedTags.push(`catalog:product:${pid}`, "catalog:list");
  }

  if (revalidatedPaths.length === 0 && revalidatedTags.length === 0) {
    return NextResponse.json(
      { error: "no path / tag / productId in body" },
      { status: 400 },
    );
  }

  return NextResponse.json({
    revalidated: true,
    paths: revalidatedPaths,
    tags: revalidatedTags,
    now: Date.now(),
  });
}
