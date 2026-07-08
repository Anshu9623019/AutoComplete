import { useState, useEffect } from 'react'
import { checkHealth } from '../services/api'
import styles from './StatusBadge.module.css'

// IMPORTANT: Make sure { source } is explicitly written here inside the brackets!
export default function StatusBadge({ source }) {
  const [online, setOnline] = useState(null)

  // This is line 8 where it was crashing before:
  const isSemantic = source === 'SEMANTIC'

  useEffect(() => {
    const check = async () => setOnline(await checkHealth())
    check()
    const interval = setInterval(check, 30000)
    return () => clearInterval(interval)
  }, [])

  return (
    <div className={`${styles.badge} ${online === null ? styles.checking : online ? styles.online : styles.offline}`}>
      {isSemantic && (
        <span className={styles.semanticBadge}>⊕ semantic</span>
      )}
      <span className={styles.dot} />
      <span className={styles.text}>
        {online === null ? 'checking...' : online ? 'API online' : 'demo mode'}
      </span>
    </div>
  )
}