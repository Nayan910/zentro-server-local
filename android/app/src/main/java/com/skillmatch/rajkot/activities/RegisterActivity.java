package com.skillmatch.rajkot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.skillmatch.rajkot.R;
import com.skillmatch.rajkot.api.ApiClient;
import com.skillmatch.rajkot.utils.PrefsManager;

import java.util.Map;

/**
 * RegisterActivity (Server Fork) - Register new users via local server.
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText etUsername, etEmail, etPassword, etPhone, etLocation;
    private Spinner spinnerRole, spinnerTrade;
    private Button btnRegister;
    private ProgressBar progressBar;
    private ApiClient apiClient;
    private PrefsManager prefs;

    private final String[] roles = {"worker", "customer"};
    private final String[] trades = {
            "Plumber", "Electrician", "Mason", "Carpenter",
            "Painter", "Mechanic", "Welder", "Tailor",
            "Driver", "Cook", "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            apiClient = ApiClient.getInstance(this);
            prefs = new PrefsManager(this);

            setContentView(R.layout.activity_register_server);

            // Bind views
            etUsername = findViewById(R.id.etUsername);
            etEmail = findViewById(R.id.etEmail);
            etPassword = findViewById(R.id.etPassword);
            etPhone = findViewById(R.id.etPhone);
            etLocation = findViewById(R.id.etLocation);
            spinnerRole = findViewById(R.id.spinnerRole);
            spinnerTrade = findViewById(R.id.spinnerTrade);
            btnRegister = findViewById(R.id.btnRegister);
            progressBar = findViewById(R.id.progressBar);
            TextView tvLogin = findViewById(R.id.tvLogin);

            // Setup spinners
            ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, roles);
            roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRole.setAdapter(roleAdapter);

            ArrayAdapter<String> tradeAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, trades);
            tradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTrade.setAdapter(tradeAdapter);

            // Register button
            btnRegister.setOnClickListener(v -> register());

            // Login link
            tvLogin.setOnClickListener(v -> finish());

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Initialization error", Toast.LENGTH_LONG).show();
        }
    }

    private void register() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String role = roles[spinnerRole.getSelectedItemPosition()];
        String trade = trades[spinnerTrade.getSelectedItemPosition()];

        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username, email and password are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        apiClient.register(username, email, password, phone, role, location, trade,
                new ApiClient.ApiCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);

                String token = (String) result.get("token");
                if (token != null) {
                    // Save user data
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
                    }

                    Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finishAffinity();
                } else {
                    Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
