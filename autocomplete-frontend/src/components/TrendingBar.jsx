// src/components/TrendingBar.jsx
import { useState, useEffect } from 'react'
import { getTrending } from '../services/api'
import styles from './TrendingBar.module.css'

export default function TrendingBar({ onSelect }) {
  const [trending, setTrending] = useState([])

  useEffect(() => {
    const load = async () => setTrending(await getTrending(8))
    load()
    // Refresh every 60 seconds
    const interval = setInterval(load, 60_000)
    return () => clearInterval(interval)
  }, [])

  if (!trending.length) return null

  return (
    <div className={styles.wrap}>
      <span className={styles.label}>
        <span className={styles.dot} />
        Trending now
      </span>
      <div className={styles.chips}>
        {trending.map(({ word, score }) => (
          <button
            key={word}
            className={styles.chip}
            onClick={() => onSelect(word)}
          >
            {word}
            <span className={styles.score}>
              {Math.round(score)}
            </span>
          </button>
        ))}
      </div>
    </div>
  )
}