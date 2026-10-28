package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppModalDialog
import com.example.ui.components.BootScreen
import com.example.ui.components.ChatFeed
import com.example.ui.components.ComposerBar
import com.example.ui.components.SettingsSheet
import com.example.ui.components.TopHeader
import com.example.ui.theme.CanvasBlack
import com.example.ui.theme.HRAssistantTheme
import com.example.ui.theme.SurfaceDark3
import com.example.ui.theme.TextWhite
import com.example.viewmodel.HRViewModel
import com.example.viewmodel.ModalState

class MainActivity : ComponentActivity() {

    private val viewModel: HRViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            HRAssistantTheme(darkTheme = uiState.isDarkTheme) {
                val context = LocalContext.current
                var composerText by remember { mutableStateOf("") }

                BackHandler(enabled = uiState.isSettingsOpen || uiState.modalState != ModalState.None) {
                    if (uiState.modalState != ModalState.None) {
                        viewModel.dismissModal()
                    } else if (uiState.isSettingsOpen) {
                        viewModel.closeSettings()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(com.example.ui.theme.AppTheme.colors.canvasBg)
                ) {
                    if (!uiState.isLoggedIn) {
                        com.example.ui.components.AuthScreen(
                            authError = uiState.authError,
                            onLogin = { email, pass -> viewModel.login(email, pass) },
                            onSignUp = { username, email, pass -> viewModel.signUp(username, email, pass) }
                        )
                    } else {
                        // Main Chat Interface
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                        ) {
                            TopHeader(
                                orgName = uiState.companySettings.orgName,
                                logoUri = uiState.companySettings.orgLogoUri,
                                docsCount = uiState.docs.size,
                                statusText = uiState.statusText,
                                isDarkTheme = uiState.isDarkTheme,
                                currentUserRole = uiState.currentUserRole,
                                currentUserEmail = uiState.currentUserEmail,
                                isSyncing = uiState.isSyncing,
                                onSyncClick = { viewModel.syncWithSupabase(isManual = true) },
                                onToggleTheme = { viewModel.toggleTheme() },
                                onNewChatClick = { viewModel.requestNewChat() },
                                onMailClick = { viewModel.handleMailClick() },
                                onSettingsClick = { viewModel.openSettings() },
                                onLogout = { viewModel.logout() }
                            )

                            ChatFeed(
                                messages = uiState.chatHistory,
                                docs = uiState.docs,
                                orgName = uiState.companySettings.orgName,
                                logoUri = uiState.companySettings.orgLogoUri,
                                hrMail = uiState.companySettings.hrMail,
                                suggestions = uiState.suggestions,
                                isTyping = uiState.isTyping,
                                onSuggestionClick = { suggestion ->
                                    viewModel.sendMessage(suggestion)
                                },
                                onCopyAnswer = { title, bullets ->
                                    viewModel.copyAnswerToClipboard(context, title, bullets)
                                },
                                onEmailHrClick = { viewModel.handleMailClick() },
                                modifier = Modifier.weight(1f)
                            )

                            ComposerBar(
                                inputText = composerText,
                                onInputChange = { composerText = it },
                                onSend = { query ->
                                    composerText = ""
                                    viewModel.sendMessage(query)
                                },
                                isLoading = uiState.isTyping,
                                onNewChatClick = { viewModel.requestNewChat() }
                            )
                        }
                    }

                    // Toast message overlay
                    AnimatedVisibility(
                        visible = uiState.toastMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 56.dp)
                    ) {
                        uiState.toastMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(if (com.example.ui.theme.AppTheme.colors.isDark) Color(0xEB48484A) else Color(0xEB1C1C1E))
                                    .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(99.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = msg,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Boot Screen Overlay
                    AnimatedVisibility(
                        visible = uiState.isBooting,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        BootScreen(
                            orgName = uiState.companySettings.orgName,
                            logoUri = uiState.companySettings.orgLogoUri
                        )
                    }

                    // Settings Bottom Sheet
                    SettingsSheet(
                        isOpen = uiState.isSettingsOpen,
                        currentTab = uiState.currentTab,
                        docs = uiState.docs,
                        companySettings = uiState.companySettings,
                        apiSettings = uiState.apiSettings,
                        supabaseSettings = uiState.supabaseSettings,
                        currentUserRole = uiState.currentUserRole,
                        currentUserEmail = uiState.currentUserEmail,
                        hrLoginEmail = uiState.hrLoginEmail,
                        hrLoginPassword = uiState.hrLoginPassword,
                        orgError = uiState.orgError,
                        onClose = { viewModel.closeSettings() },
                        onTabSelected = { tab -> viewModel.selectTab(tab) },
                        onFilesPicked = { uris -> viewModel.handleFileUpload(uris, context) },
                        onReloadSampleHandbook = { viewModel.reloadSampleHandbook() },
                        onPreviewDoc = { doc -> viewModel.previewDoc(doc) },
                        onDeleteDoc = { doc -> viewModel.promptDeleteDoc(doc) },
                        onWipeAllDocs = { viewModel.promptWipeAllDocs() },
                        onSaveCompany = { name, email -> viewModel.saveCompanyDetails(name, email) },
                        onSaveHrCredentials = { currentPass, newEmail, newPass, confirmPass ->
                            viewModel.updateHrCredentials(currentPass, newEmail, newPass, confirmPass)
                        },
                        onLogoPicked = { uri -> viewModel.setCompanyLogo(uri) },
                        onRemoveLogo = { viewModel.removeCompanyLogo() },
                        onFactoryReset = { viewModel.factoryResetEverything() },
                        onAddApiKeys = { provider, keys, model, url -> viewModel.addApiKeys(provider, keys, model, url) },
                        onRemoveApiKey = { id -> viewModel.removeApiKey(id) },
                        onResetExhaustedKeys = { viewModel.resetExhaustedKeys() },
                        onClearAllApiKeys = { viewModel.clearAllApiKeys() },
                        onSaveLegacyApi = { k, u, m -> viewModel.saveApi(k, u, m) },
                        onSaveSupabaseConfig = { url, key, auto -> viewModel.saveSupabaseConfig(url, key, auto) },
                        onTestSupabaseConnection = { viewModel.testSupabaseConnection() },
                        onSyncSupabaseNow = { viewModel.syncWithSupabase(isManual = true) },
                        onCopySql = { viewModel.copySqlToClipboard(context) },
                        onCopyCleanupSql = { viewModel.copyCleanupSqlToClipboard(context) },
                        onWipeCloudData = { viewModel.wipeSupabaseCloudData() },
                        onLogout = { viewModel.logout() }
                    )


                    // iOS-style Alert / Action Sheet Modals
                    AppModalDialog(
                        modalState = uiState.modalState,
                        onDismiss = { viewModel.dismissModal() },
                        onConfirmClearChat = { viewModel.confirmClearChat() },
                        onConfirmDeleteDoc = {
                            val target = (uiState.modalState as? ModalState.ConfirmDeleteDoc)?.doc
                            if (target != null) viewModel.confirmDeleteDoc(target)
                        },
                        onConfirmWipeDocs = { viewModel.confirmWipeAllDocs() },
                        onOpenSettingsCompany = {
                            viewModel.openSettings()
                            viewModel.selectTab(1)
                        }
                    )
                }
            }
        }
    }
}
