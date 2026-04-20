import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { passengers } from '../services/api';
import { useBooking } from '../context/BookingContext';

export default function PassengerFormPage() {
  const navigate = useNavigate();
  const { booking, selectedSeats } = useBooking();
  const [forms, setForms] = useState(
    (selectedSeats || [{ seatNumber: '' }]).map(s => ({
      firstName: '', lastName: '', dateOfBirth: '', nationality: '',
      passportNumber: '', passportExpiry: '', passportCountry: '',
      contactEmail: '', contactPhone: '', mealPreference: 'STANDARD',
      seatNumber: s.seatNumber || '',
    }))
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!booking) { navigate('/'); return null; }

  const update = (i, field, value) => {
    setForms(prev => prev.map((f, idx) => idx === i ? { ...f, [field]: value } : f));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await passengers.save(booking.id, forms);
      navigate('/payment');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save passenger details.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container" style={{ paddingTop: 40, paddingBottom: 60 }}>
      <div className="page-header" style={{ paddingTop: 0 }}>
        <h1 className="page-title">Passenger details</h1>
        <p className="page-subtitle">Enter travel document details for each passenger.</p>
      </div>

      <form onSubmit={handleSubmit}>
        {forms.map((f, i) => (
          <div key={i} className="card" style={{ padding: '1.5rem', marginBottom: '1.5rem' }}>
            <p style={{ fontWeight: 600, marginBottom: '1rem', fontSize: '0.95rem' }}>
              Passenger {i + 1} {f.seatNumber && `— Seat ${f.seatNumber}`}
            </p>
            <div className="grid-2" style={{ gap: '1rem' }}>
              {[
                ['First name', 'firstName', 'text', true],
                ['Last name', 'lastName', 'text', true],
                ['Date of birth', 'dateOfBirth', 'date', false],
                ['Nationality', 'nationality', 'text', false],
                ['Passport number', 'passportNumber', 'text', false],
                ['Passport expiry', 'passportExpiry', 'date', false],
                ['Contact email', 'contactEmail', 'email', false],
                ['Contact phone', 'contactPhone', 'tel', false],
              ].map(([label, field, type, required]) => (
                <div className="form-group" key={field}>
                  <label className="form-label">{label}</label>
                  <input className="form-input" type={type} value={f[field]}
                    onChange={e => update(i, field, e.target.value)} required={required} />
                </div>
              ))}
              <div className="form-group">
                <label className="form-label">Meal preference</label>
                <select className="form-input" value={f.mealPreference}
                  onChange={e => update(i, 'mealPreference', e.target.value)}>
                  <option value="STANDARD">Standard</option>
                  <option value="VEGETARIAN">Vegetarian</option>
                  <option value="VEGAN">Vegan</option>
                  <option value="HALAL">Halal</option>
                  <option value="KOSHER">Kosher</option>
                </select>
              </div>
            </div>
          </div>
        ))}

        {error && <div className="alert alert-error" style={{ marginBottom: '1rem' }}>{error}</div>}

        <div style={{ display: 'flex', gap: '1rem' }}>
          <button type="submit" className="btn btn-primary btn-lg" disabled={loading}>
            {loading ? <><span className="spinner" /> Saving</> : 'Continue to payment'}
          </button>
          <button type="button" className="btn btn-outline" onClick={() => navigate(-1)}>Back</button>
        </div>
      </form>
    </div>
  );
}
