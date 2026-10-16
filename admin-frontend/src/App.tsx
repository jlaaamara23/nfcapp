import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { Layout } from './components/Layout';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { EmployeesPage, EmployeeFormPage } from './pages/EmployeesPage';
import { AttendancePage } from './pages/AttendancePage';
import { StationsPage } from './pages/StationsPage';
import { SchedulesPage } from './pages/SchedulesPage';
import { ReportsPage } from './pages/ReportsPage';
import { AdminsPage } from './pages/AdminsPage';
import { AuditPage } from './pages/AuditPage';

function Protected({ children }: { children: React.ReactNode }) {
  const { user, isAdmin } = useAuth();
  if (!user || !isAdmin) return <Navigate to="/login" replace />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <Protected>
            <Layout />
          </Protected>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="employees" element={<EmployeesPage />} />
        <Route path="employees/:id" element={<EmployeeFormPage />} />
        <Route path="attendance" element={<AttendancePage />} />
        <Route path="stations" element={<StationsPage />} />
        <Route path="schedules" element={<SchedulesPage />} />
        <Route path="reports" element={<ReportsPage />} />
        <Route path="admins" element={<AdminsPage />} />
        <Route path="audit" element={<AuditPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
