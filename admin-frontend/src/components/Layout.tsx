import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const links = [
  { to: '/', label: 'לוח בקרה' },
  { to: '/employees', label: 'עובדים' },
  { to: '/attendance', label: 'נוכחות' },
  { to: '/stations', label: 'תחנות NFC' },
  { to: '/schedules', label: 'משמרות' },
  { to: '/reports', label: 'דוחות' },
  { to: '/admins', label: 'מנהלים' },
  { to: '/audit', label: 'יומן ביקורת' },
];

export function Layout() {
  const { user, logout } = useAuth();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">NFC</span>
          <div>
            <strong>נוכחות</strong>
            <p>לוח ניהול</p>
          </div>
        </div>
        <nav>
          {links.map((link) => (
            <NavLink key={link.to} to={link.to} end={link.to === '/'}>
              {link.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <span>{user?.username}</span>
          <button type="button" onClick={() => void logout()}>
            התנתקות
          </button>
        </div>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
