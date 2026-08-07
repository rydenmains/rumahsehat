/**
 * ==============================================================================
 * HEALTHY HOME ASSESSMENT APP - BACKEND SCRIPT (Google Apps Script)
 * Version: 2.4.0-MVP-Sheets (17 Indikator + Endpoint Baca Data + 3 Foto per Assessment)
 *
 * - doPost   : menyimpan baris assessment dari aplikasi Android (17 kolom skor)
 * - doGet    : ?action=data -> membaca seluruh baris sheet sebagai JSON (dipakai
 *              dashboard web/laptop dan dashboard admin Android)
 * ==============================================================================
 */

var CONFIG = {
  SHEET_NAME: "Data Assessment",
  DRIVE_FOLDER_NAME: "Healthy Home Photos",
  CUSTOM_FOLDER_ID: "",
  // Token penulisan (dikirim app via payload.token). Ganti jika bocor.
  API_TOKEN: "rs_sehat_2026"
};

function doGet(e) {
  var action = e && e.parameter && e.parameter.action;
  if (action && action.toLowerCase() === "data") {
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

function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) {
      throw new Error("Payload request kosong atau tidak valid.");
    }

    var payload = JSON.parse(e.postData.contents);

    // Cek token penulisan: tolak request tanpa token yang benar.
    if (!payload.token || payload.token !== CONFIG.API_TOKEN) {
      return createJsonResponse({ status: "FORBIDDEN", message: "Token tidak valid." }, 403);
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = setupEnvironment();

    // --- PROSES UPLOAD FOTO KE GOOGLE DRIVE (3 SLOT) ---
    var photoUrls = ["-", "-", "-"];   // URL Foto 1/2/3
    var photoLengths = 0;              // jumlah slot foto terkirim

    var photos = payload.photos || {};
    var sectionKeys = ["1", "2", "3"];

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
          file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
          var base = "https://lh3.googleusercontent.com/d/" + file.getId();
          photoUrls[k] = '=IMAGE("' + base + '")';
          photoLengths++;
        } catch (e) {
          // abaikan foto gagal; tetap simpan data skor
        }
      }
    }

    // --- BACA SKOR 17 INDIKATOR DARI PAYLOAD ANDROID ---
    var s = payload.scores || {};

    // --- SUSUN BARIS DATA ( TOTAL 26 KOLOM ) ---
    var now = new Date();
    var formattedDate = Utilities.formatDate(now, ss.getSpreadsheetTimeZone(), "yyyy-MM-dd HH:mm:ss");
    var summary = payload.summary || {};
    var meta = payload.meta || {};

    var newRow = [
      payload.assessment_id || "-",
      formattedDate,
      meta.assessor_name || payload.assessor_name || "-",
      meta.company || payload.company || "-",

      // I. KOMPONEN RUMAH (8 Items)
      s.langit_langit !== undefined ? s.langit_langit : "-",
      s.dinding !== undefined ? s.dinding : "-",
      s.lantai !== undefined ? s.lantai : "-",
      s.jendela_kamar !== undefined ? s.jendela_kamar : "-",
      s.jendela_rk !== undefined ? s.jendela_rk : "-",
      s.ventilasi !== undefined ? s.ventilasi : "-",
      s.lubang_asap !== undefined ? s.lubang_asap : "-",
      s.pencahayaan !== undefined ? s.pencahayaan : "-",

      // II. SARANA SANITASI (4 Items)
      s.air_bersih !== undefined ? s.air_bersih : "-",
      s.jamban !== undefined ? s.jamban : "-",
      s.spal !== undefined ? s.spal : "-",
      s.tempat_sampah !== undefined ? s.tempat_sampah : "-",

      // III. PERILAKU PENGHUNI (5 Items)
      s.buka_jendela_kamar !== undefined ? s.buka_jendela_kamar : "-",
      s.buka_jendela_rk !== undefined ? s.buka_jendela_rk : "-",
      s.bersih_rumah !== undefined ? s.bersih_rumah : "-",
      s.buang_tinja_bayi !== undefined ? s.buang_tinja_bayi : "-",
      s.buang_sampah !== undefined ? s.buang_sampah : "-",

      // RINGKASAN & FOTO
      summary.total_achieved !== undefined ? summary.total_achieved : 0,
      summary.status || (summary.is_healthy ? "SEHAT" : "TIDAK SEHAT"),
      payload.notes || "-",
      photoUrls[0], photoUrls[1], photoUrls[2]
    ];

    sheet.appendRow(newRow);

    var lastRow = sheet.getLastRow();
    sheet.setRowHeight(lastRow, 80); // Tinggi baris untuk foto

    return createJsonResponse({
      status: "SUCCESS",
      message: "Data berhasil disimpan!",
      assessment_id: payload.assessment_id,
      photo_url: photoUrl
    }, 200);

  } catch (error) {
    return createJsonResponse({
      status: "ERROR",
      message: error.toString()
    }, 500);
  }
}

function createJsonResponse(data, statusCode) {
  return ContentService
    .createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
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

function setupEnvironment() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName(CONFIG.SHEET_NAME);

  if (!sheet) {
    sheet = ss.insertSheet(CONFIG.SHEET_NAME);
  }

  var hasHeaders = sheet.getLastRow() > 1; // = header + minimal 1 baris data

  // Kalau sheet kosong (belum ada header, atau cuma sisa header lama),
  // tulis ulang menjadi skema 26 kolom saat ini.
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
      "Total Skor", "Status Health", "Catatan Field", "URL Foto 1", "URL Foto 2", "URL Foto 3"
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
  }

  return sheet;
}