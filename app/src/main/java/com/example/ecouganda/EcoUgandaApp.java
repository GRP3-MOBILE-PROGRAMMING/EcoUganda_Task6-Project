package com.example.ecouganda;

import android.app.Application;

import com.example.ecouganda.performance.PerfMonitor;

public class EcoUgandaApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PerfMonitor.init(this, BuildConfig.PERF_COLLECTION_ENABLED);
    }
}
