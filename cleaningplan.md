# Cleaning Plan — Persiapan Pembersihan Repo & Keamanan

Berdasarkan 2 audit defensif (source + git history). Threat model saat ini: **internal field tool, pengguna terbatas, device/petugas dipercaya**. Update plan ini kalau threat model berubah jadi publik.

## Temuan verifikasi (sudah dicek langsung di kode)

| # | Temuan | Severity | Lokasi |
|---|--------|:--------:|--------|
| 1 | APK pakai `/dev` bukan `/exec` | 🔴 FIX NOW | `AssessmentSync.kt:20` |
| 2 | Release fallback ke debug key | 🔴 FIX NOW | `build.gradle.kts:54-61` |
| 3 | Server percaya `summary` dari Android | 🔴 FIX | `AssessmentSync.kt:156-159` + `Code.gs` |
| 4 | API_TOKEN masuk APK (bukan secret) | 🟠 accepted | `build.gradle.kts:38` + `AssessmentSync.kt:141` |
| 5 | `?action=data` pakai token sama utk read-all | 🟠 accepted-internal | `Code.gs` doGet |
| 6 | Token via query string pada GET | 🟠 | `Code.gs` doGet data |
| 7 | Tidak ada validasi 17 jawaban di server | 🟠 | `Code.gs` appendRow |
| 8 | Formula injection ke Spreadsheet | 🟠 | `Code.gs` appendRow |
| 9 | AI menerima data belum divalidasi | 🟠 | `Code.gs` prompt |
| 10 | DB lokal tidak dienkripsi | 🟡 hardening | Room db |
| 11 | `journey.md` klaim "API_TOKEN tidak ada di APK" | 🔴 dokumen salah | `journey.md` |
| 12 | Password `rumahsehat2026` pernah di history | 🔴 rotate | history (sudah di-redact) |
| 13 | History lama masih bawa impl admin (commit awal) | 🟡 audit | commit `< 865fad4` |
| 14 | AGENTS/journey/planning docs di repo publik | 🟡 opsional | root |

## ✅ Yang SUDAH benar (jangan diubah)

- `local.properties`, keystore, `*.apk`, `build/`, `.gradle/`, `.idea/` di `.gitignore` ✓
- OpenRouter key di AppScript Script Properties (tidak hardcoded) ✓
- Foto Drive `PRIVATE` (bukan ANYONE) ✓
- Spreadsheet ID tidak hardcoded (pakai `getActiveSpreadsheet()`) ✓
- `allowBackup=false` ✓
- History sudah rewrite: 37→32 commit, password keystore di-redact ✓
- Admin flavor/APK sudah dihapus ✓
- APK tidak di repo (pakai GitHub Releases) ✓

## P0 — SEBELUM APK production berikutnya

### 1. `/dev` → `/exec`
```kotlin
// AssessmentSync.kt:20
const val SERVER_URL =
    "https://script.google.com/macros/s/.../exec"   // ganti /dev → /exec
```
- Perlu URL `/exec` valid dari Apps Script (Deploy → Manage deployments → New deployment → **Web app**, Execute as "Me", Access "Anyone").
- Setelah ganti: build, test push nyata ke sheet, upload ulang release.

### 2. Release HARUS gagal tanpa keystore asli
```kotlin
// build.gradle.kts — ganti fallback debug
release {
    isMinifyEnabled = true
    isShrinkResources = true
    signingConfig = signingConfigs.getByName("release")
}
// + fail cepat kalau password kosong (biarkan signingConfig error alami),
//   atau tambah check(keystorePassword != null) { ... }
```
- Hapus **seluruh** blok `else { signingConfig = debug }`.
- Implikasi: build release butuh `RS_KEYSTORE_PASSWORD` / `local.properties`. (CI/F-Droid yang tak punya keystore harus build **userDebug**, bukan release.)

### 3. Rotate kredensial lama
- Password keystore `rumahsehat2026` pernah di commit → **anggap compromised**.
- Langkah: buat keystore baru (atau set password baru), update `local.properties`/env, upload keystore baru ke tempat aman, **jangan pernah** commit ulang. Redact history sudah dilakukan; rotate tetap wajib karena password pernah *terlihat*.

### 4. Perbaiki dokumentasi journey.md
- Klaim "BuildConfig.API_TOKEN tidak ada di APK / nol string sensitif" — **SALAH**. Token itu di-inject ke BuildConfig → masuk APK → bisa diekstrak siapa pun yang punya APK.
- Ganti dengan versi jujur:
```
API token is an application-level shared credential compiled into the
client. It is NOT confidential. It gates the trusted internal deployment
only. If the app becomes public, replace with per-device/user auth.
```

## P1 — SEBELUM data assessment dianggap authoritative

### 5. Server-side validasi 17 jawaban
- Di `Code.gs` sebelum `appendRow()`: whitelist tiap key → cuma terima nilai {a,b,c,..} yang valid (bukan string bebas). Tolak/reject kalau di luar schema.

### 6. Server menghitung skor & status (server-authoritative)
- `Code.gs` hitung ulang `total_achieved` / `is_healthy` / `status` dari `answers` menggunakan **SCORING_RULES** yang sama persis dengan `AssessmentCalculator.kt` (satu sumber kebenaran).
- Android TETAP hitung untuk UI/offline, tapi server acuan final.
- Test dulu: all healthy, all unhealthy, boundary score, jawaban kosong, invalid answer, indikator tak dikenal.
- Perlu migrasi schema/store kalau ada perubahan kolom.

### 7. Jangan percaya `summary` dari Android
- Saat server sudah hitung sendiri, kolom `summary` client bisa diabaikan/di-overwrite oleh hasil hitung server.

### 8. Sanitasi input Spreadsheet (formula injection)
```javascript
function safeCell(value) {
  var s = String(value ?? "");
  if (/^[=+\-@]/.test(s)) return "'" + s;
  return s;
}
```
- Terapkan ke semua field dari client: assessor_name, company, notes, answers, assessment_id, dst.

### 9. Normalisasi sebelum prompt AI
- Setelah validasi + hitung server, baru kirim data normalized ke AI (bukan raw input client).

## P2 — Hardening (opsional, kalau mau naik)

- **Pisah token**: `APP_WRITE_TOKEN` (app) vs `ADMIN_READ_TOKEN` (`?action=data`). Hindari satu token = write + read-all.
- **Jangan token di query string**: baca data via POST body / header, bukan `?action=data&token=`.
- **Encrypt local DB / foto** (mis. SQLCipher / encrypted file storage) — pertimbangkan kalau device hilang risikonya nyata.
- **Audit diff admin lama**: commit `< 865fad4` (sebelum admin dihapus) — cek isi `c786e06`, `865fad4`, `8f79d25` kalau ingin 100% yakin tidak ada URL/api/creds admin yang tertinggal. (Content sudah ter-redact + admin code sudah dihapus, tapi diff bisa dicek.)
- **Pertimbangkan hapus dokumen internal** dari repo publik: `AGENTS.md`, `journey.md` (kalau repo mau dibuka sebagai portfolio, bukan internal).

## Langkah TIDAK BOLEH dilakukan

- ❌ Commit `local.properties`, keystore, APK, spreadsheet export, foto asli, service-account JSON, OAuth token, OpenRouter key, API token production.
- ❌ Menyebut API_TOKEN sebagai "secret". Ia adalah *shared gate* untuk deployment trusted.
- ❌ Release build dengan debug key untuk distribusi production.
- ❌ Meletakkan planning docs ini ke repo publik (kalau isinya internal).

## Status terakhir

| Prioritas | Aksi | Status |
|-----------|------|--------|
| P0-1 | ganti `/dev`→`/exec` | ⬜ butuh URL `/exec` valid |
| P0-2 | hapus fallback debug key | ✅ `95c4be6` — release kini gagal tanpa keystore |
| P0-3 | rotate password keystore | ⬜ pengguna |
| P0-4 | perbaiki journey.md | ✅ `95c4be6` — klaim token dikoreksi |
| P1-5 | server validasi 17 jawaban | ⬜ | 
| P1-6 | server hitung skor otoritatif | ⬜ (butuh sinkron SCORING_RULES dgn Kotlin) |
| P1-7 | jangan percaya `summary` client | ⬜ ikut P1-6 |
| P1-8 | sanitasi formula injection | ✅ `4e0344f` — `safeCell()` di Code.gs |
| P1-9 | normalisasi input sebelum AI | ⬜ ikut P1-5 |
| P2 | hardening opsional | ⬜ |

### Verifikasi yang sudah jalan

- ✅ Debug build + unit test sukses (`assembleUserDebug`, `testUserDebugUnitTest`)
- ✅ Release build **gagal** tanpa keystore (perilaku baru benar), dengan keystore jalan normal
- ✅ `docs/index.html` → link Release v1.4 (tidak ada binary di repo)
- ✅ History bersih: password ter-redact, APK/skills/fdroid terhapus, 32 commit
- ✅ P0-2, P0-4, P1-8 ter-commit & ter-push