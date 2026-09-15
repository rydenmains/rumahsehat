package com.rumahsehat.ui.inspection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.rumahsehat.ui.AssessmentViewModel
import com.rumahsehat.ui.components.FormBottomBar
import com.rumahsehat.ui.components.FormStepper
import com.rumahsehat.ui.components.IdentityStepCard
import com.rumahsehat.ui.components.PhotoUploadCard
import com.rumahsehat.ui.components.QuestionCard
import androidx.compose.ui.res.stringResource
import com.rumahsehat.R
import com.rumahsehat.ui.theme.Background
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionFormScreen(
    viewModel: AssessmentViewModel,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val questions = remember { AllFormQuestions }
    val total = questions.size
    var current by remember { mutableIntStateOf(0) }
    var showConfirm by remember { mutableStateOf(false) }
    var showIssues by remember { mutableStateOf(false) }
    var issuesMessage by remember { mutableStateOf("") }
    var petugasName by remember { mutableStateOf(viewModel.assessorName) }
    var instansi by remember { mutableStateOf(viewModel.companyName) }
    var houseName by remember { mutableStateOf(viewModel.houseName) }
    val selections = remember { mutableStateMapOf<String, Int>() }
    val notes = remember { mutableStateMapOf<String, String>() }
    val photos = remember { mutableStateMapOf<String, String?>() }
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var photoTarget by remember { mutableStateOf("house_front") }

    val isIdentity = current == 0
    val question = if (isIdentity) null else questions[current - 1]

    // v1.7: setelah simpan sukses → buka ResultActivity untuk assessment baru.
    // consumeLastSavedId = one-shot (QA-01): nilai lama tidak memicu Result saat form dibuka ulang.
    val lastSavedId by viewModel.lastSavedId.observeAsState()
    LaunchedEffect(lastSavedId) {
        lastSavedId?.let { id ->
            viewModel.consumeLastSavedId()
            com.rumahsehat.ui.result.ResultActivity.start(context, id)
            onFinish()
        }
    }
    val section = question?.let { q ->
        AllFormSections.firstOrNull { s -> s.questions.any { it.id == q.id } }
    }
    val isFirstQuestionOfSection = section != null && section.questions.first().id == question!!.id
    val isLast = current == total

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && pendingPhotoFile != null) {
            viewModel.markPhotoTaken(photoTarget, pendingPhotoFile!!.absolutePath)
            photos[photoTarget] = pendingPhotoFile!!.absolutePath
        }
    }

    // Galeri: salin pilihan ke file lokal agar preview & sync baca File(path) seperti hasil kamera.
    // Photo Picker = tanpa izin storage. Fallback GetContent untuk HP tanpa Photo Picker.
    fun useGalleryUri(uri: Uri) {
        try {
            val file = File(context.getExternalFilesDir(null), "IMG_GAL_${photoTarget}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            viewModel.markPhotoTaken(photoTarget, file.absolutePath)
            photos[photoTarget] = file.absolutePath
        } catch (_: Exception) { }
    }
    val pickGalleryFallback = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) useGalleryUri(uri)
    }
    val pickGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) useGalleryUri(uri)
    }

    fun pickFromGallery() {
        photoTarget = question?.let { q -> AssessmentViewModel.photoKeyFor(q.id) } ?: "house_front"
        try {
            pickGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (_: Exception) {
            pickGalleryFallback.launch("image/*")
        }
    }

    fun capturePhoto() {
        val sectionKey = question?.let { q ->
            AssessmentViewModel.photoKeyFor(q.id)
        } ?: "house_front"
        photoTarget = sectionKey
        val fileName = "IMG_${question?.id ?: "x"}_${System.currentTimeMillis()}.jpg"
        val storageDir = context.getExternalFilesDir(null)
        val file = File(storageDir, fileName)
        pendingPhotoFile = file
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        takePicture.launch(uri)
    }

    fun onFinishPressed() {
        val issues = viewModel.validationIssues(petugasName, instansi, selections, photos, houseName)
        if (issues.isNotEmpty()) {
            issuesMessage = issues.joinToString("\n\n")
            showIssues = true
            return
        }
        showConfirm = true
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.form_title), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            if (isIdentity) stringResource(R.string.identity_subtitle)
                            else "Tahap ${section!!.stepNumber} dari 3 - ${section.title}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        bottomBar = {
            FormBottomBar(
                onBack = { if (current == 0) onBack() else current-- },
                onNext = { if (isLast) onFinishPressed() else current++ },
                isFirstStep = current == 0,
                isLastStep = isLast
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                FormStepper(currentStep = section?.stepNumber ?: 1)
            }

            if (isIdentity) {
                item {
                    Text(
                        "Identitas Petugas",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalContentColor.current
                    )
                }
                item {
                    IdentityStepCard(
                        name = petugasName,
                        instansi = instansi,
                        houseName = houseName,
                        onNameChange = { petugasName = it; viewModel.assessorName = it },
                        onInstansiChange = { instansi = it; viewModel.companyName = it },
                        onHouseChange = { houseName = it; viewModel.houseName = it }
                    )
                }
            } else {
                item {
                    Text(
                        "Soal ${current} dari $total",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalContentColor.current
                    )
                }
                if (isFirstQuestionOfSection) {
                    item {
                        PhotoUploadCard(
                            title = section!!.photoTitle,
                            subtitle = section.photoSubtitle,
                            photoUri = photos[section.photoCategoryId],
                            onCaptureClick = { capturePhoto() },
                            onGalleryClick = { pickFromGallery() }
                        )
                    }
                }
                item {
                    QuestionCard(
                        number = current,
                        title = question!!.title,
                        subtitle = question.subtitle,
                        options = question.options,
                        selectedIndex = selections[question.id],
                        note = notes[question.id].orEmpty(),
                        onSelect = { index -> selections[question.id] = index },
                        onNoteChange = { text -> notes[question.id] = text }
                    )
                }
            }
        }
    }

    if (showIssues) {
        AlertDialog(
            onDismissRequest = { showIssues = false },
            title = { Text(stringResource(R.string.issues_title)) },
            text = { Text(issuesMessage) },
            confirmButton = {
                TextButton(onClick = { showIssues = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    if (showConfirm) {
        val answered = selections.size
        val photoCount = photos.values.count { it != null }
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.confirm_save_title)) },
            text = {
                Text(
                    stringResource(R.string.confirm_identity_fmt, petugasName.ifBlank { "-" }, instansi.ifBlank { "-" }, houseName.ifBlank { "-" }) +
                        "\n\n" + stringResource(R.string.confirm_save_message, answered, total, photoCount, 3)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    viewModel.saveAssessmentFromCompose(petugasName, instansi, selections, notes, houseName)
                }) {
                    Text(stringResource(R.string.save_assessment))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(R.string.back))
                }
            }
        )
    }
}