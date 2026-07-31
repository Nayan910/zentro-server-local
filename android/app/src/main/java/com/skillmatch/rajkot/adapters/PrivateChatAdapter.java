package com.skillmatch.rajkot.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skillmatch.rajkot.R;

import java.util.List;
import java.util.Map;

public class PrivateChatAdapter extends RecyclerView.Adapter<PrivateChatAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Map<String, Object>> messages;
    private String currentUserId;

    public PrivateChatAdapter(List<Map<String, Object>> messages, String token) {
        this.messages = messages;
        this.currentUserId = extractUserIdFromToken(token);
    }

    private String extractUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) return "";
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT));
                org.json.JSONObject json = new org.json.JSONObject(payload);
                return json.getString("sub");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public int getItemViewType(int position) {
        Map<String, Object> message = messages.get(position);
        Object senderId = message.get("senderId");
        if (senderId != null && senderId.toString().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        }
        return VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = viewType == VIEW_TYPE_SENT
                ? R.layout.item_message_sent
                : R.layout.item_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Map<String, Object> message = messages.get(position);
        holder.messageText.setText((String) message.get("content"));

        String timestamp = (String) message.get("timestamp");
        if (timestamp != null) {
            holder.timestamp.setText(formatTimestamp(timestamp));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private String formatTimestamp(String timestamp) {
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("HH:mm");
            java.util.Date date = inputFormat.parse(timestamp);
            return outputFormat.format(date);
        } catch (Exception e) {
            return timestamp;
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timestamp;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textViewMessage);
            timestamp = itemView.findViewById(R.id.textViewTimestamp);
        }
    }
}
