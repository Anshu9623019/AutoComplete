import { useState, useEffect } from 'react'
import { getAnalytics } from '../services/api'
import {
  BarChart, Bar, LineChart, Line,
  XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, PieChart, Pie, Cell
} from 'recharts'
import styles from './Dashboard.module.css'

const COLORS = ['#4fffb0', '#4f9fff', '#a89ffa', '#ffd24f',
                 '#ff6b9d', '#ff9f4f', '#4fffff', '#ff4f4f']

export default function Dashboard() {
  const [data, setData]       = useState(null)
  const [loading, setLoading] = useState(true)
  const [lastRefresh, setLastRefresh] = useState(null)

  const load = async () => {
    setLoading(true)
    const snapshot = await getAnalytics()
    setData(snapshot)
    setLastRefresh(new Date())
    setLoading(false)
  }

  useEffect(() => {
    load()
    // Auto-refresh every 30 seconds
    const interval = setInterval(load, 30_000)
    return () => clearInterval(interval)
  }, [])

  if (loading && !data) return <LoadingScreen />

  const pieData = [
    { name: 'Trie Cache',  value: data?.trieCacheSize  || 0 },
    { name: 'LLM Cache',   value: data?.llmCacheSize   || 0 },
  ]

  return (
    <div className={styles.page}>
      {/* Header */}
      <header className={styles.header}>
        <div className={styles.headerLeft}>
          <a href="/" className={styles.backBtn}>← Search</a>
          <div className={styles.title}>Analytics Dashboard</div>
        </div>
        <div className={styles.headerRight}>
          {lastRefresh && (
            <span className={styles.refresh}>
              Updated {lastRefresh.toLocaleTimeString()}
            </span>
          )}
          <button className={styles.refreshBtn} onClick={load}>
            {loading ? 'Refreshing...' : '↻ Refresh'}
          </button>
        </div>
      </header>

      <main className={styles.main}>

        {/* ── Stat cards ───────────────────────────────── */}
        <div className={styles.statsGrid}>
          <StatCard
            label="Unique Terms"
            value={data?.totalUniqueTerms?.toLocaleString() || '0'}
            color="green"
            sub="in PostgreSQL"
          />
          <StatCard
            label="Trie Cache Keys"
            value={data?.trieCacheSize?.toLocaleString() || '0'}
            color="blue"
            sub="Redis ac: keys"
          />
          <StatCard
            label="LLM Cache Keys"
            value={data?.llmCacheSize?.toLocaleString() || '0'}
            color="purple"
            sub="Redis llm: keys"
          />
          <StatCard
            label="Cache Hit Rate"
            value={`${Math.round((data?.cacheHitRate || 0) * 100)}%`}
            color="yellow"
            sub="Trie vs LLM ratio"
          />
        </div>

        {/* ── Charts row ───────────────────────────────── */}
        <div className={styles.chartsGrid}>

          {/* Bar chart — top terms */}
          <div className={styles.chartCard}>
            <div className={styles.chartTitle}>Top 10 Searched Terms</div>
            <div className={styles.chartSub}>
              All-time from PostgreSQL
            </div>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart
                data={data?.topTerms || []}
                margin={{ top: 10, right: 10, left: -20, bottom: 60 }}
              >
                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="rgba(255,255,255,0.05)"
                />
                <XAxis
                  dataKey="word"
                  tick={{ fill: 'rgba(238,238,242,0.4)',
                          fontSize: 11, fontFamily: 'monospace' }}
                  angle={-40}
                  textAnchor="end"
                  interval={0}
                />
                <YAxis
                  tick={{ fill: 'rgba(238,238,242,0.4)',
                          fontSize: 11, fontFamily: 'monospace' }}
                />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="count" fill="#4fffb0"
                     radius={[4, 4, 0, 0]}/>
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Line chart — volume over 24h */}
          <div className={styles.chartCard}>
            <div className={styles.chartTitle}>Search Volume (Last 24h)</div>
            <div className={styles.chartSub}>
              Queries per hour from Kafka events
            </div>
            <ResponsiveContainer width="100%" height={280}>
              <LineChart
                data={data?.searchVolumeByHour || []}
                margin={{ top: 10, right: 10, left: -20, bottom: 20 }}
              >
                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="rgba(255,255,255,0.05)"
                />
                <XAxis
                  dataKey="hour"
                  tick={{ fill: 'rgba(238,238,242,0.4)',
                          fontSize: 10, fontFamily: 'monospace' }}
                  interval={3}
                />
                <YAxis
                  tick={{ fill: 'rgba(238,238,242,0.4)',
                          fontSize: 11, fontFamily: 'monospace' }}
                />
                <Tooltip content={<CustomTooltip />} />
                <Line
                  type="monotone"
                  dataKey="count"
                  stroke="#4f9fff"
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 4, fill: '#4f9fff' }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>

        </div>

        {/* ── Bottom row ───────────────────────────────── */}
        <div className={styles.bottomGrid}>

          {/* Trending now */}
          <div className={styles.chartCard}>
            <div className={styles.chartTitle}>Trending Right Now</div>
            <div className={styles.chartSub}>Current + previous hour</div>
            <div className={styles.trendingList}>
              {(data?.trending || []).map((t, i) => (
                <div key={t.word} className={styles.trendingItem}>
                  <span className={styles.trendingRank}>{i + 1}</span>
                  <span className={styles.trendingWord}>{t.word}</span>
                  <div className={styles.trendingBar}>
                    <div
                      className={styles.trendingFill}
                      style={{
                        width: `${Math.min(100,
                          (t.score / (data.trending[0]?.score || 1)) * 100)}%`
                      }}
                    />
                  </div>
                  <span className={styles.trendingScore}>
                    {Math.round(t.score)}
                  </span>
                </div>
              ))}
              {(!data?.trending || data.trending.length === 0) && (
                <div className={styles.empty}>
                  No trending data yet — start searching!
                </div>
              )}
            </div>
          </div>

          {/* Pie chart — cache distribution */}
          <div className={styles.chartCard}>
            <div className={styles.chartTitle}>Cache Distribution</div>
            <div className={styles.chartSub}>Trie vs LLM cache keys</div>
            <ResponsiveContainer width="100%" height={200}>
              <PieChart>
                <Pie
                  data={pieData}
                  cx="50%"
                  cy="50%"
                  innerRadius={55}
                  outerRadius={80}
                  paddingAngle={3}
                  dataKey="value"
                >
                  {pieData.map((entry, index) => (
                    <Cell key={entry.name}
                          fill={COLORS[index % COLORS.length]}/>
                  ))}
                </Pie>
                <Tooltip content={<CustomTooltip />} />
              </PieChart>
            </ResponsiveContainer>
            <div className={styles.pieLegend}>
              {pieData.map((entry, i) => (
                <div key={entry.name} className={styles.legendItem}>
                  <div className={styles.legendDot}
                       style={{ background: COLORS[i] }} />
                  <span>{entry.name}</span>
                  <span className={styles.legendVal}>
                    {entry.value.toLocaleString()}
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Top terms table */}
          <div className={styles.chartCard}>
            <div className={styles.chartTitle}>Top Terms Table</div>
            <div className={styles.chartSub}>Sorted by frequency</div>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Term</th>
                  <th>Count</th>
                </tr>
              </thead>
              <tbody>
                {(data?.topTerms || []).map((t, i) => (
                  <tr key={t.word}>
                    <td className={styles.tdNum}>{i + 1}</td>
                    <td className={styles.tdWord}>{t.word}</td>
                    <td className={styles.tdCount}>
                      {t.count.toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

        </div>
      </main>
    </div>
  )
}

// ── Sub-components ────────────────────────────────────────────────
function StatCard({ label, value, color, sub }) {
  return (
    <div className={`${styles.statCard} ${styles['stat_' + color]}`}>
      <div className={styles.statLabel}>{label}</div>
      <div className={styles.statValue}>{value}</div>
      <div className={styles.statSub}>{sub}</div>
    </div>
  )
}

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null
  return (
    <div className={styles.tooltip}>
      <div className={styles.tooltipLabel}>{label}</div>
      {payload.map(p => (
        <div key={p.name} className={styles.tooltipRow}>
          <span style={{ color: p.color }}>{p.name || 'count'}</span>
          <span>{p.value?.toLocaleString()}</span>
        </div>
      ))}
    </div>
  )
}

function LoadingScreen() {
  return (
    <div className={styles.loading}>
      <div className={styles.loadingDot} />
      <span>Loading analytics...</span>
    </div>
  )
}