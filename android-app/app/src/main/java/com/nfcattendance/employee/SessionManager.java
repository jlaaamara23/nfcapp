package com.nfcattendance.employee;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREFS = "nfc_employee_session";
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, String username, String fullName, Long employeeId) {
        prefs.edit()
                .putString("token", token)
                .putString("username", username)
                .putString("fullName", fullName)
                .putLong("employeeId", employeeId != null ? employeeId : -1L)
                .apply();
    }

    public String getToken() {
        return prefs.getString("token", null);
    }

    public String getUsername() {
        return prefs.getString("username", "");
    }

    public String getFullName() {
        return prefs.getString("fullName", "");
    }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
