import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { getReportSummary, getDoctorSummary, getAppointments, getWards, getItems, getAdmissions } from '../api/client';
import Icon from '../components/Icons';
import { Avatar, Badge, EmptyState, ErrorNote, Loading, PageHeader, ProgressBar, StatusBadge } from '../components/ui';
import { drName, APPOINTMENT_TYPE_LABELS, doctorLabel, fmtDate, fmtMoneyShort, fmtTime, WARD_TYPE_LABELS } from '../utils';

function Stat({ icon, tone = '', value, label }) {
  return (
    <div className="stat-card">
      <div className={`stat-icon ${tone}`}><Icon name={icon} size={22} /></div>
      <div><div className="stat-value">{value}</div><div className="stat-label">{label}</div></div>
    </div>
  );
}

export default function Dashboard() {
  const { isAdmin, user } = useAuth();
  const [summary, setSummary] = useState(null);
  const [upcoming, setUpcoming] = useState([]);
  const [wards, setWards] = useState([]);
  const [lowStock, setLowStock] = useState([]);
  const [admitted, setAdmitted] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    async function load() {
      try {
        const [s, appts] = await Promise.all([isAdmin ? getReportSummary() : getDoctorSummary(), getAppointments({ status: 'PENDING' })]);
        setSummary(s);
        setUpcoming([...appts].sort((a, b) => `${a.appointmentDate}${a.appointmentTime}`.localeCompare(`${b.appointmentDate}${b.appointmentTime}`)).slice(0, 6));
        if (isAdmin) {
          const [w, items] = await Promise.all([getWards(), getItems()]);
          setWards(w.filter((x) => x.active));
          setLowStock(items.filter((i) => i.stockQuantity <= (i.reorderLevel ?? 10)));
        } else {
          setAdmitted(await getAdmissions('ADMITTED'));
        }
      } catch (err) {
        setError(err.message);
      }
    }
    load();
  }, [isAdmin]);

  if (error) return <ErrorNote message={error} />;
  if (!summary) return <Loading />;

  return (
    <div>
      <PageHeader
        title={isAdmin ? 'Dashboard' : `Good day, ${drName(user.fullName)}`}
        subtitle={isAdmin ? 'A live look at the whole hospital.' : 'Your schedule and ward patients at a glance.'}
      />

      {isAdmin ? (
        <div className="stat-grid">
          <Stat icon="users" value={summary.totalPatients} label="Registered patients" />
          <Stat icon="user" tone="info" value={summary.totalDoctors} label="Doctors" />
          <Stat icon="calendar" value={summary.appointmentsToday} label="Appointments today" />
          <Stat icon="clipboard" tone="warn" value={summary.pendingAppointments} label="Pending appointments" />
          <Stat icon="bed" tone="info" value={`${summary.admittedPatients}/${summary.totalBeds}`} label="Beds occupied" />
          <Stat icon="pill" tone={summary.lowStockItems ? 'bad' : 'ok'} value={summary.lowStockItems} label="Low-stock items" />
          <Stat icon="card" tone="warn" value={summary.pendingPayments} label="Unpaid charges" />
          <Stat icon="receipt" tone="ok" value={fmtMoneyShort(summary.totalRevenue)} label="Revenue collected" />
        </div>
      ) : (
        <div className="stat-grid">
          <Stat icon="calendar" value={summary.appointmentsToday} label="Appointments today" />
          <Stat icon="clipboard" tone="warn" value={summary.pendingAppointments} label="Pending appointments" />
          <Stat icon="bed" tone="info" value={summary.admittedPatients} label="Ward patients in your care" />
        </div>
      )}

      <div className="grid-2">
        <div className="panel flush">
          <div className="panel-title" style={{ padding: '18px 22px 0' }}>
            <h3>Upcoming appointments</h3>
            <Link to="/appointments" className="small">View all</Link>
          </div>
          {upcoming.length === 0 ? (
            <EmptyState icon="calendar" title="Nothing scheduled" text="Pending appointments will show up here." />
          ) : (
            <div className="table-wrap">
              <table>
                <tbody>
                  {upcoming.map((a) => (
                    <tr key={a.id}>
                      <td>
                        <div className="cell-person">
                          <Avatar name={a.patient?.fullName} />
                          <div>
                            <div className="strong">{a.patient?.fullName}</div>
                            <div className="sub">{APPOINTMENT_TYPE_LABELS[a.appointmentType]} · {isAdmin ? doctorLabel(a.doctor) : fmtDate(a.appointmentDate)}</div>
                          </div>
                        </div>
                      </td>
                      <td className="right nowrap">
                        <div>{fmtDate(a.appointmentDate)}</div>
                        <div className="muted small">{fmtTime(a.appointmentTime)}</div>
                      </td>
                      <td><StatusBadge status={a.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {isAdmin ? (
          <div className="grid" style={{ display: 'grid', gap: 20 }}>
            <div className="panel">
              <div className="panel-title"><h3>Ward occupancy</h3><Link to="/wards" className="small">Manage wards</Link></div>
              {wards.length === 0 ? <div className="muted">No wards yet.</div> : wards.map((w) => (
                <div key={w.id} style={{ marginBottom: 14 }}>
                  <div className="list-row" style={{ padding: '0 0 5px', border: 'none' }}>
                    <span className="strong">{w.name} <span className="muted small">· {WARD_TYPE_LABELS[w.wardType]}</span></span>
                    <span className="muted small">{w.occupied}/{w.capacity} beds</span>
                  </div>
                  <ProgressBar value={w.occupied} max={w.capacity} />
                </div>
              ))}
            </div>
            <div className="panel">
              <div className="panel-title"><h3>Low stock</h3><Link to="/pharmacy" className="small">Open pharmacy</Link></div>
              {lowStock.length === 0 ? <div className="muted">Everything is well stocked.</div> : lowStock.slice(0, 6).map((i) => (
                <div className="list-row" key={i.id}>
                  <span>{i.name}</span>
                  <Badge tone={i.stockQuantity === 0 ? 'bad' : 'warn'}>{i.stockQuantity} {i.unit} left</Badge>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className="panel">
            <div className="panel-title"><h3>My ward patients</h3><Link to="/wards" className="small">Open wards</Link></div>
            {admitted.length === 0 ? <div className="muted">No admitted patients under your care.</div> : admitted.map((a) => (
              <div className="list-row" key={a.id}>
                <div className="cell-person"><Avatar name={a.patient?.fullName} /><div><div className="strong">{a.patient?.fullName}</div><div className="sub">{a.reasonForAdmission || 'No reason noted'}</div></div></div>
                <Badge tone="info">{a.ward?.name} · Bed {a.bedNumber}</Badge>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
