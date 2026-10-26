package com.nfcattendance.employee;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.autofill.AutofillManager;
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
    private EditText usernameInput;
    private EditText passwordInput;

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

        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        TextView errorText = findViewById(R.id.errorText);
        Button loginButton = findViewById(R.id.loginButton);

        disableAutofill();
        clearCredentials();

        loginButton.setOnClickListener(v -> {
            errorText.setVisibility(View.GONE);
            loginButton.setEnabled(false);
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            if (username.isEmpty() || password.isEmpty()) {
                errorText.setText("יש למלא שם משתמש וסיסמה");
                errorText.setVisibility(View.VISIBLE);
                loginButton.setEnabled(true);
                return;
            }
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
    protected void onResume() {
        super.onResume();
        clearCredentials();
        usernameInput.post(this::clearCredentials);
    }

    private void clearCredentials() {
        if (usernameInput != null) {
            usernameInput.setText("");
        }
        if (passwordInput != null) {
            passwordInput.setText("");
        }
    }

    private void disableAutofill() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
            AutofillManager autofillManager = getSystemService(AutofillManager.class);
            if (autofillManager != null) {
                autofillManager.cancel();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
