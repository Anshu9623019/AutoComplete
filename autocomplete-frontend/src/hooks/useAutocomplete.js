import { useState, useEffect, useRef, useCallback } from 'react'
import { useDebounce } from './useDebounce'
import { getSuggestions, recordSelection } from '../services/api'

// Demo data shown when backend is offline
const DEMO = {
  j:    ['java', 'javascript', 'junit', 'json', 'jvm'],
  ja:   ['java', 'javascript', 'jakarta'],
  jav:  ['java', 'javascript', 'javafx'],
  java: ['java', 'javascript'],
  s:    ['spring', 'sql', 'system', 'stream', 'sort'],
  sp:   ['spring', 'spark'],
  k:    ['kafka', 'kotlin', 'kubernetes', 'kinesis'],
  ka:   ['kafka', 'kafka-streams'],
  r:    ['redis', 'rest', 'reactive', 'rabbitmq'],
  re:   ['redis', 'rest', 'reactive'],
  g:    ['grpc', 'gradle', 'git', 'graphql'],
  d:    ['docker', 'distributed', 'dynamo'],
  p:    ['postgresql', 'python', 'prometheus'],
}

function getDemoResults(prefix) {
  const key = Object.keys(DEMO)
    .filter(k => prefix.startsWith(k))
    .sort((a, b) => b.length - a.length)[0]
  if (!key) return []
  return DEMO[key].filter(w => w.startsWith(prefix))
}

export function useAutocomplete() {
  const [query, setQuery]           = useState('')
  const [suggestions, setSuggestions] = useState([])
  const [isOpen, setIsOpen]         = useState(false)
  const [isLoading, setIsLoading]   = useState(false)
  const [activeIndex, setActiveIndex] = useState(-1)
  const [latency, setLatency]       = useState(null)
  const [isOnline, setIsOnline]     = useState(false)

  // Stats
  const [stats, setStats] = useState({
    totalSearches: 0,
    totalLatency: 0,
    cacheHits: 0,
  })

  // History from localStorage
  const [history, setHistory] = useState(() => {
    try { return JSON.parse(localStorage.getItem('ac_history') || '[]') }
    catch { return [] }
  })

  const abortRef = useRef(null)
  const debouncedQuery = useDebounce(query, 150)

  // Fetch from backend
  useEffect(() => {
    if (!debouncedQuery.trim()) {
      setSuggestions([])
      setIsOpen(false)
      setLatency(null)
      return
    }

    const fetch = async () => {
      if (abortRef.current) abortRef.current.abort()
      abortRef.current = new AbortController()

      setIsLoading(true)
      const start = performance.now()

      try {
        const data = await getSuggestions(debouncedQuery.toLowerCase().trim())
        const elapsed = Math.round(performance.now() - start)
        setLatency(elapsed)
        setSuggestions(data)
        setIsOpen(data.length > 0)
        setIsOnline(true)
        setStats(prev => ({
          totalSearches: prev.totalSearches + 1,
          totalLatency:  prev.totalLatency + elapsed,
          cacheHits:     prev.cacheHits + 1,
        }))
      } catch (err) {
        if (err.name === 'AbortError' || err.name === 'CanceledError') return
        // Fallback to demo
        const elapsed = Math.round(performance.now() - start)
        const demo = getDemoResults(debouncedQuery.toLowerCase().trim())
        setLatency(elapsed)
        setSuggestions(demo)
        setIsOpen(demo.length > 0)
        setIsOnline(false)
        setStats(prev => ({
          totalSearches: prev.totalSearches + 1,
          totalLatency:  prev.totalLatency + elapsed,
          cacheHits:     prev.cacheHits,
        }))
      } finally {
        setIsLoading(false)
        setActiveIndex(-1)
      }
    }

    fetch()
  }, [debouncedQuery])

  const select = useCallback((word) => {
    setQuery(word)
    setIsOpen(false)
    setActiveIndex(-1)
    recordSelection(query, word)


  const select = useCallback((word) => {
      setQuery(word)
      setIsOpen(false)
      recordSelection(query, word, latency)  // ← pass real latency
      addToHistory(word)
    }, [query, latency])  

    // Update history
    setHistory(prev => {
      const next = [word, ...prev.filter(w => w !== word)].slice(0, 8)
      try { localStorage.setItem('ac_history', JSON.stringify(next)) } catch {}
      return next
    })
  }, [query])

  const clear = useCallback(() => {
    setQuery('')
    setSuggestions([])
    setIsOpen(false)
    setLatency(null)
    setActiveIndex(-1)
  }, [])

  const handleKeyDown = useCallback((e) => {
    if (!isOpen) return
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setActiveIndex(i => Math.min(i + 1, suggestions.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActiveIndex(i => Math.max(i - 1, -1))
    } else if (e.key === 'Enter' && activeIndex >= 0) {
      e.preventDefault()
      select(suggestions[activeIndex])
    } else if (e.key === 'Escape') {
      setIsOpen(false)
      setActiveIndex(-1)
    }
  }, [isOpen, suggestions, activeIndex, select])

  const avgLatency = stats.totalSearches > 0
    ? Math.round(stats.totalLatency / stats.totalSearches)
    : null




  return {
    query, setQuery,
    suggestions,
    isOpen, setIsOpen,
    isLoading,
    activeIndex,
    latency,
    isOnline,
    stats: { ...stats, avgLatency },
    history,
    select,
    clear,
    handleKeyDown,
  }
}
