# Rumah Sehat

**Aplikasi Android untuk penilaian kesehatan lingkungan rumah.**

Bikin penilaian kesehatan rumah jadi cepat dan rapi. Isi 17 indikator, foto kondisi rumah, dan semua data langsung tersimpan otomatis. Gak perlu khawatir kehilangan data saat sinyal putus, semua aman tersimpan di HP dulu.

| Versi | File APK | Buat siapa |
|---|---|---|
| Rumah Sehat | `RumahSehat-User.apk` | Petugas lapangan yang isi penilaian |

---

## Kenapa harus Rumah Sehat?

- **Cara pakai simpel.** Jawab pertanyaan satu per satu, foto rumah, lalu simpan.
- **Tetap jalan tanpa internet.** Data disimpan dulu di HP dan dikirim otomatis begitu ada sinyal.
- **Data lengkap.** 17 indikator mencakup komponen rumah, air bersih, jamban, SPAL, sampai perilaku penghuni.
- **Foto ikut tersimpan.** 3 foto per penilaian ikut terkirim dan mudah dilihat kembali.

---

## Cara pasang

1. Download file `RumahSehat-User.apk` di bagian **Release** halaman ini atau dari file APK di folder utama.
2. Buka file tersebut di HP.
3. Kalau muncul peringatan *"instalasi dari sumber tidak dikenal"*, izinkan. Wajar untuk aplikasi yang tidak lewat toko resmi.
4. Pasang dan buka aplikasinya.

> Kenapa ada peringatan? Karena aplikasi ini dibagikan langsung, bukan lewat toko aplikasi. Itu hal biasa untuk semua APK. File yang kamu download sudah kami pastikan asli dan aman.

---

## Cara pakai

1. Buka aplikasi, lalu isi nama petugas dan perusahaan.
2. Jawab 17 indikator sesuai kondisi rumah yang dinilai.
3. Ambil 3 foto: bagian depan rumah, sarana sanitasi, dan dapur atau SPAL.
4. Tekan **Simpan**.
5. Selesai. Data terkirim otomatis ke pusat data begitu jaringan tersambung.

---

## Cara baca hasil

Hasil penilaian berupa label status:

- **SEHAT** jika indikator rumah dan sanitasi sudah terpenuhi.
- **TIDAK SEHAT** jika masih ada indikator utama yang belum terpenuhi.

---

## Versi terdahulu

| Versi | Catatan |
|---|---|
| **v1.3.1** | Perbaikan pengiriman data ke server dan tampilan saat sinkron (ada keterangan berhasil/gagal). |
| **v1.3.0** | Tampilan lebih segar dengan menu bawah, pengiriman data lebih tahan terhadap sinyal lemah, dan foto lebih ringan saat dikirim. |
| **v1.2.0** | Keamanan data ditingkatkan dan pengiriman data diperbaiki. |
| **v1.1.0** | Data yang tertunda kini terkirim dengan benar. |
| **v1.0.0** | Rilis pertama: 17 indikator, 3 foto, sinkron otomatis. |

---

## Cara buat sendiri (untuk developer)

```powershell
# Versi debug
.\gradlew assembleUserDebug
# Versi rilis
.\gradlew assembleUserRelease
```

Hasil build ada di `app\build\outputs\apk\user\release\`.

---

## Lisensi

MIT. Bebas dipakai, diubah, dan dibagikan.

---

*Rumah Sehat* oleh Rayz.