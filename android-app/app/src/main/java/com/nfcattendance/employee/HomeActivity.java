package com.nfcattendance.employee;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ApiClient apiClient = new ApiClient();
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_home);

        TextView greetingText = findViewById(R.id.greetingText);
        TextView todayStatusText = findViewById(R.id.todayStatusText);
        TextView hoursText = findViewById(R.id.hoursText);
        greetingText.setText("שלום, " + sessionManager.getFullName());

        findViewById(R.id.clockInButton).setOnClickListener(v -> openScan("CLOCK_IN"));
        findViewById(R.id.clockOutButton).setOnClickListener(v -> openScan("CLOCK_OUT"));
        findViewById(R.id.historyButton).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.profileButton).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            sessionManager.clear();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        executor.execute(() -> {
            try {
                JsonObject today = apiClient.today(sessionManager.getToken());
                String label = today.has("statusLabel") ? today.get("statusLabel").getAsString() : "";
                int minutes = today.has("workedMinutesToday") ? today.get("workedMinutesToday").getAsInt() : 0;
                runOnUiThread(() -> {
                    todayStatusText.setText("סטטוס היום: " + label);
                    hoursText.setText("שעות היום: " + (minutes / 60) + ":" + String.format("%02d", minutes % 60));
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void openScan(String mode) {
        Intent intent = new Intent(this, NfcScanActivity.class);
        intent.putExtra(NfcScanActivity.EXTRA_MODE, mode);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
