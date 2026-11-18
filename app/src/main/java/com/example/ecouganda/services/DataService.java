package com.example.ecouganda.services;

import com.example.ecouganda.models.Project;
import com.example.ecouganda.models.Report;
import com.example.ecouganda.models.School;
import com.example.ecouganda.models.SustainabilityStats;

import java.util.ArrayList;
import java.util.List;

public class DataService {
    private static DataService instance;
    private List<Project> projects;
    private List<School> schools;
    private List<Report> reports;
    private SustainabilityStats stats;

    private DataService() {
        initMockData();
    }

    public static synchronized DataService getInstance() {
        if (instance == null) {
            instance = new DataService();
        }
        return instance;
    }

    private void initMockData() {
        projects = new ArrayList<>();
        projects.add(new Project("P1", "Kiryandongo Solar Initiative", "Solar energy", "Kiryandongo", 71));
        projects.add(new Project("P2", "Rwenzori Mini Hydro", "Hydro energy", "Kasese", 46));
        projects.add(new Project("P3", "Community Biogas", "Biogas", "Gulu", 18));

        schools = new ArrayList<>();
        schools.add(new School("S1", "St. Mary’s Primary School", "Mukono", "ACTIVE", 1240, true));
        schools.add(new School("S2", "Bweyogerere Secondary", "Wakiso", "ACTIVE", 860, true));
        schools.add(new School("S3", "Kisoro Community School", "Kisoro", "PLANNED", 520, false));

        reports = new ArrayList<>();
        reports.add(new Report("RPT-2048", "Deforestation alert • Mabira Forest", "Buikwe", "12 min ago", "URGENT", "Possible fresh clearing near the eastern access road.", "Mabira Forest"));
        reports.add(new Report("RPT-2041", "Illegal dumping near wetland", "Wakiso", "2 hours ago", "REVIEW", "Multiple reports of waste disposal in protected areas.", "Wakiso Wetland"));

        stats = new SustainabilityStats(1284, "+12% this quarter", 8420, "+640 acres this month", "12.6k t", "Estimated annual impact");
    }

    public List<Project> getProjects() {
        return new ArrayList<>(projects);
    }

    public List<School> getSchools() {
        return new ArrayList<>(schools);
    }

    public List<Report> getReports() {
        return new ArrayList<>(reports);
    }

    public SustainabilityStats getStats() {
        return stats;
    }
    
    public Report getReportById(String id) {
        for (Report r : reports) {
            if (r.getId().equals(id)) return r;
        }
        return null;
    }
}
