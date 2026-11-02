package android.content.pm;
import android.content.*;
public abstract class PackageManager {
  public static final int PERMISSION_GRANTED = 0;
  public abstract java.util.List<ResolveInfo> queryIntentActivities(Intent i, int f);
  public abstract Intent getLaunchIntentForPackage(String p);
  public abstract ActivityInfo getActivityInfo(ComponentName c, int f) throws NameNotFoundException;
  public abstract android.graphics.drawable.Drawable getApplicationIcon(String p) throws NameNotFoundException;
  public static class NameNotFoundException extends Exception {}
}
