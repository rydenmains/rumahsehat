/**
 * ==============================================================================
 * HEALTHY HOME ASSESSMENT APP - BACKEND SCRIPT (Google Apps Script)
 * Version: 3.3.0 (Groq utama + OpenRouter cadangan + prompt grounded)
 *
 * - doPost   : menyimpan baris assessment dari aplikasi Android (17 jawaban kata)
 * - doGet    : ?action=data -> membaca seluruh baris sheet sebagai JSON
 * - AI       : processPendingAi() dipanggil trigger tiap 10 menit → Gemini
 *              menganalisis 3 foto + jawaban, menulis 2 kolom hasil.
 *
 * Validasi & skor OTORITATIF di server:
 *   1) 17 jawaban divalidasi terhadap whitelist (P1-5) — payload di luar
 *      schema ditolak 400.
 *   2) Total Skor & Status Health dihitung ULANG server dari jawaban
 *      (SCORING_RULES mirror AssessmentCalculator.kt) — summary client
 *      DIABAIKAN (P1-6/P1-7). AI hanya menerima data hasil normalisasi ini
 *      (P1-9, via rowAnswers saat processPendingAi).
 * ==============================================================================
 */

var CONFIG = {
  SHEET_NAME: "Data Assessment",
  DRIVE_FOLDER_NAME: "Healthy Home Photos",
  CUSTOM_FOLDER_ID: "",
  // Token penulisan (dikirim app via payload.token). Dibaca dari Script Properties
  // "API_TOKEN" — TIDAK ada fallback hardcoded. Jika kosong, semua tulis/baca ditolak.
  API_TOKEN: PropertiesService.getScriptProperties().getProperty("API_TOKEN") || "",
  // v3.3: GROQ UTAMA (gratis longgar, vision), OpenRouter cadangan bila Groq gagal.
  // Key disimpan di Script Properties: GROQ_API_KEY (wajib), OPENROUTER_API_KEY (cadangan).
  // JANGAN hardcode key di file ini. Model Groq vision: meta-llama/llama-4-scout-17b-16e-instruct,
  // cadangan OpenRouter: google/gemma-3-27b-it:free (JANGAN *-reasoning: output chain-of-thought
  // Inggris "Okay, let's tackle..." bukan JSON → parse selalu gagal).
  GROQ_MODEL: "meta-llama/llama-4-scout-17b-16e-instruct",
  GEMINI_MODEL: "google/gemma-3-27b-it:free",
  // Jumlah baris maksimum diproses per panggilan processPendingAi() (limit runtime).
  AI_BATCH_SIZE: 5,
  // Token maksimum per request AI (model reasoning lama kepotong di 800 → JSON terpotong).
  AI_MAX_TOKENS: 1500,
  // Batas request tulis per menit (perlindungan kuota/serangan). 429 bila lewat.
  RATE_LIMIT_PER_MINUTE: 200,
  // Ukuran payload maksimum (foto terkompresi ~2MB; sisakan ruang). 413 bila lewat.
  MAX_PAYLOAD_BYTES: 5 * 1024 * 1024
};

/** Key AI diambil dari Script Properties — JANGAN hardcode.
 *  GROQ_API_KEY      : utama. Buat di https://console.groq.com/keys
 *  OPENROUTER_API_KEY: cadangan. Buat di https://openrouter.ai/keys
 */
function getGeminiKey() {
  return PropertiesService.getScriptProperties().getProperty("OPENROUTER_API_KEY");
}

/** Key Groq (provider utama v3.3). */
function getGroqKey() {
  return PropertiesService.getScriptProperties().getProperty("GROQ_API_KEY");
}

// ---------------------------------------------------------------------------
// Setup sekali (jalankan manual di editor Apps Script)
// ---------------------------------------------------------------------------

/**
 * Set token penulisan backend. NILAINYA WAJIB SAMA dengan BuildConfig.API_TOKEN
 * di app Android (isi local.properties / env RS_API_TOKEN), kalau beda maka
 * semua request app ditolak (FORBIDDEN) dan data tidak pernah masuk.
 * Jalankan sekali di editor:  Tools > Execute function > setApiToken
 */
function setApiToken(token) {
  if (!token) throw new Error("Parameter token kosong.");
  PropertiesService.getScriptProperties().setProperty("API_TOKEN", token);
  Logger.log("API_TOKEN diset: " + token.substring(0, 4) + "…");
}

/** Cek konfigurasi: apakah API_TOKEN + key AI sudah ter-set. Jalankan untuk verifikasi. */
function verifyConfig() {
  var p = PropertiesService.getScriptProperties();
  var t = p.getProperty("API_TOKEN");
  var k = p.getProperty("OPENROUTER_API_KEY");
  var g = p.getProperty("GROQ_API_KEY");
  var msg = "API_TOKEN ter-set: " + !!t + (t ? " (panjang " + t.length + ")" : "")
    + " | GROQ_API_KEY ter-set: " + !!g
    + " | OPENROUTER_API_KEY ter-set: " + !!k;
  Logger.log(msg);
  logToSheet("INFO", msg);
  return { api_token_set: !!t, groq_key_set: !!g, openrouter_key_set: !!k };
}

// ---------------------------------------------------------------------------
// doGet — endpoint baca data
// ---------------------------------------------------------------------------
function doGet(e) {
  var action = e && e.parameter && e.parameter.action;
  if (action && action.toLowerCase() === "data") {
    // Endpoint baca data TIDAK publik: butuh token yang sama dengan API_TOKEN
    // (dikirim sebagai query param ?action=data&token=...).
    var t = (e && e.parameter && e.parameter.token) || "";
    if (t !== CONFIG.API_TOKEN) {
      return createJsonResponse({ status: "FORBIDDEN", message: "Token tidak valid." }, 403);
    }
    return readDataResponse();
  }
  return createJsonResponse({
    status: "ONLINE",
    message: "Healthy Home Assessment API Endpoint is active.",
    timestamp: new Date().toISOString()
  }, 200);
}

/** Baca semua baris data assessment & kembalikan sebagai JSON. */
function readDataResponse() {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName(CONFIG.SHEET_NAME);
    if (!sheet || sheet.getLastRow() < 1) {
      return createJsonResponse({ status: "OK", headers: [], rows: [] }, 200);
    }

    var values = sheet.getDataRange().getValues();
    var headers = values[0].map(String);
    var rows = [];
    for (var i = 1; i < values.length; i++) {
      if (!values[i][0]) continue; // lewati baris kosong
      var obj = {};
      for (var c = 0; c < headers.length; c++) {
        obj[headers[c]] = values[i][c];
      }
      rows.push(obj);
    }
    return createJsonResponse({ status: "OK", headers: headers, rows: rows }, 200);
  } catch (error) {
    return createJsonResponse({ status: "ERROR", message: error.toString() }, 500);
  }
}

// ---------------------------------------------------------------------------
// doPost — simpan assessment (deferred AI: simpan cepat, analisis belakangan)
// ---------------------------------------------------------------------------
function doPost(e) {
  // LockService: cegah dua request menulis baris secara bersamaan (race condition).
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(15000); // tunggu maks 15 detik bila request lain sedang proses
  } catch (lockError) {
    return createJsonResponse({
      status: "ERROR",
      message: "Server sedang sibuk, coba lagi."
    }, 503);
  }

  try {
    if (!e || !e.postData || !e.postData.contents) {
      throw new Error("Payload request kosong atau tidak valid.");
    }

    // Rate limiting: batasi permintaan tulis per menit (CacheService, anti serangan/kuota).
    if (!allowRequest()) {
      return createJsonResponse({ status: "ERROR", message: "Terlalu banyak permintaan, coba lagi nanti." }, 429);
    }

    // Batasan ukuran payload: tolak request raksasa sebelum diproses.
    if (e.postData.contents.length > CONFIG.MAX_PAYLOAD_BYTES) {
      logToSheet("WARN", "doPost ditolak: payload terlalu besar (" + e.postData.contents.length + " byte).");
      return createJsonResponse({ status: "ERROR", message: "Payload terlalu besar." }, 413);
    }

    var payload = JSON.parse(e.postData.contents);

    // Cek token penulisan: tolak request tanpa token yang benar.
    if (!payload.token || payload.token !== CONFIG.API_TOKEN) {
      logToSheet("WARN", "doPost FORBIDDEN: token tidak valid dari app.");
      return createJsonResponse({ status: "FORBIDDEN", message: "Token tidak valid." }, 403);
    }

    // --- VALIDASI SERVER-SIDE (P1-5): jangan percaya input client mentah ---
    // Gagal validasi = tolak sebelum menyentuh Sheet/Drive (fail fast).
    var a = payload.answers || payload.scores || {};
    var validationErrors = validateAnswers(a);
    if (validationErrors.length > 0) {
      logToSheet("WARN", "doPost ditolak (jawaban tidak valid): " + validationErrors.join("; "));
      return createJsonResponse({
        status: "ERROR",
        message: "Jawaban tidak valid: " + validationErrors.join("; ")
      }, 400);
    }

    // Skor & status dihitung ULANG oleh server (P1-6/P1-7); summary client diabaikan.
    var serverSummary = computeServerSummary(a);

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = setupEnvironment();
    if (!sheet) {
      throw new Error("Sheet '" + CONFIG.SHEET_NAME + "' tidak ditemukan / gagal dibuat.");
    }

    // IDEMPOTENSI: kalau assessment_id sudah pernah masuk, jangan appendRow lagi.
    // Mencegah duplikat saat app retry (jaringan putus setelah backend sukses,
    // tombol sync ditekan dua kali, atau worker + manual jalan bersamaan).
    if (payload.assessment_id) {
      var usedIds = SpreadsheetApp.flush();
      var existingRows = sheet.getRange(2, 1, Math.max(0, sheet.getLastRow() - 1), 1).getValues();
      var strId = String(payload.assessment_id);
      for (var i = 0; i < existingRows.length; i++) {
        if (String(existingRows[i][0]) === strId) {
          logToSheet("INFO", "Duplikat dilewati (idempotent): " + strId);
          return createJsonResponse({
            status: "SUCCESS",
            message: "Data sudah ada, dilewati (idempotent).",
            assessment_id: payload.assessment_id
          }, 200);
        }
      }
    }

    // --- PROSES UPLOAD FOTO KE GOOGLE DRIVE (3 SLOT) ---
    // Setiap foto ditangani mandiri (try/catch per slot): kegagalan satu foto
    // JANGAN menghalangi appendRow — data teks indikator tetap tersimpan.
    var photoUrls = ["-", "-", "-"];
    var photos = payload.photos || {};
    var sectionKeys = ["house_front", "sanitation", "kitchen_spal"];

    if (photos && Object.keys(photos).length > 0) {
      var folder = getOrCreateFolder();
      for (var k = 0; k < sectionKeys.length; k++) {
        var base64Data = photos[sectionKeys[k]];
        if (!base64Data || base64Data.trim() === "") continue;
        try {
          if (base64Data.indexOf("base64,") !== -1) {
            base64Data = base64Data.split("base64,")[1];
          }
          var decodedImage = Utilities.base64Decode(base64Data);
          var fileName = (payload.assessment_id || "ASM_" + new Date().getTime()) + "_" + sectionKeys[k] + ".jpg";
          var blob = Utilities.newBlob(decodedImage, "image/jpeg", fileName);
          var file = folder.createFile(blob);
          // PRIVATE: hanya kepemilikan akun Google script (Dinas) yang bisa lihat.
          // Petugas cukup upload; pratinjau link hanya untuk admin punya akses Drive.
          file.setSharing(DriveApp.Access.PRIVATE, DriveApp.Permission.VIEW);
          // Simpan URL sebagai teks biasa yang bisa diklik, bukan rumus =IMAGE().
          photoUrls[k] = "https://drive.google.com/file/d/" + file.getId() + "/view";
        } catch (e) {
          logToSheet("ERROR", "Foto gagal di-upload (" + sectionKeys[k] + "): " + e);
          // abaikan foto gagal; tetap simpan data
        }
      }
    }

    // --- 17 JAWABAN SUDAH DIVALIDASI + SKOR DIHITUNG SERVER (di atas) ---

    var now = new Date();
    var formattedDate = Utilities.formatDate(now, ss.getSpreadsheetTimeZone(), "yyyy-MM-dd HH:mm:ss");
    var meta = payload.meta || {};

    var newRow = [
      safeCell(payload.assessment_id || "-"),
      formattedDate,
      safeCell(meta.assessor_name || payload.assessor_name || "-"),
      safeCell(meta.company || payload.company || "-"),
      safeCell(meta.house_name || payload.house_name || "-"),

      // I. KOMPONEN RUMAH (8 Items) — jawaban kata
      safeCell(a.langit_langit !== undefined ? a.langit_langit : "-"),
      safeCell(a.dinding !== undefined ? a.dinding : "-"),
      safeCell(a.lantai !== undefined ? a.lantai : "-"),
      safeCell(a.jendela_kamar !== undefined ? a.jendela_kamar : "-"),
      safeCell(a.jendela_rk !== undefined ? a.jendela_rk : "-"),
      safeCell(a.ventilasi !== undefined ? a.ventilasi : "-"),
      safeCell(a.lubang_asap !== undefined ? a.lubang_asap : "-"),
      safeCell(a.pencahayaan !== undefined ? a.pencahayaan : "-"),

      // II. SARANA SANITASI (4 Items)
      safeCell(a.air_bersih !== undefined ? a.air_bersih : "-"),
      safeCell(a.jamban !== undefined ? a.jamban : "-"),
      safeCell(a.spal !== undefined ? a.spal : "-"),
      safeCell(a.tempat_sampah !== undefined ? a.tempat_sampah : "-"),

      // III. PERILAKU PENGHUNI (5 Items)
      safeCell(a.buka_jendela_kamar !== undefined ? a.buka_jendela_kamar : "-"),
      safeCell(a.buka_jendela_rk !== undefined ? a.buka_jendela_rk : "-"),
      safeCell(a.bersih_rumah !== undefined ? a.bersih_rumah : "-"),
      safeCell(a.buang_tinja_bayi !== undefined ? a.buang_tinja_bayi : "-"),
      safeCell(a.buang_sampah !== undefined ? a.buang_sampah : "-"),

      // RINGKASAN — hasil hitung server (bukan summary client)
      safeCell(serverSummary.total_achieved),
      safeCell(serverSummary.status),
      safeCell(payload.notes || "-"),
      photoUrls[0], photoUrls[1], photoUrls[2],

      // ANALISIS AI (3 kolom) — diisi belakangan oleh processPendingAi()
      "", "", ""
    ];

    sheet.appendRow(newRow);

    var lastRow = sheet.getLastRow();
    sheet.setRowHeight(lastRow, 80); // Tinggi baris untuk foto

    logToSheet("INFO", "Data baru tersimpan: " + (payload.assessment_id || "tanpa-id") + " | " + formattedDate);

    // Q19: 1 baris log sync per doPost sukses (jejak kiriman dari HP).
    logSyncRow(payload.assessment_id, meta, serverSummary, formattedDate);

    return createJsonResponse({
      status: "SUCCESS",
      message: "Data berhasil disimpan!",
      assessment_id: payload.assessment_id,
      photo_url: photoUrls[0] !== "-" ? photoUrls[0] : null
    }, 200);

  } catch (error) {
    // Semua error ditangkap rapi: balasan JSON status ERROR (tidak pernah response polos).
    logToSheet("ERROR", "doPost gagal: " + error.toString());
    return createJsonResponse({
      status: "ERROR",
      message: error.toString()
    }, 500);
  } finally {
    try { lock.releaseLock(); } catch (ignored) {}
  }
}

// ---------------------------------------------------------------------------
// AI — deferred analysis (dipanggil trigger, atau manual)
// ---------------------------------------------------------------------------

/**
 * Proses baris yang belum dianalisis (kolom "Status Validasi AI" kosong).
 * Foto diambil ulang dari Drive (file ID tersimpan di URL kolom). Dipanggil
 * oleh createAiTrigger() tiap 10 menit. Data tetap tersimpan walau AI gagal.
 */
function processPendingAi() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName(CONFIG.SHEET_NAME);
  if (!sheet || sheet.getLastRow() < 2) return;

  var headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0].map(String);
  var colStatus = headers.indexOf("Status Validasi AI") + 1;
  var colExplanation = headers.indexOf("Penjelasan AI") + 1;
  var colRecommendation = headers.indexOf("Rekomendasi AI") + 1;
  if (colStatus === 0 || colRecommendation === 0) return; // header AI belum ada

  var data = sheet.getDataRange().getValues();
  var processed = 0;
  for (var r = 1; r < data.length && processed < CONFIG.AI_BATCH_SIZE; r++) {
    if (String(data[r][colStatus - 1]).trim() !== "") continue; // sudah diproses

    // Susun ulang photos dari kolom URL foto (file ID tersimpan di URL IMAGE).
    var photos = {};
    var photoCols = [
      { header: "URL Foto Komponen Rumah", key: "house_front" },
      { header: "URL Foto Sarana Sanitasi", key: "sanitation" },
      { header: "URL Foto Perilaku Penghuni", key: "kitchen_spal" }
    ];
    for (var key = 0; key < photoCols.length; key++) {
      var col = headers.indexOf(photoCols[key].header) + 1;
      if (col === 0) continue;
      var cellValue = String(data[r][col - 1] || "");
      var fileId = (cellValue.match(/\/d\/([a-zA-Z0-9_-]+)/) || [])[1];
      if (!fileId) continue;
      try {
        var blob = DriveApp.getFileById(fileId).getBlob();
        // Foto raksasa bikin request OpenRouter raksasa/lambat dan model sering
        // mengabaikan gambar (hasil "foto tidak jelas" yang ngawur padahal foto ada).
        // App sudah kompres ≤500KB; ini jaring pengaman bila file Drive besar.
        if (blob.getBytes().length > 900 * 1024) {
          try {
            blob = DriveApp.getFileById(fileId).getThumbnail();
            logToSheet("WARN", "Foto " + photoCols[key].key + " >900KB, pakai thumbnail.");
          } catch (thumbErr) { logToSheet("WARN", "Thumbnail gagal, pakai blob asli: " + thumbErr); }
        }
        photos[photoCols[key].key] = Utilities.base64Encode(blob.getBytes());
      } catch (e) { logToSheet("ERROR", "Foto gagal dibaca (" + photoCols[key].key + "): " + e); }
    }

    var result = analyzeAssessmentWithGemini(photos, {
      answers: rowAnswers(data[r], headers),
      summary: { total_achieved: data[r][headers.indexOf("Total Skor")] },
      is_healthy: String(data[r][headers.indexOf("Status Health")] || "").trim().toUpperCase() === "SEHAT",
      status: String(data[r][headers.indexOf("Status Health")] || "SEHAT")
    });

    // v3.2: JSON gagal parse → JANGAN segel "dilewati" (segel = skip selamanya).
    // Status dibiarkan kosong agar trigger 10 menit berikut retry otomatis, maks 3x
    // (counter di kolom Penjelasan AI = "RETRY n") supaya tidak membakar kuota :free.
    if (result.parseFailed) {
      var retryCount = 0;
      var m = String(data[r][colExplanation - 1] || "").match(/RETRY (\d+)/);
      if (m) retryCount = Number(m[1]);
      if (retryCount >= 3) {
        sheet.getRange(r + 1, colStatus).setValue("Analisis AI dilewati");
        if (colExplanation > 0) sheet.getRange(r + 1, colExplanation).clearContent();
        sheet.getRange(r + 1, colRecommendation).setValue("Respons AI bukan JSON valid setelah 3x coba: " + String(result.rawTail || "").substring(0, 120));
        logToSheet("ERROR", "AI " + String(data[r][0]) + " gagal parse 3x, disegel. Ekor: " + String(result.rawTail || ""));
      } else {
        if (colExplanation > 0) sheet.getRange(r + 1, colExplanation).setValue("RETRY " + (retryCount + 1));
        logToSheet("WARN", "AI " + String(data[r][0]) + " parse gagal (retry " + (retryCount + 1) + "/3). Ekor: " + String(result.rawTail || ""));
      }
      processed++;
      continue;
    }

    var colHealth = headers.indexOf("Status Health") + 1;

    // FOTO TIDAK VALID (buram/tidak relevan): tandai jelas, jangan ubah verdict.
    if (result.is_valid === false) {
      sheet.getRange(r + 1, colStatus).setValue("FOTO TIDAK VALID");
      if (colExplanation > 0) sheet.getRange(r + 1, colExplanation).setValue(result.explanation || "Foto tidak jelas / tidak menunjukkan kondisi rumah.");
      sheet.getRange(r + 1, colRecommendation).setValue(result.recommendation || "Foto tidak jelas; mohon ulangi pengambilan foto.");
      logToSheet("WARN", "AI " + String(data[r][0]) + " -> FOTO TIDAK VALID");
      processed++;
      continue;
    }

    sheet.getRange(r + 1, colStatus).setValue(result.flag);
    if (result.explanation) {
      if (colExplanation > 0) sheet.getRange(r + 1, colExplanation).setValue(result.explanation);
    }
    sheet.getRange(r + 1, colRecommendation).setValue(result.recommendation);

    // BODYGUARD: AI bisa menurunkan verdict FINAL bila foto tidak mendukung SEHAT.
    // Petugas jawab SEHAT tapi foto jelas menunjukkan kondisi buruk → diturunkan.
    // v1.7: map kenal KURANG SEHAT (QA-02) — SEHAT(0) < KURANG(1) < TIDAK(2).
    if (colHealth > 0) {
      var prevHealth = String(data[r][colHealth - 1] || "").toUpperCase();
      var severity = { "SEHAT": 0, "KURANG SEHAT": 1, "PERLU PERBAIKAN": 1, "TIDAK SEHAT": 2 };
      var aiSev = severity[result.flag];
      var prevSev = severity[prevHealth];
      if (aiSev !== undefined && prevSev !== undefined && aiSev > prevSev) {
        sheet.getRange(r + 1, colHealth).setValue(result.flag);
        logToSheet("WARN", "AI bodyguard: " + String(data[r][0]) + " Status Health diturunkan " + prevHealth + " -> " + result.flag);
      }
    }

    logToSheet("INFO", "AI " + String(data[r][0]) + " -> " + result.flag + " | " + result.recommendation);
    processed++;
  }
}

/** Setup trigger yang memanggil processPendingAi tiap X menit. Jalankan sekali manual. */
function createAiTrigger() {
  ScriptApp.newTrigger("processPendingAi")
    .timeBased()
    .everyMinutes(10)
    .create();
}

/**
 * Buka segel baris yang pernah "Analisis AI dilewati" (fallback dari proses yang gagal)
 * agar bisa dianalisis ulang. Jalankan SEKALI manual SETELAH men-deploy kode fix.
 */
function resetAiStatuses() {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(CONFIG.SHEET_NAME);
  if (!sheet || sheet.getLastRow() < 2) return;

  var headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0].map(String);
  var colStatus = headers.indexOf("Status Validasi AI") + 1;
  var colExplanation = headers.indexOf("Penjelasan AI") + 1;
  var colRecommendation = headers.indexOf("Rekomendasi AI") + 1;
  if (colStatus === 0 || colRecommendation === 0) return;

  var data = sheet.getDataRange().getValues();
  var resetCount = 0;
  for (var r = 1; r < data.length; r++) {
    var status = String(data[r][colStatus - 1] || "");
    if (status.indexOf("Analisis AI dilewati") === -1) continue;
    sheet.getRange(r + 1, colStatus).clearContent();
    if (colExplanation > 0) sheet.getRange(r + 1, colExplanation).clearContent();
    sheet.getRange(r + 1, colRecommendation).clearContent();
    resetCount++;
  }
  Logger.log("resetAiStatuses: " + resetCount + " baris dibuka untuk diproses ulang.");
  logToSheet("INFO", "resetAiStatuses: " + resetCount + " baris dibuka untuk diproses ulang.");
}

/**
 * Analisis 3 foto + jawaban kata. v3.3: GROQ UTAMA → OpenRouter CADANGAN.
 * Output konsisten: { is_valid, flag, recommendation }.
 * Fallback bila key kosong / foto tak lengkap / request gagal → data tetap tersimpan.
 */
function analyzeAssessmentWithGemini(photos, assessment) {
  var fallback = function (flag, recommendation) {
    return { is_valid: true, flag: flag, recommendation: recommendation };
  };

  var sectionKeys = ["house_front", "sanitation", "kitchen_spal"];
  var content = [];
  var photoCount = 0;
  for (var k = 0; k < sectionKeys.length; k++) {
    var dataB64 = photos[sectionKeys[k]];
    if (!dataB64) continue;
    if (dataB64.indexOf("base64,") !== -1) dataB64 = dataB64.split("base64,")[1];
    content.push({
      type: "image_url",
      image_url: { url: "data:image/jpeg;base64," + dataB64 }
    });
    photoCount++;
  }
  if (photoCount === 0) {
    return fallback("Analisis AI dilewati", "Tidak ada foto untuk dianalisis.");
  }

  var answersText = "";
  var answers = assessment.answers || {};
  Object.keys(answers).forEach(function (key) {
    answersText += key + "=" + sanitizeForPrompt(answers[key]) + "; ";
  });

  var prompt = [
    "Kamu adalah asisten Dinas Kesehatan untuk validasi Rumah Sehat. Jawab HANYA JSON, tanpa teks lain.",
    "Ada 3 foto, masing-masing punya peran BERBEDA — jangan tertukar:",
    "Foto 1 = KOMPONEN RUMAH (langit-langit, dinding, lantai, jendela kamar, jendela ruang keluarga, ventilasi, pencahayaan).",
    "Foto 2 = SARANA SANITASI (sumber air bersih, jamban/leher angsa/tutup/septic tank, SPAL/saluran limbah, tempat sampah).",
    "Foto 3 = PERILAKU PENGHUNI (jendela dibuka, rumah dibersihkan, pembuangan tinja bayi & sampah).",
    "Jawaban petugas (17 indikator): " + answersText,
    "Status sementara dari jawaban (hitung server, otoritatif): " + assessment.status + ".",
    "",
    "ATURAN WAJIB:",
    "1. Deskripsikan tiap foto SECARA SPESIFIK dan BERBEDA satu sama lain: sebut objek/ruangan yang terlihat",
    "   (mis. 'terlihat lantai keramik bersih', 'terlihat jamban leher angsa dengan tutup').",
    "   DILARANG menulis 'foto tidak jelas, tidak bisa dipastikan' yang SAMA untuk ketiga foto sekaligus.",
    "   Kalimat itu hanya boleh dipakai untuk SATU foto yang memang buram/hitam/blank/bukan rumah.",
    "2. Jawaban petugas adalah sumber UTAMA; foto hanya pendukung.",
    "3. Flag WAJIB sama persis dengan Status sementara (" + assessment.status + "), KECUALI foto dengan JELAS",
    "   bertentangan (mis. jawaban 'SEHAT' tapi foto menunjukkan jamban kotor/terbuka, sampah menumpuk,",
    "   genangan SPAL, dinding retak parah). Bila menurunkan, bukti konkretnya HARUS tertulis di per_photo.",
    "4. is_valid=false HANYA bila foto benar-benar blank/hitam/buram total/bukan foto rumah.",
    "   Foto seadanya (gelap, miring, sebagian ruangan) tetap is_valid=true + deskripsikan apa adanya.",
    "5. Rekomendasi: 1-2 kalimat Bahasa Indonesia, menyebut indikator terburuk dari jawaban (huruf 'a' dulu),",
    "   JANGAN mengulang kalimat generik.",
    "",
    "Jawab JSON HANYA dengan format persis ini (tanpa markdown fence):",
    '{"is_valid": true, "flag": "' + assessment.status + '",',
    ' "per_photo": ["deskripsi spesifik foto 1 (komponen rumah)", "deskripsi spesifik foto 2 (sanitasi)", "deskripsi spesifik foto 3 (perilaku)"],',
    ' "recommendation": "rekomendasi 1-2 kalimat Bahasa Indonesia"}'
  ].join("\n");

  content.unshift({ type: "text", text: prompt });

  // Helper lokal: kirim ke 1 provider, return { ok, text } atau { ok:false, reason }.
  // response_format json_object dicoba dulu (Groq dukung penuh); bila 400 → retry tanpa.
  var tryProvider = function (url, apiKey, model, label) {
    var body = {
      model: model,
      messages: [{ role: "user", content: content }],
      temperature: 0.1, // rendah tapi >0: 0 murni bikin model gratis ngaco/stuck
      max_tokens: CONFIG.AI_MAX_TOKENS,
      response_format: { type: "json_object" }
    };
    var resp = postToAi(url, apiKey, body, label);
    if (resp === null) { // 400 karena response_format tidak didukung → 1x tanpa JSON mode
      delete body.response_format;
      resp = postToAi(url, apiKey, body, label);
    }
    if (resp === null) return { ok: false, reason: "request gagal (lihat Execution log)" };
    if (resp.error) return { ok: false, reason: String(resp.error.message || resp.error) };
    var t = resp.choices && resp.choices[0] && resp.choices[0].message && resp.choices[0].message.content;
    if (t && typeof t !== "string") {
      // Sebagian model mengembalikan content sebagai array part (bukan string).
      try {
        t = t.map(function (p) { return (p && p.text) || ""; }).join("");
      } catch (convErr) { t = String(t); }
    }
    if (!t) return { ok: false, reason: "respons kosong" };
    var p = parseAiJson(t);
    if (!p) return { ok: false, reason: "bukan JSON", rawTail: String(t).substring(0, 200) };
    return { ok: true, parsed: p };
  };

  // 1) GROQ UTAMA
  var groqKey = getGroqKey();
  var usedProvider = "";
  var attempt = null;
  if (groqKey) {
    usedProvider = "groq";
    attempt = tryProvider("https://api.groq.com/openai/v1/chat/completions", groqKey, CONFIG.GROQ_MODEL, "Groq");
    if (!attempt.ok) logToSheet("WARN", "Groq gagal (" + attempt.reason + "), fallback ke OpenRouter.");
  } else {
    logToSheet("WARN", "GROQ_API_KEY belum diisi, langsung ke OpenRouter.");
  }

  // 2) OPENROUTER CADANGAN (bila Groq gagal / key kosong)
  if (!attempt || !attempt.ok) {
    var orKey = getGeminiKey();
    if (!orKey) {
      return fallback("Analisis AI dilewati",
        "Key AI belum diisi di Script Properties (GROQ_API_KEY utama, OPENROUTER_API_KEY cadangan).");
    }
    usedProvider = "openrouter";
    attempt = tryProvider("https://openrouter.ai/api/v1/chat/completions", orKey, CONFIG.GEMINI_MODEL, "OpenRouter");
    if (!attempt.ok) {
      // Kedua provider gagal → sinyal retry (JANGAN segel), pemanggil yang atur counter.
      return { parseFailed: true, rawTail: usedProvider + ": " + String(attempt.reason || "") + " " + String(attempt.rawTail || "").substring(0, 160) };
    }
  }

  logToSheet("INFO", "AI via " + usedProvider + " OK.");
  var parsed = attempt.parsed;
  var flag = String(parsed.flag || assessment.status || "SEHAT").toUpperCase();
  // Urutan penting: cek TIDAK dulu, lalu KURANG/PERLU, terakhir SEHAT murni —
  // "KURANG SEHAT" mengandung kata "SEHAT" sehingga cek SEHAT duluan = bug.
  var normalizedFlag = "PERLU PERBAIKAN";
  if (flag.indexOf("TIDAK") !== -1) normalizedFlag = "TIDAK SEHAT";
  else if (flag.indexOf("KURANG") !== -1 || flag.indexOf("PERLU") !== -1) normalizedFlag = "KURANG SEHAT";
  else if (flag === "SEHAT") normalizedFlag = "SEHAT";
  var perPhoto = [];
  if (Array.isArray(parsed.per_photo)) {
    perPhoto = parsed.per_photo.map(function (p) { return String(p); });
  }
  var explanation = [
    "Dasar penilaian (gabungan jawaban petugas + analisis foto):",
    "Foto 1 (depan rumah): " + (perPhoto[0] || "-"),
    "Foto 2 (sanitasi): " + (perPhoto[1] || "-"),
    "Foto 3 (dapur/SPAL): " + (perPhoto[2] || "-"),
    "Kesimpulan: " + normalizedFlag + ". " + (parsed.recommendation || "")
  ].join("\n");
  return {
    is_valid: !!parsed.is_valid,
    flag: normalizedFlag,
    explanation: explanation,
    recommendation: String(parsed.recommendation || "-")
  };
}

/** Kirim chat-completion ke provider AI mana pun (Groq / OpenRouter, format OpenAI).
 *  Return parsed JSON body, atau null bila gagal/jaringan/400 (pemanggil yang retry). */
function postToAi(url, apiKey, requestBody, label) {
  try {
    var response = UrlFetchApp.fetch(url, {
      method: "post",
      headers: { Authorization: "Bearer " + apiKey },
      contentType: "application/json",
      payload: JSON.stringify(requestBody),
      muteHttpExceptions: true,
      timeoutSeconds: 60
    });
    return JSON.parse(response.getContentText());
  } catch (error) {
    logToSheet("ERROR", "postToAi " + (label || "") + " gagal: " + error.toString());
    return null;
  }
}

/** Nama lama (kompatibel bila ada trigger/test lama yang memanggilnya). */
function postToOpenRouter(apiKey, requestBody) {
  return postToAi("https://openrouter.ai/api/v1/chat/completions", apiKey, requestBody, "OpenRouter");
}

// ---------------------------------------------------------------------------
// Logging persistent: tulis debug/error ke tab "Logs" di spreadsheet yang sama.
// Apps Script Logger log-nya terbatas; tab Logs bisa dicek kapan saja.
// ---------------------------------------------------------------------------

/**
 * Tulis satu baris log ke sheet "Logs". Baris terbaru di paling bawah.
 * Menambahkan kolom "Timestamp" dengan timezone spreadsheet.
 */
function logToSheet(level, message) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName("Logs");
    if (!sheet) {
      sheet = ss.insertSheet("Logs");
      sheet.appendRow(["Waktu", "Level", "Pesan"]);
      sheet.getRange(1, 1, 1, 3).setFontWeight("bold");
      sheet.setFrozenRows(1);
    }
    var now = Utilities.formatDate(new Date(), ss.getSpreadsheetTimeZone(), "yyyy-MM-dd HH:mm:ss");
    sheet.appendRow([now, level, String(message).substring(0, 800)]);
  } catch (logError) {
    // Jangan biarkan logging mematikan alur utama.
    Logger.log("logToSheet gagal: " + logError.toString());
  }
}

/** Bersihkan tab Logs (header tetap). Panggil manual kalau kebanyakan. */
function clearLogs() {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName("Logs");
  if (!sheet) return;
  var lastRow = sheet.getLastRow();
  if (lastRow > 1) sheet.getRange(2, 1, lastRow - 1, sheet.getLastColumn()).clearContent();
  Logger.log("clearLogs: " + (lastRow - 1) + " baris log dihapus.");
}

// ---------------------------------------------------------------------------
// AuditLog v1.7 (Q19): jejak edit manual admin + log sync dari HP
// ---------------------------------------------------------------------------

/**
 * 1 baris log sync per doPost sukses → sheet "AuditLog" (dibuat bila belum
 * ada): waktu, assessor, assessment_id, status 3-tier, total skor.
 * Kegagalan logging TIDAK menggagalkan doPost (fail-open, seperti logToSheet).
 */
function logSyncRow(assessmentId, meta, serverSummary, formattedDate) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName("AuditLog");
    if (!sheet) {
      sheet = ss.insertSheet("AuditLog");
      sheet.appendRow(["Waktu", "Aktor", "Aksi", "Assessment ID", "Detail"]);
      sheet.getRange(1, 1, 1, 5).setFontWeight("bold");
      sheet.setFrozenRows(1);
    }
    var who = (meta && (meta.assessor_name || meta.company)) || "-";
    sheet.appendRow([
      formattedDate, String(who), "SYNC",
      String(assessmentId || "-"),
      String((serverSummary && serverSummary.status) || "-") +
        " | skor " + String((serverSummary && serverSummary.total_achieved) || 0)
    ]);
  } catch (e) {
    Logger.log("logSyncRow gagal: " + e.toString());
  }
}

/**
 * Tangkap EDIT MANUAL admin di sheet Data Assessment → sheet "AuditLog".
 * Installable onEdit trigger (install manual SEKALI di editor Apps Script):
 *   1. Buka editor Apps Script → Triggers (jam) → Add Trigger.
 *   2. Function: onAdminEdit | Event source: From spreadsheet | Event type: On edit.
 *   3. Save (otorisasi sekali). Selesai — edit manual tercatat otomatis.
 * Batasan Google: Session.getActiveUser().getEmail() kosong untuk akun Gmail
 * biasa (privasi) → fallback "unknown". Hanya akun Workspace domain sama yang
 * mengisi email. Edit via API/script (sync HP) TIDAK memicu trigger ini —
 * sync HP ditutup oleh logSyncRow di atas.
 */
function onAdminEdit(e) {
  try {
    if (!e || !e.source) return;
    var sheet = e.source.getActiveSheet();
    if (!sheet || sheet.getName() !== CONFIG.SHEET_NAME) return;
    var range = e.range;
    if (!range || range.getRow() === 1) return; // header diabaikan
    var ss = e.source;
    var log = ss.getSheetByName("AuditLog");
    if (!log) {
      log = ss.insertSheet("AuditLog");
      log.appendRow(["Waktu", "Aktor", "Aksi", "Assessment ID", "Detail"]);
      log.getRange(1, 1, 1, 5).setFontWeight("bold");
      log.setFrozenRows(1);
    }
    var email = "";
    try { email = Session.getActiveUser().getEmail() || ""; } catch (ignored) {}
    var now = Utilities.formatDate(new Date(), ss.getSpreadsheetTimeZone(), "yyyy-MM-dd HH:mm:ss");
    var rowId = String(sheet.getRange(range.getRow(), 1).getValue() || "-");
    log.appendRow([
      now, email || "unknown", "EDIT",
      rowId,
      "range " + range.getA1Notation() + " | lama: " +
        String(e.oldValue !== undefined ? e.oldValue : "?").substring(0, 60) +
        " → baru: " + String(e.value !== undefined ? e.value : "?").substring(0, 60)
    ]);
  } catch (err) {
    Logger.log("onAdminEdit gagal: " + err.toString());
  }
}

/** Parse JSON dari teks model yang sering dibungkus teks/fence ```json```.
 * v3.2: coba SEMUA kandidat {...} dari yang TERPANJANG (JSON asli), bukan yang
 * pertama (sering hanya potongan chain-of-thought reasoning yang tak valid). */
function parseAiJson(text) {
  var source = String(text || "");
  source = source.replace(/```(?:json)?/gi, "").trim();
  var candidates = source.match(/\{[\s\S]*?\}(?=\s*(\{|$))/g)
    || source.match(/\{[\s\S]*\}/g) || [];
  candidates.sort(function (x, y) { return y.length - x.length; });
  for (var i = 0; i < candidates.length; i++) {
    try {
      var obj = JSON.parse(candidates[i]);
      if (obj && (obj.flag !== undefined || obj.recommendation !== undefined || obj.is_valid !== undefined)) return obj;
    } catch (ignore) { /* kandidat berikut */ }
  }
  try { return JSON.parse(source); } catch (e) { return null; }
}

// ---------------------------------------------------------------------------
// Utilities
// ---------------------------------------------------------------------------

function createJsonResponse(data, statusCode) {
  return ContentService
    .createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}

/**
 * Rate limiter minimal berbasis CacheService: membatasi request tulis per menit.
 * Apps Script tidak punya middleware; counter per-menit ini cukup untuk
 * melindungi kuota dari lonjakan/penyalahgunaan token. Bukan anti-bot penuh.
 * ponytail: per-menit global (bukan per-IP) karena Apps Script tidak memberi IP client
 * via doPost; sudah cukup server-side karena semua app pakai token yang sama.
 */
function allowRequest() {
  try {
    var cache = CacheService.getScriptCache();
    var minuteKey = Math.floor(Date.now() / 60000).toString();
    var count = Number(cache.get(minuteKey) || 0) + 1;
    if (count > CONFIG.RATE_LIMIT_PER_MINUTE) {
      return false;
    }
    cache.put(minuteKey, count.toString(), 65); // TTL = menit + 5 detik buffer
    return true;
  } catch (e) {
    // Bila cache gagal (jarang), jangan blokir alur utama.
    return true;
  }
}

/**
 * Sanitasi sel sebelum ditulis ke Spreadsheet: nilai user yang diawali
 * '=', '+', '-', '@' diubah jadi teks biasa (prefix apostrof) supaya tidak
 * diinterpretasikan sebagai formula oleh Sheets (spreadsheet injection).
 */
function safeCell(value) {
  var s = String(value == null ? "" : value);
  if (/^[=+\-@]/.test(s)) return "'" + s;
  return s;
}

function getOrCreateFolder() {
  if (CONFIG.CUSTOM_FOLDER_ID && CONFIG.CUSTOM_FOLDER_ID.trim() !== "") {
    return DriveApp.getFolderById(CONFIG.CUSTOM_FOLDER_ID);
  }
  var folders = DriveApp.getFoldersByName(CONFIG.DRIVE_FOLDER_NAME);
  if (folders.hasNext()) {
    return folders.next();
  } else {
    return DriveApp.createFolder(CONFIG.DRIVE_FOLDER_NAME);
  }
}

/** Mapping header kolom (17 indikator) → key jawaban (sama dgn Android). */
var ANSWER_KEY_BY_HEADER = {
  "1. Langit-langit": "langit_langit", "2. Dinding": "dinding", "3. Lantai": "lantai",
  "4. Jendela Kamar": "jendela_kamar", "5. Jendela RK": "jendela_rk",
  "6. Ventilasi": "ventilasi", "7. Lubang Asap": "lubang_asap",
  "8. Pencahayaan": "pencahayaan",
  "9. Air Bersih": "air_bersih", "10. Jamban": "jamban", "11. SPAL": "spal",
  "12. Tempat Sampah": "tempat_sampah",
  "13. Buka Jend. Kamar": "buka_jendela_kamar", "14. Buka Jend. RK": "buka_jendela_rk",
  "15. Bersih Rumah": "bersih_rumah", "16. Tinja Bayi": "buang_tinja_bayi",
  "17. Buang Sampah": "buang_sampah"
};

/** Baca 17 jawaban kata dari satu baris sheet. */
function rowAnswers(row, headers) {
  var answers = {};
  for (var h = 0; h < headers.length; h++) {
    var key = ANSWER_KEY_BY_HEADER[headers[h]];
    if (key && row[h] !== "" && row[h] !== undefined && row[h] !== null) {
      answers[key] = row[h];
    }
  }
  return answers;
}

// ---------------------------------------------------------------------------
// Validasi & skor otoritatif server — mirror AssessmentCalculator.kt +
// FormItemsProvider.kt. Jangan ubah bobot di sini tanpa menyamakan Kotlin.
// ---------------------------------------------------------------------------

/** Item esensial = bobot >= nilai ini; SEHAT butuh semua esensial faktor penuh. */
var ESSENTIAL_MIN_WEIGHT = 100;

/** Bobot tiap indikator (= maxScore di FormItemsProvider.kt). */
var SCORING_RULES = {
  langit_langit: 20, dinding: 20, lantai: 20,
  jendela_kamar: 20, jendela_rk: 20, ventilasi: 20,
  lubang_asap: 20, pencahayaan: 20,
  air_bersih: 150, jamban: 150, spal: 100, tempat_sampah: 150,
  buka_jendela_kamar: 20, buka_jendela_rk: 20, bersih_rumah: 20,
  buang_tinja_bayi: 20, buang_sampah: 20
};

/** Faktor huruf opsi → proporsi skor (a=0, b=0.5, c=1; d=1 khusus 3.4). */
var ANSWER_FACTOR = { a: 0, b: 0.5, c: 1, d: 1 };

/**
 * Bersihkan teks jawaban sebelum masuk prompt AI (anti prompt-injection):
 * buang newline/karakter kontrol (injeksi butuh baris baru), pembatas ";"
 * diganti koma, dan panjang dicap 80 char.
 * ponytail: cap 80 + strip newline mempersempit payload, bukan imun penuh;
   vektor foto (teks di dalam gambar) tetap tak terbendung — ditutup oleh
   flag-enum + JSON-only output di bawahnya.
 */
function sanitizeForPrompt(value) {
  return String(value)
    .replace(/[\u0000-\u001f\u007f]+/g, " ")
    .replace(/;/g, ",")
    .substring(0, 80);
}

/**
 * Validasi 17 jawaban dari client (P1-5): semua key whitelist harus ada dan
 * nilainya label opsi berformat "a. teks" / "b" / ... atau "Tidak berlaku".
 * Key di luar whitelist juga ditolak. Return array pesan error (kosong=valid).
 */
function validateAnswers(answers) {
  var errors = [];
  var a = answers || {};
  for (var key in SCORING_RULES) {
    var val = a[key];
    if (val === undefined || val === null || String(val).trim() === "") {
      errors.push(key + ": jawaban hilang");
      continue;
    }
    var s = String(val).trim();
    if (s === "Tidak berlaku") continue;
    if (!/^([abcd])([.\s]|$)/i.test(s)) errors.push(key + ": format tidak dikenal");
  }
  for (var extra in a) {
    if (!SCORING_RULES.hasOwnProperty(extra)) errors.push(extra + ": key tidak dikenal");
  }
  return errors;
}

/**
 * Hitung skor & status 3-tier dari jawaban yang SUDAH tervalidasi (P1-6, v1.7).
 * Mirror AssessmentCalculator.kt: total = Σ bobot×faktor (persen dari total
 * applicable → kebal N/A 3.4); SEHAT = inti penuh + total ≥90%; TIDAK = inti
 * <80% ATAU total <70%; KURANG = sisanya. Prioritas SEHAT→TIDAK→KURANG;
 * applicable=0 = TIDAK + invalid. "Tidak berlaku" dilewati dari pembagi.
 * ponytail: duplikasi aturan skor dgn Kotlin disengaja — Apps Script tak bisa
 *   import Kotlin; sinkron manual, ada test backend/test_scoring.js.
 */
var CORE_KEYS = ["air_bersih", "jamban", "spal", "tempat_sampah"];
var SEHAT_MIN_PERCENT = 90;
var KURANG_CORE_MIN_PERCENT = 80;
var KURANG_TOTAL_MIN_PERCENT = 70;

function computeServerSummary(answers) {
  var achieved = 0, applicable = 0, coreAch = 0, coreApp = 0;
  for (var key in SCORING_RULES) {
    var weight = SCORING_RULES[key];
    var s = String(answers[key]).trim();
    if (s === "Tidak berlaku") continue;
    var m = s.match(/^([abcd])/i);
    var factor = m ? ANSWER_FACTOR[m[1].toLowerCase()] : 0;
    applicable += weight;
    achieved += weight * factor;
    if (CORE_KEYS.indexOf(key) !== -1) {
      coreApp += weight;
      coreAch += weight * factor;
    }
  }
  if (applicable === 0) {
    return { total_achieved: 0, total_applicable: 0, is_healthy: false,
      status: "TIDAK SEHAT", invalid: true };
  }
  var pct = achieved / applicable * 100;
  var corePct = coreApp > 0 ? coreAch / coreApp * 100 : 0;
  var coreFull = coreApp > 0 && coreAch === coreApp;
  var status;
  if (coreFull && pct >= SEHAT_MIN_PERCENT) status = "SEHAT";
  else if (corePct < KURANG_CORE_MIN_PERCENT || pct < KURANG_TOTAL_MIN_PERCENT) status = "TIDAK SEHAT";
  else status = "KURANG SEHAT";
  return {
    total_achieved: achieved,
    total_applicable: applicable,
    is_healthy: status === "SEHAT",
    status: status
  };
}

function setupEnvironment() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName(CONFIG.SHEET_NAME);

  if (!sheet) {
    sheet = ss.insertSheet(CONFIG.SHEET_NAME);
  }

  var hasHeaders = sheet.getLastRow() > 1; // = header + minimal 1 baris data

  // Kalau sheet kosong (belum ada header), tulis skema 29 kolom saat ini.
  if (!hasHeaders) {
    if (sheet.getLastRow() > 0) {
      sheet.getRange(1, 1, sheet.getLastRow(), sheet.getLastColumn()).clearContent();
    }
    var headers = [
      "Audit ID", "Tanggal Sync", "Assessor", "Perusahaan / Kebun", "Nama Pemilik / Alamat Rumah",

      // Komponen Rumah (1-8)
      "1. Langit-langit", "2. Dinding", "3. Lantai", "4. Jendela Kamar",
      "5. Jendela RK", "6. Ventilasi", "7. Lubang Asap", "8. Pencahayaan",

      // Sarana Sanitasi (9-12)
      "9. Air Bersih", "10. Jamban", "11. SPAL", "12. Tempat Sampah",

      // Perilaku Penghuni (13-17)
      "13. Buka Jend. Kamar", "14. Buka Jend. RK", "15. Bersih Rumah",
      "16. Tinja Bayi", "17. Buang Sampah",

      // Ringkasan
      "Total Skor", "Status Health", "Catatan Field", "URL Foto Komponen Rumah", "URL Foto Sarana Sanitasi", "URL Foto Perilaku Penghuni",

      // Analisis AI (3 kolom)
      "Status Validasi AI", "Penjelasan AI", "Rekomendasi AI"
    ];

    if (sheet.getLastRow() > 0) sheet.clear();
    sheet.appendRow(headers);

    // Styling Header
    var headerRange = sheet.getRange(1, 1, 1, headers.length);
    headerRange.setFontWeight("bold");
    headerRange.setBackground("#1F4E79");
    headerRange.setFontColor("#FFFFFF");
    headerRange.setHorizontalAlignment("center");

    for (var h = 0; h < headers.length; h++) {
      var hc = headers[h].toString();
      if (hc.indexOf("URL Foto") === 0) {
        sheet.setColumnWidth(h + 1, 160); // Foto (IMAGE/SHEET image)
      }
    }
    sheet.setFrozenRows(1);
  } else {
    // Migrasi sheet lama (data sudah ada): pastikan kolom AI & foto lengkap.
    ensureAiColumns(sheet);
  }

  return sheet;
}

/** Append kolom yang hilang (foto & AI) ke header tanpa menghapus data lama. */
function ensureAiColumns(sheet) {
  if (!sheet) return;
  var lastCol = sheet.getLastColumn();
  if (!lastCol) lastCol = sheet.getMaxColumns();
  var headers = sheet.getRange(1, 1, 1, lastCol).getValues()[0].map(String);

  // v1.8: kolom Nama Pemilik / Alamat Rumah (sisip setelah Perusahaan)
  var houseHeader = "Nama Pemilik / Alamat Rumah";
  if (headers.indexOf(houseHeader) === -1) {
    // Sisip di kolom 5 (setelah Perusahaan / Kebun = col 4)
    sheet.insertColumnAfter(4);
    sheet.getRange(1, 5).setValue(houseHeader).setFontWeight("bold").setBackground("#1F4E79").setFontColor("#FFFFFF").setHorizontalAlignment("center");
    headers.splice(4, 0, houseHeader);
    lastCol++;
  }

  var required = [
    "URL Foto Komponen Rumah",
    "URL Foto Sarana Sanitasi",
    "URL Foto Perilaku Penghuni",
    "Status Validasi AI",
    "Penjelasan AI",
    "Rekomendasi AI"
  ];

  var toAdd = [];
  for (var i = 0; i < required.length; i++) {
    if (headers.indexOf(required[i]) === -1) {
      toAdd.push(required[i]);
    }
  }

  if (toAdd.length > 0) {
    var startCol = lastCol + 1;
    sheet.getRange(1, startCol, 1, toAdd.length).setValues([toAdd]);
    var headerRange = sheet.getRange(1, startCol, 1, toAdd.length);
    headerRange.setFontWeight("bold");
    headerRange.setBackground("#1F4E79");
    headerRange.setFontColor("#FFFFFF");
    headerRange.setHorizontalAlignment("center");
  }
}
