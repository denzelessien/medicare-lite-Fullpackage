import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../api/client';
import { useAuth } from '../AuthContext';
import AuthLayout from '../components/AuthLayout';
import PasswordInput from '../components/PasswordInput';
import { ErrorNote } from '../components/ui';

/** Shared sign-in form. `portal` only changes the wording and the "other portal" link. */
export function SignInForm({ portal }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { loginSuccess } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const result = await login(email, password);
      loginSuccess(result);
      navigate('/', { replace: true });
    } catch (err) {
      if (err.code === 'EMAIL_NOT_VERIFIED') {
        navigate('/verify-email', { state: { email } });
      } else {
        setError(err.message);
      }
    } finally {
      setLoading(false);
    }
  }

  const isStaff = portal === 'staff';
  return (
    <AuthLayout>
      <h2>{isStaff ? 'Staff sign in' : 'Patient sign in'}</h2>
      <p className="muted mb">{isStaff ? 'Admins and doctors sign in here.' : 'Welcome back. Sign in to see your appointments.'}</p>

      <form onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <PasswordInput id="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>

        <ErrorNote message={error} />

        <button className="btn btn-block" type="submit" disabled={loading}>{loading ? 'Signing in…' : 'Sign in'}</button>
      </form>

      {isStaff ? (
        <div className="auth-switch">Booking a visit for yourself? <Link to="/patient-login">Patient sign in</Link></div>
      ) : (
        <>
          <div className="auth-switch">New here? <Link to="/patient-register">Create a patient account</Link></div>
          <div className="auth-switch">Staff member? <Link to="/login">Staff sign in</Link></div>
        </>
      )}
    </AuthLayout>
  );
}

export default function Login() {
  return <SignInForm portal="staff" />;
}
