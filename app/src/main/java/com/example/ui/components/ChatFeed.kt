package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ChatMessage
import com.example.data.DocumentItem
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CanvasBlack
import kotlinx.coroutines.delay

@Composable
fun ChatFeed(
    messages: List<ChatMessage>,
    docs: List<DocumentItem>,
    orgName: String,
    logoUri: String?,
    hrMail: String,
    suggestions: List<String>,
    isTyping: Boolean,
    onSuggestionClick: (String) -> Unit,
    onCopyAnswer: (title: String, bullets: List<String>) -> Unit,
    onEmailHrClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty() || isTyping) {
            val target = messages.size + if (isTyping) 1 else 0
            listState.animateScrollToItem(maxOf(0, target - 1))
        }
    }

    if (messages.isEmpty()) {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 28.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WelcomeView(
                    docs = docs,
                    orgName = orgName,
                    logoUri = logoUri,
                    suggestions = suggestions,
                    onSuggestionClick = onSuggestionClick
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                if (msg.role == "user") {
                    UserMessageBubble(text = msg.text)
                } else {
                    BotMessageItem(
                        message = msg,
                        logoUri = logoUri,
                        hrMail = hrMail,
                        onCopy = { title, bullets -> onCopyAnswer(title, bullets) },
                        onEmailHrClick = onEmailHrClick
                    )
                }
            }

            if (isTyping) {
                item(key = "typing_indicator") {
                    TypingIndicator(logoUri = logoUri)
                }
            }
        }
    }
}

@Composable
private fun WelcomeView(
    docs: List<DocumentItem>,
    orgName: String,
    logoUri: String?,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    val colors = AppTheme.colors
    val totalChunks = docs.sumOf { it.chunks.size }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App mark
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(18.dp))
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
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = "HR",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = CanvasBlack,
                    letterSpacing = (-0.04).sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (orgName.isNotBlank()) {
            Text(
                text = orgName.uppercase(),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.green,
                letterSpacing = 0.09.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
            text = if (docs.isNotEmpty()) "Hi, I'm HR Assistant" else "Welcome",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            letterSpacing = (-0.035).sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ask about company policies, leave, attendance or rules. I only answer from the documents uploaded on this device.",
            fontSize = 15.5.sp,
            color = colors.textSecondary,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Document count badge chip
        if (docs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(colors.surface1)
                    .border(0.5.dp, colors.separator, RoundedCornerShape(99.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.green, fontWeight = FontWeight.SemiBold)) {
                            append("$totalChunks")
                        }
                        append(" passages from ")
                        withStyle(SpanStyle(color = colors.green, fontWeight = FontWeight.SemiBold)) {
                            append("${docs.size}")
                        }
                        append(" document${if (docs.size == 1) "" else "s"}")
                    },
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(colors.redSoft)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "No documents loaded yet",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.red
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Ask HR to upload the company handbook from the settings menu.",
                fontSize = 13.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Suggestions Group
        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "TRY ASKING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    letterSpacing = 0.04.sp,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface1)
                        .border(0.5.dp, colors.separator, RoundedCornerShape(14.dp))
                ) {
                    suggestions.forEachIndexed { index, question ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSuggestionClick(question) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = question,
                                fontSize = 15.sp,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (index < suggestions.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp)
                                    .height(0.5.dp)
                                    .background(colors.separator)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(text: String) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 6.dp
                    )
                )
                .background(colors.green)
                .padding(horizontal = 15.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                color = if (colors.isDark) CanvasBlack else Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 15.5.sp,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun BotMessageItem(
    message: ChatMessage,
    logoUri: String?,
    hrMail: String,
    onCopy: (String, List<String>) -> Unit,
    onEmailHrClick: () -> Unit
) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Bot Avatar
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(colors.green),
            contentAlignment = Alignment.Center
        ) {
            if (!logoUri.isNullOrEmpty()) {
                AsyncImage(
                    model = logoUri,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "HR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (colors.isDark) Color.Black else Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Content
        if (message.isRefusal) {
            RefusalBubble(
                text = message.text,
                hrMail = hrMail,
                onEmailHrClick = onEmailHrClick
            )
        } else if (!message.bullets.isNullOrEmpty()) {
            PolicyAnswerCard(
                title = message.title ?: "Answer",
                bullets = message.bullets,
                sourceName = message.sourceName,
                sourceText = message.sourceText,
                qTerms = message.qTerms,
                onCopy = { onCopy(message.title ?: "Answer", message.bullets) }
            )
        } else {
            // Plain text greeting / response
            Box(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomEnd = 20.dp,
                            bottomStart = 6.dp
                        )
                    )
                    .background(colors.surface1)
                    .border(0.5.dp, colors.separator, RoundedCornerShape(20.dp))
                    .padding(horizontal = 15.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    color = colors.textPrimary,
                    fontSize = 15.5.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun RefusalBubble(
    text: String,
    hrMail: String,
    onEmailHrClick: () -> Unit
) {
    val colors = AppTheme.colors

    Box(
        modifier = Modifier
            .widthIn(max = 310.dp)
            .clip(
                RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomEnd = 20.dp,
                    bottomStart = 6.dp
                )
            )
            .background(colors.redSoft)
            .border(0.5.dp, colors.red.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = "Warning",
                    tint = colors.red,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "NOT FOUND",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.red,
                    letterSpacing = 0.06.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = text,
                fontSize = 15.sp,
                color = colors.textPrimary,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Try rephrasing, or contact HR for policies not yet covered.",
                fontSize = 13.sp,
                color = colors.textSecondary,
                lineHeight = 18.sp
            )

            if (hrMail.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(colors.red)
                        .clickable { onEmailHrClick() }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mail,
                        contentDescription = "Mail",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Email HR about this",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PolicyAnswerCard(
    title: String,
    bullets: List<String>,
    sourceName: String?,
    sourceText: String?,
    qTerms: List<String>,
    onCopy: () -> Unit
) {
    val colors = AppTheme.colors
    var isCopied by remember { mutableStateOf(false) }
    var isSourceOpen by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(1400)
            isCopied = false
        }
    }

    Box(
        modifier = Modifier
            .widthIn(max = 320.dp)
            .clip(
                RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomEnd = 20.dp,
                    bottomStart = 6.dp
                )
            )
            .background(colors.surface1)
            .border(0.5.dp, colors.separator, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column {
            // Header with title and copy button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.025).sp,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        onCopy()
                        isCopied = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isCopied) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Copied",
                            tint = colors.green,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy answer",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bullet list
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                bullets.forEachIndexed { index, bullet ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(colors.separator)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Green bullet dot
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 10.dp)
                                .size(6.5.dp)
                                .clip(CircleShape)
                                .background(colors.green)
                        )

                        // Highlighted text
                        Text(
                            text = highlightKeywords(bullet, qTerms, colors.greenSoft, colors.green),
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            color = colors.textPrimary
                        )
                    }
                }
            }

            // Expandable source citation
            if (!sourceName.isNullOrEmpty() && !sourceText.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(colors.separator)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSourceOpen = !isSourceOpen }
                        .padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Source · $sourceName",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Icon(
                        imageVector = if (isSourceOpen) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Toggle source",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isSourceOpen,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface2)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = sourceText,
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun highlightKeywords(
    text: String,
    qTerms: List<String>,
    greenSoft: Color,
    greenColor: Color
): androidx.compose.ui.text.AnnotatedString {
    val terms = qTerms.filter { it.length > 1 }.sortedByDescending { it.length }
    if (terms.isEmpty()) return buildAnnotatedString { append(text) }

    val regex = try {
        Regex("\\b(" + terms.joinToString("|") { Regex.escape(it) } + ")\\b", RegexOption.IGNORE_CASE)
    } catch (_: Exception) {
        return buildAnnotatedString { append(text) }
    }

    val matches = regex.findAll(text).toList()
    if (matches.isEmpty()) return buildAnnotatedString { append(text) }

    return buildAnnotatedString {
        var currentIndex = 0
        for (m in matches) {
            val start = m.range.first
            val end = m.range.last + 1
            if (start > currentIndex) {
                append(text.substring(currentIndex, start))
            }
            withStyle(
                SpanStyle(
                    background = greenSoft,
                    color = greenColor,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(text.substring(start, end))
            }
            currentIndex = end
        }
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

@Composable
private fun TypingIndicator(logoUri: String?) {
    val colors = AppTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")

    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(colors.green),
            contentAlignment = Alignment.Center
        ) {
            if (!logoUri.isNullOrEmpty()) {
                AsyncImage(
                    model = logoUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "HR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (colors.isDark) Color.Black else Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomEnd = 20.dp,
                        bottomStart = 6.dp
                    )
                )
                .background(colors.surface1)
                .border(0.5.dp, colors.separator, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .offset(y = dot1.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(colors.textSecondary)
            )
            Box(
                modifier = Modifier
                    .offset(y = dot2.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(colors.textSecondary)
            )
            Box(
                modifier = Modifier
                    .offset(y = dot3.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(colors.textSecondary)
            )
        }
    }
}
