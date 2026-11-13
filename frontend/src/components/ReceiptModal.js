import React, { useEffect, useState } from 'react';
import Modal from './Modal';
import Icon from './Icons';
import { Loading, ErrorNote } from './ui';
import { getReceipt } from '../api/client';
import { fmtMoney, fmtDateTime } from '../utils';

const METHOD_LABEL = { CARD: 'Debit / credit card', BANK_TRANSFER: 'Bank transfer', CASH: 'Cash' };

export function Receipt({ r }) {
  return (
    <div className="receipt receipt-print">
      <div className="r-head">
        <div className="r-brand">
          <div className="brand-mark"><Icon name="heart" size={22} /></div>
          <div>
            <h2 style={{ margin: 0 }}>{r.hospitalName}</h2>
            <div className="meta">{r.hospitalAddress}</div>
            <div className="meta">{r.hospitalPhone}</div>
          </div>
        </div>
        <div className="r-title">
          <div className="t">PAYMENT RECEIPT</div>
          <div className="meta">Receipt no. <b>{r.receiptNumber}</b></div>
          <div className="meta">{fmtDateTime(r.paidAt)}</div>
        </div>
      </div>

      <div className={`stamp ${r.status === 'REFUNDED' ? 'refunded' : ''}`}>{r.status === 'REFUNDED' ? 'REFUNDED' : 'PAID'}</div>

      <div className="r-cols">
        <div>
          <div className="label">Patient</div>
          <div className="val">{r.patientName}</div>
          <div className="meta">{r.patientNumber}{r.patientPhone ? ` · ${r.patientPhone}` : ''}</div>
        </div>
        <div>
          <div className="label">Paid by</div>
          <div className="val">{r.payerName || '—'}</div>
          {r.payerEmail && <div className="meta">{r.payerEmail}</div>}
        </div>
      </div>

      <table>
        <thead><tr><th>Description</th><th className="right">Amount</th></tr></thead>
        <tbody>
          <tr>
            <td>
              <div className="val">{r.description || 'Hospital charge'}</div>
              {r.appointmentSummary && <div className="meta">{r.appointmentSummary}</div>}
            </td>
            <td className="right nowrap">{fmtMoney(r.amount)}</td>
          </tr>
        </tbody>
      </table>

      <div className="divider" style={{ borderColor: '#e8eeec' }} />
      <div className="label" style={{ marginBottom: 8 }}>Payment details</div>
      <div className="r-cols" style={{ marginBottom: 0 }}>
        <dl className="kv" style={{ gridTemplateColumns: '120px 1fr' }}>
          <dt>Method</dt><dd>{METHOD_LABEL[r.method] || r.method}</dd>
          <dt>{r.method === 'CARD' ? 'Card' : 'Bank'}</dt><dd>{r.bankName || '—'}</dd>
          {r.method === 'BANK_TRANSFER' && <><dt>Account name</dt><dd>{r.accountName || '—'}</dd></>}
          {r.method === 'BANK_TRANSFER' && <><dt>Account no.</dt><dd>{r.accountNumber || '—'}</dd></>}
          {r.method === 'CARD' && <><dt>Card number</dt><dd>•••• •••• •••• {r.cardLast4}</dd></>}
          {r.method === 'CASH' && r.receivedBy && <><dt>Received by</dt><dd>{r.receivedBy}</dd></>}
        </dl>
        <dl className="kv" style={{ gridTemplateColumns: '120px 1fr' }}>
          <dt>Reference</dt><dd>{r.reference}</dd>
          <dt>Transaction ID</dt><dd>{r.transactionRef || '—'}</dd>
          <dt>Currency</dt><dd>{r.currency}</dd>
        </dl>
      </div>

      <div className="total">
        <span>Total paid</span>
        <span className="amt">{fmtMoney(r.amount)}</span>
      </div>
      <div className="foot">
        This is a computer-generated receipt. Payments in this system are a demo simulation and no real money was moved.
      </div>
    </div>
  );
}

/** Fetches and shows the receipt for a payment, with a print / save-as-PDF button. */
export default function ReceiptModal({ paymentId, onClose }) {
  const [receipt, setReceipt] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getReceipt(paymentId).then(setReceipt).catch((e) => setError(e.message));
  }, [paymentId]);

  return (
    <Modal
      title="Payment receipt"
      size="lg"
      onClose={onClose}
      footer={receipt && (
        <>
          <button className="btn btn-ghost no-print" onClick={onClose}>Close</button>
          <button className="btn no-print" onClick={() => window.print()}>
            <Icon name="printer" size={16} /> Print / Save as PDF
          </button>
        </>
      )}
    >
      <ErrorNote message={error} />
      {!receipt && !error && <Loading label="Preparing receipt…" />}
      {receipt && <Receipt r={receipt} />}
    </Modal>
  );
}
