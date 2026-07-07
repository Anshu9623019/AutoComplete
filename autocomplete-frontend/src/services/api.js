import axios from 'axios'

// Local dev  → VITE_API_URL not set → Vite proxy → localhost:8081
// Production → VITE_API_URL=https://your-backend.com/api/v1
const BASE_URL = import.meta.env.VITE_API_URL || '/api/v1'

// ── Session ID (anonymous user identity) ─────────────────────────
function getSessionId() {
  let sessionId = localStorage.getItem('ac_session_id')
  if (!sessionId) {
    sessionId = crypto.randomUUID()
    localStorage.setItem('ac_session_id', sessionId)
  }
  return sessionId
}

// ── Single axios instance ─────────────────────────────────────────
const api = axios.create({
  baseURL: BASE_URL,
  timeout: 5000,
  headers: { 'Content-Type': 'application/json' }
})

// Attach X-Session-ID to every request automatically
api.interceptors.request.use(config => {
  config.headers['X-Session-ID'] = getSessionId()
  return config
})

// ── Autocomplete ──────────────────────────────────────────────────
export const getSuggestions = async (query, limit = 5) => {
  const { data } = await api.get('/suggest', {
    params: { q: query, limit }
  })
  return data // List<String> from your backend
}

// Rich suggestions with trending flag
export const getRichSuggestions = async (query, limit = 5) => {
  try {
    const { data } = await api.get('/suggest/rich', {
      params: { q: query, limit }
    })
    return data // [{ word: "kafka", trending: true }, ...]
  } catch {
    // Fallback to plain suggestions if /suggest/rich not available yet
    const words = await getSuggestions(query, limit)
    return words.map(w => ({ word: w, trending: false }))
  }
}

// In api.js
export const recordSelection = async (query, selected, latencyMs = 0) => {
  try {
    await api.post('/select', { query, selected, latencyMs })
  } catch {}
}

// ── Trending ──────────────────────────────────────────────────────
export const getTrending = async (limit = 8) => {
  try {
    const { data } = await api.get('/trending', { params: { limit } })
    return data // [{ word: "kafka", score: 42.5 }, ...]
  } catch {
    return []
  }
}

// ── User history (personalization) ───────────────────────────────
export const getUserHistory = async () => {
  try {
    const { data } = await api.get('/user/history')
    return data
  } catch {
    return []
  }
}

export const clearHistory = async () => {
  try {
    await api.delete('/user/history')
    localStorage.removeItem('ac_session_id') // clear locally too
  } catch {
    // best effort
  }
}

// ── Health check ──────────────────────────────────────────────────
export const checkHealth = async () => {
  try {
    const { data } = await axios.get('/actuator/health', { timeout: 3000 })
    return data?.status === 'UP'
  } catch {
    return false
  }
}

// Add to src/services/api.js

export const getAnalytics = async () => {
  try {
    const { data } = await api.get('/analytics')
    return data
  } catch {
    return null
  }
}

export const getTopTerms = async (limit = 10) => {
  try {
    const { data } = await api.get('/analytics/top-terms',
      { params: { limit } })
    return data  // [{ word: "java", count: 980 }, ...]
  } catch {
    return []
  }
}

export const getVolumeByHour = async (hours = 24) => {
  try {
    const { data } = await api.get('/analytics/volume',
      { params: { hours } })
    return data  // [{ hour: "14:00", count: 342 }, ...]
  } catch {
    return []
  }
}


export default api