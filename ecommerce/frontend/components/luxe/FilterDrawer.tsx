"use client";

import { useState } from "react";

type GroupKey = "category" | "color" | "material" | "size" | "price";

const GROUPS: { key: GroupKey; label: string }[] = [
  { key: "category", label: "CATEGORY" },
  { key: "color",    label: "COLOR" },
  { key: "material", label: "MATERIAL" },
  { key: "size",     label: "SIZE" },
  { key: "price",    label: "PRICE" },
];

const CATEGORY_OPTS = [
  "Crossbody Bags",
  "Mini Bags",
  "Shoulder Bags",
  "Tote Bags",
  "Top Handle Bags",
  "Backpacks",
  "Clutches",
  "Belt Bags",
];

const COLORS = [
  { name: "Black",  hex: "#000" },
  { name: "Brown",  hex: "#7a4b2a" },
  { name: "Beige",  hex: "#d6c5a8" },
  { name: "White",  hex: "#fff" },
  { name: "Red",    hex: "#b8252a" },
  { name: "Green",  hex: "#2d6a3a" },
  { name: "Pink",   hex: "#e8b8c0" },
  { name: "Blue",   hex: "#3a5a7a" },
];

const MATERIALS = ["Canvas", "Leather", "Suede", "Exotic"];
const SIZES = ["Mini", "Small", "Medium", "Large", "Extra Large"];
const PRICES = ["Under $1,000", "$1,000 - $2,000", "$2,000 - $3,000", "$3,000 - $5,000", "$5,000+"];

export default function FilterDrawer({ onClose }: { onClose: () => void }) {
  const [openGroup, setOpenGroup] = useState<GroupKey | null>("category");
  const [picked, setPicked] = useState<Record<string, boolean>>({});

  const toggle = (k: string) => setPicked((p) => ({ ...p, [k]: !p[k] }));

  const renderGroupBody = (k: GroupKey) => {
    if (k === "category")
      return CATEGORY_OPTS.map((c) => (
        <label key={c}>
          <input
            type="checkbox"
            checked={!!picked[`cat:${c}`]}
            onChange={() => toggle(`cat:${c}`)}
          />
          {c}
        </label>
      ));
    if (k === "color")
      return COLORS.map((c) => (
        <label key={c.name}>
          <input
            type="checkbox"
            checked={!!picked[`color:${c.name}`]}
            onChange={() => toggle(`color:${c.name}`)}
          />
          <span className="filter-color-swatch" style={{ background: c.hex }} />
          {c.name}
        </label>
      ));
    if (k === "material")
      return MATERIALS.map((m) => (
        <label key={m}>
          <input
            type="checkbox"
            checked={!!picked[`mat:${m}`]}
            onChange={() => toggle(`mat:${m}`)}
          />
          {m}
        </label>
      ));
    if (k === "size")
      return SIZES.map((s) => (
        <label key={s}>
          <input
            type="checkbox"
            checked={!!picked[`size:${s}`]}
            onChange={() => toggle(`size:${s}`)}
          />
          {s}
        </label>
      ));
    if (k === "price")
      return PRICES.map((p) => (
        <label key={p}>
          <input
            type="checkbox"
            checked={!!picked[`price:${p}`]}
            onChange={() => toggle(`price:${p}`)}
          />
          {p}
        </label>
      ));
    return null;
  };

  return (
    <div className="filter-overlay" onClick={onClose}>
      <div className="filter-drawer" onClick={(e) => e.stopPropagation()}>
        <div className="top">
          <h3>FILTERS</h3>
          <button className="close-x-sm" onClick={onClose} aria-label="close">
            ×
          </button>
        </div>

        {GROUPS.map((g) => (
          <div className="filter-group" key={g.key}>
            <div
              className="filter-group-head"
              onClick={() =>
                setOpenGroup((cur) => (cur === g.key ? null : g.key))
              }
            >
              <span>{g.label}</span>
              <span className="plus">{openGroup === g.key ? "−" : "+"}</span>
            </div>
            {openGroup === g.key && (
              <div className="filter-group-body">{renderGroupBody(g.key)}</div>
            )}
          </div>
        ))}

        <div className="filter-foot">
          <button className="clear" onClick={() => setPicked({})}>
            CLEAR
          </button>
          <button className="apply" onClick={onClose}>
            APPLY
          </button>
        </div>
      </div>
    </div>
  );
}
