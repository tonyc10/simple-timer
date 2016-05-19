package tonycase.simpletimer;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * As indicated in the manifest, this activity is launched whenever the widget is installed (on the
 * homescreen) and needs to be configured.
 *
 * The result of a successful installation (done pressed) is an Intent is created, to be thrown
 * (and caught by our receiver) whenever the widget on the homescreen is selected.
 */
public class WidgetConfigureActivity extends AppCompatActivity {

    // the Id of this installation.
    private int appWidgetId;

    // Views
    @Bind(R.id.edittext_seconds) EditText secondsTF;
    @Bind(R.id.edittext_minutes) EditText minutesTF;
    @Bind(R.id.checkbox_display_on) CheckBox screenOnCB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_configure);
        ButterKnife.bind(this);

        setTitle("Timer Settings");

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }


        // set result to cancelled in case user backs out before being done.  This will cause the widget
        // to not be installed on the homescreen.
        setResult(RESULT_CANCELED, null);
    }

    // done pressed
    @OnClick(R.id.button_done) void doneButtonPressed() {

        Timber.d("Done pressed, configuring widgetId " + appWidgetId);

        // screenOn determines whether to use a Display On Wakelock, or just a CPU wakelock
        boolean screenOn = screenOnCB.isChecked();

        TimerWidgetUtils.persistScreenOnForId(this, appWidgetId, screenOn);

        String minutesStr = minutesTF.getText().toString().trim();
        int minutes = minutesStr.length() > 0
                ? Integer.parseInt(minutesStr)
                : 0;
        String secondsStr = secondsTF.getText().toString().trim();
        int seconds = secondsStr.length() > 0
                ? Integer.parseInt(secondsStr)
                : 0;

        int durationSec = minutes*60 + seconds;
        TimerWidgetUtils.buildLabelViews(this, appWidgetId, durationSec);
        TimerWidgetUtils.persistDurationForId(this, appWidgetId, durationSec);

        // set result and finish activity
        Timber.i("Creating widget intent, with appId = %d, screenOn = %b", appWidgetId, screenOn);
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
