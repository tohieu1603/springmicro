import type { Metadata } from "next";

import { PolicyBlock } from "./components";
import { ReturnsFormIsland } from "./ReturnsFormIsland";

export const metadata: Metadata = {
  title: "HIEU — Returns & Exchanges",
  description:
    "Đổi/trả miễn phí trong 30 ngày kể từ ngày nhận, với hàng còn nguyên trạng và đầy đủ tem nhãn.",
  openGraph: {
    title: "HIEU — Returns & Exchanges",
    description: "Đổi/trả miễn phí trong 30 ngày — chính sách HIEU.",
    type: "website",
    siteName: "HIEU",
  },
};

/**
 * Returns page — RSC shell. Hero + intro + policy block render as static
 * HTML on the server (good for SEO + share previews). The form is a CSR
 * island that mounts in the middle of the layout.
 */
export default function ReturnsPage() {
  return (
    <section className="track-section">
      <div className="track-hero">
        <h1>RETURNS &amp; EXCHANGES</h1>
        <p>
          Đổi/trả trong 30 ngày kể từ ngày nhận, với hàng còn nguyên trạng. Điền form bên dưới để bắt đầu yêu cầu.
        </p>
      </div>

      <ReturnsFormIsland />

      <PolicyBlock />
    </section>
  );
}
