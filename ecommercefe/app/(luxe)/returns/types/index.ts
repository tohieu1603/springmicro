export type Reason =
  | "WRONG_SIZE"
  | "DAMAGED"
  | "NOT_AS_DESCRIBED"
  | "CHANGED_MIND"
  | "OTHER";

export interface ReturnFormState {
  orderNumber: string;
  reason: Reason;
  description: string;
  email: string;
}

export type ReturnPhase = "idle" | "submitting" | "submitted";

export const REASONS: { v: Reason; label: string }[] = [
  { v: "WRONG_SIZE", label: "Wrong size or fit" },
  { v: "DAMAGED", label: "Item arrived damaged" },
  { v: "NOT_AS_DESCRIBED", label: "Not as described" },
  { v: "CHANGED_MIND", label: "Changed my mind" },
  { v: "OTHER", label: "Other" },
];
