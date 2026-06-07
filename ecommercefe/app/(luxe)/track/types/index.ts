export interface TimelineEvent {
  timestamp: string;
  status: string;
  description?: string;
  location?: string;
}

export interface Tracking {
  orderNumber: string;
  status: string;
  trackingNumber?: string;
  carrier?: string;
  events: TimelineEvent[];
}

export type TrackPhase = "idle" | "loading" | "found" | "not-found" | "error";
