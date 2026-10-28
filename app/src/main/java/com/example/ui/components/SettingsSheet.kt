package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ApiSettings
import com.example.data.CompanySettings
import com.example.data.DocumentItem
import com.example.ui.theme.CanvasBlack
import com.example.ui.theme.GreenSoft
import com.example.ui.theme.Separator
import com.example.ui.theme.SurfaceDark1
import com.example.ui.theme.SurfaceDark2
import com.example.ui.theme.SurfaceDark3
import com.example.ui.theme.SystemGreen
import com.example.ui.theme.SystemRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    isOpen: Boolean,
    currentTab: Int,
    docs: List<DocumentItem>,
    companySettings: CompanySettings,
    apiSettings: ApiSettings,
    supabaseSettings: com.example.data.SupabaseSettings,
    currentUserRole: String,
    currentUserEmail: String,
    hrLoginEmail: String,
    hrLoginPassword: String,
    orgError: String,
    onClose: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onFilesPicked: (List<Uri>) -> Unit,
    onReloadSampleHandbook: () -> Unit,
    onPreviewDoc: (DocumentItem) -> Unit,
    onDeleteDoc: (DocumentItem) -> Unit,
    onWipeAllDocs: () -> Unit,
    onSaveCompany: (name: String, email: String) -> Unit,
    onSaveHrCredentials: (currentPass: String, newEmail: String, newPass: String, confirmPass: String) -> Unit,
    onLogoPicked: (Uri) -> Unit,
    onRemoveLogo: () -> Unit,
    onFactoryReset: () -> Unit,
    onAddApiKeys: (provider: com.example.data.ApiProvider, keysText: String, model: String, customUrl: String) -> Unit,
    onRemoveApiKey: (id: String) -> Unit,
    onResetExhaustedKeys: () -> Unit,
    onClearAllApiKeys: () -> Unit,
    onSaveLegacyApi: (key: String, url: String, model: String) -> Unit,
    onSaveSupabaseConfig: (String, String, Boolean) -> Unit,
    onTestSupabaseConnection: () -> Unit,
    onSyncSupabaseNow: () -> Unit,
    onCopySql: () -> Unit,
    onCopyCleanupSql: () -> Unit = {},
    onWipeCloudData: () -> Unit = {},
    onLogout: () -> Unit
) {
    if (!isOpen) return

    val colors = com.example.ui.theme.AppTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isHr = currentUserRole.equals("HR", ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = colors.surface1,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onLogout,
                    shape = RoundedCornerShape(20.dp),
                    color = colors.red.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.red.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .testTag("logout_button")
                        .height(34.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.red
                        )
                    }
                }

                Text(
                    text = if (isHr) "HR Settings ($currentUserEmail)" else "Employee Portal",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                TextButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("settings_done_button")
                ) {
                    Text(
                        text = "Done",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.green
                    )
                }
            }

            // Segmented Tabs
            SettingsTabs(
                isHr = isHr,
                selectedTab = currentTab,
                onTabSelected = onTabSelected
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    label = "settings_tab_content"
                ) { tab ->
                    if (isHr) {
                        when (tab) {
                            0 -> DocumentsTab(
                                docs = docs,
                                isHr = true,
                                onFilesPicked = onFilesPicked,
                                onReloadSampleHandbook = onReloadSampleHandbook,
                                onPreviewDoc = onPreviewDoc,
                                onDeleteDoc = onDeleteDoc,
                                onWipeAllDocs = onWipeAllDocs
                            )
                            1 -> CompanyTab(
                                company = companySettings,
                                hrLoginEmail = hrLoginEmail,
                                hrLoginPassword = hrLoginPassword,
                                orgError = orgError,
                                onSaveCompany = onSaveCompany,
                                onSaveHrCredentials = onSaveHrCredentials,
                                onLogoPicked = onLogoPicked,
                                onRemoveLogo = onRemoveLogo,
                                onFactoryReset = onFactoryReset
                            )
                            2 -> ApiTab(
                                apiSettings = apiSettings,
                                onAddApiKeys = onAddApiKeys,
                                onRemoveApiKey = onRemoveApiKey,
                                onResetExhaustedKeys = onResetExhaustedKeys,
                                onClearAllApiKeys = onClearAllApiKeys,
                                onSaveLegacyApi = onSaveLegacyApi
                            )
                            3 -> CloudTab(
                                supabaseSettings = supabaseSettings,
                                onSaveConfig = onSaveSupabaseConfig,
                                onTestConnection = onTestSupabaseConnection,
                                onSyncNow = onSyncSupabaseNow,
                                onCopySql = onCopySql,
                                onCopyCleanupSql = onCopyCleanupSql,
                                onWipeCloudData = onWipeCloudData
                            )
                            4 -> AboutTab(
                                currentUserEmail = currentUserEmail,
                                currentUserRole = currentUserRole,
                                onLogout = onLogout
                            )
                        }
                    } else {
                        // Employee tabs: 0: Documents (view only), 1: About
                        when (tab) {
                            0 -> DocumentsTab(
                                docs = docs,
                                isHr = false,
                                onFilesPicked = onFilesPicked,
                                onReloadSampleHandbook = onReloadSampleHandbook,
                                onPreviewDoc = onPreviewDoc,
                                onDeleteDoc = onDeleteDoc,
                                onWipeAllDocs = onWipeAllDocs
                            )
                            1 -> AboutTab(
                                currentUserEmail = currentUserEmail,
                                currentUserRole = currentUserRole,
                                onLogout = onLogout
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTabs(
    isHr: Boolean,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val colors = com.example.ui.theme.AppTheme.colors
    val tabs = if (isHr) listOf("Documents", "Company", "AI", "Cloud", "About") else listOf("Documents", "About")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface2)
            .border(1.dp, colors.separator, RoundedCornerShape(10.dp))
            .padding(3.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            val bg = if (isSelected) colors.surface1 else Color.Transparent
            val borderModifier = if (isSelected) Modifier.border(1.dp, colors.separator.copy(alpha = 0.6f), RoundedCornerShape(7.dp)) else Modifier
            val textColor = if (isSelected) colors.textPrimary else colors.textSecondary
            val weight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(bg)
                    .then(borderModifier)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = weight,
                    color = textColor,
                    maxLines = 1
                )
            }
        }
    }
}


@Composable
private fun DocumentsTab(
    docs: List<DocumentItem>,
    isHr: Boolean,
    onFilesPicked: (List<Uri>) -> Unit,
    onReloadSampleHandbook: () -> Unit,
    onPreviewDoc: (DocumentItem) -> Unit,
    onDeleteDoc: (DocumentItem) -> Unit,
    onWipeAllDocs: () -> Unit
) {
    val colors = com.example.ui.theme.AppTheme.colors
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            onFilesPicked(uris)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (isHr) {
            Text(
                text = "UPLOAD POLICY FILES",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                letterSpacing = 0.04.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Dropzone
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.greenSoft)
                    .border(1.5.dp, colors.green.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .clickable {
                        launcher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "text/plain",
                                "application/msword"
                            )
                        )
                    }
                    .padding(vertical = 24.dp, horizontal = 16.dp)
                    .testTag("upload_dropzone"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileUpload,
                    contentDescription = null,
                    tint = colors.green,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to upload",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "PDF, Word (.docx) or Text · multiple files OK",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick restore sample handbook button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.separator, RoundedCornerShape(12.dp))
                    .clickable { onReloadSampleHandbook() }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = null,
                    tint = colors.green,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Load Standard Sample Handbook",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        } else {
            // Employee view info card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.separator, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Employee Handbook (Synced from HR)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You are logged in as an employee. Document uploads and administrative changes are restricted. You can view all company policy documents and ask AI questions below.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        Text(
            text = "LOADED DOCUMENTS",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            letterSpacing = 0.04.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )


        if (docs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.separator, RoundedCornerShape(14.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(38.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No documents yet.\nUpload a policy file above.",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.separator, RoundedCornerShape(14.dp))
            ) {
                val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

                docs.forEachIndexed { index, doc ->
                    val ext = doc.name.substringAfterLast(".", "doc").take(3).uppercase()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.green),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ext,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (colors.isDark) Color.Black else Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${doc.chunks.size} passages · ${dateFormat.format(Date(doc.uploadedAt))}",
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                        }

                        IconButton(
                            onClick = { onPreviewDoc(doc) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RemoveRedEye,
                                contentDescription = "Preview",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (isHr) {
                            IconButton(
                                onClick = { onDeleteDoc(doc) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = colors.red,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (index < docs.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 58.dp)
                                .height(0.5.dp)
                                .background(colors.separator)
                        )
                    }
                }
            }

            if (isHr) {
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onWipeAllDocs,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surface2,
                        contentColor = colors.red
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, colors.separator, RoundedCornerShape(14.dp))
                ) {
                    Text(
                        text = "Remove All Documents",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.red
                    )
                }
            }
        }
    }
}

@Composable
private fun CompanyTab(
    company: CompanySettings,
    hrLoginEmail: String,
    hrLoginPassword: String,
    orgError: String,
    onSaveCompany: (String, String) -> Unit,
    onSaveHrCredentials: (currentPass: String, newEmail: String, newPass: String, confirmPass: String) -> Unit,
    onLogoPicked: (Uri) -> Unit,
    onRemoveLogo: () -> Unit,
    onFactoryReset: () -> Unit
) {
    val colors = com.example.ui.theme.AppTheme.colors
    var nameInput by remember(company.orgName) { mutableStateOf(company.orgName) }
    var emailInput by remember(company.hrMail) { mutableStateOf(company.hrMail) }

    var currentPassInput by remember { mutableStateOf("") }
    var newEmailInput by remember(hrLoginEmail) { mutableStateOf(hrLoginEmail) }
    var newPassInput by remember { mutableStateOf("") }
    var confirmPassInput by remember { mutableStateOf("") }

    var currentPassVisible by remember { mutableStateOf(false) }
    var newPassVisible by remember { mutableStateOf(false) }
    var confirmPassVisible by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onLogoPicked(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "LOGO",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            letterSpacing = 0.04.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface2)
                .border(1.dp, colors.separator, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface1)
                    .border(1.dp, colors.separator, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!company.orgLogoUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = company.orgLogoUri,
                        contentDescription = "Logo preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        photoPicker.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surface3,
                        contentColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.separator, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = if (company.orgLogoUri != null) "Replace Logo" else "Upload Logo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }

                if (company.orgLogoUri != null) {
                    Button(
                        onClick = onRemoveLogo,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surface3,
                            contentColor = colors.red
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.separator, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "Remove Logo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.red
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "PNG, JPG or WebP. The logo is displayed in the header, boot screen and chat bubbles.",
            fontSize = 13.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "DETAILS",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            letterSpacing = 0.04.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        // Org Name Field
        Text(
            text = "Organization name",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            placeholder = { Text("e.g. Acme Corp", color = colors.textPlaceholder) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface2,
                unfocusedContainerColor = colors.surface2,
                focusedBorderColor = colors.green,
                unfocusedBorderColor = colors.separator,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // HR Email Field
        Text(
            text = "HR email",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            placeholder = { Text("hr@company.com", color = colors.textPlaceholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface2,
                unfocusedContainerColor = colors.surface2,
                focusedBorderColor = colors.green,
                unfocusedBorderColor = colors.separator,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (orgError.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = orgError,
                fontSize = 13.sp,
                color = colors.red,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { onSaveCompany(nameInput, emailInput) },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.green,
                contentColor = if (colors.isDark) Color.Black else Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Save Details",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (colors.isDark) Color.Black else Color.White
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "HR LOGIN CREDENTIALS",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            letterSpacing = 0.04.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Text(
            text = "Securely update your HR login credentials. Enter your current password to authorize changes.",
            fontSize = 13.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // Current Password Field
        Text(
            text = "Current Password",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = currentPassInput,
            onValueChange = { currentPassInput = it },
            placeholder = { Text("Enter current password", color = colors.textPlaceholder) },
            singleLine = true,
            visualTransformation = if (currentPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { currentPassVisible = !currentPassVisible }) {
                    Icon(
                        imageVector = if (currentPassVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (currentPassVisible) "Hide password" else "Show password",
                        tint = colors.textSecondary
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface2,
                unfocusedContainerColor = colors.surface2,
                focusedBorderColor = colors.green,
                unfocusedBorderColor = colors.separator,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // New Email Field
        Text(
            text = "New HR Email (Optional)",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = newEmailInput,
            onValueChange = { newEmailInput = it },
            placeholder = { Text("hr@apex.com", color = colors.textPlaceholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface2,
                unfocusedContainerColor = colors.surface2,
                focusedBorderColor = colors.green,
                unfocusedBorderColor = colors.separator,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // New Password Field
        Text(
            text = "New Password (Optional)",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = newPassInput,
            onValueChange = { newPassInput = it },
            placeholder = { Text("Enter new password", color = colors.textPlaceholder) },
            singleLine = true,
            visualTransformation = if (newPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { newPassVisible = !newPassVisible }) {
                    Icon(
                        imageVector = if (newPassVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (newPassVisible) "Hide password" else "Show password",
                        tint = colors.textSecondary
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface2,
                unfocusedContainerColor = colors.surface2,
                focusedBorderColor = colors.green,
                unfocusedBorderColor = colors.separator,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm New Password Field
        Text(
            text = "Confirm New Password",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = confirmPassInput,
            onValueChange = { confirmPassInput = it },
            placeholder = { Text("Re-enter new password", color = colors.textPlaceholder) },
            singleLine = true,
            visualTransformation = if (confirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { confirmPassVisible = !confirmPassVisible }) {
                    Icon(
                        imageVector = if (confirmPassVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (confirmPassVisible) "Hide password" else "Show password",
                        tint = colors.textSecondary
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface2,
                unfocusedContainerColor = colors.surface2,
                focusedBorderColor = colors.green,
                unfocusedBorderColor = colors.separator,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { onSaveHrCredentials(currentPassInput, newEmailInput, newPassInput, confirmPassInput) },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.surface3,
                contentColor = colors.textPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, colors.separator, RoundedCornerShape(14.dp))
        ) {
            Text(
                text = "Update HR Credentials",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "FACTORY RESET",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.red,
            letterSpacing = 0.04.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Button(
            onClick = onFactoryReset,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.surface2,
                contentColor = colors.red
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, colors.red.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
        ) {
            Text(
                text = "Factory Reset Everything",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.red
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "The name and logo appear in the header and chat. Employees use the mail button to write to this HR email address.",
            fontSize = 13.sp,
            color = colors.textSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun ApiTab(
    apiSettings: ApiSettings,
    onAddApiKeys: (provider: com.example.data.ApiProvider, keysText: String, model: String, customUrl: String) -> Unit,
    onRemoveApiKey: (id: String) -> Unit,
    onResetExhaustedKeys: () -> Unit,
    onClearAllApiKeys: () -> Unit,
    onSaveLegacyApi: (key: String, url: String, model: String) -> Unit
) {
    val colors = com.example.ui.theme.AppTheme.colors
    var selectedProviderTab by remember { mutableStateOf(0) } // 0: Groq, 1: Gemini, 2: Custom / Legacy

    var groqKeysInput by remember { mutableStateOf("") }
    var groqModel by remember { mutableStateOf("llama-3.3-70b-versatile") }

    var geminiKeysInput by remember { mutableStateOf("") }
    var geminiModel by remember { mutableStateOf("gemini-3.5-flash") }

    var customKey by remember(apiSettings.legacyApiKey) { mutableStateOf(apiSettings.legacyApiKey) }
    var customUrl by remember(apiSettings.legacyApiUrl) { mutableStateOf(apiSettings.legacyApiUrl) }
    var customModel by remember(apiSettings.legacyModel) { mutableStateOf(apiSettings.legacyModel) }

    val totalKeys = apiSettings.keys.size + if (apiSettings.legacyApiKey.isNotBlank()) 1 else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "AI ENGINE WITH SILENT KEY FAILOVER",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            letterSpacing = 0.04.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Text(
            text = "Add your Groq and Gemini API keys below. If the current API key limit or quota is exhausted, the app automatically and silently shifts to the next available key without displaying any warning to the user.",
            fontSize = 13.5.sp,
            color = colors.textSecondary,
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Provider Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface2)
                .border(1.dp, colors.separator, RoundedCornerShape(10.dp))
                .padding(3.dp)
        ) {
            listOf("Groq API", "Gemini API", "Custom").forEachIndexed { index, name ->
                val isSelected = selectedProviderTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.surface1 else Color.Transparent)
                        .border(
                            if (isSelected) 1.dp else 0.dp,
                            if (isSelected) colors.separator.copy(alpha = 0.6f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedProviderTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) colors.textPrimary else colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedProviderTab) {
            0 -> {
                // Groq API Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface2)
                        .border(1.dp, colors.separator, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Add Groq API Keys",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.greenSoft)
                                .border(1.dp, colors.green.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "GROQ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.green
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Model",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant").forEach { m ->
                            val isSel = groqModel == m
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) colors.surface1 else colors.surface3)
                                    .border(1.dp, if (isSel) colors.green else colors.separator, RoundedCornerShape(8.dp))
                                    .clickable { groqModel = m }
                                    .padding(vertical = 7.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (m.contains("70b")) "Llama 3.3 70B" else "Llama 3.1 8B",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSel) colors.textPrimary else colors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "API Key(s)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = groqKeysInput,
                        onValueChange = { groqKeysInput = it },
                        placeholder = { Text("gsk_… (single or multiple keys, line/comma separated)", color = colors.textPlaceholder, fontSize = 13.sp) },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface1,
                            unfocusedContainerColor = colors.surface1,
                            focusedBorderColor = colors.green,
                            unfocusedBorderColor = colors.separator,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (groqKeysInput.isNotBlank()) {
                                onAddApiKeys(com.example.data.ApiProvider.GROQ, groqKeysInput, groqModel, "")
                                groqKeysInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.green,
                            contentColor = if (colors.isDark) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Add Groq Keys to Pool",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (colors.isDark) Color.Black else Color.White
                        )
                    }
                }
            }
            1 -> {
                // Gemini API Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface2)
                        .border(1.dp, colors.separator, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Add Gemini API Keys",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x224E8BFF))
                                .border(1.dp, Color(0x664E8BFF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "GEMINI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E60D4)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Model: $geminiModel (Auto-optimized)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "API Key(s)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = geminiKeysInput,
                        onValueChange = { geminiKeysInput = it },
                        placeholder = { Text("AIzaSy… (single or multiple keys, line/comma separated)", color = colors.textPlaceholder, fontSize = 13.sp) },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface1,
                            unfocusedContainerColor = colors.surface1,
                            focusedBorderColor = Color(0xFF1E60D4),
                            unfocusedBorderColor = colors.separator,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (geminiKeysInput.isNotBlank()) {
                                onAddApiKeys(com.example.data.ApiProvider.GEMINI, geminiKeysInput, geminiModel, "")
                                geminiKeysInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E60D4),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Add Gemini Keys to Pool",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
            2 -> {
                // Custom / OpenAI Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface2)
                        .border(1.dp, colors.separator, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Custom / OpenAI-Compatible Endpoint",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("API Key", fontSize = 12.5.sp, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customKey,
                        onValueChange = { customKey = it },
                        placeholder = { Text("sk-…", color = colors.textPlaceholder) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface1,
                            unfocusedContainerColor = colors.surface1,
                            focusedBorderColor = colors.green,
                            unfocusedBorderColor = colors.separator,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("API URL", fontSize = 12.5.sp, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface1,
                            unfocusedContainerColor = colors.surface1,
                            focusedBorderColor = colors.green,
                            unfocusedBorderColor = colors.separator,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Model", fontSize = 12.5.sp, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customModel,
                        onValueChange = { customModel = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface1,
                            unfocusedContainerColor = colors.surface1,
                            focusedBorderColor = colors.green,
                            unfocusedBorderColor = colors.separator,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { onSaveLegacyApi(customKey, customUrl, customModel) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.green,
                            contentColor = if (colors.isDark) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Save Custom Endpoint",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (colors.isDark) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Active Key Rotation Pool
        Text(
            text = "ACTIVE KEY ROTATION POOL (${totalKeys})",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            letterSpacing = 0.04.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        if (apiSettings.keys.isEmpty() && apiSettings.legacyApiKey.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.separator, RoundedCornerShape(14.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No custom API keys added yet.\nThe app is operating with the local semantic policy engine and any environment secrets.",
                    fontSize = 13.5.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.separator, RoundedCornerShape(14.dp))
            ) {
                apiSettings.keys.forEachIndexed { index, item ->
                    val badgeColor = when (item.provider) {
                        com.example.data.ApiProvider.GROQ -> colors.green
                        com.example.data.ApiProvider.GEMINI -> Color(0xFF1E60D4)
                        com.example.data.ApiProvider.OPENAI_CUSTOM -> Color(0xFFD97706)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = item.provider.name,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.maskedKey,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (item.isExhausted) Color(0xFFD97706) else colors.green)
                                )
                                Text(
                                    text = if (item.isExhausted) "Limit exhausted (auto-shifted)" else "Active in pool · ${item.model}",
                                    fontSize = 12.sp,
                                    color = if (item.isExhausted) Color(0xFFD97706) else colors.textSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = { onRemoveApiKey(item.id) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Remove key",
                                tint = colors.red,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    if (index < apiSettings.keys.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp)
                                .height(0.5.dp)
                                .background(colors.separator)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onResetExhaustedKeys,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surface2,
                        contentColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .border(1.dp, colors.separator, RoundedCornerShape(12.dp))
                ) {
                    Text("Reset Rate Limits", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                }

                Button(
                    onClick = onClearAllApiKeys,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surface2,
                        contentColor = colors.red
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .border(1.dp, colors.separator, RoundedCornerShape(12.dp))
                ) {
                    Text("Clear All Keys", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = colors.red)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Keys are securely stored on this device. If all configured API keys reach their rate limit, requests automatically and silently fall back to the built-in offline policy extractor without interruption.",
            fontSize = 12.5.sp,
            color = colors.textSecondary,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun AboutTab(
    currentUserEmail: String = "",
    currentUserRole: String = "EMPLOYEE",
    onLogout: () -> Unit = {}
) {
    val colors = com.example.ui.theme.AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Account & Session Card with Prominent Sign Out
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface2)
                .border(1.dp, colors.separator, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "ACCOUNT & SESSION",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                letterSpacing = 0.04.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Signed in as: ${currentUserEmail.ifEmpty { "Active User" }}",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            val isHr = currentUserRole.equals("HR", ignoreCase = true)
            Text(
                text = "Role: ${if (isHr) "HR Administrator" else "Employee"}",
                fontSize = 13.sp,
                color = if (isHr) colors.green else colors.textSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.red.copy(alpha = 0.15f),
                    contentColor = colors.red
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.red.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("about_signout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colors.red
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign Out of Account",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.red
                )
            }
        }

        Column {
            Text(
                text = "HOW IT WORKS",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                letterSpacing = 0.04.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            Text(
                text = "HR uploads company policy documents; everything stays on this device. Each question is matched to the best passages, and only the relevant sentences are shown as bullets. If nothing matches closely enough, it declines instead of guessing.",
                fontSize = 14.sp,
                color = colors.textPrimary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Column {
            Text(
                text = "PRIVACY",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                letterSpacing = 0.04.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            Text(
                text = "Documents, chat history, branding and settings live on this device only. The HR PIN is stored as a SHA-256 hash and only gates the settings screens on this device. If an AI key is added, just the matching passages and your question are sent to that API.",
                fontSize = 14.sp,
                color = colors.textPrimary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Column {
            Text(
                text = "VERSION",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                letterSpacing = 0.04.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            Text(
                text = "HR Assistant · v6.0 · Android Native Edition",
                fontSize = 14.sp,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun CloudTab(
    supabaseSettings: com.example.data.SupabaseSettings,
    onSaveConfig: (String, String, Boolean) -> Unit,
    onTestConnection: () -> Unit,
    onSyncNow: () -> Unit,
    onCopySql: () -> Unit,
    onCopyCleanupSql: () -> Unit = {},
    onWipeCloudData: () -> Unit = {}
) {
    val colors = com.example.ui.theme.AppTheme.colors
    var urlInput by remember(supabaseSettings.projectUrl) { mutableStateOf(supabaseSettings.projectUrl) }
    var keyInput by remember(supabaseSettings.anonKey) { mutableStateOf(supabaseSettings.anonKey) }
    var autoSync by remember(supabaseSettings.isAutoSyncEnabled) { mutableStateOf(supabaseSettings.isAutoSyncEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SUPABASE CLOUD SYNC & DB",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            letterSpacing = 0.04.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        Text(
            text = "Sync policy documents and user accounts between HR and employees in real-time.",
            fontSize = 13.5.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface2)
                .border(1.dp, colors.separator, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Supabase Project URL", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface1,
                    unfocusedContainerColor = colors.surface1,
                    focusedBorderColor = colors.green,
                    unfocusedBorderColor = colors.separator,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Supabase Anon API Key", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface1,
                    unfocusedContainerColor = colors.surface1,
                    focusedBorderColor = colors.green,
                    unfocusedBorderColor = colors.separator,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Auto-Sync", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                androidx.compose.material3.Switch(
                    checked = autoSync,
                    onCheckedChange = { autoSync = it }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { onSaveConfig(urlInput, keyInput, autoSync) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.green, contentColor = if (colors.isDark) Color.Black else Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Sync Now", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onTestConnection,
                colors = ButtonDefaults.buttonColors(containerColor = colors.surface2, contentColor = colors.textPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, colors.separator, RoundedCornerShape(10.dp))
            ) {
                Text("Test Connection", fontSize = 13.sp)
            }
            Button(
                onClick = onCopySql,
                colors = ButtonDefaults.buttonColors(containerColor = colors.surface2, contentColor = colors.green),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, colors.separator, RoundedCornerShape(10.dp))
            ) {
                Text("Copy Setup SQL", fontSize = 13.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onCopyCleanupSql,
                colors = ButtonDefaults.buttonColors(containerColor = colors.surface2, contentColor = colors.green),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, colors.separator, RoundedCornerShape(10.dp))
            ) {
                Text("Copy Cleanup SQL", fontSize = 13.sp)
            }
            Button(
                onClick = onWipeCloudData,
                colors = ButtonDefaults.buttonColors(containerColor = colors.surface2, contentColor = colors.red),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, colors.red.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            ) {
                Text("Wipe App Cloud Data", fontSize = 13.sp)
            }
        }
    }
}
