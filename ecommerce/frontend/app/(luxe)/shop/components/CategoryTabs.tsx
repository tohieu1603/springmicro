"use client";

interface CategoryTabsProps {
  tabs: string[];
  activeTab: number;
  setActiveTab: (i: number) => void;
}

export function CategoryTabs({ tabs, activeTab, setActiveTab }: CategoryTabsProps) {
  return (
    <div className="cat-tabs">
      {tabs.map((t, i) => (
        <div
          key={t}
          className={`tab${i === activeTab ? " active" : ""}`}
          onClick={() => setActiveTab(i)}
          style={{ cursor: "pointer" }}
        >
          {t}
        </div>
      ))}
    </div>
  );
}
