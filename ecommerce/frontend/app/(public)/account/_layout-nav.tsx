"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Package, Heart, MapPin, User, Lock, Bell, LogOut } from "lucide-react";
import axios from "axios";
import { useRouter } from "next/navigation";

/**
 * Sidebar nav for /account/*. Server layout passes the current user so we can
 * pop the avatar/name without an extra fetch.
 */
const ITEMS = [
  { href: "/account", icon: User, label: "Tổng quan" },
  { href: "/account/orders", icon: Package, label: "Đơn hàng" },
  { href: "/account/wishlist", icon: Heart, label: "Yêu thích" },
  { href: "/account/addresses", icon: MapPin, label: "Sổ địa chỉ" },
  { href: "/account/notifications", icon: Bell, label: "Thông báo" },
  { href: "/account/profile", icon: User, label: "Hồ sơ" },
  { href: "/account/password", icon: Lock, label: "Đổi mật khẩu" },
];

export function AccountNav({ user }: { user: { fullName?: string; username: string; email: string } }) {
  const path = usePathname();
  const router = useRouter();

  async function logout() {
    try {
      await axios.post("/api/auth/logout", null, { withCredentials: true });
    } finally {
      router.replace("/");
      router.refresh();
    }
  }

  return (
    <aside className="space-y-3 lg:sticky lg:top-32 self-start">
      <div className="rounded-lg border border-border-base bg-white p-4 flex items-center gap-3">
        <div className="h-12 w-12 rounded-full bg-primary text-white font-bold flex items-center justify-center">
          {(user.fullName || user.username).charAt(0).toUpperCase()}
        </div>
        <div className="min-w-0">
          <p className="font-semibold truncate">{user.fullName || user.username}</p>
          <p className="text-xs text-slate truncate">{user.email}</p>
        </div>
      </div>
      <nav className="rounded-lg border border-border-base bg-white p-2">
        {ITEMS.map((i) => {
          const Icon = i.icon;
          const active = path === i.href;
          return (
            <Link
              key={i.href}
              href={i.href}
              className={
                "flex items-center gap-3 px-3 py-2.5 rounded text-sm transition-colors " +
                (active ? "bg-primary text-white" : "hover:bg-surface-soft")
              }
            >
              <Icon className="h-4 w-4" />
              {i.label}
            </Link>
          );
        })}
        <button
          onClick={logout}
          className="w-full mt-1 flex items-center gap-3 px-3 py-2.5 rounded text-sm text-danger hover:bg-red-50"
        >
          <LogOut className="h-4 w-4" /> Đăng xuất
        </button>
      </nav>
    </aside>
  );
}
