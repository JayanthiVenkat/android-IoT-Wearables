package com.locationmonitor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class ConfigActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView goBtn;
    private EditText radiusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        goBtn = (TextView) findViewById(R.id.go);
        radiusView = (EditText) findViewById(R.id.radius);
        goBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.go) {
            Intent i = new Intent(ConfigActivity.this, LocationTrackerActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("radius", radiusView.getText().toString());
            i.putExtras(bundle);
            i.setType("normal");
            startActivity(i);
            finish();
        }

    }
}
