package com.nfcattendance.employee;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();
    private final String baseUrl;

    public ApiClient() {
        this.baseUrl = BuildConfig.API_BASE_URL;
    }

    public JsonObject login(String username, String password) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/auth/login")
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        return executeJson(request);
    }

    public JsonObject me(String token) throws IOException {
        return executeJson(authGet("/api/auth/me", token));
    }

    public JsonObject today(String token) throws IOException {
        return executeJson(authGet("/api/attendance/me/today", token));
    }

    public List<JsonObject> history(String token) throws IOException {
        Request request = authGet("/api/attendance/me", token);
        try (Response response = client.newCall(request).execute()) {
            String payload = response.body() != null ? response.body().string() : "[]";
            if (!response.isSuccessful()) {
                throw new IOException(extractMessage(payload, response.code()));
            }
            Type type = new TypeToken<List<JsonObject>>() {}.getType();
            List<JsonObject> list = gson.fromJson(payload, type);
            return list != null ? list : Collections.emptyList();
        }
    }

    public JsonObject clock(String token, String endpoint, String stationCode) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("stationCode", stationCode);
        body.addProperty("clientRequestId", UUID.randomUUID().toString());
        Request request = new Request.Builder()
                .url(baseUrl + endpoint)
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        return executeJson(request);
    }

    private Request authGet(String path, String token) {
        return new Request.Builder()
                .url(baseUrl + path)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();
    }

    private JsonObject executeJson(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            String payload = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                throw new IOException(extractMessage(payload, response.code()));
            }
            return gson.fromJson(payload, JsonObject.class);
        }
    }

    private String extractMessage(String payload, int code) {
        try {
            JsonObject obj = gson.fromJson(payload, JsonObject.class);
            if (obj != null && obj.has("message")) {
                return obj.get("message").getAsString();
            }
        } catch (Exception ignored) {
        }
        return "שגיאת שרת (" + code + ")";
    }
}
