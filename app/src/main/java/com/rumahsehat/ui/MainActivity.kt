package com.rumahsehat.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import com.rumahsehat.data.model.Assessment
import com.rumahsehat.ui.components.RSBottomNavBar
import com.rumahsehat.ui.components.RSTab
import com.rumahsehat.ui.home.HomeScreen
import com.rumahsehat.ui.home.SavedAssessmentUi
import com.rumahsehat.ui.home.SyncStatusUi
import com.rumahsehat.ui.history.HistoryScreen
import com.rumahsehat.ui.inspection.InspectionFormScreen
import com.rumahsehat.ui.theme.Background
import com.rumahsehat.ui.theme.OnPrimary
import com.rumahsehat.ui.theme.OnSurfaceVariant
import com.rumahsehat.ui.theme.Primary
import com.rumahsehat.ui.theme.RumahSehatTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: AssessmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        setContent {
            RumahSehatTheme {
                SplashGate {
                    AppShell(viewModel)
                }
            }
        }
    }
}

@Composable
fun SplashGate(content: @Composable () -> Unit) {
    var splashDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1600); splashDone = true }
    Crossfade(targetState = splashDone, animationSpec = tween(500)) { done ->
        if (done) content() else SplashScreen()
    }
}

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "splashScale"
    )
    Box(
        modifier = modifier.fillMaxSize().background(Color(0xFFE3F2EA)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible, enter = fadeIn(tween(500))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(scale)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(52.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text("Rumah Sehat", style = MaterialTheme.typography.headlineMedium, color = Primary)
                Spacer(Modifier.height(48.dp))
                CircularProgressIndicator(
                    color = Primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("Menyiapkan...", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
            }
        }
    }
}

@Composable
fun AppShell(viewModel: AssessmentViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentTab by remember { mutableStateOf(RSTab.BERANDA) }
    val assessments by viewModel.allAssessments.observeAsState(emptyList())
    val isSyncing by viewModel.isSyncing.observeAsState(false)

    val pendingCount = assessments.count { it.syncStatus != "SYNCED" }
    val saved = assessments.map { it.toSavedAssessmentUi() }

    val onOpenAssessment: (String) -> Unit = { id ->
        context.startActivity(Intent(context, ReviewActivity::class.java).putExtra(ReviewActivity.EXTRA_ID, id))
    }

    if (currentTab == RSTab.INSPEKSI) {
        InspectionFormScreen(
            viewModel = viewModel,
            onBack = { currentTab = RSTab.BERANDA },
            onFinish = { currentTab = RSTab.RIWAYAT },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            RSBottomNavBar(current = currentTab, onSelect = { currentTab = it })
        }
    ) { padding ->
        Crossfade(targetState = currentTab, animationSpec = tween(250), label = "tab") { tab ->
            when (tab) {
                RSTab.BERANDA -> HomeScreen(
                    pendingCount = pendingCount,
                    isSyncing = isSyncing,
                    savedAssessments = saved,
                    onStartAssessment = { currentTab = RSTab.INSPEKSI },
                    onSyncNow = { viewModel.syncNow() },
                    onOpenHistory = { currentTab = RSTab.RIWAYAT },
                    onOpenAssessment = onOpenAssessment,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )

                RSTab.INSPEKSI -> {}

                RSTab.RIWAYAT -> HistoryScreen(
                    assessments = saved,
                    onOpenAssessment = onOpenAssessment,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
        }
    }
}

private fun Assessment.toSavedAssessmentUi(): SavedAssessmentUi {
    val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(createdAt))
    return SavedAssessmentUi(
        id = id,
        companyName = company,
        assessorName = assessorId,
        date = date,
        syncStatus = if (syncStatus == "SYNCED") SyncStatusUi.TERKIRIM else SyncStatusUi.MENUNGGU_KIRIM
    )
}