import React from 'react';
import Icon from './Icons';
import { ThemeToggle } from '../ThemeContext';

export default function AuthLayout({ children, wide = false }) {
  return (
    <div className="auth-page">
      <div className="auth-hero">
        <div className="brand">
          <div className="brand-mark"><Icon name="heart" size={20} /></div>
          <div className="brand-name">MediCare Lite</div>
        </div>
        <div>
          <h2>Care that stays organised, from booking to bedside.</h2>
          <p>Appointments, wards, pharmacy, records and payments in one secure place.</p>
          <ul className="auth-points">
            <li><span className="dot"><Icon name="calendar" size={16} /></span> Book checkups, scans and surgeries</li>
            <li><span className="dot"><Icon name="bed" size={16} /></span> Ward admissions and live bed tracking</li>
            <li><span className="dot"><Icon name="receipt" size={16} /></span> Pay online and download receipts</li>
          </ul>
        </div>
        <div className="small" style={{ color: 'rgba(255,255,255,0.6)' }}>© {new Date().getFullYear()} MediCare Lite</div>
      </div>
      <div className="auth-form-side">
        <div className={`auth-card ${wide ? 'wide' : ''}`}>
          <div className="auth-top">
            <div className="muted small strong">MediCare Lite</div>
            <ThemeToggle />
          </div>
          {children}
        </div>
      </div>
    </div>
  );
}
