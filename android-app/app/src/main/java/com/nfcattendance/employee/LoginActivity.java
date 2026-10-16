package com.nfcattendance.employee;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ApiClient apiClient = new ApiClient();
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_login);

        EditText usernameInput = findViewById(R.id.usernameInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        TextView errorText = findViewById(R.id.errorText);
        Button loginButton = findViewById(R.id.loginButton);

        usernameInput.setText("employee1");
        passwordInput.setText("Employee123!");

        loginButton.setOnClickListener(v -> {
            errorText.setVisibility(View.GONE);
            loginButton.setEnabled(false);
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            executor.execute(() -> {
                try {
                    JsonObject auth = apiClient.login(username, password);
                    boolean isEmployee = auth.has("roles") && auth.getAsJsonArray("roles").toString().contains("EMPLOYEE");
                    boolean isAdminOnly = auth.has("roles")
                            && auth.getAsJsonArray("roles").toString().contains("ADMIN")
                            && !isEmployee;
                    if (isAdminOnly) {
                        throw new IllegalStateException("התחברו עם חשבון עובד באפליקציה זו");
                    }
                    Long employeeId = auth.has("employeeId") && !auth.get("employeeId").isJsonNull()
                            ? auth.get("employeeId").getAsLong() : null;
                    if (employeeId == null) {
                        throw new IllegalStateException("אין פרופיל עובד מקושר למשתמש");
                    }
                    sessionManager.saveSession(
                            auth.get("accessToken").getAsString(),
                            auth.get("username").getAsString(),
                            auth.has("fullName") ? auth.get("fullName").getAsString() : username,
                            employeeId
                    );
                    runOnUiThread(() -> {
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        errorText.setText(e.getMessage());
                        errorText.setVisibility(View.VISIBLE);
                        loginButton.setEnabled(true);
                    });
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
