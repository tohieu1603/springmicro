"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

import { useAccountNotifications } from "./hooks/useAccountNotifications";
import { NotificationsList } from "./components";

export function NotificationsClient() {
  const vm = useAccountNotifications();
  return (
    <Card>
      <CardHeader>
        <CardTitle>Thông báo</CardTitle>
      </CardHeader>
      <CardContent>
        <NotificationsList loading={vm.loading} items={vm.items} onMarkRead={vm.markRead} />
      </CardContent>
    </Card>
  );
}
