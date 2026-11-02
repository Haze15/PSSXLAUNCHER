package android.webkit;
public class WebViewClient {
  public boolean shouldOverrideUrlLoading(WebView v, String u) { return false; }
  public WebResourceResponse shouldInterceptRequest(WebView v, String u) { return null; }
  public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail d) { return false; }
}
