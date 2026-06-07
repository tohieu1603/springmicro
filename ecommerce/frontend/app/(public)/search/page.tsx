import { Container } from "@/components/ui/container";

import { SearchApi } from "./services/api";
import { SearchHeader, SearchResults } from "./components";

interface PageProps {
  searchParams: Promise<{ q?: string; page?: string }>;
}

export default async function SearchPage({ searchParams }: PageProps) {
  const sp = await searchParams;
  const q = (sp.q ?? "").trim();
  const page = Math.max(0, Number(sp.page ?? 0) || 0);

  const { products, total } = await SearchApi.run(q, page);

  return (
    <Container className="py-8">
      <SearchHeader query={q} total={total} />
      <SearchResults products={products} query={q} />
    </Container>
  );
}
