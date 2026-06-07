import type { Config } from "tailwindcss";

// Luxury Mart design tokens — Corporate Modern e-commerce.
// Public pages reach for these custom tokens; admin pages rely on shadcn + antd themes.
const config: Config = {
  darkMode: "class",
  content: [
    "./app/**/*.{ts,tsx}",
    "./components/**/*.{ts,tsx}",
    "./lib/**/*.{ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: "#1A2B3C",
        "primary-dark": "#041627",
        accent: "#FF6B35",
        "accent-dark": "#ab3500",
        slate: "#4A5568",
        "border-base": "#E2E8F0",
        "surface-soft": "#F8FAFC",
        // Mirror Material-style tokens used in mock HTML.
        surface: "#f7f9fb",
        "on-surface": "#191c1e",
        "on-surface-variant": "#44474c",
        success: "#10B981",
        warning: "#F59E0B",
        danger: "#DC2626",
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
      },
      fontSize: {
        "h1-d": ["48px", { lineHeight: "1.2", letterSpacing: "-0.02em", fontWeight: "700" }],
        "h1-m": ["32px", { lineHeight: "1.2", fontWeight: "700" }],
        "h2-d": ["36px", { lineHeight: "1.3", fontWeight: "600" }],
        "h3-d": ["24px", { lineHeight: "1.4", fontWeight: "600" }],
        "body-lg": ["18px", { lineHeight: "1.6" }],
        "body-md": ["16px", { lineHeight: "1.6" }],
        "body-sm": ["14px", { lineHeight: "1.5" }],
        "label-bold": ["14px", { lineHeight: "1.2", fontWeight: "600" }],
        "price": ["20px", { lineHeight: "1.2", fontWeight: "700" }],
      },
      borderRadius: {
        DEFAULT: "4px",
        sm: "2px",
        md: "6px",
        lg: "8px",
        xl: "12px",
      },
      spacing: {
        gutter: "24px",
        "container-max": "1200px",
      },
      maxWidth: {
        container: "1200px",
      },
      boxShadow: {
        soft: "0 4px 12px rgba(0, 0, 0, 0.05)",
        cta: "0 6px 16px rgba(255, 107, 53, 0.25)",
      },
    },
  },
  plugins: [],
};

export default config;
