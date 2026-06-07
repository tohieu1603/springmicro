import type { NextConfig } from "next";

const config: NextConfig = {
  reactStrictMode: true,
  // Keep trailing slashes intact so /api/proxy/api/inventory/ doesn't get
  // 308-redirected before our route handler sees it. The Spring BE distinguishes
  // /api/inventory (404) from /api/inventory/ (the actual list endpoint).
  skipTrailingSlashRedirect: true,
  images: {
    remotePatterns: [
      { protocol: "https", hostname: "**" },
      { protocol: "http", hostname: "localhost" },
    ],
  },
  // antd v5 + React 19 compat
  transpilePackages: ["antd", "@ant-design/icons", "rc-util", "rc-pagination", "rc-picker"],
};

export default config;
