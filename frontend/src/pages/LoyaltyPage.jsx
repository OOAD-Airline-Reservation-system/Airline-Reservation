import { useState, useEffect } from 'react';
import { loyalty } from '../services/api';

const TIER_EMOJI = { BRONZE: '🥉', SILVER: '🥈', GOLD: '🥇', PLATINUM: '💎' };

export default function LoyaltyPage() {
  const [account, setAccount] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [redeemPoints, setRedeemPoints] = useState('');
  const [redeemMsg, setRedeemMsg] = useState('');

  const load = async () => {
    try {
      const res = await loyalty.getAccount();
      setAccount(res.data);
    } catch {
      setError('Could not load loyalty account.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleRedeem = async (e) => {
    e.preventDefault();
    setRedeemMsg('');
    try {
      const res = await loyalty.redeem(Number(redeemPoints), '');
      setRedeemMsg(`Redeemed! Discount: ₹${res.data.discountINR}. Remaining: ${res.data.remainingPoints} pts`);
      setRedeemPoints('');
      load();
    } catch (err) {
      setRedeemMsg(err.response?.data?.message || 'Redemption failed.');
    }
  };

  if (loading) return <div className="loading-center"><span className="spinner" /> Loading loyalty account</div>;
  if (error)   return <div className="page-container" style={{ paddingTop: 40 }}><div className="alert alert-error">{error}</div></div>;

  const tier = account?.tier || 'BRONZE';

  return (
    <div className="page-container" style={{ paddingTop: 40, paddingBottom: 60 }}>
      <div className="page-header" style={{ paddingTop: 0 }}>
        <h1 className="page-title">{TIER_EMOJI[tier]} Loyalty Programme</h1>
        <p className="page-subtitle">Membership #{account?.membershipNumber}</p>
      </div>

      <div className="grid-2" style={{ gap: '1.5rem', marginBottom: '2rem' }}>
        <div className="card" style={{ padding: '1.5rem' }}>
          <p style={{ fontSize: '0.78rem', fontWeight: 500, color: 'var(--ink-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>Points balance</p>
          <p style={{ fontSize: '2.5rem', fontWeight: 700, margin: '0.5rem 0' }}>{account?.pointsBalance?.toLocaleString('en-IN')}</p>
          <p style={{ color: 'var(--ink-muted)', fontSize: '0.85rem' }}>≈ ₹{account?.redeemableValueINR?.toLocaleString('en-IN')} redeemable</p>
        </div>
        <div className="card" style={{ padding: '1.5rem' }}>
          <p style={{ fontSize: '0.78rem', fontWeight: 500, color: 'var(--ink-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>Tier status</p>
          <p style={{ fontSize: '2rem', fontWeight: 700, margin: '0.5rem 0' }}>{TIER_EMOJI[tier]} {tier}</p>
          {account?.pointsToNextTier > 0 && (
            <p style={{ color: 'var(--ink-muted)', fontSize: '0.85rem' }}>{account.pointsToNextTier.toLocaleString('en-IN')} pts to next tier</p>
          )}
        </div>
      </div>

      <div className="card" style={{ padding: '1.5rem', marginBottom: '2rem' }}>
        <p style={{ fontWeight: 600, marginBottom: '1rem' }}>Redeem points</p>
        <form onSubmit={handleRedeem} style={{ display: 'flex', gap: '1rem', alignItems: 'flex-end' }}>
          <div className="form-group">
            <label className="form-label">Points to redeem (10 pts = ₹1)</label>
            <input className="form-input" type="number" min="10" value={redeemPoints}
              onChange={e => setRedeemPoints(e.target.value)} placeholder="e.g. 100" style={{ width: 160 }} required />
          </div>
          <button type="submit" className="btn btn-primary">Redeem</button>
        </form>
        {redeemMsg && <p style={{ marginTop: '0.75rem', fontSize: '0.875rem', color: 'var(--success)' }}>{redeemMsg}</p>}
      </div>

      <div className="card" style={{ padding: '1.5rem' }}>
        <p style={{ fontWeight: 600, marginBottom: '1rem' }}>Transaction history</p>
        {(!account?.transactions || account.transactions.length === 0) ? (
          <p style={{ color: 'var(--ink-muted)', fontSize: '0.875rem' }}>No transactions yet.</p>
        ) : (
          <div>
            {account.transactions.map((tx, i) => (
              <div key={tx.id || i} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.75rem 0', borderBottom: i < account.transactions.length - 1 ? '1px solid var(--border-soft)' : 'none' }}>
                <div>
                  <p style={{ fontSize: '0.875rem', fontWeight: 500 }}>{tx.description}</p>
                  <p style={{ fontSize: '0.78rem', color: 'var(--ink-muted)' }}>{tx.createdAt ? new Date(tx.createdAt).toLocaleDateString('en-IN') : ''}</p>
                </div>
                <p style={{ fontWeight: 600, color: tx.points > 0 ? 'var(--success)' : 'var(--accent)' }}>
                  {tx.points > 0 ? '+' : ''}{tx.points} pts
                </p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
