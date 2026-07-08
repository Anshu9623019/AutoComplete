import { useEffect, useRef } from 'react'
import SearchBar    from '../components/SearchBar'
import StatusBadge  from '../components/StatusBadge'
import StatsRow     from '../components/StatsRow'
import HistoryChips from '../components/HistoryChips'
import { useAutocomplete } from '../hooks/useAutocomplete'
import styles from './Home.module.css'
import TrendingBar from '../components/TrendingBar'

const TECH_TAGS = ['Java 21', 'Spring Boot', 'Redis', 'Kafka', 'PostgreSQL', 'Trie']

export default function Home() {
  const ac = useAutocomplete()

  const handleHistorySelect = (word) => {
    ac.setQuery(word)
  }

  return (
    <div className={styles.page}>
      {/* Ambient background */}
      <div className={styles.ambient}>
        <div className={styles.orb1} />
        <div className={styles.orb2} />
        <div className={styles.grid} />
      </div>

      {/* Header */}
      <header className={styles.header}>
        <div className={styles.logo}>
          <div className={styles.logoMark}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
            </svg>
          </div>
          <div>
            <div className={styles.logoName}>AutoComplete</div>
            <div className={styles.logoSub}>real-time trie search</div>
          </div>
        </div>
        <StatusBadge />
      </header>

      {/* Main */}
      <main className={styles.main}>
        {/* Hero */}
        <div className={styles.hero}>
          <div className={styles.eyebrow}>
            {TECH_TAGS.map(t => (
              <span key={t} className={styles.tag}>{t}</span>
            ))}
          </div>

          <h1 className={styles.title}>
            Search that learns<br />
            <span className={styles.accent}>as you type</span>
          </h1>
          <p className={styles.sub}>
            In-memory Trie · Redis ZSET ranking · Kafka event pipeline
          </p>
           <a href="/dashboard" className={styles.dashboardLink}>
                              View Analytics →
           </a>
        </div>


        {/* Search */}
        <div className={styles.searchSection}>
          <SearchBar
            query={ac.query}
            setQuery={ac.setQuery}
            clear={ac.clear}
            handleKeyDown={ac.handleKeyDown}
            isLoading={ac.isLoading}
            latency={ac.latency}
            isOpen={ac.isOpen}
            setIsOpen={ac.setIsOpen}
            suggestions={ac.suggestions}
            activeIndex={ac.activeIndex}
            select={ac.select}
            autoFocus={true}
          />

          <TrendingBar onSelect={(word) => ac.setQuery(word)} />
          <div className={styles.hints}>
            <span><kbd className={styles.kbd}>↑↓</kbd> navigate</span>
            <span><kbd className={styles.kbd}>↵</kbd> select</span>
            <span><kbd className={styles.kbd}>Esc</kbd> close</span>
          </div>
        </div>

        {/* Stats */}
        <StatsRow stats={ac.stats} query={ac.query} />

        {/* History */}
        <HistoryChips
          history={ac.history}
          onSelect={handleHistorySelect}
        />

        {/* Architecture note */}
        <div className={styles.flow}>
          {['Client', 'API', 'Redis', 'Trie', 'Kafka', 'Ranking'].map((step, i, arr) => (
            <span key={step} className={styles.flowItem}>
              <span className={styles.flowStep}>{step}</span>
              {i < arr.length - 1 && <span className={styles.flowArrow}>→</span>}
            </span>
          ))}
        </div>
      </main>

      {/* Footer */}
      <footer className={styles.footer}>
        <span>autocomplete-service · port 8081</span>
        <span>
          {ac.isOnline
            ? '✓ connected to backend'
            : '◎ demo mode — start your Spring Boot service'}
        </span>
      </footer>
    </div>
  )
}
