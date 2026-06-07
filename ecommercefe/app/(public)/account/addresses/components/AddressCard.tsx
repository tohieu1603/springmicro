"use client";

import { Button } from "@/components/ui/button";
import { Check, Edit2, Trash2 } from "lucide-react";

import type { Address } from "../types";

interface AddressCardProps {
  address: Address;
  onEdit: () => void;
  onSetDefault: () => void;
  onRemove: () => void;
}

export function AddressCard({ address, onEdit, onSetDefault, onRemove }: AddressCardProps) {
  return (
    <div className="rounded border border-border-base p-4 relative">
      <div className="flex items-center justify-between mb-1">
        <p className="font-semibold">{address.recipientName}</p>
        {address.isDefault && (
          <span className="text-[10px] bg-primary text-white px-1.5 py-0.5 rounded">Mặc định</span>
        )}
      </div>
      <p className="text-sm text-slate">{address.recipientPhone}</p>
      <p className="text-sm mt-2">
        {[address.street, address.ward, address.district, address.city, address.country]
          .filter(Boolean)
          .join(", ")}
      </p>
      <div className="mt-3 flex gap-2">
        <Button variant="ghost" size="sm" onClick={onEdit}>
          <Edit2 className="h-3.5 w-3.5" /> Sửa
        </Button>
        {!address.isDefault && (
          <Button variant="ghost" size="sm" onClick={onSetDefault}>
            <Check className="h-3.5 w-3.5" /> Mặc định
          </Button>
        )}
        <Button variant="ghost" size="sm" onClick={onRemove} className="text-danger">
          <Trash2 className="h-3.5 w-3.5" /> Xóa
        </Button>
      </div>
    </div>
  );
}
