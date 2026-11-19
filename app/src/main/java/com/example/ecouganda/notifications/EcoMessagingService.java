package com.example.ecouganda.notifications;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.ecouganda.navigation.DeepLinks;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Receives Firebase Cloud Messaging pushes for new field reports.
 *
 * Send a data message such as
 * <pre>
 * { "data": { "reportId": "RPT-2041",
 *             "title": "Illegal dumping near wetland",
 *             "body": "Wakiso District - needs review" } }
 * </pre>
 * Data messages always reach onMessageReceived, so the notification is built by
 * ReportNotifications and tapping it opens ReportsFragment for that report.
 * (If a "notification" payload is sent while the app is in the background, the
 * system shows it and MainActivity still reads the "reportId" extra.)
 */
public class EcoMessagingService extends FirebaseMessagingService {

    private static final String TAG = "EcoMessaging";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();
        String reportId = data.get(DeepLinks.EXTRA_REPORT_ID);
        if (reportId == null) return;

        String title = data.get("title");
        String body = data.get("body");
        RemoteMessage.Notification n = message.getNotification();
        if (title == null && n != null) title = n.getTitle();
        if (body == null && n != null) body = n.getBody();
        if (title == null) title = "New field report " + reportId;
        if (body == null) body = "Tap to open the report";

        if (!ReportNotifications.show(this, reportId, title, body)) {
            Log.w(TAG, "Report notification not shown (invalid id or notifications disabled)");
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        // A backend would store this token to target this officer's device.
        Log.i(TAG, "FCM registration token refreshed");
    }
}
