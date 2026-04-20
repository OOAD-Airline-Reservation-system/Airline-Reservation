import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { payments as paymentsApi } from '../services/api';
import { useBooking } from '../context/BookingContext';
import './PaymentPage.css';

/*
 * Payment flow (as per system architecture):
 *
 *   Frontend (this page)
 *       |  POST /api/payments  { bookingId, provider, paymentToken }
 *       v
 *   Backend -> PaymentService -> PaymentGatewayAdapter -> (Razorpay API when keys set)
 *       |
 *       v
 *   Backend updates Booking status -> returns PaymentResponse
 *       |
 *       v
 *   Frontend shows confirmation
 *
 * The frontend never calls Razorpay directly. It sends a simulated token to
 * the backend which handles verification. When real Razorpay keys are added
 * to application.yml, the backend will verify signatures server-side.
 */

export default function PaymentPage() {
  const navigate = useNavigate();
  const { booking, selectedFlight, selectedSeats, reset } = useBooking();
  const [method, setMethod] = useState('card');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [cardForm, setCardForm] = useState({ number: '', expiry: '', cvv: '', name: '' });
  const [upiId, setUpiId] = useState('');

  useEffect(() => {
    if (!booking) navigate('/');
  }, []);

  if (!booking || !selectedFlight) return null;

  const total = Number(booking.totalAmount) ||
    selectedSeats.length * Number(selectedFlight.basePrice);

  const fmtCard = (val) => val.replace(/\D/g, '').slice(0, 16).replace(/(.{4})/g, '$1 ').trim();
  const fmtExpiry = (val) => {
    const digits = val.replace(/\D/g, '').slice(0, 4);
    return digits.length > 2 ? digits.slice(0, 2) + '/' + digits.slice(2) : digits;
  };

  const submitPayment = async (token, provider) => {
    setError('');
    setLoading(true);
    try {
      await paymentsApi.process({ bookingId: booking.id, provider, paymentToken: token });
      reset();
      navigate('/booking/success', { state: { booking } });
    } catch (err) {
      setError(err.response?.data?.message || 'Payment failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleCardPay = (e) => {
    e.preventDefault();
    const digits = cardForm.number.replace(/\s/g, '');
    if (digits.length < 16) { setError('Enter a valid 16-digit card number.'); return; }
    if (!cardForm.expiry.includes('/')) { setError('Enter a valid expiry (MM/YY).'); return; }
    if (cardForm.cvv.length < 3) { setError('Enter a valid CVV.'); return; }
    if (!cardForm.name.trim()) { setError('Enter the cardholder name.'); return; }
    submitPayment('card_demo_' + Date.now(), 'CARD');
  };

  const handleUpiPay = (e) => {
    e.preventDefault();
    if (!upiId.includes('@')) { setError('Enter a valid UPI ID (e.g. name@upi)'); return; }
    submitPayment('upi_demo_' + Date.now(), 'UPI');
  };

  return (
    <div className="page-container" style={{ paddingTop: 40, paddingBottom: 60 }}>
      <div className="page-header" style={{ paddingTop: 0 }}>
        <h1 className="page-title">Complete payment</h1>
        <p className="page-subtitle">Booking reference: {booking.bookingReference}</p>
      </div>

      <div className="payment-layout">
        <div className="payment-main">
          <div className="card payment-card">
            <p className="payment-section-label">Payment method</p>
            <div className="method-tabs">
              {['card', 'upi'].map(m => (
                <button
                  key={m}
                  className={`method-tab ${method === m ? 'active' : ''}`}
                  onClick={() => { setMethod(m); setError(''); }}
                >
                  {m === 'card' ? 'Credit / Debit card' : 'UPI'}
                </button>
              ))}
            </div>

            <hr className="divider" />

            {method === 'card' && (
              <form onSubmit={handleCardPay} className="method-body">
                <p className="method-desc">
                  Your card details are transmitted to the backend which securely processes the payment.
                  Card numbers are never stored.
                </p>
                <div className="payment-form">
                  <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                    <label className="form-label">Card number</label>
                    <input
                      className="form-input"
                      placeholder="1234 5678 9012 3456"
                      value={cardForm.number}
                      onChange={e => setCardForm({ ...cardForm, number: fmtCard(e.target.value) })}
                      maxLength={19}
                    />
                  </div>
                  <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                    <label className="form-label">Cardholder name</label>
                    <input
                      className="form-input"
                      placeholder="Jane Doe"
                      value={cardForm.name}
                      onChange={e => setCardForm({ ...cardForm, name: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Expiry</label>
                    <input
                      className="form-input"
                      placeholder="MM/YY"
                      value={cardForm.expiry}
                      onChange={e => setCardForm({ ...cardForm, expiry: fmtExpiry(e.target.value) })}
                      maxLength={5}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">CVV</label>
                    <input
                      className="form-input"
                      placeholder="123"
                      value={cardForm.cvv}
                      onChange={e => setCardForm({ ...cardForm, cvv: e.target.value.replace(/\D/g, '').slice(0, 4) })}
                      maxLength={4}
                    />
                  </div>
                </div>
                {error && <div className="alert alert-error" style={{ marginTop: 16 }}>{error}</div>}
                <button
                  type="submit"
                  className="btn btn-primary btn-lg"
                  style={{ width: '100%', marginTop: 20 }}
                  disabled={loading}
                >
                  {loading ? <><span className="spinner" /> Processing</> : `Pay INR ${total.toLocaleString('en-IN')}`}
                </button>
              </form>
            )}

            {method === 'upi' && (
              <form onSubmit={handleUpiPay} className="method-body">
                <p className="method-desc">
                  Enter your UPI ID. The payment request is routed through the backend to the payment gateway.
                </p>
                <div className="form-group" style={{ marginTop: 20 }}>
                  <label className="form-label">UPI ID</label>
                  <input
                    className="form-input"
                    placeholder="yourname@upi"
                    value={upiId}
                    onChange={e => setUpiId(e.target.value)}
                    autoFocus
                  />
                </div>
                {error && <div className="alert alert-error" style={{ marginTop: 12 }}>{error}</div>}
                <button
                  type="submit"
                  className="btn btn-primary btn-lg"
                  style={{ width: '100%', marginTop: 20 }}
                  disabled={loading}
                >
                  {loading ? <><span className="spinner" /> Processing</> : `Pay INR ${total.toLocaleString('en-IN')}`}
                </button>
              </form>
            )}
          </div>

          <div className="security-note">
            Payments are routed through the backend. API keys and card data never reach the browser in production. The backend calls the payment gateway and updates booking status before returning a result.
          </div>
        </div>

        <div className="payment-sidebar card">
          <p className="sidebar-heading">Order summary</p>
          <hr className="divider" />
          <div className="summary-row">
            <span>Flight</span>
            <span>{booking.flightNumber}</span>
          </div>
          <div className="summary-row">
            <span>Seats</span>
            <span>{(booking.seats || []).join(', ') || selectedSeats.map(s => s.seatNumber).join(', ')}</span>
          </div>
          <div className="summary-row">
            <span>Status</span>
            <span><span className="badge badge-pending">{booking.status}</span></span>
          </div>
          <hr className="divider" />
          <div className="summary-row summary-total-row">
            <span>Total due</span>
            <span>INR {total.toLocaleString('en-IN')}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
