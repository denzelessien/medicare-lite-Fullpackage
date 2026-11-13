import React, { useEffect, useState } from 'react';
import { getMyCareTeam } from '../api/client';
import { Avatar, Badge, EmptyState, ErrorNote, Loading, PageHeader } from '../components/ui';
import { drName, fmtDate } from '../utils';

export default function PatientCareTeam() {
  const [team, setTeam] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { getMyCareTeam().then(setTeam).catch((e) => setError(e.message)); }, []);

  return (
    <div>
      <PageHeader title="My care team" subtitle="The doctors who are treating you or have you booked." />
      <ErrorNote message={error} />
      {!team && !error ? <Loading /> : team && team.length === 0 ? (
        <div className="panel"><EmptyState icon="user" title="No doctors yet" text="Once you book an appointment, your doctor will appear here." /></div>
      ) : team && (
        <div className="team-grid">
          {team.map((d) => (
            <div className="team-card" key={d.doctorId}>
              <div className="cell-person">
                <Avatar name={d.name} size={46} />
                <div><div className="strong">{drName(d.name)}</div><div className="muted small">{d.doctorType}{d.specialization ? ` · ${d.specialization}` : ''}</div></div>
              </div>
              <div className="tags">
                {d.pendingAppointments > 0 && <Badge tone="warn">{d.pendingAppointments} upcoming</Badge>}
                {d.pastAppointments > 0 && <Badge tone="ok">{d.pastAppointments} completed</Badge>}
              </div>
              {d.nextAppointment && <div className="small"><span className="muted">Next visit:</span> <b>{fmtDate(d.nextAppointment)}</b> · {d.nextAppointmentType}</div>}
              {d.phoneNumber && <div className="small muted">Contact: {d.phoneNumber}</div>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
