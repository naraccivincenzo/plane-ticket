package com.example.demo.model;

public class Airline {
    private String code;
    private String name;
    private String logoPath;
    private String rules;
    private String primaryColor;
    private String secondaryColor;

    public Airline(String code, String name, String logoPath, String rules, String primaryColor, String secondaryColor) {
        this.code = code;
        this.name = name;
        this.logoPath = logoPath;
        this.rules = rules;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }

    // Getters
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getLogoPath() { return logoPath; }
    public String getRules() { return rules; }
    public String getPrimaryColor() { return primaryColor; }
    public String getSecondaryColor() { return secondaryColor; }
}