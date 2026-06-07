"use client";

import { Skeleton } from "antd";

/**
 * Shown while the admin layout SSR's getSession() + dashboard data fetches.
 * Without this, the navigator sees a blank document for the duration of the
 * RSC fetch — which can be 1-5s on a cold gateway.
 */
export default function AdminLoading() {
  return (
    <div className="min-h-screen flex">
      <aside className="w-60 bg-primary text-white p-4 space-y-3">
        <Skeleton.Avatar active size={32} shape="square" />
        {Array.from({ length: 10 }).map((_, i) => (
          <Skeleton.Input key={i} active size="small" style={{ width: 180, opacity: 0.4 }} />
        ))}
      </aside>
      <main className="flex-1 p-6 space-y-4">
        <Skeleton active title paragraph={{ rows: 0 }} />
        <div className="grid md:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="border border-border-base rounded p-5 bg-white">
              <Skeleton.Input active size="small" style={{ width: 100 }} />
              <div className="mt-2"><Skeleton.Input active style={{ width: 160 }} /></div>
            </div>
          ))}
        </div>
        <div className="border border-border-base rounded p-5 bg-white">
          <Skeleton active paragraph={{ rows: 6 }} />
        </div>
      </main>
    </div>
  );
}
