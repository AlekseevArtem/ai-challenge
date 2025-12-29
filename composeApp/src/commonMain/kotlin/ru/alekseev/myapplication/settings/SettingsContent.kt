package ru.alekseev.myapplication.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.alekseev.myapplication.core.common.RAGDefaults
import ru.alekseev.myapplication.domain.entity.RagMode
import ru.alekseev.myapplication.feature.settings.presentation.SettingsComponent
import ru.alekseev.myapplication.feature.settings.presentation.SettingsIntent
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.compose.dsl.subscribe
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.round

@Composable
fun SettingsContent(
    component: SettingsComponent,
    modifier: Modifier = Modifier
) {
    val state by component.store.subscribe()
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF0F3460),
                        Color(0xFF16213E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar with gradient
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF6C63FF),
                                    Color(0xFF5B4DFF),
                                    Color(0xFF4A3FFF)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = component::onBackClicked
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Settings",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Settings content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // RAG Mode Selection
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2C2C54),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "RAG Mode",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Disabled option
                        RagModeOption(
                            title = "Disabled",
                            description = "No document context used",
                            isSelected = state.ragMode is RagMode.Disabled,
                            onClick = {
                                scope.launch {
                                    component.store.emit(SettingsIntent.UpdateRagMode(RagMode.Disabled))
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Enabled option
                        RagModeOption(
                            title = "RAG",
                            description = "Use document context for responses",
                            isSelected = state.ragMode is RagMode.Enabled,
                            onClick = {
                                scope.launch {
                                    component.store.emit(SettingsIntent.UpdateRagMode(RagMode.Enabled))
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Enabled with filtering option
                        val currentThreshold = when (val mode = state.ragMode) {
                            is RagMode.EnabledWithFiltering -> mode.threshold
                            else -> RAGDefaults.DEFAULT_THRESHOLD
                        }

                        var sliderThreshold by remember(currentThreshold) { mutableStateOf(currentThreshold) }

                        RagModeOption(
                            title = "RAG + Filtering",
                            description = "Use document context with relevance filtering",
                            isSelected = state.ragMode is RagMode.EnabledWithFiltering,
                            onClick = {
                                scope.launch {
                                    component.store.emit(
                                        SettingsIntent.UpdateRagMode(
                                            RagMode.EnabledWithFiltering(sliderThreshold)
                                        )
                                    )
                                }
                            }
                        )

                        // Threshold slider (visible only when filtering is selected)
                        if (state.ragMode is RagMode.EnabledWithFiltering) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp)
                            ) {
                                val formattedThreshold = (round(sliderThreshold * 100) / 100).toString()
                                Text(
                                    text = "Similarity Threshold: $formattedThreshold",
                                    color = Color(0xFFB0B0B0),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = sliderThreshold,
                                    onValueChange = { newValue ->
                                        sliderThreshold = newValue
                                    },
                                    onValueChangeFinished = {
                                        scope.launch {
                                            component.store.emit(
                                                SettingsIntent.UpdateRagMode(
                                                    RagMode.EnabledWithFiltering(sliderThreshold)
                                                )
                                            )
                                        }
                                    },
                                    valueRange = 0.0f..1.0f,
                                    steps = 19,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color(0xFF6C63FF),
                                        inactiveTrackColor = Color(0xFF3A3A6B)
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "0.0",
                                        color = Color(0xFF808080),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "1.0",
                                        color = Color(0xFF808080),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Info card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1F1F3A),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "About RAG",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "RAG (Retrieval-Augmented Generation) uses relevant information from your project's codebase to provide more accurate responses. With filtering enabled, only chunks with similarity above the threshold are used.",
                            color = Color(0xFFB0B0B0),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RagModeOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(
                if (isSelected) Color(0xFF3A3A6B) else Color.Transparent
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Color(0xFF6C63FF) else Color(0xFF3A3A6B)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = description,
                color = Color(0xFFB0B0B0),
                fontSize = 13.sp
            )
        }
    }
}
