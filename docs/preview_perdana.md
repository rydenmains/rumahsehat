# Preview Perdana — v1.7.1 (12-09-2026)

## Hasil uji perangkat lama
- Oppo CPH1901 (ColorOS 5.2, Android 8.1, SDM450, RAM 3GB) — crash launch, dialog "gagal memulai".
- Samsung J4+ SM-J415F (Android 8.0, RAM 2GB) — "aplikasi terhenti".
- Infinix Smart 5 X657 (Android 11) — ikon launcher transparan, background ijo.

## Root cause
1. `MainActivity.kt:62` — `window.isNavigationBarContrastEnforced` = API 29+, crash `NoSuchMethodError` di API 26/27.
2. Manifest pakai `@drawable/ic_logo` (vektor transparan), adaptive icon (`mipmap`, bg putih `#F8F6F1`) diabaikan.
3. R8 `minify+shrink` on + keep minim — risiko strip di ART lama + flag Play Protect.

## Fix (commit e50586a)
- Guard `if (SDK_INT >= 29)` untuk contrast enforced.
- Icon/roundIcon → `@mipmap/ic_launcher`.
- `isMinifyEnabled=false`, `isShrinkResources=false`. Sentry tetap (dep 8.53.0 + manual init).

## Uji Sentry (emulator, build user-debug + DSN local.properties)
- `libsentry.so` load OK, manual init jalan (auto-init false sesuai desain).
- Ping `RS test ping` dikirim 21:55 — cek di dashboard Issues.
- Kode ping sudah revert; codebase bersih.

## Rilis v1.7.1 (commit 9293467, tag v1.7.1)
- `versionCode 11`, `versionName 1.7.1`. APK `app-user-release.apk` 17.3MB, signed.
- Release GitHub: https://github.com/rydenmains/rumahsehat/releases/tag/v1.7.1
- Situs: https://rydenmains.github.io/rumahsehat/ (link + label v1.7.1, ukuran ±17MB).

## Belum masuk rilis
- Fitur galeri (Photo Picker + fallback GetContent) masih uncommitted.
- File dump `*.xml/log` + `adb_dev.txt` dkk. belum dibersihkan.
