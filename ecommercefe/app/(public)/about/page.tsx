import Image from "next/image";
import Link from "next/link";
import { Container } from "@/components/ui/container";
import { Button } from "@/components/ui/button";
import { env } from "@/lib/env";

export const metadata = {
  title: "Về chúng tôi",
  description: "Câu chuyện và sứ mệnh của Luxury Mart — đem trải nghiệm mua sắm đẳng cấp đến mọi nhà.",
};

export default function AboutPage() {
  return (
    <>
      <section className="bg-primary text-white py-20">
        <Container className="text-center">
          <p className="text-accent uppercase text-xs tracking-widest font-semibold">Câu chuyện</p>
          <h1 className="mt-3 text-4xl md:text-5xl font-bold">Về {env.BRAND_NAME}</h1>
          <p className="mt-4 text-lg text-white/80 max-w-2xl mx-auto">
            Chúng tôi tin rằng mua sắm là một trải nghiệm. Mỗi sản phẩm, mỗi đơn hàng đều đáng để được trau chuốt.
          </p>
        </Container>
      </section>

      <Container className="py-16 grid md:grid-cols-2 gap-12 items-center">
        <div className="relative aspect-video rounded-lg overflow-hidden">
          <Image
            src="https://images.unsplash.com/photo-1556761175-5973dc0f32e7?auto=format&fit=crop&w=1200&q=80"
            alt="Đội ngũ"
            fill
            sizes="600px"
            className="object-cover"
          />
        </div>
        <div>
          <h2 className="text-h2-d">Sứ mệnh</h2>
          <p className="mt-3 text-slate leading-relaxed">
            Mang đến nền tảng thương mại điện tử chuẩn quốc tế cho người Việt — giá tốt, hàng thật,
            giao nhanh. Mỗi ngày chúng tôi phục vụ hàng nghìn đơn hàng từ khắp 63 tỉnh thành.
          </p>
          <ul className="mt-5 space-y-2 text-sm">
            <li className="flex gap-2"><span className="text-accent">✓</span> 10.000+ sản phẩm chính hãng</li>
            <li className="flex gap-2"><span className="text-accent">✓</span> 50.000+ khách hàng đã mua sắm</li>
            <li className="flex gap-2"><span className="text-accent">✓</span> 4.9/5 điểm hài lòng trung bình</li>
            <li className="flex gap-2"><span className="text-accent">✓</span> Đối tác với 200+ thương hiệu</li>
          </ul>
        </div>
      </Container>

      <section className="bg-surface-soft py-16">
        <Container>
          <h2 className="text-h2-d text-center">Giá trị cốt lõi</h2>
          <div className="mt-10 grid md:grid-cols-3 gap-6">
            {[
              { icon: "verified", title: "Chính trực", desc: "Hàng thật giá thật, không tâng giá ảo." },
              { icon: "rocket_launch", title: "Tốc độ", desc: "Đặt hàng hôm nay, nhận trong 24h ở các thành phố lớn." },
              { icon: "favorite", title: "Tận tâm", desc: "Hỗ trợ trước và sau bán đều tận tình 24/7." },
            ].map((v) => (
              <div key={v.title} className="rounded-lg border border-border-base bg-white p-6">
                <span className="material-symbols-outlined text-3xl text-accent">{v.icon}</span>
                <h3 className="mt-3 font-bold">{v.title}</h3>
                <p className="text-sm text-slate mt-2 leading-relaxed">{v.desc}</p>
              </div>
            ))}
          </div>
        </Container>
      </section>

      <Container className="py-16 text-center">
        <h2 className="text-h2-d">Hợp tác cùng chúng tôi</h2>
        <p className="mt-3 text-slate max-w-md mx-auto">Trở thành đối tác cung ứng hoặc ứng tuyển vào đội ngũ.</p>
        <div className="mt-6 flex gap-3 justify-center">
          <Button asChild variant="cta"><Link href="/contact">Liên hệ</Link></Button>
          <Button asChild variant="secondary"><Link href="/careers">Tuyển dụng</Link></Button>
        </div>
      </Container>
    </>
  );
}
