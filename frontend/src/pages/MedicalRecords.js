import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { getRecordPatients, getPatientSummary, createMedicalRecord, getDoctors, getAppointments } from '../api/client';
import { useAuth } from '../AuthContext';
import { useToast } from '../components/Toast';
import Modal from '../components/Modal';
import Icon from '../components/Icons';
import { Avatar, Badge, EmptyState, ErrorNote, Loading, PageHeader, SearchBox, StatusBadge, Tabs } from '../components/ui';
import { drName, ageFromDob, APPOINTMENT_TYPE_LABELS, daysBetween, fmtDate, fmtDateTime, patientNumber, todayISO, WARD_TYPE_LABELS } from '../utils';

export default function MedicalRecords() {
  const { isAdmin } = useAuth();
  const toast = useToast();
  const [params, setParams] = useSearchParams();
  const [patients, setPatients] = useState([]);
  const [search, setSearch] = useState('');
  const [selectedId, setSelectedId] = useState(params.get('patient') ? Number(params.get('patient')) : null);
  const [summary, setSummary] = useState(null);
  const [tab, setTab] = useState('records');
  const [loadingList, setLoadingList] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [error, setError] = useState('');
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    getRecordPatients().then(setPatients).catch((e) => setError(e.message)).finally(() => setLoadingList(false));
  }, []);

  const loadSummary = useCallback(async (id) => {
    setLoadingDetail(true);
    try { setSummary(await getPatientSummary(id)); setError(''); }
    catch (err) { setError(err.message); setSummary(null); }
    finally { setLoadingDetail(false); }
  }, []);

  useEffect(() => { if (selectedId) loadSummary(selectedId); }, [selectedId, loadSummary]);

  // Opened from an appointment's "Record" button: jump straight to the new-record form.
  useEffect(() => {
    if (summary && params.get('appointment')) setAdding(true);
  }, [summary]); // eslint-disable-line react-hooks/exhaustive-deps

  const shown = useMemo(() => patients.filter((p) =>
    `${p.fullName} ${patientNumber(p.id)} ${p.phoneNumber || ''}`.toLowerCase().includes(search.toLowerCase())), [patients, search]);

  function select(id) {
    setSelectedId(id); setTab('records');
    setParams({ patient: id });
  }

  const p = summary?.patient;

  return (
    <div>
      <PageHeader title="Medical records" subtitle={isAdmin ? 'Records, treating doctors and ward stays for every registered patient.' : 'Records for patients you are treating.'} />
      <ErrorNote message={error} />

      <div className="records-layout">
        <div className="panel flush">
          <div style={{ padding: 14, borderBottom: '1px solid var(--line)' }}><SearchBox value={search} onChange={setSearch} placeholder="Search patients…" /></div>
          <div className="patient-list">
            {loadingList ? <Loading /> : shown.length === 0 ? <EmptyState icon="users" title="No patients" text={isAdmin ? 'Register patients first.' : 'Patients you treat will appear here.'} /> : shown.map((x) => (
              <button key={x.id} className={`patient-item ${selectedId === x.id ? 'active' : ''}`} onClick={() => select(x.id)}>
                <Avatar name={x.fullName} />
                <div style={{ minWidth: 0, flex: 1 }}>
                  <div className="name">{x.fullName}</div>
                  <div className="muted small">{patientNumber(x.id)} · {x.recordCount} record{x.recordCount === 1 ? '' : 's'}</div>
                </div>
                {x.admitted && <Badge tone="info">{x.wardName}</Badge>}
              </button>
            ))}
          </div>
        </div>

        <div>
          {!selectedId ? (
            <div className="panel"><EmptyState icon="filetext" title="Select a patient" text="Choose someone on the left to see their full medical history." /></div>
          ) : loadingDetail && !summary ? <div className="panel"><Loading /></div> : summary && (
            <>
              <div className="panel">
                <div className="patient-head">
                  <Avatar name={p.fullName} size={64} />
                  <div className="info">
                    <h2>{p.fullName}</h2>
                    <div className="muted">{patientNumber(p.id)}</div>
                    <div className="facts">
                      <div><b>{p.gender || '—'}</b>Gender</div>
                      <div><b>{ageFromDob(p.dateOfBirth) != null ? `${ageFromDob(p.dateOfBirth)} yrs` : '—'}</b>Age</div>
                      <div><b>{p.bloodGroup || '—'}</b>Blood group</div>
                      <div><b>{p.phoneNumber || '—'}</b>Phone</div>
                    </div>
                  </div>
                  <button className="btn" onClick={() => setAdding(true)}><Icon name="plus" size={16} /> New record</button>
                </div>
                {summary.admissions.some((a) => a.status === 'ADMITTED') && (
                  <div className="alert alert-info" style={{ marginTop: 14, marginBottom: 0 }}><Icon name="bed" size={16} />
                    <span>Currently admitted to {summary.admissions.find((a) => a.status === 'ADMITTED').ward.name}, bed {summary.admissions.find((a) => a.status === 'ADMITTED').bedNumber}.</span></div>
                )}
                <div className="mini-stats">
                  <div className="mini-stat"><div className="v">{summary.records.length}</div><div className="l">Medical records</div></div>
                  <div className="mini-stat"><div className="v">{summary.doctors.length}</div><div className="l">Doctors</div></div>
                  <div className="mini-stat"><div className="v">{summary.admissions.length}</div><div className="l">Ward stays</div></div>
                  <div className="mini-stat"><div className="v">{summary.appointmentCount}</div><div className="l">Appointments</div></div>
                  <div className="mini-stat"><div className="v">{summary.administrations.length}</div><div className="l">Medications given</div></div>
                </div>
              </div>

              <div style={{ marginTop: 18 }}>
                <Tabs value={tab} onChange={setTab} tabs={[
                  { value: 'records', label: 'Records', count: summary.records.length },
                  { value: 'doctors', label: 'Treating doctors', count: summary.doctors.length },
                  { value: 'wards', label: 'Ward stays', count: summary.admissions.length },
                  { value: 'meds', label: 'Medications', count: summary.administrations.length },
                ]} />

                {tab === 'records' && (summary.records.length === 0 ? (
                  <div className="panel"><EmptyState icon="filetext" title="No records yet" text="Add the first record after a visit." /></div>
                ) : (
                  <div className="timeline">
                    {summary.records.map((r) => (
                      <div className="t-item" key={r.id}>
                        <div className="t-card">
                          <div className="t-head">
                            <div><div className="strong">{fmtDate(r.visitDate)}</div><div className="muted small">{drName(r.doctor?.user?.fullName)} · {r.doctor?.doctorType?.replace(/_/g, ' ').toLowerCase()}</div></div>
                            <div className="tags">
                              {r.appointment && <Badge tone="primary">{APPOINTMENT_TYPE_LABELS[r.appointment.appointmentType]}</Badge>}
                              {r.admission && <Badge tone="info">{r.admission.ward?.name}</Badge>}
                            </div>
                          </div>
                          {r.diagnosis && <div className="t-field"><div className="k">Diagnosis</div>{r.diagnosis}</div>}
                          {r.prescription && <div className="t-field"><div className="k">Prescription</div>{r.prescription}</div>}
                          {r.notes && <div className="t-field"><div className="k">Notes</div>{r.notes}</div>}
                        </div>
                      </div>
                    ))}
                  </div>
                ))}

                {tab === 'doctors' && (summary.doctors.length === 0 ? (
                  <div className="panel"><EmptyState icon="user" title="No treating doctors yet" /></div>
                ) : (
                  <div className="team-grid">
                    {summary.doctors.map((d) => (
                      <div className="team-card" key={d.doctorId}>
                        <div className="cell-person"><Avatar name={d.name} size={44} /><div><div className="strong">{drName(d.name)}</div><div className="muted small">{d.doctorType}{d.specialization ? ` · ${d.specialization}` : ''}</div></div></div>
                        <div className="small"><b>{d.encounters}</b> encounter{d.encounters === 1 ? '' : 's'} · last seen {fmtDate(d.lastSeen)}</div>
                        <div className="tags">{d.through.map((t) => <Badge key={t} tone="muted">{t}</Badge>)}</div>
                      </div>
                    ))}
                  </div>
                ))}

                {tab === 'wards' && (
                  <div className="panel flush">
                    {summary.admissions.length === 0 ? <EmptyState icon="bed" title="Never admitted" text="This patient hasn't stayed in a ward." /> : (
                      <div className="table-wrap"><table>
                        <thead><tr><th>Ward</th><th>Admitted</th><th>Discharged</th><th>Attending</th><th>Status</th></tr></thead>
                        <tbody>
                          {summary.admissions.map((a) => (
                            <tr key={a.id}>
                              <td><div className="strong">{a.ward.name}</div><div className="muted small">{WARD_TYPE_LABELS[a.ward.wardType]} · Bed {a.bedNumber}</div>{a.reasonForAdmission && <div className="small">{a.reasonForAdmission}</div>}</td>
                              <td className="nowrap">{fmtDateTime(a.admittedAt)}</td>
                              <td className="nowrap">{a.dischargedAt ? <>{fmtDateTime(a.dischargedAt)}<div className="muted small">{daysBetween(a.admittedAt, a.dischargedAt)} day(s)</div></> : '—'}
                                {a.dischargeSummary && <div className="small muted">{a.dischargeSummary}</div>}</td>
                              <td>{a.attendingDoctor ? drName(a.attendingDoctor.user?.fullName) : '—'}</td>
                              <td><StatusBadge status={a.status} /></td>
                            </tr>
                          ))}
                        </tbody>
                      </table></div>
                    )}
                  </div>
                )}

                {tab === 'meds' && (
                  <div className="panel flush">
                    {summary.administrations.length === 0 ? <EmptyState icon="pill" title="No medications administered" /> : (
                      <div className="table-wrap"><table>
                        <thead><tr><th>When</th><th>Item</th><th>Qty</th><th>Given by</th><th>Notes</th></tr></thead>
                        <tbody>
                          {summary.administrations.map((m) => (
                            <tr key={m.id}>
                              <td className="nowrap">{fmtDateTime(m.administeredAt)}</td>
                              <td className="strong">{m.item?.name}</td>
                              <td>{m.quantity} {m.item?.unit}</td>
                              <td>{m.administeredBy}</td>
                              <td className="muted">{m.notes || '—'}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table></div>
                    )}
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>

      {adding && summary && (
        <NewRecordModal patient={summary.patient} presetAppointmentId={params.get('appointment')}
          onClose={() => { setAdding(false); if (params.get('appointment')) setParams({ patient: summary.patient.id }); }}
          onSaved={() => {
            toast.success('Record saved.'); setAdding(false);
            setParams({ patient: summary.patient.id });
            loadSummary(summary.patient.id);
            getRecordPatients().then(setPatients).catch(() => {});
          }} />
      )}
    </div>
  );
}

function NewRecordModal({ patient, presetAppointmentId, onClose, onSaved }) {
  const { isAdmin } = useAuth();
  const [doctors, setDoctors] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ doctorId: '', appointmentId: presetAppointmentId || '', visitDate: todayISO(), diagnosis: '', prescription: '', notes: '' });
  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  useEffect(() => {
    if (isAdmin) getDoctors().then(setDoctors).catch((e) => setError(e.message));
    getAppointments().then((a) => setAppointments(a.filter((x) => x.patient.id === patient.id && x.status !== 'CANCELLED'))).catch(() => {});
  }, [isAdmin, patient.id]);

  async function submit(e) {
    e.preventDefault();
    setError('');
    try {
      await createMedicalRecord({
        patientId: patient.id,
        doctorId: isAdmin ? Number(form.doctorId) : null,
        appointmentId: form.appointmentId ? Number(form.appointmentId) : null,
        visitDate: form.visitDate, diagnosis: form.diagnosis, prescription: form.prescription, notes: form.notes,
      });
      onSaved();
    } catch (err) { setError(err.message); }
  }

  return (
    <Modal title="New medical record" subtitle={patient.fullName} onClose={onClose}
      footer={<><button className="btn btn-ghost" onClick={onClose}>Cancel</button><button className="btn" form="record-form" type="submit">Save record</button></>}>
      <form id="record-form" onSubmit={submit}>
        <ErrorNote message={error} />
        <div className="form-grid">
          {isAdmin && (
            <div className="field"><label>Treating doctor</label>
              <select value={form.doctorId} onChange={(e) => set('doctorId', e.target.value)} required>
                <option value="">Select doctor</option>
                {doctors.map((d) => <option key={d.id} value={d.id}>{drName(d.user?.fullName)}</option>)}
              </select></div>
          )}
          <div className="field"><label>Visit date</label><input type="date" max={todayISO()} value={form.visitDate} onChange={(e) => set('visitDate', e.target.value)} required /></div>
          <div className="field full"><label>Linked appointment (optional)</label>
            <select value={form.appointmentId} onChange={(e) => set('appointmentId', e.target.value)}>
              <option value="">None</option>
              {appointments.map((a) => <option key={a.id} value={a.id}>{fmtDate(a.appointmentDate)} — {APPOINTMENT_TYPE_LABELS[a.appointmentType]}</option>)}
            </select></div>
          <div className="field full"><label>Diagnosis</label><textarea rows={2} value={form.diagnosis} onChange={(e) => set('diagnosis', e.target.value)} /></div>
          <div className="field full"><label>Prescription</label><textarea rows={2} value={form.prescription} onChange={(e) => set('prescription', e.target.value)} /></div>
          <div className="field full"><label>Notes</label><textarea rows={3} value={form.notes} onChange={(e) => set('notes', e.target.value)} /></div>
        </div>
      </form>
    </Modal>
  );
}
