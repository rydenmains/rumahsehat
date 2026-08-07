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

    init {
        val dao = AppDatabase.getDatabase(application).assessmentDao()
        repository = AssessmentRepository(dao)
        allAssessments = repository.allAssessments.asLiveData()
    }

    fun getFormItemAt(index: Int): FormItem = _formItems[index]
    fun getFormItemsCount(): Int = _formItems.size

    fun isFormComplete(): Boolean =
        _formItems.all { !it.isApplicable || it.currentScore > 0 }

    fun missingItems(): List<FormItem> =
        _formItems.filter { it.isApplicable && it.currentScore <= 0 }

    fun markPhotoTaken(section: String, path: String) {
        photoPaths[section] = path
    }

    fun isPhotoTaken(section: String): Boolean = photoPaths.containsKey(section)

    fun missingPhotos(): List<String> = photoKeys.filter { it !in photoPaths }

    companion object {
        // Nama bagian foto: Rumah, Sanitasi, Perilaku (dapur/SPAL)
        val photoKeys = listOf("house_front", "sanitation", "kitchen_spal")

        /** Konversi id item (1.1, 2.3, ...) ke kunci bagian foto. */
        fun photoKeyFor(itemId: String): String = when (itemId.substringBefore('.')) {
            "1" -> photoKeys[0]
            "2" -> photoKeys[1]
            else -> photoKeys[2]
        }
    }

    fun loadReview(id: String) {
        viewModelScope.launch {
            _review.value = repository.getAssessmentById(id) to repository.getScoreItemsForAssessment(id)
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
}