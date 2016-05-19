package tonycase.simpletimer;

import android.app.Application;
import android.util.Log;

import timber.log.Timber;

/**
 * @author Tony Case (case.tony@gmail.com)
 *         Created on 1/13/16.
 */
public class TimerWidgetApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }
    }

    // Adapted from Timber example

    /**
     * A tree which logs important information for crash reporting.
     */
    private static class CrashReportingTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {

            switch (priority) {
                case Log.VERBOSE:
                case Log.DEBUG:
                    return;
                case Log.INFO:
                    Log.i(tag, message);
                    break;
                case Log.WARN:
                    Log.w(tag, message, t);
                    break;
                case Log.ERROR:
                    Log.e(tag, message, t);
                    break;
            }
        }
    }
}

