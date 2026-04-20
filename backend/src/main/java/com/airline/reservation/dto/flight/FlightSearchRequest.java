package com.airline.reservation.dto.flight;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class FlightSearchRequest {

    @NotBlank
    private String source;       // IATA airport code e.g. DEL

    @NotBlank
    private String destination;  // IATA airport code e.g. BOM

    @NotNull
    private LocalDate date;

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
