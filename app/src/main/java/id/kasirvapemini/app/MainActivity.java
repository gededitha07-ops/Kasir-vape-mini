
package id.kasirvapemini.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.webkit.JavascriptInterface;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.OutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    private WebView webView;
    private String backupData = "";
    private ValueCallback<Uri[]> fileCallback;

    private BluetoothSocket printerSocket;

    private static final int CREATE_FILE = 1001;
    private static final int OPEN_FILE = 1002;
    private static final int REQUEST_BLUETOOTH = 1003;

    private static final UUID SPP_UUID = UUID.fromString(
        "00001101-0000-1000-8000-00805F9B34FB"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                WebView view,
                ValueCallback<Uri[]> callback,
                FileChooserParams params
            ) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = callback;

                try {
                    Intent intent = params.createIntent();
                    intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

                    startActivityForResult(intent, OPEN_FILE);
                    return true;

                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
            }
        });

        webView.addJavascriptInterface(new Object() {

            // =========================
            // BACKUP
            // =========================

            @JavascriptInterface
            public void saveBackup(String data) {
                runOnUiThread(() -> {
                    backupData = data;

                    Intent intent = new Intent(
                        Intent.ACTION_CREATE_DOCUMENT
                    );

                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");

                    intent.putExtra(
                        Intent.EXTRA_TITLE,
                        "backup-kasir-vape-mini.json"
                    );

                    startActivityForResult(intent, CREATE_FILE);
                });
            }

            // =========================
            // BAGIKAN STRUK
            // =========================

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

            // =========================
            // CETAK PDF STRUK
            // =========================

            @JavascriptInterface
            public void printReceipt(String receipt) {
                runOnUiThread(() -> {
                    WebView printView = new WebView(MainActivity.this);

                    String html =
                        "<html><body><pre style='font-size:14px;'>"
                        + receipt.replace("&", "&amp;")
                                 .replace("<", "&lt;")
                                 .replace(">", "&gt;")
                        + "</pre></body></html>";

                    printView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(
                            WebView view, String url
                        ) {
                            PrintManager manager =
                                (PrintManager) getSystemService(
                                    PRINT_SERVICE
                                );

                            manager.print(
                                "Struk-Twenty-One-Vapor",
                                view.createPrintDocumentAdapter(
                                    "Struk-Twenty-One-Vapor"
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

            // =========================
            // CETAK PDF LAPORAN
            // =========================

            @JavascriptInterface
            public void printReport(String report) {
                runOnUiThread(() -> {
                    WebView printView = new WebView(MainActivity.this);

                    printView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(
                            WebView view, String url
                        ) {
                            PrintManager manager =
                                (PrintManager) getSystemService(
                                    PRINT_SERVICE
                                );

                            manager.print(
                                "Laporan-Twenty-One-Vapor",
                                view.createPrintDocumentAdapter(
                                    "Laporan-Twenty-One-Vapor"
                                ),
                                null
                            );
                        }
                    });

                    printView.loadDataWithBaseURL(
                        null, report, "text/html", "UTF-8", null
                    );
                });
            }

            // =========================
            // DAFTAR PRINTER TERPASANG
            // =========================

            @JavascriptInterface
            public String getPairedPrinters() {

                if (!mintaIzinBluetooth()) {
                    return "[]";
                }

                try {
                    BluetoothAdapter adapter =
                        BluetoothAdapter.getDefaultAdapter();

                    if (adapter == null || !adapter.isEnabled()) {
                        return "[]";
                    }

                    Set<BluetoothDevice> devices =
                        adapter.getBondedDevices();

                    JSONArray hasil = new JSONArray();

                    for (BluetoothDevice device : devices) {
                        JSONObject item = new JSONObject();

                        item.put("name", device.getName());
                        item.put("address", device.getAddress());

                        hasil.put(item);
                    }

                    return hasil.toString();

                } catch (Exception e) {
                    return "[]";
                }
            }

            // =========================
            // HUBUNGKAN PRINTER
            // =========================

            @JavascriptInterface
            public void connectPrinter(String address) {

                if (!mintaIzinBluetooth()) {
                    return;
                }

                new Thread(() -> {
                    try {
                        BluetoothAdapter adapter =
                            BluetoothAdapter.getDefaultAdapter();

                        if (adapter == null || !adapter.isEnabled()) {
                            tampilkanPesan("Bluetooth belum aktif");
                            return;
                        }

                        BluetoothDevice device =
                            adapter.getRemoteDevice(address);

                        if (printerSocket != null) {
                            try {
                                printerSocket.close();
                            } catch (Exception ignored) {
                            }
                        }

                        BluetoothSocket socket =
                            device.createRfcommSocketToServiceRecord(
                                SPP_UUID
                            );

                        socket.connect();
                        printerSocket = socket;

                        tampilkanPesan(
                            "Printer berhasil terhubung"
                        );

                    } catch (Exception e) {
                        printerSocket = null;

                        tampilkanPesan(
                            "Gagal menghubungkan printer. "
                            + "Pastikan printer sudah dipasangkan."
                        );
                    }
                }).start();
            }

            // =========================
            // PUTUSKAN PRINTER
            // =========================

            @JavascriptInterface
            public void disconnectPrinter() {
                try {
                    if (printerSocket != null) {
                        printerSocket.close();
                        printerSocket = null;
                    }

                    tampilkanPesan("Printer diputus");

                } catch (Exception e) {
                    tampilkanPesan("Gagal memutus printer");
                }
            }

            // =========================
            // CETAK LANGSUNG BLUETOOTH
            // paperWidth: 58 atau 80
            // =========================

            @JavascriptInterface
            public void printBluetooth(
                String text,
                int paperWidth
            ) {
                new Thread(() -> {
                    try {
                        if (printerSocket == null ||
                            !printerSocket.isConnected()) {

                            tampilkanPesan(
                                "Printer belum terhubung"
                            );
                            return;
                        }

                        int lebar = paperWidth == 80 ? 48 : 32;

                        String hasil = rapikanTeks(text, lebar);

                        OutputStream output =
                            printerSocket.getOutputStream();

                        // ESC/POS: inisialisasi printer
                        output.write(new byte[] {
                            0x1B, 0x40
                        });

                        output.write(
                            hasil.getBytes(
                                Charset.forName("US-ASCII")
                            )
                        );

                        // Tambahkan jarak kertas di bagian akhir
                        output.write(new byte[] {
                            0x0A, 0x0A, 0x0A, 0x0A
                        });

                        output.flush();

                        tampilkanPesan(
                            "Struk berhasil dikirim ke printer"
                        );

                    } catch (Exception e) {
                        tampilkanPesan(
                            "Gagal mencetak. Periksa koneksi printer."
                        );
                    }
                }).start();
            }

        }, "Android");

        webView.loadUrl(
            "file:///android_asset/index.html"
        );

        setContentView(webView);
    }

    // =========================
    // IZIN BLUETOOTH ANDROID 12+
    // =========================

    private boolean mintaIzinBluetooth() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (checkSelfPermission(
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED) {

                runOnUiThread(() -> {
                    requestPermissions(
                        new String[] {
                            android.Manifest.permission.BLUETOOTH_CONNECT
                        },
                        REQUEST_BLUETOOTH
                    );
                });

                return false;
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );

        if (requestCode == REQUEST_BLUETOOTH) {

            if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                tampilkanPesan(
                    "Izin Bluetooth diberikan. Tekan refresh printer lagi."
                );

            } else {
                tampilkanPesan(
                    "Izin Bluetooth diperlukan untuk menggunakan printer."
                );
            }
        }
    }

    // =========================
    // FORMAT TEKS STRUK
    // =========================

    private String rapikanTeks(String teks, int lebar) {

        String aman = teks
            .replace("×", "x")
            .replace("—", "-")
            .replace("–", "-")
            .replace("•", "-")
            .replace("\r", "");

        StringBuilder hasil = new StringBuilder();

        String[] baris = aman.split("\n");

        for (String barisAsli : baris) {

            String s = barisAsli;

            if (s.length() == 0) {
                hasil.append("\n");
                continue;
            }

            while (s.length() > lebar) {

                int potong = s.lastIndexOf(" ", lebar);

                if (potong <= 0) {
                    potong = lebar;
                }

                hasil.append(
                    s.substring(0, potong).trim()
                ).append("\n");

                s = s.substring(potong).trim();
            }

            hasil.append(s).append("\n");
        }

        return hasil.toString();
    }

    // =========================
    // PESAN DARI JAVA KE APLIKASI
    // =========================

    private void tampilkanPesan(String pesan) {

        runOnUiThread(() -> {
            String json = JSONObject.quote(pesan);

            webView.evaluateJavascript(
                "alert(" + json + ")",
                null
            );
        });
    }

    // =========================
    // BACA FILE BACKUP
    // =========================

    private String bacaFile(Uri uri) throws IOException {

        StringBuilder hasil = new StringBuilder();

        try (
            InputStream input =
                getContentResolver().openInputStream(uri);

            BufferedReader reader =
                new BufferedReader(
                    new InputStreamReader(input, "UTF-8")
                )
        ) {
            String baris;

            while ((baris = reader.readLine()) != null) {
                hasil.append(baris).append("\n");
            }
        }

        return hasil.toString();
    }

    // =========================
    // HASIL PILIH / SIMPAN FILE
    // =========================

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(
            requestCode, resultCode, data
        );

        if (requestCode == OPEN_FILE) {

            if (fileCallback != null) {

                if (resultCode == RESULT_OK &&
                    data != null &&
                    data.getData() != null) {

                    Uri uri = data.getData();

                    try {
                        String isiBackup = bacaFile(uri);

                        if (isiBackup.trim().isEmpty()) {
                            throw new IOException(
                                "File backup kosong"
                            );
                        }

                        String json =
                            JSONObject.quote(isiBackup);

                        webView.evaluateJavascript(
                            "pulihkanDariTeks(" + json + ")",
                            null
                        );

                    } catch (Exception e) {

                        webView.evaluateJavascript(
                            "alert('Gagal membaca file backup')",
                            null
                        );
                    }
                }

                fileCallback.onReceiveValue(null);
                fileCallback = null;
            }

            return;
        }

        if (requestCode == CREATE_FILE &&
            resultCode == RESULT_OK &&
            data != null &&
            data.getData() != null) {

            Uri uri = data.getData();

            try {
                OutputStream output =
                    getContentResolver().openOutputStream(uri);

                if (output == null) {
                    throw new IOException(
                        "Tidak bisa membuka file"
                    );
                }

                output.write(
                    backupData.getBytes("UTF-8")
                );

                output.close();

                webView.evaluateJavascript(
                    "alert('Backup berhasil disimpan!')",
                    null
                );

            } catch (IOException e) {

                webView.evaluateJavascript(
                    "alert('Gagal menyimpan backup')",
                    null
                );
            }
        }
    }
}
