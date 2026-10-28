package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
import com.example.viewmodel.ModalState
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalDialog(
    modalState: ModalState,
    onDismiss: () -> Unit,
    onConfirmClearChat: () -> Unit,
    onConfirmDeleteDoc: () -> Unit,
    onConfirmWipeDocs: () -> Unit,
    onOpenSettingsCompany: () -> Unit
) {
    if (modalState == ModalState.None) return

    val colors = AppTheme.colors
    val context = LocalContext.current

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface1)
            .border(1.dp, colors.separator, RoundedCornerShape(16.dp))
    ) {
        when (modalState) {
            is ModalState.ClearChat -> {
                StandardDialog(
                    title = "Start a new chat?",
                    body = "The current conversation will be cleared. Documents stay loaded.",
                    cancelLabel = "Cancel",
                    confirmLabel = "Confirm",
                    onCancel = onDismiss,
                    onConfirm = onConfirmClearChat
                )
            }
            is ModalState.ConfirmDeleteDoc -> {
                StandardDialog(
                    title = "Remove document?",
                    body = "\"${modalState.doc.name}\" will be removed from this device.",
                    cancelLabel = "Cancel",
                    confirmLabel = "Remove",
                    isDestructive = true,
                    onCancel = onDismiss,
                    onConfirm = onConfirmDeleteDoc
                )
            }
            is ModalState.ConfirmWipeDocs -> {
                StandardDialog(
                    title = "Remove all documents?",
                    body = "All uploaded policy files will be deleted from this device.",
                    cancelLabel = "Cancel",
                    confirmLabel = "Remove",
                    isDestructive = true,
                    onCancel = onDismiss,
                    onConfirm = onConfirmWipeDocs
                )
            }
            is ModalState.HrEmailNotSet -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "HR email not set",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "HR has not added a contact email yet. They can add it under Settings → Company.",
                            fontSize = 13.5.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(colors.separator)
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Close",
                                fontSize = 16.sp,
                                color = colors.green
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(0.5.dp)
                                .height(46.dp)
                                .background(colors.separator)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable {
                                    onDismiss()
                                    onOpenSettingsCompany()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Settings",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.green
                            )
                        }
                    }
                }
            }
            is ModalState.DocPreview -> {
                val fullText = modalState.doc.chunks.mapIndexed { idx, chunk ->
                    "— Passage ${idx + 1} —\n$chunk"
                }.joinToString("\n\n")

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = modalState.doc.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface2)
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = fullText,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(colors.separator)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Close",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.green
                        )
                    }
                }
            }
            is ModalState.EmailHr -> {
                val subject = "Question for HR"
                val bodyText = if (!modalState.lastQuestionRefusal.isNullOrEmpty()) {
                    "Hello HR team,\n\nI could not find this policy in the uploaded documents:\n\"${modalState.lastQuestionRefusal}\"\n\nCould you please advise?\n"
                } else {
                    "Hello HR team,\n\nI have a question regarding company policies:\n\n"
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Email HR",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = modalState.hrEmail,
                            fontSize = 13.5.sp,
                            color = colors.textSecondary
                        )
                    }

                    EmailOptionButton(
                        text = "Default Mail App",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${modalState.hrEmail}?subject=${Uri.encode(subject)}&body=${Uri.encode(bodyText)}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            onDismiss()
                        }
                    )

                    EmailOptionButton(
                        text = "Gmail Web",
                        onClick = {
                            val url = "https://mail.google.com/mail/?view=cm&fs=1&to=${URLEncoder.encode(modalState.hrEmail, "UTF-8")}&su=${URLEncoder.encode(subject, "UTF-8")}&body=${URLEncoder.encode(bodyText, "UTF-8")}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            onDismiss()
                        }
                    )

                    EmailOptionButton(
                        text = "Outlook Web",
                        onClick = {
                            val url = "https://outlook.office.com/mail/deeplink/compose?to=${URLEncoder.encode(modalState.hrEmail, "UTF-8")}&subject=${URLEncoder.encode(subject, "UTF-8")}&body=${URLEncoder.encode(bodyText, "UTF-8")}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            onDismiss()
                        }
                    )

                    EmailOptionButton(
                        text = "Copy Address",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("HR Email", modalState.hrEmail))
                            onDismiss()
                        }
                    )

                    EmailOptionButton(
                        text = "Cancel",
                        isCancel = true,
                        onClick = onDismiss
                    )
                }
            }
            is ModalState.ShowSqlDialog -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Supabase Table Setup",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Run this SQL command in your Supabase project (SQL Editor tab) to create the document sync table:",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface2)
                            .border(1.dp, colors.separator, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = modalState.sql,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = colors.green,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(colors.separator)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Supabase SQL", modalState.sql)
                                    clipboard.setPrimaryClip(clip)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Copy SQL",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.green
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(0.5.dp)
                                .height(46.dp)
                                .background(colors.separator)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Done",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
            ModalState.None -> {}
        }
    }
}


@Composable
private fun StandardDialog(
    title: String,
    body: String,
    cancelLabel: String,
    confirmLabel: String,
    isDestructive: Boolean = false,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = AppTheme.colors

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                fontSize = 13.5.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.separator)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cancelLabel,
                    fontSize = 16.sp,
                    color = colors.green
                )
            }

            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(46.dp)
                    .background(colors.separator)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clickable { onConfirm() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = confirmLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDestructive) colors.red else colors.green
                )
            }
        }
    }
}

@Composable
private fun EmailOptionButton(
    text: String,
    isCancel: Boolean = false,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(colors.separator)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = if (isCancel) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isCancel) colors.green else colors.textPrimary
        )
    }
}
