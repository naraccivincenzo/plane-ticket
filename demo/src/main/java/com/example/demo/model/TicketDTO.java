package com.example.demo.model;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public class TicketDTO {
    private List<String> passengers;
    private String route;
    private String departureTime;
    private String arrivalTime;
    private String airline;
    private double price;
    private String rules;
    private MultipartFile airlineLogo;
    private MultipartFile agencyLogo;

    // Getters e Setters
    public List<String> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<String> passengers) {
        this.passengers = passengers;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public MultipartFile getAirlineLogo() {
        return airlineLogo;
    }

    public void setAirlineLogo(MultipartFile airlineLogo) {
        this.airlineLogo = airlineLogo;
    }

    public MultipartFile getAgencyLogo() {
        return agencyLogo;
    }

    public void setAgencyLogo(MultipartFile agencyLogo) {
        this.agencyLogo = agencyLogo;
    }
}