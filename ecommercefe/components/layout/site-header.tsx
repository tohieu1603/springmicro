import Link from "next/link";
import { Container } from "@/components/ui/container";
import { env } from "@/lib/env";
import { getSession, isAdmin } from "@/lib/auth/session";
import { listCategories } from "@/lib/api/catalog";
import { buildCategoryTree } from "@/lib/categories";
import { CartBadge } from "./cart-badge";
import { MobileNav } from "./mobile-nav";
import { MegaMenu } from "./mega-menu";
import { Heart, Search } from "lucide-react";

/**
 * Sticky storefront header.
 *
 * Structure:
 *   Row 1 (announce strip)     — promos, hotline. Pure marketing.
 *   Row 2 (main nav)           — logo, mega-menu, search, account icons.
 *
 * Pre-rendered server-side using getSession() so the "Đăng nhập" /
 * "Tài khoản" state matches the user's cookie on first paint (no flicker).
 */
export async function SiteHeader() {
  const [user, categoriesFlat] = await Promise.all([
    getSession(),
    listCategories().catch(() => [] as never[]),
  ]);
  const admin = isAdmin(user);
  const categories = buildCategoryTree(categoriesFlat);

  return (
    <header className="sticky top-0 z-40 bg-white border-b border-border-base shadow-sm">
      {/* Announce strip */}
      <div className="bg-primary text-white/90 text-xs">
        <Container className="h-9 flex items-center justify-between">
          <span className="flex items-center gap-2">
            <span className="material-symbols-outlined text-base">local_shipping</span>
            Miễn phí vận chuyển cho đơn từ 500.000₫
          </span>
          <div className="hidden md:flex items-center gap-4">
            <span>Hotline: <strong>1900 1234</strong></span>
            <Link href="/account/orders" className="hover:text-accent">Theo dõi đơn</Link>
            <Link href="/help/faq" className="hover:text-accent">Trợ giúp</Link>
          </div>
        </Container>
      </div>

      {/* Main nav */}
      <Container className="h-20 flex items-center gap-6">
        <Link href="/" className="flex items-center gap-2 shrink-0">
          <div className="h-10 w-10 rounded-lg bg-primary text-white flex items-center justify-center font-bold text-lg">
            L
          </div>
          <div className="leading-tight hidden sm:block">
            <div className="font-bold text-base tracking-tight">{env.BRAND_NAME}</div>
            <div className="text-[10px] uppercase text-slate tracking-widest">Premium store</div>
          </div>
        </Link>

        <MegaMenu categories={categories} />

        <form action="/search" className="hidden md:flex flex-1 max-w-xl mx-2">
          <div className="relative w-full">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate" />
            <input
              type="search"
              name="q"
              placeholder="Tìm sản phẩm, thương hiệu, danh mục..."
              className="h-11 w-full rounded-lg border border-border-base bg-surface-soft pl-10 pr-24 text-sm focus:border-primary focus:outline-none focus:bg-white transition-colors"
            />
            <button
              type="submit"
              className="absolute right-1 top-1 h-9 px-4 rounded bg-primary text-white text-sm font-semibold hover:bg-primary-dark"
            >
              Tìm
            </button>
          </div>
        </form>

        <div className="flex items-center gap-1 shrink-0 ml-auto">
          <Link
            href="/account/wishlist"
            className="hidden md:inline-flex h-10 w-10 rounded-full hover:bg-surface-soft items-center justify-center transition-colors"
            aria-label="Yêu thích"
          >
            <Heart className="h-5 w-5" />
          </Link>
          <Link
            href="/cart"
            className="relative inline-flex h-10 w-10 rounded-full hover:bg-surface-soft items-center justify-center transition-colors"
            aria-label="Giỏ hàng"
          >
            <span className="material-symbols-outlined text-[22px]">shopping_cart</span>
            <CartBadge />
          </Link>

          {user ? (
            <div className="hidden md:flex items-center gap-1 ml-2 pl-3 border-l border-border-base">
              {admin && (
                <Link
                  href="/admin"
                  className="px-3 h-9 inline-flex items-center rounded text-sm font-semibold text-accent hover:bg-orange-50"
                >
                  Quản trị
                </Link>
              )}
              <Link
                href="/account"
                className="flex items-center gap-2 px-2 py-1.5 rounded hover:bg-surface-soft"
              >
                <div className="h-8 w-8 rounded-full bg-primary text-white text-sm font-semibold flex items-center justify-center">
                  {(user.fullName || user.username).charAt(0).toUpperCase()}
                </div>
                <div className="text-left leading-tight">
                  <div className="text-[11px] text-slate">Xin chào</div>
                  <div className="text-sm font-medium truncate max-w-[120px]">
                    {user.fullName || user.username}
                  </div>
                </div>
              </Link>
            </div>
          ) : (
            <div className="hidden md:flex items-center gap-2 ml-2 pl-3 border-l border-border-base">
              <Link
                href="/login"
                className="px-3 h-9 inline-flex items-center text-sm font-medium hover:text-accent"
              >
                Đăng nhập
              </Link>
              <Link
                href="/register"
                className="px-4 h-9 inline-flex items-center rounded bg-primary text-white text-sm font-semibold hover:bg-primary-dark"
              >
                Đăng ký
              </Link>
            </div>
          )}

          <MobileNav user={user} categories={categories} />
        </div>
      </Container>
    </header>
  );
}
