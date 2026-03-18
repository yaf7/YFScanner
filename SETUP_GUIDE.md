# 📱 YFScanner - Setup & Run Guide

## ✅ Status Build
**BUILD SUCCESSFUL** ✓ - Semua error dan warning sudah diperbaiki!

---

## 🚀 Cara Menjalankan Aplikasi

### Opsi 1: Menjalankan di Android Studio
1. Buka Android Studio
2. File → Open → Pilih folder `D:\ANDROID STUDIO PROJECT\YFScanner`
3. Tunggu gradle sync selesai
4. Tekan `Shift + F10` atau klik tombol **Run** (hijau ▶)
5. Pilih emulator atau device fisik
6. Aplikasi akan terbuka secara otomatis

### Opsi 2: Menjalankan via Terminal/Command Prompt
```bash
cd D:\ANDROID STUDIO PROJECT\YFScanner
gradlew.bat installDebug
```

### Opsi 3: Membuat APK Release untuk Distribusi
```bash
cd D:\ANDROID STUDIO PROJECT\YFScanner
gradlew.bat assembleRelease
```
APK akan tersimpan di: `app\build\outputs\apk\release\app-release.apk`

---

## 📋 Fitur yang Sudah Lengkap

### ✓ Scan Document (Pemindaian Dokumen)
- Menggunakan ML Kit Document Scanner dari Google
- Mendukung galeri import
- Penyimpanan hingga 20 halaman per scan

### ✓ View & Edit Dokumen
- ViewPager untuk navigasi halaman
- Filter gambar (Grayscale, Magic Color, B&W, Lighten, Darken, Sharpen)
- Rotasi halaman
- Tambah halaman ke dokumen yang ada

### ✓ OCR (Optical Character Recognition)
- Ekstraksi teks dari gambar
- Salin teks ke clipboard
- Menggunakan ML Kit Text Recognition

### ✓ Export PDF
- Export dokumen ke PDF
- Support ukuran A4 dan Letter
- Buka dan bagikan PDF langsung

### ✓ Manajemen Dokumen
- List dokumen dengan thumbnail
- Cari dokumen
- Hapus dokumen
- Update nama dokumen
- Hitung storage usage

### ✓ Pengaturan
- Pilih kualitas gambar (Low, Medium, High)
- Pilih ukuran halaman PDF (A4, Letter)
- Auto enhance mode
- Dark/Light theme
- Clear semua dokumen

---

## 🗂️ Struktur Database

### Tabel: documents
- `id` - Primary Key
- `title` - Nama dokumen
- `createdAt` - Waktu pembuatan
- `updatedAt` - Waktu update terakhir
- `pageCount` - Jumlah halaman
- `thumbnailPath` - Path gambar thumbnail

### Tabel: pages
- `id` - Primary Key
- `documentId` - Foreign Key ke documents
- `pageIndex` - Nomor halaman (0-based)
- `originalImagePath` - Path gambar asli
- `processedImagePath` - Path gambar setelah filter
- `filterApplied` - Nama filter yang digunakan

---

## 📁 Struktur File Proyek

```
YFScanner/
├── app/
│   ├── src/main/
│   │   ├── java/arsetya/deyafa/yfscanner/
│   │   │   ├── MainActivity.kt .................... Activity utama
│   │   │   ├── YFScannerApp.kt ................... Application class
│   │   │   ├── ui/
│   │   │   │   ├── SplashActivity.kt ............ Splash screen
│   │   │   │   ├── DocumentViewerActivity.kt ... Viewer dokumen
│   │   │   │   ├── FilterActivity.kt ........... Filter gambar
│   │   │   │   ├── OcrActivity.kt .............. OCR/Text recognition
│   │   │   │   ├── SettingsActivity.kt ........ Pengaturan
│   │   │   │   ├── adapter/ ................... RecyclerView adapters
│   │   │   │   └── viewmodel/ ................. ViewModels
│   │   │   ├── data/
│   │   │   │   ├── db/ ....................... Room Database
│   │   │   │   ├── model/ ................... Data models
│   │   │   │   └── repository/ .............. Repository pattern
│   │   │   └── util/
│   │   │       ├── FileUtils.kt ............. File operations
│   │   │       ├── ImageProcessor.kt ....... Image filters
│   │   │       └── PdfExporter.kt .......... PDF export
│   │   ├── res/ ............................ Resources (layouts, strings, etc)
│   │   └── AndroidManifest.xml ............. Manifest
│   └── build.gradle.kts ..................... App build config
├── gradle/
│   └── libs.versions.toml .................. Dependency versions
├── build.gradle.kts ....................... Project build config
└── settings.gradle.kts .................... Project settings
```

---

## 🔧 Konfigurasi Dependencies

### Library Utama yang Digunakan
- **AndroidX Core** - Framework Android modern
- **Room Database** - Penyimpanan data lokal
- **ML Kit** - Document Scanner & Text Recognition
- **Glide** - Image loading & caching
- **iTextPDF** - PDF generation
- **Coroutines** - Async programming
- **ViewPager2** - Page navigation

---

## 📦 APK Output Location

Setelah build berhasil, APK dapat ditemukan di:

**Debug APK:**
```
app/build/outputs/apk/debug/app-debug.apk
```

**Release APK:**
```
app/build/outputs/apk/release/app-release.apk
```

---

## ⚙️ Build Information

- **minSdk:** 24 (Android 7.0)
- **targetSdk:** 36 (Android 15)
- **compileSdk:** 36
- **Kotlin:** 2.0.21
- **Gradle:** 8.13.2

---

## 🐛 Troubleshooting

### Problem: Gradle sync fails
**Solution:** 
```bash
gradlew.bat --stop
gradlew.bat clean build --no-daemon
```

### Problem: APK tidak bisa diinstall
**Solution:** Hapus instalasi lama terlebih dahulu
```bash
adb uninstall arsetya.deyafa.yfscanner
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Problem: Permission denied pada Android 6+
**Solution:** Aplikasi sudah request permissions di AndroidManifest.xml:
- CAMERA
- READ_MEDIA_IMAGES
- READ_EXTERNAL_STORAGE
- WRITE_EXTERNAL_STORAGE

Berikan permission saat aplikasi diminta.

---

## 📝 Catatan Penting

1. **Google Play Services Required** - ML Kit Document Scanner membutuhkan Google Play Services di device
2. **Storage Permission** - Pastikan device/emulator memberikan permission storage
3. **Camera Permission** - Diperlukan untuk fitur scan dokumen
4. **Device Memory** - Minimum 2GB RAM untuk performa optimal

---

## ✨ Fitur Bonus

- Splash screen dengan animasi yang menarik
- Dark mode support
- Search dokumen real-time
- Grid layout untuk list dokumen
- Material Design 3 components
- Edge-to-edge display support

---

## 📞 Support

Jika ada pertanyaan atau issue, silakan check:
1. Android Studio Logcat untuk error messages
2. File lint report: `app/build/reports/lint-results-debug.html`

---

**Last Updated:** March 2026
**Status:** ✅ Production Ready

