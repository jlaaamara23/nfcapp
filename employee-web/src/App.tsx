import { useEffect, useState, type FormEvent } from 'react'
import {
  api,
  clearAuth,
  formatHours,
  getStoredAuth,
  isEmployee,
  saveAuth,
  type AttendanceRow,
  type AuthUser,
  type TodaySummary,
} from './api'
import { nfcSupported, resolveStationCode } from './nfc'

type Screen = 'home' | 'history'

export default function App() {
  const [user, setUser] = useState<AuthUser | null>(() => getStoredAuth())
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')
  const [loading, setLoading] = useState(false)
  const [scanning, setScanning] = useState(false)
  const [screen, setScreen] = useState<Screen>('home')
  const [today, setToday] = useState<TodaySummary | null>(null)
  const [history, setHistory] = useState<AttendanceRow[]>([])

  async function refresh(current = user) {
    if (!current) return
    const summary = await api.today(current.accessToken)
    setToday(summary)
  }

  useEffect(() => {
    if (!user) return
    refresh(user).catch((err: Error) => {
      if (err.message.includes('התחבר') || err.message.includes('401')) {
        clearAuth()
        setUser(null)
      }
      setError(err.message)
    })
  }, [user])

  async function onLogin(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const auth = await api.login(username, password)
      if (!isEmployee(auth)) {
        throw new Error('התחברו עם חשבון עובד, לא מנהל')
      }
      saveAuth(auth)
      setUser(auth)
      setPassword('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'התחברות נכשלה')
    } finally {
      setLoading(false)
    }
  }

  async function onClock(mode: 'CLOCK_IN' | 'CLOCK_OUT') {
    if (!user) return
    setLoading(true)
    setScanning(true)
    setError('')
    setInfo(mode === 'CLOCK_IN' ? 'הצמידו את הטלפון לתג NFC...' : 'הצמידו את הטלפון לתג ליציאה...')
    try {
      const code = await resolveStationCode()
      const row = await api.clock(user.accessToken, mode, code)
      setInfo(mode === 'CLOCK_IN' ? `כניסה נרשמה · ${code}` : `יציאה נרשמה · ${code}`)
      await refresh(user)
      setHistory((prev) => [row, ...prev].slice(0, 30))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'הפעולה נכשלה')
      setInfo('')
    } finally {
      setLoading(false)
      setScanning(false)
    }
  }

  async function openHistory() {
    if (!user) return
    setScreen('history')
    setError('')
    try {
      setHistory(await api.history(user.accessToken))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'לא ניתן לטעון היסטוריה')
    }
  }

  if (!user) {
    return (
      <div className="login-wrap">
        <div className="brand">
          <h1>נוכחות NFC</h1>
          <p>אתר זמני לעובדים עד אישור האפליקציה</p>
        </div>
        <form className="card stack" onSubmit={onLogin} autoComplete="off">
          <h2>כניסת עובד</h2>
          <p className="muted">הזינו את הפרטים שקיבלתם מהמנהל</p>
          <label>
            שם משתמש
            <input value={username} onChange={(e) => setUsername(e.target.value)} required />
          </label>
          <label>
            סיסמה
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          {error && <div className="error">{error}</div>}
          <button className="primary" type="submit" disabled={loading}>
            {loading ? 'מתחבר...' : 'התחברות'}
          </button>
        </form>
      </div>
    )
  }

  return (
    <div className="app">
      <div className="topbar">
        <div>
          <strong>נוכחות NFC</strong>
          <div>{user.fullName || user.username}</div>
        </div>
        <button
          type="button"
          onClick={() => {
            clearAuth()
            setUser(null)
            setScreen('home')
          }}
        >
          התנתקות
        </button>
      </div>

      {screen === 'home' ? (
        <section className="card stack">
          <div className="status">
            <strong>{today?.statusLabel || 'טוען סטטוס...'}</strong>
            <span>שעות היום: {formatHours(today?.workedMinutesToday)}</span>
          </div>
          <p className="muted">
            {nfcSupported()
              ? 'לחצו כניסה או יציאה והצמידו את הטלפון לתג'
              : 'באייפון: הצמידו את הטלפון לתג ממסך הבית (התג חייב להיות קישור לאתר). באנדרואיד פתחו Chrome.'}
          </p>
          {error && <div className="error">{error}</div>}
          {info && <div className="ok">{info}</div>}
          <button className="clock-in" type="button" disabled={loading} onClick={() => void onClock('CLOCK_IN')}>
            {scanning && loading ? 'ממתין לתג...' : 'כניסה'}
          </button>
          <button className="clock-out" type="button" disabled={loading} onClick={() => void onClock('CLOCK_OUT')}>
            {scanning && loading ? 'ממתין לתג...' : 'יציאה'}
          </button>
          <button className="ghost" type="button" onClick={() => void openHistory()}>
            היסטוריה
          </button>
        </section>
      ) : (
        <section className="card stack">
          <h2>היסטוריה</h2>
          {error && <div className="error">{error}</div>}
          <div className="list">
            {history.length === 0 && <p className="muted">אין עדיין רישומים</p>}
            {history.map((row) => (
              <div className="item" key={row.id}>
                <b>{row.eventType === 'CLOCK_IN' ? 'כניסה' : 'יציאה'}</b>
                <span>
                  {row.stationCode || '-'} · {new Date(row.serverTimestamp).toLocaleString('he-IL')}
                </span>
              </div>
            ))}
          </div>
          <button className="ghost" type="button" onClick={() => setScreen('home')}>
            חזרה
          </button>
        </section>
      )}
    </div>
  )
}
