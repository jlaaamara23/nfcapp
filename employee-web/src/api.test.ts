import { describe, expect, it } from 'vitest'
import { formatHours, isEmployee, type AuthUser } from './api'

describe('employee web helpers', () => {
  it('formats worked minutes', () => {
    expect(formatHours(0)).toBe('0:00')
    expect(formatHours(75)).toBe('1:15')
  })

  it('accepts employee accounts only', () => {
    const user: AuthUser = {
      accessToken: 't',
      userId: 1,
      username: 'dana',
      email: 'dana@example.com',
      roles: ['EMPLOYEE'],
      employeeId: 9,
    }
    expect(isEmployee(user)).toBe(true)
    expect(isEmployee({ ...user, roles: ['ADMIN'], employeeId: undefined })).toBe(false)
  })
})
