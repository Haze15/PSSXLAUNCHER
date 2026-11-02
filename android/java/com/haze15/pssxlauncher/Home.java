package com.haze15.pssxlauncher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

/** Home screen: an HTML page (assets/index.html) in a WebView, with a small Java bridge for apps & photos. */
public class Home extends Activity {

    static final String URL = "http://home.local/index.html";

    WebView web;
    final Handler ui = new Handler(Looper.getMainLooper());
    private BroadcastReceiver pkgRx, wakeRx;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        w.getDecorView().setBackgroundColor(Color.BLACK);
        try {
            web = new WebView(this);
        } catch (Throwable noWebView) {
            TextView t = new TextView(this);
            t.setText("WebView indisponible sur cette TV.");
            t.setTextColor(Color.WHITE);
            setContentView(t);
            return;
        }
        web.setBackgroundColor(Color.BLACK);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        web.addJavascriptInterface(new Bridge(this), "Android");
        web.setWebViewClient(new Client(this));
        setContentView(web);
        web.loadUrl(URL);
        web.requestFocus();

        pkgRx = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                Bridge.clearCache();
                js("window.onAppsChanged&&onAppsChanged()");
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        registerReceiver(pkgRx, f);

        // TV coming back from standby: show the home screen, like after a boot
        wakeRx = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                Boot.show(Home.this);
            }
        };
        registerReceiver(wakeRx, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    void js(final String code) {
        ui.post(new Runnable() {
            public void run() {
                if (web != null) web.evaluateJavascript(code, null);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume();
            web.resumeTimers();
            web.requestFocus();
            js("window.onHomeResume&&onHomeResume()");
        }
    }

    @Override
    protected void onPause() {
        if (web != null) { web.onPause(); web.pauseTimers(); }   // leave the CPU to the app being watched
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        js("window.onHomeKey&&onHomeKey()");
    }

    @Override
    public void onBackPressed() {
        js("window.onBackKey&&onBackKey()");   // a home screen never closes itself
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        js("window.onPerm&&onPerm()");
    }

    @Override
    protected void onDestroy() {
        if (pkgRx != null) try { unregisterReceiver(pkgRx); } catch (Throwable ignored) {}
        if (wakeRx != null) try { unregisterReceiver(wakeRx); } catch (Throwable ignored) {}
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}
