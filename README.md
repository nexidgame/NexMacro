# Nex Macro

Auto clicker & macro modern untuk Android 11 – 16 (`com.nex.macro`).

## Fitur
- **Mode Target Tunggal** – overlay bar `[geser | ▶ | ✕ | geser]`, ketuk sisi untuk ganti sudut klik, bar hijau saat berjalan.
- **Mode Multi Target** – panel ▶ ＋ － ⚙ ›; target bernomor berurutan, target **geser (swipe)** (tahan ＋), ciutkan overlay, pindahkan panel (tahan ›).
- **Edit per target** – jeda awal/akhir, durasi, ulangi, nonaktifkan setelah N kali, radius acak, aktif/nonaktif, hapus.
- **Konfigurasi** – simpan / muat / perbarui dari overlay, plus layar **Konfigurasi Tersimpan** (lihat di layar 👁, ubah ✏️, salin, hapus, tombol ＋) dan editor konfigurasi: ganti nama, daftar aksi yang bisa diurutkan dengan tarik, ubah posisi/jeda tiap aksi, salin & hapus aksi.
- **Stop After** 4 pemicu: tanpa batas, setelah N menit, pada jam tertentu, atau setelah N putaran; **Pause On App Change**, **Show Active Targets**, **Target Programmability**.
- **Auto Start** – pilih aplikasi yang memunculkan overlay (dan opsional langsung mengetuk) saat dibuka.
- Penghentian darurat saat layar mati, halaman petunjuk, UI gelap modern (Material 3), Bahasa Indonesia & Inggris.

## Build
Butuh JDK 17 dan Android SDK 36.
```
gradle wrapper --gradle-version 8.11.1   # sekali saja, membuat gradlew
./gradlew assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`. Atau buka di Android Studio (Ladybug+), atau push ke GitHub:
workflow `.github/workflows/build.yml` otomatis membuat APK (Actions → Artifacts).

## Cara pakai
1. Pasang APK, buka Nex Macro, ketuk **Aktifkan layanan** lalu aktifkan di Aksesibilitas.
   Android 13+ (APK dari luar Play Store): Info Aplikasi → ⋮ → **Izinkan pengaturan terbatas**.
2. Pilih mode → **Mulai** → buka aplikasi target → tekan ▶.

## Catatan teknis
- Klik dikirim via `AccessibilityService.dispatchGesture`; overlay memakai `TYPE_ACCESSIBILITY_OVERLAY`
  (tanpa izin "tampil di atas aplikasi lain", tanpa foreground service).
- minSdk 30 (Android 11), target/compile SDK 36 (Android 16), edge-to-edge sudah ditangani.
- Gunakan secara bertanggung jawab; sebagian game/aplikasi melarang otomatisasi.

Lisensi: MIT
