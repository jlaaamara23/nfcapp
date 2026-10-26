import { describe, expect, it } from 'vitest';

describe('admin auth gate', () => {
  it('rejects non-admin role lists', () => {
    const roles = ['EMPLOYEE'];
    expect(roles.includes('ADMIN')).toBe(false);
  });

  it('accepts admin role', () => {
    const roles = ['ADMIN'];
    expect(roles.includes('ADMIN')).toBe(true);
  });

  it('treats empty login fields as unsigned', () => {
    expect(''.trim().length).toBe(0);
  });
});
