package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BUILTIN_PIXELLAB_QUOTES
import com.example.data.ExportFormat
import com.example.data.ExportQualityPreset
import com.example.data.Point2D
import com.example.data.ShapeLayer
import com.example.data.TextLayer
import com.example.ui.agent.FloatingPixelLabAgentBar
import com.example.ui.agent.PixelLabAgentModalSheet
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.canvas.StudioCanvasRenderer
import com.example.ui.components.CanvasBackgroundInspector
import com.example.ui.components.GeminiAiStudioInspector
import com.example.ui.components.InlineCanvasPropertyChips
import com.example.ui.components.LayerDrawerSheet
import com.example.ui.components.ObjectsAndDrawInspector
import com.example.ui.components.PixelLabCanvasBgTabIcon
import com.example.ui.components.PixelLabHexagonTabIcon
import com.example.ui.components.PixelLabPresetsTabIcon
import com.example.ui.components.PixelLabTextTabIcon
import com.example.ui.components.PixelLabWandTabIcon
import com.example.ui.components.PostFxInspector
import com.example.ui.components.PresetsAndPlpInspector
import com.example.ui.components.StudioBottomControls
import com.example.ui.components.StudioTopBar
import com.example.ui.components.TextLayerInspector
import kotlin.math.hypot

@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    isUserSignedIn: Boolean = false,
    userEmailOrName: String? = null,
    onAuthSuccess: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val project by viewModel.project.collectAsStateWithLifecycle()
    val selectedLayerId by viewModel.selectedLayerId.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val showGrid by viewModel.showGrid.collectAsStateWithLifecycle()
    val isDrawingMode by viewModel.isDrawingMode.collectAsStateWithLifecycle()
    val penColorHex by viewModel.penColorHex.collectAsStateWithLifecycle()
    val penWidth by viewModel.penWidth.collectAsStateWithLifecycle()
    val penNeonGlow by viewModel.penNeonGlow.collectAsStateWithLifecycle()
    val penDashed by viewModel.penDashed.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val cloudState by viewModel.cloudProjectsState.collectAsStateWithLifecycle()
    val localPlpFiles by viewModel.localPlpFiles.collectAsStateWithLifecycle()
    val learnedPlpStyles by viewModel.learnedPlpStyles.collectAsStateWithLifecycle()
    val activeLearnedStyle by viewModel.activeLearnedStyle.collectAsStateWithLifecycle()
    val customUserFonts by viewModel.customUserFonts.collectAsStateWithLifecycle()
    val logEntries by viewModel.logEntries.collectAsStateWithLifecycle()
    val diagnosticsReport by viewModel.diagnosticsReport.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val agentMessages by viewModel.agentMessages.collectAsStateWithLifecycle()
    val selectedGeminiModel by viewModel.selectedGeminiModel.collectAsStateWithLifecycle()
    val activeTransactionSteps by viewModel.activeTransactionSteps.collectAsStateWithLifecycle()
    val pendingAgentTransaction by viewModel.pendingAgentTransaction.collectAsStateWithLifecycle()
    val activeAiMutatedLayerId by viewModel.activeAiMutatedLayerId.collectAsStateWithLifecycle()
    val lastGeminiError by viewModel.lastGeminiError.collectAsStateWithLifecycle()
    val isAntiGravityActive by viewModel.isAntiGravityActive.collectAsStateWithLifecycle()
    val layerFloatingOffsets by viewModel.layerFloatingOffsets.collectAsStateWithLifecycle()
    val cosmicParticles by viewModel.cosmicParticles.collectAsStateWithLifecycle()
    val activePreviewState by viewModel.activePreviewState.collectAsStateWithLifecycle()
    val isReelsSafeZoneVisible by viewModel.isReelsSafeZoneVisible.collectAsStateWithLifecycle()
    val savedUserTextPresets by viewModel.savedUserTextPresets.collectAsStateWithLifecycle()
    val smartPaletteHexes by viewModel.smartPaletteHexes.collectAsStateWithLifecycle()
    val isEyedropperActive by viewModel.isEyedropperActive.collectAsStateWithLifecycle()
    val hasCopiedLayerStyle by viewModel.hasCopiedLayerStyle.collectAsStateWithLifecycle()
    val canvasZoomScale by viewModel.canvasZoomScale.collectAsStateWithLifecycle()
    val canvasPanOffset by viewModel.canvasPanOffset.collectAsStateWithLifecycle()
    val isZoomPanModeActive by viewModel.isZoomPanModeActive.collectAsStateWithLifecycle()
    val isEraserMode by viewModel.isEraserMode.collectAsStateWithLifecycle()
    val eraserBrushSize by viewModel.eraserBrushSize.collectAsStateWithLifecycle()

    var showLayerDrawer by remember { mutableStateOf(false) }
    var showAgentSheet by remember { mutableStateOf(false) }
    var agentSheetInitialTab by remember { mutableStateOf(0) }
    var showEditTextDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showCustomCanvasSizeDialog by remember { mutableStateOf(false) }
    var showEditProjectTitleDialog by remember { mutableStateOf(false) }
    var showOptionalCloudAuthDialog by remember { mutableStateOf(false) }
    var pickingForBackground by remember { mutableStateOf(false) }
    var pickingForTextTexture by remember { mutableStateOf(false) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var isInspectorCollapsed by remember { mutableStateOf(false) }
    var isZenFullscreenMode by remember { mutableStateOf(false) }
    var canvasAgentDiffRatio by remember(pendingAgentTransaction) { mutableStateOf(1f) }

    val activePoints = remember { mutableStateListOf<Point2D>() }
    var activeBrushTouchPos by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshLocalPlpFiles(context)
        viewModel.refreshLearnedPlpStyles(context)
        viewModel.refreshCustomFonts(context)
        viewModel.syncSelectedGeminiModel(context)
    }

    // Back handler when zen mode, layer drawer, agent sheet, eraser, zoom/pan, or pen mode is active
    BackHandler(enabled = isZenFullscreenMode || showLayerDrawer || showAgentSheet || isDrawingMode || isEraserMode || isZoomPanModeActive) {
        when {
            isZenFullscreenMode -> isZenFullscreenMode = false
            showLayerDrawer -> showLayerDrawer = false
            showAgentSheet -> showAgentSheet = false
            isEraserMode -> viewModel.setEraserMode(false)
            isDrawingMode -> viewModel.setDrawingMode(false)
            isZoomPanModeActive -> viewModel.toggleZoomPanMode()
        }
    }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            if (pickingForTextTexture) {
                pickingForTextTexture = false
                viewModel.importCustomTextureForSelectedText(context, uri)
            } else {
                viewModel.importImageFromUri(context, uri, asBackground = pickingForBackground)
            }
        }
    }

    // Custom .ttf / .otf Font Importer ("My Fonts")
    val importFontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCustomFontFromUri(context, uri)
        }
    }

    // .plp Document Exporter
    val exportPlpLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.exportPlpFileToUri(context, uri)
        }
    }

    // .plp Document Importer
    val importPlpLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importPlpFileFromUri(context, uri)
        }
    }

    // Diagnostics .log Exporter
    val exportLogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.exportAgentLogs(context, uri) { ok ->
                // Handled in logger
            }
        }
    }

    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isAgentBarVisible by viewModel.isAgentBarVisible.collectAsStateWithLifecycle()
    val isCanvasExpandedMode by viewModel.isCanvasExpandedMode.collectAsStateWithLifecycle()
    val snapGuides by viewModel.snapGuides.collectAsStateWithLifecycle()
    val palette = com.example.ui.theme.LocalStudioPalette.current

    val selectedLayer = project.layers.find { it.id == selectedLayerId }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = palette.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            StudioTopBar(
                isZenFullscreenMode = isZenFullscreenMode,
                selectedLayer = selectedLayer,
                canUndo = canUndo,
                canRedo = canRedo,
                isCanvasExpandedMode = isCanvasExpandedMode,
                showGrid = showGrid,
                isReelsSafeZoneVisible = isReelsSafeZoneVisible,
                canvasZoomScale = canvasZoomScale,
                isZoomPanModeActive = isZoomPanModeActive,
                totalLayersCount = project.layers.size,
                statusBanner = statusBanner,
                selectedModelName = selectedGeminiModel,
                onSelectModelName = { viewModel.setSelectedGeminiModel(context, it) },
                onEditSelectedLayer = {
                    if (selectedLayer is TextLayer) {
                        showEditTextDialog = true
                    }
                },
                onDeleteSelectedLayer = {
                    selectedLayerId?.let { viewModel.deleteLayer(it) }
                },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onToggleCanvasExpanded = { viewModel.toggleCanvasExpanded() },
                onToggleZoomPanMode = { viewModel.toggleZoomPanMode() },
                onToggleGrid = { viewModel.toggleGrid() },
                onToggleReelsSafeZone = { viewModel.toggleReelsSafeZone() },
                onOpenLayersDrawer = { showLayerDrawer = true },
                onAddLayer = { viewModel.addTextLayer() },
                onOpenSaveDialog = { showExportDialog = true },
                onShareCanvas = {
                    showExportDialog = true
                },
                onRandomQuote = {
                    viewModel.applyQuoteItem(BUILTIN_PIXELLAB_QUOTES.random())
                },
                onToggleZenFullscreen = { isZenFullscreenMode = !isZenFullscreenMode },
                onOpenOptionsDialog = { showOptionalCloudAuthDialog = true },
                onClearStatusBanner = { viewModel.clearStatusBanner() }
            )
        },
        bottomBar = {
            StudioBottomControls(
                isZenFullscreenMode = isZenFullscreenMode,
                isInspectorCollapsed = isInspectorCollapsed,
                activeTab = activeTab,
                totalLayersCount = project.layers.size,
                onToggleInspectorCollapsed = { isInspectorCollapsed = !isInspectorCollapsed },
                onSelectTab = { tab ->
                    if (activeTab == tab && !isInspectorCollapsed) {
                        isInspectorCollapsed = true
                    } else {
                        viewModel.selectTab(tab)
                        isInspectorCollapsed = false
                    }
                },
                onOpenLayerDrawer = { showLayerDrawer = true }
            ) {
                when (activeTab) {
                    StudioBottomTab.PRESETS -> PresetsAndPlpInspector(
                        projectTitle = project.title,
                        canvasWidth = project.canvasWidth,
                        canvasHeight = project.canvasHeight,
                        cloudState = cloudState,
                        localPlpFiles = localPlpFiles,
                        learnedPlpStyles = learnedPlpStyles,
                        activeLearnedStyle = activeLearnedStyle,
                        isReelsSafeZoneVisible = isReelsSafeZoneVisible,
                        onToggleReelsSafeZone = { viewModel.toggleReelsSafeZone() },
                        onApplyReelsTemplate = { viewModel.applyReelsCoverTemplate(it, context) },
                        onGenerateReelsCover = { topic, category ->
                            viewModel.generateInstagramReelsCover(topic, category, context)
                        },
                        onApplyMemeGenerator = { viewModel.applyMemeGenerator() },
                        onApplyTextPreset = { viewModel.applyTextPreset(it) },
                        onApplyQuote = { viewModel.applyQuoteItem(it) },
                        onSelectAspectPreset = { viewModel.setCanvasAspectPreset(it) },
                        onOpenCustomSizeDialog = { showCustomCanvasSizeDialog = true },
                        onSaveCloudAndPlp = {
                            viewModel.saveProjectToCloudAndLocalPlp(
                                context = context,
                                onRequireSignInForCloud = { showOptionalCloudAuthDialog = true }
                            )
                        },
                        onExportPlpFile = {
                            val cleanName = project.title.replace(" ", "_").ifBlank { "pixellab_project" }
                            exportPlpLauncher.launch("$cleanName.plp")
                        },
                        onImportPlpFile = {
                            importPlpLauncher.launch(arrayOf("*/*"))
                        },
                        onOpenLocalPlp = { viewModel.openLocalPlpFile(it, context) },
                        onLearnCurrentCanvasStyle = {
                            viewModel.learnStyleFromCurrentCanvas(context)
                        },
                        onLearnFromLocalPlpFile = { file ->
                            viewModel.learnStyleFromPlpFile(file, context)
                        },
                        onGenerateFromLearnedStyle = { style, headline, subtitle ->
                            viewModel.generateProjectFromLearnedStyle(context, style, headline, subtitle)
                        },
                        onApplyLearnedStyleToCanvas = { style ->
                            viewModel.applyLearnedStyleToCurrentCanvas(context, style)
                        },
                        onSelectLearnedStyle = { style ->
                            viewModel.selectActiveLearnedStyle(context, style)
                        },
                        onDeleteLearnedStyle = { styleId ->
                            viewModel.deleteLearnedStyle(context, styleId)
                        },
                        onLoadCloudDoc = { viewModel.loadCloudProjectDoc(context, it) },
                        onDeleteCloudDoc = { viewModel.deleteCloudProject(it) },
                        onOpenCloudSignIn = { showOptionalCloudAuthDialog = true }
                    )

                    StudioBottomTab.TEXT -> TextLayerInspector(
                        selectedLayer = selectedLayer,
                        customUserFonts = customUserFonts,
                        savedUserPresets = savedUserTextPresets,
                        smartPaletteHexes = smartPaletteHexes,
                        isEyedropperActive = isEyedropperActive,
                        onAddTextLayer = { viewModel.addTextLayer() },
                        onOpenEditTextDialog = { showEditTextDialog = true },
                        onUpdateTextLayer = { viewModel.updateSelectedTextLayer(it) },
                        onCenterLayer = { h, v -> viewModel.centerSelectedLayer(h, v) },
                        onImportCustomFont = {
                            importFontLauncher.launch(arrayOf("*/*"))
                        },
                        onPickCustomTextureImage = {
                            pickingForTextTexture = true
                            pickingForBackground = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onApplyQuote = { viewModel.applyQuoteItem(it) },
                        onDuplicateLayer = { selectedLayer?.let { viewModel.duplicateLayer(it.id) } },
                        onDeleteLayer = { selectedLayer?.let { viewModel.deleteLayer(it.id) } },
                        onNudgeLayer = { dx, dy -> viewModel.nudgeSelectedLayer(dx, dy) },
                        onBringToFront = { selectedLayer?.let { viewModel.bringLayerToFront(it.id) } },
                        onSendToBack = { selectedLayer?.let { viewModel.sendLayerToBack(it.id) } },
                        onToggleEyedropper = { viewModel.toggleEyedropper() },
                        onExtractSmartPalette = { viewModel.extractSmartPaletteFromCanvas(context) },
                        onApplyTextPreset = { viewModel.applyTextPreset(it) },
                        onSaveCurrentTextStyle = { viewModel.saveCurrentLayerAsTextPreset() },
                        onDeleteSavedTextStyle = { viewModel.deleteSavedTextPreset(it) }
                    )

                    StudioBottomTab.OBJECTS -> ObjectsAndDrawInspector(
                        selectedLayer = selectedLayer,
                        isDrawingMode = isDrawingMode,
                        isEraserMode = isEraserMode,
                        eraserBrushSize = eraserBrushSize,
                        penColorHex = penColorHex,
                        penWidth = penWidth,
                        penNeon = penNeonGlow,
                        penDashed = penDashed,
                        canUndo = canUndo,
                        canRedo = canRedo,
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onAddShape = { viewModel.addShapeLayer(it) },
                        onAddBuiltinSticker = { viewModel.addBuiltinStickerLayer(it) },
                        onPickGalleryImageLayer = {
                            pickingForTextTexture = false
                            pickingForBackground = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onSetDrawingMode = { viewModel.setDrawingMode(it) },
                        onSetEraserMode = { viewModel.setEraserMode(it) },
                        onSetEraserBrushSize = { viewModel.setEraserBrushSize(it) },
                        onClearEraserStrokes = { viewModel.clearLayerEraserStrokes() },
                        onApplyImageCrop = { l, t, r, b -> viewModel.applyInteractiveImageCrop(l, t, r, b) },
                        onResetImageCrop = { viewModel.resetImageCropToOriginal() },
                        onToggleImageFlip = { h, v -> viewModel.toggleSelectedImageFlip(h, v) },
                        onUpdatePenOptions = { c, w, n, d -> viewModel.setPenOptions(c, w, n, d) },
                        onUpdateShapeLayer = { viewModel.updateSelectedShapeLayer(it) },
                        onUpdateImageLayer = { viewModel.updateSelectedImageLayer(it) },
                        onDuplicateLayer = { selectedLayer?.let { viewModel.duplicateLayer(it.id) } },
                        onDeleteLayer = { selectedLayer?.let { viewModel.deleteLayer(it.id) } },
                        onBringToFront = { selectedLayer?.let { viewModel.bringLayerToFront(it.id) } },
                        onSendToBack = { selectedLayer?.let { viewModel.sendLayerToBack(it.id) } },
                        onCenterLayer = { h, v -> viewModel.centerSelectedLayer(h, v) },
                        onNudgeLayer = { dx, dy -> viewModel.nudgeSelectedLayer(dx, dy) }
                    )

                    StudioBottomTab.CANVAS_BG -> CanvasBackgroundInspector(
                        background = project.background,
                        canvasWidth = project.canvasWidth,
                        canvasHeight = project.canvasHeight,
                        onUpdateBackground = { viewModel.updateCanvasBackground(it) },
                        onPickBackgroundGalleryImage = {
                            pickingForTextTexture = false
                            pickingForBackground = true
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onSelectAspectPreset = { viewModel.setCanvasAspectPreset(it) },
                        onOpenCustomSizeDialog = { showCustomCanvasSizeDialog = true },
                        onResetCanvas = { viewModel.resetCanvasToBlank() }
                    )

                    StudioBottomTab.POST_FX -> PostFxInspector(
                        postFx = project.postFx,
                        onUpdatePostFx = { viewModel.updateCanvasPostFx(it) }
                    )

                    StudioBottomTab.AI_STUDIO -> GeminiAiStudioInspector(
                        aiState = aiState,
                        activeLearnedStyle = activeLearnedStyle,
                        selectedGeminiModel = selectedGeminiModel,
                        activeTransactionSteps = activeTransactionSteps,
                        onSelectGeminiModel = { viewModel.setSelectedGeminiModel(context, it) },
                        onGenerateAutoLayout = { viewModel.generateAiAutoLayout(it) },
                        onGenerateAiImage = { prompt, asBg -> viewModel.generateAiImage(prompt, asBg) },
                        onGenerateAiSticker = { viewModel.generateAiSticker(it) },
                        onEditWithAi = { viewModel.editCurrentLayerOrCanvasWithAi(context, it) },
                        onDismissAiStatus = { viewModel.dismissAiState() },
                        onExecuteAgentCommand = { viewModel.sendCommandToPixelLabAgent(context, it) },
                        onOpenFullAgentSheet = { showAgentSheet = true },
                        onRequestAutoLayout = { viewModel.requestAutoLayoutPreview() },
                        isAntiGravityActive = isAntiGravityActive,
                        onToggleAntiGravity = { viewModel.toggleAntiGravityMode(context) },
                        onLearnCurrentCanvasStyle = { viewModel.learnStyleFromCurrentCanvas(context) },
                        onGenerateFromLearnedStyle = { style, h, s -> viewModel.generateProjectFromLearnedStyle(context, style, h, s) },
                        onApplyLearnedStyleToCanvas = { style -> viewModel.applyLearnedStyleToCurrentCanvas(context, style) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. FULL-BLEED ZERO-WASTE INTERACTIVE CANVAS VIEWPORT
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = if (isZenFullscreenMode) 0.dp else if (isCanvasExpandedMode) 2.dp else 4.dp,
                        bottom = if (isZenFullscreenMode) 0.dp else 4.dp,
                        start = 2.dp,
                        end = 2.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
            val canvasAspect = (project.canvasWidth.toFloat() / project.canvasHeight.coerceAtLeast(1).toFloat())
                .coerceIn(0.4f, 2.5f)

            Box(
                modifier = Modifier
                    .aspectRatio(canvasAspect, matchHeightConstraintsFirst = maxWidth > maxHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                    .testTag("studio_interactive_canvas")
            ) {
                val currentProject by rememberUpdatedState(project)
                val currentSelectedLayerId by rememberUpdatedState(selectedLayerId)
                val currentSelectedLayer by rememberUpdatedState(selectedLayer)
                val currentIsEyedropperActive by rememberUpdatedState(isEyedropperActive)
                val currentIsEraserMode by rememberUpdatedState(isEraserMode)
                val currentIsDrawingMode by rememberUpdatedState(isDrawingMode)
                val currentIsZoomPanMode by rememberUpdatedState(isZoomPanModeActive)
                val currentZoomScale by rememberUpdatedState(canvasZoomScale)

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isDrawingMode, isEraserMode, isZoomPanModeActive) {
                            if (isDrawingMode || isEraserMode) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val w = size.width.coerceAtLeast(1).toFloat()
                                        val h = size.height.coerceAtLeast(1).toFloat()
                                        activeBrushTouchPos = offset
                                        activePoints.clear()
                                        activePoints.add(
                                            viewModel.screenToCanvasNormalized(offset.x, offset.y, w, h)
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val w = size.width.coerceAtLeast(1).toFloat()
                                        val h = size.height.coerceAtLeast(1).toFloat()
                                        activeBrushTouchPos = change.position
                                        val pt = viewModel.screenToCanvasNormalized(
                                            change.position.x,
                                            change.position.y,
                                            w,
                                            h
                                        )
                                        activePoints.add(pt)
                                    },
                                    onDragEnd = {
                                        val strokePts = activePoints.toList()
                                        activeBrushTouchPos = null
                                        activePoints.clear()
                                        if (currentIsEraserMode) {
                                            viewModel.appendInteractiveEraserStroke(strokePts)
                                        } else {
                                            viewModel.appendFreehandStroke(strokePts)
                                        }
                                    },
                                    onDragCancel = {
                                        activeBrushTouchPos = null
                                        activePoints.clear()
                                    }
                                )
                            } else {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val downPos = down.position
                                    val w = size.width.coerceAtLeast(1).toFloat()
                                    val h = size.height.coerceAtLeast(1).toFloat()
                                    val mappedDown = viewModel.screenToCanvasNormalized(downPos.x, downPos.y, w, h)

                                    if (currentIsEyedropperActive) {
                                        down.consume()
                                        viewModel.sampleEyedropperColorAt(
                                            context = context,
                                            normX = mappedDown.x.coerceIn(0f, 1f),
                                            normY = mappedDown.y.coerceIn(0f, 1f)
                                        )
                                        return@awaitEachGesture
                                    }

                                    // If dedicated Zoom & Pan mode is active, 1-finger pans and 2-finger pinch zooms canvas
                                    if (currentIsZoomPanMode) {
                                        var prevPos = downPos
                                        do {
                                            val event = awaitPointerEvent()
                                            val activePointers = event.changes.filter { it.pressed }
                                            if (activePointers.isEmpty()) break
                                            if (activePointers.size == 1) {
                                                val ch = activePointers[0]
                                                val delta = ch.position - prevPos
                                                ch.consume()
                                                viewModel.panAndZoomCanvasByGesture(
                                                    panDxNorm = delta.x / w,
                                                    panDyNorm = delta.y / h,
                                                    zoomFactor = 1f
                                                )
                                                prevPos = ch.position
                                            } else if (activePointers.size >= 2) {
                                                val p1 = activePointers[0]
                                                val p2 = activePointers[1]
                                                val prevCentroid = (p1.previousPosition + p2.previousPosition) / 2f
                                                val currCentroid = (p1.position + p2.position) / 2f
                                                val panDelta = currCentroid - prevCentroid
                                                val prevDist = (p1.previousPosition - p2.previousPosition).getDistance().coerceAtLeast(1f)
                                                val currDist = (p1.position - p2.position).getDistance().coerceAtLeast(1f)
                                                val zoomFactor = (currDist / prevDist).coerceIn(0.75f, 1.35f)
                                                p1.consume()
                                                p2.consume()
                                                viewModel.panAndZoomCanvasByGesture(
                                                    panDxNorm = panDelta.x / w,
                                                    panDyNorm = panDelta.y / h,
                                                    zoomFactor = zoomFactor
                                                )
                                            }
                                        } while (event.changes.any { it.pressed })
                                        return@awaitEachGesture
                                    }

                                    val normDownX = mappedDown.x - 0.5f
                                    val normDownY = mappedDown.y - 0.5f

                                    val visibleLayers = currentProject.layers.filter { it.isVisible }
                                    val currentSelId = currentSelectedLayerId
                                    val currentSelLayer = visibleLayers.find { it.id == currentSelId }

                                    // Check if user grabbed the bottom-right Scale Handle or bottom-left Rotate Handle of currentSelLayer
                                    var activeHandleMode = 0 // 0 = Pan/Drag, 1 = Scale Handle (Bottom-Right), 2 = Rotate Handle (Bottom-Left)
                                    if (currentSelLayer != null && !currentSelLayer.isLocked) {
                                        val halfNormW = 0.14f * currentSelLayer.scaleX.coerceIn(0.3f, 2.5f)
                                        val halfNormH = 0.08f * currentSelLayer.scaleY.coerceIn(0.3f, 2.5f)
                                        val brX = currentSelLayer.x + halfNormW
                                        val brY = currentSelLayer.y + halfNormH
                                        val blX = currentSelLayer.x - halfNormW
                                        val blY = currentSelLayer.y + halfNormH
                                        if (hypot(normDownX - brX, normDownY - brY) < 0.09f) {
                                            activeHandleMode = 1
                                        } else if (hypot(normDownX - blX, normDownY - blY) < 0.09f) {
                                            activeHandleMode = 2
                                        }
                                    }

                                    val hitLayer = if (activeHandleMode != 0 && currentSelLayer != null) {
                                        currentSelLayer
                                    } else if (currentSelLayer != null && hypot(currentSelLayer.x - normDownX, currentSelLayer.y - normDownY) < 0.28f) {
                                        currentSelLayer
                                    } else {
                                        visibleLayers.reversed().firstOrNull { l ->
                                            hypot(l.x - normDownX, l.y - normDownY) < 0.28f
                                        }
                                    }

                                    if (hitLayer != null && hitLayer.id != currentSelId) {
                                        viewModel.selectLayer(hitLayer.id)
                                    }

                                    var hasMoved = false
                                    var prevPos = downPos
                                    val touchSlop = viewConfiguration.touchSlop

                                    do {
                                        val event = awaitPointerEvent()
                                        val activePointers = event.changes.filter { it.pressed }
                                        if (activePointers.isEmpty()) break

                                        if (activePointers.size == 1) {
                                            val change = activePointers[0]
                                            val currPos = change.position
                                            val dragDelta = currPos - prevPos
                                            if (!hasMoved && (currPos - downPos).getDistance() > touchSlop) {
                                                hasMoved = true
                                            }
                                            if (hasMoved) {
                                                if (hitLayer != null && !hitLayer.isLocked) {
                                                    change.consume()
                                                    val mappedPrev = viewModel.screenToCanvasNormalized(prevPos.x, prevPos.y, w, h)
                                                    val mappedCurr = viewModel.screenToCanvasNormalized(currPos.x, currPos.y, w, h)
                                                    val layerCenterPxX = (0.5f + hitLayer.x) * w
                                                    val layerCenterPxY = (0.5f + hitLayer.y) * h
                                                    val prevCanvasPxX = mappedPrev.x * w
                                                    val prevCanvasPxY = mappedPrev.y * h
                                                    val currCanvasPxX = mappedCurr.x * w
                                                    val currCanvasPxY = mappedCurr.y * h
                                                    when (activeHandleMode) {
                                                        1 -> {
                                                            // 1-Finger Corner Scale Handle
                                                            val prevDist = hypot(prevCanvasPxX - layerCenterPxX, prevCanvasPxY - layerCenterPxY).coerceAtLeast(10f)
                                                            val currDist = hypot(currCanvasPxX - layerCenterPxX, currCanvasPxY - layerCenterPxY).coerceAtLeast(10f)
                                                            val zoom = (currDist / prevDist).coerceIn(0.8f, 1.25f)
                                                            viewModel.transformSelectedLayerByGesture(
                                                                dxNorm = 0f,
                                                                dyNorm = 0f,
                                                                zoomChange = zoom,
                                                                rotationChangeDeg = 0f
                                                            )
                                                        }
                                                        2 -> {
                                                            // 1-Finger Corner Rotate Handle
                                                            val prevAng = Math.toDegrees(kotlin.math.atan2((prevCanvasPxY - layerCenterPxY).toDouble(), (prevCanvasPxX - layerCenterPxX).toDouble())).toFloat()
                                                            val currAng = Math.toDegrees(kotlin.math.atan2((currCanvasPxY - layerCenterPxY).toDouble(), (currCanvasPxX - layerCenterPxX).toDouble())).toFloat()
                                                            var rotDelta = currAng - prevAng
                                                            if (rotDelta > 180f) rotDelta -= 360f
                                                            if (rotDelta < -180f) rotDelta += 360f
                                                            viewModel.transformSelectedLayerByGesture(
                                                                dxNorm = 0f,
                                                                dyNorm = 0f,
                                                                zoomChange = 1f,
                                                                rotationChangeDeg = rotDelta
                                                            )
                                                        }
                                                        else -> {
                                                            val dxNorm = (dragDelta.x / w) / currentZoomScale.coerceAtLeast(1f)
                                                            val dyNorm = (dragDelta.y / h) / currentZoomScale.coerceAtLeast(1f)
                                                            viewModel.transformSelectedLayerByGesture(
                                                                dxNorm = dxNorm,
                                                                dyNorm = dyNorm,
                                                                zoomChange = 1f,
                                                                rotationChangeDeg = 0f
                                                            )
                                                        }
                                                    }
                                                } else if (hitLayer == null && currentZoomScale > 1.01f) {
                                                    // Pan the zoomed canvas when dragging empty area
                                                    change.consume()
                                                    viewModel.panAndZoomCanvasByGesture(
                                                        panDxNorm = dragDelta.x / w,
                                                        panDyNorm = dragDelta.y / h,
                                                        zoomFactor = 1f
                                                    )
                                                }
                                            }
                                            prevPos = currPos
                                        } else if (activePointers.size >= 2) {
                                            hasMoved = true
                                            val p1 = activePointers[0]
                                            val p2 = activePointers[1]
                                            val p1Prev = p1.previousPosition
                                            val p2Prev = p2.previousPosition
                                            val p1Curr = p1.position
                                            val p2Curr = p2.position

                                            val prevCentroid = (p1Prev + p2Prev) / 2f
                                            val currCentroid = (p1Curr + p2Curr) / 2f
                                            val panDelta = currCentroid - prevCentroid

                                            val prevDist = (p1Prev - p2Prev).getDistance().coerceAtLeast(1f)
                                            val currDist = (p1Curr - p2Curr).getDistance().coerceAtLeast(1f)
                                            val zoom = (currDist / prevDist).coerceIn(0.7f, 1.3f)

                                            val prevAngle = Math.toDegrees(kotlin.math.atan2((p2Prev.y - p1Prev.y).toDouble(), (p2Prev.x - p1Prev.x).toDouble())).toFloat()
                                            val currAngle = Math.toDegrees(kotlin.math.atan2((p2Curr.y - p1Curr.y).toDouble(), (p2Curr.x - p1Curr.x).toDouble())).toFloat()
                                            var rotDelta = currAngle - prevAngle
                                            if (rotDelta > 180f) rotDelta -= 360f
                                            if (rotDelta < -180f) rotDelta += 360f

                                            p1.consume()
                                            p2.consume()

                                            if (hitLayer == null) {
                                                // 2-Finger Pinch Zoom & Pan on Canvas (100%..500%)
                                                viewModel.panAndZoomCanvasByGesture(
                                                    panDxNorm = panDelta.x / w,
                                                    panDyNorm = panDelta.y / h,
                                                    zoomFactor = zoom
                                                )
                                            } else {
                                                viewModel.transformSelectedLayerByGesture(
                                                    dxNorm = (panDelta.x / w) / currentZoomScale.coerceAtLeast(1f),
                                                    dyNorm = (panDelta.y / h) / currentZoomScale.coerceAtLeast(1f),
                                                    zoomChange = zoom,
                                                    rotationChangeDeg = rotDelta
                                                )
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })

                                    if (!hasMoved) {
                                        val now = System.currentTimeMillis()
                                        if (now - lastTapTimestamp < 320L && currentSelectedLayer is TextLayer) {
                                            showEditTextDialog = true
                                        } else {
                                            lastTapTimestamp = now
                                            if (hitLayer == null) {
                                                viewModel.selectLayer(null)
                                            }
                                        }
                                    } else {
                                        viewModel.finalizeGestureTransform()
                                    }
                                }
                            }
                        }
                ) {
                    drawIntoCanvas { composeCanvas ->
                        val nativeCanvas = composeCanvas.nativeCanvas
                        val pendingTxForDiff = pendingAgentTransaction
                        val isDiffWipeActive = pendingTxForDiff != null && activePreviewState == null && canvasAgentDiffRatio < 0.995f
                        val currentProjectToRender = if (activePreviewState?.showOriginalComparison == true) {
                            activePreviewState!!.originalProject
                        } else {
                            activePreviewState?.suggestion?.suggestedProject ?: project
                        }
                        nativeCanvas.save()
                        nativeCanvas.translate(
                            size.width * 0.5f + canvasPanOffset.x * size.width,
                            size.height * 0.5f + canvasPanOffset.y * size.height
                        )
                        nativeCanvas.scale(canvasZoomScale, canvasZoomScale)
                        nativeCanvas.translate(-size.width * 0.5f, -size.height * 0.5f)

                        if (isDiffWipeActive && pendingTxForDiff != null) {
                            // 1. Render BEFORE state across full canvas
                            StudioCanvasRenderer.renderProjectToCanvas(
                                context = context,
                                canvas = nativeCanvas,
                                project = pendingTxForDiff.preTransactionProject,
                                viewportWidth = size.width,
                                viewportHeight = size.height,
                                selectedLayerId = null,
                                showSelectionHandles = false,
                                showGrid = showGrid,
                                includeCheckerboard = true,
                                layerFloatingOffsets = layerFloatingOffsets,
                                cosmicParticles = cosmicParticles,
                                showReelsSafeZone = isReelsSafeZoneVisible,
                                aiMutatedLayerId = null
                            )
                            // 2. Render AFTER state clipped to canvasAgentDiffRatio
                            if (canvasAgentDiffRatio > 0.005f) {
                                nativeCanvas.save()
                                nativeCanvas.clipRect(0f, 0f, size.width * canvasAgentDiffRatio, size.height)
                                StudioCanvasRenderer.renderProjectToCanvas(
                                    context = context,
                                    canvas = nativeCanvas,
                                    project = currentProjectToRender,
                                    viewportWidth = size.width,
                                    viewportHeight = size.height,
                                    selectedLayerId = selectedLayerId,
                                    showSelectionHandles = false,
                                    showGrid = showGrid,
                                    includeCheckerboard = true,
                                    layerFloatingOffsets = layerFloatingOffsets,
                                    cosmicParticles = cosmicParticles,
                                    showReelsSafeZone = isReelsSafeZoneVisible,
                                    aiMutatedLayerId = activeAiMutatedLayerId
                                )
                                nativeCanvas.restore()
                            }
                        } else {
                            StudioCanvasRenderer.renderProjectToCanvas(
                                context = context,
                                canvas = nativeCanvas,
                                project = currentProjectToRender,
                                viewportWidth = size.width,
                                viewportHeight = size.height,
                                selectedLayerId = selectedLayerId,
                                showSelectionHandles = !isDrawingMode && !isEraserMode && activePreviewState == null,
                                showGrid = showGrid,
                                includeCheckerboard = true,
                                layerFloatingOffsets = layerFloatingOffsets,
                                cosmicParticles = cosmicParticles,
                                showReelsSafeZone = isReelsSafeZoneVisible,
                                aiMutatedLayerId = activeAiMutatedLayerId
                            )
                        }
                        nativeCanvas.restore()

                        if (isDiffWipeActive) {
                            val splitX = (size.width * canvasAgentDiffRatio).coerceIn(0f, size.width)
                            drawLine(
                                color = Color(0xFF10B981),
                                start = Offset(splitX, 0f),
                                end = Offset(splitX, size.height),
                                strokeWidth = 2.5.dp.toPx()
                            )
                        }
                    }

                    // Live stroke path & circular brush/eraser preview on canvas
                    if (activePoints.size >= 2 && (isDrawingMode || isEraserMode)) {
                        val livePath = Path()
                        val firstPt = activePoints.first()
                        val startScreenX = (firstPt.x - 0.5f) * size.width * canvasZoomScale + size.width * 0.5f + canvasPanOffset.x * size.width
                        val startScreenY = (firstPt.y - 0.5f) * size.height * canvasZoomScale + size.height * 0.5f + canvasPanOffset.y * size.height
                        livePath.moveTo(startScreenX, startScreenY)
                        for (i in 1 until activePoints.size) {
                            val pt = activePoints[i]
                            val sx = (pt.x - 0.5f) * size.width * canvasZoomScale + size.width * 0.5f + canvasPanOffset.x * size.width
                            val sy = (pt.y - 0.5f) * size.height * canvasZoomScale + size.height * 0.5f + canvasPanOffset.y * size.height
                            livePath.lineTo(sx, sy)
                        }
                        val strokeWidthPx = (if (isEraserMode) eraserBrushSize else penWidth) * (size.width / 1080f) * canvasZoomScale
                        drawPath(
                            path = livePath,
                            color = if (isEraserMode) Color.White.copy(alpha = 0.55f) else palette.primaryAccent.copy(alpha = 0.75f),
                            style = Stroke(width = strokeWidthPx.coerceAtLeast(2f))
                        )
                    }

                    activeBrushTouchPos?.let { touchPos ->
                        val brushPx = (if (isEraserMode) eraserBrushSize else penWidth) * (size.width / 1080f) * canvasZoomScale
                        val radiusPx = (brushPx * 0.5f).coerceAtLeast(6f)
                        drawCircle(
                            color = if (isEraserMode) Color.White.copy(alpha = 0.28f) else palette.primaryAccent.copy(alpha = 0.22f),
                            radius = radiusPx,
                            center = touchPos
                        )
                        drawCircle(
                            color = if (isEraserMode) Color(0xFF00E5FF) else Color.White,
                            radius = radiusPx,
                            center = touchPos,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Smart Magnetic Snap Guide Lines (Center X and Y)
                    if (snapGuides.first) {
                        drawLine(
                            color = palette.primaryAccent,
                            start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                            strokeWidth = 2f
                        )
                    }
                    if (snapGuides.second) {
                        drawLine(
                            color = palette.secondaryAccent,
                            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                            strokeWidth = 2f
                        )
                    }
                }

                // Direct Inline Canvas Property Chips for Selected Layer
                if (selectedLayer != null && !isZenFullscreenMode && !isZoomPanModeActive && canvasZoomScale <= 1.01f && activePreviewState == null && pendingAgentTransaction == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                    ) {
                        InlineCanvasPropertyChips(
                            selectedLayer = selectedLayer,
                            isZenFullscreenMode = isZenFullscreenMode,
                            onEditText = { showEditTextDialog = true },
                            onToggle3d = {
                                when (val layer = selectedLayer) {
                                    is TextLayer -> viewModel.updateSelectedTextLayer { it.copy(is3dEnabled = !layer.is3dEnabled) }
                                    is ShapeLayer -> viewModel.updateSelectedShapeLayer { it.copy(is3dExtruded = !layer.is3dExtruded) }
                                    else -> {}
                                }
                            },
                            onCenterHorizontally = { viewModel.centerSelectedLayer(horizontal = true, vertical = true) },
                            onBringToFront = { selectedLayer?.let { viewModel.bringLayerToFront(it.id) } },
                            onFlipHorizontal = { viewModel.toggleSelectedImageFlip(true, false) },
                            onDuplicateLayer = { selectedLayer?.let { viewModel.duplicateLayer(it.id) } },
                            onDeleteLayer = { selectedLayer?.let { viewModel.deleteLayer(it.id) } },
                            onOpenInspectorForLayer = { tab ->
                                viewModel.selectTab(tab)
                                isInspectorCollapsed = false
                            }
                        )
                    }
                }

                // Floating Zoom & Pan HUD Bar (100%..500%)
                if (isZoomPanModeActive || canvasZoomScale > 1.01f) {
                    val zoomPct = (canvasZoomScale * 100f).toInt().coerceIn(100, 500)
                    Surface(
                        color = palette.surfaceElevated.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, palette.primaryAccent),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp, start = 10.dp, end = 10.dp)
                            .testTag("canvas_zoom_hud_bar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "🔍 $zoomPct%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryAccent
                            )
                            Slider(
                                value = canvasZoomScale,
                                onValueChange = { viewModel.setCanvasZoom(it) },
                                valueRange = 1.0f..5.0f,
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(26.dp)
                                    .testTag("canvas_zoom_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.primaryAccent,
                                    activeTrackColor = palette.primaryAccent
                                )
                            )
                            Surface(
                                color = palette.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .clickable { viewModel.resetCanvasZoomAndPan() }
                                    .testTag("reset_zoom_100_button")
                            ) {
                                Text(
                                    text = "ریست ۱۰۰٪",
                                    fontSize = 10.sp,
                                    color = palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = if (isZoomPanModeActive) palette.primaryAccent else palette.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable { viewModel.toggleZoomPanMode() }
                            ) {
                                Text(
                                    text = if (isZoomPanModeActive) "جابه‌جایی بوم: فعال" else "قفل پن",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isZoomPanModeActive) Color.Black else palette.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Floating Interactive Eraser HUD Pill on Canvas when Eraser Mode is active
                if (isEraserMode) {
                    Surface(
                        color = palette.surfaceElevated.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, palette.primaryAccent),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .testTag("canvas_eraser_hud_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Live circular brush preview
                            val previewDp = (eraserBrushSize.coerceIn(4f, 90f) * 0.3f).coerceIn(6f, 26f).dp
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(palette.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(previewDp)
                                        .clip(CircleShape)
                                        .background(palette.primaryAccent.copy(alpha = 0.3f))
                                        .border(1.5.dp, palette.primaryAccent, CircleShape)
                                )
                            }
                            Text(
                                text = "پاک‌کن: ${eraserBrushSize.toInt()} px",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryAccent
                            )
                            Slider(
                                value = eraserBrushSize,
                                onValueChange = { viewModel.setEraserBrushSize(it) },
                                valueRange = 4f..90f,
                                modifier = Modifier.width(96.dp).height(24.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.primaryAccent,
                                    activeTrackColor = palette.primaryAccent
                                )
                            )
                            Surface(
                                color = palette.danger.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { viewModel.setEraserMode(false) }
                            ) {
                                Text(
                                    text = "پایان پاک‌کن",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.danger,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Live Animated Smart Layout Preview HUD & Confirmation Overlay
                if (activePreviewState != null) {
                    val preview = activePreviewState!!
                    val infiniteTransition = rememberInfiniteTransition(label = "preview_pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 0.95f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "previewPulseAlpha"
                    )

                    // Pulsing Neon Border Around Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 3.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        palette.primaryAccent.copy(alpha = pulseAlpha),
                                        palette.secondaryAccent.copy(alpha = pulseAlpha),
                                        palette.primaryAccent.copy(alpha = pulseAlpha)
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )

                    // Top Floating Preview Banner
                    Surface(
                        color = palette.surfaceElevated.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, palette.primaryAccent),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp, start = 12.dp, end = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "✨ ${preview.suggestion.titleFa}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryAccent
                                )
                                Surface(
                                    color = palette.primaryAccent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "نمره تعادل: ${preview.suggestion.designHealthScore}٪",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primaryAccent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = preview.suggestion.descriptionFa,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Bottom Floating Action Pill (Confirm / Cancel / Toggle Compare)
                    Surface(
                        color = palette.surfaceElevated.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, palette.secondaryAccent),
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Cancel Button
                            TextButton(
                                onClick = { viewModel.cancelPreviewSuggestion() },
                                colors = ButtonDefaults.textButtonColors(contentColor = palette.danger)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "انصراف", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("انصراف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Compare Toggle Button
                            OutlinedButton(
                                onClick = { viewModel.togglePreviewComparison(!preview.showOriginalComparison) },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, palette.border)
                            ) {
                                Text(
                                    text = if (preview.showOriginalComparison) "👁️ نمایش طرح جدید" else "👁️ مشاهده طرح قبلی",
                                    fontSize = 11.sp,
                                    color = palette.secondaryAccent
                                )
                            }

                            // Confirm Button
                            Button(
                                onClick = { viewModel.confirmPreviewSuggestion() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primaryAccent,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "تایید", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تایید و ثبت", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 4.8. FLOATING PENDING AGENT TRANSACTION CONFIRM / ROLLBACK BAR WITH VISUAL DIFF SLIDER
                val pendingTx = pendingAgentTransaction
                if (pendingTx != null && activePreviewState == null) {
                    Surface(
                        color = palette.surfaceElevated.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
                            .testTag("canvas_pending_transaction_bar")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Interactive Before / After Diff Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "قبل",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canvasAgentDiffRatio < 0.2f) Color(0xFFF59E0B) else palette.textSecondary,
                                    modifier = Modifier.clickable { canvasAgentDiffRatio = 0f }
                                )
                                Slider(
                                    value = canvasAgentDiffRatio,
                                    onValueChange = { canvasAgentDiffRatio = it },
                                    valueRange = 0f..1f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(22.dp)
                                        .testTag("canvas_diff_slider"),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF10B981),
                                        activeTrackColor = Color(0xFF10B981),
                                        inactiveTrackColor = Color(0xFFF59E0B).copy(alpha = 0.5f)
                                    )
                                )
                                Text(
                                    text = "بعد (${(canvasAgentDiffRatio * 100).toInt()}٪)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canvasAgentDiffRatio > 0.8f) Color(0xFF10B981) else palette.textSecondary,
                                    modifier = Modifier.clickable { canvasAgentDiffRatio = 1f }
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { viewModel.rollbackPendingAgentTransaction() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = palette.danger),
                                    modifier = Modifier.testTag("canvas_rollback_tx_button")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "لغو و بازگشت", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("لغو و بازگشت", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showAgentSheet = true },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, palette.border)
                                ) {
                                    Text(
                                        text = "📋 مراحل (${pendingTx.steps.size})",
                                        fontSize = 11.sp,
                                        color = palette.secondaryAccent
                                    )
                                }

                                Button(
                                    onClick = { viewModel.confirmPendingAgentTransaction() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.testTag("canvas_confirm_tx_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "تأیید تغییرات", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تأیید تغییرات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. DISCRETE FLOATING ZEN FULLSCREEN EXIT BUTTON
        if (isZenFullscreenMode) {
            Surface(
                color = palette.surface.copy(alpha = 0.85f),
                shape = CircleShape,
                border = BorderStroke(1.dp, palette.border),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp)
                    .size(44.dp)
            ) {
                IconButton(
                    onClick = { isZenFullscreenMode = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("exit_zen_fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "خروج از تمام‌صفحه",
                        tint = palette.primaryAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

    // --- PIXELLAB AI AGENT MULTI-TURN CHAT, DIAGNOSTICS & LOG MANAGEMENT SHEET ---
    if (showAgentSheet) {
        PixelLabAgentModalSheet(
            selectedLayer = selectedLayer,
            messages = agentMessages,
            aiState = aiState,
            canUndo = canUndo,
            isGoogleSignedIn = isUserSignedIn,
            googleUserEmail = userEmailOrName,
            logEntries = logEntries,
            diagnosticsReport = diagnosticsReport,
            initialTab = agentSheetInitialTab,
            selectedGeminiModel = selectedGeminiModel,
            activeTransactionSteps = activeTransactionSteps,
            pendingAgentTransaction = pendingAgentTransaction,
            lastGeminiError = lastGeminiError,
            onSelectGeminiModel = { modelName ->
                viewModel.setSelectedGeminiModel(context, modelName)
            },
            onConfirmTransaction = {
                viewModel.confirmPendingAgentTransaction()
            },
            onRollbackTransaction = {
                viewModel.rollbackPendingAgentTransaction()
            },
            onDismissGeminiError = {
                viewModel.dismissGeminiError()
            },
            onSendCommand = { cmd, vision ->
                viewModel.sendCommandToPixelLabAgent(context, cmd, useCanvasVision = vision)
            },
            onUndoAction = { viewModel.rollbackPendingAgentTransaction() },
            onClearHistory = { viewModel.clearAgentHistory() },
            onGoogleSignIn = { showOptionalCloudAuthDialog = true },
            onGoogleSignOut = onSignOut,
            onRunDiagnostics = { viewModel.runConnectionDiagnostics(context) },
            onClearLogs = { viewModel.clearAgentLogs(context) },
            onExportLogs = { exportLogLauncher.launch("pixellab_agent_diagnostics.log") },
            onShareLogs = {
                try {
                    context.startActivity(viewModel.getAgentLogShareIntent(context))
                } catch (e: Exception) {
                    // Log or ignore if no share app
                }
            },
            onDismiss = { showAgentSheet = false }
        )
    }

    // --- LAYER MANAGEMENT DRAWER SHEET ---
    if (showLayerDrawer) {
        LayerDrawerSheet(
            layers = project.layers,
            selectedLayerId = selectedLayerId,
            hasCopiedStyle = hasCopiedLayerStyle,
            onSelectLayer = { viewModel.selectLayer(it) },
            onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
            onToggleLock = { viewModel.toggleLayerLock(it) },
            onMoveUp = { viewModel.moveLayerOrder(it, moveUpInVisualStack = true) },
            onMoveDown = { viewModel.moveLayerOrder(it, moveUpInVisualStack = false) },
            onDuplicate = { viewModel.duplicateLayer(it) },
            onDelete = { viewModel.deleteLayer(it) },
            onMergeVisibleLayers = { viewModel.mergeVisibleLayersToImage(context) },
            onMergeSelectedLayers = { ids -> viewModel.mergeSelectedLayersToImage(context, ids) },
            onDeleteSelectedLayers = { ids -> viewModel.deleteSelectedLayers(ids) },
            onGroupSelectedLayers = { ids -> viewModel.groupSelectedLayers(ids) },
            onUngroupSelectedLayers = { ids -> viewModel.ungroupSelectedLayers(ids) },
            onCopyLayerStyle = { id -> viewModel.copyLayerStyle(id) },
            onPasteLayerStyle = { id -> viewModel.pasteLayerStyle(id) },
            onUpdateSelectedOpacity = { viewModel.updateSelectedLayerCommon(opacity = it) },
            onUpdateSelectedBlendMode = { viewModel.updateSelectedLayerCommon(blendMode = it) },
            onUnlockAllLayers = { viewModel.unlockAllLayers() },
            onClearAllLayers = { viewModel.clearAllLayers(clearBackgroundImage = true) },
            onResetCanvas = { viewModel.resetCanvasToBlank() },
            onDismiss = { showLayerDrawer = false }
        )
    }

    // --- OPTIONAL CLOUD ACCOUNT / GOOGLE SIGN-IN DIALOG ---
    if (showOptionalCloudAuthDialog) {
        var authLoading by remember { mutableStateOf(false) }
        var authError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showOptionalCloudAuthDialog = false },
            containerColor = Color(0xFF161D2A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Text(
                    text = if (isUserSignedIn) "حساب ابری پیکسل‌لب" else "همگام‌سازی ابری اختیاری (Google Sign-In)"
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isUserSignedIn) {
                        Text(
                            text = "شما با حساب ${userEmailOrName ?: "گوگل"} متصل هستید. تمام پروژه‌های شما علاوه بر فایل PLP داخلی، در فضای ابری همگام‌سازی می‌شوند.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )
                    } else {
                        Text(
                            text = "استفاده از استودیو، ابزارهای سه‌بعدی، ذخیره فایل PLP و ایجنت هوش مصنوعی جمینای کاملاً بدون نیاز به ورود فعال است.\n\nفقط در صورتی که مایلید پروژه‌های خود را در فضای ابری (Cloud) پشتیبان‌گیری کنید، می‌توانید وارد حساب گوگل شوید:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )
                        if (authError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = authError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF87171)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (isUserSignedIn) {
                    OutlinedButton(
                        onClick = {
                            showOptionalCloudAuthDialog = false
                            onSignOut()
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("خروج از حساب ابری", color = Color(0xFFF87171))
                    }
                } else {
                    Button(
                        onClick = {
                            authError = null
                            authLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    authLoading = false
                                    showOptionalCloudAuthDialog = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    authLoading = false
                                    authError = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    authLoading = false
                                }
                            )
                        },
                        enabled = !authLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color(0xFF04131D)
                        ),
                        modifier = Modifier.testTag("google_sign_in_button")
                    ) {
                        if (authLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF04131D),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("ورود با گوگل (Sign in with Google)", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showOptionalCloudAuthDialog = false }) {
                    Text("ادامه در حالت محلی", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // --- EDIT TEXT CONTENT DIALOG ---
    if (showEditTextDialog && selectedLayer is TextLayer) {
        var draftText by remember(selectedLayer.id) { mutableStateOf(selectedLayer.text) }
        AlertDialog(
            onDismissRequest = { showEditTextDialog = false },
            containerColor = Color(0xFF161D2A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("ویرایش متن لایه") },
            text = {
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_text_input"),
                    label = { Text("متن فارسی یا انگلیسی (چندخطی مجاز است)") },
                    minLines = 3,
                    maxLines = 6
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSelectedTextLayer { it.copy(text = draftText) }
                        showEditTextDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38BDF8),
                        contentColor = Color(0xFF04131D)
                    )
                ) {
                    Text("اعمال روی بوم", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTextDialog = false }) {
                    Text("انصراف", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // --- EDIT PROJECT TITLE DIALOG ---
    if (showEditProjectTitleDialog) {
        var draftTitle by remember(project.title) { mutableStateOf(project.title) }
        AlertDialog(
            onDismissRequest = { showEditProjectTitleDialog = false },
            containerColor = Color(0xFF161D2A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("نام پروژه استودیو") },
            text = {
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان پروژه") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProjectTitle(draftTitle)
                        showEditProjectTitleDialog = false
                    }
                ) {
                    Text("ذخیره نام")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProjectTitleDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }

    // --- CUSTOM NUMERIC CANVAS DIMENSIONS DIALOG (WIDTH & HEIGHT IN PX + VALIDATION) ---
    if (showCustomCanvasSizeDialog) {
        var widthInput by remember(project.canvasWidth) { mutableStateOf(project.canvasWidth.toString()) }
        var heightInput by remember(project.canvasHeight) { mutableStateOf(project.canvasHeight.toString()) }
        var keepAspectRatio by remember { mutableStateOf(false) }
        var validationError by remember { mutableStateOf<String?>(null) }
        val currentAspect = project.canvasWidth.toFloat().coerceAtLeast(1f) / project.canvasHeight.toFloat().coerceAtLeast(1f)

        AlertDialog(
            onDismissRequest = { showCustomCanvasSizeDialog = false },
            containerColor = Color(0xFF161D2A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("تنظیم ابعاد سفارشی بوم (Custom Size)") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "واحد اندازه‌گیری:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                        Surface(
                            color = Color(0xFF0C4A6E),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Text(
                                text = "پیکسل (px)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = widthInput,
                            onValueChange = { newW ->
                                widthInput = newW.filter { it.isDigit() }
                                validationError = null
                                if (keepAspectRatio) {
                                    val wVal = widthInput.toIntOrNull()
                                    if (wVal != null && wVal > 0) {
                                        heightInput = (wVal / currentAspect).toInt().coerceIn(128, 4096).toString()
                                    }
                                }
                            },
                            label = { Text("عرض (Width px)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_canvas_width_input")
                        )

                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { newH ->
                                heightInput = newH.filter { it.isDigit() }
                                validationError = null
                                if (keepAspectRatio) {
                                    val hVal = heightInput.toIntOrNull()
                                    if (hVal != null && hVal > 0) {
                                        widthInput = (hVal * currentAspect).toInt().coerceIn(128, 4096).toString()
                                    }
                                }
                            },
                            label = { Text("ارتفاع (Height px)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_canvas_height_input")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "حفظ نسبت فعلی بوم هنگام ورود عدد",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Switch(
                            checked = keepAspectRatio,
                            onCheckedChange = { keepAspectRatio = it }
                        )
                    }

                    Text(
                        text = "محدوده مجاز: ۱۲۸ تا ۴۰۹۶ پیکسل (موقعیت نسبی و کیفیت تمام لایه‌ها حفظ می‌شود)",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )

                    if (validationError != null) {
                        Text(
                            text = validationError ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF87171),
                            modifier = Modifier.testTag("custom_size_validation_error")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = widthInput.toIntOrNull()
                        val h = heightInput.toIntOrNull()
                        if (w == null || h == null) {
                            validationError = "لطفاً عرض و ارتفاع را به صورت عدد صحیح وارد کنید."
                        } else if (w !in 128..4096 || h !in 128..4096) {
                            validationError = "ابعاد بوم باید بین ۱۲۸ تا ۴۰۹۶ پیکسل باشد."
                        } else {
                            val ok = viewModel.setCustomCanvasDimensions(w, h)
                            if (ok) {
                                showCustomCanvasSizeDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38BDF8),
                        contentColor = Color(0xFF04131D)
                    ),
                    modifier = Modifier.testTag("confirm_custom_canvas_size_button")
                ) {
                    Text("اعمال ابعاد", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCanvasSizeDialog = false }) {
                    Text("انصراف", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // --- HIGH-RES EXPORT, DIRECT ANDROID SHARE (PNG/JPG) & PLP ARCHIVE DIALOG ---
    if (showExportDialog) {
        var selectedFormat by remember { mutableStateOf(ExportFormat.PNG) }
        var selectedQuality by remember { mutableStateOf(ExportQualityPreset.ULTRA_4K) }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = Color(0xFF161D2A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("خروجی، اشتراک‌گذاری مستقیم و پروژه PLP") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "۱. انتخاب فرمت خروجی و اشتراک‌گذاری (PNG / JPG):",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ExportFormat.entries.forEach { fmt ->
                        val isCurrent = selectedFormat == fmt
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedFormat = fmt }
                                .testTag("export_format_${fmt.name}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) Color(0xFF0C4A6E) else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = fmt.label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "۲. سطح کیفیت و رزولوشن خروجی:",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ExportQualityPreset.entries.forEach { preset ->
                        val isCurrent = selectedQuality == preset
                        val targetW = (project.canvasWidth * preset.scaleMultiplier).toInt().coerceAtMost(4096)
                        val targetH = (project.canvasHeight * preset.scaleMultiplier).toInt().coerceAtMost(4096)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedQuality = preset },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) Color(0xFF0C4A6E) else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = preset.labelFa,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "${targetW}×${targetH}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    // Direct Android Share Sheet Button (Supports PNG & JPG)
                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.shareCanvasImageDirectly(context, selectedFormat, selectedQuality)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color(0xFF04131D)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("direct_share_image_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "اشتراک‌گذاری مستقیم تصویر (${selectedFormat.extension.uppercase()})",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            val cleanName = project.title.replace(" ", "_").ifBlank { "pixellab_project" }
                            exportPlpLauncher.launch("$cleanName.plp")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ذخیره فایل لایه‌باز پروژه (.plp)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        viewModel.exportHighResImageToGallery(context, selectedFormat, selectedQuality)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34D399),
                        contentColor = Color(0xFF04131D)
                    ),
                    modifier = Modifier.testTag("confirm_export_gallery_button")
                ) {
                    Text("ذخیره در گالری (Save to Gallery)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("انصراف", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}
