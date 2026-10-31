const API_BASE = import.meta.env.VITE_API_BASE_URL || 'https://api.sreamline.tech'
const AUTH_KEY = 'nfc_employee_auth'
const STATION_KEY = 'nfc_employee_station'

export type AuthUser = {
  accessToken: string
  userId: number
  username: string
  email: string
  roles: string[]
  employeeId?: number
  fullName?: string
}

export type TodaySummary = {
  clockedIn: boolean
  lastClockIn?: string
  lastClockOut?: string
  workedMinutesToday?: number
  statusLabel?: string
}

export type AttendanceRow = {
  id: number
  eventType: string
  stationCode?: string
  serverTimestamp: string
  status: string
  workedMinutes?: number
}

function loadAuth(): AuthUser | null {
  const raw = localStorage.getItem(AUTH_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

export function getStoredAuth(): AuthUser | null {
  return loadAuth()
}

export function saveAuth(user: AuthUser) {
  localStorage.setItem(AUTH_KEY, JSON.stringify(user))
}

export function clearAuth() {
  localStorage.removeItem(AUTH_KEY)
}

export function getLastStation(): string {
  return localStorage.getItem(STATION_KEY) || ''
}

export function saveLastStation(code: string) {
  localStorage.setItem(STATION_KEY, code.trim())
}

async function request<T>(path: string, init: RequestInit = {}, token?: string): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(init.headers || {}),
      },
    })
  } catch {
    throw new Error('לא ניתן להתחבר לשרת')
  }
  if (!response.ok) {
    let message = 'שגיאת שרת'
    try {
      const body = (await response.json()) as { message?: string }
      message = body.message || message
    } catch {
      /* ignore */
    }
    throw new Error(message)
  }
  if (response.status === 204) return undefined as T
  const text = await response.text()
  return text ? (JSON.parse(text) as T) : (undefined as T)
}

export const api = {
  login: (username: string, password: string) =>
    request<AuthUser>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
  today: (token: string) => request<TodaySummary>('/api/attendance/me/today', {}, token),
  history: (token: string) => request<AttendanceRow[]>('/api/attendance/me', {}, token),
  clock: (token: string, mode: 'CLOCK_IN' | 'CLOCK_OUT', stationCode: string) => {
    const path = mode === 'CLOCK_IN' ? '/api/attendance/clock-in' : '/api/attendance/clock-out'
    return request<AttendanceRow>(
      path,
      {
        method: 'POST',
        body: JSON.stringify({
          stationCode: stationCode.trim(),
          clientRequestId: crypto.randomUUID(),
        }),
      },
      token,
    )
  },
}

export function isEmployee(user: AuthUser) {
  return user.roles?.includes('EMPLOYEE') && user.employeeId != null
}

export function formatHours(minutes?: number) {
  const value = minutes ?? 0
  return `${Math.floor(value / 60)}:${String(value % 60).padStart(2, '0')}`
}
