package com.haze15.pssxlauncher;

import android.net.Uri;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;

/** Serves http://home.local/… from the APK's assets, app icons and the box's pictures. */
public class Client extends WebViewClient {

    private static final String HOST = "http://home.local/";
    private final Home home;

    Client(Home h) { home = h; }

    @Override
    public boolean shouldOverrideUrlLoading(WebView v, String url) {
        return !url.startsWith(HOST);   // never navigate away from the home page
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView v, String url) {
        if (url == null || !url.startsWith(HOST)) return null;
        try {
            Uri u = Uri.parse(url);
            String path = u.getPath();
            if (path == null || path.equals("/")) path = "/index.html";
            if (path.equals("/icon")) {
                byte[] b = Bridge.icon(home, u.getQueryParameter("k"));
                return b == null ? notFound() : ok("image/png", new ByteArrayInputStream(b), true);
            }
            if (path.equals("/photo")) {
                int w = 1920;
                try { w = Integer.parseInt(u.getQueryParameter("w")); } catch (Throwable ignored) {}
                byte[] b = Bridge.photo(u.getQueryParameter("p"), w);
                return b == null ? notFound() : ok("image/jpeg", new ByteArrayInputStream(b), true);
            }
            String asset = path.substring(1);
            if (asset.contains("..")) return notFound();
            InputStream in = home.getAssets().open(asset);
            return ok(mime(asset), in, !asset.endsWith(".html"));
        } catch (Throwable t) {
            return notFound();
        }
    }

    private static WebResourceResponse ok(String mime, InputStream in, boolean cache) {
        HashMap<String, String> h = new HashMap<String, String>();
        h.put("Cache-Control", cache ? "max-age=86400" : "no-cache");
        h.put("Access-Control-Allow-Origin", "*");
        return new WebResourceResponse(mime, mime.startsWith("text") ? "utf-8" : null, 200, "OK", h, in);
    }

    private static WebResourceResponse notFound() {
        return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found",
                new HashMap<String, String>(), new ByteArrayInputStream(new byte[0]));
    }

    private static String mime(String p) {
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".js")) return "text/javascript";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".woff2")) return "font/woff2";
        return "image/jpeg";
    }

    /** The page's renderer died (low memory): rebuild the home screen instead of crashing. */
    @Override
    public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail d) {
        home.web = null;
        try { v.destroy(); } catch (Throwable ignored) {}
        home.ui.post(new Runnable() {
            public void run() { home.recreate(); }
        });
        return true;
    }
}
