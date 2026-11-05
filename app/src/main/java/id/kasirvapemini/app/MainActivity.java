package id.kasirvapemini.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import java.io.OutputStream;
import java.io.IOException;

public class MainActivity extends Activity {
    private WebView webView;
    private String backupData = "";
    private static final int CREATE_FILE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void saveBackup(String data) {
                runOnUiThread(() -> {
                    backupData = data;
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    intent.putExtra(Intent.EXTRA_TITLE, "backup-kasir-vape-mini.json");
                    startActivityForResult(intent, CREATE_FILE);
                });
            }
        }, "Android");

        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                OutputStream output = getContentResolver().openOutputStream(uri);
                output.write(backupData.getBytes("UTF-8"));
                output.close();
                webView.evaluateJavascript(
                    "alert('Backup berhasil disimpan!')", null
                );
            } catch (IOException e) {
                webView.evaluateJavascript(
                    "alert('Gagal menyimpan backup')", null
                );
            }
        }
    }
}
