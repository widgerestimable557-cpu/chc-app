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
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        requestNativePermissions();

        String html = "<!DOCTYPE html><html><head>"
            + "<meta charset='UTF-8'/>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1,viewport-fit=cover'/>"
            + "<style>"
            + "*{margin:0;padding:0;box-sizing:border-box}"
            + "body{font-family:sans-serif;background:linear-gradient(145deg,#075e54,#128c7e);min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}"
            + ".card{background:#fff;border-radius:20px;padding:28px;width:100%;max-width:400px;text-align:center}"
            + "#org-logo{width:80px;height:80px;border-radius:20px;object-fit:cover;margin:0 auto 12px;display:none;border:3px solid #f0f0f0}"
            + "#org-name{font-size:20px;font-weight:800;color:#1f2937;margin-bottom:4px}"
            + "#org-sub{font-size:13px;color:#6b7280;margin-bottom:20px}"
            + "label{font-size:13px;font-weight:600;color:#374151;display:block;margin-bottom:6px;text-align:left}"
            + "input{width:100%;padding:12px;border:2px solid #e5e7eb;border-radius:10px;font-size:14px;outline:none;margin-bottom:4px}"
            + ".err{color:#ef4444;font-size:12px;margin-bottom:12px;display:none;text-align:left}"
            + "button{background:#25d366;color:#fff;border:none;padding:14px;border-radius:10px;width:100%;font-size:16px;font-weight:700;cursor:pointer;margin-top:8px}"
            + ".hint{color:#9ca3af;font-size:11px;text-align:center;margin-top:14px;line-height:1.5}"
            + "</style></head><body>"
            + "<div class='card'>"
            + "<img id='org-logo' src='' alt='Logo'>"
            + "<div id='org-name'>Church App</div>"
            + "<div id='org-sub'>Connectez votre &eacute;glise</div>"
            + "<br>"
            + "<label>&#x1F517; URL de votre application</label>"
            + "<input type='url' id='u' placeholder='https://script.google.com/macros/s/...'/>"
            + "<div class='err' id='e'>Veuillez entrer un lien valide</div>"
            + "<button onclick='go()'>Se connecter &rarr;</button>"
            + "<div class='hint'>&#x1F4A1; Ce lien vous est fourni par votre administrateur.</div>"
            + "</div>"
            + "<script>"
            + "try{"
            + "  var b=JSON.parse(localStorage.getItem('chc_org_branding')||'null');"
            + "  if(b&&b.orgName){document.getElementById('org-name').textContent=b.orgName;document.getElementById('org-sub').textContent='Bienvenue !';}"
            + "  if(b&&b.logoUrl){var img=document.getElementById('org-logo');img.src=b.logoUrl;img.style.display='block';}"
            + "}catch(ex){}"
            + "try{var sv=localStorage.getItem('gas_url');if(sv&&sv.startsWith('http')){window.location.replace(sv);}}catch(ex){}"
            + "function go(){"
            + "var u=document.getElementById('u').value.trim();"
            + "var e=document.getElementById('e');"
            + "if(!u||!u.startsWith('http')){e.style.display='block';return;}"
            + "e.style.display='none';"
            + "try{localStorage.setItem('gas_url',u);}catch(ex){}"
            + "window.location.replace(u);"
            + "}"
            + "document.getElementById('u').addEventListener('keydown',function(ev){if(ev.key==='Enter')go();});"
            + "</script>"
            + "</body></html>";

        webView.loadDataWithBaseURL("https://app.churchlocal", html, "text/html", "UTF-8", null);
    }

    public class AndroidBridge {
        @JavascriptInterface
        public boolean isAPK() { return true; }

        @JavascriptInterface
        public boolean hasMicPermission() {
            return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        }

        @JavascriptInterface
        public void requestMicPermission() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    requestPermissions(new String[]{ Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA }, PERM_CODE);
                }
            });
        }

        // ✅ Appelé par le frontend GAS après chargement de l'org
        @JavascriptInterface
        public void updateBranding(final String orgName, final String logoUrl) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (orgName != null && !orgName.isEmpty()) setTitle(orgName);
                    if (logoUrl != null && !logoUrl.isEmpty()) loadLogoAsTaskIcon(orgName, logoUrl);
                }
            });
        }
    }

    // Met à jour l'icône dans le gestionnaire de tâches Android
    private void loadLogoAsTaskIcon(final String orgName, final String logoUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(logoUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.connect();
                    InputStream input = conn.getInputStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setTaskDescription(new ActivityManager.TaskDescription(
                                    orgName != null ? orgName : "Church App",
                                    bitmap,
                                    0xFF075E54
                                ));
                            }
                        });
                    }
                } catch (Exception e) { /* Silencieux */ }
            }
        }).start();
    }

    private void requestNativePermissions() {
        String[] perms = { Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS };
        java.util.List<String> needed = new java.util.ArrayList<>();
        for (String p : perms) { if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) needed.add(p); }
        if (!needed.isEmpty()) requestPermissions(needed.toArray(new String[0]), PERM_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (webView != null) webView.reload();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (webView != null) webView.saveState(out);
    }
}
