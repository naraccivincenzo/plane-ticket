package com.example.demo.model;

public class Passenger {
    private String type;
    private String fullName;
    private boolean handLuggage;
    private int handLuggageKg;
    private boolean checkedLuggage;
    private int checkedLuggageCount;
    private int checkedLuggageKg;

    // Getters e Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public boolean hasHandLuggage() { return handLuggage; }
    public void setHandLuggage(boolean handLuggage) { this.handLuggage = handLuggage; }
    public int getHandLuggageKg() { return handLuggageKg; }
    public void setHandLuggageKg(int handLuggageKg) { this.handLuggageKg = handLuggageKg; }
    public boolean hasCheckedLuggage() { return checkedLuggage; }
    public void setCheckedLuggage(boolean checkedLuggage) { this.checkedLuggage = checkedLuggage; }
    public int getCheckedLuggageCount() { return checkedLuggageCount; }
    public void setCheckedLuggageCount(int checkedLuggageCount) { this.checkedLuggageCount = checkedLuggageCount; }
    public int getCheckedLuggageKg() { return checkedLuggageKg; }
    public void setCheckedLuggageKg(int checkedLuggageKg) { this.checkedLuggageKg = checkedLuggageKg; }
}