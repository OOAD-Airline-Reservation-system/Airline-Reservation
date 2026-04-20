// firebase/firestoreBookings.js
// Real-time Firestore helpers.
// These complement the REST API — Firebase gives push updates without polling.
//
// Data model in Firestore:
//   bookings/{bookingRef}  — booking status snapshots (mirrored from backend)
//   seatAvailability/{flightId}/seats/{seatId} — live seat status
//   loyaltyUpdates/{userId} — live points balance updates

import { db, isConfigured } from './firebaseConfig';
import {
  doc,
  setDoc,
  onSnapshot,
  collection,
  query,
  where,
  serverTimestamp,
  getDoc,
} from 'firebase/firestore';

// ─── Booking snapshot ─────────────────────────────────────────────────────────

/**
 * Mirror a booking to Firestore after creation.
 * Called from the frontend after POST /api/bookings succeeds.
 */
export async function mirrorBookingToFirestore(booking, userEmail) {
  if (!isConfigured || !db) return;
  try {
    await setDoc(doc(db, 'bookings', booking.bookingReference), {
      bookingReference: booking.bookingReference,
      bookingId:        booking.id,
      flightNumber:     booking.flightNumber,
      status:           booking.status,
      paymentStatus:    booking.paymentStatus,
      totalAmount:      booking.totalAmount,
      seats:            booking.seats || [],
      userEmail,
      updatedAt:        serverTimestamp(),
    }, { merge: true });
  } catch (e) {
    console.warn('[Firestore] mirrorBooking failed:', e);
  }
}

/**
 * Subscribe to real-time booking status updates.
 * Returns an unsubscribe function — call it on component unmount.
 */
export function subscribeToBooking(bookingReference, callback) {
  if (!isConfigured || !db) return () => {};
  const ref = doc(db, 'bookings', bookingReference);
  return onSnapshot(ref, (snap) => {
    if (snap.exists()) callback(snap.data());
  }, (err) => console.warn('[Firestore] booking sub error:', err));
}

// ─── Seat availability ────────────────────────────────────────────────────────

/**
 * Write seat availability snapshot for a flight.
 * Called after seats API returns — keeps Firestore in sync for real-time display.
 */
export async function mirrorSeatAvailability(flightId, seats) {
  if (!isConfigured || !db) return;
  try {
    const batch = seats.map(seat =>
      setDoc(
        doc(db, 'seatAvailability', String(flightId), 'seats', String(seat.id)),
        { seatNumber: seat.seatNumber, status: seat.status, seatClass: seat.seatClass, updatedAt: serverTimestamp() },
        { merge: true }
      )
    );
    await Promise.all(batch);
  } catch (e) {
    console.warn('[Firestore] mirrorSeats failed:', e);
  }
}

/**
 * Subscribe to real-time seat status for a flight.
 * Returns unsubscribe function.
 */
export function subscribeToSeatAvailability(flightId, callback) {
  if (!isConfigured || !db) return () => {};
  const seatsRef = collection(db, 'seatAvailability', String(flightId), 'seats');
  return onSnapshot(seatsRef, (snap) => {
    const seats = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    callback(seats);
  }, (err) => console.warn('[Firestore] seat sub error:', err));
}

// ─── Loyalty points live balance ──────────────────────────────────────────────

/**
 * Push loyalty account summary to Firestore.
 * Called after fetching /api/loyalty/me.
 */
export async function mirrorLoyaltyToFirestore(userId, loyaltyData) {
  if (!isConfigured || !db) return;
  try {
    await setDoc(doc(db, 'loyaltyAccounts', String(userId)), {
      ...loyaltyData,
      updatedAt: serverTimestamp(),
    }, { merge: true });
  } catch (e) {
    console.warn('[Firestore] mirrorLoyalty failed:', e);
  }
}

/**
 * Subscribe to live loyalty balance updates.
 */
export function subscribeToLoyalty(userId, callback) {
  if (!isConfigured || !db) return () => {};
  const ref = doc(db, 'loyaltyAccounts', String(userId));
  return onSnapshot(ref, (snap) => {
    if (snap.exists()) callback(snap.data());
  }, (err) => console.warn('[Firestore] loyalty sub error:', err));
}
