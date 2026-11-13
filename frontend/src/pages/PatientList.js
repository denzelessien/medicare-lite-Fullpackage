import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { getPatients, createPatient, updatePatient, getAdmissions } from '../api/client';
import { useAuth } from '../AuthContext';
import { useToast } from '../components/Toast';
import Modal from '../components/Modal';
import Icon from '../components/Icons';
import { Avatar, Badge, EmptyState, ErrorNote, Loading, PageHeader, SearchBox } from '../components/ui';
import { ageFromDob, fmtDate, patientNumber, todayISO } from '../utils';

const emptyForm = { fullName: '', dateOfBirth: '', gender: '', phoneNumber: '', address: '', bloodGroup: '' };

export default function PatientList() {
  const { isAdmin } = useAuth();
  const toast = useToast();
  const [patients, setPatients] = useState([]);
  const [admittedIds, setAdmittedIds] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null); // null | 'new' | patient
  const [form, setForm] = useState(emptyForm);
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');
  const [formError, setFormError] = useState('');

  async function load() {
    setLoading(true);
    try {
      const [p, adm] = await Promise.all([getPatients(), getAdmissions('ADMITTED')]);
      setPatients(p);
      setAdmittedIds(new Set(adm.map((a) => a.patient.id)));
    } catch (err) { setError(err.message); } finally { setLoading(false); }
  }
  useEffect(() => { load(); }, []);

  function openNew() { setForm(emptyForm); setFormError(''); setEditing('new'); }
  function openEdit(p) {
    setForm({ fullName: p.fullName, dateOfBirth: p.dateOfBirth || '', gender: p.gender || '', phoneNumber: p.phoneNumber || '', address: p.address || '', bloodGroup: p.bloodGroup || '' });
    setFormError(''); setEditing(p);
  }

  async function save(e) {
    e.preventDefault();
    setFormError('');
    const body = { ...form, dateOfBirth: form.dateOfBirth || null };
    try {
      if (editing === 'new') await createPatient(body); else await updatePatient(editing.id, body);
      toast.success(editing === 'new' ? 'Patient registered.' : 'Patient updated.');
      setEditing(null);
      load();
    } catch (err) { setFormError(err.message); }
  }

  const shown = useMemo(() => patients.filter((p) =>
    `${p.fullName} ${p.phoneNumber || ''} ${patientNumber(p.id)}`.toLowerCase().includes(search.toLowerCase())), [patients, search]);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  return (
    <div>
      <PageHeader
        title={isAdmin ? 'Patients' : 'My patients'}
        subtitle={isAdmin ? 'Everyone registered with the hospital.' : 'Patients you have appointments, records or ward care with.'}
        actions={isAdmin && <button className="btn" onClick={openNew}><Icon name="plus" size={16} /> New patient</button>}
      />
      <ErrorNote message={error} />
      <div className="toolbar"><SearchBox value={search} onChange={setSearch} placeholder="Search by name, phone or ID…" /></div>

      <div className="panel flush">
        {loading ? <Loading /> : shown.length === 0 ? (
          <EmptyState icon="users" title="No patients found" text={search ? 'Try a different search.' : 'Registered patients will appear here.'} />
        ) : (
          <div className="table-wrap"><table>
            <thead><tr><th>Patient</th><th>Gender / Age</th><th>Phone</th><th>Blood</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {shown.map((p) => (
                <tr key={p.id}>
                  <td><div className="cell-person"><Avatar name={p.fullName} /><div><div className="strong">{p.fullName}</div><div className="sub">{patientNumber(p.id)}{p.user ? ' · has login' : ''}</div></div></div></td>
                  <td>{p.gender || '—'}{ageFromDob(p.dateOfBirth) != null ? ` · ${ageFromDob(p.dateOfBirth)} yrs` : ''}</td>
                  <td>{p.phoneNumber || '—'}</td>
                  <td>{p.bloodGroup ? <Badge tone="primary">{p.bloodGroup}</Badge> : '—'}</td>
                  <td>{admittedIds.has(p.id) ? <Badge tone="info">Admitted</Badge> : <span className="muted small">Registered {fmtDate(p.registeredAt)}</span>}</td>
                  <td className="right"><div className="actions" style={{ justifyContent: 'flex-end' }}>
                    <Link className="btn btn-ghost btn-sm" to={`/medical-records?patient=${p.id}`}>Records</Link>
                    {isAdmin && <button className="btn btn-ghost btn-sm" onClick={() => openEdit(p)}><Icon name="edit" size={14} /> Edit</button>}
                  </div></td>
                </tr>
              ))}
            </tbody>
          </table></div>
        )}
      </div>

      {editing && (
        <Modal title={editing === 'new' ? 'Register patient' : 'Edit patient'} onClose={() => setEditing(null)}
          footer={<><button className="btn btn-ghost" onClick={() => setEditing(null)}>Cancel</button><button className="btn" form="patient-form" type="submit">Save patient</button></>}>
          <form id="patient-form" onSubmit={save}>
            <ErrorNote message={formError} />
            <div className="form-grid">
              <div className="field full"><label>Full name</label><input value={form.fullName} onChange={(e) => set('fullName', e.target.value)} required /></div>
              <div className="field"><label>Date of birth</label><input type="date" max={todayISO()} value={form.dateOfBirth} onChange={(e) => set('dateOfBirth', e.target.value)} /></div>
              <div className="field"><label>Gender</label>
                <select value={form.gender} onChange={(e) => set('gender', e.target.value)}><option value="">Select</option><option>Male</option><option>Female</option><option>Other</option></select></div>
              <div className="field"><label>Phone number</label><input value={form.phoneNumber} onChange={(e) => set('phoneNumber', e.target.value)} /></div>
              <div className="field"><label>Blood group</label>
                <select value={form.bloodGroup} onChange={(e) => set('bloodGroup', e.target.value)}><option value="">Unknown</option>{['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map((b) => <option key={b}>{b}</option>)}</select></div>
              <div className="field full"><label>Address</label><input value={form.address} onChange={(e) => set('address', e.target.value)} /></div>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
