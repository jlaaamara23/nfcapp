import { useEffect, useState } from 'react';
import { api, type DailyRow, type MonthlyRow } from '../api/client';

export function ReportsPage() {
  const today = new Date().toISOString().slice(0, 10);
  const month = today.slice(0, 7);
  const [date, setDate] = useState(today);
  const [monthValue, setMonthValue] = useState(month);
  const [daily, setDaily] = useState<DailyRow[]>([]);
  const [monthly, setMonthly] = useState<MonthlyRow[]>([]);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([api.dailyReport(date), api.monthlyReport(monthValue)])
      .then(([d, m]) => {
        setDaily(d);
        setMonthly(m);
      })
      .catch((e: Error) => setError(e.message));
  }, [date, monthValue]);

  async function exportFile(format: string) {
    const token = JSON.parse(localStorage.getItem('nfc_admin_auth') || '{}').accessToken;
    const url = api.exportUrl(format, date, date);
    const res = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
    if (!res.ok) throw new Error('ייצוא נכשל');
    const blob = await res.blob();
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = format === 'xlsx' ? 'attendance.xlsx' : 'attendance.csv';
    a.click();
  }

  return (
    <section className="page">
      <header className="page-header row">
        <div>
          <h1>דוחות וייצוא</h1>
          <p>דוח יומי, חודשי וייצוא CSV / Excel</p>
        </div>
        <div className="actions">
          <button type="button" onClick={() => void exportFile('csv').catch((e: Error) => setError(e.message))}>
            ייצוא CSV
          </button>
          <button type="button" onClick={() => void exportFile('xlsx').catch((e: Error) => setError(e.message))}>
            ייצוא Excel
          </button>
        </div>
      </header>
      {error && <div className="error">{error}</div>}
      <div className="filters">
        <label>
          יום
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </label>
        <label>
          חודש
          <input type="month" value={monthValue} onChange={(e) => setMonthValue(e.target.value)} />
        </label>
      </div>
      <div className="panel">
        <h2>דוח יומי</h2>
        <table>
          <thead>
            <tr>
              <th>עובד</th>
              <th>כניסה</th>
              <th>יציאה</th>
              <th>דקות</th>
              <th>איחור</th>
              <th>חסר</th>
              <th>יציאה מוקדמת</th>
            </tr>
          </thead>
          <tbody>
            {daily.map((r) => (
              <tr key={r.employeeNumber}>
                <td>
                  {r.employeeName} ({r.employeeNumber})
                </td>
                <td>{r.firstClockIn || '-'}</td>
                <td>{r.lastClockOut || '-'}</td>
                <td>{r.workedMinutes}</td>
                <td>{r.late ? 'כן' : 'לא'}</td>
                <td>{r.missingClockIn ? 'כן' : 'לא'}</td>
                <td>{r.earlyDeparture ? 'כן' : 'לא'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="panel">
        <h2>דוח חודשי</h2>
        <table>
          <thead>
            <tr>
              <th>עובד</th>
              <th>ימי נוכחות</th>
              <th>סה״כ דקות</th>
              <th>איחורים</th>
              <th>יציאות מוקדמות</th>
            </tr>
          </thead>
          <tbody>
            {monthly.map((r) => (
              <tr key={r.employeeNumber}>
                <td>
                  {r.employeeName} ({r.employeeNumber})
                </td>
                <td>{r.daysPresent}</td>
                <td>{r.totalWorkedMinutes}</td>
                <td>{r.lateCount}</td>
                <td>{r.earlyDepartureCount}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
