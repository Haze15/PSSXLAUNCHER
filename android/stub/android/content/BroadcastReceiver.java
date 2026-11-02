package android.content;
public abstract class BroadcastReceiver { public final PendingResult goAsync() { return null; }
  public static class PendingResult { public final void finish() {} }
  public abstract void onReceive(Context c, Intent i); }
