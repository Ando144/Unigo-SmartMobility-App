package com.example.unigo_das.item;

public class TransitStop {
    private String id;
    private String name;
    private String type;
    private double latitude;
    private double longitude;

    public TransitStop(String id, String name, String type, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}