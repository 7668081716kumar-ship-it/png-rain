package com.pngrain.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.*;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> picker;
    private Uri pendingUri;
    private OutputStream out;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (picker != null) picker.onReceiveValue(null);
                picker = cb;
                try { startActivityForResult(p.createIntent(), 1); }
                catch (Exception e) { picker = null; return false; }
                return true;
            }
        });
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public void beginSave(String name, String mime) {
                try {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Video.Media.DISPLAY_NAME, name);
                    cv.put(MediaStore.Video.Media.MIME_TYPE, mime);
                    cv.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PNGRain");
                    cv.put(MediaStore.Video.Media.IS_PENDING, 1);
                    ContentResolver cr = getContentResolver();
                    pendingUri = cr.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv);
                    out = cr.openOutputStream(pendingUri);
                } catch (Exception e) { toast("Save failed"); }
            }
            @JavascriptInterface public void appendSave(String b64) {
                try { out.write(Base64.decode(b64, Base64.DEFAULT)); } catch (Exception e) { toast("Save failed"); }
            }
            @JavascriptInterface public void endSave() {
                try {
                    out.close();
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Video.Media.IS_PENDING, 0);
                    getContentResolver().update(pendingUri, cv, null, null);
                    toast("Saved to Movies/PNGRain");
                } catch (Exception e) { toast("Save failed"); }
            }
        }, "Android");
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    private void toast(final String m) { runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show()); }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req == 1 && picker != null) {
            picker.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            picker = null;
        }
    }

    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
