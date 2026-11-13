import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createAppointment, getAppointmentTypes, getDoctors, getPatients } from '../api/client';
import { useAuth } from '../AuthContext';
import { useToast } from '../components/Toast';
import { ErrorNote, Loading, PageHeader } from '../components/ui';
import { drName, fmtMoney, todayISO } from '../utils';

/** One booking form for everyone: admins pick patient + doctor, doctors pick a patient, patients pick a doctor. */
export default function BookAppointment() {
  const { isDoctor, isPatient, user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const [types, setTypes] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    patientId: '', doctorId: '', appointmentType: 'CHECKUP', appointmentDate: '', appointmentTime: '', reasonForVisit: '',
  });
  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  useEffect(() => {
    Promise.all([
      getAppointmentTypes(), getDoctors(),
      isPatient ? Promise.resolve([]) : getPatients(),
    ]).then(([t, d, p]) => { setTypes(t); setDoctors(d); setPatients(p); })
      .catch((e) => setError(e.message)).finally(() => setLoading(false));
  }, [isPatient]);

  const selectedType = types.find((t) => t.value === form.appointmentType);
  const allowed = useMemo(() => selectedType?.allowedDoctorTypes || [], [selectedType]);

  // Radiology needs a radiologist, surgery needs a surgeon (or OB/GYN) - only show the right doctors.
  const eligibleDoctors = useMemo(
    () => doctors.filter((d) => allowed.length === 0 || allowed.includes(d.doctorType)),
    [doctors, allowed]);

  const groups = useMemo(() => {
    const g = {};
    types.forEach((t) => { (g[t.group] = g[t.group] || []).push(t); });
    return g;
  }, [types]);

  useEffect(() => {
    if (form.doctorId && !eligibleDoctors.some((d) => String(d.id) === String(form.doctorId))) set('doctorId', '');
  }, [eligibleDoctors]); // eslint-disable-line react-hooks/exhaustive-deps

  async function submit(e) {
    e.preventDefault();
    setError('');
    setSaving(true);
    try {
      await createAppointment({
        patientId: form.patientId ? Number(form.patientId) : null,
        doctorId: isDoctor ? null : Number(form.doctorId),
        appointmentType: form.appointmentType,
        appointmentDate: form.appointmentDate,
        appointmentTime: form.appointmentTime,
        reasonForVisit: form.reasonForVisit,
      });
      toast.success('Appointment booked.' + (isPatient ? ' The fee is in "My payments".' : ''));
      navigate(isPatient ? '/my-appointments' : '/appointments');
    } catch (err) { setError(err.message); } finally { setSaving(false); }
  }

  if (loading) return <Loading />;

  return (
    <div>
      <PageHeader title="Book an appointment" subtitle="Check-ups, scans, lab tests and surgeries." />
      <div className="panel form-card">
        <form onSubmit={submit}>
          <ErrorNote message={error} />

          {!isPatient && (
            <div className="field"><label>Patient</label>
              <select value={form.patientId} onChange={(e) => set('patientId', e.target.value)} required>
                <option value="">Select patient</option>
                {patients.map((p) => <option key={p.id} value={p.id}>{p.fullName}</option>)}
              </select>
              {isDoctor && <span className="hint">You can book for patients you already treat.</span>}
            </div>
          )}

          <div className="field"><label>Appointment type</label>
            <select value={form.appointmentType} onChange={(e) => set('appointmentType', e.target.value)}>
              {Object.keys(groups).map((g) => (
                <optgroup key={g} label={g}>{groups[g].map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}</optgroup>
              ))}
            </select>
          </div>

          {isDoctor ? (
            <div className="field"><label>Doctor</label><div className="strong">{user.fullName} (you)</div></div>
          ) : (
            <div className="field"><label>Doctor</label>
              <select value={form.doctorId} onChange={(e) => set('doctorId', e.target.value)} required>
                <option value="">{eligibleDoctors.length ? 'Select doctor' : 'No matching doctor available'}</option>
                {eligibleDoctors.map((d) => <option key={d.id} value={d.id}>{drName(d.user?.fullName)} — {d.doctorType.replace(/_/g, ' ').toLowerCase()}</option>)}
              </select>
              {allowed.length > 0 && <span className="hint">{selectedType.label} is performed by {allowed.map((a) => a.replace(/_/g, ' ').toLowerCase()).join(' or ')}s.</span>}
            </div>
          )}

          <div className="form-grid">
            <div className="field"><label>Date</label>
              <input type="date" min={todayISO()} value={form.appointmentDate} onChange={(e) => set('appointmentDate', e.target.value)} required /></div>
            <div className="field"><label>Time</label>
              <input type="time" value={form.appointmentTime} onChange={(e) => set('appointmentTime', e.target.value)} required /></div>
          </div>

          <div className="field"><label>Reason for visit</label>
            <textarea rows={3} value={form.reasonForVisit} onChange={(e) => set('reasonForVisit', e.target.value)} placeholder="Symptoms, what you'd like checked…" /></div>

          {form.appointmentType === 'SURGERY' && !isPatient && (
            <div className="alert alert-info">If the patient is currently admitted, this surgery is linked to their ward stay automatically.</div>
          )}

          {selectedType && (
            <div className="fee-box"><span>Appointment fee (added to the patient's bills)</span><b>{fmtMoney(selectedType.fee)}</b></div>
          )}

          <button className="btn" type="submit" disabled={saving}>{saving ? 'Booking…' : 'Book appointment'}</button>
        </form>
      </div>
    </div>
  );
}
