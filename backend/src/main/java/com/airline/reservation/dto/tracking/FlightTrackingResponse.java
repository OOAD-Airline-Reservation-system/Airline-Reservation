package com.airline.reservation.dto.tracking;

public class FlightTrackingResponse {

    private String flightNumber;
    private String airline;
    private String status;
    private String currentLocation;
    private String destination;
    private String scheduledDeparture;
    private String actualDeparture;
    private String scheduledArrival;
    private String actualArrival;
    private String departureTerminal;
    private String arrivalTerminal;
    private String departureGate;
    private String remarks;
    private String dataSource; // "LIVE" or "SCHEDULED"

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public String getAirline() { return airline; }
    public void setAirline(String airline) { this.airline = airline; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getScheduledDeparture() { return scheduledDeparture; }
    public void setScheduledDeparture(String scheduledDeparture) { this.scheduledDeparture = scheduledDeparture; }

    public String getActualDeparture() { return actualDeparture; }
    public void setActualDeparture(String actualDeparture) { this.actualDeparture = actualDeparture; }

    public String getScheduledArrival() { return scheduledArrival; }
    public void setScheduledArrival(String scheduledArrival) { this.scheduledArrival = scheduledArrival; }

    public String getActualArrival() { return actualArrival; }
    public void setActualArrival(String actualArrival) { this.actualArrival = actualArrival; }

    public String getDepartureTerminal() { return departureTerminal; }
    public void setDepartureTerminal(String departureTerminal) { this.departureTerminal = departureTerminal; }

    public String getArrivalTerminal() { return arrivalTerminal; }
    public void setArrivalTerminal(String arrivalTerminal) { this.arrivalTerminal = arrivalTerminal; }

    public String getDepartureGate() { return departureGate; }
    public void setDepartureGate(String departureGate) { this.departureGate = departureGate; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
}
