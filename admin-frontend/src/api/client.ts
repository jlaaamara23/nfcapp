const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export type AuthUser = {
  accessToken: string;
  userId: number;
  username: string;
  email: string;
  roles: string[];
  employeeId?: number;
  fullName?: string;
};

function authHeader(): HeadersInit {
  const raw = localStorage.getItem('nfc_admin_auth');
  if (!raw) return {};
  const parsed = JSON.parse(raw) as AuthUser;
  return { Authorization: `Bearer ${parsed.accessToken}` };
}

export class AuthExpiredError extends Error {
  constructor(message = 'יש להתחבר') {
    super(message);
    this.name = 'AuthExpiredError';
  }
}

function clearStoredAuth() {
  localStorage.removeItem('nfc_admin_auth');
  window.dispatchEvent(new Event('nfc-auth-expired'));
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...authHeader(),
        ...(init.headers || {}),
      },
    });
  } catch {
    throw new Error('לא ניתן להתחבר לשרת');
  }
  const publicAuth = path.startsWith('/api/auth/login')
    || path.startsWith('/api/auth/bootstrap')
    || path.startsWith('/api/auth/setup-status');
  if (response.status === 401 && !publicAuth) {
    clearStoredAuth();
    throw new AuthExpiredError();
  }
  if (!response.ok) {
    let message = 'שגיאת שרת';
    try {
      const body = await response.json();
      message = body.message || message;
    } catch {
      /* ignore */
    }
    throw new Error(message);
  }
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
}

export const api = {
  setupStatus: () => request<{ needsBootstrap: boolean }>('/api/auth/setup-status'),
  bootstrap: (username: string, email: string, password: string) =>
    request<AuthUser>('/api/auth/bootstrap', {
      method: 'POST',
      body: JSON.stringify({ username, email, password }),
    }),
  login: (username: string, password: string) =>
    request<AuthUser>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
  me: () => request<Record<string, unknown>>('/api/auth/me'),
  logout: () => request<void>('/api/auth/logout', { method: 'POST' }),
  dashboard: () => request<DashboardStats>('/api/admin/reports/dashboard'),
  employees: (params = '') => request<Employee[]>(`/api/admin/employees${params}`),
  createEmployee: (body: unknown) =>
    request<Employee>('/api/admin/employees', { method: 'POST', body: JSON.stringify(body) }),
  updateEmployee: (id: number, body: unknown) =>
    request<Employee>(`/api/admin/employees/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  updateEmployeeStatus: (id: number, status: string) =>
    request<Employee>(`/api/admin/employees/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }),
  resetPassword: (id: number, newPassword: string) =>
    request<{ message: string }>(`/api/admin/employees/${id}/reset-password`, {
      method: 'POST',
      body: JSON.stringify({ newPassword }),
    }),
  revokeAccess: (id: number) =>
    request<{ message: string }>(`/api/admin/employees/${id}/revoke-access`, { method: 'POST' }),
  attendance: (params = '') => request<Attendance[]>(`/api/admin/attendance${params}`),
  correctAttendance: (id: number, body: unknown) =>
    request<Attendance>(`/api/admin/attendance/${id}/correct`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  stations: () => request<Station[]>('/api/admin/nfc-stations'),
  createStation: (body: unknown) =>
    request<Station>('/api/admin/nfc-stations', { method: 'POST', body: JSON.stringify(body) }),
  updateStationStatus: (id: number, body: unknown) =>
    request<Station>(`/api/admin/nfc-stations/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(body),
    }),
  schedules: (params = '') => request<Schedule[]>(`/api/admin/schedules${params}`),
  createSchedule: (body: unknown) =>
    request<Schedule>('/api/admin/schedules', { method: 'POST', body: JSON.stringify(body) }),
  deactivateSchedule: (id: number) =>
    request<{ message: string }>(`/api/admin/schedules/${id}/deactivate`, { method: 'PATCH' }),
  dailyReport: (date: string) => request<DailyRow[]>(`/api/admin/reports/daily?date=${date}`),
  monthlyReport: (month: string) => request<MonthlyRow[]>(`/api/admin/reports/monthly?month=${month}`),
  exportUrl: (format: string, from: string, to: string) =>
    `${API_BASE}/api/admin/reports/export?format=${format}&from=${from}&to=${to}`,
  departments: () => request<MetaItem[]>('/api/admin/departments'),
  workplaces: () => request<MetaItem[]>('/api/admin/workplaces'),
  admins: () => request<AdminUser[]>('/api/admin/users'),
  createAdmin: (body: unknown) =>
    request<AdminUser>('/api/admin/users', { method: 'POST', body: JSON.stringify(body) }),
  setAdminEnabled: (id: number, enabled: boolean) =>
    request<AdminUser>(`/api/admin/users/${id}/enabled`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    }),
  auditLogs: () => request<{ content: AuditRow[] }>('/api/admin/audit-logs?size=100'),
  authHeader,
  apiBase: API_BASE,
};

export type Employee = {
  id: number;
  employeeNumber: string;
  fullName: string;
  departmentId?: number;
  departmentName?: string;
  jobTitle?: string;
  email: string;
  phone?: string;
  employmentStatus: string;
  workplaceId?: number;
  workplaceName?: string;
  username?: string;
  hasLogin: boolean;
};

export type Attendance = {
  id: number;
  employeeNumber: string;
  employeeName: string;
  departmentName?: string;
  stationCode?: string;
  eventType: string;
  serverTimestamp: string;
  workedMinutes?: number;
  status: string;
  correctionReason?: string;
};

export type Station = {
  id: number;
  stationCode: string;
  name: string;
  workplaceId: number;
  workplaceName: string;
  registrationValue: string;
  nfcPayload: string;
  status: string;
};

export type Schedule = {
  id: number;
  employeeId: number;
  employeeName: string;
  workplaceName: string;
  name: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  graceMinutes: number;
  overnight: boolean;
  active: boolean;
};

export type DashboardStats = {
  activeEmployees: number;
  inactiveEmployees: number;
  clockedInNow: number;
  lateToday: number;
  missingClockInsToday: number;
  totalStationsActive: number;
  recentEvents: Attendance[];
};

export type DailyRow = {
  employeeNumber: string;
  employeeName: string;
  departmentName: string;
  firstClockIn: string;
  lastClockOut: string;
  workedMinutes: number;
  late: boolean;
  missingClockIn: boolean;
  earlyDeparture: boolean;
};

export type MonthlyRow = {
  employeeNumber: string;
  employeeName: string;
  departmentName: string;
  daysPresent: number;
  totalWorkedMinutes: number;
  lateCount: number;
  earlyDepartureCount: number;
};

export type MetaItem = { id: number; name: string; code?: string };
export type AdminUser = {
  id: number;
  username: string;
  email: string;
  enabled: boolean;
  locked: boolean;
  roles: string[];
};
export type AuditRow = {
  id: number;
  action: string;
  entityType: string;
  entityId?: string;
  details?: string;
  createdAt: string;
  actor?: { username?: string };
};
