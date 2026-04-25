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

        // WebView directement sans layout XML
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

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, android.webkit.WebResourceRequest r) {
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        // Page HTML chargée localement — pas besoin d'internet au démarrage
        String html = "<!DOCTYPE html><html><head>"
            + "<meta charset='UTF-8'/>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1,viewport-fit=cover'/>"
            + "<style>"
            + "*{margin:0;padding:0;box-sizing:border-box}"
            + "body{font-family:sans-serif;background:linear-gradient(145deg,#2563eb,#06b6d4);min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}"
            + ".card{background:#fff;border-radius:20px;padding:28px;width:100%;max-width:400px}"
            + ".logo{text-align:center;font-size:52px;margin-bottom:12px}"
            + "h1{text-align:center;color:#1f2937;font-size:20px;margin-bottom:4px}"
            + "p{text-align:center;color:#6b7280;font-size:13px;margin-bottom:20px}"
            + "label{font-size:13px;font-weight:600;color:#374151;display:block;margin-bottom:6px}"
            + "input{width:100%;padding:12px;border:2px solid #e5e7eb;border-radius:10px;font-size:14px;outline:none;margin-bottom:4px}"
            + ".err{color:#ef4444;font-size:12px;margin-bottom:12px;display:none}"
            + "button{background:#2563eb;color:#fff;border:none;padding:14px;border-radius:10px;width:100%;font-size:16px;font-weight:700;cursor:pointer}"
            + ".hint{color:#9ca3af;font-size:11px;text-align:center;margin-top:14px;line-height:1.5}"
            + "</style></head><body>"
            + "<div class='card'>"
            + "<div class='logo'>&#x271D;&#xFE0F;</div>"
            + "<h1>Church App</h1>"
            + "<p>Connectez votre &eacute;glise</p>"
            + "<label>&#x1F517; URL de votre application</label>"
            + "<input type='url' id='u' placeholder='https://script.google.com/macros/s/...'/>"
            + "<div class='err' id='e'>Veuillez entrer un lien valide (https://...)</div>"
            + "<button onclick='go()'>Se connecter &#x2192;</button>"
            + "<div class='hint'>&#x1F4A1; Ce lien vous est fourni par votre administrateur d&#x27;&#xe9;glise. L&#x27;app s&#x27;en souvient automatiquement.</div>"
            + "</div>"
            + "<script>"
            + "try{"
            + "var s=localStorage.getItem('gas_url');"
            + "if(s&&s.startsWith('http')){window.location.replace(s);}"
            + "}catch(ex){}"
            + "function go(){"
            + "var u=document.getElementById('u').value.trim();"
            + "var e=document.getElementById('e');"
            + "if(!u||!u.startsWith('http')){e.style.display='block';return;}"
            + "e.style.display='none';"
            + "try{localStorage.setItem('gas_url',u);}catch(ex){}"
            + "window.location.replace(u);"
            + "}"
            + "document.getElementById('u').addEventListener('keydown',function(ev){"
            + "if(ev.key==='Enter')go();"
            + "});"
            + "</script>"
            + "</body></html>";

        webView.loadDataWithBaseURL(
            "https://app.churchlocal",
            html,
            "text/html",
            "UTF-8",
            null
        );
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (webView != null) webView.saveState(out);
    }

    @Override
    protected void onRestoreInstanceState(Bundle saved) {
        super.onRestoreInstanceState(saved);
        if (webView != null) webView.restoreState(saved);
    }
}
