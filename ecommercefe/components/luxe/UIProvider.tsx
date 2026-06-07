"use client";

import { createContext, useContext, useState, ReactNode } from "react";

type Overlay = "cart" | "search" | "menu" | null;

type UICtx = {
  overlay: Overlay;
  open: (o: Exclude<Overlay, null>) => void;
  close: () => void;
};

const Ctx = createContext<UICtx | null>(null);

export function UIProvider({ children }: { children: ReactNode }) {
  const [overlay, setOverlay] = useState<Overlay>(null);
  return (
    <Ctx.Provider
      value={{
        overlay,
        open: (o) => setOverlay(o),
        close: () => setOverlay(null),
      }}
    >
      {children}
    </Ctx.Provider>
  );
}

export function useUI() {
  const v = useContext(Ctx);
  if (!v) throw new Error("useUI must be inside UIProvider");
  return v;
}
