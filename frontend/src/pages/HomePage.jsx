import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { flights, trips } from '../services/api';
import { useBooking } from '../context/BookingContext';
import './HomePage.css';

const CITIES = [
  // India
  { code: 'DEL', name: 'Delhi' },
  { code: 'BOM', name: 'Mumbai' },
  { code: 'BLR', name: 'Bengaluru' },
  { code: 'MAA', name: 'Chennai' },
  { code: 'CCU', name: 'Kolkata' },
  { code: 'HYD', name: 'Hyderabad' },
  { code: 'GOI', name: 'Goa' },
  { code: 'AMD', name: 'Ahmedabad' },
  { code: 'PNQ', name: 'Pune' },
  { code: 'JAI', name: 'Jaipur' },
  { code: 'COK', name: 'Kochi' },
  { code: 'ATQ', name: 'Amritsar' },
  // International
  { code: 'DXB', name: 'Dubai' },
  { code: 'LHR', name: 'London Heathrow' },
  { code: 'SIN', name: 'Singapore' },
  { code: 'JFK', name: 'New York JFK' },
  { code: 'CDG', name: 'Paris CDG' },
  { code: 'FRA', name: 'Frankfurt' },
  { code: 'NRT', name: 'Tokyo Narita' },
  { code: 'HKG', name: 'Hong Kong' },
  { code: 'SYD', name: 'Sydney' },
  { code: 'LAX', name: 'Los Angeles' },
];

const formatDate = (dt) => {
  if (!dt) return '';
  return new Date(dt).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', hour12: true });
};

const getDuration = (dep, arr) => {
  const diff = Math.abs(new Date(arr) - new Date(dep));
  const h = Math.floor(diff / 3600000);
  const m = Math.floor((diff % 3600000) / 60000);
  return `${h}h ${m}m`;
};

function CityInput({ label, placeholder, value, onChange }) {
  const [suggestions, setSuggestions] = useState([]);
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  const handleChange = (e) => {
    const v = e.target.value;
    onChange(v);
    const q = v.toLowerCase();
    setSuggestions(CITIES.filter(c =>
      c.code.toLowerCase().startsWith(q) || c.name.toLowerCase().startsWith(q)
    ));
    setOpen(true);
  };

  const handleFocus = () => {
    const q = value.toLowerCase();
    setSuggestions(q ? CITIES.filter(c =>
      c.code.toLowerCase().startsWith(q) || c.name.toLowerCase().startsWith(q)
    ) : CITIES);
    setOpen(true);
  };

  const handleBlur = () => setTimeout(() => setOpen(false), 150);
  const pick = (city) => { onChange(city.code); setOpen(false); };

  return (
    <div className="search-field" style={{ position: 'relative' }} ref={ref}>
      <label className="form-label">{label}</label>
      <input
        className="form-input search-input"
        type="text"
        placeholder={placeholder}
        value={value}
        onChange={handleChange}
        onFocus={handleFocus}
        onBlur={handleBlur}
        autoComplete="off"
        required
      />
      {open && suggestions.length > 0 && (
        <ul style={{
          position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 200,
          background: '#fff', border: '1px solid var(--border)', borderRadius: 'var(--radius)',
          boxShadow: 'var(--shadow-md)', listStyle: 'none', margin: 0, padding: '4px 0', marginTop: 2,
        }}>
          {suggestions.map(city => (
            <li key={city.code} onMouseDown={() => pick(city)}
              style={{ padding: '8px 14px', cursor: 'pointer', fontSize: '0.9rem', display: 'flex', gap: 10, alignItems: 'center' }}
              onMouseEnter={e => e.currentTarget.style.background = 'var(--paper-warm)'}
              onMouseLeave={e => e.currentTarget.style.background = 'transparent'}>
              <span style={{ fontWeight: 600, minWidth: 36 }}>{city.code}</span>
              <span style={{ color: 'var(--ink-muted)' }}>{city.name}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default function HomePage() {
  const navigate = useNavigate();
  const { setSelectedFlight, setSearchParams } = useBooking();

  const today = new Date().toISOString().split('T')[0];
  const [form, setForm] = useState({ source: '', destination: '', date: today });
  const [results, setResults] = useState([]);
  const [searched, setSearched] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // AI suggestions state
  const [aiForm, setAiForm] = useState({ from: '', budget: 15000, interests: 'beaches, culture', duration: 5 });
  const [aiResults, setAiResults] = useState([]);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState('');
  const [showAi, setShowAi] = useState(false);

  const handleSearch = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    setSearched(true);
    try {
      const res = await flights.search({ source: form.source, destination: form.destination, date: form.date });
      setResults(res.data || []);
      setSearchParams(form);
    } catch {
      setError('Could not fetch flights. Please check that the backend is running.');
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSelect = (flight) => {
    setSelectedFlight(flight);
    navigate(`/flights/${flight.id}/seats`);
  };

  const handleAiSuggest = async (e) => {
    e.preventDefault();
    setAiError('');
    setAiLoading(true);
    try {
      const res = await trips.suggest(aiForm);
      setAiResults(res.data || []);
    } catch {
      setAiError('AI suggestions unavailable. Add a Gemini API key in application.yml to enable.');
      setAiResults([]);
    } finally {
      setAiLoading(false);
    }
  };

  return (
    <div className="home-page">
      <div className="search-hero">
        <div className="page-container">
          <div className="hero-content">
            <h1 className="hero-title">Where to next?</h1>
            <p className="hero-sub">Search flights, select your seat, and be on your way.</p>

            <form onSubmit={handleSearch} className="search-bar">
              <CityInput label="From" placeholder="Mumbai" value={form.source}
                onChange={v => setForm({ ...form, source: v })} />
              <div className="search-divider" />
              <CityInput label="To" placeholder="Delhi" value={form.destination}
                onChange={v => setForm({ ...form, destination: v })} />
              <div className="search-divider" />
              <div className="search-field">
                <label className="form-label">Date</label>
                <input className="form-input search-input" type="date" value={form.date} min={today}
                  onChange={e => setForm({ ...form, date: e.target.value })} required />
              </div>
              <button type="submit" className="btn btn-primary search-btn">
                {loading ? <span className="spinner" /> : 'Search flights'}
              </button>
            </form>
          </div>
        </div>
      </div>

      {(searched || error) && (
        <div className="page-container" style={{ paddingTop: 40, paddingBottom: 60 }}>
          {error && <div className="alert alert-error">{error}</div>}
          {!loading && !error && (
            <>
              <div className="results-header">
                <p className="results-count">
                  {results.length > 0
                    ? `${results.length} flight${results.length !== 1 ? 's' : ''} found`
                    : 'No flights found for this route and date'}
                </p>
                {results.length > 0 && <p className="results-route">{form.source} to {form.destination}</p>}
              </div>
              <div className="flights-list">
                {results.map((f, i) => (
                  <div key={f.id} className="flight-card card fade-up" style={{ animationDelay: `${i * 0.05}s` }}>
                    <div className="flight-card-main">
                      <div className="flight-times">
                        <div className="time-block">
                          <span className="time-value">{formatDate(f.departureTime)}</span>
                          <span className="time-city">{f.source} · {f.sourceAirport || f.source}</span>
                        </div>
                        <div className="flight-arrow">
                          <div className="arrow-line" />
                          <span className="arrow-duration">{getDuration(f.departureTime, f.arrivalTime)}</span>
                        </div>
                        <div className="time-block">
                          <span className="time-value">{formatDate(f.arrivalTime)}</span>
                          <span className="time-city">{f.destination} · {f.destinationAirport || f.destination}</span>
                        </div>
                      </div>
                      <div className="flight-meta">
                        <span className="flight-number">{f.flightNumber}</span>
                        {f.airline && <span className="flight-number" style={{marginLeft:8,color:'var(--ink-muted)'}}>{f.airline}</span>}
                      </div>
                    </div>
                    <div className="flight-card-side">
                      <div className="flight-price">
                        <span className="price-currency">INR</span>
                        <span className="price-value">{Number(f.basePrice).toLocaleString('en-IN')}</span>
                      </div>
                      <button className="btn btn-primary" onClick={() => handleSelect(f)}>Select</button>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      )}

      {/* AI Trip Suggestions */}
      <div className="page-container" style={{ paddingBottom: 60 }}>
        <div className="ai-section card" style={{ padding: '1.5rem', marginTop: searched ? 0 : 40 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: showAi ? '1.25rem' : 0 }}>
            <div>
              <h3 style={{ fontFamily: 'var(--font-display)', fontWeight: 400, fontSize: '1.3rem' }}>✨ AI Trip Suggestions</h3>
              <p style={{ color: 'var(--ink-muted)', fontSize: '0.85rem', marginTop: 4 }}>Powered by Gemini — personalised destination ideas</p>
            </div>
            <button className="btn btn-outline btn-sm" onClick={() => setShowAi(!showAi)}>
              {showAi ? 'Hide' : 'Get suggestions'}
            </button>
          </div>

          {showAi && (
            <>
              <form onSubmit={handleAiSuggest} style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
                <div className="form-group">
                  <label className="form-label">Travelling from</label>
                  <input className="form-input" placeholder="Mumbai" value={aiForm.from}
                    onChange={e => setAiForm({ ...aiForm, from: e.target.value })} required style={{ width: 140 }} />
                </div>
                <div className="form-group">
                  <label className="form-label">Budget (INR)</label>
                  <input className="form-input" type="number" value={aiForm.budget}
                    onChange={e => setAiForm({ ...aiForm, budget: Number(e.target.value) })} style={{ width: 120 }} />
                </div>
                <div className="form-group">
                  <label className="form-label">Interests</label>
                  <input className="form-input" placeholder="beaches, food" value={aiForm.interests}
                    onChange={e => setAiForm({ ...aiForm, interests: e.target.value })} style={{ width: 180 }} />
                </div>
                <div className="form-group">
                  <label className="form-label">Days</label>
                  <input className="form-input" type="number" value={aiForm.duration}
                    onChange={e => setAiForm({ ...aiForm, duration: Number(e.target.value) })} style={{ width: 80 }} />
                </div>
                <button type="submit" className="btn btn-primary" disabled={aiLoading}>
                  {aiLoading ? <span className="spinner" /> : 'Suggest'}
                </button>
              </form>

              {aiError && <div className="alert alert-error" style={{ marginTop: '1rem' }}>{aiError}</div>}

              {aiResults.length > 0 && (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '1rem', marginTop: '1.25rem' }}>
                  {aiResults.map((s, i) => (
                    <div key={i} style={{ background: 'var(--paper)', border: '1px solid var(--border-soft)', borderRadius: 'var(--radius-lg)', padding: '1rem' }}>
                      <p style={{ fontWeight: 600, fontSize: '1rem', marginBottom: 4 }}>{s.destination}</p>
                      <p style={{ fontSize: '0.82rem', color: 'var(--ink-muted)', marginBottom: 8 }}>{s.reason}</p>
                      <p style={{ fontSize: '0.82rem' }}>💰 ₹{Number(s.estimatedCost).toLocaleString('en-IN')}</p>
                      <p style={{ fontSize: '0.82rem' }}>🗓 {s.bestTimeToVisit}</p>
                      {s.highlights?.length > 0 && (
                        <p style={{ fontSize: '0.78rem', color: 'var(--ink-muted)', marginTop: 6 }}>
                          {s.highlights.slice(0, 3).join(' · ')}
                        </p>
                      )}
                      {s.iataCode && (
                        <button
                          className="btn btn-outline btn-sm"
                          style={{ marginTop: 10, width: '100%' }}
                          onClick={() => {
                            setForm(f => ({ ...f, source: s.flightFrom || aiForm.from, destination: s.iataCode }));
                            setShowAi(false);
                            window.scrollTo({ top: 0, behavior: 'smooth' });
                          }}
                        >
                          Search {s.iataCode} flights →
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {!searched && (
        <div className="home-features page-container">
          <div className="feature-item">
            <span className="feature-num">01</span>
            <h3 className="feature-title">Search and compare</h3>
            <p className="feature-desc">Find the best flights across all available routes instantly.</p>
          </div>
          <div className="feature-item">
            <span className="feature-num">02</span>
            <h3 className="feature-title">Choose your seat</h3>
            <p className="feature-desc">Interactive seat map so you know exactly where you sit.</p>
          </div>
          <div className="feature-item">
            <span className="feature-num">03</span>
            <h3 className="feature-title">Pay securely</h3>
            <p className="feature-desc">UPI, card, or wallet — all payments processed through Razorpay.</p>
          </div>
        </div>
      )}
    </div>
  );
}
