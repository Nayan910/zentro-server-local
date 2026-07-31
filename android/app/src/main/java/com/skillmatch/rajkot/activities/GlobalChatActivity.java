package com.skillmatch.rajkot.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.skillmatch.rajkot.R;
import com.skillmatch.rajkot.adapters.GlobalChatAdapter;
import com.skillmatch.rajkot.api.ApiClient;
import com.skillmatch.rajkot.utils.PrefsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class GlobalChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private TextView titleBar;
    private GlobalChatAdapter adapter;
    private ApiClient apiClient;
    private WebSocket webSocket;
    private Handler mainHandler;
    private List<Map<String, Object>> messages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_chat);

        apiClient = ApiClient.getInstance(this);
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        setupRecyclerView();
        connectWebSocket();
        loadMessages();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewMessages);
        messageInput = findViewById(R.id.editTextMessage);
        sendButton = findViewById(R.id.buttonSend);
        titleBar = findViewById(R.id.textViewTitle);

        titleBar.setText("Global Chat");

        sendButton.setOnClickListener(v -> sendMessage());

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new GlobalChatAdapter(messages, apiClient.getJwtToken());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    private void connectWebSocket() {
        try {
            String serverUrl = apiClient.getServerUrl();
            String wsUrl = serverUrl.replace("http", "ws") + "/ws?token=" + apiClient.getJwtToken();

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(wsUrl)
                    .build();

            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    mainHandler.post(() -> {
                        Toast.makeText(GlobalChatActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    mainHandler.post(() -> {
                        try {
                            org.json.JSONObject json = new org.json.JSONObject(text);
                            String type = json.getString("type");

                            if ("global_chat".equals(type)) {
                                org.json.JSONObject messageData = json.getJSONObject("message");
                                addMessageToChat(messageData);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    mainHandler.post(() -> {
                        Toast.makeText(GlobalChatActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    mainHandler.post(() -> {
                        Toast.makeText(GlobalChatActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMessages() {
        apiClient.getApi().getGlobalMessages(apiClient.getJwtToken()).enqueue(new retrofit2.Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Map<String, Object>>> call, retrofit2.Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messages.clear();
                    messages.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(GlobalChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        if (content.isEmpty()) return;

        try {
            org.json.JSONObject json = new org.json.JSONObject();
            json.put("type", "global_chat");
            json.put("content", content);
            webSocket.send(json.toString());
            messageInput.setText("");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
        }
    }

    private void addMessageToChat(org.json.JSONObject messageData) {
        try {
            Map<String, Object> message = new java.util.HashMap<>();
            message.put("id", messageData.getLong("id"));
            message.put("senderId", messageData.getLong("senderId"));
            message.put("senderName", messageData.getString("senderName"));
            message.put("content", messageData.getString("content"));
            message.put("timestamp", messageData.getString("timestamp"));

            messages.add(message);
            adapter.notifyItemInserted(messages.size() - 1);
            recyclerView.scrollToPosition(messages.size() - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocket != null) {
            webSocket.close(1000, "Activity destroyed");
        }
    }
}
