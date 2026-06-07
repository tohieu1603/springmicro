import Link from "next/link";
import { Container } from "@/components/ui/container";
import { env } from "@/lib/env";
import { Facebook, Instagram, Youtube, Mail, Phone } from "lucide-react";

export function SiteFooter() {
  return (
    <footer className="mt-16 bg-primary text-white/80">
      <Container className="py-12 grid gap-10 md:grid-cols-4">
        <div>
          <div className="flex items-center gap-2 mb-3">
            <div className="h-8 w-8 rounded bg-white text-primary flex items-center justify-center font-bold">
              L
            </div>
            <span className="font-bold text-lg text-white">{env.BRAND_NAME}</span>
          </div>
          <p className="text-sm leading-relaxed">{env.BRAND_TAGLINE}</p>
          <div className="flex gap-3 mt-4">
            <a href="#" className="p-2 rounded hover:bg-white/10" aria-label="Facebook">
              <Facebook className="h-4 w-4" />
            </a>
            <a href="#" className="p-2 rounded hover:bg-white/10" aria-label="Instagram">
              <Instagram className="h-4 w-4" />
            </a>
            <a href="#" className="p-2 rounded hover:bg-white/10" aria-label="Youtube">
              <Youtube className="h-4 w-4" />
            </a>
          </div>
        </div>

        <FooterCol
          title="Mua sắm"
          links={[
            { href: "/c/all", label: "Tất cả sản phẩm" },
            { href: "/c/new", label: "Mới về" },
            { href: "/c/sale", label: "Khuyến mãi" },
            { href: "/c/best-sellers", label: "Bán chạy" },
          ]}
        />
        <FooterCol
          title="Hỗ trợ"
          links={[
            { href: "/help/shipping", label: "Vận chuyển" },
            { href: "/help/returns", label: "Đổi trả" },
            { href: "/help/payment", label: "Thanh toán" },
            { href: "/help/faq", label: "Câu hỏi thường gặp" },
          ]}
        />
        <div>
          <h4 className="font-semibold text-white mb-3">Liên hệ</h4>
          <ul className="space-y-2 text-sm">
            <li className="flex items-center gap-2"><Phone className="h-4 w-4" /> 1900 1234</li>
            <li className="flex items-center gap-2"><Mail className="h-4 w-4" /> support@luxury-mart.vn</li>
          </ul>
        </div>
      </Container>
      <div className="border-t border-white/10">
        <Container className="py-4 text-xs flex flex-col md:flex-row justify-between items-center gap-2">
          <span>© {new Date().getFullYear()} {env.BRAND_NAME}. All rights reserved.</span>
          <div className="flex gap-4">
            <Link href="/legal/terms" className="hover:text-white">Điều khoản</Link>
            <Link href="/legal/privacy" className="hover:text-white">Bảo mật</Link>
          </div>
        </Container>
      </div>
    </footer>
  );
}

function FooterCol({ title, links }: { title: string; links: { href: string; label: string }[] }) {
  return (
    <div>
      <h4 className="font-semibold text-white mb-3">{title}</h4>
      <ul className="space-y-2 text-sm">
        {links.map((l) => (
          <li key={l.href}>
            <Link href={l.href} className="hover:text-white">{l.label}</Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
