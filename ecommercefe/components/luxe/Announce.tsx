"use client";

import { useEffect, useState } from "react";

export default function Announce() {
  const [hide, setHide] = useState(false);

  useEffect(() => {
    const onScroll = () => setHide(window.scrollY > 40);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <div className={`announce${hide ? " hide" : ""}`}>
      <div className="left">
        <div className="dot" style={{ ["--rot" as string]: "240deg" } as React.CSSProperties} />
        <span>3/3</span>
      </div>
      <div className="center">
        <a href="#">Introducing Generation Hieu</a>
      </div>
      <div className="right">
        <span className="pause" aria-label="pause" />
      </div>
    </div>
  );
}
