import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { bookings as bookingsApi } from '../services/api';
import './MyBookingsPage.css';

const formatDate = (dt) => dt ? new Date(dt).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: true }) : '—';

const statusClass = (status) => {
  if (!status) return '';
  const s = status.toLowerCase();
  if (s === 'confirmed') return 'badge-confirmed';
  if (s === 'pending') return 'badge-pending';
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

      {!loading && myBookings.length > 0 && (
        <div className="bookings-list">
          {myBookings.map((b, i) => (
            <div key={b.id} className="booking-row card fade-up" style={{ animationDelay: `${i * 0.04}s` }}>
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
                  <span className={`badge ${statusClass(b.status)}`}>{b.status}</span>
                  <span className={`badge ${paymentClass(b.paymentStatus)}`}>{b.paymentStatus}</span>
                </div>
                <button
                  className="btn btn-outline btn-sm"
                  onClick={() => navigate(`/track?flight=${b.flightNumber}`)}
                >
                  Track flight
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
