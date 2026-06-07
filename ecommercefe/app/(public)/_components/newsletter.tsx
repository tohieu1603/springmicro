"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Container } from "@/components/ui/container";
import { Mail } from "lucide-react";

export function Newsletter() {
  const [email, setEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!email.includes("@")) {
      toast.error("Email không hợp lệ");
      return;
    }
    setSubmitting(true);
    await new Promise((r) => setTimeout(r, 600));
    toast.success("Đã đăng ký nhận tin", { description: "Cảm ơn bạn!" });
    setEmail("");
    setSubmitting(false);
  }

  return (
    <section className="py-16 bg-primary text-white">
      <Container className="grid md:grid-cols-2 gap-8 items-center">
        <div>
          <p className="inline-flex items-center gap-2 text-accent font-semibold uppercase text-xs tracking-widest">
            <Mail className="h-3.5 w-3.5" /> Bản tin Luxury Mart
          </p>
          <h2 className="mt-3 text-3xl md:text-4xl font-bold leading-tight">
            Ưu đãi độc quyền vào hộp thư của bạn
          </h2>
          <p className="mt-3 text-white/70 max-w-md">
            Đăng ký để nhận voucher 10% lần đầu + cập nhật bộ sưu tập mới và flash sale sớm 24h.
          </p>
        </div>
        <form
          onSubmit={submit}
          className="flex flex-col sm:flex-row gap-3"
        >
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="email@cua-ban.com"
            className="h-12 flex-1 rounded-lg bg-white text-on-surface px-4 placeholder:text-slate"
          />
          <button
            type="submit"
            disabled={submitting}
            className="h-12 px-6 rounded-lg bg-accent text-white font-semibold hover:bg-accent-dark disabled:opacity-60 transition-colors"
          >
            {submitting ? "Đang gửi..." : "Đăng ký"}
          </button>
        </form>
      </Container>
    </section>
  );
}
