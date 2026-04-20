import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { BookingProvider } from './context/BookingContext';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import HomePage from './pages/HomePage';
import SeatPage from './pages/SeatPage';
import ConfirmPage from './pages/ConfirmPage';
import PassengerFormPage from './pages/PassengerFormPage';
import PaymentPage from './pages/PaymentPage';
import SuccessPage from './pages/SuccessPage';
import MyBookingsPage from './pages/MyBookingsPage';
import TrackPage from './pages/TrackPage';
import LoyaltyPage from './pages/LoyaltyPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <BookingProvider>
          <Navbar />
          <Routes>
            <Route path="/login"    element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            <Route path="/"                    element={<ProtectedRoute><HomePage /></ProtectedRoute>} />
            <Route path="/flights/:flightId/seats" element={<ProtectedRoute><SeatPage /></ProtectedRoute>} />
            <Route path="/booking/confirm"     element={<ProtectedRoute><ConfirmPage /></ProtectedRoute>} />
            <Route path="/booking/passengers"  element={<ProtectedRoute><PassengerFormPage /></ProtectedRoute>} />
            <Route path="/payment"             element={<ProtectedRoute><PaymentPage /></ProtectedRoute>} />
            <Route path="/booking/success"     element={<ProtectedRoute><SuccessPage /></ProtectedRoute>} />
            <Route path="/my-bookings"         element={<ProtectedRoute><MyBookingsPage /></ProtectedRoute>} />
            <Route path="/track"               element={<ProtectedRoute><TrackPage /></ProtectedRoute>} />
            <Route path="/loyalty"             element={<ProtectedRoute><LoyaltyPage /></ProtectedRoute>} />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BookingProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
