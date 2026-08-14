package com.rumahsehat.ui

import android.app.Application
import androidx.lifecycle.*
import com.rumahsehat.data.db.AppDatabase
import com.rumahsehat.data.model.Assessment
import com.rumahsehat.data.model.FormItem
import com.rumahsehat.data.model.FormItemsProvider
import com.rumahsehat.data.model.ScoreItem
import com.rumahsehat.data.repository.AssessmentRepository
import com.rumahsehat.domain.AssessmentCalculator
import com.rumahsehat.ui.inspection.AllFormQuestions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssessmentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AssessmentRepository
    val allAssessments: LiveData<List<Assessment>>

    private val _review = MutableLiveData<Pair<Assessment?, List<ScoreItem>>>()
    val review: LiveData<Pair<Assessment?, List<ScoreItem>>> get() = _review

    private val _formItems = FormItemsProvider.getFormItems()
    val formItems: List<FormItem> get() = _formItems
    private val weights = _formItems.associate { it.id to it.maxScore }
    private val photoPaths = mutableMapOf<String, String>()
    private val _photoPaths = MutableLiveData(emptyMap<String, String>())
    /** Status foto per section; observe oleh fragment untuk refresh hint. */
    val photoPathsLive: LiveData<Map<String, String>> get() = _photoPaths

    // Identitas dari halaman-0 (dipakai subtitle AppBar + saat simpan).
    var assessorName: String = ""
    var companyName: String = ""

    init {
        val dao = AppDatabase.getDatabase(application).assessmentDao()
        repository = AssessmentRepository(dao)
        allAssessments = repository.allAssessments.asLiveData()
    }

    fun getFormItemAt(index: Int): FormItem = _formItems[index]
    fun getFormItemsCount(): Int = _formItems.size

    fun isFormComplete(): Boolean =
        _formItems.all { !it.isApplicable || it.selectedOptionIndex >= 0 }

    fun missingItems(): List<FormItem> =
        _formItems.filter { it.isApplicable && it.selectedOptionIndex < 0 }

    fun markPhotoTaken(section: String, path: String) {
        photoPaths[section] = path
        _photoPaths.value = photoPaths.toMap()
    }

    fun isPhotoTaken(section: String): Boolean = photoPaths.containsKey(section)

    fun photoPath(section: String): String? = photoPaths[section]

    fun missingPhotos(): List<String> = photoKeys.filter { it !in photoPaths }

    fun loadReview(id: String) {
        viewModelScope.launch {
            _review.value = repository.getAssessmentById(id) to repository.getScoreItemsForAssessment(id)
        }
    }

    private val _isSyncing = MutableLiveData(false)
    val isSyncing: LiveData<Boolean> get() = _isSyncing

    private val _syncMessage = MutableLiveData<String?>()
    val syncMessage: LiveData<String?> get() = _syncMessage

    fun syncNow() {
        if (_isSyncing.value == true) return // cegah klik ganda
        viewModelScope.launch {
            _isSyncing.value = true
            val (sent, failed) = repository.syncPending()
            _isSyncing.value = false
            _syncMessage.value =
                if (failed == 0) "Sinkronisasi selesai. $sent data terkirim."
                else "Selesai: $sent terkirim, $failed gagal. Akan dicoba otomatis."
        }
    }

    fun saveAssessment(assessorName: String, company: String) {
        viewModelScope.launch {
            val scoreItems = _formItems.map {
                ScoreItem(
                    assessmentId = "",
                    itemId = it.id,
                    score = it.currentScore,
                    isApplicable = it.isApplicable,
                    optionIndex = it.selectedOptionIndex,
                    reason = it.reason
                )
            }

            val calcResult = AssessmentCalculator.calculate(scoreItems, weights)
            val assessmentId = "ASM-${System.currentTimeMillis()}"
            val photoPathsSnapshot = photoPaths.toMap()

            val assessment = Assessment(
                id = assessmentId,
                company = company,
                assessorId = assessorName,
                createdAt = System.currentTimeMillis(),
                totalAchieved = calcResult.totalAchieved,
                totalApplicable = calcResult.totalApplicable,
                percentage = calcResult.percentage,
                isHealthy = calcResult.isHealthy,
                syncStatus = "PENDING",
                photoPathsJson = photoPathsSnapshot.entries.joinToString(";") { "${it.key}=${it.value}" }
            )

            // Simpan harus selesai walau layar ditutup segera setelah tombol Simpan.
            // viewModelScope dibatalkan saat Activity finish; bungkus dengan NonCancellable.
            withContext(NonCancellable) {
                repository.insert(assessment, scoreItems.map { it.copy(assessmentId = assessmentId) })
                // Offline-first: gagal kirim = tetap PENDING, dicoba ulang oleh SyncWorker.
                repository.syncPending()
            }
        }
    }

    /** Versi untuk UI Compose: pilihan jawaban dipegang map id->index di screen. */
    fun saveAssessmentFromCompose(
        assessorName: String,
        company: String,
        selections: Map<String, Int>,
        notes: Map<String, String>
    ) {
        viewModelScope.launch {
            val formItems = FormItemsProvider.getFormItems()
            val scoreItems = formItems.map { item ->
                val optionIndex = selections[item.id] ?: -1
                ScoreItem(
                    assessmentId = "",
                    itemId = item.id,
                    score = if (optionIndex >= 0) item.scoreForOption(optionIndex) else 0,
                    isApplicable = true,
                    optionIndex = optionIndex,
                    reason = notes[item.id]
                )
            }

            val calcResult = AssessmentCalculator.calculate(scoreItems, weights)
            val assessmentId = "ASM-${System.currentTimeMillis()}"
            val photoPathsSnapshot = photoPaths.toMap()

            val assessment = Assessment(
                id = assessmentId,
                company = company,
                assessorId = assessorName,
                createdAt = System.currentTimeMillis(),
                totalAchieved = calcResult.totalAchieved,
                totalApplicable = calcResult.totalApplicable,
                percentage = calcResult.percentage,
                isHealthy = calcResult.isHealthy,
                syncStatus = "PENDING",
                photoPathsJson = photoPathsSnapshot.entries.joinToString(";") { "${it.key}=${it.value}" }
            )

            withContext(NonCancellable) {
                repository.insert(assessment, scoreItems.map { it.copy(assessmentId = assessmentId) })
                repository.syncPending()
            }
        }
    }

    /** Validasi untuk UI Compose: kembalikan list pesan masalah, kosong = siap simpan. */
    fun validationIssues(
        assessorName: String,
        company: String,
        selections: Map<String, Int>,
        photos: Map<String, String?>
    ): List<String> {
        val issues = mutableListOf<String>()
        val parts = mutableListOf<String>()
        if (assessorName.isBlank()) parts.add("Nama Petugas")
        if (company.isBlank()) parts.add("Asal Kader / Instansi")
        if (parts.isNotEmpty()) issues.add("Identitas Petugas belum lengkap: " + parts.joinToString(", "))

        val missingItems = AllFormQuestionIds.filter { selections[it] == null }
        if (missingItems.isNotEmpty()) {
            issues.add("Masih ada ${missingItems.size} soal yang belum dijawab")
        }

        val missingPhotos = photoKeys.filter { photos[it] == null }
        if (missingPhotos.isNotEmpty()) {
            issues.add("Masih ada ${missingPhotos.size} dari 3 foto yang belum diambil")
        }
        return issues
    }

    companion object {
        val AllFormQuestionIds = AllFormQuestions.map { it.id }

        // Nama bagian foto: Rumah, Sanitasi, Perilaku (dapur/SPAL)
        val photoKeys = listOf("house_front", "sanitation", "kitchen_spal")

        /** Konversi id item (1.1, 2.3, ...) ke kunci bagian foto. */
        fun photoKeyFor(itemId: String): String = when (itemId.substringBefore('.')) {
            "1" -> photoKeys[0]
            "2" -> photoKeys[1]
            else -> photoKeys[2]
        }
    }
}