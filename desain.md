# Desain Rumah Sehat — Reference & Patokan

> Dokumen ini adalah **satu-satunya sumber patokan desain** untuk semua pekerjaan ke depan (Android, web, dashboard).
>
> **Sumber yang di-scrap:**
> - `Downloads/rumahsehat/` (icon-192/512, favicon)
> - `Downloads/rumahsehatz/tahap1-3.png` (referensi visual — verifikasi manual)
> - `Downloads/rumahsehatz/stitch_rumahsehat_inspector_ui/`
>   - `eco_systemic_professional/DESIGN.md` ← **sistem desain utama**
>   - `beranda_rumahsehat_final/code.html` + `screen.png`
>   - `splash_screen_rumahsehat/code.html` + `screen.png`
>   - `history_screen_guest/code.html` + `screen.png`
>   - `app_overview.md`, `compose_ui_implementation.txt`
>   - `extracted…github.com_rydenmains_rumahsehat.md`
> - `Downloads/rumahsehatz/rumahsehat-inspeksi/` (Tailwind 3-tahap form: `src/components.js`, `data.js`, `input.css`, `tailwind.config.js`, `dist/tahap1-3.html`, `styles.css`)

---

## 1. Arah & Kepribadian Brand

Nama sistem: **Eco-Systemic Professional** — untuk inspeksi kesehatan lingkungan berisiko tinggi yang menuntut **precise + akuntabilitas**.

- Personality: **Authoritative, Clinical, Efficient**
- Gaya: **Premium Enterprise** = Corporate Modernism + Minimalism, tanpa hiasan berlebihan.
- Emosi yang ingin ditimbulkan: **organized calm** — navigasi data entry kompleks tanpa cognitive overload; alat seprofesional pemakainya.

| Aspek | Disebut di DESIGN.md |
|---|---|
| Warna sektor | "Deep Teal" → menjembatani sterilisasi-biru (healthcare) & hijau (environmental) |
| Font | **Inter** — sistematis, utilitarian, tetap tajam di lapangan |
| Mode | Light (default) + Dark (deep charcoal `#191C1B`) |

---

## 2. Color Tokens (sumber: `DESIGN.md` front-matter)

### 2.1 Full palette light

| Token M3 | HEX | Pemakaian |
|---|---|---|
| `surface` / `background` | `#F7FAFA` | Latar utama (putih-cool abu) |
| `surface-container-lowest` | `#FFFFFF` | Card tertinggi / konten berdiri sendiri |
| `surface-container-low` | `#F1F4F4` | Card ber-shadow, strip |
| `surface-container` | `#EBEEEE` | Level 2 container |
| `surface-container-high` | `#E5E9E9` | Level 3 / hover |
| `surface-container-highest` | `#E0E3E3` | Level 4 / elevasi maks |
| `surface-dim` | `#D7DBDA` | Dim surface (dark hover) |
| `on-surface` | `#181C1D` | Teks utama |
| `on-surface-variant` | `#3E4947` | Teks sekunder / keterangan |
| `outline` | `#6E7977` | Border ter-output |
| `outline-variant` | `#BEC9C6` | Border lembut / divider |
| `surface-tint` | `#016A60` | Tint accent |
| **`primary`** | **`#005099048`** | **Warna kunci: aksi, aktif, brand** |
| `on-primary` | `#FFFFFF` | Teks di atas primary |
| `primary-container` | `#006A60` | Tombol/stepper aktif ber-/on-primary-container |
| `on-primary-container` | `#95E7DA` | Icon/teks di atas primary-container |
| `primary-fixed` | `#9FF2E4` | tonal container tint 12% |
| `secondary` | `#4A6367` | Aksi sekunder, stabil |
| `secondary-container` | `#CCE7EC` | Container secondary (synced icon bg) |
| `on-secondary-container` | `#50696D` | |
| `tertiary` | `#38475F` | Aksi tersier |
| `error` | `#BA1A1A` | Status GAGAL / error (crimson) |
| `error-container` | `#FFDAD6` | bg error chip |
| `on-error-container` | `#93000A` | |

### 2.2 Dark mode (ringkas) — yang wajib ada di tema

```
colorscheme dark: primary #9FF2E4 (fixed), on-primary #00201C,
  surface #191C1B, on-surface #EEF1F1, on-surface-variant #BEC9C6,
  primary-container #006A60
```
- Elevasi di dark → via **Surface Tints** (overlay primary opacity), bukan shadow.

### 2.3 Semantic / status (jangan dipakai di tempat lain)

| Status | Warna | Konteks |
|---|---|---|
| **PASS / SEHAT / Terkirim** | Emerald `#2E7D32` | hasil inspeksi lolos / sync sukses |
| **FAIL / TIDAK SEHAT / Gagal** | Crimson `#B00020` | hasil tidak lolos |
| **PENDING / Menunggu kirim** | Amber `#FFA000` | menunggu sync |

> **Aturan**: tripel emerald/crimson/amber **hanya** untuk indikator status, bukan untuk aksi.

---

## 3. Typography — Inter

Semua level memakai **Inter**; headline SemiBold 600; label Medium 500.

| Scale | Font Size | Line | Weight | Letter | Pakai di |
|---|---|---|---|---|---|
| `display-lg` | 57 | 64 | 400 | -0.25 | Logo splash |
| `headline-lg` | 32 | 40 | 600 | 0 | Judul halaman desktop |
| `headline-lg-mobile` | 28 | 36 | 600 | 0 | Judul halaman HP |
| `headline-md` | 28 | 36 | 600 | 0 | Judul section |
| `headline-sm` | 24 | 32 | 500 | 0 | Header brand |
| `title-lg` | 22 | 28 | 500 | 0 | Card judul |
| `title-md` | 16 | 24 | 600 | 0.15 | Judul kecil / tombol+label penting |
| `body-lg` | 16 | 24 | 400 | 0.5 | Body utama (legibility entry) |
| `body-md` | 14 | 20 | 400 | 0.25 | Konten sekunder |
| `label-lg` | 14 | 20 | 500 | 0.1 | Button / labels |
| `label-md` | 12 | 16 | 500 | 0.5 | Meta, chips, hint |

Mobile ringkas: tampilan besar di-downshoot ke `headline-lg-mobile` di handset.

---

## 4. Spacing — Fluid Grid base 8dp

| Token | Nilai | Pakai |
|---|---|---|
| `unit` | 8px | baseline rhythm |
| `stack-sm` | 4px | gap mikro (icon-label) |
| `stack-md` | 12px | antar field terkait |
| `stack-lg` | 24px | antar kartu/section logis |
| `gutter` | 16px | gutter antar kolom |
| `edge-margin-mobile` | 16px | margin luar mobile (≤600) |
| `edge-margin-tablet` | 24px | margin luar tablet (600–840) |

**Breakpoint grid**:
- Mobile <600dp → 4 kolom, 16dp margin, bertumpuk vertikal
- Tablet 600–840dp → 8 kolom, 24dp margin, form 2 kolom
- Desktop ≥840 → 12 kolom, sidebar fixed

**Sumber proporsi**: component paddings `p-stack-*`, `h-16` header, `min-h-[160px]` main CTA.

---

## 5. Radius & Elevation

### Radius (rounded)
| Level | Nilai | Pakai |
|---|---|---|
| `sm` | 4px (0.25rem) | checkbox (2px klo seleksi) |
| `DEFAULT`/`lg` | 8–10px (0.5rem) | tombol, input, filter chips |
| `md`/`xl` | 12–16px (0.75–1rem) | kartu isi |
| `full` | 9999px | chips, FAB, pill button, stepper dot |

Per DESIGN.md: tombol & input **8px**, kartu **16px**, chips/search pill, checkbox 2px, radio tetap bulat.

### Elevation & shadow
| Level | Elevation | Shadow |
|---|---|---|
| 0 Flat | — | bg utama |
| 1 Card base | 1dp | `0 1px 3px rgba(0,0,0,.08)` *(blur 3, Y-offset 1, opacity 8%)* |
| 2 Active/hover | 3dp | `hover:shadow-md` |
| FAB | 6dp | — |
| Dark mode | — | ganti shadow dengan surface tints |

Kartu disarankan stroke ringan `outline-variant` (border) 0.5dp bila kontras rendah.

---

## 6. Components Library (referensi markup `rumahsehat-inspeksi` + stitch)

### 6.1 Header / TopAppBar
- `sticky top-0 h-16 bg-surface border-b border-outline-variant shadow-sm`, tinggi baris pakai `px-edge-margin-*`, `max-w-7xl` center.
- Kiri: icon back (pill hover) + **logo 32×32** + judul "RumahSehat" `text-primary` bold.
- Kanan (web): top nav pill (Beranda / Penilaian / **Riwayat** / Pengaturan); riwayat aktif = `bg-primary`? no → `bg-secondary-container` + `text-primary` + icon.
- Kanan (mobile): icon avatar person bulat `bg-primary-container`.

### 6.2 Bottom Navigation (mobile only, `md:hidden`)
- `fixed bottom-0 w-full bg-surface-container shadow-[0_-1px_3px_0_rgba(0,0,0,0.08)]`, `pb-safe` (safe-area).
- 3 tab: **Beranda** (`grid_view`), **Inspeksi** (`assignment`/`fact_check`), **Riwayat** (`history`).
- Tab aktif = `bg-primary-container text-on-primary-container rounded-full px-5 py-1 font-bold` (bentuk pill terangkat); non-aktif `text-on-surface-variant`.

### 6.3 Stepper (3-tahap)
- Horizontal di desktop; **vertical rail** di mobile.
- Completed step → dot `bg-primary` + ikon **check**; current → dot `bg-primary` + angka, teks `text-primary font-bold`; future → `bg-surface-container-high border`. 
- Garis penghubung: `bg-primary` (if prev done) / `bg-outline-variant`.
- Label tahap: `Komponen | Sanitasi | Perilaku`; header form: "Formulir Inspeksi", subteks "Tahap X dari 3 - <label>".

### 6.4 Question Card (field inspeksi)
- `bg-surface-container-lowest rounded-xl border border-outline-variant p-5 shadow-sm`.
- Nomor indikator bulat kiri `w-6 h-6 rounded-full bg-primary-container text-on-primary-container font-bold`.
- Judul (`title-md text-on-surface`) + subtitle deskripsi 1 kalimat (`body-md text-on-surface-variant`).
- Opsi: radio custom (bulat, border primary saat checked, bullet `bg-primary`) dalam label `flex gap-3 p-3 rounded-lg border`; checked → `bg-secondary-container border-primary`. Item-divider `0.5px outline-variant`.

### 6.5 Photo capture block
- Card `bg-surface-container-low rounded-xl p-5 border border-outline-variant shadow-sm mb`.
- Title + `body-md` deskripsi; tombol dashed `border-2 border-dashed border-outline-variant rounded-lg p-3 text-primary` → icon `photo_camera` + label **Ambil Foto**.
- Preview (Compose): img 64×64 thumb + label "Dokumentasi <kategori>" + "Wajib ambil foto"/"Ganti".

### 6.6 Buttons & Chips
- Primary: `bg-primary text-on-primary rounded-full px-6 py-2.5 font-label-lg` + icon arrow.
- Secondary: `border border-outline text-on-surface rounded-full px-6 py-2.5`.
- Filter chip: pill `border-outline-variant`, aktif = `bg-primary/10 text-primary` + icon check.

### 6.7 Status chip / badge (digunakan di history)
- `Terkirim`: dot `bg-primary` + `text-primary label-lg` ; `Menunggu Dikirim`: dot `bg-outline-variant text-on-surface-variant`.
- Home: badge "3 tertunda" `bg-primary` (Compose Badge).

### 6.8 Empty state
- Illustration line-art monochrome + tint primary opacity rendah; header `title-md` + tombol CTA jelas.

### 6.9 Ripple
- Material 3 ripple tuned **opacity 0.08** (premium, tak memekakan).

---

## 7. Halaman / Screens (inventory dari stitch)

### 7.1 Splash — "Eco-Landing" (`splash_screen_rumahsehat`)
- BG `bg-surface`, ambient gradient blobs (secondary-container blur slow float) → halus.
- Center: logo 96–128 + **display-lg** "RumahSehat" `text-primary` + tagline **"Enterprise Inspection & Safety Management"** `body-lg text-on-surface-variant`.
- Bottom: spinner putar primary + "INITIALIZING WORKSPACE" `label-md uppercase tracking`.
- Animasi logo masuk (scale .95→1 + fade, 1.2s cubic).

### 7.2 Beranda — `beranda_rumahsehat_final`
1. Header logo + tombol **sync** (`cloud_sync`).
2. Hero welcome: `headline-lg-mobile` "Selamat Datang, RumahSehat" + sub "Siap untuk…hari ini?"
3. **Sync banner**: card `surface-container-low`, icon `sync`, "Status Sinkronisasi" + "3 data menunggu dikirim", pill **Kirim Sekarang**.
4. **CTA bento** (2 card, `min-h-[160px]` blob bg):
   - "Mulai Inspeksi Baru" `bg-primary` + icon `assignment_add`.
   - "Lihat Riwayat" `bg-surface-container` + icon `history`.
5. **Ringkasan Hari Ini**: card `surface-container-lowest`, grid 2-kolom: `Selesai` (headline-md primary) • `Draf` (headline-md secondary).

### 7.3 Inspeksi 3 Tahap — `rumahsehat-inspeksi`
Alur: `tahap1 (Komponen Rumah)` → `tahap2 (Sarana Sanitasi)` → `tahap3 (Perilaku Penghuni)`.

**Isi pertanyaan (dari `src/data.js`):**

_Tahap 1 — Komponen Rumah (3 soal) + Foto 1:_
| # | Soal | Opsi (0/1/2) |
|---|---|---|
| 1 | Kondisi Langit-langit (Plafon) | Tidak ada \\ Ada kotor/rawan \\ Ada bersih & tidak rawat |
| 2 | Material & Kondisi Dinding | Bukan tembok \\ Semi permanen \\ Permanen (diplester, papan kedap) |
| 3 | Jenis Lantai | Tanah murni \\ Papan/bambu dekat tanah \\ Ubin/keramik/plesteran |

_Tahap 2 — Sanitasi (2 soal) + Foto 2:_
1. Sarana Air Bersih: Tidak ada \\ Ada (bukan milik, tak layak) \\ Ada milik sendiri & layak sehat
2. Jamban: Tidak ada \\ Ada (bukan leher angsa) / dibuang ke sungai \\ Ada (leher angsa, tutup, septic tank)

_Tahap 3 — Perilaku (5 soal) + Foto 3 (opsional):_
- Buka jendela kamar / RK: Tidak pernah \\ Kadang \\ Setiap hari
- Membersihkan rumah & halaman: Tidak pernah / Kadang / Setiap hari
- Buang tinja bayi: Sembarangan / Kadang jamban / Tiap hari jamban
- Buang sampah: Sembarangan / lugar / tempat sampah

Tombol: tahap1/2 `Batal`→`Kembali`; next `Lanjut` + `arrow_forward`; tahap3 next = **"Simpan & Kirim"** + `send`.

---

## 8. Integrasi: Android (project RumahSehat saat ini)

### 8.1 Aplikasi yang ada (`app_overview.md`)
- Kotlin, **XML View (bukan Compose)**, MVVM + Room + WorkManager.
- Flow: `Splash → Main → Assessment (17 soal, 3 foto) → [Simpan & Kirim] → kembali`.
- 17 indikator (I. Komponen 8, II. Sanitasi 4, III. Perilaku 5), **1 foto per kategori** = 3 foto.
- Status: `PENDING → SYNCED` via WorkManager retry 10s; POST JSON ke Apps Script.
  - bobot: I&III = 20 poin; sanitasi Air/Jawab SPAL/Tempat = 100–150 (esensial).
  - `SEHAT` hanya bila semua item esensial terisi penuh; selain itu `TIDAK SEHAT`.
- **Masalah yang harus dilumbles** (dari notes): token hardcoded, keystore pw di build file, state form tidak survive rotation, foto di memory, endpoint GET tanpa token.

### 8.2 Referensi Compose (`compose_ui_implementation.txt`) — arah migrasi
- Skema color: `Primary #006A60`, `SurfaceLight #F7FAFA`, `SecondaryBlue #4A635F`, `SuccessGreen #2E7D32`, `ErrorRed #B00020`, `PendingAmber #FFA000`, dark `SurfaceDark #191C1C`.
- Typography Compose: display large 32 bold, title-xl 18 semibold, body 16.
- Komponen: `IndicatorItem` (row + Switch), `PhotoCaptureSection` (Card + AsyncImage + tombol Ganti), HomeScreen (Scaffold + bottomBar sync badge), InspectionScreen (TopAppBar + LinearProgressIndicator + AnimatedContent 3 step + FAB Simpan & Selesai), ResultScreen (medal status Circle 120 + CircularProgress skor `totalScore/100` + "SKOR TOTAL").

---

## 9. Backend / API Contract (tetap, dari `Code.gs`)

- Web App endpoint `/exec`: `GET ?action=data` → JSON `{status, headers, rows}`; `POST` simpan.
- Token tulis: `rs_sehat_2026` → **pindah ke env/config sebelum rilis**.
- Sheet: **"Data Assessment"**, 26 kolom; Drive: **"Healthy Home Photos"**.
- Payload Android: `assessment_id`, `token`, `meta.{assessor_name,company}`, `scores.{17}`, `summary` (total_achieved, is_healthy, status), `photos.{house_front,sanitation,kitchen_spal}`.
- `index.html` (landing unduh), `user.html` ("Penilaian Saya" via fetch `?action=data`).

---

## 10. Aturan Konsistensi ke Depan (patokan)

1. **Satu sumber komponen**: jangan menulis markup card/stepper/tombol langsung — semua lewat komponen (di web: `components.js`; nanti Compose: shared composable).
2. **Palet sama**: token M3 teal (`primary #005048`, `primary-container #006A60`, bg `#F7FAFA`) — warna green lama Nunito `#0F6A4B` **tidak dipakai lagi** secara alias; kalau diperluankan di-map ke token teal.
3. `Inter` menggantikan Nunito untuk semua new UI.
4. Setiap pertanyaan/form wajib punya `subtitle/sub-deskripsi` 1 kalimat + opsi ditulis lengkap, tanpa prefix "a./b./c.".
5. Status SEHAT/TIDAK/TERKIRIM/PENDING hanya pakai semantic colors (emerald/crimson/amber; sync status pakai primary/outline).
6. Elevasi mengikuti Tonal Layers; dark mode = surface tints (bukan shadow tebal).
7. Ripple opacity 0.08; ripple dimasukkan komponen, bukan per layar.
8. Breakpoint mobile-first (4/8/12 kolom) sesuai §4.
9. Foto: 3 wajib (depan, sanitasi, dapur/SPAL/sampah) — tombol "Ambil Foto" hanya di 1 soal per tahap.

---

## 11. Asset Files

| File | Use |
|---|---|
| `Downloads/rumahsehat/favicon.png` | Favicon |
| `Downloads/rumahsehat/icon-192.png` / `-512.png` | PWA/icon |
| `Logo` asli `RumahSehat/logo.png` → **dipakai ulang di header/splash/splash logo (logo baru belum ada)** |
| `uploads/rumahsehatz/tahap1-3.png` | visual per tahap (verifikasi manual) |
| `stitch_.../beranda…/screen.png`, `splash…/screen.png`, `history…/screen.png` | Screenshot referensi visual (manual) |

> Semua `.png` tidak dapat dibaca oleh model tanpa image input — keputusan visual/brand final dari output "seas" jalan manual manusia.

---

## 12. Pin / Sumber rujukan

| Source | Path |
|---|---|
| Design system | `Downloads/rumahsehatz/stitch_…/eco_systemic_professional/DESIGN.md` |
| Forms | `Downloads/rumahsehatz/rumahsehat-inspeksi/…/src/{components,data,config,styles}` |
| Home | `…/beranda_rumahsehat_final/code.html` |
| Splash | `…/splash_screen_rumahsehat/code.html` |
| Berita | `…/history_screen_guest/code.html` |
| App flow | `…/app_overview.md`, original GitHub `rydenmains/rumahsehat` |

_Referensi terbaru `2026-08-08` — update file ini bila ada keputusan desain baru (jangan biarkan dua kode yang berbeda bermuara ke evaluasi style antar)._