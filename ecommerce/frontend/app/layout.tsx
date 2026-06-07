import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Toaster } from "sonner";
import { AntdRegistry } from "@/components/providers/antd-registry";
import { QueryProvider } from "@/lib/query/QueryProvider";
import { env } from "@/lib/env";

const inter = Inter({ subsets: ["latin", "vietnamese"], variable: "--font-inter" });

export const metadata: Metadata = {
  title: {
    default: `${env.BRAND_NAME} — ${env.BRAND_TAGLINE}`,
    template: `%s | ${env.BRAND_NAME}`,
  },
  description: env.BRAND_TAGLINE,
  icons: { icon: "/favicon.ico" },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi" className={inter.variable}>
      <head>
        <link
          href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap"
          rel="stylesheet"
        />
      </head>
      <body>
        <QueryProvider>
          <AntdRegistry>{children}</AntdRegistry>
          <Toaster
            position="top-right"
            closeButton
            className="lux-sonner"
            toastOptions={{ className: "lux-toast" }}
          />
        </QueryProvider>
      </body>
    </html>
  );
}
