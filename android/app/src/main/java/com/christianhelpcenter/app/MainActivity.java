package com.christianhelpcenter.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        String html = "<!DOCTYPE html><html><head>"
            + "<meta charset='UTF-8'/>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'/>"
            + "<style>"
            + "*{margin:0;padding:0;box-sizing:border-box}"
            + "body{font-family:system-ui,sans-serif;background:linear-gradient(145deg,#2563eb,#06b6d4);min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}"
            + ".card{background:#fff;border-radius:20px;padding:28px;width:100%;max-width:400px;box-shadow:0 20px 60px rgba(0,0,0,0.2)}"
            + ".logo{text-align:center;font-size:52px;margin-bottom:12px}"
            + "h1{text-align:center;color:#1f2937;font-size:20px;margin-bottom:4px}"
            + "p{text-align:center;color:#6b7280;font-size:13px;margin-bottom:24px}"
            + "label{font-size:13px;font-weight:600;color:#374151;display:block;margin-bottom:6px}"
            + "input{width:100%;padding:12px;border:2px solid #e5e7eb;border-radius:10px;font-size:14px;margin-bottom:16px}"
            + "button{background:#2563eb;color:#fff;border:none;padding:14px;border-radius:10px;width:100%;font-size:16px;font-weight:700;cursor:pointer}"
            + ".hint{color:#9ca3af;font-size:11px;text-align:center;margin-top:12px;line-height:1.5}"
            + "</style></head><body>"
            + "<div class='card'>"
            + "<div class='logo'>✝️</div>"
            + "<h1>Church App</h1>"
            + "<p>Connectez votre église</p>"
            + "<label>🔗 URL de votre application</label>"
            + "<input type='url' id='url' placeholder='https://script.google.com/macros/s/...'/>"
            + "<button onclick='go()'>Se connecter →</button>"
            + "<div class='hint'>Entrez le lien fourni par votre administrateur</div>"
            + "</div>"
            + "<script>"
            + "var saved=localStorage.getItem('gas_url');"
            + "if(saved){window.location.href=saved;}"
            + "function go(){"
            + "var u=document.getElementById('url').value.trim();"
            + "if(u.startsWith('http')){localStorage.setItem('gas_url',u);window.location.href=u;}"
            + "else{alert('Veuillez entrer un lien valide');}"
            + "}"
            + "</script>"
            + "</body></html>";

        webView.loadDataWithBaseURL("https://app.local", html, "text/html", "UTF-8", null);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
