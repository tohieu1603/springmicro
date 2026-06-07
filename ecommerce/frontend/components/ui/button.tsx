import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

/**
 * Public-page primary button. Three intents mapping to the Luxury Mart spec:
 *  - `primary`  → Deep Navy on white (default page CTAs)
 *  - `cta`      → Accent Orange — reserved for revenue-driving actions
 *                  (Add to cart, Checkout). Mock has subtle shadow + scale.
 *  - `secondary`→ Outlined navy — destructive-adjacent confirms
 *  - `ghost`    → Transparent — nav/util actions
 *
 * `asChild` lets the caller render a Link without losing styling.
 */
const buttonStyles = cva(
  "inline-flex items-center justify-center gap-2 font-medium transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-primary disabled:opacity-50 disabled:pointer-events-none whitespace-nowrap rounded select-none",
  {
    variants: {
      variant: {
        primary: "bg-primary text-white hover:bg-primary-dark shadow-sm",
        cta: "bg-accent text-white hover:bg-accent-dark shadow-cta active:scale-[0.98]",
        secondary: "bg-transparent text-primary border border-primary hover:bg-primary hover:text-white",
        ghost: "bg-transparent text-primary hover:bg-surface-soft",
        danger: "bg-danger text-white hover:bg-red-700",
        link: "bg-transparent text-primary underline-offset-4 hover:underline px-0 py-0",
      },
      size: {
        sm: "h-9 px-3 text-sm",
        md: "h-11 px-5 text-sm",
        lg: "h-12 px-6 text-base",
        icon: "h-10 w-10",
      },
    },
    defaultVariants: { variant: "primary", size: "md" },
  },
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonStyles> {
  asChild?: boolean;
  loading?: boolean;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, loading, children, disabled, ...props }, ref) => {
    // Radix <Slot> demands exactly one React child — injecting the spinner
    // alongside `children` makes it throw `React.Children.only`. With asChild
    // we forward as-is and skip the spinner (the consumer's <Link> usually
    // doesn't need loading anyway).
    if (asChild) {
      return (
        <Slot
          ref={ref}
          className={cn(buttonStyles({ variant, size }), className)}
          {...props}
        >
          {children}
        </Slot>
      );
    }
    return (
      <button
        ref={ref}
        className={cn(buttonStyles({ variant, size }), className)}
        disabled={disabled || loading}
        {...props}
      >
        {loading ? <Spinner /> : null}
        {children}
      </button>
    );
  },
);
Button.displayName = "Button";

function Spinner() {
  return (
    <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden>
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeOpacity="0.25" strokeWidth="4" />
      <path
        d="M4 12a8 8 0 018-8"
        stroke="currentColor"
        strokeWidth="4"
        strokeLinecap="round"
      />
    </svg>
  );
}
