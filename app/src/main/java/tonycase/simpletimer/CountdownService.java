package tonycase.simpletimer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.TimedMetaData;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

import static android.os.PowerManager.*;

/**
 * Provides background processing for our widget.  Specifically, for each running widget, this
 * has a background thread doing the counting down and updating the display.  At the end of the countdown,
 * it flashes the text (if still displayed) and plays an alarm sound.
 *
 * @author Tony Case (case.tony@gmail.com)
 *         Created on 1/13/16.
 */
public class CountdownService extends Service {

    // A thread for each widget process
    private Map<Integer, CountdownThread> timerCountdownsTable = new HashMap<>();
    // As well as a wake lock.  The lock will either be for the display, if chosen, or otherwise just the cpu.
    private Map<Integer, PowerManager.WakeLock> wakeLocks = new HashMap<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int appWidgetId = intent.getIntExtra(TimerWidgetProvider.APP_WIDGET_ID, -1);
        boolean screenOn = TimerWidgetUtils.getScreenOnForId(this, appWidgetId);

        // determine whether this countdown is starting or being stopped
        boolean starting = !timerCountdownsTable.containsKey(appWidgetId);

        if (starting) {
            // create and start Countdown thread; place entry in hashmap.
            int lengthSec = intent.getIntExtra(TimerWidgetProvider.EXTRA_TIMER_LENGTH, 0);
            Timber.d("starting countdown of %d for process %d. screenOn = %b",lengthSec,appWidgetId, screenOn);
            // Don't do anything if the length is 0 seconds.
            if (lengthSec > 0) {
                CountdownThread countdownThread = new CountdownThread(appWidgetId, lengthSec);
                timerCountdownsTable.put(appWidgetId, countdownThread);
                countdownThread.start();
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

                @SuppressWarnings("deprecation")
                PowerManager.WakeLock wakeLock = screenOn
                        // Old fashioned wake lock needed as we're not within a standard app window
                        ? powerManager.newWakeLock(SCREEN_BRIGHT_WAKE_LOCK, String.valueOf(appWidgetId))
                        : powerManager.newWakeLock(PARTIAL_WAKE_LOCK, String.valueOf(appWidgetId));
                wakeLock.acquire();
                wakeLocks.put(appWidgetId, wakeLock);
            }
        }

        if (!starting) {
            // stop countdown thread; remove entry from hashmap.
            CountdownThread thread = timerCountdownsTable.remove(appWidgetId);
            thread.stopThread();

            // get and release the wakelock
            PowerManager.WakeLock wakeLock = wakeLocks.remove(appWidgetId);
            if (wakeLock != null) {
                wakeLock.release();
            }

            // Stop the service if there are no timers
            if (timerCountdownsTable.size() == 0) {
                Timber.d("Stopping service thread and service");
                // non left, we can end service.
                stopSelf();
            }
        }

        // If we get killed, after returning from here, let it be
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // clients not allowed to bind to this service.
        return null;
    }

    // displays the new duration, formatted, in the widget.
    private void displayNewValue(int appWidgetId, int newDuration) {

        // start blinking when count down gets to zero.
        boolean blink = newDuration <= 0 && newDuration%2 == 0;

        // formatted time remaining
        String formattedDuration = formatDuration(newDuration);

        // build the UI, as a RemoteViews object
        RemoteViews updateViews = buildCountdownViews(getApplicationContext(), formattedDuration, blink);

        // Push update for this widget to the home screen
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        manager.updateAppWidget(appWidgetId, updateViews);
    }

    // Currently we support one time format:  M:SS
    private String formatDuration(int duration) {
        duration = Math.max(duration, 0);
        int seconds = duration%60;
        int minutes = duration/60;
        return String.format("%d:%02d", minutes, seconds);
    }

    // update the widgets view for the new (formatted) time remaining, and whether the time is
    // currently blinking
    private RemoteViews buildCountdownViews(Context context, String formattedDuration, boolean blink) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_face);
        views.setViewVisibility(R.id.timer_icon, View.GONE);
        views.setViewVisibility(R.id.text_top, View.GONE);
        views.setViewVisibility(R.id.text_bottom, View.GONE);
        views.setViewVisibility(R.id.textView2, View.VISIBLE);

        views.setTextViewText(R.id.textView2, formattedDuration);

        if (blink) {
            views.setViewVisibility(R.id.textView2, View.INVISIBLE);
        } else {
            views.setViewVisibility(R.id.textView2, View.VISIBLE);
        }

        return views;
    }

    // The countdown thread.  Each timer gets its own.
    class CountdownThread extends Thread {

        private static final long ONE_SECOND = 1000;

        // the widget id this thread is working for
        private final int widgetId;

        // the duration of the timer (reset to this value upon completion or reset)
        private final int resetTime;

        // current remaining time on the countdown.
        private int currentTime;

        // whether this thread should be stopped
        private boolean stopped = false;

        // the ringtone to play when the time gets to 0.
        private Ringtone ringtone;

        // We use the notification manager to send a notification *IF* the screen is currently not on.
        //  The user can then silence the alarm by tapping the notification.
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        CountdownThread(int widgetId, int resetTime) {

            this.widgetId = widgetId;
            this.resetTime = resetTime;
            this.currentTime = resetTime;
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        }

        @Override
        public void run() {
            Timber.d("thread starting");

            long sleepDuration = ONE_SECOND;
            displayNewValue(widgetId, resetTime);

            while (!stopped) {
                try {
                    Thread.sleep(sleepDuration);
                    currentTime--;
                    if (currentTime % 10 == 0) {
                        Timber.v("current time down to %d", currentTime);
                    }
                    if (currentTime == 0) {
                        Timber.d("current time is 0, playing ring town");
                        ringtone.play();

                        // if screen is off, turn it on.
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        boolean isScreenOn = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            isScreenOn = pm.isInteractive();
                        } else {
                            //noinspection deprecation
                            isScreenOn = pm.isScreenOn();
                        }
                        Timber.d("screen " + (isScreenOn ? "on" : "off"));
                        if (!isScreenOn) {
                            //new wakelock probably not needed, but I'm leaving this in here for good measure.
                            PowerManager.WakeLock wl = pm.newWakeLock(SCREEN_BRIGHT_WAKE_LOCK
                                    | ACQUIRE_CAUSES_WAKEUP, "tag");
                            wl.acquire();
                            Context context = getApplicationContext();

                            // including button control
                            Intent intent = new Intent(context, TimerWidgetProvider.class);
                            intent.setAction(TimerWidgetProvider.TIMER_EVENT);
                            intent.putExtra(TimerWidgetProvider.APP_WIDGET_ID, widgetId);
                            PendingIntent pIntent = PendingIntent.getBroadcast(context,
                                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                                    .setContentTitle(getString(R.string.time_is_up))
                                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                                    .setContentText(getString(R.string.tap_to_dismiss))
                                    .setContentIntent(pIntent)
                                    .setSmallIcon(R.drawable.hourglass4b);

                            RemoteViews view = new RemoteViews(getPackageName(), R.layout.notification_layout);
                            view.setOnClickPendingIntent(R.id.notification_container, pIntent);
                            builder.setContent(view);

                            Notification noti = builder.build();
                            noti.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_LOCAL_ONLY;
                            // id unique to second of the week, so separate notifications appear
                            int id = widgetId;
                            Timber.v("sending notification");
                            notificationManager.notify(id, noti);
                        }
                    }
                    if (currentTime <= 0) {
                        sleepDuration = 250;
                    }
                    displayNewValue(widgetId, currentTime);
                    if (currentTime <= -60) {    // plays alarm for 15 seconds, unless stopped
                        Timber.d("stopping alarm");
                        ringtone.stop();
                        stopped = true;
                        notificationManager.cancel(widgetId);

                        TimerWidgetUtils.buildLabelViews(getApplicationContext(), widgetId, resetTime);
                    }
                } catch (InterruptedException e) {
                    Timber.i("timer thread interupted");
                }
            }
        }

        public void stopThread() {
            Timber.v("stop thread");
            TimerWidgetUtils.buildLabelViews(getApplicationContext(), widgetId, resetTime);
            ringtone.stop();
            stopped = true;
            interrupt();
            notificationManager.cancel(widgetId);
        }
    }
}
