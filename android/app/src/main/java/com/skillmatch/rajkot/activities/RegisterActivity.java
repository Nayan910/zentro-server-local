package com.skillmatch.rajkot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.skillmatch.rajkot.R;
import com.skillmatch.rajkot.firebase.AuthHelper;
import com.skillmatch.rajkot.firebase.FirestoreHelper;
import com.skillmatch.rajkot.models.User;
import com.skillmatch.rajkot.utils.PrefsManager;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword, etLocation, etExperience, etBio;
    private Spinner spRole, spTrade;
    private Button btnRegister;
    private AuthHelper auth;
    private FirestoreHelper firestore;

    private static final String[] ROLES = {"Worker", "Recruiter", "Freelancer", "Business Owner", "Customer"};
    private static final String[] TRADES = {"Plumber", "Electrician", "Mason", "Carpenter", "Painter",
            "Welder", "Mechanic", "Tailor", "Driver", "Cleaner", "Security Guard", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = new AuthHelper();
        firestore = new FirestoreHelper();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etLocation = findViewById(R.id.etLocation);
        spRole = findViewById(R.id.spRole);
        spTrade = findViewById(R.id.spTrade);
        etExperience = findViewById(R.id.etExperience);
        etBio = findViewById(R.id.etBio);
        btnRegister = findViewById(R.id.btnRegister);

        spRole.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, ROLES));
        spTrade.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, TRADES));

        btnRegister.setOnClickListener(v -> register());
    }

    private void register() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String role = ROLES[spRole.getSelectedItemPosition()].toLowerCase().replace(" ", "_");
        String trade = TRADES[spTrade.getSelectedItemPosition()].toLowerCase();
        String bio = etBio.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Name, email, and password are required",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int experience = 0;
        String expStr = etExperience.getText().toString().trim();
        if (!expStr.isEmpty()) {
            try { experience = Integer.parseInt(expStr); } catch (NumberFormatException ignored) {}
        }

        final int exp = experience;
        btnRegister.setEnabled(false);
        auth.signUp(email, password, new AuthHelper.OnAuthListener() {
            @Override
            public void onSuccess(String uid) {
                User user = new User(uid, name, email, phone, role, location);
                user.setTradeCategory(trade);
                user.setExperienceYears(exp);

                firestore.saveUser(user, new FirestoreHelper.OnResultListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        PrefsManager prefs = new PrefsManager(RegisterActivity.this);
                        prefs.setLoggedIn(true);
                        prefs.setUserUid(uid);
                        prefs.setUserRole(role);
                        prefs.setUserName(name);
                        prefs.setUserEmail(email);
                        prefs.setUserPhone(phone);
                        prefs.setUserLocation(location);
                        prefs.setUserTrade(trade);
                        prefs.setUserExperience(exp);
                        prefs.setUserBio(bio);

                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(RegisterActivity.this, "Profile save failed: " + error,
                                Toast.LENGTH_LONG).show();
                        btnRegister.setEnabled(true);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(RegisterActivity.this, "Registration failed: " + error,
                        Toast.LENGTH_LONG).show();
                btnRegister.setEnabled(true);
            }
        });
    }
}
