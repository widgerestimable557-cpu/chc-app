package com.christianhelpcenter.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PushUtils {

    private static final String PREFS = "chc_prefs";
    private static final String KEY_ORG_URL = "org_url";

    public static void saveOrgUrl(Context ctx, String orgUrl) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ORG_URL, orgUrl).apply();
    }

    public static String getSavedOrgUrl(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ORG_URL, null);
    }

    public static void sendTokenToBackend(final Context ctx, final String orgUrl, final String token) {
        sendJsonPost_(ctx, orgUrl,
            "{\"action\":\"registerPushToken\",\"token\":\"" + token
            + "\",\"deviceId\":\"" + getDeviceId_(ctx) + "\"}");
    }

    public static void linkTokenToUser(final Context ctx, final String orgUrl, final String token, final String email) {
        sendJsonPost_(ctx, orgUrl,
            "{\"action\":\"linkPushUser\",\"token\":\"" + token
            + "\",\"email\":\"" + email.replace("\"", "") + "\"}");
    }

    private static String getDeviceId_(Context ctx) {
        try {
            String id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
            return (id != null) ? id : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static void sendJsonPost_(final Context ctx, final String orgUrl, final String json) {
        if (orgUrl == null || json == null) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(orgUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                    OutputStream os = conn.getOutputStream();
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.close();
                    conn.getResponseCode(); // déclenche réellement la requête
                } catch (Exception e) {
                    // silencieux — sera retenté au prochain lancement de l'app
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }
}
