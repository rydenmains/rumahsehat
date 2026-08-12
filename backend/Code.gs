/**
 * ==============================================================================
 * HEALTHY HOME ASSESSMENT APP - BACKEND SCRIPT (Google Apps Script)
 * Version: 3.0.0 (17 Indikator berbasis jawaban kata + Deferred AI)
 *
 * - doPost   : menyimpan baris assessment dari aplikasi Android (17 jawaban kata)
 * - doGet    : ?action=data -> membaca seluruh baris sheet sebagai JSON
 * - AI       : processPendingAi() dipanggil trigger tiap 10 menit → Gemini
 *              menganalisis 3 foto + jawaban, menulis 2 kolom hasil.
 *
 * TIDAK ADA kalkulator skor di backend. Status SEHAT/TIDAK ditentukan dari:
 *   1) Jawaban indikator (pilihan kata a/b/c)  → kolom 5..21
 *   2) Hasil analisis foto oleh Gemini         → kolom 28..29
 * Total Skor (kolom 22) dikirim dari Android (summary.total_achieved).
 * ==============================================================================
 */

var CONFIG = {
  SHEET_NAME: "Data Assessment",
  DRIVE_FOLDER_NAME: "Healthy Home Photos",
  CUSTOM_FOLDER_ID: "",
  // Token penulisan (dikirim app via payload.token). Dibaca dari Script Properties
  // "API_TOKEN" — TIDAK ada fallback hardcoded. Jika kosong, semua tulis/baca ditolak.
  API_TOKEN: PropertiesService.getScriptProperties().getProperty("API_TOKEN") || "",
  // Model visi GRATIS via OpenRouter (suffix :free = $0, tanpa kartu kredit).
  // Kuota: 50 request/hari (naik ke 1000 jika akun pernah isi kredit $10 sekali).
  // Terverifikasi support foto. Ganti ke model berbayar (mis. google/gemini-2.5-flash) hanya jika saldo ada.
  GEMINI_MODEL: "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free",
  // Jumlah baris maksimum diproses per panggilan processPendingAi() (limit runtime).
  AI_BATCH_SIZE: 5,
  // Batas request tulis per menit (perlindungan kuota/serangan). 429 bila lewat.
  RATE_LIMIT_PER_MINUTE: 200,
  // Ukuran payload maksimum (foto terkompresi ~2MB; sisakan ruang). 413 bila lewat.
  MAX_PAYLOAD_BYTES: 5 * 1024 * 1024
};

/** Key OpenRouter diambil dari Script Properties — JANGAN hardcode.
 *  Buat di https://openrouter.ai/keys lalu simpan sebagai script property "OPENROUTER_API_KEY".
 */
function getGeminiKey() {
  return PropertiesService.getScriptProperties().getProperty("OPENROUTER_API_KEY");
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

/** Cek konfigurasi: apakah API_TOKEN sudah ter-set. Jalankan untuk verifikasi. */
function verifyConfig() {
  var t = PropertiesService.getScriptProperties().getProperty("API_TOKEN");
  var k = PropertiesService.getScriptProperties().getProperty("OPENROUTER_API_KEY");
  var msg = "API_TOKEN ter-set: " + !!t + (t ? " (panjang " + t.length + ")" : "") + " | OPENROUTER_API_KEY ter-set: " + !!k;
  Logger.log(msg);
  logToSheet("INFO", msg);
  return { api_token_set: !!t, openrouter_key_set: !!k };
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

    // --- BACA 17 JAWABAN INDIKATOR (kata) DARI PAYLOAD ANDROID ---
    var a = payload.answers || payload.scores || {};

    var now = new Date();
    var formattedDate = Utilities.formatDate(now, ss.getSpreadsheetTimeZone(), "yyyy-MM-dd HH:mm:ss");
    var summary = payload.summary || {};
    var meta = payload.meta || {};

    var newRow = [
      payload.assessment_id || "-",
      formattedDate,
      meta.assessor_name || payload.assessor_name || "-",
      meta.company || payload.company || "-",

      // I. KOMPONEN RUMAH (8 Items) — jawaban kata
      a.langit_langit !== undefined ? a.langit_langit : "-",
      a.dinding !== undefined ? a.dinding : "-",
      a.lantai !== undefined ? a.lantai : "-",
      a.jendela_kamar !== undefined ? a.jendela_kamar : "-",
      a.jendela_rk !== undefined ? a.jendela_rk : "-",
      a.ventilasi !== undefined ? a.ventilasi : "-",
      a.lubang_asap !== undefined ? a.lubang_asap : "-",
      a.pencahayaan !== undefined ? a.pencahayaan : "-",

      // II. SARANA SANITASI (4 Items)
      a.air_bersih !== undefined ? a.air_bersih : "-",
      a.jamban !== undefined ? a.jamban : "-",
      a.spal !== undefined ? a.spal : "-",
      a.tempat_sampah !== undefined ? a.tempat_sampah : "-",

      // III. PERILAKU PENGHUNI (5 Items)
      a.buka_jendela_kamar !== undefined ? a.buka_jendela_kamar : "-",
      a.buka_jendela_rk !== undefined ? a.buka_jendela_rk : "-",
      a.bersih_rumah !== undefined ? a.bersih_rumah : "-",
      a.buang_tinja_bayi !== undefined ? a.buang_tinja_bayi : "-",
      a.buang_sampah !== undefined ? a.buang_sampah : "-",

      // RINGKASAN & FOTO
      summary.total_achieved !== undefined ? summary.total_achieved : "",
      summary.status || (summary.is_healthy ? "SEHAT" : "TIDAK SEHAT"),
      payload.notes || "-",
      photoUrls[0], photoUrls[1], photoUrls[2],

      // ANALISIS AI (2 kolom) — diisi belakangan oleh processPendingAi()
      "", ""
    ];

    sheet.appendRow(newRow);

    var lastRow = sheet.getLastRow();
    sheet.setRowHeight(lastRow, 80); // Tinggi baris untuk foto

    logToSheet("INFO", "Data baru tersimpan: " + (payload.assessment_id || "tanpa-id") + " | " + formattedDate);

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
        photos[photoCols[key].key] = Utilities.base64Encode(blob.getBytes());
      } catch (e) { logToSheet("ERROR", "Foto gagal dibaca (" + photoCols[key].key + "): " + e); }
    }

    var result = analyzeAssessmentWithGemini(photos, {
      answers: rowAnswers(data[r], headers),
      summary: { total_achieved: data[r][headers.indexOf("Total Skor")] },
      is_healthy: String(data[r][headers.indexOf("Status Health")]).indexOf("TIDAK") === -1,
      status: String(data[r][headers.indexOf("Status Health")] || "SEHAT")
    });

    sheet.getRange(r + 1, colStatus).setValue(result.flag);
    if (result.explanation) {
      if (colExplanation > 0) sheet.getRange(r + 1, colExplanation).setValue(result.explanation);
    }
    sheet.getRange(r + 1, colRecommendation).setValue(result.recommendation);
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
 * Analisis 3 foto + jawaban kata OpenRouter (model vision).
 * Gunakan SEMUA photo key (house_front, sanitation, kitchen_spal).
 * Output konsisten: { is_valid, flag, recommendation }.
 * Fallback bila key kosong / foto tak lengkap / request gagal → data tetap tersimpan.
 */
function analyzeAssessmentWithGemini(photos, assessment) {
  var fallback = function (flag, recommendation) {
    return { is_valid: true, flag: flag, recommendation: recommendation };
  };

  var apiKey = getGeminiKey();
  if (!apiKey) {
    return fallback("Analisis AI dilewati", "Key OpenRouter belum diisi di Script Properties (OPENROUTER_API_KEY).");
  }

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
    answersText += key + "=" + answers[key] + "; ";
  });

  var prompt = [
    "Kamu adalah asisten Dinas Kesehatan untuk validasi Rumah Sehat.",
    "Berikut 3 foto kondisi rumah (depan, sanitasi, dapur/SPAL).",
    "Jawaban petugas (indikator): " + answersText,
    "Status sementara: " + assessment.status + " (is_healthy=" + assessment.is_healthy + ").",
    "",
    "Nilai dari jawaban + foto: apakah rumah ini SEHAT?",
    "PENTING - Lakukan langkah ini:",
    "1. Analisis SETIAP foto satu per satu. Tulis apa yang benar-benar terlihat di foto",
    "   (kondisi plafon, dinding, lantai, jendela, ventilasi, jamban, SPAL, tempat sampah).",
    "   Bila foto buram / sudut tidak jelas / tidak menampilkan ruangan yang dimaksud,",
    "   tuliskan jelas bahwa jenis foto tidak bisa dipastikan (mis. 'foto tidak jelas, tidak bisa dipastikan').",
    "2. Gunakan jawaban petugas sebagai sumber utama; foto sebagai pendukung.",
    "   Jangan menurunkan penilaian hanya karena foto buram bila jawaban petugas lengkap.",
    "3. Putuskan flag akhir (SEHAT / TIDAK SEHAT / PERLU PERBAIKAN) dari gabungan kedua sumber.",
    "",
    "Jawab JSON HANYA dengan format:",
    '{ "is_valid": true/false, "flag": "SEHAT"|"TIDAK SEHAT"|"PERLU PERBAIKAN",',
    '  "per_photo": ["deskripsi foto 1", "deskripsi foto 2", "deskripsi foto 3"],',
    '  "recommendation": "rekomendasi singkat dalam 1-2 kalimat (Indonesia)" }',
    "Jika foto buram/tidak jelas, is_valid=false."
  ].join("\n");

  content.unshift({ type: "text", text: prompt });

  var requestBody = {
    model: CONFIG.GEMINI_MODEL,
    messages: [{ role: "user", content: content }],
    temperature: 0, // deterministik: hindari flag berubah-ubah antar run
    max_tokens: 800
  };

  // JSON Mode bikin banyak model :free di-400 (param tidak didukung) → coba dulu, retry tanpa.
  requestBody.response_format = { type: "json_object" };
  var response = postToOpenRouter(apiKey, requestBody);
  if (response === null) {
    // 400 karena response_format tidak didukung → coba sekali lagi tanpa JSON Mode.
    delete requestBody.response_format;
    response = postToOpenRouter(apiKey, requestBody);
  }
  if (response === null) {
    return fallback("Analisis AI dilewati", "Error OpenRouter: request gagal (lihat Execution log).");
  }
  if (response.error) {
    return fallback("Analisis AI dilewati", "Error OpenRouter: " + response.error.message);
  }
  var text = response.choices && response.choices[0]
    && response.choices[0].message && response.choices[0].message.content;
  if (!text) {
    return fallback("Analisis AI dilewati", "Respons OpenRouter kosong.");
  }
  var parsed = parseAiJson(text);
  if (!parsed) {
    Logger.log("JSON tidak ter-parse: " + String(text).substring(0, 300));
    logToSheet("ERROR", "JSON tidak ter-parse: " + String(text).substring(0, 300));
    return fallback("Analisis AI dilewati", "Respons AI bukan JSON valid: " + String(text).substring(0, 120));
  }
  var flag = String(parsed.flag || assessment.status || "SEHAT").toUpperCase();
  var normalizedFlag = "PERLU PERBAIKAN";
  if (flag.indexOf("TIDAK") !== -1) normalizedFlag = "TIDAK SEHAT";
  else if (flag.indexOf("SEHAT") !== -1) normalizedFlag = "SEHAT";
  var perPhoto = [];
  if (Array.isArray(parsed.per_photo)) {
    perPhoto = parsed.per_photo.map(function (p) { return String(p); });
  }
  var explanation = [
    "Dasar penilaian (gabungan jawaban + foto):",
    "Foto 1 (depan rumah): " + (perPhoto[0] || "-"),
    "Foto 2 (sanitasi): " + (perPhoto[1] || "-"),
    "Foto 3 (dapur/SPAL): " + (perPhoto[2] || "-")
  ].join("\n");
  return {
    is_valid: !!parsed.is_valid,
    flag: normalizedFlag,
    explanation: explanation,
    recommendation: String(parsed.recommendation || "-")
  };
}

/** Kirim ke OpenRouter. Return parsed JSON body, atau null bila gagal/jaringan. */
function postToOpenRouter(apiKey, requestBody) {
  try {
    var response = UrlFetchApp.fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "post",
      headers: { Authorization: "Bearer " + apiKey },
      contentType: "application/json",
      payload: JSON.stringify(requestBody),
      muteHttpExceptions: true,
      timeoutSeconds: 60
    });
    return JSON.parse(response.getContentText());
  } catch (error) {
    logToSheet("ERROR", "postToOpenRouter gagal: " + error.toString());
    return null;
  }
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

/** Parse JSON dari teks model yang sering dibungkus teks/fence ```json```. */
function parseAiJson(text) {
  var source = String(text || "");
  source = source.replace(/```(?:json)?/gi, "").trim();
  var start = source.indexOf("{");
  var end = source.lastIndexOf("}");
  if (start !== -1 && end > start) {
    source = source.substring(start, end + 1);
  }
  try {
    return JSON.parse(source);
  } catch (e) { return null; }
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
      "Audit ID", "Tanggal Sync", "Assessor", "Perusahaan / Kebun",

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
  var lastCol = sheet.getLastColumn();
  var headers = sheet.getRange(1, 1, 1, lastCol).getValues()[0].map(String);

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
