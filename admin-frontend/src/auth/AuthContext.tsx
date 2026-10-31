import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, type AuthUser } from '../api/client';

type AuthContextValue = {
  user: AuthUser | null;
  ready: boolean;
  login: (username: string, password: string) => Promise<void>;
  bootstrap: (username: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  isAdmin: boolean;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function loadUser(): AuthUser | null {
  const raw = localStorage.getItem('nfc_admin_auth');
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

function persist(auth: AuthUser) {
  localStorage.setItem('nfc_admin_auth', JSON.stringify(auth));
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => loadUser());
  const [ready, setReady] = useState(!loadUser());

  useEffect(() => {
    const stored = loadUser();
    if (!stored) {
      setReady(true);
      return;
    }
    api
      .me()
      .then(() => {
        setUser(stored);
        setReady(true);
      })
      .catch(() => {
        localStorage.removeItem('nfc_admin_auth');
        setUser(null);
        setReady(true);
      });
  }, []);

  useEffect(() => {
    const onExpired = () => setUser(null);
    window.addEventListener('nfc-auth-expired', onExpired);
    return () => window.removeEventListener('nfc-auth-expired', onExpired);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      ready,
      isAdmin: !!user?.roles?.includes('ADMIN'),
      login: async (username, password) => {
        const auth = await api.login(username, password);
        if (!auth.roles.includes('ADMIN')) {
          throw new Error('גישה ללוח הניהול מותרת למנהלים בלבד');
        }
        persist(auth);
        setUser(auth);
      },
      bootstrap: async (username, email, password) => {
        const auth = await api.bootstrap(username, email, password);
        persist(auth);
        setUser(auth);
      },
      logout: async () => {
        try {
          await api.logout();
        } catch {
          /* ignore */
        }
        localStorage.removeItem('nfc_admin_auth');
        setUser(null);
      },
    }),
    [user, ready],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
