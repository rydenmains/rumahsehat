# Rumah Sehat

Aplikasi **Android** untuk penilaian kesehatan lingkungan rumah oleh petugas lapangan. Offline-first: data disimpan di HP dulu, dikirim otomatis ke pusat data saat ada sinyal.

## Pekerjaan kita (status saat ini)

- **v1.4 (sedang jalan)**: redesign premium UI — font Plus Jakarta Sans, warna forest-green dipertajam, identitas petugas jadi halaman-0, tombol kamera bulat di bar bawah (muncul di halaman awal tiap section), kartu jawaban dirampingkan. Rencana lengkap di `nextplan.md`.
- **Backend**: Google Apps Script (`backend/Code.gs`) → Google Sheets. Idempotensi anti-duplikat, analisis AI foto rumah, log persisten ke tab "Logs". Rencana di `planbackend.md` & `backend/plan.md`.

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