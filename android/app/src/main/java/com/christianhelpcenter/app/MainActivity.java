package com.christianhelpcenter.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.Manifest;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private WebView webView;
    private static final int PERM_CODE = 1001;

    // ✅ CLEF : Charger directement la page bridge sur GitHub Pages
    // Cette page lit le localStorage (sauvegardé lors du téléchargement)
    // et redirige automatiquement vers l'église
    private static final String BRIDGE_URL =
        "https://widgerestimable557-cpu.github.io/chc-app/bridge.html";

    @SuppressLint({"SetJavaScriptEnabled","AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(s.getUserAgentString() + " CHCApp/1.0");

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, android.webkit.WebResourceRequest r) {
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() { request.grant(request.getResources()); }
                });
            }
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                    GeolocationPermissions.Callback cb) {
                cb.invoke(origin, true, false);
            }
        });

        requestNativePermissions();

        // ✅ Charger la page bridge — elle gère tout automatiquement
        webView.loadUrl(BRIDGE_URL);
    }

    public class AndroidBridge {
        @JavascriptInterface public boolean isAPK() { return true; }

        @JavascriptInterface public boolean hasMicPermission() {
            return checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        }

        @JavascriptInterface public void requestMicPermission() {
            runOnUiThread(new Runnable() { @Override public void run() {
                requestPermissions(new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
                }, PERM_CODE);
            }});
        }

        @JavascriptInterface public void updateBranding(final String name, final String logo) {
            runOnUiThread(new Runnable() { @Override public void run() {
                if (name != null && !name.isEmpty()) setTitle(name);
                if (logo != null && !logo.isEmpty()) loadLogoAsTaskIcon(name, logo);
            }});
        }
    }

    private void loadLogoAsTaskIcon(final String name, final String logoUrl) {
        new Thread(new Runnable() { @Override public void run() {
            try {
                URL url = new URL(logoUrl);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(5000); c.setReadTimeout(5000); c.connect();
                final Bitmap bmp = BitmapFactory.decodeStream(c.getInputStream());
                if (bmp != null) runOnUiThread(new Runnable() { @Override public void run() {
                    setTaskDescription(new ActivityManager.TaskDescription(
                        name != null ? name : "Church App", bmp, 0xFF075E54));
                }});
            } catch(Exception e) {}
        }}).start();
    }

    private void requestNativePermissions() {
        String[] perms = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        };
        java.util.List<String> needed = new java.util.ArrayList<>();
        for (String p : perms)
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) needed.add(p);
        if (!needed.isEmpty())
            requestPermissions(needed.toArray(new String[0]), PERM_CODE);
    }

    @Override public void onRequestPermissionsResult(int c, String[] p, int[] r) {
        super.onRequestPermissionsResult(c, p, r);
        if (webView != null) webView.reload();
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (webView != null) webView.saveState(out);
    }
}
