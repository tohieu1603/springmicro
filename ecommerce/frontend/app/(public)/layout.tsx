import LuxeHeader from "@/components/luxe/LuxeHeader";
import LuxeFooter from "@/components/luxe/LuxeFooter";
import { UIProvider } from "@/components/luxe/UIProvider";
import Overlays from "@/components/luxe/Overlays";
import "../luxe-home.css";

/**
 * Storefront non-product pages (login, register, account, help, legal, …)
 * share the same HIEU chrome as the (luxe) experience for visual consistency.
 *
 * Pages render their existing Tailwind cards on top of the dark `.luxe-root`
 * background — Cards/forms stay white, navbar + footer stay dark. We don't
 * wrap content in `.luxe-root` directly; instead the layout adds a light
 * wrapper so Tailwind utilities used inside the cards keep working.
 */
export default function PublicLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="luxe-root">
      <UIProvider>
        <LuxeHeader />
        <main style={{ minHeight: "60vh", background: "#f7f7f5", color: "#000" }}>
          {children}
        </main>
        <LuxeFooter extended />
        <Overlays />
      </UIProvider>
    </div>
  );
}
