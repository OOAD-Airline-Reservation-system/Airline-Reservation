import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { seats as seatsApi } from '../services/api';
import { useBooking } from '../context/BookingContext';
import './SeatPage.css';

const formatDate = (dt) => dt ? new Date(dt).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', hour12: true }) : '';

// Business: 2+2 → A B | C D
const BUS_LEFT  = ['A', 'B'];
const BUS_RIGHT = ['C', 'D'];
// Economy: 3+3 → A B C | D E F
const ECO_LEFT  = ['A', 'B', 'C'];
const ECO_RIGHT = ['D', 'E', 'F'];

// Emergency exit rows (after last business row, mid-economy)
const EXIT_ROWS = new Set([4, 18]);

const colLabel = (col, isEco) => {
  if (isEco) {
    if (col === 'A' || col === 'F') return 'W';
    if (col === 'C' || col === 'D') return 'A';
    return 'M';
  }
  if (col === 'A' || col === 'D') return 'W';
  return 'A';
};

function ExitRow({ cols }) {
  const half = Math.ceil(cols / 2);
  return (
    <div className="exit-row">
      <div className="exit-marker exit-left">
        <span className="exit-icon">🚪</span>
        <span className="exit-label">EXIT</span>
      </div>
      <div className="exit-line" />
      <div className="exit-marker exit-right">
        <span className="exit-label">EXIT</span>
        <span className="exit-icon">🚪</span>
      </div>
    </div>
  );
}

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
    if (seat.status === 'BOOKED' || seat.status === 'LOCKED') return;
    const already = selectedSeats.find(s => s.id === seat.id);
    setSelectedSeats(already
      ? selectedSeats.filter(s => s.id !== seat.id)
      : [...selectedSeats, seat]
    );
  };

  const total = selectedSeats.reduce((acc, s) => acc + Number(s.price || selectedFlight?.basePrice || 0), 0);

  const seatByNum = Object.fromEntries(seatMap.map(s => [s.seatNumber, s]));

  const busRows = [...new Set(
    seatMap.filter(s => s.seatClass === 'BUSINESS').map(s => parseInt(s.seatNumber))
  )].sort((a, b) => a - b);

  const ecoRows = [...new Set(
    seatMap.filter(s => s.seatClass === 'ECONOMY').map(s => parseInt(s.seatNumber))
  )].sort((a, b) => a - b);

  const renderSeat = (seatNum) => {
    const seat = seatByNum[seatNum];
    if (!seat) return <div key={seatNum} className="seat-empty" />;
    const isSelected = !!selectedSeats.find(s => s.id === seat.id);
    const isTaken    = seat.status === 'BOOKED' || seat.status === 'LOCKED';
    return (
      <button
        key={seat.id}
        className={`seat-btn ${isTaken ? 'seat-booked' : isSelected ? 'seat-selected' : 'seat-available'}`}
        onClick={() => toggleSeat(seat)}
        disabled={isTaken}
        title={`${seat.seatNumber} — ${isTaken ? 'Taken' : 'Available'} · INR ${Number(seat.price).toLocaleString('en-IN')}`}
      >
        {seat.seatNumber}
      </button>
    );
  };

  const renderCabinRow = (row, leftCols, rightCols) => (
    <div key={row}>
      {EXIT_ROWS.has(row) && <ExitRow cols={leftCols.length + rightCols.length} />}
      <div className="cabin-row">
        <div className="col-group">
          {leftCols.map(col => renderSeat(`${row}${col}`))}
        </div>
        <div className="aisle-gap"><span className="row-num">{row}</span></div>
        <div className="col-group">
          {rightCols.map(col => renderSeat(`${row}${col}`))}
        </div>
      </div>
    </div>
  );

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
                <span className="legend-item"><span className="seat-dot booked" /> Taken</span>
                <span className="legend-item"><span className="seat-dot selected" /> Selected</span>
                <span className="legend-item seat-legend-type"><span className="type-tag">W</span> Window</span>
                <span className="legend-item seat-legend-type"><span className="type-tag">M</span> Middle</span>
                <span className="legend-item seat-legend-type"><span className="type-tag">A</span> Aisle</span>
                <span className="legend-item"><span className="exit-legend-icon">🚪</span> Emergency exit</span>
              </div>

              {/* BUSINESS CLASS */}
              {busRows.length > 0 && (
                <div className="seat-section">
                  <div className="seat-class-label">Business class · 2+2</div>
                  <div className="cabin-row cabin-header">
                    <div className="col-group">
                      {BUS_LEFT.map(c => <div key={c} className="col-header">{c}<span className="col-type">{colLabel(c, false)}</span></div>)}
                    </div>
                    <div className="aisle-gap" />
                    <div className="col-group">
                      {BUS_RIGHT.map(c => <div key={c} className="col-header">{c}<span className="col-type">{colLabel(c, false)}</span></div>)}
                    </div>
                  </div>
                  {busRows.map(row => renderCabinRow(row, BUS_LEFT, BUS_RIGHT))}
                </div>
              )}

              {busRows.length > 0 && ecoRows.length > 0 && (
                <div className="cabin-divider">✈ Economy cabin</div>
              )}

              {/* ECONOMY CLASS */}
              {ecoRows.length > 0 && (
                <div className="seat-section">
                  <div className="seat-class-label">Economy class · 3+3</div>
                  <div className="cabin-row cabin-header">
                    <div className="col-group">
                      {ECO_LEFT.map(c => <div key={c} className="col-header">{c}<span className="col-type">{colLabel(c, true)}</span></div>)}
                    </div>
                    <div className="aisle-gap" />
                    <div className="col-group">
                      {ECO_RIGHT.map(c => <div key={c} className="col-header">{c}<span className="col-type">{colLabel(c, true)}</span></div>)}
                    </div>
                  </div>
                  {ecoRows.map(row => renderCabinRow(row, ECO_LEFT, ECO_RIGHT))}
                </div>
              )}
            </>
          )}
        </div>

        {/* Sidebar */}
        <div className="seat-sidebar card">
          <h2 className="sidebar-title">Booking summary</h2>
          <hr className="divider" />
          <div className="summary-row"><span>Flight</span><span>{selectedFlight.flightNumber}</span></div>
          <div className="summary-row"><span>Departure</span><span>{formatDate(selectedFlight.departureTime)}</span></div>
          <div className="summary-row"><span>Route</span><span>{selectedFlight.source} → {selectedFlight.destination}</span></div>
          <hr className="divider" />
          {selectedSeats.length > 0 ? (
            <>
              <div className="summary-row"><span>Seats</span><span>{selectedSeats.map(s => s.seatNumber).join(', ')}</span></div>
              <div className="summary-row summary-total"><span>Total</span><span>INR {total.toLocaleString('en-IN')}</span></div>
              <button className="btn btn-primary" style={{ width: '100%', marginTop: 20 }} onClick={() => navigate('/booking/confirm')}>
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
