import styles from './HistoryChips.module.css'

export default function HistoryChips({ history, onSelect }) {
  if (!history.length) return null

  // Inside HistoryChips component
  const handleClear = async () => {
    await clearHistory()
    // Also clear localStorage
    localStorage.removeItem('ac_history')
    localStorage.removeItem('ac_session_id')
    window.location.reload()  // fresh session
  }

  return (
    <div className={styles.wrap}>
      <span className={styles.label}>Recent</span>
      // In JSX next to label:
      <button onClick={handleClear} className={styles.clearBtn}>
        Clear
      </button>
      <div className={styles.chips}>
        {history.map(word => (
          <button
            key={word}
            className={styles.chip}
            onClick={() => onSelect(word)}
          >
            {word}
          </button>
        ))}
      </div>
    </div>
  )
}
