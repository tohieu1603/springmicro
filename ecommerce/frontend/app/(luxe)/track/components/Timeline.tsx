"use client";

import type { Tracking } from "../types";

interface TimelineProps {
  data: Tracking;
  currentStep: number;
  steps: string[];
}

export function Timeline({ data, currentStep, steps }: TimelineProps) {
  return (
    <div className="track-result">
      <div className="track-summary">
        <div>
          <span className="track-label">ORDER</span>
          <span className="track-value">{data.orderNumber}</span>
        </div>
        <div>
          <span className="track-label">STATUS</span>
          <span className="track-status">{data.status}</span>
        </div>
        {data.trackingNumber && (
          <div>
            <span className="track-label">TRACKING NO.</span>
            <span className="track-value">
              {data.trackingNumber}{data.carrier ? ` · ${data.carrier}` : ""}
            </span>
          </div>
        )}
      </div>

      <div className="track-steps">
        {steps.map((s, i) => (
          <div
            key={s}
            className={`track-step${i <= currentStep ? " done" : ""}${i === currentStep ? " current" : ""}`}
          >
            <div className="track-step-dot" />
            <div className="track-step-label">{s}</div>
          </div>
        ))}
      </div>

      <ol className="track-timeline">
        {data.events.map((e, idx) => (
          <li key={idx} className={idx === 0 ? "first" : ""}>
            <span className="track-time">{new Date(e.timestamp).toLocaleString("vi-VN")}</span>
            <span className="track-event-status">{e.status}</span>
            {e.description && <span className="track-event-desc">{e.description}</span>}
            {e.location && <span className="track-event-loc">{e.location}</span>}
          </li>
        ))}
      </ol>
    </div>
  );
}
