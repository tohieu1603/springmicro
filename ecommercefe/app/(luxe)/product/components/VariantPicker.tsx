"use client";

import type { AttrGroup, PickedAttrs } from "../types";

interface VariantPickerProps {
  groups: AttrGroup[];
  picked: PickedAttrs;
  isReachable: (attrId: string, valId: string) => boolean;
  onPick: (attrId: string, valId: string) => void;
}

/**
 * Shopee-style grouped picker. One row per attribute (Color, Size, …).
 * COLOR values render with image swatches; others render as text pills.
 * Out-of-stock combinations are dashed + diagonally crossed.
 */
export function VariantPicker({ groups, picked, isReachable, onPick }: VariantPickerProps) {
  if (groups.length === 0) return null;

  return (
    <>
      {groups.map((g) => {
        const isColor = g.attrCode === "COLOR";
        return (
          <div key={g.attrId} className="pd-attr-group">
            <div className="pd-attr-label">
              <span>{g.attrName}</span>
              {picked[g.attrId] != null && (
                <span className="pd-attr-picked">
                  : {g.values.find((v) => v.valId === picked[g.attrId])?.valText}
                </span>
              )}
            </div>
            <div className={`pd-attr-options ${isColor ? "color" : "size"}`}>
              {g.values.map((v) => {
                const active = picked[g.attrId] === v.valId;
                const dead = !active && !isReachable(g.attrId, v.valId);
                const cls = `pd-opt ${isColor ? "color" : "size"}${active ? " active" : ""}${dead ? " dead" : ""}`;
                if (isColor && v.image) {
                  return (
                    <button
                      key={v.valId}
                      type="button"
                      className={cls}
                      title={v.valText}
                      disabled={dead}
                      onClick={() => onPick(g.attrId, v.valId)}
                    >
                      <span className="pd-opt-swatch" style={{ backgroundImage: `url(${v.image})` }} />
                      <span className="pd-opt-text">{v.valText}</span>
                    </button>
                  );
                }
                return (
                  <button
                    key={v.valId}
                    type="button"
                    className={cls}
                    disabled={dead}
                    onClick={() => onPick(g.attrId, v.valId)}
                  >
                    {v.valText}
                  </button>
                );
              })}
            </div>
          </div>
        );
      })}
    </>
  );
}
