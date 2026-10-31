declare global {
  interface Window {
    NDEFReader?: new () => NdefReaderLike
  }
}

type NdefReaderLike = {
  scan: (options?: { signal?: AbortSignal }) => Promise<void>
  addEventListener: (type: 'reading' | 'readingerror', listener: (event: Event) => void) => void
  onreading: ((event: NdefReadingEvent) => void) | null
  onreadingerror: ((event: Event) => void) | null
}

type NdefReadingEvent = Event & {
  message?: { records?: Array<{ recordType?: string; data?: BufferSource; encoding?: string }> }
}

export type ClockMode = 'CLOCK_IN' | 'CLOCK_OUT'

const PENDING_KEY = 'nfc_pending_clock'
export const EMPLOYEE_SITE_URL = 'https://nfcapp-nine.vercel.app'

export function nfcSupported(): boolean {
  return typeof window !== 'undefined' && typeof window.NDEFReader === 'function'
}

export function isIosDevice(): boolean {
  if (typeof navigator === 'undefined') return false
  return /iPhone|iPad|iPod/i.test(navigator.userAgent)
}

export function extractStationCode(raw: string | null | undefined): string | null {
  if (!raw || !raw.trim()) return null
  let value = raw.trim()
  const idx = value.indexOf('nfcattendance://')
  if (idx >= 0) {
    value = value.slice(idx)
  }
  const match = value.match(/nfcattendance:\/\/station\/([^/?#]+)/i)
  if (match?.[1]) {
    return decodeURIComponent(match[1]).toUpperCase()
  }
  try {
    const url = new URL(value)
    const fromQuery = url.searchParams.get('station') || url.searchParams.get('st')
    if (fromQuery) {
      return fromQuery.trim().toUpperCase()
    }
    const pathMatch = url.pathname.match(/\/station\/([^/]+)/i)
    if (pathMatch?.[1]) {
      return decodeURIComponent(pathMatch[1]).toUpperCase()
    }
  } catch {
    /* not a URL */
  }
  const compact = value.replace(/[^A-Za-z0-9-]/g, '')
  if (/^[A-Z0-9-]{3,}$/i.test(compact)) {
    return compact.toUpperCase()
  }
  return null
}

export function stationFromPageUrl(href = typeof window === 'undefined' ? '' : window.location.href): string | null {
  return extractStationCode(href)
}

export function tagUrlForStation(code: string, registrationValue?: string): string {
  const station = code.trim().toUpperCase()
  const app = `nfcattendance://station/${station}${
    registrationValue ? `?rv=${encodeURIComponent(registrationValue)}` : ''
  }`
  return `${EMPLOYEE_SITE_URL}/?station=${encodeURIComponent(station)}#${app}`
}

export function iphoneTagHelp(): string {
  return `תג אחד לאייפון ולגלקסי — כתבו פעם אחת ככתובת URL, למשל ${tagUrlForStation('GATE-01')}. אל תכתבו שני פורמטים על אותו תג (הכתיבה השנייה מוחקת את הראשונה). באייפון לחצו כניסה והצמידו לתג, ואז לבאנר למעלה. הנוכחות נשמרת בשרת לפי חשבון העובד, לא לפי הטלפון.`
}

function storage(): Storage | null {
  try {
    return typeof sessionStorage === 'undefined' ? null : sessionStorage
  } catch {
    return null
  }
}

export function savePendingClock(mode: ClockMode) {
  storage()?.setItem(PENDING_KEY, JSON.stringify({ mode, at: Date.now() }))
}

export function peekPendingClock(): ClockMode | null {
  return readPending(false)
}

export function takePendingClock(): ClockMode | null {
  return readPending(true)
}

function readPending(remove: boolean): ClockMode | null {
  const store = storage()
  if (!store) return null
  const raw = store.getItem(PENDING_KEY)
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as { mode?: ClockMode; at?: number }
    if (parsed.at && Date.now() - parsed.at > 5 * 60 * 1000) {
      store.removeItem(PENDING_KEY)
      return null
    }
    if (parsed.mode === 'CLOCK_IN' || parsed.mode === 'CLOCK_OUT') {
      if (remove) store.removeItem(PENDING_KEY)
      return parsed.mode
    }
  } catch {
    store.removeItem(PENDING_KEY)
  }
  return null
}

export function clearPendingClock() {
  storage()?.removeItem(PENDING_KEY)
}

export async function resolveStationCode(): Promise<string> {
  const fromUrl = stationFromPageUrl()
  if (fromUrl) {
    return fromUrl
  }
  if (nfcSupported()) {
    return readStationFromNfc()
  }
  return waitForStationFromOsTag()
}

export async function waitForStationFromOsTag(timeoutMs = 30000): Promise<string> {
  if (typeof window === 'undefined') {
    throw new Error(iphoneTagHelp())
  }
  const immediate = stationFromPageUrl()
  if (immediate) return immediate

  return new Promise((resolve, reject) => {
    const finish = (code?: string, error?: Error) => {
      window.clearTimeout(timer)
      window.clearInterval(poll)
      window.removeEventListener('pageshow', onMaybe)
      window.removeEventListener('focus', onMaybe)
      window.removeEventListener('hashchange', onMaybe)
      window.removeEventListener('popstate', onMaybe)
      document.removeEventListener('visibilitychange', onMaybe)
      if (code) resolve(code)
      else reject(error ?? new Error(iphoneTagHelp()))
    }

    const onMaybe = () => {
      const code = stationFromPageUrl()
      if (code) finish(code)
    }

    const timer = window.setTimeout(() => finish(undefined, new Error(iphoneTagHelp())), timeoutMs)
    const poll = window.setInterval(onMaybe, 350)
    window.addEventListener('pageshow', onMaybe)
    window.addEventListener('focus', onMaybe)
    window.addEventListener('hashchange', onMaybe)
    window.addEventListener('popstate', onMaybe)
    document.addEventListener('visibilitychange', onMaybe)
  })
}

function decodeRecord(record: { recordType?: string; data?: BufferSource }): string {
  if (!record.data) return ''
  const bytes = new Uint8Array(record.data instanceof ArrayBuffer ? record.data : record.data.buffer)
  let start = 0
  if (bytes.length && bytes[0] < 32 && record.recordType !== 'text') {
    start = 1
  }
  if (record.recordType === 'text' && bytes.length) {
    const langLen = bytes[0] & 0x3f
    start = 1 + langLen
  }
  return new TextDecoder().decode(bytes.slice(start))
}

export async function readStationFromNfc(timeoutMs = 25000): Promise<string> {
  if (!nfcSupported() || !window.NDEFReader) {
    throw new Error('NFC באתר עובד ב-Chrome באנדרואיד. באייפון כתבו על התג קישור לאתר.')
  }
  const reader = new window.NDEFReader()
  const abort = new AbortController()
  await reader.scan({ signal: abort.signal })

  return new Promise((resolve, reject) => {
    const timer = window.setTimeout(() => {
      abort.abort()
      reject(new Error('לא נמצא תג. הצמידו את הטלפון לתג ונסו שוב.'))
    }, timeoutMs)

    const finish = (error?: Error, code?: string) => {
      window.clearTimeout(timer)
      abort.abort()
      if (error) reject(error)
      else if (code) resolve(code)
    }

    reader.onreading = (event) => {
      const records = event.message?.records ?? []
      for (const record of records) {
        const code = extractStationCode(decodeRecord(record))
        if (code) {
          finish(undefined, code)
          return
        }
      }
      finish(new Error('לא ניתן לקרוא מזהה תחנה מהתג'))
    }
    reader.onreadingerror = () => {
      finish(new Error('לא ניתן לקרוא את התג. נסו שוב.'))
    }
  })
}
