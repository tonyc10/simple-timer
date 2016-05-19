package tonycase.simpletimer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * The Main Activity is opened only when app is installed, and if, I supposed, User selected Simple Timer
 * from list of applications.  All this does is present a very simple set of instructions.  The real
 * action occurs when the user select to install the widget.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View dismissView = findViewById(R.id.dismiss_button);
        dismissView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                finish();
            }
        });
    }
}
