package com.example.demo.model;

public class FlightDTO {
    private String route;
    private String departureTime;
    private String arrivalTime;
    private String airlineCode;
    private String airlineName;
    private double price;
    private String pnr;
    private String logoUrl;

    // Getters e Setters
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }
    public String getAirlineCode() { return airlineCode; }
    public void setAirlineCode(String airlineCode) { this.airlineCode = airlineCode; }
    public String getAirlineName() { return airlineName; }
    public void setAirlineName(String airlineName) { this.airlineName = airlineName; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
}
