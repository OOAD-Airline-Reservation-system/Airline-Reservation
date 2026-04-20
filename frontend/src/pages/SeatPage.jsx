import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { seats as seatsApi } from '../services/api';
import { useBooking } from '../context/BookingContext';
import './SeatPage.css';

const formatDate = (dt) => dt ? new Date(dt).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', hour12: true }) : '';

export default function SeatPage() {
  const navigate = useNavigate();
  const { selectedFlight, selectedSeats, setSelectedSeats } = useBooking();
  const [seatMap, setSeatMap] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!selectedFlight) { navigate('/'); return; }
    (async () => {
      try {
        const res = await seatsApi.getByFlight(selectedFlight.id);
        setSeatMap(res.data || []);
      } catch {
        setError('Could not load seat map.');
      } finally {
        setLoading(false);
      }
    })();
  }, [selectedFlight]);

  const toggleSeat = (seat) => {
    if (seat.status === 'BOOKED') return;
    const already = selectedSeats.find(s => s.id === seat.id);
    if (already) {
      setSelectedSeats(selectedSeats.filter(s => s.id !== seat.id));
    } else {
      setSelectedSeats([...selectedSeats, seat]);
    }
  };

  const total = selectedSeats.reduce((acc, s) => acc + (s.price || Number(selectedFlight?.basePrice) || 0), 0);

  // Group by class
  const byClass = seatMap.reduce((acc, s) => {
    const cls = s.seatClass || 'ECONOMY';
    if (!acc[cls]) acc[cls] = [];
    acc[cls].push(s);
    return acc;
  }, {});

  const classOrder = ['FIRST', 'BUSINESS', 'ECONOMY'];

  if (!selectedFlight) return null;

  return (
    <div className="page-container" style={{ paddingTop: 40, paddingBottom: 60 }}>
      <div className="seat-layout">
        <div className="seat-main">
          <div className="page-header" style={{ paddingTop: 0 }}>
            <h1 className="page-title">Select a seat</h1>
            <p className="page-subtitle">{selectedFlight.flightNumber} · {selectedFlight.source} to {selectedFlight.destination}</p>
          </div>

          {loading && <div className="loading-center"><span className="spinner" /> Loading seat map</div>}
          {error && <div className="alert alert-error">{error}</div>}

          {!loading && !error && (
            <>
              <div className="seat-legend">
                <span className="legend-item"><span className="seat-dot available" /> Available</span>
                <span className="legend-item"><span className="seat-dot booked" /> Booked</span>
                <span className="legend-item"><span className="seat-dot selected" /> Selected</span>
              </div>

              {classOrder.map(cls => byClass[cls] && (
                <div key={cls} className="seat-section">
                  <div className="seat-class-label">{cls.charAt(0) + cls.slice(1).toLowerCase()} class</div>
                  <div className="seat-grid">
                    {byClass[cls].map(seat => {
                      const isSelected = !!selectedSeats.find(s => s.id === seat.id);
                      const isBooked = seat.status === 'BOOKED';
                      return (
                        <button
                          key={seat.id}
                          className={`seat-btn ${isBooked ? 'seat-booked' : isSelected ? 'seat-selected' : 'seat-available'}`}
                          onClick={() => toggleSeat(seat)}
                          disabled={isBooked}
                          title={`${seat.seatNumber} — ${isBooked ? 'Taken' : 'Available'}`}
                        >
                          {seat.seatNumber}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))}
            </>
          )}
        </div>

        <div className="seat-sidebar card">
          <h2 className="sidebar-title">Booking summary</h2>
          <hr className="divider" />
          <div className="summary-row">
            <span>Flight</span>
            <span>{selectedFlight.flightNumber}</span>
          </div>
          <div className="summary-row">
            <span>Departure</span>
            <span>{formatDate(selectedFlight.departureTime)}</span>
          </div>
          <div className="summary-row">
            <span>Route</span>
            <span>{selectedFlight.source} → {selectedFlight.destination}</span>
          </div>
          <hr className="divider" />

          {selectedSeats.length > 0 ? (
            <>
              <div className="summary-row">
                <span>Seats</span>
                <span>{selectedSeats.map(s => s.seatNumber).join(', ')}</span>
              </div>
              <div className="summary-row summary-total">
                <span>Total</span>
                <span>INR {total.toLocaleString('en-IN')}</span>
              </div>
              <button
                className="btn btn-primary"
                style={{ width: '100%', marginTop: 20 }}
                onClick={() => navigate('/booking/confirm')}
              >
                Continue to booking
              </button>
            </>
          ) : (
            <p className="sidebar-empty">No seat selected yet</p>
          )}
        </div>
      </div>
    </div>
  );
}
