import { useEffect, useState } from 'react';
import { api, type Attendance } from '../api/client';

export function AttendancePage() {
  const [rows, setRows] = useState<Attendance[]>([]);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');

  async function load() {
    const params = new URLSearchParams();
    if (from) params.set('from', new Date(from).toISOString());
    if (to) params.set('to', new Date(to).toISOString());
    if (status) params.set('status', status);
    const qs = params.toString() ? `?${params}` : '';
    setRows(await api.attendance(qs));
  }

  useEffect(() => {
    load().catch((e: Error) => setError(e.message));
  }, []);

  async function correct(id: number) {
    const reason = prompt('סיבת תיקון (חובה)');
    if (!reason) return;
    const ts = prompt('חותמת זמן חדשה (ISO, למשל 2026-10-16T08:05:00Z)');
    if (!ts) return;
    await api.correctAttendance(id, { newTimestamp: ts, reason });
    await load();
  }

  return (
    <section className="page">
      <header className="page-header">
        <h1>רשומות נוכחות</h1>
        <p>סינון, תיקון עם סיבה מחייבת ומסלול ביקורת</p>
      </header>
      <div className="filters">
        <input type="datetime-local" value={from} onChange={(e) => setFrom(e.target.value)} />
        <input type="datetime-local" value={to} onChange={(e) => setTo(e.target.value)} />
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">כל הסטטוסים</option>
          <option value="VALID">תקין</option>
          <option value="LATE">איחור</option>
          <option value="EARLY_DEPARTURE">יציאה מוקדמת</option>
          <option value="CORRECTED">תוקן</option>
        </select>
        <button type="button" onClick={() => void load().catch((e: Error) => setError(e.message))}>
          סנן
        </button>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>עובד</th>
              <th>מחלקה</th>
              <th>סוג</th>
              <th>תחנה</th>
              <th>זמן שרת</th>
              <th>דקות</th>
              <th>סטטוס</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id}>
                <td>
                  {r.employeeName} ({r.employeeNumber})
                </td>
                <td>{r.departmentName || '-'}</td>
                <td>{r.eventType === 'CLOCK_IN' ? 'כניסה' : 'יציאה'}</td>
                <td>{r.stationCode || '-'}</td>
                <td>{new Date(r.serverTimestamp).toLocaleString('he-IL')}</td>
                <td>{r.workedMinutes ?? '-'}</td>
                <td>{r.status}</td>
                <td>
                  <button type="button" onClick={() => void correct(r.id)}>
                    תיקון
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
