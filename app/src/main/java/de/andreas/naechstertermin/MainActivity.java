package de.andreas.naechstertermin;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQ_CALENDAR = 1001;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.statusText);
        Button grantButton = findViewById(R.id.grantButton);
        Button refreshButton = findViewById(R.id.refreshButton);

        grantButton.setOnClickListener(v -> requestCalendarPermission());
        refreshButton.setOnClickListener(v -> refreshWidgets());

        updateStatus();
    }

    private void requestCalendarPermission() {
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            refreshWidgets();
            updateStatus();
            return;
        }
        requestPermissions(new String[]{Manifest.permission.READ_CALENDAR}, REQ_CALENDAR);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CALENDAR) {
            updateStatus();
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                refreshWidgets();
            }
        }
    }

    private void updateStatus() {
        boolean granted = checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
        status.setText(granted
                ? "Kalenderzugriff erlaubt. Jetzt das Widget auf dem Startbildschirm hinzufügen."
                : "Bitte Kalenderzugriff erlauben, damit das Widget den nächsten Termin lesen kann.");
    }

    private void refreshWidgets() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName component = new ComponentName(this, NextEventWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        NextEventWidgetProvider.updateAll(this, manager, ids);
        status.setText("Widget aktualisiert.");
    }
}
