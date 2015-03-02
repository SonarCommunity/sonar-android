package org.example;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ExampleActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView text = new TextView(this);
        text.setText("Hello World");
        setContentView(text);
    }

    public void notCoveredByTests() {
    }

}
