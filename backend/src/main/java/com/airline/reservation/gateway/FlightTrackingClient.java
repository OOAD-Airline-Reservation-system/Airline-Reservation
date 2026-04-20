package com.airline.reservation.gateway;

import com.airline.reservation.dto.tracking.FlightTrackingResponse;

/**
 * Adapter interface for external flight tracking providers.
 * The backend is the sole caller — API keys never leave the server.
 */
public interface FlightTrackingClient {
    FlightTrackingResponse fetchStatus(String flightNumber);
}
