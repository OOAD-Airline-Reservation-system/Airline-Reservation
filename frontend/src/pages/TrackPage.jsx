import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { tracking } from '../services/api';
import './TrackPage.css';

const STATUS_CONFIG = {
  SCHEDULED:      { color: '#2563eb', bg: '#eff6ff', label: 'Scheduled' },
  CHECK_IN_OPEN:  { color: '#7c3aed', bg: '#f5f3ff', label: 'Check-in Open' },
  BOARDING:       { color: '#d97706', bg: '#fffbeb', label: 'Boarding' },
  IN_FLIGHT:      { color: '#059669', bg: '#ecfdf5', label: 'In Flight' },
  LANDED:         { color: '#16a34a', bg: '#f0fdf4', label: 'Landed' },
  ARRIVED:        { color: '#16a34a', bg: '#f0fdf4', label: 'Arrived' },
  ACTIVE:         { color: '#059669', bg: '#ecfdf5', label: 'Active' },
  DEPARTED:       { color: '#059669', bg: '#ecfdf5', label: 'Departed' },
  CANCELLED:      { color: '#dc2626', bg: '#fef2f2', label: 'Cancelled' },
  DELAYED:        { color: '#d97706', bg: '#fffbeb', label: 'Delayed' },
  NOT_FOUND:      { color: '#6b7280', bg: '#f9fafb', label: 'Not Found' },
  NOT_IN_SYSTEM:  { color: '#6b7280', bg: '#f9fafb', label: 'Not Found' },
  UNKNOWN:        { color: '#6b7280', bg: '#f9fafb', label: 'Unknown' },
};

const getStatusConfig = (status) =>
  STATUS_CONFIG[status?.toUpperCase()] || { color: '#6b7280', bg: '#f9fafb', label: status || 'Unknown' };

export default function TrackPage() {
  const location = useLocation();
  const prefill = new URLSearchParams(location.search).get('flight') || '';

  const [flightNumber, setFlightNumber] = useState(prefill);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => { if (prefill) doTrack(prefill); }, []);

  const doTrack = async (num) => {
    const query = num.trim().toUpperCase();
    if (!query) return;
    setError('');
    setResult(null);
    setLoading(true);
    try {
      const res = await tracking.track(query);
      setResult(res.data);
    } catch {
      setError('Tracking service unavailable. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e) => { e.preventDefault(); doTrack(flightNumber); };

  const cfg = result ? getStatusConfig(result.status) : null;

  return (
    <div className="page-container" style={{ paddingBottom: 60 }}>
      <div className="page-header">
        <h1 className="page-title">Track a flight</h1>
        <p className="page-subtitle">
          Enter any IATA flight number. Live data via Aviationstack, schedule data from our system.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="track-form card">
        <div className="track-input-group">
          <div className="form-group" style={{ flex: 1 }}>
            <label className="form-label">Flight number</label>
            <input
              className="form-input"
              placeholder="e.g. AI860, EK512, 6E2134"
              value={flightNumber}
              onChange={e => setFlightNumber(e.target.value.toUpperCase())}
              required
              autoFocus
            />
          </div>
          <button type="submit" className="btn btn-primary" style={{ marginTop: 22 }} disabled={loading}>
            {loading ? <span className="spinner" /> : 'Track'}
          </button>
        </div>
        <p className="track-hint">
          Try flights you've booked: AI860, 6E2134, EK512, BA142, SQ423
        </p>
      </form>

      {error && <div className="alert alert-error" style={{ marginTop: 20 }}>{error}</div>}

      {!loading && result && (
        <div className="track-results fade-up">
          <div className="track-main card">

            {/* Header */}
            <div className="track-header">
              <div>
                <p className="track-fn">{result.flightNumber}</p>
                {result.airline && <p style={{ color: 'var(--ink-muted)', fontSize: '0.9rem', marginTop: 2 }}>{result.airline}</p>}
                <p className="track-source-label">
                  {result.dataSource === 'LIVE' ? '🟢 Live data · Aviationstack' : '🔵 Schedule data · Our system'}
                </p>
              </div>
              <span style={{
                padding: '6px 16px', borderRadius: 100, fontWeight: 600, fontSize: '0.85rem',
                background: cfg.bg, color: cfg.color, border: `1px solid ${cfg.color}22`
              }}>
                {cfg.label}
              </span>
            </div>

            <hr className="divider" />

            {/* Route */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
              <div style={{ flex: 1 }}>
                <p style={{ fontSize: '0.75rem', color: 'var(--ink-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>From</p>
                <p style={{ fontWeight: 700, fontSize: '1.1rem' }}>{result.currentLocation || 'N/A'}</p>
                {result.scheduledDeparture && (
                  <p style={{ fontSize: '0.82rem', color: 'var(--ink-muted)' }}>
                    Sched: {result.scheduledDeparture}
                    {result.actualDeparture && result.actualDeparture !== result.scheduledDeparture &&
                      <span style={{ color: '#d97706' }}> · Actual: {result.actualDeparture}</span>}
                  </p>
                )}
                {result.departureTerminal && <p style={{ fontSize: '0.78rem', color: 'var(--ink-muted)' }}>Terminal {result.departureTerminal}{result.departureGate ? ` · Gate ${result.departureGate}` : ''}</p>}
              </div>

              <div style={{ textAlign: 'center', color: 'var(--ink-muted)', fontSize: '1.5rem' }}>✈</div>

              <div style={{ flex: 1, textAlign: 'right' }}>
                <p style={{ fontSize: '0.75rem', color: 'var(--ink-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>To</p>
                <p style={{ fontWeight: 700, fontSize: '1.1rem' }}>{result.destination || 'N/A'}</p>
                {result.scheduledArrival && (
                  <p style={{ fontSize: '0.82rem', color: 'var(--ink-muted)' }}>
                    Sched: {result.scheduledArrival}
                    {result.actualArrival && result.actualArrival !== result.scheduledArrival &&
                      <span style={{ color: '#d97706' }}> · Actual: {result.actualArrival}</span>}
                  </p>
                )}
                {result.arrivalTerminal && <p style={{ fontSize: '0.78rem', color: 'var(--ink-muted)' }}>Terminal {result.arrivalTerminal}</p>}
              </div>
            </div>

            {/* Progress bar for in-flight */}
            {result.status === 'IN_FLIGHT' && (
              <div style={{ marginBottom: '1.5rem' }}>
                <div style={{ background: 'var(--border-soft)', borderRadius: 100, height: 6, overflow: 'hidden' }}>
                  <div style={{
                    height: '100%', borderRadius: 100, background: '#059669',
                    width: (() => {
                      const match = result.remarks?.match(/(\d+)%/);
                      return match ? match[1] + '%' : '50%';
                    })(),
                    transition: 'width 1s ease'
                  }} />
                </div>
                <p style={{ fontSize: '0.78rem', color: 'var(--ink-muted)', marginTop: 4 }}>{result.remarks}</p>
              </div>
            )}

            {/* Remarks */}
            {result.status !== 'IN_FLIGHT' && result.remarks && (
              <div style={{ background: 'var(--paper)', borderRadius: 'var(--radius)', padding: '0.75rem 1rem', fontSize: '0.875rem', color: 'var(--ink-soft)' }}>
                {result.remarks}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
