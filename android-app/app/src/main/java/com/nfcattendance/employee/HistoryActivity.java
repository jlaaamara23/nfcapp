package com.nfcattendance.employee;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ApiClient apiClient = new ApiClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        RecyclerView list = findViewById(R.id.historyList);
        list.setLayoutManager(new LinearLayoutManager(this));
        HistoryAdapter adapter = new HistoryAdapter();
        list.setAdapter(adapter);

        SessionManager sessionManager = new SessionManager(this);
        executor.execute(() -> {
            try {
                List<JsonObject> rows = apiClient.history(sessionManager.getToken());
                runOnUiThread(() -> adapter.setItems(rows));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        private final List<JsonObject> items = new ArrayList<>();
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.of("Asia/Jerusalem"));

        void setItems(List<JsonObject> rows) {
            items.clear();
            items.addAll(rows);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            JsonObject item = items.get(position);
            String type = item.get("eventType").getAsString();
            holder.eventType.setText("CLOCK_IN".equals(type) ? "כניסה" : "יציאה");
            String ts = fmt.format(Instant.parse(item.get("serverTimestamp").getAsString()));
            String station = item.has("stationCode") && !item.get("stationCode").isJsonNull()
                    ? item.get("stationCode").getAsString() : "-";
            holder.eventMeta.setText(ts + " | תחנה " + station + " | " + item.get("status").getAsString());
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView eventType;
            final TextView eventMeta;

            VH(@NonNull View itemView) {
                super(itemView);
                eventType = itemView.findViewById(R.id.eventType);
                eventMeta = itemView.findViewById(R.id.eventMeta);
            }
        }
    }
}
