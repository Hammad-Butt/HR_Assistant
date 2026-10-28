package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.CanvasBlack
import com.example.ui.theme.SurfaceDark2
import com.example.ui.theme.SystemGreen
import com.example.ui.theme.TextWhite

@Composable
fun BootScreen(
    orgName: String,
    logoUri: String?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "boot_anims")

    // Breathing scale animation for the logo
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Sliding indicator animation
    val slideOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slide"
    )

    val colors = com.example.ui.theme.AppTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Breathing Logo
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF5AE67D), Color(0xFF1FB446))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!logoUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = logoUri,
                        contentDescription = "Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "HR",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (colors.isDark) Color.Black else Color.White,
                        letterSpacing = (-0.04).sp
                    )
                }
            }

            // Title
            Text(
                text = orgName.ifEmpty { "HR Assistant" },
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                letterSpacing = (-0.025).sp
            )

            // Progress bar
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.surface3)
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.dp)
                        .offset(x = (90.dp * slideOffset))
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.green)
                )
            }
        }
    }
}

