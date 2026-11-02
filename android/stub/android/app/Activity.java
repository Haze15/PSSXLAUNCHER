package android.app;
import android.content.*; import android.os.Bundle; import android.view.*;
public abstract class Activity extends Context {
  protected void onCreate(Bundle b) {}
  public Window getWindow() { return null; }
  public void setContentView(View v) {}
  protected void onResume() {}
  protected void onPause() {}
  protected void onNewIntent(Intent i) {}
  public void onBackPressed() {}
  public void onRequestPermissionsResult(int c, String[] p, int[] r) {}
  protected void onDestroy() {}
  public final void requestPermissions(String[] p, int c) {}
  public void recreate() {}
}
