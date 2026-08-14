package com.rumahsehat.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import com.rumahsehat.data.model.FormItemsProvider
import com.rumahsehat.data.model.ScoreItem
import com.rumahsehat.ui.theme.Background
import com.rumahsehat.ui.theme.OnSurface
import com.rumahsehat.ui.theme.OnSurfaceVariant
import com.rumahsehat.ui.theme.Primary
import com.rumahsehat.ui.theme.RumahSehatTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReviewActivity : ComponentActivity() {
    private val viewModel: AssessmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val assessmentId = intent.getStringExtra(EXTRA_ID)
        if (assessmentId == null) {
            finish()
            return
        }
        viewModel.loadReview(assessmentId)
        enableEdgeToEdge()
        setContent {
            RumahSehatTheme {
                ReviewScreen(viewModel = viewModel, onBack = { finish() })
            }
        }
    }

    companion object {
        const val EXTRA_ID = "assessment_id"
        fun start(context: android.content.Context, assessmentId: String) {
            context.startActivity(Intent(context, ReviewActivity::class.java).putExtra(EXTRA_ID, assessmentId))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(viewModel: AssessmentViewModel, onBack: () -> Unit) {
    val review by viewModel.review.observeAsState(null to emptyList<ScoreItem>())
    val assessment = review?.first
    val scoreItems = review?.second ?: emptyList()
    val formItems = remember { FormItemsProvider.getFormItems() }
    val formById = remember { formItems.associate { it.id to it } }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Detail Penilaian", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (assessment != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(assessment.company, style = MaterialTheme.typography.titleMedium, color = OnSurface)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${assessment.assessorId} · ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(assessment.createdAt))}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Skor: ${assessment.totalAchieved} dari ${assessment.totalApplicable} (${"%.1f".format(assessment.percentage)}%)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Primary
                            )
                        }
                    }
                }
            }

            items(scoreItems) { item ->
                ScoreRow(item, formById[item.itemId]?.options?.getOrNull(item.optionIndex)?.label)
            }
        }
    }
}

@Composable
fun ScoreRow(item: ScoreItem, answerLabel: String?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Soal ${item.itemId}", style = MaterialTheme.typography.labelMedium, color = Primary)
                Text("${item.score}", style = MaterialTheme.typography.labelMedium, color = OnSurface)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                answerLabel ?: "Tidak berlaku",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface
            )
            if (!item.reason.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Catatan: ${item.reason}", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            }
        }
    }
}