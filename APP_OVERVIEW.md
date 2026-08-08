# Rumah Sehat — Penampilan & Cara Kerja Aplikasi

Dokumen ini menjelaskan apa yang dilihat pengguna dan bagaimana aplikasi bekerja secara teknis. Ditulis dari pembacaan kode (`app/`, `backend/`, `index.html`).

---

## 1. Gambaran Umum

Aplikasi Android **Rumah Sehat** (`com.rumahsehat`) adalah alat bantu petugas lapangan (assessor) untuk mengaudit kesehatan lingkungan rumah sesuai 3 kategori indikator:

| No | Kategori | Jumlah Item |
|---|---|---|
| I | Komponen Rumah | 8 |
| II | Sarana Sanitasi | 4 (bobot besar = esensial) |
| III | Perilaku Penghuni | 5 |
| | **Total** | **17** |

- Setiap penilaian menyertakan **3 foto** (tampak depan rumah, sarana sanitasi, area dapur/SPAL).
- Data disimpan lokal (Room DB) lalu dikirim offline-first ke **Google Apps Script → Google Sheets + Google Drive**.
- Build user (flavor `user`, applicationId `com.rumahsehat.user`) — satu APK publik: `RumahSehat-User.apk`.

> Tombol/teks UI seluruhnya Bahasa Indonesia. Skor numerik **tidak ditampilkan** ke petugas; hanya label level teks.

---

## 2. Alur Interaksi (Screens & Flow)

```
SplashActivity ──> MainActivity ──> AssessmentActivity ──17 soal──> Save/Review
                        │                    │
                        └── ReviewActivity <──┘
```

### 2.1 SplashActivity
- Tidak ada loading/splash art. Langsung `startActivity(MainActivity)` lalu `finish()`. (Splash hanya birokrasi entry point launcher.)

### 2.2 MainActivity (Beranda)
Tampilan (scrollable, latar `#F8F6F1` hangat, toolbar hijau tua `#0F6A4B`):
- **Greeting**: "Halo," / "Petugas" / "Pilih mulai untuk menilai kondisi rumah warga."
- **Tombol hijau "Mulai Penilaian"** (tanpa logo/home, ikon putih).
- **Info pending**: muncul jika ada penilaian belum terkirim, teks misal "2 penilaian menunggu dikirim. Akan dikirim otomatis saat ada internet."
- **List "Penilaian Tersimpan"** (RecyclerView kartu):
  - Judul = nama perusahaan/puskesmas.
  - Meta = "NamaPetugas · 08 Aug 2026".
  - Chip status **SEHAT** (hijau emerald) / **TIDAK SEHAT** (merah) — tapi di flavor `user` chip ini di-`GONE` (lihat ReviewActivity; kode memakai `BuildConfig.FLAVOR == "admin"`).
  - Status sync kanan bawah: **"Terkirim"** / **"Menunggu Kirim"**.

Flow behavior:
- Setiap iris list di-update dari `AssessmentViewModel.allAssessments` (LiveData dari Room, order `createdAt DESC`).
- Tip: blok yang menyentuh chip SEHAT sebenarnya dipasu kode di `AssessmentAdapter`, tetapi `MainActivity` memakai adapter default (`showStatus=false`) makanya user tidak melihat status SEHAT di list.
- Kalau list kosong → "Belum ada penilaian."

### 2.3 AssessmentActivity (Form penilaian, 17 langkah)
Tampilan:
- Toolbar "Penilaian Baru" + **LinearProgressIndicator** (progress = `(posisi+1)*100/17`).
- Header form: 2 kolom `TextInputLayout` OutlinedBox → **"Nama Petugas"** dan **"Nama Perusahaan / Puskesmas"** (tetap terlihat di semua halaman).
- **ViewPager2** (swipe manual **dinonaktifkan** `isUserInputEnabled=false` — navigate hanya lewat tombol **Kembali** / **Lanjut** di bawah).
- Tombol bawah: "Kembali" (text, tombol kiri) | "Lanjut" (tombol hijau kanan). Di halaman terakhir "Lanjut" berubah jadi **"Simpan & Kirim"**.

Behavior per soal (`QuestionFragment`):
- Judul: angka kecil hijau "PERTANYAAN 1 DARI 17" + judul item tebal 22sp.
- Kartu berisi:
  - **Slider** 0–4 (step 1, label teks: **Belum dinilai, Kurang, Cukup, Baik, Sangat Baik** sesuai pencapaian skor; angka bobot internal dihitung `maxScore * (level/4)`).
  - **Catatan** (opsional, OutlinedBox multiline).
  - Checkbox **"Tidak Berlaku"** (default checked). Saat dicentang → slider dan catatan nonaktif.
- Tombol **"Ambil Foto"** muncul **hanya di item pertama tiap kategori** (1.1, 2.1, 3.1), karena 1 foto per kategori. Memakai camera intents via `FileProvider` → file `IMG_<id>_<timestamp>.jpg` di external files dir, disimpan path di memory map `photoPaths`.

Validasi saat **Simpan & Kirim**:
1. Nama petugas + perusahaan wajib (alert "Penilaian Belum Lengkap").
2. Semua item yang **berlaku** wajib diberi level >0 (item level 0 → daftar "Item Belum Dinilai").
3. Ketiga foto wajib ada (daftar "Foto Belum Diambil": Foto 1/2/3).
- Jika lolos → `saveAssessment` → daftar `Assessment` id `ASM-<timestamp>`, status `PENDING` → simpan ke Room (gunakan `NonCancellable` agar tidak terjeda saat layar terutup) → segera `syncPending()` (percobaan kirim, gagal tetap PENDING) → Toast "Penilaian Berhasil Disimpan" → kembali ke MainActivity.

### 2.4 ReviewActivity (Tinjauan / riwayat detail)
- Dilewati dari list main (per card).
- Header meta: "Assessor: <nama>\n<perusahaan>\ndd MMM yyyy HH:mm".
- Chip status **SEHAT**/**TIDAK SEHAT** hanya tampil ketika `BuildConfig.FLAVOR == "admin"`; untuk user `visibility=GONE`.
- Daftar "Hasil Per Item" (RecyclerView card): judul item kiri + label level teks kanan (hijau untuk berlaku, abu-abu "Tidak Berlaku").
- Data diambil by ID dari Room (`loadReview`).

### 2.5 Splash → APP jalur lengkap
```
Launch → Splash → Main → [Mulai Penilaian] → Assessment (17 langkah + 3 foto) → Simpan → kembali Main
                       └> [tap card] → ReviewDetail
```

---

## 3. Statuses & Rule Penilaian

Bobot setiap item di-define di `FormItemsProvider`:
- Komponen Rumah & Perilaku: **20 poin** masing-masing.
- Sanitasi: Air Bersih **150**, Jamban **150**, SPAL **100**, Tempat Sampah **150**.

`AssessmentCalculator` (`domain/AssessmentCalculator.kt`):
- Menjumlah `totalAchieved` dan `totalApplicable` (item `isApplicable=false` dilewati).
- `percentage = totalAchieved/totalApplicable * 100`.
- **SEHAT** hanya jika **semua item esensial** (`weight >= 100`) ber-skore penuh. Selain itu **TIDAK SEHAT**.
- Perhitungan tidak menyimpan tabel bobot sendiri — bobot selalu diambil dari FormItems.

Data model `Assessment`:
- `id` = `ASM-<timestamp>`, `syncStatus`: `DRAFT` (default) ter-set `PENDING` setelah simpan, jadi `SYNCED`.
- `photoPathsJson` = `section=path` dipisah `;` (3 foto).

---

## 4. Persistensi & Sinkronisasi

- **Room DB** (`app/src/main/java/com/rumahsehat/data/db/AppDatabase.kt`, v2, WAL):
  - Tabel `assessments` + `score_items` (FK cascade).
  - Migration 1→2: menambah kolom `photoPathsJson`.
- **AssessmentDao**: `insert`, `getAllAssessments` (Flow), `getById`, `getScoreItemsForAssessment`, `getPendingAssessments` (`syncStatus != 'SYNCED'`), `updateSyncStatus`.

**Sync ke backend** (`data/remote/AssessmentSync.kt`) → endpoint Apps Script `.../exec`:
- POST JSON payload: `assessment_id`, `token` (`rs_sehat_2026`), `meta.{assessor_name,company}`, `scores.{17 item}` (key dipetakan 1-ke-1 dari itemId ke kolom), `summary.{total_achieved,is_healthy,status}`, `photos.{house_front,sanitation,kitchen_spal}` (base64, sampai 3 foto, setiap foto dibaca dari path lokal = file asli tanpa kompresi terpasang).
- Kode Apps Script (`backend/Code.gs`): validasi token → simpan 26 kolom ke sheet "Data Assessment", foto decode → Google Drive folder "Healthy Home Photos" → col dengan formula `=IMAGE(...)`.
- Apps Script memberi **302 redirect**; klien mengikut manual dengan POST berulang.

**Offline-first / retry**:
- Simpan lokal selalu berhasil walau offline (`syncPending` gagal diam-diam; assessment tetap `PENDING`).
- `RumahSehatApp.onCreate` → WorkManager enqueue unique work `sync-pending`: `Constraints(CONNECTED)`, `BackoffPolicy.EXPONENTIAL`, 10s. Memanggil `SyncWorker` → `syncPending()` daftar semua `PENDING`.
- `fetchCloudAssessments()` disiapkan untuk dashboard admin (GET `?action=data` → parse JSON `rows` ke `Assessment`), tapi belum dipakai di layar manapun.

---

## 5. Frontend Web / Landing & Admin

### `index.html` (landing/unduh)
- Halaman satu-col max-width 480px, tema hijau (Nunito, `--green:#0F6A4B`, `--green2:#1E8E5A`).
- Hero: logo + tagline; lalu kartu **"Petugas Lapangan"** dengan tombol unduh **RumahSehat-User.apk** (Android 6.0+, ±7MB); kartu "Cara Pasang"; kartu bantuan.

### backend/user.html
- (belum dibaca; di sini hanya dicatat) Halaman web "Penilaian Saya" per petugas — tidak disentuh oleh app Android.

---

## 6. Tech Stack & Build

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin (jvmToolchain 17) |
| UI | XML + View **Bukan Compose** (deprecated `View`, RecyclerView, Material Components, Slider) |
| Arsitektur | MVVM: Activity + Fragment + `AndroidViewModel` (LiveData) + Repository + Room |
| Database | Room 2.x (KSP schema export), `RoomDatabase.MIGRATION_1_2` |
| Background | WorkManager + Coroutines |
| Sinkronisasi | `HttpsUrlConnection` manual (no Retrofit) ke Apps Script |
| Kamera | `ActivityResultContracts.TakePicture` + FileProvider |
| Jaringan | INTERNET + camera (optional) |

`app/build.gradle.kts`:
- minSdk 23, targetSdk 35, compileSdk 35, Flavor `user` (`applicationIdSuffix ..user`).
- Signing release dari keystore `../rumahsehat-upload.keystore` (password di `GRADLE_PROP` seharusnya bukan plaintext — lihat nilai). Note: **storePassword fallback "rumahsehat2026" di kode** — hardening perlu.
- Release: minify+shrink aktif.
- `resValue("string","app_name","Rumah Sehat")` diterapkan ke semua variant.

---

## 7. Catatan untuk Review Kedua (sebelum obfuscate)

Hal yang perlu dicek sebelum release berikutnya:

- **Kode admin referensi tak ada**: `BuildConfig.FLAVOR == "admin"` di `ReviewActivity` & `AssessmentAdapter(showStatus)` — hanya flavor `user` ada di `build.gradle.kts`. Status SEHAT/TIDAK SEHAT tidak pernah tampil di mana pun di APK user.
- **Token hardcoded**: `API_TOKEN` di `backend/Code.gs` dan `token` di `AssessmentSync.buildPayload` sama (`rs_sehat_2026`) dan tertulis di sumber. Aman sebelum release publik, harus dipindah ke konfigurasi/env.
- **Kredensial keystore di build file**: `storePassword` dan `keyPassword` hardcoded `rumahsehat2026` di `app/build.gradle.kts`. Sebaiknya dari env `RS_KEYSTORE_PASSWORD`.
- **State form tidak bertahan restart**: `currentScore` pada `FormItem` dimutasi di memory; kalau Activity di-recreate (rotasi/batalkan proses) di tengah isian, jawaban hilang.
- **Foto hanya di memory**: `photoPaths` di ViewModel tidak dipersist; hilang saat Activity di-recreate sebelum Simpan.
- **Endpoint GET publik**: `fetchCloudAssessments` memakai `?action=data` tanpa token → seluruh baris spreadsheet terbaca siapa pun yang tahu URL endpoint.

---

## 8. Struktur Kode (ringkas)

```
app/src/main/java/com/rumahsehat/
├── RumahSehatApp.kt            # Application + enqueue sync
├── data/
│   ├── db/AppDatabase.kt       # Room v2, WAL, migration 1→2
│   ├── dao/AssessmentDao.kt    # CRUD + pending
│   ├── model/                  # Assessment, ScoreItem, FormItem, FormItemsProvider
│   ├── remote/AssessmentSync.kt # POST ke Apps Script, buildPhotos/payload
│   └── repository/AssessmentRepository.kt
├── domain/AssessmentCalculator.kt # skor & sehat/tidak sehat
├── sync/SyncWorker.kt          # retry offline
└── ui/                         # Activities, Fragments, Adapters, ViewModel, Level label
backend/Code.gs                 # Apps Script (doPost/doGet)
backend/user.html               # halaman web petugas
index.html, user.html           # landing unduh APK
```