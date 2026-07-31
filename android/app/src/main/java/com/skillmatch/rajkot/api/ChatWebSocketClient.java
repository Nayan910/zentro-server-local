package com.skillmatch.rajkot.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * ChatWebSocketClient - Handles real-time chat via WebSocket.
 *
 * Connects to: ws://SERVER_IP:8080/ws/chat?token=JWT_TOKEN
 *
 * Usage:
 *   ChatWebSocketClient ws = new ChatWebSocketClient(serverUrl, token);
 *   ws.setListener(new ChatWebSocketClient.Listener() { ... });
 *   ws.connect();
 */
public class ChatWebSocketClient {

    private static final String TAG = "ChatWSClient";
    private static final int RECONNECT_DELAY_MS = 3000;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;

    private WebSocket webSocket;
    private OkHttpClient client;
    private String serverUrl;
    private String token;
    private Listener listener;
    private boolean isConnected = false;
    private boolean shouldReconnect = true;
    private int reconnectAttempts = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ---- Listener Interface ----

    public interface Listener {
        void onConnected();
        void onMessageReceived(long senderId, String senderName, String content, String timestamp);
        void onTyping(long userId, String username);
        void onReadReceipt(long userId);
        void onDisconnected();
        void onError(String error);
    }

    // ---- Constructor ----

    public ChatWebSocketClient(String serverUrl, String token) {
        this.serverUrl = serverUrl;
        this.token = token;

        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    // ---- Connection Management ----

    public void connect() {
        String wsUrl = serverUrl.replace("http://", "ws://")
                                .replace("https://", "wss://")
                                + "/ws/chat?token=" + token;

        Log.d(TAG, "Connecting to: " + wsUrl);

        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                Log.d(TAG, "WebSocket connected");
                isConnected = true;
                reconnectAttempts = 0;
                mainHandler.post(() -> {
                    if (listener != null) listener.onConnected();
                });
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                Log.d(TAG, "Received: " + text);
                try {
                    JSONObject json = new JSONObject(text);
                    String type = json.getString("type");

                    switch (type) {
                        case "connected":
                            // Connection confirmation
                            break;

                        case "chat":
                            long senderId = json.getLong("senderId");
                            String senderName = json.getString("senderName");
                            String content = json.getString("content");
                            String timestamp = json.getString("timestamp");
                            mainHandler.post(() -> {
                                if (listener != null) {
                                    listener.onMessageReceived(senderId, senderName, content, timestamp);
                                }
                            });
                            break;

                        case "typing":
                            long typingUserId = json.getLong("userId");
                            String typingUsername = json.getString("username");
                            mainHandler.post(() -> {
                                if (listener != null) {
                                    listener.onTyping(typingUserId, typingUsername);
                                }
                            });
                            break;

                        case "read":
                            long readUserId = json.getLong("userId");
                            mainHandler.post(() -> {
                                if (listener != null) {
                                    listener.onReadReceipt(readUserId);
                                }
                            });
                            break;

                        case "sent":
                            // Message sent confirmation
                            break;

                        case "error":
                            String errorMsg = json.getString("message");
                            mainHandler.post(() -> {
                                if (listener != null) {
                                    listener.onError(errorMsg);
                                }
                            });
                            break;
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing message", e);
                }
            }

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                Log.d(TAG, "WebSocket closing: " + reason);
                ws.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + reason);
                isConnected = false;
                mainHandler.post(() -> {
                    if (listener != null) listener.onDisconnected();
                });
                if (shouldReconnect) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                Log.e(TAG, "WebSocket failure", t);
                isConnected = false;
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onError("Connection error: " + t.getMessage());
                        listener.onDisconnected();
                    }
                });
                if (shouldReconnect) {
                    scheduleReconnect();
                }
            }
        });
    }

    public void disconnect() {
        shouldReconnect = false;
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
        }
    }

    private void scheduleReconnect() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            Log.d(TAG, "Reconnecting in " + RECONNECT_DELAY_MS + "ms (attempt " + reconnectAttempts + ")");
            mainHandler.postDelayed(this::connect, RECONNECT_DELAY_MS);
        } else {
            Log.d(TAG, "Max reconnect attempts reached");
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onError("Connection lost. Please restart chat.");
                }
            });
        }
    }

    // ---- Send Methods ----

    public void sendMessage(long receiverId, String content) {
        if (webSocket == null || !isConnected) {
            if (listener != null) {
                listener.onError("Not connected to server");
            }
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("type", "chat");
            json.put("receiverId", receiverId);
            json.put("content", content);
            webSocket.send(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error sending message", e);
        }
    }

    public void sendTyping(long receiverId) {
        if (webSocket == null || !isConnected) return;

        try {
            JSONObject json = new JSONObject();
            json.put("type", "typing");
            json.put("receiverId", receiverId);
            webSocket.send(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error sending typing", e);
        }
    }

    public void sendReadReceipt(long userId) {
        if (webSocket == null || !isConnected) return;

        try {
            JSONObject json = new JSONObject();
            json.put("type", "read");
            json.put("userId", userId);
            webSocket.send(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error sending read receipt", e);
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
}
