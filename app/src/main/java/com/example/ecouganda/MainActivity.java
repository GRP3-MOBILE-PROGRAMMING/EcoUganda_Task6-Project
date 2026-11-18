package com.example.ecouganda;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.core.splashscreen.SplashScreen;

import com.example.ecouganda.models.Project;
import com.example.ecouganda.models.Report;
import com.example.ecouganda.models.School;
import com.example.ecouganda.models.SustainabilityStats;
import com.example.ecouganda.services.DataService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Main Activity for the EcoUganda application.
 * Handles navigation between screens and coordinates data between models and views.
 */
public class MainActivity extends Activity {

    private String currentPage = "dashboard";
    private View drawer;
    private View scrim;
    private DataService dataService;

    @Override
    protected void onCreate(Bundle state) {
        // Initialize Splash Screen before super.onCreate()
        SplashScreen.installSplashScreen(this);
        
        super.onCreate(state);

        dataService = DataService.getInstance();

        // Professional styling: Match status bar to the brand header
        getWindow().setStatusBarColor(Color.parseColor("#14543A"));
        getWindow().getDecorView().setSystemUiVisibility(0);

        setContentView(R.layout.activity_main);

        drawer = findViewById(R.id.mobile_drawer);
        scrim = findViewById(R.id.drawer_scrim);

        // Restore state if returning from background
        if (state != null) {
            currentPage = state.getString("current_page", "dashboard");
        }

        // Setup Drawer Toggles
        View menuToggle = findViewById(R.id.menu_toggle);
        if (menuToggle != null) menuToggle.setOnClickListener(v -> toggleDrawer());
        if (scrim != null) scrim.setOnClickListener(v -> closeDrawer());

        bindNavigation();
        showPage(currentPage);
    }

    private void toggleDrawer() {
        if (drawer != null) {
            if (drawer.getVisibility() == View.VISIBLE) {
                closeDrawer();
            } else {
                drawer.setVisibility(View.VISIBLE);
                if (scrim != null) scrim.setVisibility(View.VISIBLE);
            }
        }
    }

    private void closeDrawer() {
        if (drawer != null) drawer.setVisibility(View.GONE);
        if (scrim != null) scrim.setVisibility(View.GONE);
    }

    private void bindNavigation() {
        // Binding sidebar items
        bind(R.id.nav_dashboard, "dashboard");
        bind(R.id.nav_projects, "projects");
        bind(R.id.nav_schools, "schools");
        bind(R.id.nav_reports, "reports");
        bind(R.id.nav_settings, "settings");

        // Binding bottom navigation items
        bind(R.id.bottom_home, "dashboard");
        bind(R.id.bottom_projects, "projects");
        bind(R.id.bottom_schools, "schools");
        bind(R.id.bottom_reports, "reports");
    }

    private void bind(int id, String page) {
        View view = findViewById(id);
        if (view != null) {
            view.setOnClickListener(v -> showPage(page));
        }
    }

    private void showPage(String page) {
        currentPage = page;
        closeDrawer();
        updateBottomNavUI(page);

        int layoutRes;
        String title;

        switch (page) {
            case "projects":
                layoutRes = R.layout.fragment_projects;
                title = getString(R.string.nav_projects);
                break;
            case "schools":
                layoutRes = R.layout.fragment_schools;
                title = getString(R.string.nav_schools);
                break;
            case "reports":
                layoutRes = R.layout.fragment_reports;
                title = getString(R.string.nav_reports);
                break;
            case "report_detail":
                layoutRes = R.layout.fragment_report_detail;
                title = "Report Details";
                break;
            case "settings":
                layoutRes = R.layout.fragment_settings;
                title = getString(R.string.nav_settings);
                break;
            default:
                layoutRes = R.layout.fragment_dashboard;
                title = getString(R.string.dashboard_title);
                currentPage = "dashboard";
        }

        // Update Screen Title
        TextView titleView = findViewById(R.id.screen_title);
        if (titleView != null) titleView.setText(title);

        // Load Content into container
        FrameLayout container = findViewById(R.id.page_container);
        if (container != null) {
            container.removeAllViews();
            LayoutInflater.from(this).inflate(layoutRes, container, true);
            setupPageLogic(page, container);
        }
    }

    private void updateBottomNavUI(String page) {
        int activeColor = Color.parseColor("#14543A");
        int inactiveColor = Color.parseColor("#718078");

        updateNavItem(R.id.bottom_home, "dashboard".equals(page), activeColor, inactiveColor);
        updateNavItem(R.id.bottom_projects, "projects".equals(page), activeColor, inactiveColor);
        updateNavItem(R.id.bottom_schools, "schools".equals(page), activeColor, inactiveColor);
        updateNavItem(R.id.bottom_reports, "reports".equals(page) || "report_detail".equals(page), activeColor, inactiveColor);
    }

    private void updateNavItem(int id, boolean isActive, int activeColor, int inactiveColor) {
        LinearLayout item = findViewById(id);
        if (item == null) return;
        
        ImageView icon = (ImageView) item.getChildAt(0);
        TextView text = (TextView) item.getChildAt(1);
        
        int color = isActive ? activeColor : inactiveColor;
        if (icon != null) icon.setColorFilter(color);
        if (text != null) text.setTextColor(color);
    }

    private void setupPageLogic(String page, FrameLayout container) {
        switch (page) {
            case "dashboard":
                populateDashboard(container);
                break;
            
            case "projects":
                loadProjectMap(container);
                populateProjects(container, dataService.getProjects());
                setupSearch(container.findViewById(R.id.search_projects), "projects", container);
                break;

            case "schools":
                populateSchools(container, dataService.getSchools());
                setupSearch(container.findViewById(R.id.search_schools), "schools", container);
                break;

            case "reports":
                View reportItem = container.findViewById(R.id.report_mabira);
                if (reportItem != null) reportItem.setOnClickListener(v -> showPage("report_detail"));
                break;

            case "report_detail":
                populateReportDetail(container, "RPT-2048"); 
                break;
        }
    }

    private void populateDashboard(FrameLayout container) {
        SustainabilityStats stats = dataService.getStats();
        
        setText(container, R.id.stat_solar_value, String.valueOf(stats.getSolarInstallations()));
        setText(container, R.id.stat_solar_delta, stats.getSolarDelta());
        setText(container, R.id.stat_acres_value, String.format(Locale.US, "%,d", stats.getAcresReforested()));
        setText(container, R.id.stat_acres_delta, stats.getAcresDelta());
        setText(container, R.id.stat_co2_value, stats.getCo2Offset());
        setText(container, R.id.stat_co2_impact, stats.getCo2Impact());

        // Featured alert
        Report topReport = dataService.getReports().get(0);
        setText(container, R.id.alert_title, topReport.getTitle());
        String subtitle = getString(R.string.report_subtitle_format, topReport.getDistrict(), topReport.getId(), topReport.getTimestamp());
        setText(container, R.id.alert_subtitle, subtitle);
        setText(container, R.id.alert_status, topReport.getStatus());

        // Featured project
        Project topProject = dataService.getProjects().get(0);
        setText(container, R.id.featured_project_name, topProject.getName());
        ProgressBar progress = container.findViewById(R.id.featured_project_progress);
        if (progress != null) progress.setProgress(topProject.getProgress());
        setText(container, R.id.featured_project_percent, getString(R.string.percent_format, topProject.getProgress()));

        View alert = container.findViewById(R.id.report_mabira);
        if (alert != null) alert.setOnClickListener(v -> showPage("report_detail"));
    }

    private void populateProjects(FrameLayout container, List<Project> projects) {
        LinearLayout listContainer = container.findViewById(R.id.projects_list_container);
        if (listContainer == null) return;
        listContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Project p : projects) {
            View item = inflater.inflate(R.layout.item_project, listContainer, false);
            ((TextView) item.findViewById(R.id.project_name)).setText(p.getName());
            ((TextView) item.findViewById(R.id.project_details)).setText(getString(R.string.project_details_format, p.getType(), p.getLocation()));
            ((ProgressBar) item.findViewById(R.id.project_progress_bar)).setProgress(p.getProgress());
            listContainer.addView(item);
        }
    }

    private void populateSchools(FrameLayout container, List<School> schools) {
        LinearLayout listContainer = container.findViewById(R.id.schools_list_container);
        if (listContainer == null) return;
        listContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (School s : schools) {
            View item = inflater.inflate(R.layout.item_school, listContainer, false);
            ((TextView) item.findViewById(R.id.school_name)).setText(s.getName());
            ((TextView) item.findViewById(R.id.school_details)).setText(getString(R.string.school_details_format, s.getDistrict(), s.getTreeCount()));
            ((TextView) item.findViewById(R.id.school_status)).setText(s.getStatus());
            listContainer.addView(item);
        }
    }

    private void setupSearch(EditText searchEdit, String type, FrameLayout container) {
        if (searchEdit == null) return;
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase(Locale.US);
                if ("projects".equals(type)) {
                    List<Project> filtered = new ArrayList<>();
                    for (Project p : dataService.getProjects()) {
                        if (p.getName().toLowerCase(Locale.US).contains(query) || p.getLocation().toLowerCase(Locale.US).contains(query)) {
                            filtered.add(p);
                        }
                    }
                    populateProjects(container, filtered);
                } else if ("schools".equals(type)) {
                    List<School> filtered = new ArrayList<>();
                    for (School sc : dataService.getSchools()) {
                        if (sc.getName().toLowerCase(Locale.US).contains(query) || sc.getDistrict().toLowerCase(Locale.US).contains(query)) {
                            filtered.add(sc);
                        }
                    }
                    populateSchools(container, filtered);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void populateReportDetail(FrameLayout container, String reportId) {
        Report report = dataService.getReportById(reportId);
        if (report == null) return;

        View backBtn = container.findViewById(R.id.btn_back_to_reports);
        if (backBtn != null) backBtn.setOnClickListener(v -> showPage("reports"));
        
        View assignBtn = container.findViewById(R.id.assign_investigation);
        if (assignBtn != null) assignBtn.setOnClickListener(v -> 
            Toast.makeText(this, getString(R.string.toast_investigation_assigned, report.getId()), Toast.LENGTH_SHORT).show());
        
        View mapBtn = container.findViewById(R.id.view_location);
        if (mapBtn != null) mapBtn.setOnClickListener(v -> 
            Toast.makeText(this, getString(R.string.toast_opening_map, report.getLocation()), Toast.LENGTH_SHORT).show());
    }

    private void loadProjectMap(FrameLayout container) {
        WebView map = container.findViewById(R.id.project_map);
        if (map != null) {
            WebSettings settings = map.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            map.loadUrl("https://www.openstreetmap.org/export/embed.html?bbox=29.5%2C-1.8%2C35.2%2C4.3&layer=mapnik&marker=1.3733%2C32.2903");
        }
    }

    private void setText(View container, int id, String text) {
        TextView view = container.findViewById(id);
        if (view != null) view.setText(text);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("current_page", currentPage);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.getVisibility() == View.VISIBLE) {
            closeDrawer();
        } else if ("report_detail".equals(currentPage)) {
            showPage("reports");
        } else if (!"dashboard".equals(currentPage)) {
            showPage("dashboard");
        } else {
            super.onBackPressed();
        }
    }
}
