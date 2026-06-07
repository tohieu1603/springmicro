import type { Metadata } from "next";

import { TrackServerApi } from "./services/server";
import { TrackClient } from "./TrackClient";

export const metadata: Metadata = {
  title: "HIEU — Track Order",
  description: "Tra cứu trạng thái và hành trình giao hàng đơn HIEU của bạn.",
  openGraph: { title: "HIEU — Track Order", siteName: "HIEU" },
};

interface PageProps {
  searchParams: Promise<{ o?: string; phone?: string }>;
}

/**
 * /track is a public form. The shell (hero + form) is always SSR. If the URL
 * carries `?o=ORD&phone=09...` (post-checkout redirect, deep link), the SC
 * also pre-fetches the timeline server-side so the user lands on a fully
 * rendered tracking page without a client-side spinner.
 */
export default async function TrackPage({ searchParams }: PageProps) {
  const sp = await searchParams;
  const initialData = await TrackServerApi.lookup(sp.o ?? "", sp.phone ?? "");

  return (
    <TrackClient
      initialOrderNumber={sp.o ?? ""}
      initialPhone={sp.phone ?? ""}
      initialData={initialData}
    />
  );
}
