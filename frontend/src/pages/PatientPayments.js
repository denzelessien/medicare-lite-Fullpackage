import React, { useCallback, useEffect, useState } from 'react';
import { getMyPayments } from '../api/client';
import { useAuth } from '../AuthContext';
import PaymentCheckout from '../components/PaymentCheckout';
import ReceiptModal from '../components/ReceiptModal';
import Icon from '../components/Icons';
import { EmptyState, ErrorNote, Loading, PageHeader, StatusBadge } from '../components/ui';
import { fmtDateTime, fmtMoney } from '../utils';

export default function PatientPayments() {
  const { user } = useAuth();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [checkout, setCheckout] = useState(null);
  const [receiptId, setReceiptId] = useState(null);

  const load = useCallback(async () => {
    try { setItems(await getMyPayments()); setError(''); } catch (err) { setError(err.message); } finally { setLoading(false); }
  }, []);
  useEffect(() => { load(); }, [load]);

  const outstanding = items.filter((p) => p.status === 'PENDING').reduce((s, p) => s + Number(p.amount), 0);
  const paid = items.filter((p) => p.status === 'PAID').reduce((s, p) => s + Number(p.amount), 0);

  return (
    <div>
      <PageHeader title="My payments" subtitle="Pay your bills securely and download receipts." />
      <ErrorNote message={error} />
      <div className="stat-grid">
        <div className="stat-card"><div className="stat-icon warn"><Icon name="clipboard" size={22} /></div><div><div className="stat-value">{fmtMoney(outstanding)}</div><div className="stat-label">To pay</div></div></div>
        <div className="stat-card"><div className="stat-icon ok"><Icon name="receipt" size={22} /></div><div><div className="stat-value">{fmtMoney(paid)}</div><div className="stat-label">Paid so far</div></div></div>
      </div>

      <div className="panel flush">
        {loading ? <Loading /> : items.length === 0 ? <EmptyState icon="card" title="No bills yet" text="Fees for your appointments and treatments will appear here." /> : (
          <div className="table-wrap"><table>
            <thead><tr><th>Description</th><th>Amount</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {items.map((p) => (
                <tr key={p.id}>
                  <td><div className="strong">{p.description}</div><div className="muted small">{p.reference} · {fmtDateTime(p.paidAt || p.createdAt)}</div></td>
                  <td className="strong nowrap">{fmtMoney(p.amount)}</td>
                  <td><StatusBadge status={p.status} />{p.failureReason && p.status === 'PENDING' && <div className="small" style={{ color: 'var(--danger)' }}>Last attempt failed</div>}</td>
                  <td className="right">
                    {p.status === 'PENDING' && <button className="btn btn-sm" onClick={() => setCheckout(p)}><Icon name="lock" size={14} /> Pay now</button>}
                    {(p.status === 'PAID' || p.status === 'REFUNDED') && <button className="btn btn-secondary btn-sm" onClick={() => setReceiptId(p.id)}><Icon name="receipt" size={14} /> Receipt</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table></div>
        )}
      </div>

      {checkout && (
        <PaymentCheckout payment={checkout} defaultName={user.fullName} defaultEmail={user.email}
          onClose={() => { setCheckout(null); load(); }} onPaid={load}
          onViewReceipt={(paidP) => { setCheckout(null); setReceiptId(paidP.id); }} />
      )}
      {receiptId && <ReceiptModal paymentId={receiptId} onClose={() => setReceiptId(null)} />}
    </div>
  );
}
