package com.example.demo.model;

import java.util.List;

public class TicketDTO {
    private List<Passenger> passengers;
    private List<FlightDTO> flights;
    private String rules;
    private boolean samePnr;

    // Getters e Setters
    public List<Passenger> getPassengers() { return passengers; }
    public void setPassengers(List<Passenger> passengers) { this.passengers = passengers; }
    public List<FlightDTO> getFlights() { return flights; }
    public void setFlights(List<FlightDTO> flights) { this.flights = flights; }
    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }
    public boolean isSamePnr() { return samePnr; }
    public void setSamePnr(boolean samePnr) { this.samePnr = samePnr; }
}
