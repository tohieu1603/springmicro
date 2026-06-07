import Link from "next/link";
import { Container } from "@/components/ui/container";
import { Button } from "@/components/ui/button";

export default function NotFound() {
  return (
    <Container className="py-24 flex flex-col items-center text-center">
      <h1 className="text-6xl font-bold text-primary">404</h1>
      <p className="mt-4 text-h3-d">Không tìm thấy trang</p>
      <p className="mt-2 text-slate max-w-md">
        Trang bạn đang tìm không còn tồn tại hoặc đã được di chuyển.
      </p>
      <Button asChild variant="cta" className="mt-6">
        <Link href="/">Về trang chủ</Link>
      </Button>
    </Container>
  );
}
