import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyAppointments, cancelMyAppointment } from '../api/client';
import { useToast } from '../components/Toast';
import Icon from '../components/Icons';
import { Avatar, Badge, EmptyState, ErrorNote, Loading, PageHeader, StatusBadge, Tabs } from '../components/ui';
import { APPOINTMENT_TYPE_LABELS, doctorLabel, fmtDate, fmtTime } from '../utils';

export default function PatientAppointments() {
  const toast = useToast();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tab, setTab] = useState('PENDING');

  const load = useCallback(async () => {
    try { setItems(await getMyAppointments()); setError(''); }
    catch (err) { setError(err.message); } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); const t = setInterval(load, 60000); return () => clearInterval(t); }, [load]);

  async function cancel(a) {
    if (!window.confirm('Cancel this appointment?')) return;
    try { await cancelMyAppointment(a.id); toast.success('Appointment cancelled.'); load(); }
    catch (err) { toast.error(err.message); }
  }

  const shown = items.filter((a) => tab === 'ALL' || a.status === tab);
  const pendingFirst = [...shown].sort((a, b) => tab === 'PENDING'
    ? `${a.appointmentDate}${a.appointmentTime}`.localeCompare(`${b.appointmentDate}${b.appointmentTime}`) : 0);

  return (
    <div>
      <PageHeader title="My appointments" subtitle="Your upcoming and past visits."
        actions={<Link to="/book-visit" className="btn"><Icon name="plus" size={16} /> Book a visit</Link>} />
      <ErrorNote message={error} />
      <Tabs value={tab} onChange={setTab} tabs={[
        { value: 'PENDING', label: 'Upcoming', count: items.filter((a) => a.status === 'PENDING').length },
        { value: 'COMPLETED', label: 'Completed', count: items.filter((a) => a.status === 'COMPLETED').length },
        { value: 'CANCELLED', label: 'Cancelled', count: items.filter((a) => a.status === 'CANCELLED').length },
        { value: 'ALL', label: 'All' },
      ]} />
      <div className="panel flush">
        {loading ? <Loading /> : pendingFirst.length === 0 ? (
          <EmptyState icon="calendar" title="No appointments here" text="Book a visit to get started."
            action={<Link to="/book-visit" className="btn btn-sm">Book a visit</Link>} />
        ) : (
          <div className="table-wrap"><table>
            <thead><tr><th>Doctor</th><th>Type</th><th>When</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {pendingFirst.map((a) => (
                <tr key={a.id}>
                  <td><div className="cell-person"><Avatar name={a.doctor?.user?.fullName} /><div><div className="strong">{doctorLabel(a.doctor)}</div><div className="sub">{a.doctor?.doctorType?.replace(/_/g, ' ').toLowerCase()}</div></div></div></td>
                  <td><Badge tone="primary">{APPOINTMENT_TYPE_LABELS[a.appointmentType] || a.appointmentType}</Badge><div className="muted small" style={{ marginTop: 3 }}>{a.reasonForVisit}</div></td>
                  <td className="nowrap"><div>{fmtDate(a.appointmentDate)}</div><div className="muted small">{fmtTime(a.appointmentTime)}</div></td>
                  <td><StatusBadge status={a.status} /></td>
                  <td className="right">{a.status === 'PENDING' && <button className="btn btn-danger btn-sm" onClick={() => cancel(a)}>Cancel</button>}</td>
                </tr>
              ))}
            </tbody>
          </table></div>
        )}
      </div>
    </div>
  );
}
