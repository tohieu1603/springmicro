import { Container } from "@/components/ui/container";

import { PublicVouchersApi } from "./services/api";
import { VoucherGrid } from "./components";

export const metadata = {
  title: "Mã giảm giá",
  description: "Tổng hợp các mã giảm giá đang hoạt động tại Luxury Mart.",
};

export default async function VouchersPublic() {
  const list = await PublicVouchersApi.listActive();

  return (
    <Container className="py-10">
      <h1 className="text-h2-d text-center">Mã giảm giá</h1>
      <p className="text-center text-slate mt-2">Săn deal hấp dẫn, nhập tại bước thanh toán</p>
      <VoucherGrid list={list} />
    </Container>
  );
}
