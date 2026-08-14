# Rumah Sehat

Aplikasi **Android** untuk penilaian kesehatan lingkungan rumah oleh petugas lapangan. Offline-first: data disimpan di HP dulu, dikirim otomatis ke pusat data saat ada sinyal.

## Pekerjaan kita (status saat ini)

- **FASE FINALE** — penyelesaian/pemolesan, bukan fitur besar. Journey & keputusan di `journey.md` (entry 14-08-2026).
- **v1.5.0 (rilis)**: redesign premium UI — font Plus Jakarta Sans, warna forest-green dipertajam, identitas petugas jadi halaman-0, tombol kamera bulat di bar bawah, kartu jawaban dirampingkan, splash + animasi sync halus.
- **CI/CD**: `.github/workflows/android.yml` — build debug + unit test otomatis tiap push/PR; build release signed manual (secrets: `RS_KEYSTORE_BASE64`, `RS_KEYSTORE_PASSWORD`, `RS_API_TOKEN`).
- **Backend**: Google Apps Script (`backend/Code.gs`) → Google Sheets. Idempotensi anti-duplikat, analisis AI foto rumah, log persisten ke tab "Logs". Rencana di `planbackend.md` & `backend/plan.md` (tidak di-commit).

## Stack

- **App**: Kotlin, View System (XML layout, bukan Compose), Room, WorkManager, ViewModel.
- **Backend**: Google Apps Script (deploy `/exec`, redeploy version — URL jangan diganti), Google Sheets, OpenRouter (AI analisis foto).
- Build: Gradle (Kotlin DSL).

## Perintah penting

```powershell
.\gradlew assembleUserDebug    # build debug
.\gradlew assembleUserRelease  # build rilis
```

Hasil: `app\build\outputs\apk\user\release\`.

## Struktur

- `app/` — source Android.
- `backend/` — Apps Script server (Code.gs + plan).
- `prototype/` — mockup desain.
- `nextplan.md`, `planbackend.md`, `backend/plan.md` — dokumen plan (sengaja TIDAK di-commit).

## Skill yang relevan

UI/UX di proyek Android ini → `android-mobile-frontend-design`, `android-material3-design-system`, `android-viewsystem-foundations`, `android-ui-states-validation`. Untuk mockup visual → `imagegen-frontend-mobile`. Skill frontend-web (Next.js, design-taste-frontend, dll.) tidak berlaku.

## Hemat token (PENTING)

Cache gateway free (`opencode/*:free`) ga dipangkas → tiap balasan baca ulang seluruh konteks (±1M token/balasan). Detail: `reminder-token.md`. Aturan:

- STOP & `/new` di **balasan ke-20** (hard stop ke-30).
- Ganti task = langsung `/new`.
- Cek pemakaian: `python tokendump.py`.