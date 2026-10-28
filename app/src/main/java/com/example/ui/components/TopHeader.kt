package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.AppTheme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate

@Composable
fun TopHeader(
    orgName: String,
    logoUri: String?,
    docsCount: Int,
    statusText: String,
    isDarkTheme: Boolean,
    currentUserRole: String = "EMPLOYEE",
    currentUserEmail: String = "",
    isSyncing: Boolean = false,
    onSyncClick: () -> Unit = {},
    onToggleTheme: () -> Unit,
    onNewChatClick: () -> Unit,
    onMailClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.canvasBg.copy(alpha = 0.95f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left action buttons: Prominent Sign Out button & Mail HR
            Row(
                modifier = Modifier.weight(1.35f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High-visibility, prominent Sign Out button for both HR and Employee
                Surface(
                    onClick = onLogout,
                    shape = RoundedCornerShape(20.dp),
                    color = colors.red.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.red.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .testTag("header_signout_button")
                        .height(34.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "Sign Out",
                            tint = colors.red,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sign Out",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.red
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onMailClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("email_hr_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mail,
                        contentDescription = "Email HR",
                        tint = colors.green,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Center: title and role badge / status dot
            Column(
                modifier = Modifier.weight(1.9f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (!logoUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = logoUri,
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .border(0.5.dp, colors.separator, RoundedCornerShape(5.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = orgName.ifEmpty { "HR Assistant" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    val isHr = currentUserRole.equals("HR", ignoreCase = true)
                    val roleBg = if (isHr) colors.greenSoft else Color(0x224E8BFF)
                    val roleColor = if (isHr) colors.green else Color(0xFF1E60D4)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(roleBg)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isHr) "HR ADMIN" else "EMPLOYEE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = roleColor
                        )
                    }

                    val dotColor = if (docsCount > 0) colors.green else colors.red
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = statusText,
                        fontSize = 11.5.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
            val spinAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "sync_angle"
            )

            // Right action buttons: Cloud Sync, Theme toggle, Settings
            Row(
                modifier = Modifier.weight(1.3f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSyncClick,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("cloud_sync_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudSync,
                        contentDescription = "Sync Documents with Supabase Cloud",
                        tint = if (isSyncing) colors.green else colors.textSecondary,
                        modifier = Modifier
                            .size(21.dp)
                            .rotate(if (isSyncing) spinAngle else 0f)
                    )
                }

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = if (isDarkTheme) "Change to Light Theme" else "Change to Dark Theme",
                        tint = colors.green,
                        modifier = Modifier.size(21.dp)
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Open settings",
                        tint = colors.green,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }


        // Hairline bottom separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.separator)
        )
    }
}
