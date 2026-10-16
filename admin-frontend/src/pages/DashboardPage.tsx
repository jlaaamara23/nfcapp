import { useEffect, useState } from 'react';
import { api, type DashboardStats } from '../api/client';

export function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .dashboard()
      .then(setStats)
      .catch((e: Error) => setError(e.message));
  }, []);

  if (error) return <div className="error">{error}</div>;
  if (!stats) return <p>טוען לוח בקרה...</p>;

  const cards = [
    { label: 'עובדים פעילים', value: stats.activeEmployees },
    { label: 'במשמרת כעת', value: stats.clockedInNow },
    { label: 'איחורים היום', value: stats.lateToday },
    { label: 'חסרות כניסות', value: stats.missingClockInsToday },
    { label: 'תחנות פעילות', value: stats.totalStationsActive },
    { label: 'לא פעילים', value: stats.inactiveEmployees },
  ];

  return (
    <section className="page">
      <header className="page-header">
        <h1>לוח בקרה</h1>
        <p>סיכום נוכחות יומי וסטטיסטיקות בזמן אמת</p>
      </header>
      <div className="stat-grid">
        {cards.map((c) => (
          <article key={c.label} className="stat">
            <span>{c.label}</span>
            <strong>{c.value}</strong>
          </article>
        ))}
      </div>
      <div className="panel">
        <h2>אירועים אחרונים היום</h2>
        <table>
          <thead>
            <tr>
              <th>עובד</th>
              <th>סוג</th>
              <th>תחנה</th>
              <th>זמן שרת</th>
              <th>סטטוס</th>
            </tr>
          </thead>
          <tbody>
            {stats.recentEvents.map((e) => (
              <tr key={e.id}>
                <td>{e.employeeName}</td>
                <td>{e.eventType === 'CLOCK_IN' ? 'כניסה' : 'יציאה'}</td>
                <td>{e.stationCode || '-'}</td>
                <td>{new Date(e.serverTimestamp).toLocaleString('he-IL')}</td>
                <td>{e.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
