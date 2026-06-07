import { Container } from "@/components/ui/container";
import { Skeleton, ProductCardSkeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <Container className="py-8">
      <Skeleton className="h-10 w-72 mb-6" />
      <Skeleton className="h-64 w-full mb-8 rounded-lg" />
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <ProductCardSkeleton key={i} />
        ))}
      </div>
    </Container>
  );
}
