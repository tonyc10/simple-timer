package tonycase.simpletimer;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import timber.log.Timber;

/**
 * This class is the broadcast receiver (AppWidgetProvider) for our widget.  When the widget is selected,
 * an intent is fired and we catch it here.  We pass it off to our service to do the countdown, etc.
 *
 * @author Tony Case (case.tony@gmail.com)
 *         Created on 1/12/16.
 */
public class TimerWidgetProvider extends AppWidgetProvider {

    public static final String APP_WIDGET_ID = "app_widget_id";
    public static final String EXTRA_TIMER_LENGTH = "extra_timer_length";

    public static final String TIMER_EVENT = "start_timer";

    /** Capture non-framework broadcast events from our widget (e.g. button press events) */
    @Override
    public void onReceive(Context context, Intent intent) {

        Timber.d("onReceive: " + intent);
        Timber.d("extras: " + intent.getExtras());
        if (intent.getAction().equals(TIMER_EVENT)) {
            timerEvent(context, intent);
        } else {
            super.onReceive(context, intent);
        }
    }

    /** Whenever app is installed or updated.  We don't implement updates, so really just when app is
     * installed. */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int count = appWidgetIds.length;

        for (int i = 0; i < count; i++) {
            int widgetId = appWidgetIds[i];
            int duration = TimerWidgetUtils.getDurationForId(context, widgetId);
            if (duration > 0) {
                TimerWidgetUtils.buildLabelViews(context, widgetId, duration);
            }
        }
    }

    private void timerEvent(Context context, Intent intent) {

        Timber.d("timer event for " + intent.getIntExtra(APP_WIDGET_ID, -1));;

        // Call Service to handle the event.
        Intent serviceIntent = new Intent(context, CountdownService.class);
        serviceIntent.putExtra(APP_WIDGET_ID, intent.getIntExtra(APP_WIDGET_ID, -1));
        serviceIntent.putExtra(EXTRA_TIMER_LENGTH, intent.getIntExtra(EXTRA_TIMER_LENGTH, 0));
        context.startService(serviceIntent);
    }
}
