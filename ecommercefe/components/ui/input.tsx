import * as React from "react";
import { cn } from "@/lib/utils";

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type = "text", ...props }, ref) => (
    <input
      ref={ref}
      type={type}
      className={cn(
        "h-11 w-full rounded border border-border-base bg-white px-3 text-sm text-on-surface placeholder:text-slate transition-shadow",
        "focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/10",
        "disabled:bg-surface-soft disabled:cursor-not-allowed",
        className,
      )}
      {...props}
    />
  ),
);
Input.displayName = "Input";

export const Textarea = React.forwardRef<
  HTMLTextAreaElement,
  React.TextareaHTMLAttributes<HTMLTextAreaElement>
>(({ className, ...props }, ref) => (
  <textarea
    ref={ref}
    className={cn(
      "min-h-[120px] w-full rounded border border-border-base bg-white px-3 py-2 text-sm transition-shadow",
      "focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/10",
      className,
    )}
    {...props}
  />
));
Textarea.displayName = "Textarea";

export function Label({ className, ...props }: React.LabelHTMLAttributes<HTMLLabelElement>) {
  return (
    <label
      className={cn("text-label-bold text-on-surface block mb-1.5", className)}
      {...props}
    />
  );
}

/** Composite field: label + input/textarea slot + error text. */
export function Field({
  label,
  required,
  error,
  hint,
  children,
}: {
  label?: string;
  required?: boolean;
  error?: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      {label && (
        <Label>
          {label}
          {required && <span className="text-danger ml-0.5">*</span>}
        </Label>
      )}
      {children}
      {hint && !error && <p className="mt-1 text-xs text-slate">{hint}</p>}
      {error && <p className="mt-1 text-xs text-danger">{error}</p>}
    </div>
  );
}
