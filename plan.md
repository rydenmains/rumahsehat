# PLAN RumahSehat — single source of truth

> Active focus: **Phase 2**. Phase 1 ✅ selesai (2026-08-08). Phase 2 sedang dieksekusi — butuh deployment manual (lihat unter).

## Phase 1: ✅ SELESAI — Refactor UI & Form Model (APK)
Goal: slider → radio a/b/c/d Kemenkes; skor tersembunyi dari pilihan.

| # | Langkah | Status |
|---|---|---|
| 1.1 | Tambah `data class Option(letter, label, scoreWeight)` + `FormItem.options: List<Option>`; `currentScore` tetap ada, di-drive dari option (minim churn) | ✅ FormItem.kt |
| 1.2 | Isi 17 item dgn options dari SPEC (3 opsi/indikator, teks Kemenkes baku); bobot option per item | ✅ FormItemsProvider.kt |
| 1.3 | Ganti `Slider` + `tvLevel` → `RadioGroup` + `RadioButton`; `isApplicable` false → disable group | ✅ fragment_question.xml + QuestionFragment.kt |
| 1.4 | `AssessmentViewModel`: `isFormComplete`/`missingItems` cek `selectedOptionIndex>=0`; `ScoreItem.score = option.scorePoints` | ✅ AssessmentViewModel.kt |
| 1.5 | Review & sync: label status reuse terjemahan option-law / hidden (score tersembunyi) | ✅ ReviewAdapter.kt |

Mitigasi: bobot lama (20/150/100) dipertahankan → `weight>=100` esensial di `AssessmentCalculator` tak berubah. Backend `scores` tetap 17 int → payload kompatibel, `Code.gs` tak perlu diubah untuk Phase 1. Verify: `compileUserDebugKotlin` + `testUserDebugUnitTest` PASS.

## Phase 2: IN PROGRESS — Gemini AI di Backend (`Code.gs`)
Goal: data langsung disimpan di Sheet, AI diproses BELAKANGAN (deferred) karena limit Apps Script (6 menit/payload besar).

| # | Langkah | Status |
|---|---|---|
| 2.1 | Key di Script Properties (`GEMINI_API_KEY`); `getGeminiKey()` fallback; jangan hardcode | ✅ |
| 2.2 | `analyzeAssessmentWithGemini({house_front,sanitation,kitchen_spal}, scoresSummary)` → multimodal base64 + prompt, `response_mime_type=json`; output `{is_valid, flag, recommendation}` | ✅ |
| 2.3 | Analisis pakai **semua 3 photo keys**, bukan hanya `sanitation` | ✅ |
| 2.4 | **Deferred** → `doPost` simpan data dulu (kolom AI kosong), `processPendingAi()` proses batch 5 baris & tulis hasil; fallback bila gagal | ✅ |
| 2.5 | `setupEnvironment()` header → **29 kolom** (tambah `Status Validasi AI`, `Rekomendasi AI`); migrasi lama via `ensureAiColumns()` | ✅ |
| 2.6 | Fix bug `photo_url` undefined → `photoUrls[0]` | ✅ |
| 2.7 | `compressPhoto()` Android: resize ≤1024px, JPEG 80% sebelum kirim | ✅ |
| 2.8 | Payload Android kirim `answers` (kata opsi "c. …"), bukan angka `scores`; `Code.gs` baca `answers || scores` | ✅ |

**Keputusan:** model `gemini-2.5-flash` (murah/cepat, cukup untuk multimodal 3 foto).

**Deployment (manual, wajib):**
1. Tempel `backend/Code.gs` ke Apps Script sheet → **Deploy → Web App** → update `SERVER_URL` di `AssessmentSync.kt:18` bila URL berubah.
2. Script Properties → tambah `GEMINI_API_KEY`.
3. Jalankan sekali `createAiTrigger()` di Apps Script (trigger tiap 10 menit → `processPendingAi()`).
4. Rebuild & reinstall APK (`./gradlew assembleUserRelease`).

## Phase 3: DISTRIBUSI & RELEASE (v1.2.0)

### Instant Fixes (Done/Status)
- ✅ **Least-privilege permissions**: manifest hanya `INTERNET` + fitur kamera (tak ada `READ/WRITE_EXTERNAL_STORAGE`/`CAMERA` — kamera via `TakePicture` intent). Komentar kebijakan ditambahkan di `AndroidManifest.xml`.
- ✅ **APK sudah ditandatangani** key produksi (CN=Rumah Sehat, SHA-256 b41424150…) + `checksums.txt` (SHA256 `77a6ddd9…`) di repo & tag `v1.2.0`.
- ⏳ **GitHub Release**: butuh PAT (`repo` scope) → `.\scripts\release.ps1 -Token <PAT>` atau buat manual di web (tag v1.2.0 sudah ada).
- ⏳ **Firebase App Distribution**: butuh aksi manual user (buat proyek Firebase + `firebase login`) →
  ```bash
  npm i -g firebase-tools && firebase login
  firebase appdistribution:distribute RumahSehat-User.apk --app <APP_ID> --groups testers
  ```

### F-Droid — Dua Jalur (file siap: `fdroiddata/com.rumahsehat.yml`)
- **Jalur A (paling gampang)**: akun gitlab.com → `gitlab.com/fdroid/fdroiddata` → *Issues* → *New issue* minta penambahan app (tulis repo, lisensi MIT, versionCode 3, versionName 1.2.0-user, task `userRelease`). Maintainer yang mengisi.
- **Jalur B (standar)**: fork `gitlab.com/fdroid/fdroiddata` → buat `metadata/com.rumahsehat.yml` (isi = `fdroiddata/com.rumahsehat.yml` di repo ini) → commit → push → Merge Request. Review ±1–2 minggu.
- Catatan: build F-Droid tanpa `local.properties` → `API_TOKEN` kosong → sync ditolak backend. Perlu desain token publik/per-device bila mau F-Droid fungsional penuh.

### Deployment v1.2.0 (manual, wajib setelah rilis)
1. Tempel `backend/Code.gs` baru → **Deploy → Web App**.
2. Script Properties → set **`API_TOKEN` = `4fde71466abbdefe609628c28d4f3196`** (sama dgn `local.properties`; tanpa ini semua sync ditolak).
3. Simpan `OPENROUTER_API_KEY` (jangan dihapus).
4. APK lama mengirim token lama → ditolak setelah redeploy. Sebar APK v1.2.0.

## ENGINEERING-PRINCIPLES.md (harus diingat)
1. Blueprint dulu: baca `SPEC.md`/`README.md` sebelum generate kode.
2. AI = tool (amplifier), bukan autopilot — task diputuskan manusia.
3. Modular layer: INFRA(`build.js`/package) → DATA(`data.js`) → API(layer terpisah) → komponen(`components.js`). Jangan taruh HTML di `data.js`, jangan hardcode konten di `components.js`.
4. Wajib `npm run build` + verifikasi hasil cocok `SPEC.md` sebelum "selesai". Tambah unit test untuk logic baru.
5. Satu sumber kebenaran per jenis perubahan (style → components/tailwind, konten → data, spec → SPEC).
6. Hindari: prompt→paste→hope, AI GENERATED tanpa rencana, duct-tape, weak foundation, "looks good ship it?", tidak cross-check ke SPEC/build.

### Weakness/Vulnerability doc tsb (checkpoint)
- `npm run build` gagal by default (node_modules belum install).
- `build.js:14` hardcode `{tahap1,tahap2,tahap3}` → kontradiksi dgn prinsip "scalable".
- Tidak ada tool pembanding `SPEC ↔ data.js` → drift copy tidak terdeteksi.
- `formAction` post di `data.js` = API layer belum terpisah.
- "security" disebut tapi tidak dijabarkan: token `rs_sehat_2026` masih hardcode di `Code.gs` & `AssessmentSync.kt`; Gemini key belum ada aturan penyimpanan.
  - ✅ (v1.2.0) Token API dipindah ke `local.properties`/env → `BuildConfig.API_TOKEN`; fallback `rs_sehat_2026` DIHAPUS dari `Code.gs`; endpoint `?action=data` kini wajib token; foto Drive privat (tanpa `ANYONE_WITH_LINK`).
- Doc hanya mengikat project web, tidak backend Android/Code.gs.
- 3 file referensi terpisah (DESAIN.md / SPEC.md / ENGINEERING-PRINCIPLES.md) → risk dua sumber kebenaran.