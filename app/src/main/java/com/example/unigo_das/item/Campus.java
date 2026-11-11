package com.example.unigo_das.item;

public class Campus {
    private String id;
    private String name;
    private String university;
    private String description;
    private String districtName; // field restored
    private boolean isStarred;
    private boolean isExpanded;
    private double latitude;
    private double longitude;

    public Campus(String id, String name, String university, String description, String districtName, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.university = university;
        this.description = description;
        this.districtName = districtName;
        this.latitude = latitude;
        this.longitude = longitude;

        this.isStarred = false;
        this.isExpanded = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getUniversity() { return university; }
    public String getDescription() { return description; }
    public String getDistrict() { return districtName; } // getter added

    public boolean isStarred() { return isStarred; }
    public void setStarred(boolean starred) { this.isStarred = starred; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { this.isExpanded = expanded; }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    @Override
    public String toString() {
        return name + " (" + university + ")";
    }
}