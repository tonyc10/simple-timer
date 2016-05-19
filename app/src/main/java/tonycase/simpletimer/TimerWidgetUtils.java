package tonycase.simpletimer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import timber.log.Timber;

/**
 * Common utilities for working with the Timer Widget.
 *
 * @author Tony Case (case.tony@gmail.com)
 *         Created on 1/14/16.
 */
public final class TimerWidgetUtils {

    // A key for preferences, for whether to keep screen lit during count down.
    private static final String KEEP_SCREEN_ON = "keep_screen_on";
    private static final String DURATION = "duration";

    /** Provides a text string for the static widget label, based on the duration in seconds.  Simplifies
     * common times, e.g. 120 seconds becomes "2 Minutes", but leaved complex times in "1:23:45" format */
    public static String[] formatDurationForLabel(int durationSec) {

        int sec = durationSec%60;
        int minutes = durationSec/60;

        if (minutes == 1 && sec == 0) {
            return new String[] {"1", "Minute"};
        }
        else if (minutes > 0 && sec == 0) {
            return new String[] {String.valueOf(durationSec/60), "Minutes"};
        }
        else if (durationSec == 1) {
            return new String[] {"1", "Second"};
        }
        else if (minutes < 3) {
            return new String[] {String.valueOf(durationSec), "Seconds"};
        }
        else {
            return new String[] {"Timer", String.format("%d:%02d", minutes, sec)};
        }
    }

    /**
     * Sets timer widget text field to correct value, and attaches a pending intent to the widget view's
     * onClick.
     *
     * @param context  the current context.
     * @param appWidgetId  the id of the specific widget instance.
     * @param duration  the duration in seconds of the timer
     */
    public static void buildLabelViews(Context context, int appWidgetId, int duration) {
        String[] widgetLabel = TimerWidgetUtils.formatDurationForLabel(duration);

        // set the initial view
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget_face);
        remoteViews.setViewVisibility(R.id.textView2, View.GONE);
        remoteViews.setViewVisibility(R.id.text_top, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.text_bottom, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.timer_icon, View.VISIBLE);

        remoteViews.setTextViewText(R.id.text_top, widgetLabel[0]);
        remoteViews.setTextViewText(R.id.text_bottom, widgetLabel[1]);

        // including button control
        Intent intent = new Intent(context, TimerWidgetProvider.class);
        intent.setAction(TimerWidgetProvider.TIMER_EVENT);
        intent.putExtra(TimerWidgetProvider.EXTRA_TIMER_LENGTH, duration);
        intent.putExtra(TimerWidgetProvider.APP_WIDGET_ID, appWidgetId);

        // current widget only
        int[] idAsArray = new int[] {appWidgetId};
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idAsArray);
        int uniqueRequestCode = appWidgetId;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                uniqueRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Timber.d("setting click event with appWidget Id of " + appWidgetId);
        remoteViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    private static String key(String base, int appWidgetId) {
        return String.format("%s_%d", base, appWidgetId);
    }

    public static void persistDurationForId(Context context, int appWidgetId, int durationSec) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(key(DURATION, appWidgetId), durationSec)
                .apply();
    }

    public static int getDurationForId(Context context, int appWidgetId) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(key(DURATION, appWidgetId), -1);
    }

    public static void persistScreenOnForId(Context context, int appWidgetId, boolean screenOn) {
        PreferenceManager.getDefaultSharedPreferences(context)
                         .edit()
                         .putBoolean(key(KEEP_SCREEN_ON, appWidgetId), screenOn)
                         .apply();
    }

    public static boolean getScreenOnForId(Context context, int appWidgetId) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                                .getBoolean(key(KEEP_SCREEN_ON, appWidgetId), false);
    }

    public static void deleteId(Context context, int appWidgetId) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .remove(key(KEEP_SCREEN_ON, appWidgetId))
                .remove(key(DURATION, appWidgetId))
                .apply();
    }
}
