package android.content;
public abstract class Context {
  public Context getApplicationContext() { return null; }
  public static final int MODE_PRIVATE = 0;
  public android.content.pm.PackageManager getPackageManager(){ return null; }
  public String getPackageName(){ return null; }
  public SharedPreferences getSharedPreferences(String n, int m){ return null; }
  public Intent registerReceiver(BroadcastReceiver r, IntentFilter f){ return null; }
  public void unregisterReceiver(BroadcastReceiver r){  }
  public void startActivity(Intent i){  }
  public int checkSelfPermission(String p){ return 0; }
  public android.content.res.AssetManager getAssets(){ return null; }
}
