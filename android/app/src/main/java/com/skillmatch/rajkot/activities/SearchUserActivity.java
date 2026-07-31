package com.skillmatch.rajkot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
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

public class SearchUserActivity extends AppCompatActivity {

    private EditText searchInput;
    private RecyclerView recyclerView;
    private ApiClient apiClient;
    private UserSearchAdapter adapter;
    private List<Map<String, Object>> users = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);

        apiClient = ApiClient.getInstance(this);

        initViews();
        setupRecyclerView();
        setupSearch();
    }

    private void initViews() {
        searchInput = findViewById(R.id.editTextSearch);
        recyclerView = findViewById(R.id.recyclerViewUsers);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new UserSearchAdapter(users, user -> {
            Intent intent = new Intent(this, PrivateChatActivity.class);
            intent.putExtra("receiverId", ((Number) user.get("id")).longValue());
            intent.putExtra("receiverName", (String) user.get("username"));
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchUsers(String query) {
        if (query.length() < 2) {
            users.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        apiClient.getApi().searchUsersByUsername(query, apiClient.getJwtToken())
                .enqueue(new retrofit2.Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(retrofit2.Call<List<Map<String, Object>>> call, retrofit2.Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            users.clear();
                            users.addAll(response.body());
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<List<Map<String, Object>>> call, Throwable t) {
                        Toast.makeText(SearchUserActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private interface OnUserClickListener {
        void onUserClick(Map<String, Object> user);
    }

    private class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {
        private List<Map<String, Object>> users;
        private OnUserClickListener listener;

        UserSearchAdapter(List<Map<String, Object>> users, OnUserClickListener listener) {
            this.users = users;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_user_search, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Map<String, Object> user = users.get(position);
            holder.username.setText((String) user.get("username"));
            holder.role.setText((String) user.get("role"));
            holder.location.setText((String) user.get("location"));
            holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView username, role, location;

            ViewHolder(View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.textViewUsername);
                role = itemView.findViewById(R.id.textViewRole);
                location = itemView.findViewById(R.id.textViewLocation);
            }
        }
    }
}
