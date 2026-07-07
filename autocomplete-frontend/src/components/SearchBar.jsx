import { useRef, useEffect } from 'react'
import styles from './SearchBar.module.css'

export default function SearchBar({
  query, setQuery, clear, handleKeyDown,
  isLoading, latency, isOpen, setIsOpen,
  suggestions, activeIndex, select,
  autoFocus = false
}) {
  const inputRef = useRef(null)

  useEffect(() => {
    if (autoFocus) {
      setTimeout(() => inputRef.current?.focus(), 600)
    }
  }, [autoFocus])

  return (
    <div className={styles.wrap}>
      {/* Input row */}
      <div className={`${styles.box} ${isOpen ? styles.open : ''}`}>
        <span className={styles.icon}>
          {isLoading
            ? <span className={styles.spinner} />
            : <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8" />
              <path d="m21 21-4.35-4.35" />
            </svg>
          }
        </span>

        <input
          ref={inputRef}
          className={styles.input}
          type="text"
          value={query}
          onChange={e => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => suggestions.length > 0 && setIsOpen(true)}
          onBlur={() => setTimeout(() => setIsOpen(false), 150)}
          placeholder="Search anything..."
          autoComplete="off"
          spellCheck="false"
        />

        {latency !== null && (
          <span className={styles.latency}>{latency}ms</span>
        )}

        {query && (
          <button className={styles.clear} onClick={clear} tabIndex={-1}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" strokeWidth="2.5">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        )}
      </div>

      {/* Dropdown */}
      {isOpen && suggestions.length > 0 && (
        <div className={styles.dropdown}>
          {suggestions.map((item, i) => {
            // Support both plain string and rich object responses
            const word = typeof item === 'string' ? item : item.word
            const source = typeof item === 'object' ? item.source : null
            const isTrending = typeof item === 'object' && item.trending
            const isFuzzy = source === 'FUZZY'
            const isAI = source === 'LLM' || source === 'LLM_CACHE'
              || (typeof item === 'object' && item.aiGenerated)

            // Highlight the matching prefix in green
            const matchLen = Math.min(query.toLowerCase().length, word.length)
            const match = word.slice(0, matchLen)
            const rest = word.slice(matchLen)
            const isSemantic = source === 'SEMANTIC'

            return (
              <div
                key={word}
                className={`${styles.item} ${i === activeIndex ? styles.active : ''}`}
                onMouseDown={() => select(word)}
              >
                {/* Rank number */}
                <span className={styles.rank}>{i + 1}</span>

                {/* Word with highlighted prefix */}
                <span className={styles.word}>
                  <span className={styles.match}>{match}</span>
                  <span className={styles.rest}>{rest}</span>
                </span>

                {/* Badges — only show one at a time, priority: trending > fuzzy > AI */}
                

                {isTrending && !isFuzzy && !isAI && !isSemantic && (
                  <span className={styles.trendBadge}>↑ trending</span>
                )}
                {isFuzzy && (
                  <span className={styles.fuzzyBadge}>~ fuzzy</span>
                )}
                {isSemantic && (
                  <span className={styles.semanticBadge}>⊕ semantic</span>
                )}
                {isAI && !isFuzzy && !isSemantic && (
                  <span className={styles.aiBadge}>✦ AI</span>
                )}

                {/* Arrow */}
                <svg className={styles.arrow} width="14" height="14"
                  viewBox="0 0 24 24" fill="none" stroke="currentColor"
                  strokeWidth="2">
                  <path d="M5 12h14M12 5l7 7-7 7" />
                </svg>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}