import React, { useEffect, useMemo, useState } from 'react';
import Modal from './Modal';
import { ErrorNote } from './ui';
import { useToast } from './Toast';
import { getItems, getPatients, administerItem } from '../api/client';
import { fmtMoney, fmtDate, todayISO, ITEM_TYPE_LABELS } from '../utils';

/**
 * Give a drug / oxygen / consumable to a patient. Saving this deducts the quantity from inventory.
 * Pass `patient` (and optionally appointmentId / admissionId) to lock it to a specific context.
 */
export default function AdministerModal({ patient, appointmentId, admissionId, onClose, onDone }) {
  const toast = useToast();
  const [items, setItems] = useState([]);
  const [patients, setPatients] = useState([]);
  const [form, setForm] = useState({ itemId: '', patientId: patient?.id || '', quantity: 1, notes: '', bill: true });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    getItems().then((all) => {
      const today = todayISO();
      setItems(all.filter((i) => i.stockQuantity > 0 && (!i.expiryDate || i.expiryDate >= today)));
    }).catch((e) => setError(e.message));
    if (!patient) getPatients().then(setPatients).catch((e) => setError(e.message));
  }, [patient]);

  const item = useMemo(() => items.find((i) => String(i.id) === String(form.itemId)), [items, form.itemId]);
  const total = item && item.unitPrice ? Number(item.unitPrice) * (Number(form.quantity) || 0) : 0;

  const grouped = useMemo(() => {
    const g = {};
    items.forEach((i) => { (g[i.itemType] = g[i.itemType] || []).push(i); });
    return g;
  }, [items]);

  async function submit(e) {
    e.preventDefault();
    setError('');
    setSaving(true);
    try {
      await administerItem({
        itemId: Number(form.itemId),
        patientId: Number(form.patientId),
        quantity: Number(form.quantity),
        appointmentId: appointmentId || null,
        admissionId: admissionId || null,
        notes: form.notes,
        billPatient: form.bill,
      });
      const left = item.stockQuantity - Number(form.quantity);
      toast.success(`Administered ${form.quantity} ${item.unit} of ${item.name}. ${left} ${item.unit} left in stock.`);
      onDone && onDone();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      title="Administer to patient"
      subtitle="Stock is reduced automatically when you save."
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost" type="button" onClick={onClose}>Cancel</button>
          <button className="btn" form="administer-form" type="submit" disabled={saving || !form.itemId}>
            {saving ? 'Saving…' : 'Administer'}
          </button>
        </>
      }
    >
      <form id="administer-form" onSubmit={submit}>
        <ErrorNote message={error} />

        {patient ? (
          <div className="field"><label>Patient</label><div className="strong">{patient.fullName}</div></div>
        ) : (
          <div className="field">
            <label>Patient</label>
            <select value={form.patientId} onChange={(e) => setForm({ ...form, patientId: e.target.value })} required>
              <option value="">Select patient</option>
              {patients.map((p) => <option key={p.id} value={p.id}>{p.fullName}</option>)}
            </select>
          </div>
        )}

        <div className="field">
          <label>Item (drug, oxygen, consumable…)</label>
          <select value={form.itemId} onChange={(e) => setForm({ ...form, itemId: e.target.value, quantity: 1 })} required>
            <option value="">Select item</option>
            {Object.keys(grouped).map((type) => (
              <optgroup key={type} label={ITEM_TYPE_LABELS[type] || type}>
                {grouped[type].map((i) => (
                  <option key={i.id} value={i.id}>{i.name} — {i.stockQuantity} {i.unit} in stock</option>
                ))}
              </optgroup>
            ))}
          </select>
        </div>

        {item && (
          <div className="form-grid">
            <div className="field">
              <label>Quantity ({item.unit})</label>
              <input type="number" min="1" max={item.stockQuantity} value={form.quantity}
                onChange={(e) => setForm({ ...form, quantity: e.target.value })} required />
              <span className="hint">Available: {item.stockQuantity} {item.unit}{item.expiryDate ? ` · expires ${fmtDate(item.expiryDate)}` : ''}</span>
            </div>
            <div className="field">
              <label>Cost</label>
              <div className="strong" style={{ padding: '9px 0' }}>{item.unitPrice ? fmtMoney(total) : 'Not priced'}</div>
            </div>
          </div>
        )}

        <div className="field">
          <label>Notes (dose, route, flow rate…)</label>
          <textarea rows={2} value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} placeholder="e.g. 2 L/min via nasal cannula" />
        </div>

        {item && item.unitPrice > 0 && (
          <label className="check">
            <input type="checkbox" checked={form.bill} onChange={(e) => setForm({ ...form, bill: e.target.checked })} />
            Add {fmtMoney(total)} to the patient's bill
          </label>
        )}
      </form>
    </Modal>
  );
}
