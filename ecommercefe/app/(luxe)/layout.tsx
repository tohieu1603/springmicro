import LuxeHeader from "@/components/luxe/LuxeHeader";
import LuxeFooter from "@/components/luxe/LuxeFooter";
import { UIProvider } from "@/components/luxe/UIProvider";
import Overlays from "@/components/luxe/Overlays";
import "../luxe-home.css";

/**
 * Route group isolating the HIEU-style home page from the rest of the storefront.
 *
 * Why a separate group:
 *  - This experience uses a dark hero + sticky navbar + bespoke CSS lifted from
 *    the hieu-next prototype. Mounting it under `(public)/layout.tsx` would
 *    double-render the Luxury Mart SiteHeader / Footer.
 *  - The luxe CSS is scoped under `.luxe-root` so other pages keep Tailwind
 *    Preflight defaults; the wrapper here adds that class.
 */
export default function LuxeLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="luxe-root">
      <UIProvider>
        <LuxeHeader />
        <main>{children}</main>
        <LuxeFooter extended />
        <Overlays />
      </UIProvider>
    </div>
  );
}
