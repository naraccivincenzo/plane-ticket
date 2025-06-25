package com.example.demo.model;

import java.util.List;

public class TicketDTO {
    private List<Passenger> passengers;
    private String route;
    private String departureTime;
    private String arrivalTime;
    private String airlineCode;
    private double price;
    private String rules;
    private String pnr;

    // Getters e Setters
    public List<Passenger> getPassengers() { return passengers; }
    public void setPassengers(List<Passenger> passengers) { this.passengers = passengers; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }
    public String getAirlineCode() { return airlineCode; }
    public void setAirlineCode(String airlineCode) { this.airlineCode = airlineCode; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }
    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }
}