package com.haze15.pssxlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/** Opens the home screen when the box starts (and again a few seconds later, in case the stock launcher came up after us). */
public class Boot extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent i) {
        final Context app = c.getApplicationContext();
        show(app);
        final PendingResult pending = goAsync();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                show(app);
                pending.finish();
            }
        }, 6000);
    }

    static void show(Context c) {
        try {
            Intent h = new Intent(c, Home.class);
            h.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            c.startActivity(h);
        } catch (Throwable ignored) {}
    }
}
