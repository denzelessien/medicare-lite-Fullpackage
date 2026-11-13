import React from 'react';
import Icon from './Icons';
import { initials } from '../utils';

const STATUS_LABELS = {
  PENDING: 'Pending', COMPLETED: 'Completed', CANCELLED: 'Cancelled',
  PAID: 'Paid', REFUNDED: 'Refunded', VOIDED: 'Voided',
  ADMITTED: 'Admitted', DISCHARGED: 'Discharged',
};

const STATUS_TONE = {
  PENDING: 'warn', COMPLETED: 'ok', CANCELLED: 'bad',
  PAID: 'ok', REFUNDED: 'info', VOIDED: 'muted',
  ADMITTED: 'info', DISCHARGED: 'muted',
};

export function StatusBadge({ status }) {
  return <span className={`badge badge-${STATUS_TONE[status] || 'muted'}`}>{STATUS_LABELS[status] || status}</span>;
}

export function Badge({ tone = 'muted', children }) {
  return <span className={`badge badge-${tone}`}>{children}</span>;
}

export function Avatar({ name, size = 36 }) {
  return (
    <span className="avatar" style={{ width: size, height: size, fontSize: size * 0.38 }}>{initials(name)}</span>
  );
}

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  );
}

export function Loading({ label = 'Loading…' }) {
  return (
    <div className="loading">
      <span className="spinner" />
      <span>{label}</span>
    </div>
  );
}

export function EmptyState({ icon = 'clipboard', title, text, action }) {
  return (
    <div className="empty">
      <div className="empty-icon"><Icon name={icon} size={26} /></div>
      <div className="empty-title">{title}</div>
      {text && <div className="muted">{text}</div>}
      {action}
    </div>
  );
}

export function ErrorNote({ message }) {
  if (!message) return null;
  return <div className="alert alert-error"><Icon name="alert" size={16} /><span>{message}</span></div>;
}

export function Tabs({ tabs, value, onChange }) {
  return (
    <div className="tabs" role="tablist">
      {tabs.map((t) => (
        <button
          key={t.value}
          role="tab"
          aria-selected={value === t.value}
          className={`tab ${value === t.value ? 'active' : ''}`}
          onClick={() => onChange(t.value)}
        >
          {t.label}
          {t.count != null && <span className="tab-count">{t.count}</span>}
        </button>
      ))}
    </div>
  );
}

export function SearchBox({ value, onChange, placeholder = 'Search…' }) {
  return (
    <div className="search">
      <Icon name="search" size={16} />
      <input value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} />
    </div>
  );
}

export function ProgressBar({ value, max, tone }) {
  const pct = max > 0 ? Math.min(100, Math.round((value / max) * 100)) : 0;
  const auto = pct >= 90 ? 'bad' : pct >= 70 ? 'warn' : 'ok';
  return (
    <div className="progress"><div className={`progress-bar progress-${tone || auto}`} style={{ width: `${pct}%` }} /></div>
  );
}
