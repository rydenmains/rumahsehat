# Rumah Sehat — JOURNEY

Catatan perjalanan pengembangan & keputusan penting.

---

## 2026-08-13 · Kenapa APK v1.4 tidak kena flag Google Play

**Status:** v1.4.0-beta (obfuscated, R8) ter-upload ke GitHub Releases, tidak kena flag Play.

### Kenapa dulu kena, sekarang tidak

Alasan paling kuat adalah **penghapusan kredensial hardcoded + obfuscation menyeluruh** — bukan karena satu hal aja.

| Faktor | V1.0 (kena flag) | V1.4 (bersih) |
|---|---|---|
| Token backend | `put("token", "rs_sehat_2026")` hardcoded di `AssessmentSync.kt` semua commit awal | `BuildConfig.API_TOKEN` — di-inject saat build dan **masuk ke dalam APK** (EXTRAKTIF oleh siapa pun yang punya APK); bukan secret |
| Password keystore | `keyPassword = "rumahsehat2026"` hardcoded di `build.gradle.kts` (commit `b7f2a12`) | dibaca dari `RS_KEYSTORE_PASSWORD` / `local.properties` — tidak pernah masuk APK/repo |
| Obfuscation | belum aktif | R8: `isMinifyEnabled = true` + `isShrinkResources = true` — nama kelas/dioptimasi, APK 6.1MB → 2.2MB |
| Peran app | ada flavor admin + dashboard (`865fad4` menghapusnya) | single app user saja |
| Signing | (histori naik-turun) | release signed keystore upload; **build gagal** kalau keystore tidak tersedia |

### Analisis kenapa flag-nya turun

1. **Kredensial hardcoded hilang / di-redact** — passphrase `rs_sehat_2026` dan `rumahsehat2026` sempat ada sebagai string literal; sekarang tidak ada di source, dan history git sudah di-redact. Perlu **rotate** password keystore karena pernah terlihat.
2. **Obfuscation aktif (R8)** — nama kelas/metode di-minify, dead code dihapus. Skoring risiko otomatis Play untuk "suspicious"/"unused API" jauh lebih rendah pada bytecode yang ter-obfuscate rapi.
3. **Fitur admin dihapus** — app tunggal (user) mengurangi permukaan "app berbasis admin/dual-role" yang kadang ditandai.
4. **Bersih dari riwayat git** — keystore & password juga sudah di-redact dari seluruh history repo (`filter-repo --replace-text` pada `rumahsehat2026`), jadi tidak ada jejak credential di blame/commit publik.

### Aturan ke depan (jangan dilanggar)

- **Jangan pernah** hardcode token/password/key di `app/src` atau `build.gradle.kts`. Selalu lewat `local.properties` / env (`RS_API_TOKEN`, `RS_KEYSTORE_PASSWORD`).
- **R8/minify untuk release wajib ON** — ini garis pertahanan utama terhadap flag & membedah APK.
- `backend/Code.gs` juga sudah pakai Script Properties (tidak ada key literal).
- Kalau suatu versi di-publish ke Play, **pastikan keystore upload asli** dipakai — release build sekarang **wajib gagal** jika keystore tidak tersedia (tidak ada fallback debug key lagi).
- **API token aplikasi (BuildConfig.API_TOKEN) BUKAN secret.** Ia di-compile ke dalam APK sehingga bisa diekstrak siapa pun yang memegang APK. Fungsinya hanya *application-level access gate* untuk deployment internal yang terpercaya. Jika aplikasi menjadi publik, wajib diganti autentikasi per-device/per-user.