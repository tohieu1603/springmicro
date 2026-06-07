import type { MetadataRoute } from "next";
import { env } from "@/lib/env";

/**
 * /robots.txt — generated at build time from app/robots.ts.
 *
 * Public storefront content is indexable; admin, account, checkout and the
 * proxy / internal API endpoints are explicitly disallowed so private data
 * never accidentally leaks into a SERP.
 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: "*",
        allow: ["/"],
        disallow: [
          "/admin",
          "/admin/*",
          "/account",
          "/account/*",
          "/checkout",
          "/api/",
          "/api/*",
        ],
      },
    ],
    sitemap: `${env.SITE_URL}/sitemap.xml`,
    host: env.SITE_URL,
  };
}
