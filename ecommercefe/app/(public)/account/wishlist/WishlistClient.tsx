"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

import { useWishlist } from "./hooks/useWishlist";
import { WishlistGrid } from "./components";

export function WishlistClient() {
  const vm = useWishlist();
  return (
    <Card>
      <CardHeader>
        <CardTitle>Yêu thích ({vm.items.length})</CardTitle>
      </CardHeader>
      <CardContent>
        <WishlistGrid loading={vm.loading} items={vm.items} />
      </CardContent>
    </Card>
  );
}
