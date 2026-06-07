"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import axios from "axios";
import { Layout, Menu, Avatar, Dropdown, Badge, type MenuProps } from "antd";
import {
  DashboardOutlined,
  ShoppingOutlined,
  InboxOutlined,
  TagOutlined,
  UserOutlined,
  RollbackOutlined,
  FileTextOutlined,
  SettingOutlined,
  GlobalOutlined,
  AppstoreOutlined,
  LogoutOutlined,
  BellOutlined,
  PercentageOutlined,
  TeamOutlined,
  BarChartOutlined,
  NotificationOutlined,
  SafetyOutlined,
  AuditOutlined,
} from "@ant-design/icons";
import type { AuthUser } from "@/lib/api/types";

const { Sider, Header, Content } = Layout;

/**
 * Admin shell — antd Layout + Sider. Items map directly to /admin sub-routes;
 * the active path comes from `usePathname` so deep links highlight correctly.
 */

const ITEMS: MenuProps["items"] = [
  { key: "/admin", icon: <DashboardOutlined />, label: <Link href="/admin">Dashboard</Link> },
  { key: "/admin/reports", icon: <BarChartOutlined />, label: <Link href="/admin/reports">Báo cáo</Link> },
  {
    key: "catalog",
    icon: <AppstoreOutlined />,
    label: "Sản phẩm",
    children: [
      { key: "/admin/products", label: <Link href="/admin/products">Danh sách</Link> },
      { key: "/admin/products/new", label: <Link href="/admin/products/new">Thêm mới</Link> },
      { key: "/admin/categories", label: <Link href="/admin/categories">Danh mục</Link> },
      { key: "/admin/attrs", label: <Link href="/admin/attrs">Thuộc tính</Link> },
    ],
  },
  { key: "/admin/orders", icon: <ShoppingOutlined />, label: <Link href="/admin/orders">Đơn hàng</Link> },
  { key: "/admin/inventory", icon: <InboxOutlined />, label: <Link href="/admin/inventory">Kho hàng</Link> },
  { key: "/admin/returns", icon: <RollbackOutlined />, label: <Link href="/admin/returns">Trả hàng</Link> },
  {
    key: "marketing",
    icon: <NotificationOutlined />,
    label: "Marketing",
    children: [
      { key: "/admin/vouchers", label: <Link href="/admin/vouchers">Mã giảm giá</Link> },
      { key: "/admin/flash-sales", label: <Link href="/admin/flash-sales">Flash sale</Link> },
      { key: "/admin/banners", label: <Link href="/admin/banners">Banners</Link> },
      { key: "/admin/marketing", label: <Link href="/admin/marketing">Chiến dịch</Link> },
    ],
  },
  { key: "/admin/customers", icon: <TeamOutlined />, label: <Link href="/admin/customers">Khách hàng</Link> },
  { key: "/admin/users", icon: <UserOutlined />, label: <Link href="/admin/users">Tài khoản</Link> },
  { key: "/admin/permissions", icon: <SafetyOutlined />, label: <Link href="/admin/permissions">Vai trò</Link> },
  { key: "/admin/seo", icon: <GlobalOutlined />, label: <Link href="/admin/seo">SEO</Link> },
  { key: "/admin/activity", icon: <AuditOutlined />, label: <Link href="/admin/activity">Audit log</Link> },
  { key: "/admin/logs", icon: <FileTextOutlined />, label: <Link href="/admin/logs">Logs</Link> },
  { key: "/admin/settings", icon: <SettingOutlined />, label: <Link href="/admin/settings">Cài đặt</Link> },
];

export function AdminShell({ user, children }: { user: AuthUser; children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [collapsed, setCollapsed] = useState(false);

  async function logout() {
    try {
      await axios.post("/api/auth/logout", null, { withCredentials: true });
    } finally {
      router.replace("/login");
      router.refresh();
    }
  }

  const userMenu: MenuProps = {
    items: [
      { key: "profile", icon: <UserOutlined />, label: <Link href="/account">Hồ sơ</Link> },
      { key: "site", icon: <GlobalOutlined />, label: <Link href="/">Về trang chủ</Link> },
      { type: "divider" as const },
      { key: "logout", icon: <LogoutOutlined />, label: "Đăng xuất", onClick: logout },
    ],
  };

  // Find which top-level item to highlight — match exact, then by prefix.
  const selectedKey = (() => {
    const allKeys = ITEMS!.flatMap((i: any) =>
      (i.children || []).map((c: any) => c.key).concat([i.key]),
    );
    return (
      allKeys.find((k) => typeof k === "string" && k === pathname) ||
      allKeys.find((k) => typeof k === "string" && pathname.startsWith(k)) ||
      "/admin"
    );
  })();

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        width={240}
        theme="dark"
        style={{ background: "#041627" }}
      >
        <div className="h-16 flex items-center gap-2 px-4 border-b border-white/10">
          <div className="h-8 w-8 rounded bg-white text-primary flex items-center justify-center font-bold">L</div>
          {!collapsed && <span className="text-white font-bold">Luxury Admin</span>}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey as string]}
          defaultOpenKeys={["catalog"]}
          items={ITEMS}
          style={{ background: "transparent", borderRight: 0 }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: "#fff",
            padding: "0 24px",
            borderBottom: "1px solid #E2E8F0",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <h1 className="text-lg font-semibold text-on-surface m-0">
            {labelOf(pathname)}
          </h1>
          <div className="flex items-center gap-4">
            <Badge count={0} showZero={false}>
              <BellOutlined style={{ fontSize: 18 }} />
            </Badge>
            <Dropdown menu={userMenu} placement="bottomRight">
              <div className="flex items-center gap-2 cursor-pointer">
                <Avatar style={{ background: "#1A2B3C" }}>
                  {(user.fullName || user.username).charAt(0).toUpperCase()}
                </Avatar>
                <div className="hidden md:flex flex-col leading-tight">
                  <span className="text-sm font-medium">{user.fullName || user.username}</span>
                  <span className="text-xs text-slate">Quản trị viên</span>
                </div>
              </div>
            </Dropdown>
          </div>
        </Header>
        <Content style={{ background: "#f7f9fb", padding: 24 }}>{children}</Content>
      </Layout>
    </Layout>
  );
}

function labelOf(path: string): string {
  const map: Record<string, string> = {
    "/admin": "Dashboard",
    "/admin/reports": "Báo cáo",
    "/admin/products": "Sản phẩm",
    "/admin/products/new": "Thêm sản phẩm",
    "/admin/categories": "Danh mục",
    "/admin/attrs": "Thuộc tính",
    "/admin/orders": "Đơn hàng",
    "/admin/inventory": "Kho hàng",
    "/admin/returns": "Trả hàng",
    "/admin/vouchers": "Mã giảm giá",
    "/admin/flash-sales": "Flash sale",
    "/admin/banners": "Banners",
    "/admin/marketing": "Marketing",
    "/admin/customers": "Khách hàng",
    "/admin/users": "Tài khoản",
    "/admin/permissions": "Vai trò & quyền",
    "/admin/seo": "SEO",
    "/admin/activity": "Lịch sử hoạt động",
    "/admin/logs": "Logs",
    "/admin/settings": "Cài đặt",
  };
  if (map[path]) return map[path];
  // Detail pages — fall back to parent label + suffix
  const parent = Object.keys(map).reverse().find((k) => path.startsWith(k));
  return parent ? `${map[parent]} → Chi tiết` : "Quản trị";
}
