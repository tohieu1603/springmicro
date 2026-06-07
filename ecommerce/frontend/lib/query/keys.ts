/**
 * Central query-key registry. Every key is a function that returns a
 * read-only tuple so invalidation patterns stay typed across the app:
 *
 *   queryClient.invalidateQueries({ queryKey: qk.cart() })
 *   queryClient.invalidateQueries({ queryKey: qk.orders.list() })
 *
 * Conventions:
 *   - First element is the "namespace" (e.g. "cart", "orders"). Invalidating
 *     by namespace cascades to every sub-query that starts with it.
 *   - Parameters come AFTER the namespace so partial invalidation works:
 *     {@code qk.orders.detail(id)} starts with {@code qk.orders.all()}.
 */
export const qk = {
  // ── auth / session ───────────────────────────────────────────────
  session: () => ["session"] as const,

  // ── cart ─────────────────────────────────────────────────────────
  cart: () => ["cart"] as const,

  // ── address book ─────────────────────────────────────────────────
  addresses: () => ["addresses"] as const,

  // ── payment methods (storefront) ─────────────────────────────────
  paymentMethods: () => ["payment-methods"] as const,
  paymentMethodsAdmin: () => ["payment-methods-admin"] as const,

  // ── shipping ─────────────────────────────────────────────────────
  shipping: {
    carriers: () => ["shipping", "carriers"] as const,
    carriersAdmin: () => ["shipping", "carriers-admin"] as const,
    fee: (
      addressKey: string,
      weight: number,
      value: number,
    ) => ["shipping", "fee", addressKey, weight, value] as const,
  },

  // ── voucher ──────────────────────────────────────────────────────
  voucher: (code: string) => ["voucher", code] as const,

  // ── orders ───────────────────────────────────────────────────────
  orders: {
    all: () => ["orders"] as const,
    detail: (id: string | number) => ["orders", "detail", id] as const,
    byNumber: (orderNumber: string) => ["orders", "by-number", orderNumber] as const,
    my: (cursor?: string) => ["orders", "my", cursor ?? null] as const,
    admin: (params: Record<string, string | undefined> = {}) =>
      ["orders", "admin", params] as const,
  },

  // ── notifications ────────────────────────────────────────────────
  notifications: {
    unread: () => ["notifications", "unread"] as const,
    feed: () => ["notifications", "feed"] as const,
  },

  // ── catalog ──────────────────────────────────────────────────────
  products: {
    list: (params: Record<string, string | number | undefined> = {}) =>
      ["products", "list", params] as const,
    detail: (id: string | number) => ["products", "detail", id] as const,
    bySlug: (slug: string) => ["products", "by-slug", slug] as const,
  },
  categories: () => ["categories"] as const,

  // ── banners ──────────────────────────────────────────────────────
  banners: {
    active: () => ["banners", "active"] as const,
    admin: () => ["banners", "admin"] as const,
  },

  // ── flash sales ──────────────────────────────────────────────────
  flashSales: {
    active: () => ["flash-sales", "active"] as const,
  },
} as const;
