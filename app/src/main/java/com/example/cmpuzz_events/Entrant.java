package com.example.cmpuzz_events;

public class Entrant {
    private String name;
    private Double latitude;
    private Double longitude;

    public Entrant(String name, Double latitude, Double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() { return name; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}
