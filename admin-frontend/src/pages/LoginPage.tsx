import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export function LoginPage() {
  const { user, login, bootstrap, isAdmin } = useAuth();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [needsBootstrap, setNeedsBootstrap] = useState(false);

  useEffect(() => {
    api
      .setupStatus()
      .then((status) => setNeedsBootstrap(status.needsBootstrap))
      .catch(() => setNeedsBootstrap(false));
  }, []);

  if (user && isAdmin) {
    return <Navigate to="/" replace />;
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      if (needsBootstrap) {
        await bootstrap(username, email, password);
      } else {
        await login(username, password);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'התחברות נכשלה');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-page">
      <div className="login-backdrop" />
      <form className="login-panel" onSubmit={onSubmit} autoComplete="off">
        <p className="eyebrow">מערכת נוכחות NFC</p>
        <h1>{needsBootstrap ? 'יצירת מנהל ראשון' : 'כניסת מנהל'}</h1>
        <p className="muted">
          {needsBootstrap
            ? 'אין עדיין מנהל במערכת. צרו את חשבון המנהל הראשון.'
            : 'גישה ללוח הניהול מותרת למשתמשי ADMIN בלבד.'}
        </p>
        <label>
          שם משתמש
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="off"
            name="nfc-admin-username"
            required
          />
        </label>
        {needsBootstrap && (
          <label>
            אימייל
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="off"
              name="nfc-admin-email"
              required
            />
          </label>
        )}
        <label>
          סיסמה
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
            name="nfc-admin-password"
            required
          />
        </label>
        {error && <div className="error">{error}</div>}
        <button type="submit" disabled={loading}>
          {loading ? 'שומר...' : needsBootstrap ? 'יצירת מנהל' : 'התחברות'}
        </button>
      </form>
    </div>
  );
}
