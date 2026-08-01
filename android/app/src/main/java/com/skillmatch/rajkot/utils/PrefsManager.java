package com.skillmatch.rajkot.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PrefsManager {

    private static final String PREFS_NAME = "zentro_prefs";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_UID = "user_uid";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_LOCATION = "user_location";
    private static final String KEY_USER_TRADE = "user_trade";
    private static final String KEY_USER_EXPERIENCE = "user_experience";
    private static final String KEY_USER_BIO = "user_bio";
    private static final String KEY_USER_AVAILABLE = "user_available";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_HAS_EXPLICIT_THEME = "has_explicit_theme";
    private static final String KEY_AI_API_KEY = "ai_api_key";
    private static final String KEY_AI_CHAT_MESSAGES = "ai_chat_messages";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_PROFILE_PHOTO_URI = "profile_photo_uri";
    private static final String KEY_PAST_WORK_PHOTOS = "past_work_photos";

    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public void setUserUid(String uid) {
        prefs.edit().putString(KEY_USER_UID, uid).apply();
    }

    public String getUserUid() {
        return prefs.getString(KEY_USER_UID, null);
    }

    public void setUserRole(String role) {
        prefs.edit().putString(KEY_USER_ROLE, role).apply();
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "customer");
    }

    public void setUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public void setUserEmail(String email) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public void setUserPhone(String phone) {
        prefs.edit().putString(KEY_USER_PHONE, phone).apply();
    }

    public String getUserPhone() {
        return prefs.getString(KEY_USER_PHONE, "");
    }

    public void setUserLocation(String location) {
        prefs.edit().putString(KEY_USER_LOCATION, location).apply();
    }

    public String getUserLocation() {
        return prefs.getString(KEY_USER_LOCATION, "");
    }

    public void setUserTrade(String trade) {
        prefs.edit().putString(KEY_USER_TRADE, trade).apply();
    }

    public String getUserTrade() {
        return prefs.getString(KEY_USER_TRADE, "");
    }

    public void setUserExperience(int years) {
        prefs.edit().putInt(KEY_USER_EXPERIENCE, years).apply();
    }

    public int getUserExperience() {
        return prefs.getInt(KEY_USER_EXPERIENCE, 0);
    }

    public void setUserBio(String bio) {
        prefs.edit().putString(KEY_USER_BIO, bio).apply();
    }

    public String getUserBio() {
        return prefs.getString(KEY_USER_BIO, "");
    }

    public void setUserAvailable(boolean available) {
        prefs.edit().putBoolean(KEY_USER_AVAILABLE, available).apply();
    }

    public boolean isUserAvailable() {
        return prefs.getBoolean(KEY_USER_AVAILABLE, true);
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkMode(boolean enabled) {
        prefs.edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .putBoolean(KEY_HAS_EXPLICIT_THEME, true)
            .apply();
    }

    /**
     * Returns true if the user has ever explicitly toggled the theme.
     * If false, we should follow the system setting.
     */
    public boolean hasExplicitThemeChoice() {
        return prefs.getBoolean(KEY_HAS_EXPLICIT_THEME, false);
    }

    public String getAiApiKey() {
        return prefs.getString(KEY_AI_API_KEY, "");
    }

    public void setAiApiKey(String key) {
        prefs.edit().putString(KEY_AI_API_KEY, key).apply();
    }

    /**
     * Save AI chat messages to SharedPreferences.
     * Each entry: {"message": "...", "isUser": true/false}
     */
    public void saveAiChatMessages(List<Object[]> messages) {
        JSONArray arr = new JSONArray();
        for (Object[] msg : messages) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("message", msg[0]);
                obj.put("isUser", msg[1]);
                arr.put(obj);
            } catch (JSONException e) {
                // skip malformed entries
            }
        }
        prefs.edit().putString(KEY_AI_CHAT_MESSAGES, arr.toString()).apply();
    }

    /**
     * Load AI chat messages from SharedPreferences.
     * Returns List of Object[] where [0]=String message, [1]=Boolean isUser.
     */
    public List<Object[]> loadAiChatMessages() {
        List<Object[]> result = new ArrayList<>();
        String json = prefs.getString(KEY_AI_CHAT_MESSAGES, null);
        if (json == null || json.isEmpty()) return result;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                result.add(new Object[]{obj.getString("message"), obj.getBoolean("isUser")});
            }
        } catch (JSONException e) {
            // corrupted data, return empty
        }
        return result;
    }

    public void clearAiChatMessages() {
        prefs.edit().remove(KEY_AI_CHAT_MESSAGES).apply();
    }

    public void setJwtToken(String token) {
        prefs.edit().putString(KEY_JWT_TOKEN, token).apply();
    }

    public String getJwtToken() {
        return prefs.getString(KEY_JWT_TOKEN, null);
    }

    public void setProfilePhotoUri(String uri) {
        prefs.edit().putString(KEY_PROFILE_PHOTO_URI, uri).apply();
    }

    public String getProfilePhotoUri() {
        return prefs.getString(KEY_PROFILE_PHOTO_URI, null);
    }

    /**
     * Save past work photo URIs as a JSON array string.
     */
    public void setPastWorkPhotos(List<String> uris) {
        JSONArray arr = new JSONArray();
        for (String uri : uris) {
            arr.put(uri);
        }
        prefs.edit().putString(KEY_PAST_WORK_PHOTOS, arr.toString()).apply();
    }

    /**
     * Load past work photo URIs.
     */
    public List<String> getPastWorkPhotos() {
        List<String> result = new ArrayList<>();
        String json = prefs.getString(KEY_PAST_WORK_PHOTOS, null);
        if (json == null || json.isEmpty()) return result;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                result.add(arr.getString(i));
            }
        } catch (JSONException e) {
            // corrupted
        }
        return result;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
