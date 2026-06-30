---
name: react-fintech-ui
description: >
  Build or refactor a React component following the PayStream fintech UI design system.
  Use when creating dashboard pages, data tables, KPI cards, charts, or any UI component.
---

# React Fintech UI Component Builder

## Component File Template
```tsx
import { type FC } from 'react'
import { cn } from '@/lib/utils'

interface <ComponentName>Props {
  // all props typed explicitly
}

export const <ComponentName>: FC<<ComponentName>Props> = ({ ...props }) => {
  return <div className="...">{/* implementation */}</div>
}
```

## KPI Card Pattern
```tsx
export const KpiCard: FC<KpiCardProps> = ({ label, value, trend, trendDirection, period }) => (
  <div className="bg-surface-card border border-surface-border rounded-xl p-6">
    <p className="text-xs font-medium text-text-secondary uppercase tracking-widest">{label}</p>
    <p className="mt-2 text-3xl font-bold text-text-primary font-mono">{value}</p>
    <div className="mt-3 flex items-center gap-2">
      <span className={cn(
        "flex items-center gap-1 text-sm font-medium",
        trendDirection === 'up' ? 'text-status-success' : 'text-status-error'
      )}>
        {trendDirection === 'up' ? <TrendingUp size={14} /> : <TrendingDown size={14} />}
        {trend}
      </span>
      <span className="text-xs text-text-muted">{period}</span>
    </div>
  </div>
)
```

## Status Badge Pattern
```tsx
const STATUS_CONFIG = {
  completed: { color: 'text-status-success bg-status-success/10', icon: CheckCircle2, label: 'Completed' },
  pending:   { color: 'text-status-warning bg-status-warning/10', icon: Clock,         label: 'Pending' },
  failed:    { color: 'text-status-error   bg-status-error/10',   icon: XCircle,       label: 'Failed' },
  flagged:   { color: 'text-status-warning bg-status-warning/10', icon: AlertTriangle,  label: 'Flagged' },
} as const

export const StatusBadge: FC<{ status: keyof typeof STATUS_CONFIG }> = ({ status }) => {
  const { color, icon: Icon, label } = STATUS_CONFIG[status]
  return (
    <span className={cn('inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium', color)}>
      <Icon size={12} /> {label}
    </span>
  )
}
```

## TanStack Query Hook Pattern
```ts
export const useTransactions = (filters: TransactionFilters) =>
  useQuery({
    queryKey: ['transactions', filters],
    queryFn: () => apiClient.get<PageResponse<Transaction>>('/api/v1/transactions', { params: filters }),
    staleTime: 30_000,
    placeholderData: keepPreviousData,
  })
```

## Recharts Dark Theme Pattern
```tsx
<ResponsiveContainer width="100%" height={280}>
  <AreaChart data={data}>
    <defs>
      <linearGradient id="colorVolume" x1="0" y1="0" x2="0" y2="1">
        <stop offset="5%"  stopColor="#4f63e7" stopOpacity={0.3} />
        <stop offset="95%" stopColor="#4f63e7" stopOpacity={0} />
      </linearGradient>
    </defs>
    <CartesianGrid strokeDasharray="3 3" stroke="#2a2d3e" />
    <XAxis dataKey="date" tick={{ fill: '#94a3b8', fontSize: 12 }} />
    <YAxis tick={{ fill: '#94a3b8', fontSize: 12 }} />
    <Tooltip contentStyle={{ backgroundColor: '#1a1d27', border: '1px solid #2a2d3e', borderRadius: 8 }} />
    <Area type="monotone" dataKey="volume" stroke="#4f63e7" fill="url(#colorVolume)" strokeWidth={2} />
  </AreaChart>
</ResponsiveContainer>
```
