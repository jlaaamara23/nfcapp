import { useEffect, useState } from 'react';
import { api, type AuditRow } from '../api/client';

export function AuditPage() {
  const [rows, setRows] = useState<AuditRow[]>([]);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .auditLogs()
      .then((page) => setRows(page.content || []))
      .catch((e: Error) => setError(e.message));
  }, []);

  return (
    <section className="page">
      <header className="page-header">
        <h1>יומן ביקורת</h1>
        <p>פעולות מנהל רגישות, כניסות ותיקוני נוכחות</p>
      </header>
      {error && <div className="error">{error}</div>}
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>זמן</th>
              <th>משתמש</th>
              <th>פעולה</th>
              <th>ישות</th>
              <th>פרטים</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id}>
                <td>{new Date(r.createdAt).toLocaleString('he-IL')}</td>
                <td>{r.actor?.username || '-'}</td>
                <td>{r.action}</td>
                <td>
                  {r.entityType} {r.entityId || ''}
                </td>
                <td>{r.details || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
