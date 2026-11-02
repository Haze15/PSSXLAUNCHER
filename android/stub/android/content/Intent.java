package android.content;
public class Intent {
  public static final String ACTION_MAIN = "android.intent.action.MAIN";
  public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
  public static final String ACTION_PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";
  public static final String ACTION_PACKAGE_REMOVED = "android.intent.action.PACKAGE_REMOVED";
  public static final String ACTION_PACKAGE_CHANGED = "android.intent.action.PACKAGE_CHANGED";
  public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
  public static final int FLAG_ACTIVITY_RESET_TASK_IF_NEEDED = 0x00200000;
  public static final String ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON";
  public Intent(Context c, Class<?> k) {}
  public Intent() {} public Intent(String a) {}
  public Intent addCategory(String c) { return this; }
  public Intent setComponent(ComponentName c) { return this; }
  public Intent setData(android.net.Uri u) { return this; }
  public Intent addFlags(int f) { return this; }
  public ComponentName resolveActivity(android.content.pm.PackageManager pm) { return null; }
}
