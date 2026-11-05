
package id.kasirvapemini.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
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
        webView.setWebChromeClient(new android.webkit.WebChromeClient());
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
                    intent.putExtra(Intent.EXTRA_TITLE,
                        "backup-kasir-vape-mini.json");
                    startActivityForResult(intent, CREATE_FILE);
                });
            }

            @JavascriptInterface
            public void shareReceipt(String receipt) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, receipt);
                    startActivity(Intent.createChooser(
                        intent, "Bagikan struk"
                    ));
                });
            }

            @JavascriptInterface
            public void printReceipt(String receipt) {
                runOnUiThread(() -> {
                    WebView printView = new WebView(MainActivity.this);
                    String html = "<html><body><pre style='font-size:14px;'>"
                        + receipt.replace("&", "&amp;")
                                 .replace("<", "&lt;")
                                 .replace(">", "&gt;")
                        + "</pre></body></html>";

                    printView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            PrintManager manager =
                                (PrintManager) getSystemService(PRINT_SERVICE);

                            manager.print(
                                "Struk-Kasir-Vape-Mini",
                                view.createPrintDocumentAdapter(
                                    "Struk-Kasir-Vape-Mini"
                                ),
                                null
                            );
                        }
                    });

                    printView.loadDataWithBaseURL(
                        null, html, "text/html", "UTF-8", null
                    );
                });
            }
        }, "Android");

        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_FILE &&
            resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                OutputStream output =
                    getContentResolver().openOutputStream(uri);
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
