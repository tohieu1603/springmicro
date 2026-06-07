"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Container } from "@/components/ui/container";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Textarea, Field } from "@/components/ui/input";
import { Mail, Phone, MapPin, Clock } from "lucide-react";

export default function ContactPage() {
  const [form, setForm] = useState({ name: "", email: "", subject: "", message: "" });
  const [loading, setLoading] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    await new Promise((r) => setTimeout(r, 600));
    toast.success("Cảm ơn bạn đã liên hệ. Chúng tôi sẽ phản hồi trong 24h.");
    setForm({ name: "", email: "", subject: "", message: "" });
    setLoading(false);
  }

  return (
    <Container className="py-12">
      <h1 className="text-h2-d text-center">Liên hệ chúng tôi</h1>
      <p className="text-center text-slate mt-2 max-w-xl mx-auto">
        Có câu hỏi về sản phẩm, đơn hàng hoặc hợp tác? Gửi tin nhắn cho chúng tôi.
      </p>

      <div className="mt-10 grid lg:grid-cols-[1fr,360px] gap-8">
        <Card>
          <CardContent>
            <form onSubmit={submit} className="grid sm:grid-cols-2 gap-4">
              <Field label="Họ tên" required>
                <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </Field>
              <Field label="Email" required>
                <Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
              </Field>
              <div className="sm:col-span-2">
                <Field label="Tiêu đề" required>
                  <Input value={form.subject} onChange={(e) => setForm({ ...form, subject: e.target.value })} />
                </Field>
              </div>
              <div className="sm:col-span-2">
                <Field label="Nội dung" required>
                  <Textarea rows={6} value={form.message} onChange={(e) => setForm({ ...form, message: e.target.value })} />
                </Field>
              </div>
              <div className="sm:col-span-2">
                <Button variant="cta" type="submit" loading={loading}>Gửi liên hệ</Button>
              </div>
            </form>
          </CardContent>
        </Card>

        <div className="space-y-3">
          {[
            { icon: Phone, title: "Hotline", value: "1900 1234", desc: "8:00 - 22:00 hàng ngày" },
            { icon: Mail, title: "Email", value: "support@luxury-mart.vn", desc: "Phản hồi trong 24h" },
            { icon: MapPin, title: "Văn phòng", value: "Tầng 12, Tòa Bitexco, Quận 1, TP.HCM" },
            { icon: Clock, title: "Giờ làm việc", value: "T2 - CN: 8:00 - 22:00" },
          ].map((c, i) => {
            const Icon = c.icon;
            return (
              <Card key={i}>
                <CardContent className="flex gap-3 items-start">
                  <div className="h-10 w-10 rounded-full bg-orange-50 text-accent flex items-center justify-center shrink-0">
                    <Icon className="h-4 w-4" />
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-wider text-slate font-semibold">{c.title}</p>
                    <p className="font-semibold">{c.value}</p>
                    {c.desc && <p className="text-xs text-slate mt-0.5">{c.desc}</p>}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </div>
    </Container>
  );
}
