# Luxury Mart — Next.js Web

Web FE đầy đủ cho hệ vi dịch vụ thương mại điện tử (Java BE đã có sẵn). Một codebase phục vụ 2 trải nghiệm:

- **Storefront** `/` — bán đa dạng sản phẩm, biến thể động, Tailwind custom theo design Luxury Mart, SSR + ISR.
- **Admin** `/admin` — bảng điều khiển đầy đủ với Ant Design + ApexCharts, gated bằng `ROLE_ADMIN`.

Mọi request API đi qua **Next route handlers** ở `app/api/*` — JWT lưu trong **HttpOnly cookie**, browser không bao giờ thấy token.

---

## Chạy nhanh

Yêu cầu: Node 20+ (test 22), pnpm. BE `api-gateway` ở `http://localhost:8080`.

```bash
cd /Users/admin/HieuTo/ecommercefe
cp .env.example .env.local
pnpm install
pnpm approve-builds   # 1 lần — chọn 'a' rồi 'y' để cho phép sharp + unrs-resolver build native binaries
pnpm dev              # mở http://localhost:3000
```

> **Lưu ý copy-paste**: KHÔNG paste kèm comment `#` vào terminal — pnpm v11 sẽ parse `#` như spec gói và báo `ERR_PNPM_SPEC_NOT_SUPPORTED_BY_ANY_RESOLVER`. Mỗi dòng paste 1 lệnh thuần.

Nếu vẫn báo `ERR_PNPM_IGNORED_BUILDS`:
```bash
pnpm install --allow-build sharp --allow-build unrs-resolver
```

Lệnh khác:

```bash
pnpm build
pnpm typecheck
pnpm lint
pnpm start
```

- `pnpm build` — production build
- `pnpm typecheck` — `tsc --noEmit`, hiện PASS sạch
- `pnpm lint` — next lint
- `pnpm start` — serve build

---

## Sơ đồ trang

### Storefront

| Path | Mô tả | Render |
|------|------|--------|
| `/` | Trang chủ — Hero, trust strip, bento danh mục, flash deal + countdown, carousel best-seller, mới về, editorial banners, brand strip, newsletter | SSR + ISR 60s |
| `/c/[slug]` | Danh mục (kể cả `/c/all`, `/c/new`, `/c/sale`) + sidebar lọc, sort, paging | SSR |
| `/p/[slug]` | Chi tiết SP — gallery, variant picker dynamic, thông số attr, related, **JSON-LD Schema.org** | SSR + island CSR |
| `/search?q=` | Tìm kiếm — gọi `search-service`, fallback catalog | SSR |
| `/cart` | Giỏ hàng | CSR |
| `/checkout` | Thanh toán + voucher + Sepay/MoMo/COD | CSR |
| `/login`, `/register`, `/forgot` | Xác thực; sau login role ADMIN auto redirect `/admin` | CSR |
| `/account` | Tổng quan với account sidebar nav | SSR |
| `/account/orders`, `/account/orders/[id]` | Lịch sử + chi tiết đơn | SSR |
| `/account/profile` | Cập nhật hồ sơ | CSR |
| `/account/password` | Đổi mật khẩu | CSR |
| `/account/addresses` | Sổ địa chỉ CRUD (default, edit, xóa) | CSR |
| `/account/wishlist` | Yêu thích | CSR |
| `/account/notifications` | Thông báo + SSE stream nhận realtime | CSR |
| `/vouchers` | List voucher đang chạy + copy code | SSR |
| `/track` | Tra cứu đơn không cần đăng nhập (số đơn + SĐT) | CSR |
| `/about`, `/contact` | Giới thiệu + liên hệ | SSR + CSR form |
| `/help/[faq\|shipping\|returns\|payment]` | Trợ giúp accordion | SSR static |
| `/legal/[terms\|privacy]` | Pháp lý | SSR static |
| `/404`, `error.tsx`, `loading.tsx` | Boundary đầy đủ | RSC |

### Admin (`/admin`)

| Path | Mô tả |
|------|------|
| `/admin` | Dashboard — 4 KPI + 3 ApexCharts (area, donut, bar) |
| `/admin/reports` | Báo cáo theo khoảng ngày — Doanh thu / Tồn / Khách |
| `/admin/products` | Danh sách + filter + paging |
| `/admin/products/new`, `/admin/products/[id]` | Form 3-tab: Chung / Biến thể / SEO. **Variant builder dynamic** với Cartesian-product generator |
| `/admin/categories` | Cây danh mục — add/edit/delete, parent select |
| `/admin/attrs` | Thuộc tính (SELECT/TEXT/NUMBER) + values dynamic |
| `/admin/orders`, `/admin/orders/[id]` | Bảng đơn + Steps trạng thái |
| `/admin/inventory` | Tồn kho + adjust modal |
| `/admin/returns` | Yêu cầu trả hàng |
| `/admin/vouchers` | CRUD voucher với DateRange |
| `/admin/flash-sales` | Flash sale |
| `/admin/banners` | Banner/promotional |
| `/admin/marketing` | Chiến dịch + Email template + Push |
| `/admin/customers`, `/admin/customers/[id]` | Khách hàng + lifetime value + đơn gần đây |
| `/admin/users` | Tài khoản — bật/tắt active |
| `/admin/permissions` | Vai trò & quyền (RBAC) |
| `/admin/seo` | SEO toàn site (meta, robots, redirects) |
| `/admin/activity` | Audit log với expandable diff |
| `/admin/logs` | Logs từ analytics-service |
| `/admin/settings` | Tabs Chung / Vận chuyển / Thanh toán |

---

## Cấu trúc thư mục

```
ecommercefe/
├── app/
│   ├── (public)/                # Storefront layout group
│   │   ├── _components/         # home-product-carousel, countdown-timer, newsletter
│   │   ├── layout.tsx           # SiteHeader + SiteFooter wrapper
│   │   ├── page.tsx             # Home (SSR + ISR)
│   │   ├── error.tsx, loading.tsx
│   │   ├── c/[slug]/
│   │   ├── p/[slug]/            # Detail + BuyBox island + JSON-LD
│   │   ├── cart/, checkout/
│   │   ├── login/, register/, forgot/
│   │   ├── account/             # Layout + nav + 7 pages
│   │   ├── help/[topic]/, legal/[doc]/
│   │   ├── vouchers/, track/
│   │   └── about/, contact/, search/
│   │
│   ├── admin/                   # ROLE_ADMIN guarded layout
│   │   ├── layout.tsx           # SSR role check
│   │   ├── error.tsx, loading.tsx
│   │   ├── _components/         # AdminShell + DashboardCharts
│   │   └── 18 sub-routes
│   │
│   ├── api/
│   │   ├── auth/                # login, register, logout, refresh — set HttpOnly cookie
│   │   └── proxy/[...path]/     # Bridge browser → api-gateway
│   │
│   ├── layout.tsx               # AntdRegistry SSR + Inter font
│   ├── globals.css              # Tailwind + Material Symbols
│   ├── not-found.tsx
│   ├── sitemap.ts               # Dynamic sitemap.xml
│   └── robots.ts
│
├── components/
│   ├── ui/                      # Button, Input, Card, Badge, ProductCard, Container,
│   │                            #   Empty, Breadcrumb, Skeleton — Tailwind custom
│   ├── layout/                  # SiteHeader, SiteFooter, MegaMenu, MobileNav, CartBadge
│   └── providers/               # AntdRegistry (cssinjs SSR)
│
├── lib/
│   ├── api/
│   │   ├── client.ts            # axios browser → /api/proxy + 401 auto-refresh
│   │   ├── server.ts            # axios server — attach Bearer cookie
│   │   ├── catalog.ts           # listProducts, getProductBySlug, listCategories
│   │   └── types.ts             # Shared DTO
│   ├── auth/session.ts          # getSession(), isAdmin()
│   ├── categories.ts            # Flat ↔ tree
│   ├── env.ts                   # Centralised env
│   └── utils.ts                 # cn(), formatVnd, formatDate, slugify
│
├── middleware.ts                # Edge: soft auth gate + security headers
├── public/img/
├── tailwind.config.ts           # Luxury Mart tokens
├── postcss.config.js
├── next.config.ts               # antd transpile + image domains
├── tsconfig.json
├── .env.example, .env.local
└── fe/                          # HTML mock gốc — reference
```

---

## Auth flow (HttpOnly cookie)

1. POST `/api/auth/login` (Next route) → forward `/api/auth/login` BE.
2. BE trả `accessToken` + `refreshToken` → Next set **2 HttpOnly cookies** (`lm_access`, `lm_refresh`). Browser không đọc được JWT.
3. Browser request → `/api/proxy/*` → Next attach `Authorization: Bearer` server-side → forward gateway.
4. 401 → client interceptor gọi `/api/auth/refresh` (Next) → swap cookies → retry.
5. Login response chứa `roles`. `ROLE_ADMIN` → `router.replace("/admin")`, còn lại → `next` param hoặc `/`.
6. `middleware.ts` chặn sớm /admin, /account, /checkout khi không có cookie (bounce `/login?next=...`) — defense-in-depth.

---

## SSR / CSR strategy

- **SSR + ISR** cho mọi page SEO-quan-trọng: home, listing, detail, search, sitemap, robots, JSON-LD structured data.
- **CSR** cho user-mutating state: cart, checkout, account forms, admin forms.
- **RSC + CSR island**: product detail SSR cho meta, BuyBox CSR cho variant picker.
- **Dynamic ssr:false** cho ApexCharts (cần `window`) — KPI cards vẫn SSR.

---

## Variant builder (admin)

Mỗi sản phẩm có **biến thể động** — không cố định Size/Color như mẫu giày. Trang `/admin/products/new`:

1. Chọn các thuộc tính dùng cho sản phẩm này (Size, Color, Storage, Material, ...).
2. Nhập `SKU gốc` + `Giá gốc` → bấm **Sinh tự động** → Cartesian product tất cả values → list variant tự sinh.
3. Mỗi variant: SKU / Giá / Sale price / Tồn / Ảnh + 1 input cho mỗi attr (SELECT → dropdown, TEXT/NUMBER → input).
4. **Tạo sản phẩm** → POST `/api/products` với mảng `variants` đầy đủ.

---

## SEO

- `app/sitemap.ts` build động từ catalog (categories + products).
- `app/robots.ts` chặn /admin /api /account /checkout.
- Product detail `JSON-LD Schema.org` với Product + AggregateOffer.
- Meta title/description từ DB (`product.metaTitle`) → fallback brand defaults.
- OpenGraph tags ở `app/layout.tsx`.

---

## API endpoint mapping

Page nào gọi endpoint nào — để bạn biết phần nào cần BE expose:

| Page | Endpoint |
|------|----------|
| Home, listing, detail | `GET /api/products`, `/api/products/by-slug/:slug`, `/api/categories` |
| Search | `GET /api/search?q=` (fallback `/api/products?q=`) |
| Cart | `GET /api/cart`, `POST /api/cart/items`, `PUT /api/cart/items/:vid`, `DELETE …` |
| Checkout | `POST /api/orders/from-cart` |
| Account orders | `GET /api/orders/my`, `/api/orders/:id` |
| Account profile | `GET/PATCH /api/auth/me` |
| Addresses | `/api/users/me/addresses` |
| Wishlist | `/api/users/me/wishlist` |
| Notifications | `GET /api/notifications/me`, SSE `/api/notifications/stream` |
| Vouchers public | `GET /api/vouchers/active` |
| Track order | `GET /api/shipments/tracking/:orderNumber?phone=` |
| Admin products | `POST/PATCH /api/products`, `GET /api/attrs` |
| Admin categories | `GET/POST/PATCH/DELETE /api/categories` |
| Admin attrs | `GET/POST/PATCH/DELETE /api/attrs` |
| Admin orders | `GET /api/orders?...` |
| Admin inventory | `GET /api/inventory`, `POST /api/inventory/:id/adjust` |
| Admin vouchers | `GET/POST/PATCH /api/vouchers` |
| Admin users | `GET /api/users`, `PATCH /api/users/:id/active` |
| Admin customers | `GET /api/users/customers`, `/api/users/:id` |
| Admin reports | `GET /api/analytics/reports?from=&to=` |
| Admin roles | `GET/POST/PATCH/DELETE /api/roles` |
| Admin audit | `GET /api/audit/activity` |
| Admin logs | `GET /api/analytics/logs` |

> Endpoint nào BE chưa có → FE hiển thị bảng rỗng hoặc fallback mock (ghi chú trong code). Wiring sau là mechanical.

---

## Theming

- **Storefront**: Tailwind v3.4 custom với token Luxury Mart trong `tailwind.config.ts` (navy `#1A2B3C`, accent `#FF6B35`).
- **Admin**: Ant Design 5 với `colorPrimary: #1A2B3C` để khớp brand. CSS-in-JS stream qua `AntdRegistry` → không flash unstyled.
- **Icon**: Lucide React (storefront) + @ant-design/icons (admin) + Material Symbols (mock compatibility).

---

## Còn lại (mở rộng nếu cần)

- Upload ảnh thực (S3 presigned) — hiện hỗ trợ URL trực tiếp.
- Reviews & ratings của khách trên product detail.
- Compare / recently viewed (cần local storage history).
- Multi-language (i18n) — hiện chỉ tiếng Việt.
- PWA (manifest + service worker).
- Form flash-sale create / banner schedule chi tiết.

---

Toàn bộ FE đã typecheck **PASS** (`pnpm typecheck`). ~100+ files TypeScript trên `app/`, `components/`, `lib/`. Cần đào sâu phần nào, cứ nói.
