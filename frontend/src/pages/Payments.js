import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { getPayments, getPatients, createCharge, payCash, refundPayment, voidPayment } from '../api/client';
import { useToast } from '../components/Toast';
import Modal from '../components/Modal';
import PaymentCheckout from '../components/PaymentCheckout';
import ReceiptModal from '../components/ReceiptModal';
import Icon from '../components/Icons';
import { Avatar, EmptyState, ErrorNote, Loading, PageHeader, SearchBox, StatusBadge, Tabs } from '../components/ui';
import { fmtDateTime, fmtMoney } from '../utils';

const METHOD = { CARD: 'Card', BANK_TRANSFER: 'Bank transfer', CASH: 'Cash' };

export default function Payments() {
  const toast = useToast();
  const [items, setItems] = useState([]);
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tab, setTab] = useState('ALL');
  const [search, setSearch] = useState('');
  const [chargeOpen, setChargeOpen] = useState(false);
  const [form, setForm] = useState({ patientId: '', amount: '', description: '' });
  const [formError, setFormError] = useState('');
  const [checkout, setCheckout] = useState(null);
  const [receiptId, setReceiptId] = useState(null);

  const load = useCallback(async () => {
    try { setItems(await getPayments()); setError(''); } catch (err) { setError(err.message); } finally { setLoading(false); }
  }, []);
  useEffect(() => { load(); getPatients().then(setPatients).catch(() => {}); }, [load]);

  const sum = (st) => items.filter((p) => p.status === st).reduce((s, p) => s + Number(p.amount), 0);
  const shown = useMemo(() => items.filter((p) => (tab === 'ALL' || p.status === tab)
    && `${p.patient?.fullName} ${p.reference} ${p.description || ''}`.toLowerCase().includes(search.toLowerCase())), [items, tab, search]);

  async function act(fn, ok) {
    try { await fn(); toast.success(ok); load(); } catch (err) { toast.error(err.message); }
  }

  async function saveCharge(e) {
    e.preventDefault(); setFormError('');
    try {
      await createCharge({ patientId: Number(form.patientId), amount: Number(form.amount), description: form.description });
      toast.success('Charge created.'); setChargeOpen(false); setForm({ patientId: '', amount: '', description: '' }); load();
    } catch (err) { setFormError(err.message); }
  }

  return (
    <div>
      <PageHeader title="Payments" subtitle="Charges, demo card & bank payments, and receipts."
        actions={<button className="btn" onClick={() => { setFormError(''); setChargeOpen(true); }}><Icon name="plus" size={16} /> New charge</button>} />
      <ErrorNote message={error} />

      <div className="stat-grid">
        <div className="stat-card"><div className="stat-icon warn"><Icon name="clipboard" size={22} /></div><div><div className="stat-value">{fmtMoney(sum('PENDING'))}</div><div className="stat-label">Outstanding</div></div></div>
        <div className="stat-card"><div className="stat-icon ok"><Icon name="receipt" size={22} /></div><div><div className="stat-value">{fmtMoney(sum('PAID'))}</div><div className="stat-label">Collected</div></div></div>
        <div className="stat-card"><div className="stat-icon info"><Icon name="card" size={22} /></div><div><div className="stat-value">{fmtMoney(sum('REFUNDED'))}</div><div className="stat-label">Refunded</div></div></div>
      </div>

      <Tabs value={tab} onChange={setTab} tabs={[
        { value: 'ALL', label: 'All', count: items.length },
        { value: 'PENDING', label: 'Pending', count: items.filter((p) => p.status === 'PENDING').length },
        { value: 'PAID', label: 'Paid', count: items.filter((p) => p.status === 'PAID').length },
        { value: 'REFUNDED', label: 'Refunded' }, { value: 'VOIDED', label: 'Voided' },
      ]} />
      <div className="toolbar"><SearchBox value={search} onChange={setSearch} placeholder="Search patient or reference…" /></div>

      <div className="panel flush">
        {loading ? <Loading /> : shown.length === 0 ? <EmptyState icon="card" title="No payments" text="Appointment fees, ward stays and medication charges appear here." /> : (
          <div className="table-wrap"><table>
            <thead><tr><th>Patient</th><th>Description</th><th>Amount</th><th>Status</th><th>Paid with</th><th></th></tr></thead>
            <tbody>
              {shown.map((p) => (
                <tr key={p.id}>
                  <td><div className="cell-person"><Avatar name={p.patient?.fullName} /><div><div className="strong">{p.patient?.fullName}</div><div className="sub">{p.reference}</div></div></div></td>
                  <td>{p.description}<div className="muted small">{fmtDateTime(p.paidAt || p.createdAt)}</div></td>
                  <td className="strong nowrap">{fmtMoney(p.amount)}</td>
                  <td><StatusBadge status={p.status} />{p.failureReason && p.status === 'PENDING' && <div className="small" style={{ color: 'var(--danger)' }}>{p.failureReason}</div>}</td>
                  <td>{p.method ? <>{METHOD[p.method]}<div className="muted small">{p.payerName}</div></> : <span className="muted">—</span>}</td>
                  <td className="right"><div className="actions" style={{ justifyContent: 'flex-end' }}>
                    {p.status === 'PENDING' && <>
                      <button className="btn btn-sm" onClick={() => setCheckout(p)}>Collect</button>
                      <button className="btn btn-ghost btn-sm" onClick={() => act(() => payCash(p.id), 'Cash payment recorded.')}>Cash</button>
                      <button className="btn btn-danger btn-sm" onClick={() => window.confirm('Void this charge?') && act(() => voidPayment(p.id), 'Charge voided.')}>Void</button>
                    </>}
                    {(p.status === 'PAID' || p.status === 'REFUNDED') && <button className="btn btn-secondary btn-sm" onClick={() => setReceiptId(p.id)}><Icon name="receipt" size={14} /> Receipt</button>}
                    {p.status === 'PAID' && <button className="btn btn-danger btn-sm" onClick={() => window.confirm('Refund this payment?') && act(() => refundPayment(p.id), 'Payment refunded.')}>Refund</button>}
                  </div></td>
                </tr>
              ))}
            </tbody>
          </table></div>
        )}
      </div>

      {chargeOpen && (
        <Modal title="New charge" size="sm" onClose={() => setChargeOpen(false)}
          footer={<><button className="btn btn-ghost" onClick={() => setChargeOpen(false)}>Cancel</button><button className="btn" form="charge-form" type="submit">Create charge</button></>}>
          <form id="charge-form" onSubmit={saveCharge}>
            <ErrorNote message={formError} />
            <div className="field"><label>Patient</label>
              <select value={form.patientId} onChange={(e) => setForm({ ...form, patientId: e.target.value })} required>
                <option value="">Select patient</option>{patients.map((p) => <option key={p.id} value={p.id}>{p.fullName}</option>)}</select></div>
            <div className="field"><label>Amount (₦)</label><input type="number" min="1" step="0.01" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} required /></div>
            <div className="field"><label>Description</label><input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="e.g. Deposit, lab fee" required /></div>
          </form>
        </Modal>
      )}

      {checkout && (
        <PaymentCheckout payment={checkout} defaultName={checkout.patient?.fullName}
          onClose={() => { setCheckout(null); load(); }} onPaid={load}
          onViewReceipt={(paid) => { setCheckout(null); setReceiptId(paid.id); }} />
      )}
      {receiptId && <ReceiptModal paymentId={receiptId} onClose={() => setReceiptId(null)} />}
    </div>
  );
}
