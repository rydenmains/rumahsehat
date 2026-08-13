# GitHub Repository: `rydenmains/rumahsehat` — Peta Folder & File

Dokumen ini menjelaskan fungsi **setiap folder/file yang ada di GitHub** (repo publik). Yang TIDAK ada di GitHub (dev tooling, dokumen planning, kredensial) dicatat di bagian bawah.

---

## Root

| Path | Fungsi |
|---|---|
| `.gitignore` | Daftar file/folder yang sengaja tidak di-commit (kredensial, planning docs, tooling lokal, APK) |
| `AGENTS.md` | Instruksi untuk AI coding agent: stack, perintah build, struktur proyek |
| `LICENSE` | Lisensi proyek |
| `README.md` | Intro proyek: cara build, distribusi, riwayat versi |
| `build.gradle.kts` | Build config root (deklarasi plugin Android/Kotlin/KSP) |
| `settings.gradle.kts` | Daftar module (`:app`) + nama repo |
| `gradle.properties` | Tuning Gradle (daemon JVM, dll.) |
| `gradlew` / `gradlew.bat` | Script Gradle Wrapper (Linux/macOS dan Windows) |
| `gradle/wrapper/` | `gradle-wrapper.jar` + `.properties` — pin versi Gradle |
| `gradle/libs.versions.toml` | Version catalog: versi semua dependency (AGP, Kotlin, Room, dsb.) |
| `gradle/gradle-daemon-jvm.properties` | Pengaturan JVM daemon Gradle |
| `backend/` | Lihat bagian Backend |

---

## `app/` — Source Android

### Build config
| Path | Fungsi |
|---|---|
| `app/build.gradle.kts` | Build config: flavor `user`, `minSdk 23 / targetSdk 35`, signing release, R8 minify, `BuildConfig.API_TOKEN` |
| `app/proguard-rules.pro` | Aturan R8/ProGuard tambahan untuk release |
| `app/.gitignore` | Gitignore khusus module app |
| `app/schemas/` | Export schema Room (verifikasi migrasi DB antar versi) |
| `app/src/main/keepRules/rules.keep` | Aturan keep R8 khusus (dipakai release) |

### Manifest
| Path | Fungsi |
|---|---|
| `app/src/main/AndroidManifest.xml` | Manifest: izin minimal (hanya `INTERNET`), `allowBackup=false`, deklarasi Activity + FileProvider |

### Source Kotlin — `app/src/main/java/com/rumahsehat/`

| Path | Fungsi |
|---|---|
| `RumahSehatApp.kt` | `Application` — menjadwalkan `SyncWorker` (WorkManager) untuk retry sync background |
| `data/dao/AssessmentDao.kt` | Query Room: simpan/baca assessment & score items, cari yang `PENDING` |
| `data/db/AppDatabase.kt` | Setup Room DB v3 (WAL), migrasi 1→2→3 |
| `data/model/Assessment.kt` | Entity `assessments` (id, petugas, skor, syncStatus, path foto) |
| `data/model/FormItem.kt` | Struktur satu pertanyaan (id, opsi, skor, `reason`) |
| `data/model/FormItemsProvider.kt` | Definisi 17 pertanyaan indikator + skor + opsi |
| `data/model/ScoreItem.kt` | Entity `score_items` (jawaban per pertanyaan) |
| `data/remote/AssessmentSync.kt` | **Kirim data ke backend** Google Apps Script (payload JSON + foto base64, HTTPS) |
| `data/repository/AssessmentRepository.kt` | Akses data + logika `syncPending()` |
| `domain/AssessmentCalculator.kt` | Hitung skor total + status SEHAT/TIDAK SEHAT |
| `sync/SyncWorker.kt` | WorkManager worker: kirim ulang assessment `PENDING` |
| `ui/MainActivity.kt` | Layar utama (bottom nav Beranda/Riwayat) + tombol sync + animasi loading |
| `ui/AssessmentActivity.kt` | Form penilaian (pager 3 tahap, 17 pertanyaan + foto) |
| `ui/AssessmentViewModel.kt` | State form, skor, foto, sinkronisasi, simpan |
| `ui/AssessmentAdapter.kt` | RecyclerView adapter daftar assessment |
| `ui/AssessmentLevel.kt` | Enum level hasil penilaian |
| `ui/IdentityFragment.kt` | Halaman-0: input identitas petugas |
| `ui/QuestionFragment.kt` | Satu layar pertanyaan + tombol foto per section |
| `ui/QuestionPagerAdapter.kt` | Adapter ViewPager2 form |
| `ui/ReviewActivity.kt` | Detail hasil penilaian tersimpan |
| `ui/ReviewAdapter.kt` | Adapter tampilan review |
| `ui/SplashActivity.kt` | Splash screen (fade-in + animasi logo + spinner loading) |
| `util/ImageUtils.kt` | Kompresi foto sebelum disimpan |

### Resources — `app/src/main/res/`

| Folder | Isi |
|---|---|
| `res/layout/` | Layout XML semua screen (splash, main, assessment, review, bottom nav, home, history) |
| `res/drawable/` | Icon vector + background shape (tombol, banner sync, chip status, splash) |
| `res/drawable-nodpi/` | Logo raster (`ic_logo.png`) |
| `res/font/` | Font **Plus Jakarta Sans** (regular/medium/semibold/bold) + family XML |
| `res/mipmap-*/` | Icon launcher (semua density) |
| `res/values/` | `colors.xml`, `strings.xml`, `dimens.xml`, `themes.xml` |
| `res/values-night/` | Tema mode gelap |
| `res/color/` | Warna item bottom nav |
| `res/menu/` | Definisi menu bottom nav |
| `res/xml/` | `file_paths.xml` (FileProvider), `backup_rules.xml`, `data_extraction_rules.xml` |

### Test
| Path | Fungsi |
|---|---|
| `app/src/test/java/...` | Unit test (kalkulator skor, level) — dijalankan otomatis di CI |

---

## `backend/` — Backend Google Apps Script

| Path | Fungsi |
|---|---|
| `backend/Code.gs` | Satu-satunya file backend: `doPost` (simpan assessment + foto ke Drive), `doGet` (baca data + status), AI analisis foto via OpenRouter, rate limit, idempotensi, logging ke tab Logs |

---

## `docs/` — Landing Page (GitHub Pages)

| Path | Fungsi |
|---|---|
| `docs/index.html` | Landing page publik: deskripsi app + link unduh APK dari GitHub Releases |
| `docs/logo.png` | Logo untuk landing page |
| `docs/.nojekyll` | Menonaktifkan Jekyll GitHub Pages |

---

## `.github/` — CI/CD

| Path | Fungsi |
|---|---|
| `.github/workflows/android.yml` | GitHub Actions: build debug + unit test tiap push/PR (otomatis); build release signed (manual, butuh secrets keystore) |

---

## Yang sengaja TIDAK ada di GitHub

| Item | Alasan |
|---|---|
| APK | `.gitignore` `*.apk` — didistribusi via **GitHub Releases**, bukan di repo |
| `local.properties`, `rumahsehat-upload.keystore` | Kredensial signing + `API_TOKEN` — rahasia, hanya lokal/env |
| `docs/` tidak menyimpan APK | APK ada di Releases; index.html menautkan ke sana |
| `nextplan.md`, `planbackend.md`, `plan.md`, `cleaningplan.md`, `journey.md`, `rumahsehat.md` | Dokumen planning/arsitektur — aturan proyek: tidak di-commit |
| `.opencode/`, `.agents/`, `skills-lock.json`, `git_history.txt` | Tooling & skill opencode lokal — bukan bagian dari source app |
| `RumahSehatProtoCompose/` (prototype) | Prototype UI mock — di `.gitignore` (`/prototype/`) |

---

## Alur distribusi saat ini

```
GitHub repo (source) ──build──▶ GitHub Actions CI ──▶ debug APK (artifact)
GitHub Releases (APK signed) ──dilink──▶ docs/index.html (GitHub Pages)
```