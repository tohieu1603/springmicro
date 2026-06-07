---
name: Professional E-commerce System
colors:
  surface: '#f7f9fb'
  surface-dim: '#d8dadc'
  surface-bright: '#f7f9fb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f6'
  surface-container: '#eceef0'
  surface-container-high: '#e6e8ea'
  surface-container-highest: '#e0e3e5'
  on-surface: '#191c1e'
  on-surface-variant: '#44474c'
  inverse-surface: '#2d3133'
  inverse-on-surface: '#eff1f3'
  outline: '#74777d'
  outline-variant: '#c4c6cd'
  surface-tint: '#4f6073'
  primary: '#041627'
  on-primary: '#ffffff'
  primary-container: '#1a2b3c'
  on-primary-container: '#8192a7'
  inverse-primary: '#b7c8de'
  secondary: '#ab3500'
  on-secondary: '#ffffff'
  secondary-container: '#fe6a34'
  on-secondary-container: '#5d1900'
  tertiary: '#0a1526'
  on-tertiary: '#ffffff'
  tertiary-container: '#1f2a3b'
  on-tertiary-container: '#8691a6'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d2e4fb'
  primary-fixed-dim: '#b7c8de'
  on-primary-fixed: '#0b1d2d'
  on-primary-fixed-variant: '#38485a'
  secondary-fixed: '#ffdbd0'
  secondary-fixed-dim: '#ffb59d'
  on-secondary-fixed: '#390c00'
  on-secondary-fixed-variant: '#832600'
  tertiary-fixed: '#d8e3fa'
  tertiary-fixed-dim: '#bcc7dd'
  on-tertiary-fixed: '#111c2c'
  on-tertiary-fixed-variant: '#3c475a'
  background: '#f7f9fb'
  on-background: '#191c1e'
  surface-variant: '#e0e3e5'
typography:
  h1-desktop:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  h1-mobile:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.2'
  h2-desktop:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '600'
    lineHeight: '1.3'
  h3-desktop:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.4'
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: '1.5'
  label-bold:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: '1.2'
  price-display:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '700'
    lineHeight: '1.2'
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  container-max: 1200px
  gutter: 24px
  margin-mobile: 16px
  stack-xs: 4px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 24px
  stack-xl: 48px
---

## Brand & Style

The design system is engineered for high-conversion e-commerce platforms that prioritize trust, clarity, and structural integrity. Drawing inspiration from premium retail templates, the aesthetic is "Corporate Modern"—avoiding fleeting trends in favor of a timeless, organized interface that facilitates complex product catalogs.

The target audience consists of discerning shoppers who value efficiency and professional presentation. The UI evokes a sense of reliability through precise alignment, generous whitespace, and a high-contrast color application that directs the user’s eye naturally toward conversion points.

## Colors

This design system utilizes a high-contrast palette to establish a clear hierarchy of information:

- **Deep Navy (#1A2B3C):** Used for primary branding, navigation bars, and footer backgrounds to ground the interface in professionalism.
- **Accent Orange (#FF6B35):** Reserved exclusively for Call-to-Action (CTA) elements like "Thêm vào giỏ hàng" (Add to Cart) and "Thanh toán" (Checkout) to ensure maximum visibility.
- **Slate Grey (#4A5568):** Applied to body text and secondary information to provide readability without the harshness of pure black.
- **System Neutrals:** A range of cool greys are used for borders (#E2E8F0) and background sections (#F8FAFC) to maintain a crisp, layered look.

## Typography

The typography uses **Inter** for its exceptional legibility in Vietnamese, particularly with complex diacritics. The hierarchy is strictly enforced:

- **Headlines:** Use heavy weights (600-700) to define sections clearly.
- **Body Text:** Uses a standard 16px base for optimal readability on e-commerce product descriptions.
- **Price Styling:** Prices are treated as a distinct typographic level, often utilizing the Accent Orange and bold weights to draw immediate attention.
- **Vietnamese Language Support:** Ensure line-heights are slightly more generous (1.5 - 1.6) for body text to accommodate Vietnamese tone marks without overlapping.

## Layout & Spacing

The design system follows a **Fixed Grid** approach for desktop screens to mirror the structured look of high-end marketplaces.

- **Desktop:** 12-column grid with a 1200px maximum width. Gutters are fixed at 24px.
- **Tablet:** 8-column fluid grid with 24px side margins.
- **Mobile:** 4-column fluid grid with 16px side margins.
- **Spacing Scale:** An 8px linear scale is used for all internal component padding and margin to ensure mathematical harmony across the UI.

## Elevation & Depth

To maintain a "crisp" and "structured" feel, the design system avoids heavy blurs. Depth is communicated through:

- **Flat Borders:** Most containers use a 1px solid border (#E2E8F0) to define boundaries without relying on shadows.
- **Subtle Elevation:** High-fidelity components like buttons and "active" product cards use a very soft shadow (0px 4px 12px rgba(0, 0, 0, 0.05)) to suggest interactivity.
- **Tonal Backgrounds:** Section headers or technical specifications use the Neutral background (#F8FAFC) to separate content visually from the white canvas.

## Shapes

The shape language is "Soft" (4px standard radius), reinforcing a professional and efficient tone. 

- **Buttons & Inputs:** Use the standard 4px radius (rounded-sm) to look modern yet stable.
- **Product Cards:** Utilize a 1px border with a 4px radius to frame product photography cleanly.
- **Badge/Labels:** Promotional labels (e.g., "Giảm giá" or "Mới") may use an even smaller 2px radius to maintain a compact, technical look.

## Components

### Buttons (Nút)
- **Primary:** Deep Navy background, white text. No shadow in default state; subtle shadow on hover.
- **CTA:** Accent Orange background, white text. This is the only component allowed to use a more prominent shadow to simulate "tactile" feedback.
- **Secondary:** Transparent background with 1px Deep Navy border.

### Product Cards (Thẻ sản phẩm)
- **Structure:** 1px border (#E2E8F0), 4px border radius. 
- **Image:** Square aspect ratio (1:1) with a slight grey background for the image container to handle products with white backgrounds.
- **Content:** Title in Slate Grey (body-md), Price in Accent Orange (price-display).

### Inputs (Trường nhập liệu)
- **Style:** 1px border with 4px radius. 
- **Focus State:** Border changes to Deep Navy (#1A2B3C) with a 2px outer glow in a very pale blue.
- **Labels:** Always positioned above the field in "label-bold" style.

### Badges (Nhãn trạng thái)
- Used for "Còn hàng" (In stock), "Hết hàng" (Out of stock), or "Giảm giá" (Discount). Small, uppercase typography with subtle background tints of green, red, or orange respectively.