package com.rumahsehat.data.remote

import com.rumahsehat.data.dao.AssessmentDao
import com.rumahsehat.data.model.Assessment
import com.rumahsehat.data.model.ScoreItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/*
 * Kirim hasil penilaian ke backend Google Apps Script.
 * Offline-first: gagal = tetap PENDING di lokal, dicoba ulang oleh SyncWorker.
 */
object AssessmentSync {
    const val SERVER_URL =
        "https://script.google.com/macros/s/AKfycbxWtEzcBaHI7MeMH_fdgV-6FkhI1QLDmlt7HAFRnokuon3wPo1kewXQPASK0nvHJp7ATQ/exec"

    suspend fun push(dao: AssessmentDao, assessment: Assessment, items: List<ScoreItem>): Boolean =
        withContext(Dispatchers.IO) {
            val photos = buildPhotos(assessment)
            val body = buildPayload(assessment, items, photos).toString()
            var url = SERVER_URL

            // Apps Script balas 302 ke domain googleusercontent; ikuti manual agar POST tetap POST.
            repeat(5) {
                val conn = URL(url).openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = "POST"
                    conn.instanceFollowRedirects = false
                    conn.connectTimeout = 15_000
                    conn.readTimeout = 15_000
                    conn.setRequestProperty("Content-Type", "text/plain;charset=UTF-8")
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                    val code = conn.responseCode
                    return@withContext when {
                        code in 200..299 -> {
                            dao.updateSyncStatus(assessment.id, "SYNCED")
                            true
                        }
                        code == 302 || code == 303 || code == 307 || code == 308 -> {
                            val location = conn.getHeaderField("Location")
                            if (location.isNullOrBlank()) false
                            else {
                                url = if (location.startsWith("http")) location
                                else "https://script.google.com$location"
                                false
                            }
                        }
                        else -> false
                    }
                } catch (_: Exception) {
                    return@withContext false
                } finally {
                    conn.disconnect()
                }
            }
            false
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

    /** Ambil seluruh baris assessment dari Google Sheet (dipakai dashboard admin). */
    suspend fun fetchCloudAssessments(): List<Assessment> = withContext(Dispatchers.IO) {
        val url = URL("$SERVER_URL?action=data")
        try {
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)   // Apps Script redirects; instanceFollowRedirects menangani 302 GET.
            val rows = json.optJSONArray("rows") ?: JSONArray()
            buildList {
                for (i in 0 until rows.length()) {
                    val r = rows.getJSONObject(i)
                    add(Assessment(
                        id = r.optString("Audit ID", ""),
                        company = r.optString("Perusahaan / Kebun", ""),
                        assessorId = r.optString("Assessor", ""),
                        createdAt = parseDate(r.optString("Tanggal Sync", "")),
                        totalAchieved = r.optInt("Total Skor", 0),
                        isHealthy = r.optString("Status Health", "").contains("SEHAT") &&
                            !r.optString("Status Health", "").contains("TIDAK"),
                        syncStatus = "SYNCED"
                    ))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseDate(raw: String): Long {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                .parse(raw)?.time ?: 0L
        } catch (_: Exception) { 0L }
    }

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
                    file.readBytes(), android.util.Base64.NO_WRAP
                )
                else null
            } catch (_: Exception) { null }
        }.toMap()
    }

    private fun buildPayload(assessment: Assessment, items: List<ScoreItem>, photos: Map<String, String>) = JSONObject().apply {
        // Bentuk payload sesuai skema backend Google Apps Script (Code.gs).
        put("assessment_id", assessment.id)
        put("notes", items.firstNotNullOfOrNull { it.reason } ?: "")
        put("token", "rs_sehat_2026")

        put("meta", JSONObject().apply {
            put("assessor_name", assessment.assessorId)
            put("company", assessment.company)
        })

        put("scores", JSONObject().apply {
            items.forEach { it ->
                scoreKeys[it.itemId]?.let { key -> put(key, it.score) }
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