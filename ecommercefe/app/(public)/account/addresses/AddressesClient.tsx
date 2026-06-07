"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Plus } from "lucide-react";

import { useAddresses } from "./hooks/useAddresses";
import { AddressForm, AddressList } from "./components";

export function AddressesClient() {
  const vm = useAddresses();
  return (
    <Card>
      <CardHeader className="flex items-center justify-between">
        <CardTitle>Sổ địa chỉ</CardTitle>
        <Button onClick={() => vm.openForm()}>
          <Plus className="h-4 w-4" /> Thêm địa chỉ
        </Button>
      </CardHeader>
      <CardContent>
        <AddressList
          loading={vm.loading}
          list={vm.list}
          onEdit={vm.openForm}
          onSetDefault={vm.setDefault}
          onRemove={vm.remove}
        />

        <AddressForm
          mode={vm.editing}
          form={vm.form}
          setForm={vm.setForm}
          onSubmit={vm.save}
          onCancel={vm.closeForm}
        />
      </CardContent>
    </Card>
  );
}
