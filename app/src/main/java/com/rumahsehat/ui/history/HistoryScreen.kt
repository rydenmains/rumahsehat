package com.rumahsehat.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rumahsehat.ui.components.EmptyState
import com.rumahsehat.ui.home.AssessmentCard
import com.rumahsehat.ui.home.SavedAssessmentUi
import com.rumahsehat.ui.theme.Background
import com.rumahsehat.ui.theme.OnSurface
import com.rumahsehat.ui.theme.OnSurfaceVariant

@Composable
fun HistoryScreen(
    assessments: List<SavedAssessmentUi>,
    onOpenAssessment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(Background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Penilaian Tersimpan", style = MaterialTheme.typography.headlineSmall, color = OnSurface)
                Spacer(Modifier.height(2.dp))
                Text("Rekap penilaian terdahulu", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            }
        }

        if (assessments.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.History,
                    title = "Belum ada penilaian",
                    subtitle = "Riwayat penilaian akan muncul di sini setelah kamu menyelesaikan inspeksi pertama."
                )
            }
        } else {
            items(assessments) { assessment ->
                AssessmentCard(assessment, onClick = { onOpenAssessment(assessment.id) })
            }
        }
    }
}
