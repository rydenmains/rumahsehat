# Rumah Sehat — Journey & Progress

> Catatan perjalanan proyek dari awal sampai sekarang. Dibuat 2026-08-08.
> Referensi teknis detail ada di `APP_OVERVIEW.md`; patokan desain di `desain.md`.

---

## 1. Status Saat Ini (Snap)

| Aspek | Kondisi |
|---|---|
| Aplikasi Android | Kotlin + XML View (bukan Compose), 1 APK publik `RumahSehat-User.apk` |
| Fitur inti | 17 indikator, 3 foto, offline-first, sinkron Google Sheets/Drive |
| Backend | Google Apps Script (`backend/Code.gs`) → Sheet "Data Assessment" + Drive |
| Landing web | `index.html` (unduh APK) + `backend/user.html` (penilaian saya) |
| Rilis | v1.1.0 (2026-08-08), APK root + GitHub Release, R8 obfuscated (3.2 MB) |
| Desain | Design system "Eco-Systemic Professional" (teal #005048, Inter) — sudah di-harmonize via Stitch |
| Hal yang belum rapi | 3 catatan hardening di §5 (token, state form, foto) |

---

## 2. Timeline (dari git log)

### 2026-08-07 — Fondasi & Pisah Aplikasi
- `460871b` Initial commit (sumber dari repo rydenmains/rumahsehat).
- `da41361` Deploy user & admin dashboard ke GitHub Pages.
- `38c34b8` Sisi APK user dibangun.
- `2ecec89` Split menjadi dua app: **User & Admin**.
- `7f5a14f` Add User APK.

### 2026-08-08 — Pengerasan, Sinkronisasi, Rilis
- `00018eb` Fix admin cloud autoload; font Nunito statis; nama app & label status user.
- `9f2d8b0` Darken emerald (kontras AA); label 3 section foto.
- `c53db62` Bolder font + semibold; dialog form belum lengkap custom.
- `c184156` Upload **3 foto ke Drive**; simpan path foto untuk retry offline.
- `2eca9b9` Rename key foto ke section bernama (`house_front` / `sanitation` / `kitchen_spal`).
- `008a736` Header kolom foto lebih deskriptif di Sheet.
- `05abfaa` Audit: hapus dead code, export schema Room, constraint network WorkManager, drop CAMERA perm, unit tests.
- `7eab05e` Bersihkan repo: untrack tooling dev, hapus HTML duplikat, simpan source + APK.
- `ab04e88` Tombol foto hanya muncul di soal pertama tiap section.
- `6840d2d` Aktifkan R8 obfuscation + shrinking untuk release.
- `40a5cec` README ala ergocam + landing page unduh mobile.
- `35343d3` Hapus APK admin & link download; gitignore admin apk.
- `d375eeb` **Hapus flavor admin & dashboard** — aplikasi jadi user-only.
- `798cfb6` README: drop admin ref; tambah section releases; LICENSE.
- `8cec180` Update README.
- `afd4c78` Pakai logo & favicon asli; **sign release APK** (fix install di device).
- `f82422c` Tombol download app di `user.html`.
- `17a911c` Fix crash: unify dark theme ke NoActionBar (konflik `setSupportActionBar`).

### 2026-08-08 — Desain & Konsistensi (Stitch MCP)
- Design system **"Eco-Systemic Professional"** dibuat di Stitch (teal `#005048`/`#006` + full token M3, Inter).
- Screen final referensi: Splash, Beranda Final, History (Guest), Form inspeksi 3 tahap.
- **Harmonize 1**: Beranda, Splash, Tahap 1/2/3, Riwayat → jadi `<nama> (Harmonized)`.
- **Harmonize 2**: 3 tahap inspeksi → `<nama> (Harmonized) v3`. Jangan buat skema/desain baru — hanya bikin konsisten.

### 2026-08-08 — Phase 1 (plan.md): Slider → Radio option Kemenkes
- **1.1-1.5 selesai**: `Option(letter, label, scoreWeight)` + `FormItem.options`; 17 item × 3 opsi baku Kemenkes; UI `RadioGroup` + `RadioButton` (ganti `Slider` + `tvLevel`); `isFormComplete` cek `selectedOptionIndex>=0`; skor per-item tersembunyi (label opsi di review).
- Fix freeze sesi sebelumnya: error compile `ReviewAdapter.kt` (nullable `formItem`) — build sekarang `BUILD SUCCESSFUL` + unit test PASS.

### 2026-08-08 — Phase 2 (plan.md): Gemini AI di Backend
- **2.1-2.7 dikode**: `getGeminiKey()` (Script Properties, no hardcode); model `gemini-2.5-flash`.
- **Desain deferred-AI**: `doPost` simpan data dulu (kolom AI kosong) → `processPendingAi()` (batch 5 baris, dipanggil trigger tiap 10 mnt) ambil foto dari Drive, analisis, tulis 2 kolom. Karena limit doPost 6 menit / payload foto besar.
- `compressPhoto()` Android (≤1024px, JPEG 80%); header 29 kolom + migrasi lama `ensureAiColumns()`; fix `photo_url` undefined.
- **Belum deploy**: tempel Code.gs → set `GEMINI_API_KEY` di Script Properties → jalankan `createAiTrigger()` → update web app URL (lihat plan.md).

### 2026-08-08 — v1.1.0: Obfuscation, Fix Sync Redirect, Bersih Kredensial
- Fix bug laporan lapangan: **"menunggu kirim" padahal data full di Sheet** — akar masalah: Apps Script balas `302` redirect, kode lama **re-POST payload** ke URL redirect → response tidak pernah `2xx` → status `PENDING` selamanya. Fix: `instanceFollowRedirects = true` di `AssessmentSync.push` (ikuti redirect Apps Script dengan GET; payload dikirim sekali). Penyebab sebelumnya juga bikin dobel data untuk kasus lain.
- R8 obfuscation release diverifikasi: `mapping.txt` 14 MB, APK 7 MB → **3.2 MB**. `isShrinkResources=true` ikut hilangkan resource tak terpakai.
- **Kredensial dibersihkan**:
  - Keystore password tidak lagi hardcoded di `app/build.gradle.kts` — dibaca dari env `RS_KEYSTORE_PASSWORD` atau `local.properties` (`keystore.password`); keduanya gitignored.
  - Token tulis backend `rs_sehat_2026` dipindah dari hardcode `Code.gs` ke **Script Properties** `API_TOKEN` (fallback nilai lama agar deploy yang berjalan tetap lanjut).
  - Hapus kode admin mati: `BuildConfig.FLAVOR=="admin"` di `ReviewActivity`, param `showStatus` di `AssessmentAdapter`, `fetchCloudAssessments`+`parseDate` (dashboard admin), CSS `.role.admin` di `index.html`.
- Versi: `versionCode=2`, `versionName=1.1.0`. Unit test PASS.

---

## 3. Arsitektur (ringkas)

```
Splash → MainActivity (Beranda) → AssessmentActivity (17 soal + 3 foto) → [Simpan & Kirim] → back
                                   └── review card → ReviewActivity
```

- **MVVM**: Activity + Fragment + `AndroidViewModel` (LiveData) + Repository + Room.
- **Room v2 (WAL)**: tabel `assessments` + `score_items` (FK cascade), migration 1→2 (`photoPathsJson`).
- **Sync**: WorkManager (unique work `sync-pending`, constraint CONNECTED, backoff 10s) → POST JSON ke Apps Script; offline → tetap `PENDING`.
- **Kamera**: `TakePicture` + FileProvider; path foto persisten untuk retry.
- **Skor**: `AssessmentCalculator` — bobot 20/100-150; `SEHAT` hanya bila semua esensial penuh.

---

## 4. Status per Fase

| Fase | Status |
|---|---|
| App inti (17 soal + foto + simpan) | ✅ Selesai |
| Sinkronisasi Google Sheets + Drive | ✅ Selesai |
| Offline-first + retry auto | ✅ Selesai |
| R8/release + signing | ✅ Selesai |
| Sayang: skip admin/dashboard | ✅ Selesai (user-only) |
| Landing web + unduh APK | ✅ Selesai |
| Design system "Eco-Systemic Professional" | ✅ Selesai |
| Harmonize screen (konsistensi warna/elemen) | ✅ Selesai (6 + 3 v3) |
| Non-crash dark theme | ✅ Selesai |

---

## 5. Yang Masih Open / Hardening (dari APP_OVERVIEW §7)

- ✅ **Token backend** pindah ke Script Properties `API_TOKEN` (fallback nilai lama agar deploy jalan).
- ✅ **Keystore pw** tidak lagi hardcoded — env `RS_KEYSTORE_PASSWORD` / `local.properties`, gitignored.
- ✅ **Endpoint GET publik** `?action=data` — `fetchCloudAssessments` (dashboard admin) dihapus dari app; endpoint di `Code.gs` masih ada (perlu di token / hapus manual bila sheet sensitif).
- **State form tak bertahan restart** — jawaban hilang saat Activity di-recreate atau proses dimatikan.
- **Foto hanya di memory** — `photoPaths` tidak persisted sampai Simpan.

---

## 6. Ringkasan Sesi Terakhir

1. Set design system di Stitch ("Eco-Systemic Professional", teal + Inter).
2. Harmonise 6 screen utama (Beranda, Splash, Riwayat, Tahap 1-3) → versi "(Harmonized)".
3. Revisit 3 tahap inspeksi → konsisten saja, bukan skema baru (v3).
4. Buat `JOURNEY.md` ini untuk melacak progress.
5. **Phase 1 selesai (2026-08-08)**: slider → 3 radio option Kemenkes; fix freeze build (nullable `formItem` di ReviewAdapter); `compileUserDebugKotlin` + `testUserDebugUnitTest` PASS.
6. **Phase 2 (sedang berjalan)**: Gemini AI (`gemini-2.5-flash`) dikode — `doPost` simpan dulu, AI di proses belakangan via `processPendingAi()` (trigger 10 mnt). Payload kini kirim **kata opsi** (`answers`), bukan angka; Android `AssessmentSync.kt` dikoreksi + build/test PASS. Tinggal deploy manual (Script Properties key + update web app URL + jalankan `createAiTrigger()`).
7. **Session ini — v1.1.0 rilis**: fix bug PENDING palsu (redirect Apps Script, `instanceFollowRedirects=true`); verifikasi R8 obfuscation (APK 3.2 MB, mapping.txt 14 MB); bersih kredensial (keystore pw → env, token backend → Script Properties); hapus kode admin mati (ReviewActivity, AssessmentAdapter `showStatus`, `fetchCloudAssessments`, CSS admin). Versi `1.1.0` (`versionCode=2`), unit test PASS, APK baru di-root & link web sinkron.