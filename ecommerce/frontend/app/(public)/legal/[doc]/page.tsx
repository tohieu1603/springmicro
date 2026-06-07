import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Container } from "@/components/ui/container";
import { Breadcrumb } from "@/components/ui/breadcrumb";

const DOCS: Record<string, { title: string; html: string }> = {
  terms: {
    title: "Điều khoản dịch vụ",
    html: `
<p>Cập nhật lần cuối: 2026-01-01</p>
<h3>1. Chấp nhận điều khoản</h3>
<p>Khi sử dụng dịch vụ của Luxury Mart, bạn đồng ý tuân thủ các điều khoản dưới đây...</p>
<h3>2. Tài khoản người dùng</h3>
<p>Bạn chịu trách nhiệm bảo mật mật khẩu và mọi hoạt động trên tài khoản của mình...</p>
<h3>3. Giao dịch</h3>
<p>Mọi giao dịch tuân theo luật pháp Việt Nam. Hóa đơn điện tử được xuất sau khi đơn hàng được xác nhận...</p>
<h3>4. Hạn chế trách nhiệm</h3>
<p>Luxury Mart không chịu trách nhiệm với các tổn thất gián tiếp phát sinh từ việc sử dụng dịch vụ ngoài phạm vi đã cam kết...</p>
<h3>5. Sửa đổi</h3>
<p>Chúng tôi có thể cập nhật điều khoản này. Phiên bản mới có hiệu lực khi đăng tải tại trang này.</p>
`,
  },
  privacy: {
    title: "Chính sách bảo mật",
    html: `
<p>Cập nhật lần cuối: 2026-01-01</p>
<h3>1. Thông tin chúng tôi thu thập</h3>
<p>Họ tên, email, số điện thoại, địa chỉ giao hàng, lịch sử đơn hàng, hành vi duyệt web...</p>
<h3>2. Mục đích sử dụng</h3>
<p>Xử lý đơn hàng, gửi thông báo, đề xuất sản phẩm, cải thiện trải nghiệm. Chúng tôi <b>không</b> bán dữ liệu cá nhân cho bên thứ ba...</p>
<h3>3. Bảo mật</h3>
<p>Mật khẩu được mã hoá bcrypt. Thẻ thanh toán không lưu trên hệ thống. Truyền dữ liệu qua HTTPS bắt buộc...</p>
<h3>4. Cookie</h3>
<p>Chúng tôi dùng cookie để duy trì phiên đăng nhập (HttpOnly) và cá nhân hoá nội dung...</p>
<h3>5. Quyền của bạn</h3>
<p>Bạn có quyền yêu cầu xem, sửa, hoặc xoá dữ liệu cá nhân. Gửi yêu cầu qua privacy@luxury-mart.vn.</p>
`,
  },
};

export function generateStaticParams() {
  return Object.keys(DOCS).map((doc) => ({ doc }));
}

export async function generateMetadata({
  params,
}: { params: Promise<{ doc: string }> }): Promise<Metadata> {
  const { doc } = await params;
  const data = DOCS[doc];
  if (!data) return { title: "Pháp lý — HIEU" };
  // Strip HTML to derive a plain description (first 160 chars).
  const plain = data.html.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim().slice(0, 160);
  return {
    title: `${data.title} — HIEU`,
    description: plain,
    openGraph: { title: data.title, description: plain, type: "article", siteName: "HIEU" },
    robots: { index: true, follow: true },
  };
}

export default async function LegalDoc({ params }: { params: Promise<{ doc: string }> }) {
  const { doc } = await params;
  const data = DOCS[doc];
  if (!data) notFound();

  return (
    <Container className="py-8 max-w-3xl">
      <Breadcrumb items={[{ label: data.title }]} />
      <h1 className="text-h2-d mt-4">{data.title}</h1>
      <article
        className="prose prose-sm max-w-none mt-6 text-on-surface-variant leading-relaxed [&_h3]:text-on-surface [&_h3]:font-bold [&_h3]:text-lg [&_h3]:mt-6"
        dangerouslySetInnerHTML={{ __html: data.html }}
      />
    </Container>
  );
}
