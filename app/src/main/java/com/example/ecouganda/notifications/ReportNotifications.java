package com.example.ecouganda.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.ecouganda.MainActivity;
import com.example.ecouganda.R;
import com.example.ecouganda.navigation.DeepLinks;

/**
 * Builds the "new field report" notification. Tapping it opens
 * ReportsFragment for that report through the deep link ecouganda://reports/{id}.
 *
 * The push service (EcoMessagingService) and the instrumented tests both use
 * this class, so the tests exercise exactly the intent a real push produces.
 */
public final class ReportNotifications {

    public static final String CHANNEL_ID = "field_reports";

    private ReportNotifications() { }

    /** The intent carried by the notification's PendingIntent. */
    public static Intent buildIntent(Context context, String reportId) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinks.reportUri(reportId)));
        intent.setClass(context, MainActivity.class);
        intent.putExtra(DeepLinks.EXTRA_REPORT_ID, reportId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.notification_channel_reports),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.notification_channel_reports_desc));
        nm.createNotificationChannel(channel);
    }

    /** Whether the app may post notifications (runtime permission on API 33+). */
    public static boolean canPost(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * Shows the notification. Returns false (and posts nothing) if the id is
     * invalid or notifications are not permitted.
     */
    public static boolean show(Context context, String rawReportId, String title, String body) {
        String reportId = DeepLinks.normaliseReportId(rawReportId);
        if (reportId == null || !canPost(context)) return false;
        ensureChannel(context);

        PendingIntent tap = PendingIntent.getActivity(
                context,
                reportId.hashCode(),
                buildIntent(context, reportId),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_reports)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(tap);

        try {
            NotificationManagerCompat.from(context).notify(reportId.hashCode(), builder.build());
            return true;
        } catch (SecurityException e) {
            return false; // permission revoked between the check and the post
        }
    }
}
