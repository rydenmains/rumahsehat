# Rumah Sehat

**Aplikasi penilaian kesehatan lingkungan rumah (jamban, air bersih) berbasis Android.**

Alat bantu petugas lapangan untuk mengaudit rumah sehat sesuai indikator komponen rumah, sanitasi, dan perilaku penghuni. Semua data tersimpan otomatis ke **Google Sheets** tanpa perlu koneksi stabil.

[![Android](https://img.shields.io/badge/platform-Android%206.0%2B-0F6A4B?style=flat-square&logo=android&logoColor=white)](https://www.android.com) [![Kotlin](https://img.shields.io/badge/kotlin-2.x-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org) [![Room](https://img.shields.io/badge/storage-Room-brightgreen?style=flat-square)](https://developer.android.com/training/data-storage/room) [![License](https://img.shields.io/badge/license-MIT-1A1A1A?style=flat-square)](LICENSE)

---

## Ringkasan

Petugas menilai rumah via aplikasi, dan data langsung terkirim ke spreadsheet.

| Versi | Label | Untuk |
|---|---|---|
| **Rumah Sehat** | `RumahSehat-User.apk` | Petugas lapangan: input penilaian + foto |

---

## Rilis

Unduh APK terbaru di **Release** GitHub, atau langsung dari `RumahSehat-User.apk` di root repo.

| Versi | Tanggal | Catatan |
|---|---|---|
| **v1.1.0** | 2026-08-08 | R8 obfuscation aktif; fix sync redirect Apps Script (tiada PENDING palsu); pembersihan kredensial & kode admin |
| **v1.0.0** | 2026-08-08 | Rilis pertama: 17 indikator, 3 foto/penilaian, sinkron Google Sheets |

---

## Fitur

- **17 indikator** penilaian rumah sehat (komponen rumah, sanitasi, perilaku)
- **3 foto** per penilaian, terkirim ke Google Drive + tampil di Sheet
- **Offline-first**: penilaian disimpan lokal, disinkron otomatis saat online
- **Rekap otomatis** di dashboard web dari Google Sheet

---

## Cara Menggunakan (User)

1. Download `RumahSehat-User.apk` dari **Releases** atau folder root.
2. Pasang di HP — izinkan instalasi *unknown sources*.
3. Isi nama petugas & perusahaan.
4. Jawab 17 indikator, ambil **3 foto** (rumah, sanitasi, perilaku).
5. Tekan **Simpan** — data masuk Sheet otomatis.

---

## Skor & Status

Penilaian memakai bobot per bagian (komponen rumah 20 poin, sarana sanitasi 100-150 poin, perilaku 20 poin). Aplikasi mengkalkulasi persentase dan menentukan:

- **SEHAT** jika komposisi sanitasi esensial terpenuhi
- **TIDAK SEHAT** jika indikator esensial tidak terpenuhi

---

## Screenshots

*Menunggu tangkapan layar — akan ditaruh di folder ini.* Masukkan `art/screen-form.png` lalu update markup di bawah.

![Form](art/screen-form.png)

---

## Sync, Backend & Struktur

Aplikasi menulis ke Google Apps Script (`backend/Code.gs`) yang memproses payload → Drive + Google Sheet.

```
RumahSehat/
├── app/                        # Source Android (Kotlin + View XML)
│   ├── src/main/java/com/rumahsehat/
│   │   ├── data/             # Room db, DAO, model, remote, repository
│   │   ├── domain/           # AssessmentCalculator (logika penilaian)
│   │   ├── sync/             # SyncWorker (retry offline via WorkManager)
│   │   └── ui/               # Activity, Fragment, adapter, ViewModel
│   └── proguard-rules.pro    # R8 keep rules (obfuscation release)
├── backend/
│   ├── Code.gs          # Apps Script: terima data → Drive + Google Sheets
│   └── user.html        # Halaman "penilaian saya" per petugas
├── RumahSehat-User.apk  # Build user (release/debug)
```

---

## Build dari Sumber

```
# Debug
./gradlew assembleUserDebug
# Release (R8 obfuscation aktif)
./gradlew assembleUserRelease
```

Hasil release ada di `app/build/outputs/apk/<flavor>/release/`.

---

## Lisensi

MIT - bebas pakai, ubah, dan sebarkan.

---

*RumahSehat - Rayz*
