import Link from "next/link";
import { Card, CardContent } from "@/components/ui/card";
import { Heart, MapPin, Package, User as UserIcon, type LucideIcon } from "lucide-react";

interface Tile {
  href: string;
  icon: LucideIcon;
  label: string;
  desc: string;
}

const TILES: Tile[] = [
  { href: "/account/orders", icon: Package, label: "Đơn hàng của tôi", desc: "Theo dõi, đánh giá, đổi trả" },
  { href: "/account/addresses", icon: MapPin, label: "Sổ địa chỉ", desc: "Quản lý địa chỉ giao hàng" },
  { href: "/account/wishlist", icon: Heart, label: "Yêu thích", desc: "Lưu sản phẩm để mua sau" },
  { href: "/account/profile", icon: UserIcon, label: "Hồ sơ", desc: "Cập nhật thông tin cá nhân" },
];

export function AccountTiles() {
  return (
    <div className="grid sm:grid-cols-2 gap-4 mt-8">
      {TILES.map((l) => {
        const Icon = l.icon;
        return (
          <Link key={l.href} href={l.href}>
            <Card className="hover:border-primary hover:shadow-soft transition-all cursor-pointer">
              <CardContent className="flex items-start gap-4">
                <div className="h-12 w-12 rounded bg-primary/5 text-primary flex items-center justify-center">
                  <Icon className="h-5 w-5" />
                </div>
                <div>
                  <p className="font-semibold">{l.label}</p>
                  <p className="text-sm text-slate mt-1">{l.desc}</p>
                </div>
              </CardContent>
            </Card>
          </Link>
        );
      })}
    </div>
  );
}
