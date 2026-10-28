package com.jarvis.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.model.AssistantState
import com.jarvis.assistant.ui.theme.*

@Composable
fun VoiceOrb(
    state: AssistantState,
    audioRms: Float = 0f,
    size: Dp = 200.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Orb")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "rot"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "pulse"
    )

    val primaryColor = when (state) {
        AssistantState.IDLE -> JarvisCyan
        AssistantState.LISTENING -> JarvisCyan
        AssistantState.PROCESSING -> Color.White
        AssistantState.SPEAKING -> JarvisSpeakingWave
        AssistantState.ERROR -> JarvisError
    }

    Box(modifier = Modifier.size(size).clickable(onClick = onClick)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = (size.toPx() / 2f) * 0.45f

            // Radial Atmospheric Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = baseRadius * 2.2f
                ),
                radius = baseRadius * 2.2f
            )

            // Segmented Cyber Ring
            rotate(rotationAngle, pivot = center) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.5f),
                    center = center,
                    radius = baseRadius * 1.5f,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f)))
                )
            }

            // Pulsating Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, primaryColor, Color.Transparent),
                    center = center,
                    radius = baseRadius * pulseScale
                ),
                radius = baseRadius * pulseScale
            )
        }
    }
}