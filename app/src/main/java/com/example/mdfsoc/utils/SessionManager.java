package com.example.mdfsoc.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.mdfsoc.network.AuthInterceptor;

import java.security.GeneralSecurityException;

public class SessionManager implements AuthInterceptor.SessionTokenProvider {

    private static final String PREFS_NAME = "mdf_soc_secure";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EXPIRES_AT = "expires_at";

    private static volatile SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        Context app = context.getApplicationContext();
        SharedPreferences delegate;
        try {
            MasterKey masterKey = new MasterKey.Builder(app)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            delegate = EncryptedSharedPreferences.create(
                    app,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | java.io.IOException e) {
            delegate = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        this.prefs = delegate;
    }

    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context);
                }
            }
        }
        return instance;
    }

    public void saveSession(String token, String expiresAt, String username) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_EXPIRES_AT, expiresAt)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getExpiresAt() {
        return prefs.getString(KEY_EXPIRES_AT, "");
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}