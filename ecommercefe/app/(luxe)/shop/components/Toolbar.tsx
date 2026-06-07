"use client";

import SortDropdown from "@/components/luxe/SortDropdown";

interface ToolbarProps {
  total: number;
  loaded: boolean;
  onOpenFilter: () => void;
}

export function Toolbar({ total, loaded, onOpenFilter }: ToolbarProps) {
  return (
    <div className="cat-toolbar">
      <SortDropdown />
      <div className="center">{loaded ? total : "…"} Items</div>
      <div className="filters" onClick={onOpenFilter} style={{ cursor: "pointer" }}>
        <u>Filters</u> <span>&#9776;</span>
      </div>
    </div>
  );
}
