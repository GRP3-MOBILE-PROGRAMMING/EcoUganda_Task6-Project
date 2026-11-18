package com.example.ecouganda.models;

public class Report {
    private String id;
    private String title;
    private String district;
    private String timestamp;
    private String status; // URGENT, REVIEW, ACTION
    private String description;
    private String location;

    public Report(String id, String title, String district, String timestamp, String status, String description, String location) {
        this.id = id;
        this.title = title;
        this.district = district;
        this.timestamp = timestamp;
        this.status = status;
        this.description = description;
        this.location = location;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDistrict() { return district; }
    public String getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
}
