# GlassType Android

Projek Android Studio untuk prototaip keyboard IME GlassType.

## Build APK menggunakan GitHub Actions
1. Extract zip ini, kemudian upload ISI-nya terus ke ROOT repository GitHub (bukan folder luar), termasuk folder tersembunyi `.github`.
2. Pastikan fail workflow berada di `.github/workflows/android.yml`.
3. Buka tab **Actions** dan pilih **Build GlassType APK**.
4. Tekan **Run workflow**, atau push perubahan ke branch `main` / `master`.
5. Jika build berjaya, buka run tersebut dan muat turun artifact **GlassType-debug-APK**.
6. Extract artifact ZIP untuk mendapatkan `app-debug.apk`.

Workflow memasang Android SDK platform/build-tools terus dengan `sdkmanager`. Ia sengaja tidak menggunakan `android-actions/setup-android`, kerana log build terdahulu menunjukkan action itu cuba memasang pakej SDK lama bernama `tools` yang tidak tersedia.

## Build APK menggunakan Android Studio
1. Buka folder ini menggunakan Android Studio.
2. Tunggu Gradle Sync selesai.
3. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## Aktifkan pada telefon
1. Pasang APK.
2. Buka aplikasi GlassType.
3. Tekan **Aktifkan GlassType** dan hidupkan keyboard dalam tetapan Android.
4. Kembali ke aplikasi dan tekan **Pilih GlassType**.
5. Pilih GlassType dalam pemilih keyboard.

## Keperluan
- Android SDK Platform 35
- JDK 17
- Gradle 8.9 (disediakan oleh workflow)

## Nota Live Blur
Kesan blur rentas tetingkap memerlukan Android 12 (API 31) atau lebih baharu, dan bergantung pada sokongan pengeluar/peranti serta tetapan sistem. Peranti yang tidak menyokongnya akan memaparkan latar kaca lut sinar sebagai fallback.

## Privasi
Aplikasi ini tidak meminta kebenaran Internet dan tidak mengandungi kod untuk menghantar atau menyimpan teks yang ditaip. Ini prototaip asas; semak kod sebelum memasang atau menggunakan dengan maklumat sensitif.
