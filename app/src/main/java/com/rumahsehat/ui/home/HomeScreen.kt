package com.rumahsehat.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rumahsehat.ui.components.EmptyState
import com.rumahsehat.ui.theme.*
import java.util.Calendar

data class SavedAssessmentUi(
    val id: String,
    val companyName: String,
    val assessorName: String,
    val date: String,
    val syncStatus: SyncStatusUi
)

enum class SyncStatusUi { TERKIRIM, MENUNGGU_KIRIM }

@Composable
fun HomeScreen(
    pendingCount: Int,
    isSyncing: Boolean,
    savedAssessments: List<SavedAssessmentUi>,
    onStartAssessment: () -> Unit,
    onSyncNow: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAssessment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..10 -> "Selamat Pagi,"
            in 11..14 -> "Selamat Siang,"
            in 15..17 -> "Selamat Sore,"
            else -> "Selamat Malam,"
        }
    }
    val syncedCount = savedAssessments.count { it.syncStatus == SyncStatusUi.TERKIRIM }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(greeting, style = MaterialTheme.typography.bodyLarge, color = Primary)
                Text("Petugas", style = MaterialTheme.typography.headlineMedium, color = OnSurface)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Siap melakukan inspeksi kesehatan lingkungan hari ini?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
        }

        item {
            SyncBanner(pendingCount = pendingCount, isSyncing = isSyncing, onSyncNow = onSyncNow)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    onClick = onStartAssessment,
                    modifier = Modifier.weight(1f).height(150.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Primary)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(OnPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Assignment, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Mulai Inspeksi Baru", style = MaterialTheme.typography.titleMedium, color = OnPrimary, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text("Buka formulir penilaian", style = MaterialTheme.typography.bodyMedium, color = OnPrimary.copy(alpha = 0.9f))
                        }
                    }
                }
                Card(
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f).height(150.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    border = BorderStroke(0.5.dp, OutlineVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(SecondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.History, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Lihat Riwayat", style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text("Akses inspeksi sebelumnya", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                border = BorderStroke(0.5.dp, OutlineVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ringkasan", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("Terkirim", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                            Text("$syncedCount", style = MaterialTheme.typography.headlineMedium, color = Primary)
                        }
                        Column(
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                        ) {
                            Text("Menunggu Kirim", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                            Text("$pendingCount", style = MaterialTheme.typography.headlineMedium, color = Secondary)
                        }
                    }
                }
            }
        }

        item {
            Text("Penilaian Tersimpan", style = MaterialTheme.typography.titleMedium, color = OnSurface)
        }

        if (savedAssessments.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.History,
                    title = "Belum ada penilaian",
                    subtitle = "Riwayat penilaian akan muncul di sini setelah kamu menyelesaikan inspeksi pertama."
                )
            }
        } else {
            items(savedAssessments) { assessment ->
                AssessmentCard(assessment, onClick = { onOpenAssessment(assessment.id) })
            }
        }
    }
}

@Composable
private fun SyncBanner(pendingCount: Int, isSyncing: Boolean, onSyncNow: () -> Unit) {
    Surface(
        color = SurfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, OutlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(SecondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (isSyncing) {
                    val transition = rememberInfiniteTransition()
                    val angle by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing))
                    )
                    Icon(
                        Icons.Filled.Sync,
                        contentDescription = "Mengirim",
                        tint = Primary,
                        modifier = Modifier.size(20.dp).rotate(angle)
                    )
                } else if (pendingCount > 0) {
                    Icon(Icons.Filled.Sync, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = StatusSehat, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Status Sinkronisasi", style = MaterialTheme.typography.titleSmall, color = OnSurface)
                Text(
                    when {
                        isSyncing -> "Mengirim ke pusat data..."
                        pendingCount > 0 -> "$pendingCount data menunggu dikirim"
                        else -> "Semua data telah terkirim"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
            if (!isSyncing && pendingCount > 0) {
                TextButton(onClick = onSyncNow) {
                    Text("Kirim Sekarang", color = Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun AssessmentCard(item: SavedAssessmentUi, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(item.companyName, style = MaterialTheme.typography.titleMedium, color = OnSurface)
            Spacer(Modifier.height(2.dp))
            Text("${item.assessorName} · ${item.date}", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                val (label, color) = when (item.syncStatus) {
                    SyncStatusUi.TERKIRIM -> "Terkirim" to StatusSehat
                    SyncStatusUi.MENUNGGU_KIRIM -> "Menunggu Kirim" to StatusPending
                }
                Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            }
        }
    }
}