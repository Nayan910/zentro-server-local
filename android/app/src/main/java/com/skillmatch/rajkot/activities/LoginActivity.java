package com.skillmatch.rajkot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.skillmatch.rajkot.R;
import com.skillmatch.rajkot.api.ApiClient;
import com.skillmatch.rajkot.utils.PrefsManager;

import java.util.Map;
import java.util.UUID;

/**
 * LoginActivity (Server Fork) - Modified to connect to local server.
 *
 * Changes from original:
 *   - Added server IP input field
 *   - Removed Firebase Auth, uses REST API login
 *   - Connection status indicator
 *   - Stores server IP in SharedPreferences
 *   - JWT token stored for authenticated requests
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etServerIp, etUsername, etPassword;
    private Button btnLogin, btnConnect;
    private TextView tvStatus, tvRegister;
    private ProgressBar progressBar;
    private ApiClient apiClient;
    private PrefsManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            apiClient = ApiClient.getInstance(this);
            prefs = new PrefsManager(this);

            // If already logged in with valid token, go to main
            if (apiClient.isLoggedIn() && prefs.isLoggedIn()) {
                navigateToMain();
                return;
            }

            setContentView(R.layout.activity_login_server);

            // Bind views
            etServerIp = findViewById(R.id.etServerIp);
            etUsername = findViewById(R.id.etUsername);
            etPassword = findViewById(R.id.etPassword);
            btnLogin = findViewById(R.id.btnLogin);
            btnConnect = findViewById(R.id.btnConnect);
            tvStatus = findViewById(R.id.tvStatus);
            tvRegister = findViewById(R.id.tvRegister);
            progressBar = findViewById(R.id.progressBar);

            // Load saved server IP
            String savedIp = apiClient.getServerIp();
            etServerIp.setText(savedIp);

            // Test connection on startup
            testConnection();

            // Connect button - test server connection
            btnConnect.setOnClickListener(v -> testConnection());

            // Login button
            btnLogin.setOnClickListener(v -> login());

            // Register link
            tvRegister.setOnClickListener(v -> {
                startActivity(new Intent(this, RegisterActivity.class));
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "App initialization error. Please restart.", Toast.LENGTH_LONG).show();
        }
    }

    private void testConnection() {
        String ip = etServerIp.getText().toString().trim();
        if (ip.isEmpty()) {
            tvStatus.setText("Enter server IP");
            tvStatus.setTextColor(getResources().getColor(R.color.red));
            return;
        }

        // Save the IP
        apiClient.setServerIp(ip);

        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Connecting...");
        tvStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        btnConnect.setEnabled(false);

        apiClient.testConnection(new ApiClient.ApiCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                progressBar.setVisibility(View.GONE);
                btnConnect.setEnabled(true);

                Boolean connected = (Boolean) result.get("connected");
                if (connected != null && connected) {
                    tvStatus.setText("Connected to server");
                    tvStatus.setTextColor(getResources().getColor(R.color.accent));
                    btnLogin.setEnabled(true);
                } else {
                    String error = (String) result.get("error");
                    tvStatus.setText("Connection failed: " + (error != null ? error : "Unknown error"));
                    tvStatus.setTextColor(getResources().getColor(R.color.red));
                    btnLogin.setEnabled(false);
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnConnect.setEnabled(true);
                tvStatus.setText("Error: " + error);
                tvStatus.setTextColor(getResources().getColor(R.color.red));
                btnLogin.setEnabled(false);
            }
        });
    }

    private void login() {
        String ip = etServerIp.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (ip.isEmpty()) {
            Toast.makeText(this, "Enter server IP", Toast.LENGTH_SHORT).show();
            return;
        }
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save IP
        apiClient.setServerIp(ip);

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        apiClient.login(username, password, new ApiClient.ApiCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                String token = (String) result.get("token");
                if (token != null) {
                    // Save user data to PrefsManager
                    Map<String, Object> user = (Map<String, Object>) result.get("user");
                    if (user != null) {
                        prefs.setLoggedIn(true);
                        prefs.setUserUid(String.valueOf(user.get("id")));
                        prefs.setUserName((String) user.get("username"));
                        prefs.setUserEmail((String) user.get("email"));
                        prefs.setUserRole((String) user.get("role"));
                        prefs.setUserPhone((String) user.get("phone"));
                        prefs.setUserLocation((String) user.get("location"));
                        prefs.setUserTrade((String) user.get("tradeCategory"));

                        Object exp = user.get("experienceYears");
                        if (exp instanceof Number) {
                            prefs.setUserExperience(((Number) exp).intValue());
                        }

                        Object available = user.get("isAvailable");
                        if (available instanceof Boolean) {
                            prefs.setUserAvailable((Boolean) available);
                        }
                    }

                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToMain() {
        try {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to main", e);
        }
    }

    @Override
    public void onBackPressed() {
        // Double-tap to exit
        super.onBackPressed();
    }
}
