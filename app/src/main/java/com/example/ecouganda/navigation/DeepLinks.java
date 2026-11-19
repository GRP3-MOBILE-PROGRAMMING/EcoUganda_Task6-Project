package com.example.ecouganda.navigation;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Deep link contract shared by the app, push notifications and the tests.
 *
 * <pre>
 *   ecouganda://reports/RPT-2041   opens ReportsFragment with reportId = "RPT-2041"
 *   ecouganda://reports            opens the reports list
 * </pre>
 *
 * Pure Java (no android.net.Uri) so it can be unit-tested on the build server.
 */
public final class DeepLinks {

    public static final String SCHEME = "ecouganda";
    public static final String HOST_REPORTS = "reports";

    /** Intent extra / fragment argument key. FCM data messages use the same key. */
    public static final String EXTRA_REPORT_ID = "reportId";

    private static final Pattern REPORT_ID = Pattern.compile("RPT-\\d{1,8}");

    private DeepLinks() { }

    /** URI placed in the notification's PendingIntent. */
    public static String reportUri(String reportId) {
        return SCHEME + "://" + HOST_REPORTS + "/" + reportId;
    }

    /** True when the link targets the reports section (with or without an id). */
    public static boolean isReportsLink(String scheme, String host) {
        return scheme != null && host != null
                && SCHEME.equalsIgnoreCase(scheme)
                && HOST_REPORTS.equalsIgnoreCase(host);
    }

    /**
     * Extracts the report id from the parts of a deep link URI.
     *
     * @return normalised id such as "RPT-2041", or null when the link is not a
     *         report link or the id is malformed
     */
    public static String parseReportId(String scheme, String host, String path) {
        if (!isReportsLink(scheme, host) || path == null) return null;
        String id = path;
        while (id.startsWith("/")) id = id.substring(1);
        int slash = id.indexOf('/');
        if (slash >= 0) id = id.substring(0, slash);
        return normaliseReportId(id);
    }

    /** Validates an id from an intent extra or FCM data payload. */
    public static String normaliseReportId(String raw) {
        if (raw == null) return null;
        String id = raw.trim().toUpperCase(Locale.US);
        return REPORT_ID.matcher(id).matches() ? id : null;
    }
}
