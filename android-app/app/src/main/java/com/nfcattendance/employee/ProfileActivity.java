package com.nfcattendance.employee;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ApiClient apiClient = new ApiClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        TextView profileText = findViewById(R.id.profileText);
        SessionManager sessionManager = new SessionManager(this);

        executor.execute(() -> {
            try {
                JsonObject me = apiClient.me(sessionManager.getToken());
                String text = "שם: " + safe(me, "fullName") + "\n"
                        + "מספר עובד: " + safe(me, "employeeNumber") + "\n"
                        + "אימייל: " + safe(me, "email") + "\n"
                        + "מחלקה: " + safe(me, "department") + "\n"
                        + "תפקיד: " + safe(me, "jobTitle") + "\n"
                        + "סטטוס: " + safe(me, "employmentStatus");
                runOnUiThread(() -> profileText.setText(text));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private String safe(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "-";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
