import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import { api, type AuthUser } from '../api/client';

type AuthContextValue = {
  user: AuthUser | null;
  login: (username: string, password: string) => Promise<void>;
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

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => loadUser());

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAdmin: !!user?.roles?.includes('ADMIN'),
      login: async (username, password) => {
        const auth = await api.login(username, password);
        if (!auth.roles.includes('ADMIN')) {
          throw new Error('גישה ללוח הניהול מותרת למנהלים בלבד');
        }
        localStorage.setItem('nfc_admin_auth', JSON.stringify(auth));
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
    [user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
