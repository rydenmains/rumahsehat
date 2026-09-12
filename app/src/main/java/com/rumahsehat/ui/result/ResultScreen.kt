package com.rumahsehat.ui.result

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rumahsehat.data.model.FormItemsProvider
import com.rumahsehat.data.model.ScoreItem
import com.rumahsehat.domain.HealthStatus
import com.rumahsehat.ui.AssessmentViewModel
import com.rumahsehat.ui.theme.*

private data class ResultStyle(
    val main: Color,
    val soft: Color,
    val icon: ImageVector,
    val title: String,
    val message: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: AssessmentViewModel,
    onBack: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val review by viewModel.review.observeAsState(null to emptyList<ScoreItem>())
    val assessment = review?.first
    val scoreItems = review?.second ?: emptyList()
    val formItems = remember { FormItemsProvider.getFormItems() }
    val formById = remember { formItems.associate { it.id to it } }

    val label = assessment?.status?.ifBlank { null }
        ?: if (assessment?.isHealthy == true) HealthStatus.SEHAT.label else HealthStatus.TIDAK_SEHAT.label
    val style = when (HealthStatus.fromLabel(label)) {
        HealthStatus.SEHAT -> ResultStyle(
            StatusSehat, Color(0xFFE2EFE3), Icons.Filled.Check,
            "Rumah Sehat", "Rumah Anda dalam kondisi baik dan layak huni."
        )
        HealthStatus.KURANG_SEHAT -> ResultStyle(
            StatusKurang, StatusKurangContainer, Icons.Filled.PriorityHigh,
            "Kurang Sehat", "Beberapa kriteria belum terpenuhi."
        )
        HealthStatus.TIDAK_SEHAT -> ResultStyle(
            StatusTidakSehat, Color(0xFFF9DEDC), Icons.Filled.Close,
            "Tidak Sehat", "Banyak kriteria belum terpenuhi."
        )
    }

    val achieved = assessment?.totalAchieved ?: 0
    val applicable = assessment?.totalApplicable?.takeIf { it > 0 } ?: 810
    val fulfilled = scoreItems.count { item ->
        val w = formById[item.itemId]?.maxScore ?: 0
        item.isApplicable && w > 0 && item.score >= w
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val iconScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "resultIconScale"
    )

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Hasil Penilaian", style = MaterialTheme.typography.headlineSmall) },
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(112.dp).scale(iconScale)
                                .clip(CircleShape).background(style.soft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = null,
                                tint = style.main,
                                modifier = Modifier.size(56.dp)
                            )
                            Icon(
                                style.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp).offset(y = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(style.title, style = MaterialTheme.typography.headlineSmall, color = OnSurface)
                        Spacer(Modifier.height(16.dp))
                        ScoreRing(achieved = achieved, max = applicable, color = style.main)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "$fulfilled dari ${formItems.size} indikator terpenuhi",
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnSurface
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = OutlineVariant)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            style.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                        if ((assessment?.totalApplicable ?: 0) == 0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Data belum lengkap — dilengkapi dulu sebelum dinilai akhir.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onOpenDetail,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("Lihat Detail Penilaian")
                        }
                    }
                }
            }

            item {
                Text(
                    "Rincian per kelompok",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            listOf(
                "I · KOMPONEN RUMAH" to formItems.filter { it.id.startsWith("1.") },
                "II · SARANA SANITASI" to formItems.filter { it.id.startsWith("2.") },
                "III · PERILAKU PENGHUNI" to formItems.filter { it.id.startsWith("3.") }
            ).forEach { (groupTitle, groupItems) ->
                item {
                    GroupBreakdown(
                        title = groupTitle,
                        items = groupItems,
                        scoresById = scoreItems.associateBy { it.itemId },
                        mainColor = style.main
                    )
                }
            }
        }
    }
}
