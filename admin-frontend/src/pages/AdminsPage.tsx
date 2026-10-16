import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { api, type AdminUser } from '../api/client';

export function AdminsPage() {
  const [admins, setAdmins] = useState<AdminUser[]>([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ username: '', email: '', password: '' });

  async function load() {
    setAdmins(await api.admins());
  }

  useEffect(() => {
    load().catch((e: Error) => setError(e.message));
  }, []);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    await api.createAdmin(form);
    setForm({ username: '', email: '', password: '' });
    await load();
  }

  return (
    <section className="page">
      <header className="page-header">
        <h1>מנהלים והרשאות</h1>
        <p>ניהול חשבונות ADMIN בלבד</p>
      </header>
      <form className="filters panel" onSubmit={onSubmit}>
        <input
          required
          placeholder="שם משתמש"
          value={form.username}
          onChange={(e) => setForm({ ...form, username: e.target.value })}
        />
        <input
          required
          type="email"
          placeholder="אימייל"
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
        />
        <input
          required
          type="password"
          placeholder="סיסמה"
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
        />
        <button type="submit">הוספת מנהל</button>
      </form>
      {error && <div className="error">{error}</div>}
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>שם משתמש</th>
              <th>אימייל</th>
              <th>תפקידים</th>
              <th>פעיל</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {admins.map((a) => (
              <tr key={a.id}>
                <td>{a.username}</td>
                <td>{a.email}</td>
                <td>{a.roles.join(', ')}</td>
                <td>{a.enabled ? 'כן' : 'לא'}</td>
                <td>
                  <button type="button" onClick={() => void api.setAdminEnabled(a.id, !a.enabled).then(load)}>
                    {a.enabled ? 'השבתה' : 'הפעלה'}
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
