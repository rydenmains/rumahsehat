package com.rumahsehat.data.remote

import com.rumahsehat.BuildConfig
import com.rumahsehat.data.dao.AssessmentDao
import com.rumahsehat.data.model.Assessment
import com.rumahsehat.data.model.FormItemsProvider
import com.rumahsehat.data.model.ScoreItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/*
 * Kirim hasil penilaian ke backend Google Apps Script.
 * Offline-first: gagal = tetap PENDING di lokal, dicoba ulang oleh SyncWorker.
 */
object AssessmentSync {
    const val SERVER_URL =
        "https://script.google.com/macros/s/AKfycbyVewbcY8D6NSc7ui9GwjcG9cLYr7Ee4LLzsrb4020-/dev"

    suspend fun push(dao: AssessmentDao, assessment: Assessment, items: List<ScoreItem>): Boolean =
        withContext(Dispatchers.IO) {
            val photos = buildPhotos(assessment)
            val body = buildPayload(assessment, items, photos).toString()

            // Apps Script balas 302 ke googleusercontent; ikuti redirect dengan
            // GET (auto oleh HttpURLConnection) agar dapat 2xx. JANGAN re-POST:
            // payload jadi duplikat & status tertahan PENDING walau data sudah masuk.
            // Catatan: ContentService GAS selalu HTTP 200, jadi keberhasilan
            // ditentukan dari body JSON (status == "SUCCESS"), bukan hanya kode HTTP.
            val synced = try {
                val conn = URL(SERVER_URL).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 60_000
                conn.readTimeout = 60_000
                conn.setRequestProperty("Content-Type", "text/plain;charset=UTF-8")
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val synced = if (conn.responseCode in 200..299) {
                    val responseBody = runCatching {
                        conn.inputStream.bufferedReader().use { it.readText() }
                    }.getOrDefault("")
                    try {
                        JSONObject(responseBody).optString("status") == "SUCCESS"
                    } catch (_: Exception) {
                        false
                    }
                } else false
                conn.disconnect()
                synced
            } catch (_: Exception) {
                false
            }

            // Gagal = jangan sentuh DB; status tetap PENDING dan dicoba ulang SyncWorker.
            if (synced) dao.updateSyncStatus(assessment.id, "SYNCED")
            synced
        }

    /** peta itemId (1.1, 2.3, ...) -> kunci kolom skor di Apps Script. */
    private val scoreKeys = mapOf(
        "1.1" to "langit_langit", "1.2" to "dinding", "1.3" to "lantai",
        "1.4" to "jendela_kamar", "1.5" to "jendela_rk", "1.6" to "ventilasi",
        "1.7" to "lubang_asap", "1.8" to "pencahayaan",
        "2.1" to "air_bersih", "2.2" to "jamban", "2.3" to "spal",
        "2.4" to "tempat_sampah",
        "3.1" to "buka_jendela_kamar", "3.2" to "buka_jendela_rk",
        "3.3" to "bersih_rumah", "3.4" to "buang_tinja_bayi",
        "3.5" to "buang_sampah"
    )

    private fun buildPhotos(assessment: Assessment): Map<String, String> {
        val paths = assessment.photoPathsJson
            ?.split(";")
            ?.filter { it.contains('=') }
            ?.associate { it.substringBefore('=') to it.substringAfter('=') }
            .orEmpty()
        return paths.mapNotNull { (section, path) ->
            try {
                val file = java.io.File(path)
                if (file.exists()) section to android.util.Base64.encodeToString(
                    compressPhoto(file), android.util.Base64.NO_WRAP
                )
                else null
            } catch (_: Exception) { null }
        }.toMap()
    }

    /** Resize/compress foto ke ≤1080px & JPEG ≤500KB sebelum dikirim. */
    private fun compressPhoto(file: java.io.File): ByteArray = try {
        val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            ?: return file.readBytes()
        val maxDim = 1080
        val scale = Math.min(1f, maxDim.toFloat() / Math.max(bitmap.width, bitmap.height))
        val scaled = android.graphics.Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== bitmap) bitmap.recycle()
        val maxSizeBytes = 500 * 1024
        var quality = 80
        val out = java.io.ByteArrayOutputStream()
        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
        var bytes = out.toByteArray()
        var currentQuality = quality
        // Turunkan kualitas bertahap sampai ≤500KB sebelum dikirim.
        while (bytes.size > maxSizeBytes && currentQuality > 40) {
            out.reset()
            currentQuality -= 10
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, currentQuality, out)
            bytes = out.toByteArray()
        }
        bytes
    } catch (_: Exception) {
        file.readBytes()
    }

    /** Label opsi terpilih ("c. Ada, leher angsa, ada tutup, septic tank"), atau "Tidak berlaku". */
    private fun answerLabel(item: ScoreItem): String {
        if (!item.isApplicable) return "Tidak berlaku"
        val formItem = FormItemsProvider.getFormItems().firstOrNull { it.id == item.itemId } ?: return "-"
        val option = formItem.options.getOrNull(item.optionIndex)
            ?: formItem.options.firstOrNull { formItem.scoreForOption(formItem.options.indexOf(it)) == item.score }
        return if (option != null) "${option.letter}. ${option.label}" else "-"
    }

    /** Gabung semua catatan petugas jadi satu cell (kolom "Catatan Field").
     *  Contoh: "1.3: Lantai retak | 2.2: Jamban bau". */
    private fun buildNotes(items: List<ScoreItem>): String =
        items.filter { !it.reason.isNullOrBlank() }
            .joinToString(" | ") { "${it.itemId}: ${it.reason}" }

    private fun buildPayload(assessment: Assessment, items: List<ScoreItem>, photos: Map<String, String>) = JSONObject().apply {
        // Bentuk payload sesuai skema backend Google Apps Script (Code.gs).
        put("assessment_id", assessment.id)
        put("notes", buildNotes(items))
        put("token", BuildConfig.API_TOKEN)

        put("meta", JSONObject().apply {
            put("assessor_name", assessment.assessorId)
            put("company", assessment.company)
        })

        put("answers", JSONObject().apply {
            items.forEach { item ->
                scoreKeys[item.itemId]?.let { key ->
                    put(key, answerLabel(item))
                }
            }
        })

        put("summary", JSONObject().apply {
            put("total_achieved", assessment.totalAchieved)
            put("is_healthy", assessment.isHealthy)
            put("status", if (assessment.isHealthy) "SEHAT" else "TIDAK SEHAT")
        })

        put("photos", JSONObject().apply {
            photos.forEach { (section, base64) ->
                put(section, base64)
            }
        })
    }
}