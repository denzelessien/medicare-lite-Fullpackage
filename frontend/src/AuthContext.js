import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('user')); } catch { return null; }
  });

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  }, []);

  // The API client fires this when the server says our token is no longer valid.
  useEffect(() => {
    window.addEventListener('auth:expired', logout);
    return () => window.removeEventListener('auth:expired', logout);
  }, [logout]);

  function loginSuccess(res) {
    const u = {
      fullName: res.fullName, role: res.role, email: res.email,
      doctorId: res.doctorId, patientId: res.patientId, doctorType: res.doctorType,
    };
    localStorage.setItem('token', res.token);
    localStorage.setItem('user', JSON.stringify(u));
    setUser(u);
  }

  const isAdmin = user?.role === 'ADMIN';
  const isDoctor = user?.role === 'DOCTOR';
  const isPatient = user?.role === 'PATIENT';
  const isStaff = isAdmin || isDoctor;

  return (
    <AuthContext.Provider value={{ user, loginSuccess, logout, isAdmin, isDoctor, isPatient, isStaff }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
