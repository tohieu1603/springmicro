"use client";

import { useNotifications } from "./hooks/useNotifications";
import { BellIcon } from "./components/BellIcon";
import { NotificationsPanel } from "./components/NotificationsPanel";

/**
 * Public container for the bell + dropdown. Drop into LuxeHeader.
 * Mounts its own God Hook so the header stays clean.
 */
export function BellNotifications() {
  const vm = useNotifications();
  return (
    <div className="lux-bell-wrap">
      <BellIcon unread={vm.unread} onClick={vm.toggle} />
      <NotificationsPanel
        open={vm.open}
        loading={vm.loading}
        authed={vm.authed}
        items={vm.items}
        onClose={vm.close}
        onMarkRead={vm.markRead}
        onMarkAllRead={vm.markAllRead}
      />
    </div>
  );
}
