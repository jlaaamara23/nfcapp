import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, type Employee, type MetaItem } from '../api/client';

export function EmployeesPage() {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');

  async function load() {
    const params = new URLSearchParams();
    if (q) params.set('q', q);
    if (status) params.set('status', status);
    const qs = params.toString() ? `?${params}` : '';
    setEmployees(await api.employees(qs));
  }

  useEffect(() => {
    load().catch((e: Error) => setError(e.message));
  }, []);

  return (
    <section className="page">
      <header className="page-header row">
        <div>
          <h1>ניהול עובדים</h1>
          <p>הוספה, עריכה, הפעלה וביטול גישה</p>
        </div>
        <Link className="button" to="/employees/new">
          הוספת עובד
        </Link>
      </header>
      <div className="filters">
        <input placeholder="חיפוש שם / מספר / אימייל" value={q} onChange={(e) => setQ(e.target.value)} />
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">כל הסטטוסים</option>
          <option value="ACTIVE">פעיל</option>
          <option value="INACTIVE">לא פעיל</option>
          <option value="ARCHIVED">בארכיון</option>
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
              <th>מספר</th>
              <th>שם</th>
              <th>מחלקה</th>
              <th>תפקיד</th>
              <th>סטטוס</th>
              <th>התחברות</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {employees.map((e) => (
              <tr key={e.id}>
                <td>{e.employeeNumber}</td>
                <td>{e.fullName}</td>
                <td>{e.departmentName || '-'}</td>
                <td>{e.jobTitle || '-'}</td>
                <td>{e.employmentStatus}</td>
                <td>{e.username || 'אין'}</td>
                <td className="actions">
                  <Link to={`/employees/${e.id}`}>עריכה</Link>
                  <button
                    type="button"
                    onClick={() =>
                      void api
                        .updateEmployeeStatus(e.id, e.employmentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')
                        .then(load)
                    }
                  >
                    {e.employmentStatus === 'ACTIVE' ? 'השבתה' : 'הפעלה'}
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

export function EmployeeFormPage() {
  const id = location.pathname.split('/').pop();
  const isNew = id === 'new';
  const [departments, setDepartments] = useState<MetaItem[]>([]);
  const [workplaces, setWorkplaces] = useState<MetaItem[]>([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    employeeNumber: '',
    fullName: '',
    departmentId: '',
    jobTitle: '',
    email: '',
    phone: '',
    workplaceId: '',
    username: '',
    password: '',
    sendInvitation: false,
  });

  useEffect(() => {
    Promise.all([api.departments(), api.workplaces()])
      .then(([d, w]) => {
        setDepartments(d);
        setWorkplaces(w);
      })
      .catch((e: Error) => setError(e.message));
    if (!isNew && id) {
      api
        .employees()
        .then((list) => {
          const emp = list.find((e) => String(e.id) === id);
          if (!emp) return;
          setForm((f) => ({
            ...f,
            employeeNumber: emp.employeeNumber,
            fullName: emp.fullName,
            departmentId: emp.departmentId ? String(emp.departmentId) : '',
            jobTitle: emp.jobTitle || '',
            email: emp.email,
            phone: emp.phone || '',
            workplaceId: emp.workplaceId ? String(emp.workplaceId) : '',
            username: emp.username || '',
          }));
        })
        .catch((e: Error) => setError(e.message));
    }
  }, [id, isNew]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setMessage('');
    try {
      if (isNew) {
        await api.createEmployee({
          employeeNumber: form.employeeNumber,
          fullName: form.fullName,
          departmentId: form.departmentId ? Number(form.departmentId) : null,
          jobTitle: form.jobTitle,
          email: form.email,
          phone: form.phone,
          workplaceId: form.workplaceId ? Number(form.workplaceId) : null,
          username: form.username || null,
          password: form.password || null,
          sendInvitation: form.sendInvitation,
        });
        setMessage('העובד נוצר בהצלחה');
      } else {
        await api.updateEmployee(Number(id), {
          fullName: form.fullName,
          departmentId: form.departmentId ? Number(form.departmentId) : null,
          jobTitle: form.jobTitle,
          email: form.email,
          phone: form.phone,
          workplaceId: form.workplaceId ? Number(form.workplaceId) : null,
        });
        setMessage('הפרטים עודכנו');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'שגיאה');
    }
  }

  return (
    <section className="page">
      <header className="page-header">
        <h1>{isNew ? 'הוספת עובד' : 'עריכת עובד'}</h1>
      </header>
      <form className="form-grid panel" onSubmit={onSubmit}>
        <label>
          מספר עובד
          <input
            required
            disabled={!isNew}
            value={form.employeeNumber}
            onChange={(e) => setForm({ ...form, employeeNumber: e.target.value })}
          />
        </label>
        <label>
          שם מלא
          <input required value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        </label>
        <label>
          מחלקה
          <select value={form.departmentId} onChange={(e) => setForm({ ...form, departmentId: e.target.value })}>
            <option value="">ללא</option>
            {departments.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          תפקיד
          <input value={form.jobTitle} onChange={(e) => setForm({ ...form, jobTitle: e.target.value })} />
        </label>
        <label>
          אימייל
          <input required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </label>
        <label>
          טלפון
          <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
        </label>
        <label>
          מיקום עבודה
          <select value={form.workplaceId} onChange={(e) => setForm({ ...form, workplaceId: e.target.value })}>
            <option value="">ללא</option>
            {workplaces.map((w) => (
              <option key={w.id} value={w.id}>
                {w.name}
              </option>
            ))}
          </select>
        </label>
        {isNew && (
          <>
            <label>
              שם משתמש להתחברות
              <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
            </label>
            <label>
              סיסמה
              <input
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
              />
            </label>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={form.sendInvitation}
                onChange={(e) => setForm({ ...form, sendInvitation: e.target.checked })}
              />
              יצירת הזמנה מאובטחת (אם אין סיסמה)
            </label>
          </>
        )}
        {!isNew && (
          <div className="actions">
            <button
              type="button"
              onClick={() => {
                const pwd = prompt('סיסמה חדשה');
                if (pwd) void api.resetPassword(Number(id), pwd).then(() => setMessage('הסיסמה אופסה'));
              }}
            >
              איפוס סיסמה
            </button>
            <button type="button" onClick={() => void api.revokeAccess(Number(id)).then(() => setMessage('הגישה בוטלה'))}>
              ביטול גישה
            </button>
            <button type="button" onClick={() => void api.updateEmployeeStatus(Number(id), 'ARCHIVED').then(() => setMessage('הועבר לארכיון'))}>
              העברה לארכיון
            </button>
          </div>
        )}
        {error && <div className="error">{error}</div>}
        {message && <div className="success">{message}</div>}
        <button type="submit">שמירה</button>
      </form>
    </section>
  );
}
