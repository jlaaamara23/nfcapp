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

export function nfcSupported(): boolean {
  return typeof window !== 'undefined' && typeof window.NDEFReader === 'function'
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
  } catch {
    /* not a URL */
  }
  const compact = value.replace(/[^A-Za-z0-9-]/g, '')
  if (/^[A-Z0-9-]{3,}$/i.test(compact)) {
    return compact.toUpperCase()
  }
  return null
}

export function stationFromPageUrl(href = window.location.href): string | null {
  return extractStationCode(href)
}

export async function resolveStationCode(): Promise<string> {
  if (nfcSupported()) {
    return readStationFromNfc()
  }
  const fromUrl = stationFromPageUrl()
  if (fromUrl) {
    return fromUrl
  }
  throw new Error(
    'באייפון אי אפשר לקרוא תג מתוך האתר. כתבו על התג כתובת: https://nfcapp-nine.vercel.app/?station=GATE-01 ואז הצמידו את האייפון לתג ממסך הבית.',
  )
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
    throw new Error('NFC באתר עובד ב-Chrome באנדרואיד בלבד. פתחו את האתר ב-Chrome והפעילו NFC.')
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
