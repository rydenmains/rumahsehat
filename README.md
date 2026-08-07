# Rumah Sehat

**Aplikasi penilaian kesehatan lingkungan rumah (jamban, air bersih, SPAL) berbasis Android.**

Alat bantu petugas lapangan untuk mengaudit rumah sehat sesuai indikator komponen rumah, sanitasi, dan perilaku penghuni. Semua data tersimpan otomatis ke **Google Sheets** tanpa perlu koneksi stabil.

[![Android](https://img.shields.io/badge/platform-Android%206.0%2B-0F6A4B?style=flat-square&logo=android&logoColor=white)](https://www.android.com) [![Kotlin](https://img.shields.io/badge/kotlin-2.x-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org) [![Room](https://img.shields.io/badge/storage-Room-brightgreen?style=flat-square)](https://developer.android.com/training/data-storage/room) [![License](https://img.shields.io/badge/license-MIT-1A1A1A?style=flat-square)](LICENSE)

---

## Ringkasan

Petugas menilai rumah via aplikasi, dan hasilnya langsung terkirim ke spreadsheet. Ada **dua versi APK** untuk dua peran berbeda.

| Versi | Label | Untuk |
|---|---|---|
| **Rumah Sehat** (User) | `RumahSehat-User.apk` | Petugas lapangan: mengisi penilaian + foto |
| **Rumah Sehat Admin** | `RumahSehat-Admin.apk` | Pengelola: melihat rekap data perangkat & cloud |

---

## Fitur

- **17 indikator** penilaian rumah sehat (komponen rumah, sanitasi, perilaku)
- **3 foto** per penilaian, terkirim ke Google Drive + tampil di Sheet
- **Offline-first**: penilaian disimpan lokal, disinkron otomatis saat online
- **Dua peran** (user / admin) dengan launcher & identitas aplikasi terpisah
- **Rekap otomatis** di dashboard admin dari Google Sheet

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

*Menunggu tangkapan layar — akan ditaruh di folder ini.* Masukkan `art/screen-form.png` dan `art/screen-admin.png` lalu update markup di bawah.

![Form](art/screen-form.png)
![Admin](art/screen-admin.png)

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
│   ├── dashboard.html   # Dashboard web (laptop) baca dari sheet
│   └── user.html        # Halaman "penilaian saya" per petugas
├── RumahSehat-User.apk  # Build user (release/debug)
└── RumahSehat-Admin.apk # Build admin
```

---

## Build dari Sumber

```
# Debug (dua flavor)
./gradlew assembleUserDebug assembleAdminDebug
# Release (R8 obfuscation aktif)
./gradlew assembleUserRelease assembleAdminRelease
```

Hasil release ada di `app/build/outputs/apk/<flavor>/release/`.

---

## Lisensi

MIT - bebas pakai, ubah, dan sebarkan.

---

*RumahSehat - Projek Kemitraan Kesehatan Lingkungan 2026*