import { describe, expect, it } from 'vitest'
import { extractStationCode } from './nfc'

describe('extractStationCode', () => {
  it('reads GATE-01 from the tag URI', () => {
    expect(extractStationCode('nfcattendance://station/GATE-01?rv=NFC-ABC')).toBe('GATE-01')
  })

  it('ignores a URI prefix byte dump', () => {
    expect(extractStationCode('\u0000nfcattendance://station/GATE-01')).toBe('GATE-01')
  })

  it('returns null for empty input', () => {
    expect(extractStationCode('')).toBeNull()
  })

  it('reads station from the employee website URL', () => {
    expect(extractStationCode('https://nfcapp-nine.vercel.app/?station=GATE-01')).toBe('GATE-01')
  })
})
