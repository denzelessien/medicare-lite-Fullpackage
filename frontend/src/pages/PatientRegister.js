import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { registerPatient } from '../api/client';
import AuthLayout from '../components/AuthLayout';
import PasswordInput from '../components/PasswordInput';
import { ErrorNote } from '../components/ui';
import { todayISO } from '../utils';

const emptyForm = {
  fullName: '', email: '', password: '', dateOfBirth: '', gender: '', phoneNumber: '', address: '', bloodGroup: '',
};

export default function PatientRegister() {
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const body = { ...form, dateOfBirth: form.dateOfBirth || null };
      const res = await registerPatient(body);
      // Next step: confirm the email with the 6-digit code we just sent.
      navigate('/verify-email', { state: { email: form.email, devMode: res.devMode } });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout wide>
      <h2>Create your patient account</h2>
      <p className="muted mb">We'll email you a 6-digit code to confirm your address.</p>

      <form onSubmit={handleSubmit}>
        <div className="form-grid">
          <div className="field full"><label>Full name</label>
            <input value={form.fullName} onChange={(e) => set('fullName', e.target.value)} required />
          </div>
          <div className="field"><label>Email</label>
            <input type="email" autoComplete="email" value={form.email} onChange={(e) => set('email', e.target.value)} required />
          </div>
          <div className="field"><label>Password</label>
            <PasswordInput autoComplete="new-password" minLength={8} value={form.password} onChange={(e) => set('password', e.target.value)} />
            <span className="hint">At least 8 characters.</span>
          </div>
          <div className="field"><label>Date of birth</label>
            <input type="date" max={todayISO()} value={form.dateOfBirth} onChange={(e) => set('dateOfBirth', e.target.value)} />
          </div>
          <div className="field"><label>Gender</label>
            <select value={form.gender} onChange={(e) => set('gender', e.target.value)}>
              <option value="">Select</option><option>Male</option><option>Female</option><option>Other</option>
            </select>
          </div>
          <div className="field"><label>Phone number</label>
            <input value={form.phoneNumber} onChange={(e) => set('phoneNumber', e.target.value)} />
          </div>
          <div className="field"><label>Blood group</label>
            <select value={form.bloodGroup} onChange={(e) => set('bloodGroup', e.target.value)}>
              <option value="">Unknown</option>
              {['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map((b) => <option key={b}>{b}</option>)}
            </select>
          </div>
          <div className="field full"><label>Address</label>
            <input value={form.address} onChange={(e) => set('address', e.target.value)} />
          </div>
        </div>

        <ErrorNote message={error} />
        <button className="btn btn-block" type="submit" disabled={loading}>{loading ? 'Creating account…' : 'Create account & send code'}</button>
      </form>

      <div className="auth-switch">Already registered? <Link to="/patient-login">Sign in</Link></div>
    </AuthLayout>
  );
}
