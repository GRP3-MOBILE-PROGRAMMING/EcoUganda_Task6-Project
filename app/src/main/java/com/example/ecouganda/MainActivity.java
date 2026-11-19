package com.example.ecouganda;

import android.content.Intent;
import android.net.Uri;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.example.ecouganda.models.Project;
import com.example.ecouganda.models.Report;
import com.example.ecouganda.models.School;
import com.example.ecouganda.models.SustainabilityStats;
import com.example.ecouganda.navigation.DeepLinks;
import com.example.ecouganda.services.DataService;
import com.example.ecouganda.ui.ReportsFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Main Activity for the EcoUganda application.
 * Handles navigation between screens and coordinates data between models and views.
 *
 * Navigation drawer: on phones it is hidden and opened from the menu button;
 * on tablets (layout-sw600dp) it is a permanent sidebar. The selected item is
 * highlighted and the toolbar title follows the current page.
 *
 * Deep links: ecouganda://reports/{reportId} (from push notifications) opens
 * ReportsFragment with that reportId.
 *
 * State preservation: current page, report id and drawer state survive rotation.
 */
public class MainActivity extends FragmentActivity implements ReportsFragment.Host {

    static final String STATE_PAGE = "current_page";
    static final String STATE_REPORT_ID = "current_report_id";
    static final String STATE_DRAWER_OPEN = "drawer_open";

    private String currentPage = "dashboard";
    private String currentReportId;
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

        boolean reopenDrawer = false;
        if (state != null) {
            // Configuration change (rotation) or process restore
            currentPage = state.getString(STATE_PAGE, "dashboard");
            currentReportId = state.getString(STATE_REPORT_ID);
            reopenDrawer = state.getBoolean(STATE_DRAWER_OPEN, false);
        } else {
            // Fresh launch: honour a deep link / notification tap
            String deepLinkId = reportIdFrom(getIntent());
            if (deepLinkId != null) {
                currentPage = "report_detail";
                currentReportId = deepLinkId;
            } else if (isReportsListLink(getIntent())) {
                currentPage = "reports";
            }
        }

        // Setup Drawer Toggles
        View menuToggle = findViewById(R.id.menu_toggle);
        if (menuToggle != null) menuToggle.setOnClickListener(v -> toggleDrawer());
        if (scrim != null) scrim.setOnClickListener(v -> closeDrawer());

        bindNavigation();
        showPage(currentPage);
        if (reopenDrawer) openDrawer();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String reportId = reportIdFrom(intent);
        if (reportId != null) {
            openReport(reportId);
        } else if (isReportsListLink(intent)) {
            showPage("reports");
        }
    }

    /** Report id from ecouganda://reports/{id} or from the "reportId" extra (FCM tray tap). */
    static String reportIdFrom(Intent intent) {
        if (intent == null) return null;
        Uri data = intent.getData();
        if (data != null) {
            String id = DeepLinks.parseReportId(data.getScheme(), data.getHost(), data.getPath());
            if (id != null) return id;
        }
        return DeepLinks.normaliseReportId(intent.getStringExtra(DeepLinks.EXTRA_REPORT_ID));
    }

    private static boolean isReportsListLink(Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        return data != null && DeepLinks.isReportsLink(data.getScheme(), data.getHost());
    }

    /** True on tablets (sw600dp), where the drawer is a permanent sidebar. */
    public boolean isPermanentDrawer() {
        return getResources().getBoolean(R.bool.is_tablet);
    }

    private void toggleDrawer() {
        if (drawer != null) {
            if (drawer.getVisibility() == View.VISIBLE) {
                closeDrawer();
            } else {
                openDrawer();
            }
        }
    }

    private void openDrawer() {
        if (drawer == null) return; // tablets: sidebar is always shown
        drawer.setVisibility(View.VISIBLE);
        if (scrim != null) scrim.setVisibility(View.VISIBLE);
    }

    private boolean isDrawerOpen() {
        return drawer != null && drawer.getVisibility() == View.VISIBLE;
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

    @Override
    public void openReport(String reportId) {
        String id = DeepLinks.normaliseReportId(reportId);
        if (id == null || dataService.getReportById(id) == null) {
            Toast.makeText(this, getString(R.string.report_not_found, String.valueOf(reportId)),
                    Toast.LENGTH_SHORT).show();
            showPage("reports");
            return;
        }
        currentReportId = id;
        showPage("report_detail");
    }

    @Override
    public void showReportsList() {
        showPage("reports");
    }

    private void showPage(String page) {
        if ("report_detail".equals(page)
                && (currentReportId == null || dataService.getReportById(currentReportId) == null)) {
            page = "reports"; // restored/deep-linked id no longer exists
        }
        currentPage = page;
        if (!"report_detail".equals(page)) currentReportId = null;
        closeDrawer();

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
                title = getString(R.string.report_detail_title);
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

        updateBottomNavUI(currentPage);
        updateDrawerHighlight(currentPage);

        // Update Screen Title
        TextView titleView = findViewById(R.id.screen_title);
        if (titleView != null) titleView.setText(title);

        FrameLayout container = findViewById(R.id.page_container);
        if (container == null) return;

        FragmentManager fm = getSupportFragmentManager();
        Fragment existing = fm.findFragmentById(R.id.page_container);

        if ("reports".equals(page) || "report_detail".equals(page)) {
            // Reports is a real Fragment so the reportId travels as a fragment argument.
            String wanted = "report_detail".equals(page) ? currentReportId : null;
            if (existing instanceof ReportsFragment
                    && Objects.equals(((ReportsFragment) existing).getReportId(), wanted)) {
                return; // already showing it (e.g. restored by the FragmentManager after rotation)
            }
            if (!(existing instanceof ReportsFragment)) {
                container.removeAllViews(); // views of a layout-based page; fragments clean up themselves
            }
            fm.beginTransaction()
                    .replace(R.id.page_container, ReportsFragment.newInstance(wanted), ReportsFragment.TAG)
                    .commitNow();
            return;
        }

        // Other pages are plain layouts inflated into the container
        if (existing != null) {
            fm.beginTransaction().remove(existing).commitNow();
        }
        container.removeAllViews();
        LayoutInflater.from(this).inflate(layoutRes, container, true);
        setupPageLogic(page, container);
    }

    /** Highlights the drawer item for the current page (both phone drawer and tablet sidebar). */
    private void updateDrawerHighlight(String page) {
        String key = "report_detail".equals(page) ? "reports" : page;
        highlight(R.id.nav_dashboard, "dashboard".equals(key));
        highlight(R.id.nav_projects, "projects".equals(key));
        highlight(R.id.nav_schools, "schools".equals(key));
        highlight(R.id.nav_reports, "reports".equals(key));
        highlight(R.id.nav_settings, "settings".equals(key));
    }

    private void highlight(int id, boolean selected) {
        View item = findViewById(id);
        if (item == null) return;
        item.setSelected(selected);
        item.setActivated(selected);
        item.setBackgroundColor(selected ? getColor(R.color.eco_green_soft) : Color.TRANSPARENT);
        if (item instanceof TextView) {
            ((TextView) item).setTextColor(getColor(selected ? R.color.eco_green : R.color.eco_text));
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
        if (alert != null) alert.setOnClickListener(v -> openReport(topReport.getId()));
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
        outState.putString(STATE_PAGE, currentPage);
        outState.putString(STATE_REPORT_ID, currentReportId);
        outState.putBoolean(STATE_DRAWER_OPEN, isDrawerOpen());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (isDrawerOpen()) {
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
