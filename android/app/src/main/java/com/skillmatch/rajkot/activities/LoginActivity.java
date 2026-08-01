package com.skillmatch.rajkot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.skillmatch.rajkot.R;
import com.skillmatch.rajkot.firebase.AuthHelper;
import com.skillmatch.rajkot.firebase.FirestoreHelper;
import com.skillmatch.rajkot.models.User;
import com.skillmatch.rajkot.utils.PrefsManager;

import java.util.UUID;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword, etQuickName;
    private Button btnLogin, btnQuickLogin;
    private TextView tvRegister;
    private CheckBox cbRememberMe;
    private AuthHelper auth;
    private FirestoreHelper firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            auth = new AuthHelper();
            firestore = new FirestoreHelper();
            PrefsManager prefs = new PrefsManager(this);

            if (auth.isLoggedIn() && prefs.isLoggedIn()) {
                navigateToMain();
                return;
            }

            setContentView(R.layout.activity_login);

            etEmail = findViewById(R.id.etEmail);
            etPassword = findViewById(R.id.etPassword);
            btnLogin = findViewById(R.id.btnLogin);
            tvRegister = findViewById(R.id.tvRegister);
            cbRememberMe = findViewById(R.id.cbRememberMe);
            etQuickName = findViewById(R.id.etQuickName);
            btnQuickLogin = findViewById(R.id.btnQuickLogin);

            btnLogin.setOnClickListener(v -> login());
            tvRegister.setOnClickListener(v ->
                    startActivity(new Intent(this, RegisterActivity.class)));

            btnQuickLogin.setOnClickListener(v -> {
                if (etQuickName.getVisibility() == View.GONE) {
                    etQuickName.setVisibility(View.VISIBLE);
                    etQuickName.requestFocus();
                } else {
                    quickLogin();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "App initialization error. Please restart.", Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToMain() {
        try {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to main", e);
            Toast.makeText(this, "Navigation error. Please restart.", Toast.LENGTH_LONG).show();
        }
    }

    private void quickLogin() {
        String name = etQuickName.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String fakeUid = "guest_" + UUID.randomUUID().toString().substring(0, 8);
            PrefsManager prefs = new PrefsManager(this);
            prefs.setLoggedIn(true);
            prefs.setUserUid(fakeUid);
            prefs.setUserRole("worker");
            prefs.setUserName(name);
            prefs.setUserEmail("");
            prefs.setUserPhone("");
            prefs.setUserLocation("");
            prefs.setUserTrade("");
            prefs.setUserExperience(0);
            prefs.setUserAvailable(true);

            navigateToMain();
        } catch (Exception e) {
            Log.e(TAG, "Error in quick login", e);
            Toast.makeText(this, "Quick login failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth == null || firestore == null) {
            Toast.makeText(this, "App not initialized. Please restart.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        try {
            auth.signIn(email, password, new AuthHelper.OnAuthListener() {
                @Override
                public void onSuccess(String uid) {
                    try {
                        firestore.getUser(uid, new FirestoreHelper.OnResultListener<User>() {
                            @Override
                            public void onSuccess(User user) {
                                try {
                                    PrefsManager prefs = new PrefsManager(LoginActivity.this);
                                    prefs.setLoggedIn(true);
                                    prefs.setUserUid(uid);
                                    prefs.setUserRole(user.getRole() != null ? user.getRole() : "customer");
                                    prefs.setUserName(user.getName() != null ? user.getName() : "");
                                    prefs.setUserEmail(user.getEmail() != null ? user.getEmail() : "");
                                    prefs.setUserPhone(user.getPhone() != null ? user.getPhone() : "");
                                    prefs.setUserLocation(user.getLocation() != null ? user.getLocation() : "");
                                    prefs.setUserTrade(user.getTradeCategory() != null ? user.getTradeCategory() : "");
                                    prefs.setUserExperience(user.getExperienceYears());
                                    prefs.setUserAvailable(user.isAvailable());

                                    if (cbRememberMe.isChecked()) {
                                        prefs.setLoggedIn(true);
                                    }

                                    navigateToMain();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error saving profile", e);
                                    Toast.makeText(LoginActivity.this, "Profile save failed", Toast.LENGTH_SHORT).show();
                                    btnLogin.setEnabled(true);
                                }
                            }

                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(LoginActivity.this, "Profile load failed: " + error,
                                        Toast.LENGTH_LONG).show();
                                btnLogin.setEnabled(true);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error fetching user", e);
                        Toast.makeText(LoginActivity.this, "Login error", Toast.LENGTH_SHORT).show();
                        btnLogin.setEnabled(true);
                    }
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(LoginActivity.this, "Login failed: " + error,
                            Toast.LENGTH_LONG).show();
                    btnLogin.setEnabled(true);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error during sign in", e);
            Toast.makeText(this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
            btnLogin.setEnabled(true);
        }
    }
}
