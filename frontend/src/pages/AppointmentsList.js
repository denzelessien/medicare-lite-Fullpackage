import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getAppointments, updateAppointmentStatus, getAppointmentTypes } from '../api/client';
import { useAuth } from '../AuthContext';
import { useToast } from '../components/Toast';
import AdministerModal from '../components/AdministerModal';
import Icon from '../components/Icons';
import { Avatar, Badge, EmptyState, ErrorNote, Loading, PageHeader, SearchBox, StatusBadge, Tabs } from '../components/ui';
import { doctorLabel, fmtDate, fmtTime } from '../utils';

/** Can this appointment still be set back to "pending"? Only if its time hasn't arrived yet. */
function isFuture(a) {
  return new Date(`${a.appointmentDate}T${a.appointmentTime || '00:00:00'}`) > new Date();
}

export default function AppointmentsList() {
  const { isAdmin } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [types, setTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [status, setStatus] = useState('ALL');
  const [type, setType] = useState('');
  const [date, setDate] = useState('');
  const [search, setSearch] = useState('');
  const [administer, setAdminister] = useState(null);

  const load = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try { setItems(await getAppointments()); setError(''); }
    catch (err) { setError(err.message); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => {
    load();
    getAppointmentTypes().then(setTypes).catch(() => {});
    // Pending appointments flip to "Completed" on the server once their time passes - keep the list fresh.
    const t = setInterval(() => load(true), 60000);
    return () => clearInterval(t);
  }, [load]);

  async function changeStatus(a, next) {
    if (next === a.status) return;
    try {
      await updateAppointmentStatus(a.id, next);
      toast.success(`Marked as ${next.toLowerCase()}.`);
      load(true);
    } catch (err) { toast.error(err.message); }
  }

  const counts = useMemo(() => ({
    ALL: items.length,
    PENDING: items.filter((a) => a.status === 'PENDING').length,
    COMPLETED: items.filter((a) => a.status === 'COMPLETED').length,
    CANCELLED: items.filter((a) => a.status === 'CANCELLED').length,
  }), [items]);

  const shown = useMemo(() => items.filter((a) =>
    (status === 'ALL' || a.status === status)
    && (!type || a.appointmentType === type)
    && (!date || a.appointmentDate === date)
    && `${a.patient?.fullName} ${doctorLabel(a.doctor)} ${a.reasonForVisit || ''}`.toLowerCase().includes(search.toLowerCase())),
  [items, status, type, date, search]);

  const typeLabel = (v) => types.find((t) => t.value === v)?.label || v;

  return (
    <div>
      <PageHeader
        title={isAdmin ? 'Appointments' : 'My appointments'}
        subtitle="Pending appointments complete automatically once their time has passed."
        actions={<Link to="/appointments/new" className="btn"><Icon name="plus" size={16} /> Book appointment</Link>}
      />
      <ErrorNote message={error} />

      <Tabs value={status} onChange={setStatus} tabs={[
        { value: 'ALL', label: 'All', count: counts.ALL },
        { value: 'PENDING', label: 'Pending', count: counts.PENDING },
        { value: 'COMPLETED', label: 'Completed', count: counts.COMPLETED },
        { value: 'CANCELLED', label: 'Cancelled', count: counts.CANCELLED },
      ]} />

      <div className="toolbar">
        <SearchBox value={search} onChange={setSearch} placeholder="Search patient, doctor, reason…" />
        <select value={type} onChange={(e) => setType(e.target.value)}>
          <option value="">All types</option>
          {types.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
        </select>
        <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        {(date || type || search) && <button className="btn btn-ghost btn-sm" onClick={() => { setDate(''); setType(''); setSearch(''); }}>Clear</button>}
      </div>

      <div className="panel flush">
        {loading ? <Loading /> : shown.length === 0 ? (
          <EmptyState icon="calendar" title="No appointments match" text="Try changing the filters, or book a new appointment." />
        ) : (
          <div className="table-wrap"><table>
            <thead><tr><th>Patient</th>{isAdmin && <th>Doctor</th>}<th>Type</th><th>When</th><th>Status</th><th>Update status</th><th></th></tr></thead>
            <tbody>
              {shown.map((a) => (
                <tr key={a.id}>
                  <td><div className="cell-person"><Avatar name={a.patient?.fullName} /><div><div className="strong">{a.patient?.fullName}</div><div className="sub">{a.reasonForVisit || 'No reason given'}</div></div></div></td>
                  {isAdmin && <td>{doctorLabel(a.doctor)}<div className="muted small">{a.doctor?.doctorType?.replace(/_/g, ' ').toLowerCase()}</div></td>}
                  <td>
                    <Badge tone={a.appointmentType === 'SURGERY' ? 'bad' : ['XRAY', 'CT_SCAN', 'MRI', 'ULTRASOUND'].includes(a.appointmentType) ? 'info' : 'primary'}>{typeLabel(a.appointmentType)}</Badge>
                    {a.admission && <div className="muted small" style={{ marginTop: 3 }}>{a.admission.ward?.name} · Bed {a.admission.bedNumber}</div>}
                  </td>
                  <td className="nowrap"><div>{fmtDate(a.appointmentDate)}</div><div className="muted small">{fmtTime(a.appointmentTime)}</div></td>
                  <td><StatusBadge status={a.status} /></td>
                  <td>
                    {a.status === 'CANCELLED' ? <span className="muted small">Locked</span> : (
                      <select className="status-select" value={a.status} onChange={(e) => changeStatus(a, e.target.value)}>
                        <option value="PENDING" disabled={!isFuture(a)}>Pending{!isFuture(a) ? ' (time passed)' : ''}</option>
                        <option value="COMPLETED">Completed</option>
                        <option value="CANCELLED">Cancelled</option>
                      </select>
                    )}
                  </td>
                  <td className="right">
                    {a.status !== 'CANCELLED' && (
                      <div className="actions" style={{ justifyContent: 'flex-end' }}>
                        <button className="btn btn-ghost btn-sm" title="Give a drug, oxygen or consumable" onClick={() => setAdminister(a)}><Icon name="pill" size={14} /> Administer</button>
                        {a.status === 'COMPLETED' && <button className="btn btn-ghost btn-sm" onClick={() => navigate(`/medical-records?patient=${a.patient.id}&appointment=${a.id}`)}><Icon name="filetext" size={14} /> Record</button>}
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table></div>
        )}
      </div>

      {administer && (
        <AdministerModal patient={administer.patient} appointmentId={administer.id} admissionId={administer.admission?.id}
          onClose={() => setAdminister(null)} onDone={() => load(true)} />
      )}
    </div>
  );
}
