import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

export default function Navbar() {
  const { user, logout, isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const isActive = (path) => location.pathname === path;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand">
          <span className="brand-mark">A</span>
          <span className="brand-name">Aero</span>
        </Link>

        <div className="navbar-links">
          {isAuthenticated && (
            <>
              <Link to="/"            className={`nav-link ${isActive('/') ? 'active' : ''}`}>Search</Link>
              <Link to="/my-bookings" className={`nav-link ${isActive('/my-bookings') ? 'active' : ''}`}>My Bookings</Link>
              <Link to="/track"       className={`nav-link ${isActive('/track') ? 'active' : ''}`}>Track Flight</Link>
              <Link to="/loyalty"     className={`nav-link ${isActive('/loyalty') ? 'active' : ''}`}>Loyalty</Link>
            </>
          )}
        </div>

        <div className="navbar-actions">
          {isAuthenticated ? (
            <>
              <span className="nav-user">{user?.email}</span>
              <button className="btn btn-outline btn-sm" onClick={handleLogout}>Sign out</button>
            </>
          ) : (
            <>
              <Link to="/login"    className="btn btn-outline btn-sm">Sign in</Link>
              <Link to="/register" className="btn btn-primary btn-sm">Get started</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
