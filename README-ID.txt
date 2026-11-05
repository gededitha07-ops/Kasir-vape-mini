KASIR VAPE MINI - PROYEK APK ANDROID

Status:
- Ini adalah SOURCE PROJECT, bukan APK siap instal.
- APK harus dibangun melalui GitHub Actions atau Android Studio.
- Workflow GitHub Actions tersedia di .github/workflows/build-apk.yml.

Fitur versi awal:
- Login Pemilik/Kasir (PIN awal 1234 dan 0000)
- Produk: nama, harga modal, harga jual, stok
- Margin otomatis
- Keranjang dan checkout
- Stok berkurang otomatis setelah transaksi
- Konfirmasi pemeriksaan usia
- Pembayaran Tunai/QRIS/Transfer/E-Wallet
- Riwayat transaksi dan struk cetak/PDF
- Laporan omzet, laba kotor, jumlah transaksi
- Backup JSON dan pemulihan
- Penyimpanan lokal WebView (offline)

PENTING:
1. Ganti PIN awal setelah masuk sebagai Pemilik.
2. Data tersimpan di penyimpanan aplikasi pada perangkat. Menghapus aplikasi/data aplikasi dapat menghapus data; lakukan backup.
3. Cetak struk menggunakan dialog cetak Android. Kompatibilitas printer Bluetooth ESC/POS khusus belum diimplementasikan dan perlu tahap pengembangan lanjutan.
4. Laporan laba adalah laba kotor transaksi yang tersimpan; belum memperhitungkan biaya operasional.
5. Uji aplikasi dan buat backup sebelum digunakan untuk transaksi toko sungguhan.

Cara membangun:
1. Buat repository GitHub baru bernama Kasir-Vape-Mini.
2. Unggah seluruh isi ZIP ini ke root repository (termasuk folder .github).
3. Buka tab Actions, pilih "Build Kasir Vape Mini APK", lalu klik Run workflow jika belum berjalan.
4. Setelah selesai sukses, buka hasil run dan unduh artifact "Kasir-Vape-Mini-debug".
5. Ekstrak ZIP artifact, lalu buka app-debug.apk di Redmi Pad 2 untuk memasang. Android mungkin meminta izin instalasi dari sumber tersebut.
