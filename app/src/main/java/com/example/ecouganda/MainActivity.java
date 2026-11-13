
package com.example.ecouganda;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebSettings;

public class MainActivity extends Activity {

    private String currentPage = "dashboard";
    private View drawer;
    private View scrim;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(Color.rgb(20, 84, 58));
        getWindow().getDecorView().setSystemUiVisibility(0);

        setContentView(R.layout.activity_main);

        drawer = findViewById(R.id.mobile_drawer);
        scrim = findViewById(R.id.drawer_scrim);

        if (state != null) {
            currentPage = state.getString(
                    "current_page", "dashboard");
        }

        findViewById(R.id.menu_toggle).setOnClickListener(
                v -> toggleDrawer());

        scrim.setOnClickListener(v -> closeDrawer());

        bindNavigation();
        showPage(currentPage);
    }

    private void toggleDrawer() {
        if (drawer.getVisibility() == View.VISIBLE) {
            closeDrawer();
        } else {
            drawer.setVisibility(View.VISIBLE);
            scrim.setVisibility(View.VISIBLE);
        }
    }

    private void closeDrawer() {
        drawer.setVisibility(View.GONE);
        scrim.setVisibility(View.GONE);
    }

    private void bindNavigation() {
        bind(R.id.nav_dashboard, "dashboard");
        bind(R.id.nav_projects, "projects");
        bind(R.id.nav_schools, "schools");
        bind(R.id.nav_reports, "reports");
        bind(R.id.nav_settings, "settings");

        bind(R.id.bottom_home, "dashboard");
        bind(R.id.bottom_projects, "projects");
        bind(R.id.bottom_schools, "schools");
        bind(R.id.bottom_reports, "reports");
    }

    private void bind(int id, String page) {
        View view = findViewById(id);
        view.setOnClickListener(v -> showPage(page));
    }

    private void showPage(String page) {

        currentPage = page;
        closeDrawer();

        int layout;
        String title;

        switch (page) {
            case "projects":
                layout = R.layout.fragment_projects;
                title = "Projects";
                break;

            case "schools":
                layout = R.layout.fragment_schools;
                title = "Green Schools";
                break;

            case "reports":
                layout = R.layout.fragment_reports;
                title = "Districts / Reports";
                break;

            case "report_detail":
                layout = R.layout.fragment_report_detail;
                title = "Districts / Reports";
                break;

            case "settings":
                layout = R.layout.fragment_settings;
                title = "Settings";
                break;

            default:
                layout = R.layout.fragment_dashboard;
                title = "EcoUganda Dashboard";
                currentPage = "dashboard";
        }

        TextView titleView = findViewById(R.id.screen_title);
        titleView.setText(title);

        FrameLayout container =
                findViewById(R.id.page_container);

        container.removeAllViews();

        LayoutInflater.from(this).inflate(
                layout, container, true);

        if ("projects".equals(page)) {
            loadProjectMap(container);
        }

        if ("reports".equals(page)) {
            View report = container.findViewById(
                    R.id.report_mabira);

            if (report != null) {
                report.setOnClickListener(
                        v -> showPage("report_detail"));
            }
        }
    }

    private void loadProjectMap(FrameLayout container) {

        WebView map = container.findViewById(R.id.project_map);

        if (map != null) {
            WebSettings settings = map.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);

            map.loadUrl(
                    "https://www.openstreetmap.org/export/embed.html?bbox=29.5%2C-1.8%2C35.2%2C4.3&layer=mapnik&marker=1.3733%2C32.2903"
            );
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("current_page", currentPage);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (drawer.getVisibility() == View.VISIBLE) {
            closeDrawer();
        } else if (!"dashboard".equals(currentPage)) {
            showPage("dashboard");
        } else {
            super.onBackPressed();
        }
    }
}
