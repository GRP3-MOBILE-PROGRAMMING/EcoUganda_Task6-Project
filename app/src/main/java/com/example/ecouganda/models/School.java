package com.example.ecouganda.models;

public class School {
    private String id;
    private String name;
    private String district;
    private String status; // ACTIVE, PLANNED
    private int treeCount;
    private boolean solarInstalled;

    public School(String id, String name, String district, String status, int treeCount, boolean solarInstalled) {
        this.id = id;
        this.name = name;
        this.district = district;
        this.status = status;
        this.treeCount = treeCount;
        this.solarInstalled = solarInstalled;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDistrict() { return district; }
    public String getStatus() { return status; }
    public int getTreeCount() { return treeCount; }
    public boolean isSolarInstalled() { return solarInstalled; }
}
