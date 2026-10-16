package com.nfcattendance.employee;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NfcScanActivity extends AppCompatActivity {
    public static final String EXTRA_MODE = "mode";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ApiClient apiClient = new ApiClient();
    private SessionManager sessionManager;
    private NfcAdapter nfcAdapter;
    private String mode;
    private boolean completed;

    private TextView scanTitle;
    private TextView scanMessage;
    private TextView timestampText;
    private Button doneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_scan);
        sessionManager = new SessionManager(this);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) mode = "CLOCK_IN";

        scanTitle = findViewById(R.id.scanTitle);
        scanMessage = findViewById(R.id.scanMessage);
        timestampText = findViewById(R.id.timestampText);
        doneButton = findViewById(R.id.doneButton);
        doneButton.setOnClickListener(v -> finish());

        scanTitle.setText("CLOCK_OUT".equals(mode) ? "סריקת יציאה" : "סריקת כניסה");
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            showFailure(getString(R.string.nfc_unsupported));
            return;
        }
        if (!nfcAdapter.isEnabled()) {
            showFailure(getString(R.string.nfc_disabled));
            return;
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null && !completed) {
            Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            IntentFilter[] filters = new IntentFilter[]{
                    new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                    new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                    new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            };
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (completed || intent == null) return;
        String action = intent.getAction();
        if (action == null) return;
        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
                && !NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                && !NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            return;
        }

        try {
            String stationCode = NfcStationParser.parseStationCode(intent);
            submitAttendance(stationCode);
        } catch (Exception e) {
            showFailure(e.getMessage() != null ? e.getMessage() : getString(R.string.nfc_read_failed));
        }
    }

    private void submitAttendance(String stationCode) {
        scanMessage.setText("נקראה תחנה " + stationCode + ". שולח לשרת...");
        String endpoint = "CLOCK_OUT".equals(mode) ? "/api/attendance/clock-out" : "/api/attendance/clock-in";
        executor.execute(() -> {
            try {
                JsonObject result = apiClient.clock(sessionManager.getToken(), endpoint, stationCode);
                String ts = result.has("serverTimestamp") ? result.get("serverTimestamp").getAsString() : Instant.now().toString();
                String local = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                        .withZone(ZoneId.of("Asia/Jerusalem"))
                        .format(Instant.parse(ts));
                runOnUiThread(() -> showSuccess(local, stationCode));
            } catch (Exception e) {
                runOnUiThread(() -> showFailure(e.getMessage()));
            }
        });
    }

    private void showSuccess(String localTimestamp, String stationCode) {
        completed = true;
        scanTitle.setText("הצלחה");
        scanMessage.setText(("CLOCK_OUT".equals(mode) ? "יציאה" : "כניסה") + " נרשמה בתחנה " + stationCode);
        timestampText.setVisibility(View.VISIBLE);
        timestampText.setText("זמן שרת: " + localTimestamp);
        doneButton.setVisibility(View.VISIBLE);
    }

    private void showFailure(String message) {
        completed = true;
        scanTitle.setText("שגיאה");
        scanMessage.setText(message);
        doneButton.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
