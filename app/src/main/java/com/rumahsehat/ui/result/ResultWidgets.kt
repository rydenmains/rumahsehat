package com.rumahsehat.ui.result

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import com.rumahsehat.data.model.FormItem
import com.rumahsehat.data.model.ScoreItem
import com.rumahsehat.ui.theme.*

@Composable
fun ScoreRing(achieved: Int, max: Int, color: Color) {
    val progress = if (max > 0) (achieved.toFloat() / max.toFloat()).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scoreRing"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = Color(0xFFE0E3E3),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$achieved",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            Text("/$max", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        }
    }
}

@Composable
fun GroupBreakdown(
    title: String,
    items: List<FormItem>,
    scoresById: Map<String, ScoreItem>,
    mainColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val groupMax = items.sumOf { it.maxScore }
    val groupGot = items.sumOf { item ->
        scoresById[item.id]?.takeIf { it.isApplicable }?.score ?: 0
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        onClick = { expanded = !expanded }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = mainColor)
                Text("$groupGot/$groupMax", style = MaterialTheme.typography.labelMedium, color = OnSurface)
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                items.forEach { form ->
                    val s = scoresById[form.id]
                    val ok = s != null && s.isApplicable && s.score >= form.maxScore
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${form.id} · ${form.options.getOrNull(s?.optionIndex ?: -1)?.label ?: "Belum dijawab"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (ok) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (ok) StatusSehat else StatusTidakSehat,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Text(
                    "Ketuk untuk rincian",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    modifier = Modifier.clickable { expanded = true }
                )
            }
        }
    }
}
