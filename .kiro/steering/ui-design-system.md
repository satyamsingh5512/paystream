---
inclusion: fileMatch
fileMatch: ["frontend/**/*.tsx", "frontend/**/*.ts", "frontend/**/*.css"]
---

# PayStream – UI/UX Design System

## Design Philosophy
The PayStream UI must feel like **Stripe Dashboard meets Linear** — minimal, data-dense, fast, and trustworthy. Every pixel should communicate professionalism.

## Color Palette (tailwind.config.ts)
```js
colors: {
  brand: {
    50:  '#f0f4ff',
    500: '#4f63e7',
    600: '#3d4fd6',
    900: '#1a237e',
  },
  surface: {
    bg:     '#0f1117',
    card:   '#1a1d27',
    border: '#2a2d3e',
    muted:  '#3a3d52',
  },
  status: {
    success: '#22c55e',
    warning: '#f59e0b',
    error:   '#ef4444',
    info:    '#3b82f6',
  },
  text: {
    primary:   '#f1f5f9',
    secondary: '#94a3b8',
    muted:     '#475569',
  }
}
```

## Typography
- **Font**: Inter (body), JetBrains Mono (amounts, IDs, hashes)
- **Weights**: 400 body / 500 labels / 600 headings / 700 KPI numbers

## Spacing
- Card inner padding: `p-6`. Section gap: `gap-6`. Within card: `gap-4`.

## Component Patterns

### KPI Card
```tsx
<KpiCard label="Total Volume" value="₹ 2,40,00,000" trend="+12.4%" trendDirection="up" period="Last 30 days" />
```

### Status Badge
```tsx
// status: 'completed' | 'pending' | 'failed' | 'flagged'
// Always renders: colored dot + icon + label — never color alone
<StatusBadge status="completed" />
```

### Data Table
- Sticky header, alternating row shading, sortable columns with ChevronUp/Down icons
- Empty state: centered illustration + descriptive message

### Real-time Feed
- New items slide in from top with 200ms ease-out transition
- "New transaction" toast appears bottom-right (non-blocking)

## Layout
- Sidebar: Fixed 240px, dark. Logo top-left. User profile bottom.
- Main content: `max-w-7xl mx-auto px-6`
- Top header: `h-14`, page title + breadcrumb + right-aligned actions

## Animation
- Hover/focus: `transition-all duration-200 ease-out`
- Page transitions: 150ms fade via Framer Motion AnimatePresence
- NEVER use infinite animations on non-loading elements

## Accessibility
- All interactive elements: `focus-visible:ring-2 focus-visible:ring-brand-500`
- Color is NEVER the only status indicator — always pair color + icon + text
- ARIA labels on all icon-only buttons
- Color contrast: AA minimum (4.5:1)
