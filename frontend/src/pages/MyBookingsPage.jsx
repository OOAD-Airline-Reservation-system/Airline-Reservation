import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { bookings as bookingsApi } from '../services/api';
import './MyBookingsPage.css';

const formatDate = (dt) => dt ? new Date(dt).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: true }) : '—';

const statusClass = (status) => {
  if (!status) return '';
  const s = status.toLowerCase();
  if (s === 'confirmed') return 'badge-confirmed';
  if (s === 'pending_payment') return 'badge-pending';
  if (s === 'cancelled') return 'badge-cancelled';
  return '';
};

const paymentClass = (status) => {
  if (!status) return '';
  const s = status.toLowerCase();
  if (s === 'completed' || s === 'paid') return 'badge-confirmed';
  if (s === 'pending') return 'badge-pending';
  return '';
};

const STEPS = ['Seats selected', 'Passenger details', 'Payment'];
const stepIndex = (bookingStep) => bookingStep === 'PASSENGERS_SAVED' ? 1 : 0;

function StepTracker({ bookingStep }) {
  const current = stepIndex(bookingStep);
  return (
    <div className="step-tracker">
      {STEPS.map((label, i) => (
        <div key={i} className={`step-item ${i < current ? 'step-done' : i === current ? 'step-current' : 'step-todo'}`}>
          <div className="step-dot">{i < current ? '✓' : i + 1}</div>
          <span className="step-label">{label}</span>
          {i < STEPS.length - 1 && <div className="step-line" />}
        </div>
      ))}
    </div>
  );
}

export default function MyBookingsPage() {
  const navigate = useNavigate();
  const [myBookings, setMyBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    (async () => {
      try {
        const res = await bookingsApi.getMine();
        setMyBookings(res.data || []);
      } catch {
        setError('Could not load your bookings.');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const pending   = myBookings.filter(b => b.status === 'PENDING_PAYMENT');
  const completed = myBookings.filter(b => b.status === 'CONFIRMED' || b.status === 'CANCELLED');

  return (
    <div className="page-container" style={{ paddingBottom: 60 }}>
      <div className="page-header">
        <h1 className="page-title">My bookings</h1>
        <p className="page-subtitle">All your flight reservations in one place.</p>
      </div>

      {loading && <div className="loading-center"><span className="spinner" /> Loading bookings</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {!loading && !error && myBookings.length === 0 && (
        <div className="empty-state">
          <p className="empty-title">No bookings yet</p>
          <p className="empty-desc">Once you book a flight, it will appear here.</p>
          <button className="btn btn-primary" style={{ marginTop: 20 }} onClick={() => navigate('/')}>
            Search flights
          </button>
        </div>
      )}

      {/* In Progress */}
      {!loading && pending.length > 0 && (
        <>
          <p className="section-label">In progress</p>
          <div className="bookings-list" style={{ marginBottom: 32 }}>
            {pending.map((b, i) => (
              <div
                key={b.id}
                className="booking-row card fade-up"
                style={{ animationDelay: `${i * 0.04}s`, cursor: 'pointer', flexDirection: 'column', alignItems: 'stretch', gap: 16 }}
                onClick={() => navigate(`/my-bookings/${b.id}`)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div className="booking-ref-block">
                    <span className="booking-ref">{b.bookingReference}</span>
                    <span className="booking-flight">{b.flightNumber}</span>
                  </div>
                  <span className={`badge ${statusClass(b.status)}`}>Pending Payment</span>
                </div>
                <StepTracker bookingStep={b.bookingStep} />
                <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                  <button
                    className="btn btn-primary btn-sm"
                    onClick={e => { e.stopPropagation(); navigate(`/my-bookings/${b.id}`); }}
                  >
                    {b.bookingStep === 'PASSENGERS_SAVED' ? 'Go to payment →' : 'Complete booking →'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {/* Confirmed / Cancelled */}
      {!loading && completed.length > 0 && (
        <>
          {pending.length > 0 && <p className="section-label">Completed</p>}
          <div className="bookings-list">
            {completed.map((b, i) => (
              <div
                key={b.id}
                className="booking-row card fade-up"
                style={{ animationDelay: `${i * 0.04}s`, cursor: 'pointer' }}
                onClick={() => navigate(`/my-bookings/${b.id}`)}
              >
                <div className="booking-row-main">
                  <div className="booking-ref-block">
                    <span className="booking-ref">{b.bookingReference}</span>
                    <span className="booking-flight">{b.flightNumber}</span>
                  </div>
                  <div className="booking-info-block">
                    <div className="booking-info-item">
                      <span className="info-label">Booked on</span>
                      <span className="info-value">{formatDate(b.bookedAt)}</span>
                    </div>
                    <div className="booking-info-item">
                      <span className="info-label">Seats</span>
                      <span className="info-value">{(b.seats || []).join(', ') || '—'}</span>
                    </div>
                    <div className="booking-info-item">
                      <span className="info-label">Amount</span>
                      <span className="info-value">INR {Number(b.totalAmount).toLocaleString('en-IN')}</span>
                    </div>
                  </div>
                </div>
                <div className="booking-row-side">
                  <div className="booking-statuses">
                    <span className={`badge ${statusClass(b.status)}`}>
                      {b.status === 'PENDING_PAYMENT' ? 'Pending Payment' : b.status}
                    </span>
                  </div>
                  {b.status === 'CONFIRMED' && (
                    <button
                      className="btn btn-outline btn-sm"
                      onClick={e => { e.stopPropagation(); navigate(`/track?flight=${b.flightNumber}`); }}
                    >
                      Track flight
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
