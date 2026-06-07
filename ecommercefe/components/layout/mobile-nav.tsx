"use client";

import { useState } from "react";
import Link from "next/link";
import { Drawer } from "antd";
import { Menu as MenuIcon, X, ChevronRight, User } from "lucide-react";
import type { AuthUser, Category } from "@/lib/api/types";

export function MobileNav({ user, categories }: { user: AuthUser | null; categories: Category[] }) {
  const [open, setOpen] = useState(false);
  const [activeRoot, setActiveRoot] = useState<string | null>(null);

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        className="md:hidden h-10 w-10 inline-flex items-center justify-center rounded-full hover:bg-surface-soft"
        aria-label="Menu"
      >
        <MenuIcon className="h-5 w-5" />
      </button>

      <Drawer
        open={open}
        onClose={() => setOpen(false)}
        placement="left"
        width="85%"
        title={null}
        closable={false}
        styles={{ body: { padding: 0 } }}
      >
        <div className="bg-primary text-white p-5">
          {user ? (
            <Link href="/account" onClick={() => setOpen(false)} className="flex items-center gap-3">
              <div className="h-12 w-12 rounded-full bg-white text-primary flex items-center justify-center font-bold text-lg">
                {(user.fullName || user.username).charAt(0).toUpperCase()}
              </div>
              <div>
                <p className="text-xs text-white/70">Xin chào</p>
                <p className="font-semibold">{user.fullName || user.username}</p>
              </div>
            </Link>
          ) : (
            <div className="flex gap-2">
              <Link
                href="/login"
                onClick={() => setOpen(false)}
                className="flex-1 h-10 inline-flex items-center justify-center rounded bg-white text-primary font-semibold"
              >
                Đăng nhập
              </Link>
              <Link
                href="/register"
                onClick={() => setOpen(false)}
                className="flex-1 h-10 inline-flex items-center justify-center rounded border border-white/30 text-white font-semibold"
              >
                Đăng ký
              </Link>
            </div>
          )}
        </div>

        <nav className="p-2">
          <Section>Khám phá</Section>
          <Row href="/" onClose={() => setOpen(false)} label="Trang chủ" />
          <Row href="/c/new" onClose={() => setOpen(false)} label="Mới về" />
          <Row href="/c/sale" onClose={() => setOpen(false)} label="Khuyến mãi" />
          <Row href="/vouchers" onClose={() => setOpen(false)} label="Voucher" />

          {categories.length > 0 && (
            <>
              <Section>Danh mục</Section>
              {categories.map((c) => (
                <div key={c.id}>
                  <button
                    type="button"
                    className="w-full px-3 py-3 flex items-center justify-between text-sm font-medium hover:bg-surface-soft rounded"
                    onClick={() => setActiveRoot(activeRoot === c.id ? null : c.id)}
                  >
                    <span>{c.name}</span>
                    {c.children && c.children.length > 0 ? (
                      <ChevronRight
                        className={`h-4 w-4 transition-transform ${
                          activeRoot === c.id ? "rotate-90" : ""
                        }`}
                      />
                    ) : null}
                  </button>
                  {activeRoot === c.id && c.children && (
                    <div className="pl-4">
                      {c.children.map((sub) => (
                        <Link
                          key={sub.id}
                          href={`/c/${sub.slug}`}
                          onClick={() => setOpen(false)}
                          className="block px-3 py-2 text-sm text-on-surface-variant hover:bg-surface-soft rounded"
                        >
                          {sub.name}
                        </Link>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </>
          )}

          <Section>Tài khoản</Section>
          <Row href="/account/orders" onClose={() => setOpen(false)} label="Đơn hàng" />
          <Row href="/account/wishlist" onClose={() => setOpen(false)} label="Yêu thích" />
          <Row href="/account/addresses" onClose={() => setOpen(false)} label="Sổ địa chỉ" />
          <Row href="/help/faq" onClose={() => setOpen(false)} label="Trợ giúp" />
          <Row href="/contact" onClose={() => setOpen(false)} label="Liên hệ" />
        </nav>
      </Drawer>
    </>
  );
}

function Section({ children }: { children: React.ReactNode }) {
  return (
    <p className="px-3 pt-4 pb-1 text-[11px] uppercase tracking-wider text-slate font-semibold">
      {children}
    </p>
  );
}

function Row({ href, label, onClose }: { href: string; label: string; onClose: () => void }) {
  return (
    <Link
      href={href}
      onClick={onClose}
      className="flex items-center justify-between px-3 py-2.5 text-sm hover:bg-surface-soft rounded"
    >
      <span>{label}</span>
      <ChevronRight className="h-4 w-4 text-slate" />
    </Link>
  );
}
