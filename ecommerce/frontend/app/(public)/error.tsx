"use client";

import { Container } from "@/components/ui/container";
import { Button } from "@/components/ui/button";
import { AlertTriangle } from "lucide-react";

export default function PublicError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <Container className="py-24 text-center">
      <div className="mx-auto h-16 w-16 rounded-full bg-red-50 text-danger flex items-center justify-center">
        <AlertTriangle className="h-7 w-7" />
      </div>
      <h2 className="mt-5 text-h2-d">Có lỗi xảy ra</h2>
      <p className="mt-2 text-slate max-w-md mx-auto">
        Hệ thống tạm thời gặp sự cố. Bạn có thể thử lại sau ít phút.
      </p>
      {error.digest && (
        <p className="mt-2 text-xs text-slate">Mã sự cố: <code>{error.digest}</code></p>
      )}
      <div className="mt-6 flex justify-center gap-3">
        <Button variant="cta" onClick={reset}>Thử lại</Button>
        <Button variant="secondary" asChild>
          <a href="/">Về trang chủ</a>
        </Button>
      </div>
    </Container>
  );
}
