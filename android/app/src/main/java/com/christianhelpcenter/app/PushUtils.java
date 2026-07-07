package com.christianhelpcenter.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PushUtils {

    private static final String PREFS = "chc_prefs";
    private static final String KEY_ORG_URL = "org_url";
    private static final String KEY_ORG_ID  = "org_id";

    public static void saveOrgUrl(Context ctx, String orgUrl) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ORG_URL, orgUrl).apply();

        // ✅ App multi-église : on extrait et on garde l'orgId séparément,
        // pour pouvoir s'abonner au bon topic FCM même après un redémarrage
        // (onNewToken n'a pas l'URL complète sous la main).
        String orgId = extractOrgId(orgUrl);
        if (orgId != null) {
            prefs.edit().putString(KEY_ORG_ID, orgId).apply();
        }
    }

    public static String getSavedOrgUrl(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ORG_URL, null);
    }

    public static String getSavedOrgId(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ORG_ID, null);
    }

    /** Extrait ?orgId=... de l'URL de l'église. Retourne null si absent. */
    public static String extractOrgId(String orgUrl) {
        if (orgUrl == null) return null;
        try {
            String orgId = Uri.parse(orgUrl).getQueryParameter("orgId");
            return (orgId != null && !orgId.isEmpty()) ? orgId : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Nom de topic FCM propre à cette église (ex: "org_1a803ec8-...") */
    public static String topicForOrg(String orgId) {
        if (orgId == null || orgId.isEmpty()) return null;
        return "org_" + orgId.replaceAll("[^a-zA-Z0-9\\-_.~%]", "_");
    }

    public static void sendTokenToBackend(final Context ctx, final String orgUrl, final String orgId, final String token) {
        sendJsonPost_(ctx, orgUrl,
            "{\"action\":\"registerPushToken\",\"orgId\":\"" + safe_(orgId)
            + "\",\"token\":\"" + token
            + "\",\"deviceId\":\"" + getDeviceId_(ctx) + "\"}");
    }

    public static void linkTokenToUser(final Context ctx, final String orgUrl, final String orgId, final String token, final String email) {
        sendJsonPost_(ctx, orgUrl,
            "{\"action\":\"linkPushUser\",\"orgId\":\"" + safe_(orgId)
            + "\",\"token\":\"" + token
            + "\",\"email\":\"" + safe_(email) + "\"}");
    }

    private static String safe_(String s) {
        return s == null ? "" : s.replace("\"", "");
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
