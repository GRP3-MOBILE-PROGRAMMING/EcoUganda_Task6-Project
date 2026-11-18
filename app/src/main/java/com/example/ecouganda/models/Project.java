package com.example.ecouganda.models;

public class Project {
    private String id;
    private String name;
    private String type;
    private String location;
    private int progress;

    public Project(String id, String name, String type, String location, int progress) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.location = location;
        this.progress = progress;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getLocation() { return location; }
    public int getProgress() { return progress; }
}
