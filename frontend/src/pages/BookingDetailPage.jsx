import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { bookings as bookingsApi, flights as flightsApi, seats as seatsApi } from '../services/api';
import { useBooking } from '../context/BookingContext';
import './BookingDetailPage.css';

const fmt = (dt) => dt ? new Date(dt).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: true }) : '—';
const duration = (dep, arr) => {
  if (!dep || !arr) return '—';
  const mins = Math.round((new Date(arr) - new Date(dep)) / 60000);
  return `${Math.floor(mins / 60)}h ${mins % 60}m`;
};

const statusLabel = (s) => {
  if (s === 'PENDING_PAYMENT') return 'Pending Payment';
  if (s === 'CONFIRMED')       return 'Confirmed';
  if (s === 'CANCELLED')       return 'Cancelled';
  return s;
};
const statusClass = (s) => {
  if (!s) return '';
  const v = s.toLowerCase();
  if (v === 'confirmed')       return 'badge-confirmed';
  if (v === 'pending_payment') return 'badge-pending';
  if (v === 'cancelled')       return 'badge-cancelled';
  return '';
};

const STEPS = ['Seats selected', 'Passenger details', 'Payment'];
const stepIndex = (step) => step === 'PASSENGERS_SAVED' ? 1 : 0;

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

// Aircraft layout constants — must match SeatPage and backend buildSeats
const BUS_LEFT  = ['A', 'B'];
const BUS_RIGHT = ['C', 'D'];
const ECO_LEFT  = ['A', 'B', 'C'];
const ECO_RIGHT = ['D', 'E', 'F'];
const EXIT_ROWS = new Set([4, 18]);

function ExitRow() {
  return (
    <div className="exit-row">
      <div className="exit-marker exit-left"><span className="exit-icon">🚪</span><span className="exit-label">EXIT</span></div>
      <div className="exit-line" />
      <div className="exit-marker exit-right"><span className="exit-label">EXIT</span><span className="exit-icon">🚪</span></div>
    </div>
  );
}

export default function BookingDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { setSelectedFlight, setSelectedSeats, setBooking: setCtxBooking } = useBooking();

  const [booking, setBooking] = useState(null);
  const [flight, setFlight] = useState(null);
  const [seatMap, setSeatMap] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [cancelling, setCancelling] = useState(false);
  const [refundShown, setRefundShown] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const bRes = await bookingsApi.getOne(id);
        const b = bRes.data;
        setBooking(b);
        const fRes = await flightsApi.getById(b.flightId);
        setFlight(fRes.data);
        if (b.status === 'CONFIRMED') {
          const sRes = await seatsApi.getByFlight(b.flightId);
          setSeatMap(sRes.data || []);
        }
      } catch {
        setError('Could not load booking details.');
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  const resumeBooking = () => {
    setSelectedFlight(flight);
    setSelectedSeats([]);
    setCtxBooking(booking);
    navigate(booking.bookingStep === 'PASSENGERS_SAVED' ? '/payment' : '/booking/passengers');
  };

  const handleCancel = async () => {
    if (!window.confirm('Are you sure you want to cancel this booking?')) return;
    setCancelling(true);
    try {
      const res = await bookingsApi.cancel(id);
      setBooking(res.data);
      setRefundShown(true);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not cancel booking.');
    } finally {
      setCancelling(false);
    }
  };

  if (loading) return <div className="page-container"><div className="loading-center"><span className="spinner" /> Loading</div></div>;
  if (error)   return <div className="page-container"><div className="alert alert-error">{error}</div></div>;
  if (!booking || !flight) return null;

  const isPending   = booking.status === 'PENDING_PAYMENT';
  const isConfirmed = booking.status === 'CONFIRMED';
  const isCancelled = booking.status === 'CANCELLED';

  // Seat map helpers
  const bookedSeatNumbers = new Set(booking.seats || []);
  const seatByNum = Object.fromEntries(seatMap.map(s => [s.seatNumber, s]));

  const busRows = [...new Set(seatMap.filter(s => s.seatClass === 'BUSINESS').map(s => parseInt(s.seatNumber)))].sort((a, b) => a - b);
  const ecoRows = [...new Set(seatMap.filter(s => s.seatClass === 'ECONOMY').map(s => parseInt(s.seatNumber)))].sort((a, b) => a - b);

  const renderSeat = (seatNum) => {
    const seat = seatByNum[seatNum];
    if (!seat) return <div key={seatNum} className="seat-empty" />;
    const isYours = bookedSeatNumbers.has(seat.seatNumber);
    const isTaken = (seat.status === 'BOOKED' || seat.status === 'LOCKED') && !isYours;
    return (
      <div
        key={seat.id}
        className={`seat-btn ${isYours ? 'seat-selected' : isTaken ? 'seat-booked' : 'seat-available'}`}
        title={`${seat.seatNumber}${isYours ? ' — Your seat' : isTaken ? ' — Taken' : ' — Available'}`}
      >
        {seat.seatNumber}
      </div>
    );
  };

  const renderCabinRow = (row, leftCols, rightCols) => (
    <div key={row}>
      {EXIT_ROWS.has(row) && <ExitRow />}
      <div className="cabin-row">
        <div className="col-group">{leftCols.map(col => renderSeat(`${row}${col}`))}</div>
        <div className="aisle-gap"><span className="row-num">{row}</span></div>
        <div className="col-group">{rightCols.map(col => renderSeat(`${row}${col}`))}</div>
      </div>
    </div>
  );

  return (
    <div className="page-container" style={{ paddingBottom: 60 }}>
      <button className="back-link" onClick={() => navigate('/my-bookings')}>← Back to my bookings</button>

      {/* Header */}
      <div className="bd-header">
        <div>
          <h1 className="page-title" style={{ marginBottom: 4 }}>{booking.bookingReference}</h1>
          <span className="booking-flight">{booking.flightNumber}</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span className={`badge ${statusClass(booking.status)}`}>{statusLabel(booking.status)}</span>
          {!isCancelled && (
            <button className="btn btn-danger btn-sm" onClick={handleCancel} disabled={cancelling}>
              {cancelling ? <><span className="spinner" /> Cancelling</> : 'Cancel booking'}
            </button>
          )}
        </div>
      </div>

      {refundShown && (
        <div className="alert alert-refund">
          {booking.paymentStatus === 'SUCCESS'
            ? 'Booking cancelled. Refund initiated — your amount will be credited within 5–7 business days.'
            : 'Booking cancelled successfully. No payment was made, so no refund is required.'}
        </div>
      )}

      {/* Flight info card */}
      <div className="card bd-flight-card">
        {/* Flight number + airline row */}
        <div className="bd-flight-meta-top">
          <div className="bd-fn-block">
            <span className="bd-fn-label">Flight</span>
            <span className="bd-fn-value">{flight.flightNumber}</span>
          </div>
          {flight.airline && (
            <div className="bd-fn-block">
              <span className="bd-fn-label">Airline</span>
              <span className="bd-fn-value">{flight.airline}</span>
            </div>
          )}
          <div className="bd-fn-block">
            <span className="bd-fn-label">Duration</span>
            <span className="bd-fn-value">{duration(flight.departureTime, flight.arrivalTime)}</span>
          </div>
          <div className="bd-fn-block">
            <span className="bd-fn-label">Seats</span>
            <span className="bd-fn-value">{(booking.seats || []).join(', ') || '—'}</span>
          </div>
        </div>

        <hr className="divider" style={{ margin: '16px 0' }} />

        {/* Route */}
        <div className="bd-route">
          <div className="bd-airport">
            <span className="bd-iata">{flight.source}</span>
            <span className="bd-airport-name">{flight.sourceAirport}</span>
            <span className="bd-time">{fmt(flight.departureTime)}</span>
          </div>
          <div className="bd-route-mid">
            <div className="bd-route-line"><span className="bd-plane">✈</span></div>
          </div>
          <div className="bd-airport bd-airport-right">
            <span className="bd-iata">{flight.destination}</span>
            <span className="bd-airport-name">{flight.destinationAirport}</span>
            <span className="bd-time">{fmt(flight.arrivalTime)}</span>
          </div>
        </div>

        <hr className="divider" style={{ margin: '16px 0' }} />

        {/* Booking meta */}
        <div className="bd-meta">
          <div className="bd-meta-item"><span className="info-label">Booked on</span><span className="info-value">{fmt(booking.bookedAt)}</span></div>
          <div className="bd-meta-item"><span className="info-label">Amount</span><span className="info-value">INR {Number(booking.totalAmount).toLocaleString('en-IN')}</span></div>
          <div className="bd-meta-item"><span className="info-label">Payment</span><span className="info-value">{booking.paymentStatus}</span></div>
          {isConfirmed && (
            <button className="btn btn-outline btn-sm" style={{ marginLeft: 'auto' }} onClick={() => navigate(`/track?flight=${flight.flightNumber}`)}>
              Track flight ✈
            </button>
          )}
        </div>
      </div>

      {/* CONFIRMED — aircraft cabin seat map */}
      {isConfirmed && seatMap.length > 0 && (
        <div className="card bd-seats-card">
          <h2 className="bd-section-title">Seat map</h2>
          <div className="seat-legend" style={{ marginBottom: 16, flexWrap: 'wrap' }}>
            <span className="legend-item"><span className="seat-dot booked" /> Taken</span>
            <span className="legend-item"><span className="seat-dot selected" /> Your seat</span>
            <span className="legend-item"><span className="seat-dot available" /> Available</span>
            <span className="legend-item"><span className="exit-legend-icon">🚪</span> Emergency exit</span>
          </div>

          {/* Business */}
          {busRows.length > 0 && (
            <div className="seat-section">
              <div className="seat-class-label">Business class · 2+2</div>
              <div className="cabin-row cabin-header">
                <div className="col-group">{BUS_LEFT.map(c => <div key={c} className="col-header">{c}</div>)}</div>
                <div className="aisle-gap" />
                <div className="col-group">{BUS_RIGHT.map(c => <div key={c} className="col-header">{c}</div>)}</div>
              </div>
              {busRows.map(row => renderCabinRow(row, BUS_LEFT, BUS_RIGHT))}
            </div>
          )}

          {busRows.length > 0 && ecoRows.length > 0 && (
            <div className="cabin-divider">✈ Economy cabin</div>
          )}

          {/* Economy */}
          {ecoRows.length > 0 && (
            <div className="seat-section">
              <div className="seat-class-label">Economy class · 3+3</div>
              <div className="cabin-row cabin-header">
                <div className="col-group">{ECO_LEFT.map(c => <div key={c} className="col-header">{c}</div>)}</div>
                <div className="aisle-gap" />
                <div className="col-group">{ECO_RIGHT.map(c => <div key={c} className="col-header">{c}</div>)}</div>
              </div>
              {ecoRows.map(row => renderCabinRow(row, ECO_LEFT, ECO_RIGHT))}
            </div>
          )}
        </div>
      )}

      {/* PENDING_PAYMENT — step tracker + resume */}
      {isPending && (
        <div className="card bd-pending-card">
          <h2 className="bd-section-title" style={{ marginBottom: 16 }}>Resume your booking</h2>
          <StepTracker bookingStep={booking.bookingStep} />
          <p className="bd-pending-msg" style={{ marginTop: 20 }}>
            {booking.bookingStep === 'PASSENGERS_SAVED'
              ? 'Passenger details saved. Complete payment to confirm your booking.'
              : 'Seats locked. Enter passenger details to continue.'}
          </p>
          <button className="btn btn-primary" style={{ marginTop: 4 }} onClick={resumeBooking}>
            {booking.bookingStep === 'PASSENGERS_SAVED' ? 'Go to payment →' : 'Enter passenger details →'}
          </button>
        </div>
      )}
    </div>
  );
}
