package com.haze15.pssxlauncher;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.webkit.JavascriptInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Methods the page calls as Android.xxx(). They run on a WebView binder thread. */
public class Bridge {

    private final Home home;
    private final SharedPreferences prefs;
    private static final HashMap<String, byte[]> icons = new HashMap<String, byte[]>();

    Bridge(Home h) {
        home = h;
        prefs = h.getSharedPreferences("accueil", Context.MODE_PRIVATE);
    }

    static synchronized void clearCache() { icons.clear(); }

    /** JSON list of launchable apps: [{k:"pkg/cls", n:"Label", b:hasBanner}] */
    @JavascriptInterface
    public String apps() {
        PackageManager pm = home.getPackageManager();
        StringBuilder sb = new StringBuilder("[");
        HashSet<String> seen = new HashSet<String>();
        String[] cats = Build.VERSION.SDK_INT >= 21
                ? new String[]{"android.intent.category.LEANBACK_LAUNCHER", Intent.CATEGORY_LAUNCHER}
                : new String[]{Intent.CATEGORY_LAUNCHER};
        for (String cat : cats) {
            Intent q = new Intent(Intent.ACTION_MAIN);
            q.addCategory(cat);
            List<ResolveInfo> list;
            try { list = pm.queryIntentActivities(q, 0); } catch (Throwable t) { continue; }
            for (ResolveInfo ri : list) {
                ActivityInfo ai = ri.activityInfo;
                if (ai == null || home.getPackageName().equals(ai.packageName)) continue;
                if (!seen.add(ai.packageName)) continue;          // one tile per app
                boolean banner = Build.VERSION.SDK_INT >= 20 && (ai.banner != 0 || ai.applicationInfo.banner != 0);
                String label;
                try { label = String.valueOf(ri.loadLabel(pm)); } catch (Throwable t) { label = ai.packageName; }
                if (sb.length() > 1) sb.append(',');
                sb.append("{\"k\":").append(q(ai.packageName + "/" + ai.name))
                  .append(",\"n\":").append(q(label))
                  .append(",\"b\":").append(banner)
                  .append(",\"tv\":").append(cat.startsWith("android.intent.category.LEANBACK"))
                  .append(",\"c\":").append(Build.VERSION.SDK_INT >= 26 ? ai.applicationInfo.category : -1)
                  .append(",\"g\":").append((ai.applicationInfo.flags & 0x02000000) != 0).append('}');
            }
        }
        return sb.append(']').toString();
    }

    @JavascriptInterface
    public boolean launch(String key) {
        int slash = key.indexOf('/');
        if (slash < 0) return false;
        final Intent i = new Intent(Intent.ACTION_MAIN);
        i.setComponent(new ComponentName(key.substring(0, slash), key.substring(slash + 1)));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        return start(i) || start(home.getPackageManager().getLaunchIntentForPackage(key.substring(0, slash)));
    }

    /** Android's page for one app (storage, force stop, uninstall…). */
    @JavascriptInterface
    public void appInfo(String pkg) {
        Intent i = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
        i.setData(android.net.Uri.parse("package:" + pkg));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        start(i);
    }

    @JavascriptInterface
    public String version() { return "1.0"; }

    @JavascriptInterface
    public void settings() {
        if (!start(new Intent("android.settings.SETTINGS"))) start(new Intent("android.settings.WIRELESS_SETTINGS"));
    }

    /** Starts the screensaver right now (the weather screensaver if it is the chosen one). */
    @JavascriptInterface
    public void screensaver() {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setComponent(new ComponentName("com.android.systemui", "com.android.systemui.Somnambulator"));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        start(i);
    }

    private boolean start(final Intent i) {
        if (i == null) return false;
        if (i.resolveActivity(home.getPackageManager()) == null) return false;
        home.ui.post(new Runnable() {
            public void run() {
                try { home.startActivity(i); } catch (Throwable ignored) {}
            }
        });
        return true;
    }

    @JavascriptInterface
    public String get(String k) { return prefs.getString(k, ""); }

    @JavascriptInterface
    public void set(String k, String v) { prefs.edit().putString(k, v).apply(); }

    @JavascriptInterface
    public boolean hasPerm() {
        return Build.VERSION.SDK_INT < 23
                || home.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @JavascriptInterface
    public void askPerm() {
        if (Build.VERSION.SDK_INT < 23) return;
        home.ui.post(new Runnable() {
            public void run() {
                home.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        });
    }

    /** JSON list of picture paths on the box and on USB keys (Pictures, Download, DCIM, Fonds…). */
    @JavascriptInterface
    public String photos() {
        ArrayList<File> roots = new ArrayList<File>();
        roots.add(Environment.getExternalStorageDirectory());
        File[] st = new File("/storage").listFiles();
        if (st != null) for (File f : st) {
            String n = f.getName();
            if (!n.equals("emulated") && !n.equals("self") && f.isDirectory()) roots.add(f);
        }
        String[] subs = {"", "Fonds", "Pictures", "Download", "DCIM", "DCIM/Camera", "Pictures/Screenshots"};
        StringBuilder sb = new StringBuilder("[");
        HashSet<String> seen = new HashSet<String>();
        int count = 0;
        for (File r : roots) for (String s : subs) {
            File d = s.length() == 0 ? r : new File(r, s);
            File[] fs = d.listFiles();
            if (fs == null) continue;
            for (File f : fs) {
                String n = f.getName().toLowerCase();
                if (!f.isFile() || n.startsWith(".")) continue;
                if (!(n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp"))) continue;
                if (!seen.add(f.getAbsolutePath())) continue;
                if (sb.length() > 1) sb.append(',');
                sb.append(q(f.getAbsolutePath()));
                if (++count >= 300) return sb.append(']').toString();
            }
        }
        return sb.append(']').toString();
    }

    // ---------------------------------------------------------------- images served to the page

    static synchronized byte[] icon(Context c, String key) {
        byte[] got = icons.get(key);
        if (got != null) return got;
        PackageManager pm = c.getPackageManager();
        int slash = key.indexOf('/');
        Drawable d = null;
        try {
            ComponentName cn = new ComponentName(key.substring(0, slash), key.substring(slash + 1));
            ActivityInfo ai = pm.getActivityInfo(cn, 0);
            if (Build.VERSION.SDK_INT >= 20) d = ai.loadBanner(pm);
            if (d == null) d = ai.loadIcon(pm);
        } catch (Throwable t) {
            try { d = pm.getApplicationIcon(key.substring(0, Math.max(0, slash))); } catch (Throwable ignored) {}
        }
        if (d == null) return null;
        Bitmap bm;
        if (d instanceof BitmapDrawable && ((BitmapDrawable) d).getBitmap() != null) {
            bm = ((BitmapDrawable) d).getBitmap();
        } else {
            int w = d.getIntrinsicWidth(), h = d.getIntrinsicHeight();
            if (w <= 0 || h <= 0) { w = 192; h = 192; }
            float k = Math.min(1f, 640f / Math.max(w, h));
            w = Math.max(1, (int) (w * k)); h = Math.max(1, (int) (h * k));
            bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bm);
            d.setBounds(0, 0, w, h);
            d.draw(cv);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, out);
        byte[] bytes = out.toByteArray();
        icons.put(key, bytes);
        return bytes;
    }

    /** A picture from the box's storage, scaled down to at most maxW pixels wide, as JPEG. */
    static byte[] photo(String path, int maxW) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        if (o.outWidth <= 0) return null;
        int sample = 1;
        while (o.outWidth / (sample * 2) >= maxW) sample *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        Bitmap bm = BitmapFactory.decodeFile(path, o);
        if (bm == null) return null;
        if (bm.getWidth() > maxW * 1.3) {
            Bitmap s = Bitmap.createScaledBitmap(bm, maxW, Math.max(1, bm.getHeight() * maxW / bm.getWidth()), true);
            bm.recycle();
            bm = s;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 88, out);
        bm.recycle();
        return out.toByteArray();
    }

    static String q(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 0x20 || c == 0x2028 || c == 0x2029) b.append(String.format("\\u%04x", (int) c));
            else b.append(c);
        }
        return b.append('"').toString();
    }
}
