import React, { useState } from 'react';
import { BrowserRouter, Navigate, NavLink, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './AuthContext';
import { ThemeProvider, ThemeToggle } from './ThemeContext';
import { ToastProvider } from './components/Toast';
import Icon from './components/Icons';
import { Avatar } from './components/ui';

import Login from './pages/Login';
import PatientLogin from './pages/PatientLogin';
import PatientRegister from './pages/PatientRegister';
import VerifyEmail from './pages/VerifyEmail';
import Dashboard from './pages/Dashboard';
import PatientList from './pages/PatientList';
import StaffManagement from './pages/StaffManagement';
import AppointmentsList from './pages/AppointmentsList';
import BookAppointment from './pages/BookAppointment';
import Pharmacy from './pages/Pharmacy';
import Wards from './pages/Wards';
import MedicalRecords from './pages/MedicalRecords';
import Payments from './pages/Payments';
import PatientAppointments from './pages/PatientAppointments';
import PatientCareTeam from './pages/PatientCareTeam';
import PatientPayments from './pages/PatientPayments';

// What each role sees in the sidebar.
const NAV = {
  ADMIN: [
    { to: '/dashboard', label: 'Dashboard', icon: 'home' },
    { section: 'Clinical' },
    { to: '/patients', label: 'Patients', icon: 'users' },
    { to: '/appointments', label: 'Appointments', icon: 'calendar' },
    { to: '/wards', label: 'Wards', icon: 'bed' },
    { to: '/medical-records', label: 'Medical records', icon: 'filetext' },
    { section: 'Operations' },
    { to: '/pharmacy', label: 'Pharmacy', icon: 'pill' },
    { to: '/payments', label: 'Payments', icon: 'card' },
    { to: '/staff', label: 'Staff', icon: 'shield' },
  ],
  DOCTOR: [
    { to: '/dashboard', label: 'Dashboard', icon: 'home' },
    { to: '/appointments', label: 'Appointments', icon: 'calendar' },
    { to: '/wards', label: 'Wards & surgeries', icon: 'bed' },
    { to: '/patients', label: 'My patients', icon: 'users' },
    { to: '/medical-records', label: 'Medical records', icon: 'filetext' },
  ],
  PATIENT: [
    { to: '/my-appointments', label: 'My appointments', icon: 'calendar' },
    { to: '/book-visit', label: 'Book a visit', icon: 'plus' },
    { to: '/care-team', label: 'My care team', icon: 'user' },
    { to: '/my-payments', label: 'My payments', icon: 'card' },
  ],
};

const homeFor = (role) => (role === 'PATIENT' ? '/my-appointments' : '/dashboard');

function Shell() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const location = useLocation();
  React.useEffect(() => setOpen(false), [location.pathname]);

  return (
    <div className="app-shell">
      <div className={`sidebar-scrim ${open ? 'open' : ''}`} onClick={() => setOpen(false)} />
      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <div className="brand">
          <div className="brand-mark"><Icon name="heart" size={20} /></div>
          <div className="brand-name">MediCare Lite</div>
        </div>
        <div className="brand-sub">Hospital management</div>

        <nav>
          {NAV[user.role].map((n, i) => n.section
            ? <div className="nav-label" key={i}>{n.section}</div>
            : <NavLink key={n.to} to={n.to}><Icon name={n.icon} size={18} />{n.label}</NavLink>)}
        </nav>

        <div className="sidebar-footer">
          <div className="sidebar-user">
            <Avatar name={user.fullName} size={34} />
            <div className="who"><div>{user.fullName}</div><div className="role-tag">{user.role === 'DOCTOR' ? 'Doctor' : user.role.toLowerCase()}</div></div>
          </div>
          <ThemeToggle />
          <button className="theme-toggle" onClick={logout} style={{ borderColor: 'rgba(255,255,255,0.18)', color: 'var(--sidebar-ink-dim)', width: '100%' }}>
            <Icon name="logout" size={15} /> Sign out
          </button>
        </div>
      </aside>

      <div className="content">
        <div className="mobile-bar">
          <button className="icon-btn" onClick={() => setOpen(true)} aria-label="Open menu"><Icon name="menu" /></button>
          <strong style={{ fontFamily: 'Fraunces, serif' }}>MediCare Lite</strong>
        </div>
        <main><Outlet /></main>
      </div>
    </div>
  );
}

/** Wraps routes: must be signed in, and (optionally) have one of the allowed roles. */
function Protected({ roles }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to={homeFor(user.role)} replace />;
  return <Outlet />;
}

function HomeRedirect() {
  const { user } = useAuth();
  return <Navigate to={user ? homeFor(user.role) : '/login'} replace />;
}

/** Signed-in users who open a sign-in page are sent to their home. */
function PublicOnly({ children }) {
  const { user } = useAuth();
  return user ? <Navigate to={homeFor(user.role)} replace /> : children;
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <ToastProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<PublicOnly><Login /></PublicOnly>} />
              <Route path="/patient-login" element={<PublicOnly><PatientLogin /></PublicOnly>} />
              <Route path="/patient-register" element={<PublicOnly><PatientRegister /></PublicOnly>} />
              <Route path="/verify-email" element={<PublicOnly><VerifyEmail /></PublicOnly>} />

              <Route element={<Protected />}>
                <Route element={<Shell />}>
                  {/* Admin + Doctor */}
                  <Route element={<Protected roles={['ADMIN', 'DOCTOR']} />}>
                    <Route path="/dashboard" element={<Dashboard />} />
                    <Route path="/patients" element={<PatientList />} />
                    <Route path="/appointments" element={<AppointmentsList />} />
                    <Route path="/appointments/new" element={<BookAppointment />} />
                    <Route path="/wards" element={<Wards />} />
                    <Route path="/medical-records" element={<MedicalRecords />} />
                  </Route>
                  {/* Admin only */}
                  <Route element={<Protected roles={['ADMIN']} />}>
                    <Route path="/pharmacy" element={<Pharmacy />} />
                    <Route path="/payments" element={<Payments />} />
                    <Route path="/staff" element={<StaffManagement />} />
                  </Route>
                  {/* Patient */}
                  <Route element={<Protected roles={['PATIENT']} />}>
                    <Route path="/my-appointments" element={<PatientAppointments />} />
                    <Route path="/book-visit" element={<BookAppointment />} />
                    <Route path="/care-team" element={<PatientCareTeam />} />
                    <Route path="/my-payments" element={<PatientPayments />} />
                  </Route>
                </Route>
              </Route>

              <Route path="*" element={<HomeRedirect />} />
            </Routes>
          </BrowserRouter>
        </ToastProvider>
      </AuthProvider>
    </ThemeProvider>
  );
}
