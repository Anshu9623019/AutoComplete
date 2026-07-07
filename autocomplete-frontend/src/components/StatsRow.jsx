import styles from './StatsRow.module.css'

export default function StatsRow({ stats, query }) {
  const cards = [
    { label: 'Searches',    value: stats.totalSearches || 0,          color: 'blue'   },
    { label: 'Avg Latency', value: stats.avgLatency ? `${stats.avgLatency}ms` : '—', color: 'green' },
    { label: 'Cache Hits',  value: stats.cacheHits || 0,              color: 'pink'   },
    { label: 'Last Query',  value: query || '—',                      color: 'yellow', small: true },
  ]

  return (
    <div className={styles.row}>
      {cards.map(({ label, value, color, small }) => (
        <div key={label} className={styles.card}>
          <span className={styles.label}>{label}</span>
          <span className={`${styles.value} ${styles[color]} ${small ? styles.small : ''}`}>
            {typeof value === 'number' ? value.toLocaleString() : value}
          </span>
        </div>
      ))}
    </div>
  )
}
