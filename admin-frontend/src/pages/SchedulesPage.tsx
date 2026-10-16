import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { api, type Employee, type MetaItem, type Schedule } from '../api/client';

export function SchedulesPage() {
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [workplaces, setWorkplaces] = useState<MetaItem[]>([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    employeeId: '',
    workplaceId: '',
    name: 'משמרת בוקר',
    dayOfWeek: '1',
    startTime: '08:00',
    endTime: '17:00',
    graceMinutes: '10',
    overnight: false,
  });

  async function load() {
    const [s, e, w] = await Promise.all([api.schedules(), api.employees(), api.workplaces()]);
    setSchedules(s);
    setEmployees(e);
    setWorkplaces(w);
    if (!form.employeeId && e[0]) setForm((f) => ({ ...f, employeeId: String(e[0].id) }));
    if (!form.workplaceId && w[0]) setForm((f) => ({ ...f, workplaceId: String(w[0].id) }));
  }

  useEffect(() => {
    load().catch((err: Error) => setError(err.message));
  }, []);

  async function onSubmit(ev: FormEvent) {
    ev.preventDefault();
    await api.createSchedule({
      employeeId: Number(form.employeeId),
      workplaceId: Number(form.workplaceId),
      name: form.name,
      dayOfWeek: Number(form.dayOfWeek),
      startTime: form.startTime,
      endTime: form.endTime,
      graceMinutes: Number(form.graceMinutes),
      overnight: form.overnight,
    });
    await load();
  }

  return (
    <section className="page">
      <header className="page-header">
        <h1>משמרות ולוחות זמנים</h1>
        <p>הגדרת משמרות, חלונות חסד ושעות לילה ניתנות להגדרה</p>
      </header>
      <form className="form-grid panel" onSubmit={onSubmit}>
        <label>
          עובד
          <select value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
            {employees.map((e) => (
              <option key={e.id} value={e.id}>
                {e.fullName}
              </option>
            ))}
          </select>
        </label>
        <label>
          מיקום
          <select value={form.workplaceId} onChange={(e) => setForm({ ...form, workplaceId: e.target.value })}>
            {workplaces.map((w) => (
              <option key={w.id} value={w.id}>
                {w.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          שם
          <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        </label>
        <label>
          יום בשבוע (1=ב׳)
          <input value={form.dayOfWeek} onChange={(e) => setForm({ ...form, dayOfWeek: e.target.value })} />
        </label>
        <label>
          התחלה
          <input type="time" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
        </label>
        <label>
          סיום
          <input type="time" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
        </label>
        <label>
          דקות חסד
          <input value={form.graceMinutes} onChange={(e) => setForm({ ...form, graceMinutes: e.target.value })} />
        </label>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.overnight}
            onChange={(e) => setForm({ ...form, overnight: e.target.checked })}
          />
          משמרת לילה
        </label>
        <button type="submit">הוספת משמרת</button>
      </form>
      {error && <div className="error">{error}</div>}
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>עובד</th>
              <th>שם</th>
              <th>יום</th>
              <th>שעות</th>
              <th>חסד</th>
              <th>לילה</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {schedules.map((s) => (
              <tr key={s.id}>
                <td>{s.employeeName}</td>
                <td>{s.name}</td>
                <td>{s.dayOfWeek}</td>
                <td>
                  {s.startTime} - {s.endTime}
                </td>
                <td>{s.graceMinutes}</td>
                <td>{s.overnight ? 'כן' : 'לא'}</td>
                <td>
                  {s.active && (
                    <button type="button" onClick={() => void api.deactivateSchedule(s.id).then(load)}>
                      ביטול
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
