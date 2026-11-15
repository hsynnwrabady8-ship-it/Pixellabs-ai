package com.example.ui.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import com.example.data.ImageLayer
import com.example.data.ShapeLayer
import com.example.data.StudioLayer
import com.example.data.TextLayer
import com.example.ui.AiTaskUiState

@Composable
fun FloatingPixelLabAgentBar(
    selectedLayer: StudioLayer?,
    totalLayersCount: Int,
    aiState: AiTaskUiState,
    agentHistoryCount: Int,
    canUndo: Boolean,
    isGoogleSignedIn: Boolean,
    googleUserEmail: String?,
    onSendAgentCommand: (String) -> Unit,
    onOpenAgentSheet: () -> Unit,
    onOpenDiagnosticsTab: () -> Unit,
    onUndoLastAction: () -> Unit,
    isAntiGravityActive: Boolean = false,
    onToggleAntiGravity: () -> Unit = {},
    onRequestAutoLayout: () -> Unit = {},
    selectedGeminiModel: String = GeminiModelPreset.GEMINI_3_8_FLASH.modelId,
    activeTransactionSteps: List<AgentExecutionStep> = emptyList(),
    onSelectGeminiModel: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var quickInput by remember { mutableStateOf("") }
    var inlineStepsExpanded by remember { mutableStateOf(true) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val isBusy = aiState is AiTaskUiState.Loading

    val palette = com.example.ui.theme.LocalStudioPalette.current

    val contextLabel = when (selectedLayer) {
        is TextLayer -> "لایه فعال: متن «${selectedLayer.text.take(14)}»"
        is ShapeLayer -> "لایه فعال: شکل (${selectedLayer.name})"
        is ImageLayer -> "لایه فعال: تصویر (${selectedLayer.name})"
        else -> "کل بوم ($totalLayersCount لایه)"
    }

    Surface(
        color = palette.surface.copy(alpha = 0.95f),
        tonalElevation = 10.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(palette.secondaryAccent, palette.primaryAccent, palette.secondaryAccent)
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("floating_agent_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Extended Floating Action Button (FAB) at top
                ExtendedFloatingActionButton(
                    onClick = onOpenAgentSheet,
                    containerColor = Color(0xFF9333EA),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("floating_agent_fab")
                ) {
                    BadgedBox(
                        badge = {
                            if (agentHistoryCount > 0) {
                                Badge(
                                    containerColor = Color(0xFF38BDF8),
                                    contentColor = Color(0xFF04131D)
                                ) {
                                    Text("$agentHistoryCount")
                                }
                            }
                        }
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "پنل چت ایجنت هوشمند",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "چت ایجنت",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Command-Line Style Prompt Input Box (`>_`)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF070B12))
                        .border(
                            1.dp,
                            if (isBusy) palette.primaryAccent else palette.border,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = ">_",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = palette.primaryAccent
                        )
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (quickInput.isEmpty()) {
                            Text(
                                text = if (isBusy) {
                                    (aiState as? AiTaskUiState.Loading)?.statusText ?: "ایجنت در حال اجرای زنجیره ابزارها..."
                                } else {
                                    "دستور طراحی به ایجنت (${formatModelShortBadge(selectedGeminiModel)})..."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBusy) palette.primaryAccent else palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = quickInput,
                            onValueChange = { quickInput = it },
                            enabled = !isBusy,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = palette.textPrimary,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(palette.primaryAccent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (quickInput.isNotBlank() && !isBusy) {
                                        val cmd = quickInput
                                        quickInput = ""
                                        focusManager.clearFocus()
                                        onSendAgentCommand(cmd)
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("floating_agent_quick_input")
                        )
                    }

                    // Inline Model Selector Chip inside Command-Line Bar
                    Box {
                        Surface(
                            color = Color(0xFF141D2E),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, palette.primaryAccent.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clickable { modelMenuExpanded = true }
                                .testTag("agent_bar_model_badge")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = formatModelShortBadge(selectedGeminiModel),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryAccent
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = palette.primaryAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false },
                            modifier = Modifier.background(palette.surfaceElevated)
                        ) {
                            GeminiModelPreset.entries.forEach { preset ->
                                val isSel = selectedGeminiModel.equals(preset.modelId, ignoreCase = true)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = preset.titleFa,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) palette.primaryAccent else palette.textPrimary
                                        )
                                    },
                                    onClick = {
                                        onSelectGeminiModel(preset.modelId)
                                        modelMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Send Button
                IconButton(
                    onClick = {
                        if (quickInput.isNotBlank() && !isBusy) {
                            val cmd = quickInput
                            quickInput = ""
                            focusManager.clearFocus()
                            onSendAgentCommand(cmd)
                        } else {
                            onOpenAgentSheet()
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (quickInput.isNotBlank()) Color(0xFFA855F7) else Color(0xFF1E293B)
                        )
                        .testTag("floating_agent_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "ارسال دستور به ایجنت",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Google & Diagnostics Status Pill
                IconButton(
                    onClick = onOpenDiagnosticsTab,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isGoogleSignedIn) Color(0xFF064E3B) else Color(0xFF1E293B))
                        .testTag("floating_agent_diagnostics_button")
                ) {
                    Icon(
                        imageVector = if (isGoogleSignedIn) Icons.Default.CloudDone else Icons.Default.NetworkCheck,
                        contentDescription = "تست اتصال و لاگ ایجنت",
                        tint = if (isGoogleSignedIn) Color(0xFF34D399) else Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Bottom subtle row
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1B4B))
                        .clickable { onOpenAgentSheet() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "دید بصری فعال 👁️ • $contextLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC4B5FD),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickPrompt = when (selectedLayer) {
                        is TextLayer -> "طرح این متن رو نقد کن و با فونت لاله‌زار، استایل طلایی سه‌بعدی و سایه زمینی ارتقا بده"
                        is ShapeLayer -> "این شکل رو سه‌بعدی با گرادیان بنفش و فیروزه‌ای و پرسپکتیو جذاب کن"
                        else -> "طرح فعلی بوم رو نقد بصری کن و با تایپوگرافی سه‌بعدی و استیکر بهبود بده"
                    }
                    val quickLabel = when (selectedLayer) {
                        is TextLayer -> "✨ نقد و ارتقای متن"
                        is ShapeLayer -> "✨ شکل سه‌بعدی جذاب"
                        else -> "✨ نقد و بهینه‌سازی بوم"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .clickable(enabled = !isBusy) {
                                onSendAgentCommand(quickPrompt)
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = quickLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    // Smart Auto-Layout & Animated Preview Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F2B3B))
                            .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable(enabled = !isBusy) {
                                onRequestAutoLayout()
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("auto_layout_agent_chip")
                    ) {
                        Text(
                            text = "📐 چیدمان هوشمند",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Anti-Gravity Agent Zero-G Mode Toggle Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAntiGravityActive) Color(0xFF4C1D95) else Color(0xFF1E293B))
                            .border(
                                1.dp,
                                if (isAntiGravityActive) Color(0xFF38BDF8) else Color(0xFF334155),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = !isBusy) {
                                onToggleAntiGravity()
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("anti_gravity_agent_chip")
                    ) {
                        Text(
                            text = if (isAntiGravityActive) "🛸 جاذبه صفر (فعال)" else "🛸 آنتی‌گرویتی",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAntiGravityActive) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                            fontWeight = if (isAntiGravityActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    AnimatedVisibility(visible = canUndo) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF311042))
                                .clickable { onUndoLastAction() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "بازگردانی",
                                    tint = Color(0xFFF472B6),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "بازگردانی",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF472B6)
                                )
                            }
                        }
                    }
                }
            }

            // Inline Expandable Reasoning Card inside Command-Line Bar
            AnimatedVisibility(visible = activeTransactionSteps.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    AgentExecutionStepsCard(
                        steps = activeTransactionSteps,
                        expanded = inlineStepsExpanded,
                        onToggleExpanded = { inlineStepsExpanded = !inlineStepsExpanded }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PixelLabAgentModalSheet(
    selectedLayer: StudioLayer?,
    messages: List<AgentChatMessage>,
    aiState: AiTaskUiState,
    canUndo: Boolean,
    isGoogleSignedIn: Boolean,
    googleUserEmail: String?,
    logEntries: List<AgentLogEntry>,
    diagnosticsReport: FullDiagnosticsReport,
    initialTab: Int = 0,
    selectedGeminiModel: String = GeminiModelPreset.GEMINI_3_8_FLASH.modelId,
    activeTransactionSteps: List<AgentExecutionStep> = emptyList(),
    pendingAgentTransaction: PendingAgentTransaction? = null,
    lastGeminiError: String? = null,
    onSelectGeminiModel: (String) -> Unit = {},
    onConfirmTransaction: () -> Unit = {},
    onRollbackTransaction: () -> Unit = {},
    onDismissGeminiError: () -> Unit = {},
    onSendCommand: (String, Boolean) -> Unit,
    onUndoAction: () -> Unit,
    onClearHistory: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onRunDiagnostics: () -> Unit,
    onClearLogs: () -> Unit,
    onExportLogs: () -> Unit,
    onShareLogs: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputPrompt by remember { mutableStateOf("") }
    var useCanvasVision by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var logFilterLevel by remember { mutableStateOf<LogLevel?>(null) }
    var showBeforeInPreview by remember { mutableStateOf(false) }
    var stepsExpanded by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()
    val isBusy = aiState is AiTaskUiState.Loading

    LaunchedEffect(messages.size, pendingAgentTransaction?.transactionId) {
        if (messages.isNotEmpty() && selectedTab == 0) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF111622),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFA855F7), Color(0xFF38BDF8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ایجنت هوشمند Gemini",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color(0xFF1E1B4B),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFA855F7)),
                                modifier = Modifier.clickable { selectedTab = 1 }
                            ) {
                                Text(
                                    text = selectedGeminiModel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD8B4FE),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isGoogleSignedIn) {
                                "Function Calling فعال (۱۸ ابزار واقعی) • ${googleUserEmail ?: ""}"
                            } else {
                                "Function Calling فعال (۱۸ ابزار واقعی ویرایشگر)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isGoogleSignedIn) Color(0xFF34D399) else Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن پنل ایجنت",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two Tabs: Tab 0 (Chat, Steps & Preview) | Tab 1 (Model Selector, Google Auth & Log File)
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF141926),
                contentColor = Color(0xFF38BDF8)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "چت، مراحل و پیش‌نمایش",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "انتخاب مدل، اتصال و لاگ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gemini Error Banner (visible across tabs if any error occurred)
            AnimatedVisibility(visible = !lastGeminiError.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("gemini_error_banner"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFF87171))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "خطای Gemini",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = lastGeminiError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFECACA),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onDismissGeminiError,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن خطا",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: CHAT, LIVE EXECUTION STEPS & TRANSACTION PREVIEW
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF172033))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (useCanvasVision) Color(0xFF38BDF8) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "دید بصری زنده از بوم",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (useCanvasVision) Color.White else Color(0xFF94A3B8)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0C4A6E))
                                    .clickable(enabled = !isBusy) {
                                        onSendCommand("طرح فعلی بوم را از نظر زیبایی‌شناسی، رنگ و کنتراست نقد کن و پیشنهادهای خود را برای زیباتر شدنش اعمال کن.", true)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "🎯 نقد و اصلاح طرح",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE0F2FE),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Switch(
                                checked = useCanvasVision,
                                onCheckedChange = { useCanvasVision = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF0284C7)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Contextual Suggestions Row
                    val contextSuggestions = remember(selectedLayer) {
                        when (selectedLayer) {
                            is TextLayer -> listOf(
                                "پروژه و لایه‌ها را با inspect_document بررسی کن",
                                "این متن رو طلایی سه‌بعدی با عمق ۳۲ و سایه زمینی کن",
                                "فونت متن رو لاله‌زار کن و وسط‌بوم تراز کن",
                                "به متن گرادیان نئون بنفش و فیروزه‌ای و بافت کروم بده",
                                "استایل این پروژه رو یاد بگیر"
                            )
                            is ShapeLayer -> listOf(
                                "این شکل رو سه‌بعدی و با گرادیان آتشین کن",
                                "پرسپکتیو و کج‌نمایی Skew X به این شکل بده",
                                "یک تیتر سه‌بعدی جذاب روی این شکل قرار بده",
                                "لایه شکل رو وسط بوم تراز کن"
                            )
                            is ImageLayer -> listOf(
                                "یک تیتر تبلیغاتی سه‌بعدی و برچسب تخفیف بدون تغییر تصویر محصول اضافه کن",
                                "به لایه تصویر دورخط سفید و سایه نرم بده",
                                "یک زیرعنوان جذاب پایین تصویر محصول اضافه کن"
                            )
                            else -> listOf(
                                "یک پوستر تبلیغاتی حرفه‌ای با تیتر ۳بعدی و زیرعنوان بساز",
                                "طرح فعلی را بررسی کن و لایه‌ها را وسط‌چین و متوازن کن",
                                "پروژه و لایه‌ها را با inspect_document بررسی کن",
                                "استایل این پروژه رو یاد بگیر"
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(contextSuggestions) { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1B4B))
                                    .border(1.dp, Color(0xFF7E22CE), RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isBusy) {
                                        onSendCommand(suggestion, useCanvasVision)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "⚡ $suggestion",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE9D5FF)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Messages + Live Execution Steps + Transaction Preview List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        if (messages.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF172033)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "👋 دستیار هوشمند طراحی Gemini (Function Calling)",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "ایجنت به ۱۸ ابزار واقعی ویرایشگر (inspect_document، add_text_layer، set_layer_style، align_layers، render_preview، validate_document، undo_transaction و ...) متصل است و بدون دست زدن به تصویر اصلی محصول، عملیات را در قالب تراکنش قابل تأیید یا بازگشت اجرا می‌کند.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }

                        items(messages, key = { it.id }) { msg ->
                            UpgradedAgentMessageBubble(
                                message = msg,
                                canUndo = canUndo,
                                onUndo = onRollbackTransaction,
                                onApplySuggestion = { onSendCommand(it, useCanvasVision) }
                            )
                        }

                        // Live Execution Steps Trace Card (مراحل انجام کار ایجنت)
                        if (activeTransactionSteps.isNotEmpty()) {
                            item {
                                AgentExecutionStepsCard(
                                    steps = activeTransactionSteps,
                                    expanded = stepsExpanded,
                                    onToggleExpanded = { stepsExpanded = !stepsExpanded }
                                )
                            }
                        }

                        // Pending Transaction Preview + Confirm / Rollback Card
                        if (pendingAgentTransaction != null) {
                            item {
                                PendingTransactionPreviewCard(
                                    transaction = pendingAgentTransaction,
                                    showBefore = showBeforeInPreview,
                                    onToggleBeforeAfter = { showBeforeInPreview = !showBeforeInPreview },
                                    onConfirm = onConfirmTransaction,
                                    onRollback = onRollbackTransaction
                                )
                            }
                        }

                        if (aiState is AiTaskUiState.Loading) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1065)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = Color(0xFFD8B4FE),
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = aiState.statusText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputPrompt,
                            onValueChange = { inputPrompt = it },
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("agent_sheet_input"),
                            placeholder = {
                                Text("درخواست فارسی خود را برای ایجنت Gemini بنویسید...")
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFA855F7),
                                unfocusedBorderColor = Color(0xFF2E3A4F),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Button(
                            onClick = {
                                if (inputPrompt.isNotBlank() && !isBusy) {
                                    val cmd = inputPrompt
                                    inputPrompt = ""
                                    onSendCommand(cmd, useCanvasVision)
                                }
                            },
                            enabled = inputPrompt.isNotBlank() && !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFA855F7),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("agent_sheet_send_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "ارسال")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اجرا", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                1 -> {
                    // TAB 1: MODEL SELECTION, GOOGLE CONNECTION, DIAGNOSTICS & LOG FILE
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // 0. Configurable Gemini Model Selector Card
                        item {
                            var customModelInput by remember(selectedGeminiModel) {
                                mutableStateOf(selectedGeminiModel)
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gemini_model_selector_card"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF172033)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "انتخاب مدل هوش مصنوعی Gemini",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "فعال: $selectedGeminiModel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF34D399),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    GeminiModelPreset.entries.forEach { preset ->
                                        val isSelected = selectedGeminiModel.equals(preset.modelId, ignoreCase = true)
                                        Surface(
                                            color = if (isSelected) Color(0xFF0C4A6E) else Color(0xFF0F172A),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clickable {
                                                    customModelInput = preset.modelId
                                                    onSelectGeminiModel(preset.modelId)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = preset.titleFa,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = preset.subtitleFa,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "انتخاب‌شده",
                                                        tint = Color(0xFF38BDF8),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = customModelInput,
                                            onValueChange = { customModelInput = it },
                                            placeholder = {
                                                Text(
                                                    "شناسه مدل دلخواه (مثلاً gemini-3.8-flash)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("custom_gemini_model_input"),
                                            singleLine = true,
                                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF38BDF8),
                                                unfocusedBorderColor = Color(0xFF1E293B),
                                                focusedContainerColor = Color(0xFF0B0E14),
                                                unfocusedContainerColor = Color(0xFF0B0E14)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                if (customModelInput.isNotBlank()) {
                                                    onSelectGeminiModel(customModelInput.trim())
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                        ) {
                                            Text("ثبت مدل", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // 1. Google Account Connection Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isGoogleSignedIn) Color(0xFF064E3B) else Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isGoogleSignedIn) Icons.Default.CloudDone else Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = if (isGoogleSignedIn) Color(0xFF34D399) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isGoogleSignedIn) "حساب گوگل متصل است" else "اتصال به حساب گوگل (اختیاری)",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isGoogleSignedIn) (googleUserEmail ?: "کاربر احراز هویت شده") else "برای پشتیبان‌گیری خودکار پروژه‌ها و همگام‌سازی",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isGoogleSignedIn) Color(0xFFA7F3D0) else Color(0xFFCBD5E1),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (isGoogleSignedIn) {
                                        OutlinedButton(
                                            onClick = onGoogleSignOut,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5))
                                        ) {
                                            Text("خروج", style = MaterialTheme.typography.labelSmall)
                                        }
                                    } else {
                                        Button(
                                            onClick = onGoogleSignIn,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                        ) {
                                            Text("ورود با گوگل", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // 1.5 Security & Connection Failover Card (No hardcoded keys in APK)
                        item {
                            val context = LocalContext.current
                            var customKeyInput by remember { mutableStateOf(PixelLabAgentEngine.getCustomApiKey(context)) }
                            var keySavedMessage by remember { mutableStateOf(customKeyInput.isNotBlank()) }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFF7E22CE))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFFA855F7),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "امنیت اتصال بدون کلید ثابت در کد/APK",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "هیچ کلید ثابتی داخل سورس‌کد یا APK قرار ندارد. اتصال به صورت پیش‌فرض از طریق Firebase AI Logic و گواهی App Check انجام می‌شود و در صورت قطع شبکه یا محدودیت درخواست، خطا را مدیریت کرده و از طریق ۱۸ ابزار واقعی عملیات را کامل می‌کند.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFDDD6FE)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = customKeyInput,
                                            onValueChange = {
                                                customKeyInput = it
                                                keySavedMessage = false
                                            },
                                            placeholder = {
                                                Text("کلید API شخصی در حافظه امن دستگاه (اختیاری)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                            },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFFA855F7),
                                                unfocusedBorderColor = Color(0xFF4C1D95),
                                                focusedContainerColor = Color(0xFF140E2D),
                                                unfocusedContainerColor = Color(0xFF140E2D)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                PixelLabAgentEngine.setCustomApiKey(context, customKeyInput)
                                                keySavedMessage = true
                                                AgentDiagnosticsLogger.info(
                                                    tag = "CustomApiKey",
                                                    message = if (customKeyInput.isNotBlank()) "Custom Gemini API key saved" else "Custom Gemini API key cleared"
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                                        ) {
                                            Text("ذخیره", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    if (keySavedMessage) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (customKeyInput.isNotBlank()) {
                                                "✓ کلید API در حافظه خصوصی دستگاه ذخیره شد."
                                            } else {
                                                "✓ اتصال ابری امن از طریق Firebase AI Logic فعال است."
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF4ADE80)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Live Connection Diagnostics Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141D2B)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.NetworkCheck,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "بررسی زنده وضعیت اتصال (Live Diagnostics)",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Button(
                                            onClick = onRunDiagnostics,
                                            enabled = !diagnosticsReport.isRunning,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            if (diagnosticsReport.isRunning) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    color = Color(0xFF04131D),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("در حال بررسی...", style = MaterialTheme.typography.labelSmall, color = Color(0xFF04131D))
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = null,
                                                    tint = Color(0xFF04131D),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("اجرای تست", style = MaterialTheme.typography.labelSmall, color = Color(0xFF04131D), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 4 Diagnostic Steps Display
                                    DiagnosticStepRow(diagnosticsReport.googleAuthStep)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    DiagnosticStepRow(diagnosticsReport.appCheckStep)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    DiagnosticStepRow(diagnosticsReport.geminiPingStep)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    DiagnosticStepRow(diagnosticsReport.firestoreStep)
                                }
                            }
                        }

                        // 3. Persistent Log File Management & Live Terminal
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D111A)),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283246))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "فایل لاگ ایجنت (pixellab_agent.log)",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${logEntries.size} ردیف رویداد در حافظه دستگاه ثبت شده است",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(
                                                onClick = onShareLogs,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "اشتراک‌گذاری لاگ",
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = onExportLogs,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FileDownload,
                                                    contentDescription = "ذخیره فایل log",
                                                    tint = Color(0xFF34D399),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = onClearLogs,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteSweep,
                                                    contentDescription = "پاک‌سازی لاگ",
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Filter Chips: ALL, INFO, WARN, ERROR
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(
                                            null to "همه (${logEntries.size})",
                                            LogLevel.INFO to "اطلاعات",
                                            LogLevel.WARN to "هشدار",
                                            LogLevel.ERROR to "خطا"
                                        ).forEach { (level, label) ->
                                            val isSel = logFilterLevel == level
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) Color(0xFF38BDF8) else Color(0xFF1E293B))
                                                    .clickable { logFilterLevel = level }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSel) Color(0xFF04131D) else Color(0xFFCBD5E1),
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Monospace Terminal Box
                                    val filteredLogs = remember(logEntries, logFilterLevel) {
                                        if (logFilterLevel == null) logEntries else logEntries.filter { it.level == logFilterLevel }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF06090E))
                                            .border(1.dp, Color(0xFF1F293D), RoundedCornerShape(10.dp))
                                            .padding(8.dp)
                                    ) {
                                        if (filteredLogs.isEmpty()) {
                                            Text(
                                                text = "هیچ لاگی در این دسته‌بندی وجود ندارد.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF64748B)
                                            )
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                items(filteredLogs.reversed(), key = { it.id }) { log ->
                                                    val color = when (log.level) {
                                                        LogLevel.INFO -> Color(0xFF94A3B8)
                                                        LogLevel.WARN -> Color(0xFFFBBF24)
                                                        LogLevel.ERROR -> Color(0xFFF87171)
                                                    }
                                                    Text(
                                                        text = log.toFormattedString(),
                                                        style = TextStyle(
                                                            color = color,
                                                            fontSize = 11.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            lineHeight = 15.sp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticStepRow(step: DiagnosticStepResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B0E14))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            when (step.status) {
                DiagnosticStatus.RUNNING -> CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Color(0xFF38BDF8),
                    strokeWidth = 2.dp
                )
                DiagnosticStatus.PASSED -> Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(16.dp)
                )
                DiagnosticStatus.FAILED -> Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(16.dp)
                )
                else -> Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF475569))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (step.message.isNotBlank()) {
                    Text(
                        text = step.message,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (step.latencyMs != null) {
            Text(
                text = "${step.latencyMs} ms",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpgradedAgentMessageBubble(
    message: AgentChatMessage,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onApplySuggestion: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) Color(0xFF0C4A6E) else Color(0xFF1A2234)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header with Telemetry Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (message.isUser) "شما" else "ایجنت هوشمند پیکسل‌لب",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.isUser) Color(0xFF38BDF8) else Color(0xFFD8B4FE),
                        fontWeight = FontWeight.Bold
                    )

                    if (!message.isUser && message.latencyMs != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (message.usedCanvasVision) {
                                Text(
                                    text = "دید بصری 👁️",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "${message.modelName} • ${message.latencyMs}ms",
                                style = TextStyle(fontSize = 10.sp, color = Color(0xFF94A3B8))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                // Optional Design Critique Card
                if (!message.isUser && !message.designCritique.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2942)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🎨 نقد و تحلیل بصری هوش مصنوعی:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = message.designCritique,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }

                // Executed Canvas Actions Badges
                if (!message.isUser && message.executedActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "اقدامات اعمال‌شده روی بوم (${message.executedActions.size}):",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        message.executedActions.forEach { actionBadge ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF064E3B))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = actionBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                        }
                    }

                    if (message.canUndo && canUndo) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onUndo,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = null,
                                tint = Color(0xFFF472B6),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "بازگردانی این تغییرات",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF472B6)
                            )
                        }
                    }
                }

                // Dynamic Follow-up Suggestions Chips
                if (!message.isUser && message.followUpSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "پیشنهادهای هوشمند برای گام بعدی:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC084FC),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        message.followUpSuggestions.forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2E1065))
                                    .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(8.dp))
                                    .clickable { onApplySuggestion(suggestion) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "💡 $suggestion",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF3E8FF)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgentExecutionStepsCard(
    steps: List<AgentExecutionStep>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("agent_execution_steps_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B111E)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🧠 زنجیره استدلال و مراحل انجام کار ایجنت (${steps.size} گام)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (expanded) "بستن ▲" else "مشاهده ▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(6.dp))
                steps.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141D2F))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            when (step.status) {
                                AgentStepStatus.RUNNING -> CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color(0xFF38BDF8),
                                    strokeWidth = 2.dp
                                )
                                AgentStepStatus.COMPLETED -> Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(15.dp)
                                )
                                AgentStepStatus.FAILED -> Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(15.dp)
                                )
                                AgentStepStatus.ROLLED_BACK -> Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${step.titleFa}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = step.toolName,
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFC084FC)
                                        )
                                    )
                                }
                                if (step.resultSummaryFa.isNotBlank()) {
                                    Text(
                                        text = step.resultSummaryFa,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (step.durationMs > 0) {
                            Text(
                                text = "${step.durationMs}ms",
                                style = TextStyle(fontSize = 10.sp, color = Color(0xFF38BDF8))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingTransactionPreviewCard(
    transaction: PendingAgentTransaction,
    showBefore: Boolean,
    onToggleBeforeAfter: () -> Unit,
    onConfirm: () -> Unit,
    onRollback: () -> Unit
) {
    var diffSplitFraction by remember(transaction.transactionId) {
        mutableFloatStateOf(if (showBefore) 0f else 0.5f)
    }
    val report = transaction.validationReport
    val afterBmp = transaction.previewBitmap
    val beforeBmp = transaction.beforePreviewBitmap

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_transaction_preview_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C31)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFF34D399))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🖼️ پیش‌نمایش مقایسه‌ای قبل و بعد (Canvas Diff)",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "مدل: ${transaction.modelUsed} • ${transaction.steps.size} ابزار اجرا شد",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    color = if (report.scoreOutOf100 >= 80) Color(0xFF064E3B) else Color(0xFF78350F),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (report.scoreOutOf100 >= 80) Color(0xFF34D399) else Color(0xFFFBBF24)
                    )
                ) {
                    Text(
                        text = "امتیاز اعتبارسنجی: ${report.scoreOutOf100}/۱۰۰",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (afterBmp != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF090D16))
                        .border(1.dp, Color(0xFF283246), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (beforeBmp != null && !showBefore) {
                        // Split-Screen Before / After Wipe Comparison
                        // Base layer: BEFORE bitmap
                        Image(
                            bitmap = beforeBmp.asImageBitmap(),
                            contentDescription = "قبل از تغییرات",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(6.dp)
                        )
                        // Overlay layer: AFTER bitmap clipped by diffSplitFraction
                        Image(
                            bitmap = afterBmp.asImageBitmap(),
                            contentDescription = "بعد از تغییرات",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(6.dp)
                                .drawWithContent {
                                    clipRect(
                                        left = 0f,
                                        top = 0f,
                                        right = size.width * diffSplitFraction.coerceIn(0f, 1f),
                                        bottom = size.height
                                    ) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                        )
                    } else {
                        val singleBmp = if (showBefore && beforeBmp != null) beforeBmp else afterBmp
                        Image(
                            bitmap = singleBmp.asImageBitmap(),
                            contentDescription = "پیش‌نمایش بوم",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(6.dp)
                        )
                    }

                    if (beforeBmp != null) {
                        Surface(
                            color = Color(0xCC0F172A),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clickable {
                                    onToggleBeforeAfter()
                                    diffSplitFraction = if (!showBefore) 0f else 1f
                                }
                        ) {
                            Text(
                                text = if (showBefore) "👁️ قبل از تغییر" else "⚡ مقایسه زنده (${(diffSplitFraction * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Interactive Before / After Diff Slider
                if (beforeBmp != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "قبل",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = diffSplitFraction,
                            onValueChange = { diffSplitFraction = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF34D399),
                                activeTrackColor = Color(0xFF34D399),
                                inactiveTrackColor = Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .testTag("agent_diff_comparison_slider")
                        )
                        Text(
                            text = "بعد",
                            fontSize = 11.sp,
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "✓ ${report.summaryFa}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA7F3D0)
            )
            report.autoFixesFa.take(2).forEach { fix ->
                Text(
                    text = "✨ $fix",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFBAE6FD)
                )
            }
            report.issuesFa.take(2).forEach { warn ->
                Text(
                    text = "⚠️ $warn",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFDE68A)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRollback,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFF87171)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("agent_rollback_transaction_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "لغو و بازگشت",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "لغو و بازگشت",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color(0xFF04131D)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("agent_confirm_transaction_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "تأیید تغییرات",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تأیید تغییرات",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


