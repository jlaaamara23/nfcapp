import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import {
  extractStationCode,
  iphoneTagHelp,
  peekPendingClock,
  savePendingClock,
  tagUrlForStation,
  takePendingClock,
} from './nfc'

const memory: Record<string, string> = {}
const fakeStorage = {
  getItem: (key: string) => memory[key] ?? null,
  setItem: (key: string, value: string) => {
    memory[key] = value
  },
  removeItem: (key: string) => {
    delete memory[key]
  },
  clear: () => {
    for (const key of Object.keys(memory)) delete memory[key]
  },
  key: () => null,
  get length() {
    return Object.keys(memory).length
  },
} as Storage

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

  it('reads station from a path URL', () => {
    expect(extractStationCode('https://nfcapp-nine.vercel.app/station/GATE-01')).toBe('GATE-01')
  })

  it('reads one unified payload for iPhone and Galaxy', () => {
    const payload =
      'https://nfcapp-nine.vercel.app/?station=GATE-01#nfcattendance://station/GATE-01?rv=NFC-ABC'
    expect(extractStationCode(payload)).toBe('GATE-01')
  })
})

describe('iPhone tag URL helpers', () => {
  beforeEach(() => {
    fakeStorage.clear()
    Object.defineProperty(globalThis, 'sessionStorage', { value: fakeStorage, configurable: true })
  })
  afterEach(() => {
    fakeStorage.clear()
  })

  it('builds a URL iPhone can open from the tag', () => {
    expect(tagUrlForStation('gate-01')).toBe(
      'https://nfcapp-nine.vercel.app/?station=GATE-01#nfcattendance://station/GATE-01',
    )
  })

  it('explains that Safari cannot scan inside the page', () => {
    expect(iphoneTagHelp()).toContain('GATE-01')
    expect(iphoneTagHelp()).toContain('גלקסי')
  })

  it('stores and consumes a pending clock action after the tag reloads the page', () => {
    savePendingClock('CLOCK_OUT')
    expect(peekPendingClock()).toBe('CLOCK_OUT')
    expect(takePendingClock()).toBe('CLOCK_OUT')
    expect(takePendingClock()).toBeNull()
  })
})
