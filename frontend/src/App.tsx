import { useCallback, useEffect, useMemo, useState } from 'react'

type Customer = {
  id: number
  name: string
  phone: string
  amountDue: number
  paymentStatus: string
  failureReason: string | null
  createdAt: string
}

type CallRecord = {
  callId: number
  customerId: number
  customerName: string
  status: string
  outcome: string | null
  durationSeconds: number | null
  startedAt: string
}

type RecoveryAction = {
  actionId: number
  customerId: number
  actionType: string
  status: string
  callbackTime: string | null
  details: string | null
  createdAt: string
}

type Metrics = {
  totalCalls: number
  completedCalls: number
  recoveryRate: number
  averageCallDurationSeconds: number
  paymentLinksSent: number
  escalations: number
}

const API_BASE = (import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? 'http://localhost:8080' : '')).replace(/\/$/, '')
const API_KEY = import.meta.env.VITE_API_KEY || ''
const API_HEADERS = { 'X-API-Key': API_KEY }

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, { headers: API_HEADERS })
  if (!response.ok) throw new Error(await readError(response))
  return response.json() as Promise<T>
}

async function readError(response: Response): Promise<string> {
  try {
    const body = await response.json() as { message?: string; error?: string }
    return body.message || body.error || `Request failed (${response.status})`
  } catch {
    return `Request failed (${response.status})`
  }
}

function formatMoney(amount: number) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(amount)
}

function formatDate(value: string) {
  if (!value || Number.isNaN(new Date(value).getTime())) return '—'
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function formatDuration(seconds: number | null | undefined) {
  if (seconds == null) return '—'
  if (!Number.isFinite(seconds) || seconds < 0) return '—'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor(seconds / 60)
  const remainder = seconds % 60
  if (hours) return `${hours}h ${Math.floor((seconds % 3600) / 60).toString().padStart(2, '0')}m`
  return minutes ? `${minutes}m ${remainder.toString().padStart(2, '0')}s` : `${remainder}s`
}

function readable(value: string | null | undefined) {
  if (!value) return '—'
  return value.toLowerCase().replaceAll('_', ' ').replaceAll('-', ' ').split(' ').map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(' ')
}

function Icon({ name, className = 'h-5 w-5' }: { name: string; className?: string }) {
  const common = { className, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 1.8, strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const, 'aria-hidden': true as const }
  if (name === 'logo') return <svg {...common}><path d="M4 12.5 9.2 18 20 6" /><path d="M4 5h5M4 5v5M20 19h-5m5 0v-5" /></svg>
  if (name === 'grid') return <svg {...common}><rect x="3.5" y="3.5" width="7" height="7" rx="1.5" /><rect x="13.5" y="3.5" width="7" height="7" rx="1.5" /><rect x="3.5" y="13.5" width="7" height="7" rx="1.5" /><rect x="13.5" y="13.5" width="7" height="7" rx="1.5" /></svg>
  if (name === 'users') return <svg {...common}><path d="M16 20v-1.5a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4V20" /><circle cx="9.5" cy="7" r="3.5" /><path d="M17 11a3.5 3.5 0 0 0 0-7m4 16v-1.5a4 4 0 0 0-3-3.87" /></svg>
  if (name === 'phone') return <svg {...common}><path d="M21 16.3v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 1.1 3.5 2 2 0 0 1 3.1 1.3h3a2 2 0 0 1 2 1.7c.13.94.35 1.87.68 2.76a2 2 0 0 1-.45 2.1L7.1 9.1a16 16 0 0 0 6 6l1.24-1.24a2 2 0 0 1 2.1-.45c.89.33 1.82.55 2.76.68a2 2 0 0 1 1.8 2.21Z" /></svg>
  if (name === 'activity') return <svg {...common}><path d="M3 12h4l3-8 4 16 3-8h4" /></svg>
  if (name === 'clock') return <svg {...common}><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></svg>
  if (name === 'link') return <svg {...common}><path d="M10 13a5 5 0 0 0 7.07 0l2-2A5 5 0 0 0 12 3.93l-1.15 1.14" /><path d="M14 11a5 5 0 0 0-7.07 0l-2 2A5 5 0 0 0 12 20.07l1.15-1.14" /></svg>
  if (name === 'alert') return <svg {...common}><path d="m10.3 3.9-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-3.1l-8-14a2 2 0 0 0-3.4 0Z" /><path d="M12 9v4m0 4h.01" /></svg>
  if (name === 'arrow') return <svg {...common}><path d="M5 12h14m-6-6 6 6-6 6" /></svg>
  if (name === 'refresh') return <svg {...common}><path d="M20 7v5h-5M4 17v-5h5" /><path d="M5.6 9A7 7 0 0 1 18 6l2 2M4 16l2 2a7 7 0 0 0 12.4-3" /></svg>
  return <svg {...common}><circle cx="12" cy="12" r="9" /></svg>
}

function StatusBadge({ value }: { value: string | null | undefined }) {
  const status = value?.toUpperCase() || 'UNKNOWN'
  const style = ['COMPLETED', 'SUCCESS', 'RECOVERED', 'PAYMENT_LINK_SENT', 'ALREADY_PAID'].includes(status)
    ? 'bg-emerald-50 text-emerald-700 ring-emerald-100'
    : ['FAILED', 'NO_ANSWER', 'CUSTOMER_REFUSED', 'DECLINED', 'INSUFFICIENT_FUNDS', 'EXPIRED_CARD', 'CARD_EXPIRED', 'BANK_DECLINED', 'AUTHENTICATION_FAILED', 'TECHNICAL_ERROR'].includes(status)
      ? 'bg-rose-50 text-rose-700 ring-rose-100'
      : ['IN_PROGRESS', 'RINGING', 'INITIATED', 'PENDING', 'RETRY_SCHEDULED'].includes(status)
        ? 'bg-amber-50 text-amber-700 ring-amber-100'
        : 'bg-slate-100 text-slate-600 ring-slate-200'
  return <span className={`inline-flex whitespace-nowrap rounded-full px-2.5 py-1 text-[11px] font-semibold tracking-wide ring-1 ring-inset ${style}`}>{readable(value)}</span>
}

function App() {
  const [customers, setCustomers] = useState<Customer[]>([])
  const [calls, setCalls] = useState<CallRecord[]>([])
  const [actions, setActions] = useState<RecoveryAction[]>([])
  const [metrics, setMetrics] = useState<Metrics | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [backendOnline, setBackendOnline] = useState<boolean | null>(null)
  const [callingCustomer, setCallingCustomer] = useState<number | null>(null)
  const [callMessage, setCallMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null)

  const loadDashboard = useCallback(async (showLoading = false) => {
    if (showLoading) setLoading(true)
    else setRefreshing(true)
    setLoadError('')
    try {
      const [customerData, callData, actionData, metricData] = await Promise.all([
        getJson<Customer[]>('/api/customers'),
        getJson<CallRecord[]>('/api/calls'),
        getJson<RecoveryAction[]>('/api/recovery-actions'),
        getJson<Metrics>('/api/metrics'),
      ])
      setCustomers(customerData)
      setCalls(callData)
      setActions(actionData)
      setMetrics(metricData)
      setBackendOnline(true)
      setLastUpdated(new Date())
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : 'Could not load dashboard data.')
      setBackendOnline(false)
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [])

  useEffect(() => {
    void getJson<{ status: string }>('/api/health')
      .then((health) => setBackendOnline(health.status === 'UP'))
      .catch(() => setBackendOnline(false))
    void loadDashboard(true)
    const timer = window.setInterval(() => {
      void loadDashboard()
      void getJson<{ status: string }>('/api/health')
        .then((health) => setBackendOnline(health.status === 'UP'))
        .catch(() => setBackendOnline(false))
    }, 30000)
    return () => window.clearInterval(timer)
  }, [loadDashboard])

  const metricCards = useMemo(() => [
    { label: 'Total calls', value: metrics?.totalCalls ?? '—', icon: 'phone', detail: 'All outbound attempts', tone: 'green' },
    { label: 'Completed calls', value: metrics?.completedCalls ?? '—', icon: 'activity', detail: 'Successfully concluded', tone: 'blue' },
    { label: 'Recovery rate', value: metrics ? `${metrics.recoveryRate.toFixed(1)}%` : '—', icon: 'grid', detail: 'Recovered / completed', tone: 'green' },
    { label: 'Avg. call duration', value: formatDuration(metrics?.averageCallDurationSeconds), icon: 'clock', detail: 'Completed call average', tone: 'violet' },
    { label: 'Payment links sent', value: metrics?.paymentLinksSent ?? '—', icon: 'link', detail: 'Recorded recovery outcomes', tone: 'amber' },
    { label: 'Escalations', value: metrics?.escalations ?? '—', icon: 'alert', detail: 'Support requests recorded', tone: 'rose' },
  ], [metrics])

  async function startCall(customer: Customer) {
    setCallingCustomer(customer.id)
    setCallMessage(null)
    try {
      const response = await fetch(`${API_BASE}/api/calls/outbound/${customer.id}`, { method: 'POST', headers: API_HEADERS })
      if (!response.ok) throw new Error(await readError(response))
      const result = await response.json() as { callId: number; status: string }
      setCallMessage({ type: 'success', text: `Call request created for ${customer.name} · #${result.callId} · ${readable(result.status)}.` })
      await loadDashboard()
    } catch (error) {
      setCallMessage({ type: 'error', text: error instanceof Error ? error.message : 'Could not start the recovery call.' })
      await loadDashboard()
    } finally {
      setCallingCustomer(null)
    }
  }

  return (
    <div className="min-h-screen bg-canvas text-ink">
      <aside className="sidebar fixed inset-y-0 left-0 z-20 flex w-[248px] flex-col bg-[#172b27] px-5 py-6 text-white">
        <div className="flex items-center gap-3 px-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#c6e8d7] text-brand"><Icon name="logo" className="h-6 w-6" /></div>
          <div><div className="text-[17px] font-bold tracking-tight">PayFlow</div><div className="text-[11px] font-medium text-emerald-100/60">RECOVERY CONSOLE</div></div>
        </div>
        <div className="mt-10 px-3 text-[10px] font-bold uppercase tracking-[.18em] text-white/35">Workspace</div>
        <nav className="mt-3 space-y-1">
          <a href="#overview" className="nav-link nav-link-active"><Icon name="grid" /> Overview</a>
          <a href="#customers" className="nav-link"><Icon name="users" /> Customers</a>
          <a href="#calls" className="nav-link"><Icon name="phone" /> Call history</a>
          <a href="#activity" className="nav-link"><Icon name="activity" /> Recovery activity</a>
        </nav>
        <div className="mt-auto rounded-2xl border border-white/10 bg-white/[.05] p-4">
          <div className="flex items-center gap-2 text-xs font-semibold text-white/85"><span className="h-2 w-2 rounded-full bg-[#89d9b1]" /> Demo environment</div>
          <p className="mt-2 text-[11px] leading-5 text-white/45">Fictional customers and mock payment processing.</p>
        </div>
        <div className="mt-5 px-3 text-[10px] text-white/35">PayFlow Recovery · Demo</div>
      </aside>

      <main className="main-content ml-[248px] min-h-screen px-8 py-7 xl:px-10" aria-busy={loading || refreshing}>
        <header className="flex flex-wrap items-center justify-between gap-4 border-b border-line pb-6">
          <div>
            <div className="text-xs font-semibold text-muted">PAYFLOW <span className="mx-1 text-slate-300">/</span> OVERVIEW</div>
            <h1 className="mt-2 text-[27px] font-semibold tracking-tight">Automated Payment Recovery</h1>
            <p className="mt-1 text-sm text-muted">Monitor conversations and help customers get back on track.</p>
          </div>
          <div className="flex items-center gap-3">
            <div className={`health-pill ${backendOnline ? 'health-up' : backendOnline === false ? 'health-down' : 'health-checking'}`} role="status" aria-live="polite">
              <span className="health-dot" aria-hidden="true" />
              {backendOnline ? 'System operational' : backendOnline === false ? 'Backend unavailable' : 'Checking system'}
            </div>
            <button type="button" onClick={() => void loadDashboard()} className="refresh-button" disabled={refreshing} aria-label="Refresh dashboard data">
              <Icon name="refresh" className={refreshing ? 'h-4 w-4 refresh-spinning' : 'h-4 w-4'} />
              <span>{refreshing ? 'Refreshing' : 'Refresh'}</span>
            </button>
          </div>
        </header>

        <section id="overview" className="pt-7">
          <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
            <div><h2 className="section-title">Recovery overview</h2><p className="section-subtitle">Live performance from your recovery data</p></div>
            <div className="text-xs text-muted">{lastUpdated ? `Updated ${lastUpdated.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}` : 'Waiting for data'}</div>
          </div>
          {loading && <div className="mb-4 h-1 overflow-hidden rounded-full bg-slate-200" role="progressbar" aria-label="Loading dashboard data"><div className="h-full w-1/3 rounded-full bg-brand loading-bar" /></div>}
          {loadError && <div className="mb-4 flex items-center justify-between gap-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800" role="alert"><span>Dashboard data unavailable: {loadError}</span><button className="retry-button" onClick={() => void loadDashboard(true)}>Try again</button></div>}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
            {metricCards.map((card) => <MetricCard key={card.label} {...card} loading={loading && metrics === null} />)}
          </div>
        </section>

        <section id="customers" className="pt-9">
          <div className="mb-4 flex items-end justify-between gap-3">
            <div><h2 className="section-title">Customers requiring action</h2><p className="section-subtitle">Review the balance and choose the next recovery step</p></div>
            <span className="rounded-full bg-white px-3 py-1.5 text-xs font-semibold text-muted ring-1 ring-line" aria-label={`${customers.length} customers`}>{customers.length} customers</span>
          </div>
          <div className="overflow-hidden rounded-2xl border border-line bg-white shadow-soft">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[760px] text-left">
                <thead><tr className="border-b border-line bg-[#fafbfc]"><th className="table-head pl-6">Customer</th><th className="table-head">Amount due</th><th className="table-head">Payment status</th><th className="table-head">Failure reason</th><th className="table-head pr-6 text-right">Action</th></tr></thead>
                <tbody>
                  {loading && customers.length === 0 && <LoadingRows columns={5} />}
                  {!loading && customers.length === 0 && <tr><td colSpan={5} className="px-6 py-12 text-center text-sm text-muted">{loadError ? 'Customer records could not be loaded.' : 'No customers found.'}</td></tr>}
                  {customers.map((customer) => <tr key={customer.id} className="border-b border-line last:border-0 hover:bg-slate-50/60">
                    <td className="py-4 pl-6"><div className="flex items-center gap-3"><Avatar name={customer.name} /><div><div className="text-sm font-semibold">{customer.name}</div><div className="mt-0.5 text-xs text-muted">Customer #{customer.id}</div></div></div></td>
                    <td className="py-4 text-sm font-bold tabular-nums text-ink">{formatMoney(customer.amountDue)}</td>
                    <td className="py-4"><StatusBadge value={customer.paymentStatus} /></td>
                    <td className="py-4">{customer.failureReason ? <StatusBadge value={customer.failureReason} /> : <span className="text-sm text-muted">—</span>}</td>
                    <td className="py-4 pr-6 text-right"><button onClick={() => void startCall(customer)} disabled={callingCustomer !== null} className="primary-button" aria-label={`Start recovery call for ${customer.name}`} aria-describedby={callingCustomer === customer.id ? 'call-progress' : undefined}>
                      {callingCustomer === customer.id ? <><span className="button-spinner" aria-hidden="true" /> Starting…</> : <><Icon name="phone" className="h-4 w-4" /> Start recovery call</>}
                    </button></td>
                  </tr>)}
                </tbody>
              </table>
            </div>
            {callingCustomer !== null && <p id="call-progress" className="sr-only" role="status" aria-live="polite">Starting recovery call…</p>}
            {callMessage && <div className={`flex items-start gap-2 border-t px-6 py-3 text-sm ${callMessage.type === 'success' ? 'border-emerald-100 bg-emerald-50 text-emerald-800' : 'border-rose-100 bg-rose-50 text-rose-800'}`} role="status" aria-live="polite"><span className="font-semibold">{callMessage.type === 'success' ? 'Call request submitted.' : 'Could not start call.'}</span><span>{callMessage.text}</span></div>}
          </div>
        </section>

        <div className="mt-9 grid grid-cols-1 gap-5 2xl:grid-cols-[1.35fr_.9fr]">
          <section id="calls" className="overflow-hidden rounded-2xl border border-line bg-white shadow-soft">
            <div className="flex items-center justify-between border-b border-line px-6 py-5"><div><h2 className="section-title">Recent calls</h2><p className="section-subtitle">Latest customer conversations</p></div><a href="#calls" className="text-xs font-semibold text-brand">View all</a></div>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[650px] text-left">
                <thead><tr className="border-b border-line bg-[#fafbfc]"><th className="table-head pl-6">Customer</th><th className="table-head">Status</th><th className="table-head">Outcome</th><th className="table-head">Duration</th><th className="table-head pr-6">Started</th></tr></thead>
                <tbody>
                  {loading && calls.length === 0 && <LoadingRows columns={5} />}
                  {!loading && calls.length === 0 && <EmptyRow columns={5} text="No calls have been recorded yet." />}
                  {calls.slice(0, 6).map((call) => <tr key={call.callId} className="border-b border-line last:border-0">
                    <td className="py-3.5 pl-6"><div className="text-sm font-semibold">{call.customerName}</div><div className="text-[11px] text-muted">Call #{call.callId}</div></td>
                    <td className="py-3.5"><StatusBadge value={call.status} /></td>
                    <td className="py-3.5">{call.outcome ? <StatusBadge value={call.outcome} /> : <span className="text-sm text-muted">—</span>}</td>
                    <td className="py-3.5 text-sm tabular-nums text-slate-600">{formatDuration(call.durationSeconds)}</td>
                    <td className="py-3.5 pr-6 text-xs text-muted">{formatDate(call.startedAt)}</td>
                  </tr>)}
                </tbody>
              </table>
            </div>
            {loadError && calls.length === 0 && <div className="px-6 pb-4 text-xs text-rose-600">Calls could not be loaded.</div>}
          </section>

          <section id="activity" className="overflow-hidden rounded-2xl border border-line bg-white shadow-soft">
            <div className="flex items-center justify-between border-b border-line px-6 py-5"><div><h2 className="section-title">Recovery activity</h2><p className="section-subtitle">Actions recorded by the system</p></div><span className="flex h-9 w-9 items-center justify-center rounded-xl bg-mint text-brand"><Icon name="activity" /></span></div>
            <div className="divide-y divide-line">
              {loading && actions.length === 0 && <div className="space-y-5 p-6"><div className="skeleton h-10" /><div className="skeleton h-10" /><div className="skeleton h-10" /></div>}
              {!loading && actions.length === 0 && <div className="px-6 py-12 text-center text-sm text-muted">No recovery actions recorded yet.</div>}
              {actions.slice(0, 7).map((action) => <ActivityItem key={action.actionId} action={action} />)}
            </div>
          </section>
        </div>

        <footer className="flex flex-wrap items-center justify-between gap-2 py-7 text-[11px] text-muted"><span>PayFlow Autopay Recovery · Demo workspace</span><span>Metrics reflect recorded backend data</span></footer>
      </main>
    </div>
  )
}

function MetricCard({ label, value, icon, detail, tone, loading: isLoading }: { label: string; value: string | number; icon: string; detail: string; tone: string; loading: boolean }) {
  return <article className={`metric-card rounded-2xl border border-line bg-white p-4 shadow-soft xl:p-5 ${label === 'Recovery rate' ? 'metric-card-featured' : ''}`}>
    <div className="flex items-start justify-between gap-3"><div className="text-xs font-semibold text-muted">{label}</div><span className={`metric-icon metric-${tone}`}><Icon name={icon} className="h-[18px] w-[18px]" /></span></div>
    {isLoading ? <div className="skeleton mt-4 h-8 w-24" aria-label="Loading metric" /> : <div className="metric-value mt-4 text-[27px] font-semibold tracking-tight tabular-nums">{value}</div>}
    <div className="mt-1 text-[11px] text-muted">{detail}</div>
  </article>
}

function Avatar({ name }: { name: string }) {
  const initials = name.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
  return <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#e8f1ec] text-xs font-bold text-brand">{initials}</div>
}

function LoadingRows({ columns }: { columns: number }) {
  return <tr><td colSpan={columns} className="px-6 py-6"><div className="space-y-4">{[0, 1, 2].map((item) => <div key={item} className="skeleton h-8" />)}</div></td></tr>
}

function EmptyRow({ columns, text }: { columns: number; text: string }) {
  return <tr><td colSpan={columns} className="px-6 py-12 text-center text-sm text-muted">{text}</td></tr>
}

function ActivityItem({ action }: { action: RecoveryAction }) {
  const outcomeColor = action.status === 'RECOVERED' ? 'bg-emerald-100 text-emerald-700' : action.status === 'ESCALATED' || action.actionType === 'SUPPORT_ESCALATION' ? 'bg-rose-100 text-rose-700' : action.status === 'CUSTOMER_REFUSED' ? 'bg-slate-100 text-slate-600' : 'bg-amber-100 text-amber-700'
  const title = action.actionType === 'SUPPORT_ESCALATION' ? 'Support escalation' : action.actionType === 'CALLBACK' ? 'Callback scheduled' : readable(action.status)
  return <article className="activity-item flex gap-3 px-6 py-4">
    <div className={`activity-icon mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full ${outcomeColor}`}><Icon name={action.actionType === 'SUPPORT_ESCALATION' ? 'alert' : action.actionType === 'CALLBACK' ? 'clock' : action.status === 'PAYMENT_LINK_SENT' ? 'link' : 'activity'} className="h-4 w-4" /></div>
    <div className="min-w-0 flex-1"><div className="flex flex-wrap items-center justify-between gap-2"><div className="text-sm font-semibold">{title}</div><time className="text-[11px] text-muted" dateTime={action.createdAt}>{formatDate(action.createdAt)}</time></div><div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-muted"><span>Customer #{action.customerId}</span><StatusBadge value={action.status} /></div>{action.callbackTime && <div className="mt-2 text-xs text-muted">Callback: {formatDate(action.callbackTime)}</div>}{action.details && <div className="mt-2 truncate text-xs text-muted">{action.details}</div>}</div>
  </article>
}

export default App
