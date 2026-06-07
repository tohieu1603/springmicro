"use client";

import React, { useState } from "react";
import { ConfigProvider, theme as antdTheme } from "antd";
import viVN from "antd/locale/vi_VN";
import { StyleProvider, createCache, extractStyle } from "@ant-design/cssinjs";
import { useServerInsertedHTML } from "next/navigation";

/**
 * Streams antd's runtime CSS into the SSR response so the first paint matches
 * the hydrated tree. Without this you get a flash of unstyled antd components.
 *
 * Theme is scoped: only consumers that render inside this provider see the
 * customised tokens (admin pages use antd; the public Luxury Mart store uses
 * Tailwind primitives, so the brand colour leak is contained).
 */
export function AntdRegistry({ children }: { children: React.ReactNode }) {
  const [cache] = useState(() => createCache());

  useServerInsertedHTML(() => (
    <style
      id="antd-cssinjs"
      dangerouslySetInnerHTML={{ __html: extractStyle(cache, true) }}
    />
  ));

  return (
    <StyleProvider cache={cache}>
      <ConfigProvider
        locale={viVN}
        theme={{
          algorithm: antdTheme.defaultAlgorithm,
          token: {
            colorPrimary: "#1A2B3C",
            colorInfo: "#1A2B3C",
            colorLink: "#1A2B3C",
            borderRadius: 4,
            fontFamily: "Inter, system-ui, sans-serif",
          },
        }}
      >
        {children}
      </ConfigProvider>
    </StyleProvider>
  );
}
