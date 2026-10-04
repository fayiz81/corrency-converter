package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CurrencyUiState
import com.example.ui.Timeframe
import java.text.DecimalFormat

@Composable
fun TrendChart(
    uiState: CurrencyUiState,
    onTimeframeSelected: (Timeframe) -> Unit,
    modifier: Modifier = Modifier
) {
    val rateFormat = remember { DecimalFormat("#,##0.0000") }
    val points = uiState.trendPoints

    val minRate = remember(points) { points.minOfOrNull { it.second } ?: 1.0 }
    val maxRate = remember(points) { points.maxOfOrNull { it.second } ?: 1.0 }
    val avgRate = remember(points) {
        if (points.isNotEmpty()) points.map { it.second }.average() else 1.0
    }

    val firstRate = remember(points) { points.firstOrNull()?.second ?: 1.0 }
    val lastRate = remember(points) { points.lastOrNull()?.second ?: 1.0 }
    val percentageChange = remember(firstRate, lastRate) {
        if (firstRate != 0.0) ((lastRate - firstRate) / firstRate) * 100.0 else 0.0
    }
    val isPositive = percentageChange >= 0

    // Scrub state
    var scrubIndex by remember { mutableStateOf<Int?>(null) }
    val activePoint = scrubIndex?.let { points.getOrNull(it) } ?: points.lastOrNull()

    // Smooth entry animation for chart draw
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(points, uiState.selectedTimeframe) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trend_chart_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Title and Timeframe Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Historical Trend",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "${uiState.fromCurrency} / ${uiState.toCurrency}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Timeframe Segmented Tabs
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        Timeframe.entries.forEach { tf ->
                            val isSelected = uiState.selectedTimeframe == tf
                            val tabBgColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "tabBg_${tf.label}"
                            )
                            val tabTextColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(durationMillis = 200),
                                label = "tabText_${tf.label}"
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(tabBgColor)
                                    .pointerInput(tf) {
                                        detectTapGestures { onTimeframeSelected(tf) }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("timeframe_tab_${tf.label}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tf.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = tabTextColor
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Price & Percentage Change
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    AnimatedContent(
                        targetState = if (activePoint != null) rateFormat.format(activePoint.second) else "--",
                        transitionSpec = {
                            fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
                        },
                        label = "activeRateAnim"
                    ) { rateStr ->
                        Text(
                            text = rateStr,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    Text(
                        text = activePoint?.first ?: "Latest",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPositive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = if (isPositive) "Rate increased" else "Rate decreased",
                            tint = if (isPositive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.2f%%", percentageChange),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isPositive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoadingChart) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp
                    )
                } else if (points.size >= 2) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val gradientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("trend_line_canvas")
                            .pointerInput(points) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val idx = ((offset.x / size.width) * (points.size - 1))
                                            .toInt()
                                            .coerceIn(0, points.size - 1)
                                        scrubIndex = idx
                                    },
                                    onDragEnd = { scrubIndex = null },
                                    onDragCancel = { scrubIndex = null },
                                    onDrag = { change, _ ->
                                        val idx = ((change.position.x / size.width) * (points.size - 1))
                                            .toInt()
                                            .coerceIn(0, points.size - 1)
                                        scrubIndex = idx
                                    }
                                )
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val paddingY = 16f
                        val effectiveHeight = height - paddingY * 2

                        val range = (maxRate - minRate).let { if (it == 0.0) 0.0001 else it }

                        val path = Path()
                        val fillPath = Path()

                        val stepX = width / (points.size - 1).coerceAtLeast(1)

                        // Draw Grid lines
                        drawLine(
                            color = outlineColor,
                            start = Offset(0f, paddingY),
                            end = Offset(width, paddingY),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = outlineColor,
                            start = Offset(0f, height / 2),
                            end = Offset(width, height / 2),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = outlineColor,
                            start = Offset(0f, height - paddingY),
                            end = Offset(width, height - paddingY),
                            strokeWidth = 1f
                        )

                        // Construct curves with animated progress
                        val visibleCount = ((points.size - 1) * animationProgress.value).toInt().coerceAtLeast(1)
                        var lastDrawnX = 0f

                        points.take(visibleCount + 1).forEachIndexed { index, pair ->
                            val x = index * stepX
                            lastDrawnX = x
                            val normalizedY = ((pair.second - minRate) / range).toFloat()
                            val y = (height - paddingY) - (normalizedY * effectiveHeight)

                            if (index == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                        }

                        fillPath.lineTo(lastDrawnX, height)
                        fillPath.close()

                        // Draw Gradient Fill under line
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(gradientColor, Color.Transparent),
                                startY = paddingY,
                                endY = height
                            )
                        )

                        // Draw Stroke Line
                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Scrub indicator
                        scrubIndex?.let { idx ->
                            val pt = points[idx]
                            val sx = idx * stepX
                            val normalizedY = ((pt.second - minRate) / range).toFloat()
                            val sy = (height - paddingY) - (normalizedY * effectiveHeight)

                            drawLine(
                                color = primaryColor.copy(alpha = 0.5f),
                                start = Offset(sx, 0f),
                                end = Offset(sx, height),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            drawCircle(
                                color = primaryColor,
                                radius = 6.dp.toPx(),
                                center = Offset(sx, sy)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = Offset(sx, sy)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Historical data unavailable for this pair",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row: Low, Avg, High
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Period Low", value = rateFormat.format(minRate))
                StatItem(label = "Average", value = rateFormat.format(avgRate))
                StatItem(label = "Period High", value = rateFormat.format(maxRate))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
