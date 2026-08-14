package com.rumahsehat.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rumahsehat.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SecondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FormStepper(
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    val steps = listOf("Komponen", "Sanitasi", "Perilaku")
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        steps.forEachIndexed { index, label ->
            val stepNumber = index + 1
            val isDone = stepNumber < currentStep
            val isActive = stepNumber == currentStep
            val leftDone = index > 0 && index < currentStep
            val rightDone = index < steps.size - 1 && index < currentStep - 1

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (leftDone) Primary else if (index == 0) Color.Transparent else OutlineVariant)
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isDone || isActive -> Primary
                                    else -> SurfaceContainerHigh
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(18.dp))
                        } else {
                            Text(
                                text = "$stepNumber",
                                color = if (isActive) OnPrimary else OnSurfaceVariant,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (rightDone) Primary else if (index == steps.size - 1) Color.Transparent else OutlineVariant)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isActive || isDone) Primary else OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PhotoUploadCard(
    title: String,
    subtitle: String,
    photoUri: String?,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        border = BorderStroke(0.5.dp, OutlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = OnSurface)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            if (photoUri != null) {
                val preview by produceState<ImageBitmap?>(initialValue = null, photoUri) {
                    value = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeFile(photoUri)?.asImageBitmap()
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    if (preview != null) {
                        Image(
                            bitmap = preview!!,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Camera, contentDescription = null, tint = Primary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("Foto diambil", style = MaterialTheme.typography.labelMedium, color = Primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        BorderStroke(1.dp, Outline),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onCaptureClick),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Camera, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (photoUri != null) "Ganti Foto" else "Ambil Foto",
                        style = MaterialTheme.typography.labelLarge,
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionCard(
    number: Int,
    title: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int?,
    note: String,
    onSelect: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        border = BorderStroke(0.5.dp, OutlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$number", color = OnPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(Modifier.selectableGroup()) {
                options.forEachIndexed { index, option ->
                    val isSelected = selectedIndex == index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                BorderStroke(1.dp, if (isSelected) Primary else OutlineVariant),
                                RoundedCornerShape(8.dp)
                            )
                            .selectable(
                                selected = isSelected,
                                onClick = { onSelect(index) },
                                role = androidx.compose.ui.semantics.Role.RadioButton
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = Primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(option, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Catatan (opsional)", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnSurface),
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
fun IdentityStepCard(
    name: String,
    instansi: String,
    onNameChange: (String) -> Unit,
    onInstansiChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        border = BorderStroke(0.5.dp, OutlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Identitas Petugas", style = MaterialTheme.typography.titleMedium, color = OnSurface)
            Spacer(Modifier.height(2.dp))
            Text(
                "Isi identitas sebelum menilai. Dipakai untuk pelacakan hasil penilaian.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama Petugas") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = instansi,
                onValueChange = onInstansiChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Asal Kader / Instansi") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
fun FormBottomBar(
    onBack: () -> Unit,
    onNext: () -> Unit,
    isFirstStep: Boolean,
    isLastStep: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
        color = SurfaceContainerLowest,
        border = BorderStroke(0.5.dp, OutlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface)
            ) {
                Text(if (isFirstStep) "Batal" else "Kembali")
            }

            Button(
                onClick = onNext,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary)
            ) {
                Text(if (isLastStep) "Simpan & Kirim" else "Lanjut")
                if (!isLastStep) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
