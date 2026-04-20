import { createContext, useContext, useState } from 'react';

const BookingContext = createContext(null);

export function BookingProvider({ children }) {
  const [selectedFlight, setSelectedFlight] = useState(null);
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [booking, setBooking] = useState(null);
  const [searchParams, setSearchParams] = useState(null);

  const reset = () => {
    setSelectedFlight(null);
    setSelectedSeats([]);
    setBooking(null);
  };

  return (
    <BookingContext.Provider value={{
      selectedFlight, setSelectedFlight,
      selectedSeats, setSelectedSeats,
      booking, setBooking,
      searchParams, setSearchParams,
      reset,
    }}>
      {children}
    </BookingContext.Provider>
  );
}

export const useBooking = () => useContext(BookingContext);
