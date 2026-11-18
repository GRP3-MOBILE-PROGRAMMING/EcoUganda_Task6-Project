package com.example.ecouganda.models;

public class SustainabilityStats {
    private int solarInstallations;
    private String solarDelta;
    private int acresReforested;
    private String acresDelta;
    private String co2Offset;
    private String co2Impact;

    public SustainabilityStats(int solarInstallations, String solarDelta, int acresReforested, String acresDelta, String co2Offset, String co2Impact) {
        this.solarInstallations = solarInstallations;
        this.solarDelta = solarDelta;
        this.acresReforested = acresReforested;
        this.acresDelta = acresDelta;
        this.co2Offset = co2Offset;
        this.co2Impact = co2Impact;
    }

    public int getSolarInstallations() { return solarInstallations; }
    public String getSolarDelta() { return solarDelta; }
    public int getAcresReforested() { return acresReforested; }
    public String getAcresDelta() { return acresDelta; }
    public String getCo2Offset() { return co2Offset; }
    public String getCo2Impact() { return co2Impact; }
}
