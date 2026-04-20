import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bookings as bookingsApi } from '../services/api';
import { useBooking } from '../context/BookingContext';
import './ConfirmPage.css';

const formatDate = (dt) => dt ? new Date(dt).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: true }) : '';

const getDuration = (dep, arr) => {
  const diff = Math.abs(new Date(arr) - new Date(dep));
  const h = Math.floor(diff / 3600000);
  const m = Math.floor((diff % 3600000) / 60000);
  return `${h}h ${m}m`;
};

export default function ConfirmPage() {
  const navigate = useNavigate();
  const { selectedFlight, selectedSeats, setBooking } = useBooking();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!selectedFlight || selectedSeats.length === 0) {
    navigate('/');
    return null;
  }

  const seatIds = selectedSeats.map(s => s.id);
  const total = selectedSeats.length * Number(selectedFlight.basePrice);

  const handleConfirm = async () => {
    setError('');
    setLoading(true);
    try {
      const res = await bookingsApi.create({ flightId: selectedFlight.id, seatIds });
      setBooking(res.data);
      navigate('/booking/passengers');
    } catch (err) {
      setError(err.response?.data?.message || 'Booking failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container" style={{ paddingTop: 40, paddingBottom: 60 }}>
      <div className="page-header" style={{ paddingTop: 0 }}>
        <h1 className="page-title">Review your booking</h1>
        <p className="page-subtitle">Confirm the details below before proceeding to payment.</p>
      </div>

      <div className="confirm-layout">
        <div className="confirm-main">
          <div className="card confirm-card">
            <div className="confirm-section">
              <p className="confirm-section-label">Flight details</p>
              <div className="confirm-flight-row">
                <div>
                  <p className="confirm-flight-time">{formatDate(selectedFlight.departureTime)}</p>
                  <p className="confirm-flight-city">{selectedFlight.source}</p>
                </div>
                <div className="confirm-flight-mid">
                  <span className="confirm-duration">{getDuration(selectedFlight.departureTime, selectedFlight.arrivalTime)}</span>
                  <div className="confirm-line" />
                </div>
                <div style={{ textAlign: 'right' }}>
                  <p className="confirm-flight-time">{formatDate(selectedFlight.arrivalTime)}</p>
                  <p className="confirm-flight-city">{selectedFlight.destination}</p>
                </div>
              </div>
              <p className="confirm-fn">{selectedFlight.flightNumber}</p>
            </div>

            <hr className="divider" />

            <div className="confirm-section">
              <p className="confirm-section-label">Selected seats</p>
              <div className="seats-chips">
                {selectedSeats.map(s => (
                  <span key={s.id} className="seat-chip">
                    {s.seatNumber}
                    {s.seatClass && <span className="seat-chip-class">{s.seatClass.charAt(0) + s.seatClass.slice(1).toLowerCase()}</span>}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="confirm-sidebar card">
          <p className="sidebar-title" style={{ fontFamily: 'var(--font-display)', fontSize: '1.1rem', fontWeight: 400, letterSpacing: '-0.01em' }}>Price breakdown</p>
          <hr className="divider" />
          <div className="price-line">
            <span>Base fare × {selectedSeats.length}</span>
            <span>INR {Number(selectedFlight.basePrice).toLocaleString('en-IN')}</span>
          </div>
          <div className="price-line">
            <span>Taxes & fees</span>
            <span>Included</span>
          </div>
          <hr className="divider" />
          <div className="price-line price-line-total">
            <span>Total</span>
            <span>INR {total.toLocaleString('en-IN')}</span>
          </div>

          {error && <div className="alert alert-error" style={{ marginTop: 16, marginBottom: 0 }}>{error}</div>}

          <button
            className="btn btn-primary"
            style={{ width: '100%', marginTop: 24 }}
            onClick={handleConfirm}
            disabled={loading}
          >
            {loading ? <><span className="spinner" /> Creating booking</> : 'Confirm and pay'}
          </button>

          <button
            className="btn btn-outline"
            style={{ width: '100%', marginTop: 10 }}
            onClick={() => navigate(-1)}
          >
            Back to seats
          </button>
        </div>
      </div>
    </div>
  );
}
