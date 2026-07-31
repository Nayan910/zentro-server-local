package com.skillmatch.rajkot.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skillmatch.rajkot.utils.PrefsManager;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * ApiClient - Handles all communication with the Zentro server.
 *
 * Usage:
 *   ApiClient api = ApiClient.getInstance(context);
 *   api.login("username", "password", new ApiCallback<Map<String, Object>>() { ... });
 *
 * The server IP is read from SharedPreferences and can be changed from the LoginActivity.
 */
public class ApiClient {

    private static ApiClient instance;
    private Retrofit retrofit;
    private ApiService apiService;
    private final Context context;
    private final PrefsManager prefsManager;

    private static final String PREFS_NAME = "zentro_prefs";
    private static final String KEY_SERVER_IP = "server_ip";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String DEFAULT_IP = "192.168.1.1";
    private static final int DEFAULT_PORT = 8080;

    // ---- Retrofit API Interface ----

    public interface ApiService {

        // Auth
        @POST("api/auth/register")
        retrofit2.Call<Map<String, Object>> register(@Body Map<String, Object> body);

        @POST("api/auth/login")
        retrofit2.Call<Map<String, Object>> login(@Body Map<String, String> body);

        // Users
        @GET("api/users/{id}")
        retrofit2.Call<Map<String, Object>> getUser(@Path("id") Long id);

        @PUT("api/users/{id}")
        retrofit2.Call<Map<String, Object>> updateUser(@Path("id") Long id,
                                                        @Body Map<String, Object> body);

        @GET("api/users")
        retrofit2.Call<java.util.List<Map<String, Object>>> getAllWorkers();

        @GET("api/users/search")
        retrofit2.Call<java.util.List<Map<String, Object>>> searchUsers(@Query("q") String query);

        // Gigs
        @POST("api/gigs")
        retrofit2.Call<Map<String, Object>> createGig(@Header("Authorization") String auth,
                                                       @Body Map<String, Object> body);

        @GET("api/gigs")
        retrofit2.Call<java.util.List<Map<String, Object>>> getAllGigs();

        @GET("api/gigs/my")
        retrofit2.Call<java.util.List<Map<String, Object>>> getMyGigs(@Header("Authorization") String auth);

        @GET("api/gigs/{id}")
        retrofit2.Call<Map<String, Object>> getGig(@Path("id") Long id);

        @POST("api/gigs/{id}/apply")
        retrofit2.Call<Map<String, Object>> applyForGig(@Path("id") Long id,
                                                         @Header("Authorization") String auth,
                                                         @Body Map<String, String> body);

        @GET("api/gigs/{id}/applications")
        retrofit2.Call<java.util.List<Map<String, Object>>> getGigApplications(@Path("id") Long id);

        // Chat
        @POST("api/chat/send")
        retrofit2.Call<Map<String, Object>> sendMessage(@Header("Authorization") String auth,
                                                        @Body Map<String, Object> body);

        @GET("api/chat/{userId}")
        retrofit2.Call<java.util.List<Map<String, Object>>> getConversation(
                @Path("userId") Long userId,
                @Header("Authorization") String auth);

        @POST("api/chat/read")
        retrofit2.Call<Map<String, Object>> markRead(@Header("Authorization") String auth,
                                                     @Body Map<String, Long> body);

        @GET("api/chat/conversations")
        retrofit2.Call<java.util.List<Map<String, Object>>> getConversations(
                @Header("Authorization") String auth);

        @GET("api/chat/unread/count")
        retrofit2.Call<Map<String, Object>> getUnreadCount(@Header("Authorization") String auth);

        // Global Chat
        @POST("api/chat/global/send")
        retrofit2.Call<Map<String, Object>> sendGlobalMessage(
                @Header("Authorization") String auth,
                @Body Map<String, String> body);

        @GET("api/chat/global")
        retrofit2.Call<java.util.List<Map<String, Object>>> getGlobalMessages(
                @Header("Authorization") String auth);

        @GET("api/chat/global/recent")
        retrofit2.Call<java.util.List<Map<String, Object>>> getRecentGlobalMessages(
                @Header("Authorization") String auth,
                @Query("limit") int limit);

        // Private Chat
        @POST("api/chat/private/send")
        retrofit2.Call<Map<String, Object>> sendPrivateMessage(
                @Header("Authorization") String auth,
                @Body Map<String, Object> body);

        @GET("api/chat/private/{userId}")
        retrofit2.Call<java.util.List<Map<String, Object>>> getPrivateConversation(
                @Path("userId") Long userId,
                @Header("Authorization") String auth);

        @POST("api/chat/private/read")
        retrofit2.Call<Map<String, Object>> markPrivateMessagesRead(
                @Header("Authorization") String auth,
                @Body Map<String, Long> body);

        @GET("api/chat/private/conversations")
        retrofit2.Call<java.util.List<Map<String, Object>>> getPrivateConversations(
                @Header("Authorization") String auth);

        @GET("api/chat/private/unread/count")
        retrofit2.Call<Map<String, Object>> getPrivateUnreadCount(
                @Header("Authorization") String auth);

        // User Search
        @GET("api/users/search")
        retrofit2.Call<java.util.List<Map<String, Object>>> searchUsersByUsername(
                @Query("username") String username,
                @Header("Authorization") String auth);

        @GET("api/users/username/{username}")
        retrofit2.Call<Map<String, Object>> getUserByUsername(
                @Path("username") String username,
                @Header("Authorization") String auth);

        // Logs
        @GET("api/logs")
        retrofit2.Call<java.util.List<Map<String, Object>>> getAllLogs(@Header("Authorization") String auth);
    }

    // ---- Callback Interface ----

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    // ---- Singleton ----

    private ApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.prefsManager = new PrefsManager(this.context);
        initRetrofit();
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context);
        }
        return instance;
    }

    // ---- Server URL Management ----

    public String getServerIp() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SERVER_IP, DEFAULT_IP);
    }

    public void setServerIp(String ip) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SERVER_IP, ip).apply();
        initRetrofit(); // Reinitialize with new IP
    }

    public String getBaseUrl() {
        return "http://" + getServerIp() + ":" + DEFAULT_PORT + "/";
    }

    public String getServerUrl() {
        return "http://" + getServerIp() + ":" + DEFAULT_PORT;
    }

    private void initRetrofit() {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .create();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        String token = prefsManager.getJwtToken();
                        if (token != null && !token.isEmpty()) {
                            Request request = original.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                            return chain.proceed(request);
                        }
                        return chain.proceed(original);
                    }
                })
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public ApiService getApi() {
        return apiService;
    }

    // ---- Convenience Methods ----

    public String getJwtToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_JWT_TOKEN, null);
    }

    public void setJwtToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_JWT_TOKEN, token).apply();
    }

    public void clearToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_JWT_TOKEN).apply();
    }

    public boolean isLoggedIn() {
        return getJwtToken() != null && !getJwtToken().isEmpty();
    }

    private String authHeader() {
        String token = getJwtToken();
        return token != null ? "Bearer " + token : "";
    }

    // ---- Auth Methods ----

    public void register(String username, String email, String password,
                          String phone, String role, String location,
                          String tradeCategory, ApiCallback<Map<String, Object>> callback) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("username", username);
        body.put("email", email);
        body.put("password", password);
        body.put("phone", phone);
        body.put("role", role);
        body.put("location", location);
        body.put("tradeCategory", tradeCategory);

        apiService.register(body).enqueue(new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<Map<String, Object>> call,
                                   retrofit2.Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = (String) response.body().get("token");
                    if (token != null) setJwtToken(token);
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Registration failed: " + response.message());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                callback.onError("Connection error: " + t.getMessage());
            }
        });
    }

    public void login(String username, String password,
                       ApiCallback<Map<String, Object>> callback) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("username", username);
        body.put("password", password);

        apiService.login(body).enqueue(new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<Map<String, Object>> call,
                                   retrofit2.Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = (String) response.body().get("token");
                    if (token != null) setJwtToken(token);
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Login failed: " + response.message());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                callback.onError("Connection error: " + t.getMessage());
            }
        });
    }

    // ---- Gig Methods ----

    public void createGig(String title, String description, double budget,
                           String location, String tradeCategory,
                           ApiCallback<Map<String, Object>> callback) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", title);
        body.put("description", description);
        body.put("budget", budget);
        body.put("location", location);
        body.put("tradeCategory", tradeCategory);

        apiService.createGig(authHeader(), body).enqueue(new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<Map<String, Object>> call,
                                   retrofit2.Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to create gig");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                callback.onError("Connection error: " + t.getMessage());
            }
        });
    }

    public void getAllGigs(ApiCallback<java.util.List<Map<String, Object>>> callback) {
        apiService.getAllGigs().enqueue(new retrofit2.Callback<java.util.List<Map<String, Object>>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<Map<String, Object>>> call,
                                   retrofit2.Response<java.util.List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load gigs");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<Map<String, Object>>> call, Throwable t) {
                callback.onError("Connection error: " + t.getMessage());
            }
        });
    }

    public void applyForGig(Long gigId, String message,
                             ApiCallback<Map<String, Object>> callback) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("message", message);

        apiService.applyForGig(gigId, authHeader(), body).enqueue(new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<Map<String, Object>> call,
                                   retrofit2.Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to apply");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                callback.onError("Connection error: " + t.getMessage());
            }
        });
    }

    // ---- Chat Methods ----

    public void getConversation(Long userId,
                                 ApiCallback<java.util.List<Map<String, Object>>> callback) {
        apiService.getConversation(userId, authHeader()).enqueue(new retrofit2.Callback<java.util.List<Map<String, Object>>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<Map<String, Object>>> call,
                                   retrofit2.Response<java.util.List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load messages");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<Map<String, Object>>> call, Throwable t) {
                callback.onError("Connection error: " + t.getMessage());
            }
        });
    }

    public void sendMessage(Long receiverId, String content,
                             ApiCallback<Map<String, Object>> callback) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("receiverId", receiverId);
        body.put("content", content);

        apiService.sendMessage(authHeader(), body).enqueue(new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<Map<String, Object>> call,
                                   retrofit2.Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to send message");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                callback.onError("Connection error: " + t.getMessage());
            }
        });
    }

    // ---- Connection Test ----

    public void testConnection(ApiCallback<Map<String, Object>> callback) {
        apiService.getAllGigs().enqueue(new retrofit2.Callback<java.util.List<Map<String, Object>>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<Map<String, Object>>> call,
                                   retrofit2.Response<java.util.List<Map<String, Object>>> response) {
                java.util.Map<String, Object> result = new java.util.HashMap<>();
                result.put("connected", response.isSuccessful());
                result.put("statusCode", response.code());
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<Map<String, Object>>> call, Throwable t) {
                java.util.Map<String, Object> result = new java.util.HashMap<>();
                result.put("connected", false);
                result.put("error", t.getMessage());
                callback.onSuccess(result);
            }
        });
    }
}
