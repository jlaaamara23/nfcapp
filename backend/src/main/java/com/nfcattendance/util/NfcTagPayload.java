package com.nfcattendance.util;

public final class NfcTagPayload {
    public static final String DEFAULT_EMPLOYEE_WEB_URL = "https://nfcapp-nine.vercel.app";

    private NfcTagPayload() {}

    /**
     * One NDEF URI for both phones. iPhone opens the https site; Galaxy Chrome and the
     * Android app still parse station from the same string (including the app URI in the hash).
     */
    public static String unified(String employeeWebUrl, String stationCode, String registrationValue) {
        String site = employeeWebUrl == null || employeeWebUrl.isBlank()
                ? DEFAULT_EMPLOYEE_WEB_URL
                : employeeWebUrl.trim().replaceAll("/+$", "");
        String code = stationCode.trim().toUpperCase();
        String app = "nfcattendance://station/" + code;
        if (registrationValue != null && !registrationValue.isBlank()) {
            app += "?rv=" + registrationValue.trim();
        }
        return site + "/?station=" + code + "#" + app;
    }
}
