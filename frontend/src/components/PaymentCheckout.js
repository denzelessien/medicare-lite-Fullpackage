import React, { useEffect, useState } from 'react';
import Modal from './Modal';
import Icon from './Icons';
import { ErrorNote } from './ui';
import { getBanks, payPayment } from '../api/client';
import { fmtMoney } from '../utils';

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function formatCard(v) {
  return v.replace(/\D/g, '').slice(0, 19).replace(/(.{4})/g, '$1 ').trim();
}
function formatExpiry(v) {
  const d = v.replace(/\D/g, '').slice(0, 4);
  return d.length > 2 ? `${d.slice(0, 2)}/${d.slice(2)}` : d;
}

/**
 * A demo checkout. It looks and behaves like a real one (validation, processing state,
 * success / failure) but talks to the backend's fake "DemoPay" gateway - no real money moves.
 */
export default function PaymentCheckout({ payment, defaultName = '', defaultEmail = '', onClose, onPaid, onViewReceipt }) {
  const [method, setMethod] = useState('CARD');
  const [banks, setBanks] = useState([]);
  const [phase, setPhase] = useState('form'); // form | processing | success
  const [error, setError] = useState('');
  const [paid, setPaid] = useState(null);

  const [card, setCard] = useState({ number: '', holder: defaultName, expiry: '', cvv: '' });
  const [bank, setBank] = useState({ bankName: '', accountNumber: '', accountName: defaultName });

  useEffect(() => { getBanks().then(setBanks).catch(() => {}); }, []);

  async function submit(e) {
    e.preventDefault();
    setError('');
    setPhase('processing');
    const body = method === 'CARD'
      ? { method, cardNumber: card.number, cardHolder: card.holder, expiry: card.expiry, cvv: card.cvv, payerEmail: defaultEmail }
      : { method, bankName: bank.bankName, accountNumber: bank.accountNumber, accountName: bank.accountName, payerEmail: defaultEmail };
    try {
      // The minimum delay makes the "processing" state visible, like a real gateway round-trip.
      const [result] = await Promise.all([payPayment(payment.id, body), sleep(1800)]);
      setPaid(result);
      setPhase('success');
      onPaid && onPaid(result);
    } catch (err) {
      setError(err.message);
      setPhase('form');
    }
  }

  const title = phase === 'success' ? 'Payment successful' : 'Secure checkout';

  return (
    <Modal title={title} subtitle={phase === 'form' ? 'Demo payment — no real money is charged' : undefined} onClose={phase === 'processing' ? () => {} : onClose} size="md">
      {phase === 'processing' && (
        <div className="processing">
          <span className="spinner" />
          <div className="strong">Processing your payment…</div>
          <div className="muted small">Please don't close this window.</div>
        </div>
      )}

      {phase === 'success' && paid && (
        <div className="success-burst">
          <div className="success-check"><Icon name="check" size={36} strokeWidth={3} /></div>
          <h2>{fmtMoney(paid.amount)} paid</h2>
          <p className="muted">Receipt {paid.receiptNumber} · Ref {paid.transactionRef}</p>
          <div className="actions" style={{ justifyContent: 'center', marginTop: 18 }}>
            <button className="btn" onClick={() => onViewReceipt && onViewReceipt(paid)}><Icon name="receipt" size={16} /> View receipt</button>
            <button className="btn btn-ghost" onClick={onClose}>Done</button>
          </div>
        </div>
      )}

      {phase === 'form' && (
        <form onSubmit={submit}>
          <div className="pay-summary">
            <div>
              <div className="strong">{payment.description || 'Hospital charge'}</div>
              <div className="muted small">Ref {payment.reference}</div>
            </div>
            <div className="amt">{fmtMoney(payment.amount)}</div>
          </div>

          <div className="method-tabs">
            <button type="button" className={`method ${method === 'CARD' ? 'active' : ''}`} onClick={() => setMethod('CARD')}>
              <Icon name="card" /> Card
            </button>
            <button type="button" className={`method ${method === 'BANK_TRANSFER' ? 'active' : ''}`} onClick={() => setMethod('BANK_TRANSFER')}>
              <Icon name="building" /> Bank transfer
            </button>
          </div>

          <div className="demo-note">
            {method === 'CARD'
              ? <>Test card: <b>4242 4242 4242 4242</b>, any future expiry, any CVV. Use <b>4000 0000 0000 0002</b> to see a declined payment.</>
              : <>Use any 10-digit account number. Numbers ending in <b>0000</b> simulate insufficient funds.</>}
          </div>

          {payment.failureReason && !error && <ErrorNote message={`Last attempt failed: ${payment.failureReason}`} />}
          <ErrorNote message={error} />

          {method === 'CARD' ? (
            <>
              <div className="field">
                <label>Card number</label>
                <input inputMode="numeric" placeholder="1234 5678 9012 3456" value={card.number}
                  onChange={(e) => setCard({ ...card, number: formatCard(e.target.value) })} required />
              </div>
              <div className="field">
                <label>Name on card</label>
                <input value={card.holder} onChange={(e) => setCard({ ...card, holder: e.target.value })} required />
              </div>
              <div className="form-grid">
                <div className="field">
                  <label>Expiry (MM/YY)</label>
                  <input placeholder="MM/YY" inputMode="numeric" value={card.expiry}
                    onChange={(e) => setCard({ ...card, expiry: formatExpiry(e.target.value) })} required />
                </div>
                <div className="field">
                  <label>CVV</label>
                  <input type="password" inputMode="numeric" placeholder="•••" maxLength={4} value={card.cvv}
                    onChange={(e) => setCard({ ...card, cvv: e.target.value.replace(/\D/g, '') })} required />
                </div>
              </div>
            </>
          ) : (
            <>
              <div className="field">
                <label>Bank</label>
                <select value={bank.bankName} onChange={(e) => setBank({ ...bank, bankName: e.target.value })} required>
                  <option value="">Select your bank</option>
                  {banks.map((b) => <option key={b} value={b}>{b}</option>)}
                </select>
              </div>
              <div className="form-grid">
                <div className="field">
                  <label>Account number</label>
                  <input inputMode="numeric" placeholder="10 digits" maxLength={10} value={bank.accountNumber}
                    onChange={(e) => setBank({ ...bank, accountNumber: e.target.value.replace(/\D/g, '') })} required />
                </div>
                <div className="field">
                  <label>Account name</label>
                  <input value={bank.accountName} onChange={(e) => setBank({ ...bank, accountName: e.target.value })} required />
                </div>
              </div>
            </>
          )}

          <button className="btn btn-block" type="submit" style={{ marginTop: 6 }}>
            <Icon name="lock" size={16} /> Pay {fmtMoney(payment.amount)}
          </button>
          <div className="muted small" style={{ textAlign: 'center', marginTop: 10 }}>
            Card numbers and CVVs are never stored. Only the last 4 digits are kept for your receipt.
          </div>
        </form>
      )}
    </Modal>
  );
}
