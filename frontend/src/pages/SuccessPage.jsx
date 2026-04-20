import { useLocation, useNavigate } from 'react-router-dom';
import './SuccessPage.css';

export default function SuccessPage() {
  const { state } = useLocation();
  const navigate = useNavigate();
  const booking = state?.booking;

  if (!booking) {
    navigate('/');
    return null;
  }

  return (
    <div className="page-container success-page">
      <div className="success-card card fade-up">
        <div className="success-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </div>

        <h1 className="success-title">Booking confirmed</h1>
        <p className="success-sub">Your seats are reserved. Check your email for a confirmation.</p>

        <div className="success-ref">
          <span className="ref-label">Booking reference</span>
          <span className="ref-value">{booking.bookingReference}</span>
        </div>

        <hr className="divider" />

        <div className="success-details">
          <div className="detail-row">
            <span className="detail-label">Flight</span>
            <span className="detail-value">{booking.flightNumber}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Seats</span>
            <span className="detail-value">{(booking.seats || []).join(', ')}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Amount paid</span>
            <span className="detail-value">INR {Number(booking.totalAmount).toLocaleString('en-IN')}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Status</span>
            <span><span className="badge badge-confirmed">Confirmed</span></span>
          </div>
        </div>

        <div className="success-actions">
          <button className="btn btn-primary" onClick={() => navigate('/my-bookings')}>
            View all bookings
          </button>
          <button className="btn btn-outline" onClick={() => navigate('/')}>
            Search more flights
          </button>
        </div>
      </div>
    </div>
  );
}
