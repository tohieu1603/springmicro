"use client";

import { Empty } from "@/components/ui/empty";
import type { Address } from "../types";
import { AddressCard } from "./AddressCard";

interface AddressListProps {
  loading: boolean;
  list: Address[];
  onEdit: (a: Address) => void;
  onSetDefault: (id: string) => void;
  onRemove: (id: string) => void;
}

export function AddressList({ loading, list, onEdit, onSetDefault, onRemove }: AddressListProps) {
  if (loading) return <div className="h-32 animate-pulse rounded bg-surface-container" />;
  if (list.length === 0) {
    return <Empty title="Chưa có địa chỉ nào" description="Thêm địa chỉ để thanh toán nhanh hơn." />;
  }
  return (
    <div className="grid md:grid-cols-2 gap-4">
      {list.map((a) => (
        <AddressCard
          key={a.id}
          address={a}
          onEdit={() => onEdit(a)}
          onSetDefault={() => onSetDefault(a.id)}
          onRemove={() => onRemove(a.id)}
        />
      ))}
    </div>
  );
}
