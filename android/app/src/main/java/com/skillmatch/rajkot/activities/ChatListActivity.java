package com.skillmatch.rajkot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.skillmatch.rajkot.R;
import com.skillmatch.rajkot.api.ApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ApiClient apiClient;
    private ConversationAdapter adapter;
    private List<Map<String, Object>> conversations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        apiClient = ApiClient.getInstance(this);

        initViews();
        setupRecyclerView();
        loadConversations();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewConversations);

        findViewById(R.id.buttonGlobalChat).setOnClickListener(v -> {
            startActivity(new Intent(this, GlobalChatActivity.class));
        });

        findViewById(R.id.buttonSearchUser).setOnClickListener(v -> {
            startActivity(new Intent(this, SearchUserActivity.class));
        });
    }

    private void setupRecyclerView() {
        adapter = new ConversationAdapter(conversations, conversation -> {
            Intent intent = new Intent(this, PrivateChatActivity.class);
            intent.putExtra("receiverId", ((Number) conversation.get("userId")).longValue());
            intent.putExtra("receiverName", (String) conversation.get("username"));
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadConversations() {
        apiClient.getApi().getPrivateConversations(apiClient.getJwtToken())
                .enqueue(new retrofit2.Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(retrofit2.Call<List<Map<String, Object>>> call, retrofit2.Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            conversations.clear();
                            conversations.addAll(response.body());
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<List<Map<String, Object>>> call, Throwable t) {
                        Toast.makeText(ChatListActivity.this, "Failed to load conversations", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private interface OnConversationClickListener {
        void onConversationClick(Map<String, Object> conversation);
    }

    private class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
        private List<Map<String, Object>> conversations;
        private OnConversationClickListener listener;

        ConversationAdapter(List<Map<String, Object>> conversations, OnConversationClickListener listener) {
            this.conversations = conversations;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_conversation, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Map<String, Object> conversation = conversations.get(position);
            holder.username.setText((String) conversation.get("username"));
            holder.lastMessage.setText((String) conversation.get("lastMessage"));
            holder.itemView.setOnClickListener(v -> listener.onConversationClick(conversation));
        }

        @Override
        public int getItemCount() {
            return conversations.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView username, lastMessage;

            ViewHolder(View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.textViewUsername);
                lastMessage = itemView.findViewById(R.id.textViewLastMessage);
            }
        }
    }
}
