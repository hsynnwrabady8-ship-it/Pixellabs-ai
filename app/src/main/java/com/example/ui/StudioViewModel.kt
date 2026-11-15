package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.BUILTIN_PIXELLAB_STICKERS
import com.example.data.BUILTIN_REELS_TEMPLATES
import com.example.data.ReelsCoverTemplate
import com.example.data.BackgroundType
import com.example.data.CanvasAspectPreset
import com.example.data.CanvasBackground
import com.example.data.CanvasPostFx
import com.example.data.BuiltinStickerItem
import com.example.data.CustomUserFont
import com.example.data.DEFAULT_ASPECT_PRESETS
import com.example.data.DrawingLayer
import com.example.data.DrawnStroke
import com.example.data.ExportFormat
import com.example.data.ExportQualityPreset
import com.example.data.ImageLayer
import com.example.data.LayerBlendMode
import com.example.data.LayerEmboss
import com.example.data.LayerInnerShadow
import com.example.data.LayerShadow
import com.example.data.LayerStroke
import com.example.data.MaskPresetMode
import com.example.data.PlpProjectManager
import com.example.data.Point2D
import com.example.data.ShapeKind
import com.example.data.ShapeLayer
import com.example.data.StudioFont
import com.example.data.StudioLayer
import com.example.data.StudioProject
import com.example.data.StudioProjectDoc
import com.example.data.StudioProjectRepository
import com.example.data.CropAspectPreset
import com.example.data.StudioQuoteItem
import com.example.data.TextAlignMode
import com.example.data.TextLayer
import com.example.data.TextStylePreset
import com.example.data.TexturePreset
import com.example.ui.agent.AgentChatMessage
import com.example.ui.agent.AgentDiagnosticsLogger
import com.example.ui.agent.AgentLogEntry
import com.example.ui.agent.FullDiagnosticsReport
import com.example.ui.agent.OfflineDesignEngine
import com.example.ui.agent.PixelLabAgentEngine
import com.example.ui.agent.PlpLearnedStyleDNA
import com.example.ui.agent.PlpStyleLearningEngine
import com.example.ui.canvas.StudioCanvasRenderer
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.auth.auth
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import java.util.UUID

private const val TAG = "StudioViewModel"

sealed interface CloudProjectsUiState {
    data object Unauthenticated : CloudProjectsUiState
    data object Loading : CloudProjectsUiState
    data class Success(val projects: List<StudioProjectDoc>) : CloudProjectsUiState
    data class Error(val message: String) : CloudProjectsUiState
}

sealed interface AiTaskUiState {
    data object Idle : AiTaskUiState
    data class Loading(val statusText: String) : AiTaskUiState
    data class Success(val summaryText: String) : AiTaskUiState
    data class Error(val errorText: String) : AiTaskUiState
}

enum class StudioBottomTab(val labelFa: String) {
    PRESETS("پروژه و قالب"),
    TEXT("متن و 3D"),
    OBJECTS("اشکال و رسم"),
    CANVAS_BG("پس‌زمینه"),
    POST_FX("افکت و رنگ"),
    AI_STUDIO("ایجنت جمینای")
}

class StudioViewModel(
    private val repository: StudioProjectRepository? = null,
    initialUserId: String? = null
) : ViewModel() {

    private val _currentUserId = MutableStateFlow(initialUserId)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _project = MutableStateFlow(createInitialProject())
    val project: StateFlow<StudioProject> = _project.asStateFlow()

    private val _selectedLayerId = MutableStateFlow<String?>(_project.value.layers.lastOrNull()?.id)
    val selectedLayerId: StateFlow<String?> = _selectedLayerId.asStateFlow()

    private val _activeTab = MutableStateFlow(StudioBottomTab.TEXT)
    val activeTab: StateFlow<StudioBottomTab> = _activeTab.asStateFlow()

    private val _showGrid = MutableStateFlow(false)
    val showGrid: StateFlow<Boolean> = _showGrid.asStateFlow()

    private val _isDrawingMode = MutableStateFlow(false)
    val isDrawingMode: StateFlow<Boolean> = _isDrawingMode.asStateFlow()

    private val _penColorHex = MutableStateFlow("#38BDF8")
    val penColorHex: StateFlow<String> = _penColorHex.asStateFlow()

    private val _penWidth = MutableStateFlow(12f)
    val penWidth: StateFlow<Float> = _penWidth.asStateFlow()

    private val _penNeonGlow = MutableStateFlow(true)
    val penNeonGlow: StateFlow<Boolean> = _penNeonGlow.asStateFlow()

    private val _penDashed = MutableStateFlow(false)
    val penDashed: StateFlow<Boolean> = _penDashed.asStateFlow()

    // Priority 1: Canvas Zoom (100%..500%) & Pan State
    private val _canvasZoomScale = MutableStateFlow(1.0f)
    val canvasZoomScale: StateFlow<Float> = _canvasZoomScale.asStateFlow()

    private val _canvasPanOffset = MutableStateFlow(Point2D(0f, 0f))
    val canvasPanOffset: StateFlow<Point2D> = _canvasPanOffset.asStateFlow()

    private val _isZoomPanModeActive = MutableStateFlow(false)
    val isZoomPanModeActive: StateFlow<Boolean> = _isZoomPanModeActive.asStateFlow()

    // Priority 2: Interactive Touch Eraser State (for ImageLayer & DrawingLayer)
    private val _isEraserMode = MutableStateFlow(false)
    val isEraserMode: StateFlow<Boolean> = _isEraserMode.asStateFlow()

    private val _eraserBrushSize = MutableStateFlow(28f)
    val eraserBrushSize: StateFlow<Float> = _eraserBrushSize.asStateFlow()

    private val _themeMode = MutableStateFlow(com.example.ui.theme.StudioThemeMode.AMOLED)
    val themeMode: StateFlow<com.example.ui.theme.StudioThemeMode> = _themeMode.asStateFlow()

    private val _isAgentBarVisible = MutableStateFlow(true)
    val isAgentBarVisible: StateFlow<Boolean> = _isAgentBarVisible.asStateFlow()

    private val _isCanvasExpandedMode = MutableStateFlow(false)
    val isCanvasExpandedMode: StateFlow<Boolean> = _isCanvasExpandedMode.asStateFlow()

    private val _snapGuides = MutableStateFlow<Pair<Boolean, Boolean>>(Pair(false, false))
    val snapGuides: StateFlow<Pair<Boolean, Boolean>> = _snapGuides.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _localPlpFiles = MutableStateFlow<List<File>>(emptyList())
    val localPlpFiles: StateFlow<List<File>> = _localPlpFiles.asStateFlow()

    private val _learnedPlpStyles = MutableStateFlow<List<PlpLearnedStyleDNA>>(
        PlpStyleLearningEngine.createDefaultLearnedPlpStyles()
    )
    val learnedPlpStyles: StateFlow<List<PlpLearnedStyleDNA>> = _learnedPlpStyles.asStateFlow()

    private val _activeLearnedStyle = MutableStateFlow<PlpLearnedStyleDNA?>(
        _learnedPlpStyles.value.firstOrNull()
    )
    val activeLearnedStyle: StateFlow<PlpLearnedStyleDNA?> = _activeLearnedStyle.asStateFlow()

    private val _customUserFonts = MutableStateFlow<List<CustomUserFont>>(emptyList())
    val customUserFonts: StateFlow<List<CustomUserFont>> = _customUserFonts.asStateFlow()

    private val _savedUserTextPresets = MutableStateFlow<List<TextStylePreset>>(emptyList())
    val savedUserTextPresets: StateFlow<List<TextStylePreset>> = _savedUserTextPresets.asStateFlow()

    private val _smartPaletteHexes = MutableStateFlow<List<String>>(emptyList())
    val smartPaletteHexes: StateFlow<List<String>> = _smartPaletteHexes.asStateFlow()

    private val _isEyedropperActive = MutableStateFlow(false)
    val isEyedropperActive: StateFlow<Boolean> = _isEyedropperActive.asStateFlow()

    private var _copiedLayerStyleSource: StudioLayer? = null
    private val _hasCopiedLayerStyle = MutableStateFlow(false)
    val hasCopiedLayerStyle: StateFlow<Boolean> = _hasCopiedLayerStyle.asStateFlow()

    private val _isReelsSafeZoneVisible = MutableStateFlow(false)
    val isReelsSafeZoneVisible: StateFlow<Boolean> = _isReelsSafeZoneVisible.asStateFlow()

    fun toggleReelsSafeZone(visible: Boolean? = null) {
        val next = visible ?: !_isReelsSafeZoneVisible.value
        _isReelsSafeZoneVisible.value = next
        _statusBanner.value = if (next) {
            "راهنمای ناحیه امن ریلز و استوری فعال شد (کادر ۱:۱ و حاشیه‌های تعاملی)."
        } else {
            "راهنمای ناحیه امن ریلز غیرفعال شد."
        }
    }

    // Anti-Gravity Zero-G Simulation Engine
    private var _physicsEngine: com.example.ui.physics.AntiGravityPhysicsEngine? = null
    fun getOrCreatePhysicsEngine(context: Context): com.example.ui.physics.AntiGravityPhysicsEngine {
        if (_physicsEngine == null) {
            val engine = com.example.ui.physics.AntiGravityPhysicsEngine(context.applicationContext, viewModelScope)
            _physicsEngine = engine
            viewModelScope.launch {
                launch {
                    engine.layerOffsets.collect { offsets ->
                        _layerFloatingOffsets.value = offsets
                    }
                }
                launch {
                    engine.particles.collect { particles ->
                        _cosmicParticles.value = particles
                    }
                }
            }
        }
        return _physicsEngine!!
    }

    private val _isAntiGravityActive = MutableStateFlow(false)
    val isAntiGravityActive: StateFlow<Boolean> = _isAntiGravityActive.asStateFlow()

    private val _layerFloatingOffsets = MutableStateFlow<Map<String, com.example.ui.physics.LayerPhysicsOffset>>(emptyMap())
    val layerFloatingOffsets: StateFlow<Map<String, com.example.ui.physics.LayerPhysicsOffset>> = _layerFloatingOffsets.asStateFlow()

    private val _cosmicParticles = MutableStateFlow<List<com.example.ui.physics.CosmicZeroGParticle>>(emptyList())
    val cosmicParticles: StateFlow<List<com.example.ui.physics.CosmicZeroGParticle>> = _cosmicParticles.asStateFlow()

    // Smart Agent Layout Suggestion & Live Animated Preview Flow
    private val _activePreviewState = MutableStateFlow<com.example.data.AgentPreviewState?>(null)
    val activePreviewState: StateFlow<com.example.data.AgentPreviewState?> = _activePreviewState.asStateFlow()

    fun requestAutoLayoutPreview() {
        val original = _project.value
        val suggestion = com.example.ui.agent.OfflineDesignEngine.generateHarmonicAutoLayout(original)
        _activePreviewState.value = com.example.data.AgentPreviewState(
            originalProject = original,
            suggestion = suggestion,
            showOriginalComparison = false
        )
        _statusBanner.value = "✨ پیش‌نمایش چیدمان هوشمند با نسبت طلایی فعال شد. می‌توانید طرح را مقایسه، تایید یا لغو کنید."
    }

    fun togglePreviewComparison(showOriginal: Boolean) {
        _activePreviewState.value = _activePreviewState.value?.copy(showOriginalComparison = showOriginal)
    }

    fun confirmPreviewSuggestion() {
        val preview = _activePreviewState.value ?: return
        pushUndoSnapshot()
        _project.value = preview.suggestion.suggestedProject
        _selectedLayerId.value = preview.suggestion.suggestedProject.layers.lastOrNull()?.id
        _activePreviewState.value = null
        _statusBanner.value = "✔ چیدمان پیشنهادی ایجنت با موفقیت در پروژه ثبت و اعمال شد."
    }

    fun cancelPreviewSuggestion() {
        _activePreviewState.value = null
        _statusBanner.value = "پیش‌نمایش لغو شد و طرح قبلی حفظ گردید."
    }

    fun toggleAntiGravityMode(context: Context) {
        val next = !_isAntiGravityActive.value
        _isAntiGravityActive.value = next
        _project.value = _project.value.copy(isAntiGravityActive = next)
        val engine = getOrCreatePhysicsEngine(context)
        engine.setAntiGravityActive(next, _project.value.layers.map { it.id })
        _statusBanner.value = if (next) {
            "🚀 حالت ضدجاذبه (Anti-Gravity Zero-G) فعال شد! لایه‌ها با فیزیک معلق شناور شدند."
        } else {
            "حالت ضدجاذبه غیرفعال شد."
        }
    }

    // Undo / Redo History Stacks
    private val undoStack = ArrayDeque<StudioProject>()
    private val redoStack = ArrayDeque<StudioProject>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Auth-gated Cloud Projects Flow: Never queries Firestore when user is null
    @OptIn(ExperimentalCoroutinesApi::class)
    val cloudProjectsState: StateFlow<CloudProjectsUiState> = _currentUserId
        .flatMapLatest { uid ->
            val repo = repository
            if (uid.isNullOrBlank() || repo == null) {
                flowOf(CloudProjectsUiState.Unauthenticated)
            } else {
                repo.observeProjects(uid)
                    .map<List<StudioProjectDoc>, CloudProjectsUiState> { CloudProjectsUiState.Success(it) }
                    .catch { e ->
                        Log.w(TAG, "Error observing cloud projects", e)
                        emit(CloudProjectsUiState.Error(e.localizedMessage ?: "خطا در دریافت پروژه‌های ابری"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = if (initialUserId.isNullOrBlank()) {
                CloudProjectsUiState.Unauthenticated
            } else {
                CloudProjectsUiState.Loading
            }
        )

    // AI Studio & PixelLab AI Agent State
    private val _aiState = MutableStateFlow<AiTaskUiState>(AiTaskUiState.Idle)
    val aiState: StateFlow<AiTaskUiState> = _aiState.asStateFlow()

    private val _agentMessages = MutableStateFlow<List<AgentChatMessage>>(emptyList())
    val agentMessages: StateFlow<List<AgentChatMessage>> = _agentMessages.asStateFlow()

    private val _selectedGeminiModel = MutableStateFlow(com.example.ui.agent.GeminiModelPreset.GEMINI_3_8_FLASH.modelId)
    val selectedGeminiModel: StateFlow<String> = _selectedGeminiModel.asStateFlow()

    private val _activeTransactionSteps = MutableStateFlow<List<com.example.ui.agent.AgentExecutionStep>>(emptyList())
    val activeTransactionSteps: StateFlow<List<com.example.ui.agent.AgentExecutionStep>> = _activeTransactionSteps.asStateFlow()

    private val _pendingAgentTransaction = MutableStateFlow<com.example.ui.agent.PendingAgentTransaction?>(null)
    val pendingAgentTransaction: StateFlow<com.example.ui.agent.PendingAgentTransaction?> = _pendingAgentTransaction.asStateFlow()

    private val _activeAiMutatedLayerId = MutableStateFlow<String?>(null)
    val activeAiMutatedLayerId: StateFlow<String?> = _activeAiMutatedLayerId.asStateFlow()

    private val _lastGeminiError = MutableStateFlow<String?>(null)
    val lastGeminiError: StateFlow<String?> = _lastGeminiError.asStateFlow()

    // 1. Lazy Gemini Nano Banana 2.1 Image Generation & Editing Model
    private val imageGenModel by lazy {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )
    }

    // 2. Dynamic Gemini PixelLab Agent Brain (Multi-Action Tool Executor)
    private fun getPixelLabAgentModel() = Firebase.ai.generativeModel(
        modelName = _selectedGeminiModel.value,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = PixelLabAgentEngine.agentResponseSchema
        }
    )

    // 3. Lazy Gemini 3.8 Flash Structured Layout & Typography Design Model
    private val autoLayoutSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "projectTitle" to Schema.string(description = "Creative title for the design project"),
                "backgroundPrimaryHex" to Schema.string(description = "Primary background hex color like #0F172A"),
                "backgroundSecondaryHex" to Schema.string(description = "Secondary gradient hex color like #3B0764"),
                "headlineText" to Schema.string(description = "Main poster headline in Persian or English matching user prompt"),
                "headlineColorHex" to Schema.string(description = "Main headline hex color like #FDE047"),
                "headlineSecondaryHex" to Schema.string(description = "Headline gradient second hex color like #F59E0B"),
                "headlineStrokeHex" to Schema.string(description = "Headline stroke outline hex color like #090D16"),
                "fontId" to Schema.enumeration(
                    values = listOf("lalezar", "vazirmatn", "amiri", "montserrat", "playfair", "pacifico"),
                    description = "Best matching font family ID"
                ),
                "depth3d" to Schema.integer(description = "3D extrusion depth between 10 and 45"),
                "curveAngle" to Schema.integer(description = "Text curve angle in degrees between -120 and 120, 0 for straight"),
                "subtitleText" to Schema.string(description = "Supporting subtitle or call-to-action text"),
                "subtitleColorHex" to Schema.string(description = "Subtitle hex color like #E2E8F0"),
                "badgeShape" to Schema.enumeration(
                    values = listOf("HEXAGON", "STAR", "CIRCLE", "BADGE", "HEART", "RECTANGLE"),
                    description = "Decorative shape backdrop kind"
                ),
                "badgeColorHex" to Schema.string(description = "Decorative shape hex color")
            )
        )
    }

    private fun getLayoutAiModel() = Firebase.ai.generativeModel(
        modelName = _selectedGeminiModel.value,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = autoLayoutSchema
        }
    )

    fun setAuthenticatedUser(userId: String?) {
        _currentUserId.value = userId
    }

    fun clearAgentHistory() {
        _agentMessages.value = emptyList()
        _activeTransactionSteps.value = emptyList()
        _pendingAgentTransaction.value = null
        _activeAiMutatedLayerId.value = null
        _lastGeminiError.value = null
    }

    fun syncSelectedGeminiModel(context: Context) {
        _selectedGeminiModel.value = PixelLabAgentEngine.getSelectedModelName(context)
    }

    fun setSelectedGeminiModel(context: Context, modelName: String) {
        val clean = modelName.trim().ifBlank { com.example.ui.agent.GeminiModelPreset.GEMINI_3_8_FLASH.modelId }
        PixelLabAgentEngine.setSelectedModelName(context, clean)
        _selectedGeminiModel.value = clean
        _statusBanner.value = "مدل هوش مصنوعی Gemini روی «$clean» تنظیم شد."
    }

    fun dismissGeminiError() {
        _lastGeminiError.value = null
    }

    fun confirmPendingAgentTransaction() {
        val tx = _pendingAgentTransaction.value ?: return
        _project.value = tx.postTransactionProject
        _selectedLayerId.value = tx.postTransactionSelectedLayerId
        _pendingAgentTransaction.value = null
        _activeAiMutatedLayerId.value = null
        _statusBanner.value = "تغییرات ایجنت هوشمند تأیید و نهایی شد (${tx.steps.size} گام اجرا شد)."
    }

    fun rollbackPendingAgentTransaction() {
        val tx = _pendingAgentTransaction.value
        if (tx != null) {
            _project.value = tx.preTransactionProject
            _selectedLayerId.value = tx.preTransactionSelectedLayerId
            _pendingAgentTransaction.value = null
            _activeAiMutatedLayerId.value = null
            _statusBanner.value = "تغییرات ایجنت لغو شد و پروژه به وضعیت قبل از تراکنش بازگشت (undo_transaction)."
        } else if (_canUndo.value) {
            undo()
            _activeAiMutatedLayerId.value = null
            _statusBanner.value = "آخرین عملیات ایجنت به حالت قبل بازگردانده شد."
        }
    }

    fun executeRealAgentToolDirectly(
        context: Context,
        toolName: String,
        argsJson: JSONObject = JSONObject()
    ): JSONObject {
        val adapter = com.example.ui.agent.GeminiCanvasAdapter.createSession(
            context = context,
            userCommand = "اجرای مستقیم ابزار $toolName",
            initialProject = _project.value,
            initialSelectedLayerId = _selectedLayerId.value
        )
        adapter.onLayerHighlighted = { layerId, _ ->
            _activeAiMutatedLayerId.value = layerId
        }
        val output = adapter.executeToolAsOrgJson(
            toolName = toolName,
            orgJsonArgs = argsJson
        )
        if (adapter.currentWorkingProject != _project.value) {
            pushUndoSnapshot()
            _project.value = adapter.currentWorkingProject
            _selectedLayerId.value = adapter.selectedLayerId
        }
        _activeTransactionSteps.value = adapter.steps
        return output
    }

    private fun pushUndoSnapshot() {
        undoStack.addLast(_project.value)
        if (undoStack.size > 30) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        updateHistoryFlags()
    }

    private fun updateHistoryFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _project.value
        val previous = undoStack.removeLast()
        redoStack.addLast(current)
        _project.value = previous
        if (_selectedLayerId.value !in previous.layers.map { it.id }) {
            _selectedLayerId.value = previous.layers.lastOrNull()?.id
        }
        updateHistoryFlags()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _project.value
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        _project.value = next
        updateHistoryFlags()
    }

    fun selectTab(tab: StudioBottomTab) {
        _activeTab.value = tab
        if (tab != StudioBottomTab.OBJECTS) {
            _isDrawingMode.value = false
        }
    }

    fun cycleThemeMode() {
        _themeMode.value = when (_themeMode.value) {
            com.example.ui.theme.StudioThemeMode.AMOLED -> com.example.ui.theme.StudioThemeMode.DARK
            com.example.ui.theme.StudioThemeMode.DARK -> com.example.ui.theme.StudioThemeMode.LIGHT
            com.example.ui.theme.StudioThemeMode.LIGHT -> com.example.ui.theme.StudioThemeMode.AMOLED
        }
    }

    fun setThemeMode(mode: com.example.ui.theme.StudioThemeMode) {
        _themeMode.value = mode
    }

    fun toggleAgentBar() {
        _isAgentBarVisible.value = !_isAgentBarVisible.value
    }

    fun toggleCanvasExpanded() {
        _isCanvasExpandedMode.value = !_isCanvasExpandedMode.value
    }

    fun toggleGrid() {
        _showGrid.value = !_showGrid.value
    }

    fun setDrawingMode(enabled: Boolean) {
        _isDrawingMode.value = enabled
        if (enabled) {
            _isEraserMode.value = false
            _isZoomPanModeActive.value = false
        }
    }

    fun setEraserMode(enabled: Boolean) {
        _isEraserMode.value = enabled
        if (enabled) {
            _isDrawingMode.value = false
            _isZoomPanModeActive.value = false
            val sel = _project.value.layers.find { it.id == _selectedLayerId.value }
            if (sel is ImageLayer) {
                _eraserBrushSize.value = sel.eraserBrushSize.coerceIn(4f, 90f)
            }
            _statusBanner.value = "پاک‌کن تعاملی فعال شد: با کشیدن انگشت روی بوم، بخش دلخواه لایه انتخابی را پاک کنید."
        }
    }

    fun setEraserBrushSize(size: Float) {
        val clamped = size.coerceIn(4f, 90f)
        _eraserBrushSize.value = clamped
        val sel = _project.value.layers.find { it.id == _selectedLayerId.value }
        if (sel is ImageLayer) {
            updateSelectedLayer(recordUndo = false) { layer ->
                if (layer is ImageLayer) layer.copy(eraserBrushSize = clamped) else layer
            }
        }
    }

    fun toggleZoomPanMode(active: Boolean? = null) {
        val next = active ?: !_isZoomPanModeActive.value
        _isZoomPanModeActive.value = next
        if (next) {
            _isDrawingMode.value = false
            _isEraserMode.value = false
            _statusBanner.value = "🔍 ابزار بزرگ‌نمایی و جابه‌جایی بوم فعال شد (${(_canvasZoomScale.value * 100).toInt()}٪)."
        }
    }

    fun setCanvasZoom(scale: Float) {
        val clamped = scale.coerceIn(1.0f, 5.0f)
        _canvasZoomScale.value = clamped
        if (clamped <= 1.01f) {
            _canvasPanOffset.value = Point2D(0f, 0f)
        } else {
            val maxPan = (clamped - 1f) * 0.55f
            val cur = _canvasPanOffset.value
            _canvasPanOffset.value = Point2D(
                x = cur.x.coerceIn(-maxPan, maxPan),
                y = cur.y.coerceIn(-maxPan, maxPan)
            )
        }
    }

    fun panAndZoomCanvasByGesture(
        panDxNorm: Float,
        panDyNorm: Float,
        zoomChange: Float = 1f,
        zoomFactor: Float = zoomChange
    ) {
        val effectiveZoom = if (zoomChange != 1f) zoomChange else zoomFactor
        val newZoom = (_canvasZoomScale.value * effectiveZoom).coerceIn(1.0f, 5.0f)
        _canvasZoomScale.value = newZoom
        if (newZoom <= 1.01f) {
            _canvasPanOffset.value = Point2D(0f, 0f)
        } else {
            val maxPan = (newZoom - 1f) * 0.55f
            val cur = _canvasPanOffset.value
            _canvasPanOffset.value = Point2D(
                x = (cur.x + panDxNorm).coerceIn(-maxPan, maxPan),
                y = (cur.y + panDyNorm).coerceIn(-maxPan, maxPan)
            )
        }
    }

    fun resetCanvasZoomAndPan() {
        _canvasZoomScale.value = 1.0f
        _canvasPanOffset.value = Point2D(0f, 0f)
        _statusBanner.value = "بزرگ‌نمایی بوم به ۱۰۰٪ و موقعیت مرکزی بازنشانی شد."
    }

    /**
     * Converts a normalized viewport touch coordinate (0..1, 0..1) into logical canvas normalized
     * coordinates (0..1, 0..1) accounting for active Zoom (100%..500%) and Pan offset.
     */
    fun screenToCanvasNormalized(screenNormX: Float, screenNormY: Float): Point2D {
        val z = _canvasZoomScale.value.coerceIn(1.0f, 5.0f)
        val pan = _canvasPanOffset.value
        val cx = 0.5f + (screenNormX - 0.5f - pan.x) / z
        val cy = 0.5f + (screenNormY - 0.5f - pan.y) / z
        return Point2D(cx, cy)
    }

    fun screenToCanvasNormalized(
        screenPxX: Float,
        screenPxY: Float,
        viewportWidth: Float,
        viewportHeight: Float
    ): Point2D {
        val normX = (screenPxX / viewportWidth.coerceAtLeast(1f)).coerceIn(0f, 1f)
        val normY = (screenPxY / viewportHeight.coerceAtLeast(1f)).coerceIn(0f, 1f)
        return screenToCanvasNormalized(normX, normY)
    }

    fun setPenOptions(colorHex: String? = null, width: Float? = null, neon: Boolean? = null, dashed: Boolean? = null) {
        colorHex?.let { _penColorHex.value = it }
        width?.let { _penWidth.value = it }
        neon?.let { _penNeonGlow.value = it }
        dashed?.let { _penDashed.value = it }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun dismissAiState() {
        _aiState.value = AiTaskUiState.Idle
    }

    fun selectLayer(layerId: String?) {
        _selectedLayerId.value = layerId
    }

    // --- Layer Stack Operations ---

    fun addTextLayer(initialText: String = "متن جدید سه‌بعدی") {
        pushUndoSnapshot()
        val newLayer = TextLayer(
            name = "متن ${_project.value.layers.size + 1}",
            text = initialText,
            y = ((_project.value.layers.size % 5) - 2) * 0.06f
        )
        _project.value = _project.value.copy(layers = _project.value.layers + newLayer)
        _selectedLayerId.value = newLayer.id
        _activeTab.value = StudioBottomTab.TEXT
    }

    fun addShapeLayer(kind: ShapeKind = ShapeKind.HEXAGON) {
        pushUndoSnapshot()
        val defaultBezierNodes = if (kind == ShapeKind.BEZIER_PATH) {
            listOf(
                com.example.data.BezierNode(x = -0.35f, y = 0.15f, ctrlX = -0.20f, ctrlY = -0.35f),
                com.example.data.BezierNode(x = 0.0f, y = -0.05f, ctrlX = 0.20f, ctrlY = 0.35f),
                com.example.data.BezierNode(x = 0.35f, y = -0.15f, ctrlX = 0.0f, ctrlY = -0.35f)
            )
        } else {
            emptyList()
        }
        val newLayer = ShapeLayer(
            name = kind.labelFa,
            shapeKind = kind,
            bezierNodes = defaultBezierNodes,
            isBezierClosed = false,
            isBezierFilled = kind != ShapeKind.BEZIER_PATH,
            stroke = if (kind == ShapeKind.BEZIER_PATH) {
                LayerStroke(enabled = true, colorHex = "#38BDF8", width = 10f)
            } else {
                LayerStroke(enabled = true, colorHex = "#FFFFFF", width = 4f)
            }
        )
        _project.value = _project.value.copy(layers = _project.value.layers + newLayer)
        _selectedLayerId.value = newLayer.id
        _activeTab.value = StudioBottomTab.OBJECTS
    }

    fun addImageLayerFromBitmap(bitmap: Bitmap, layerName: String = "لایه تصویر") {
        pushUndoSnapshot()
        val newLayer = ImageLayer(
            name = layerName,
            bitmap = bitmap,
            originalBitmap = bitmap
        )
        _project.value = _project.value.copy(layers = _project.value.layers + newLayer)
        _selectedLayerId.value = newLayer.id
        _activeTab.value = StudioBottomTab.OBJECTS
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var inSampleSize = 1
            val maxOriginal = maxOf(origWidth, origHeight)
            while (maxOriginal / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            AgentDiagnosticsLogger.warn("ImageLoader", "Failed to decode sampled bitmap", e.message)
            null
        }
    }

    fun importImageFromUri(context: Context, uri: Uri, asBackground: Boolean) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                decodeSampledBitmapFromUri(context, uri, maxDimension = 2048)
            }
            if (bitmap != null) {
                if (asBackground) {
                    pushUndoSnapshot()
                    _project.value = _project.value.copy(
                        background = _project.value.background.copy(
                            type = BackgroundType.IMAGE,
                            backgroundBitmap = bitmap
                        )
                    )
                    _statusBanner.value = "تصویر گالری به عنوان پس‌زمینه بوم تنظیم شد."
                } else {
                    addImageLayerFromBitmap(bitmap, "تصویر گالری")
                    _statusBanner.value = "تصویر جدید به لایه‌ها اضافه شد."
                }
            } else {
                _statusBanner.value = "خطا در بارگذاری تصویر از گالری."
            }
        }
    }

    fun importCustomTextureForSelectedText(context: Context, uri: Uri) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                decodeSampledBitmapFromUri(context, uri, maxDimension = 1024)
            }
            if (bitmap != null) {
                updateSelectedTextLayer {
                    it.copy(
                        texturePreset = TexturePreset.CUSTOM_BITMAP,
                        customTextureBitmap = bitmap
                    )
                }
                _statusBanner.value = "تصویر دلخواه به عنوان بافت (Texture) داخل متن اعمال شد."
            } else {
                _statusBanner.value = "خطا در بارگذاری تصویر بافت."
            }
        }
    }

    fun refreshCustomFonts(context: Context) {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val dir = File(context.filesDir, "custom_fonts")
                if (!dir.exists()) emptyList()
                else dir.listFiles { f ->
                    f.extension.equals("ttf", ignoreCase = true) || f.extension.equals("otf", ignoreCase = true)
                }?.map { file ->
                    CustomUserFont(
                        id = file.nameWithoutExtension,
                        displayName = "فونت: ${file.nameWithoutExtension.take(16)}",
                        filePath = file.absolutePath
                    )
                } ?: emptyList()
            }
            _customUserFonts.value = loaded
        }
    }

    fun importCustomFontFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val savedFont = withContext(Dispatchers.IO) {
                try {
                    val dir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
                    val fontFile = File(dir, "user_font_${System.currentTimeMillis() % 100000}.ttf")
                    context.contentResolver.openInputStream(uri)?.use { ins ->
                        fontFile.outputStream().use { outs ->
                            ins.copyTo(outs)
                        }
                    }
                    if (fontFile.exists() && fontFile.length() > 128) {
                        CustomUserFont(
                            id = fontFile.nameWithoutExtension,
                            displayName = "فونت اختصاصی (${_customUserFonts.value.size + 1})",
                            filePath = fontFile.absolutePath
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            if (savedFont != null) {
                refreshCustomFonts(context)
                updateSelectedTextLayer {
                    it.copy(customFontPath = savedFont.filePath)
                }
                _statusBanner.value = "فونت سفارشی (.ttf/.otf) بارگذاری و روی لایه متن اعمال شد!"
            } else {
                _statusBanner.value = "خطا در بارگذاری فایل فونت."
            }
        }
    }

    fun applyQuoteItem(quote: StudioQuoteItem) {
        pushUndoSnapshot()
        val selected = _project.value.layers.find { it.id == _selectedLayerId.value }
        if (selected is TextLayer) {
            updateSelectedTextLayer {
                it.copy(
                    text = quote.text,
                    fontId = quote.recommendedFont.id,
                    customFontPath = ""
                )
            }
        } else {
            val newLayer = TextLayer(
                name = "جمله (${quote.categoryFa})",
                text = quote.text,
                fontId = quote.recommendedFont.id,
                fontSizeSp = 44f
            )
            _project.value = _project.value.copy(layers = _project.value.layers + newLayer)
            _selectedLayerId.value = newLayer.id
        }
        _statusBanner.value = "جمله آماده «${quote.text.take(24)}...» روی بوم قرار گرفت."
    }

    fun addBuiltinStickerLayer(sticker: BuiltinStickerItem) {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.Default) {
                StudioCanvasRenderer.renderBuiltinStickerToBitmap(
                    symbolText = sticker.symbolText,
                    primaryHex = sticker.primaryHex,
                    secondaryHex = sticker.secondaryHex
                )
            }
            pushUndoSnapshot()
            val layer = ImageLayer(
                name = "استیکر: ${sticker.labelFa}",
                bitmap = bmp,
                scaleX = 0.65f,
                scaleY = 0.65f,
                shadow = LayerShadow(enabled = true, blurRadius = 18f, offsetY = 8f)
            )
            _project.value = _project.value.copy(layers = _project.value.layers + layer)
            _selectedLayerId.value = layer.id
            _statusBanner.value = "استیکر «${sticker.labelFa}» به لایه‌ها اضافه شد."
        }
    }

    fun appendFreehandStroke(points: List<Point2D>) {
        if (points.size < 2) return
        pushUndoSnapshot()
        val stroke = DrawnStroke(
            points = points,
            colorHex = _penColorHex.value,
            strokeWidth = _penWidth.value,
            isDashed = _penDashed.value,
            neonGlow = _penNeonGlow.value,
            isEraser = false
        )
        val currentLayers = _project.value.layers.toMutableList()
        val selectedIdx = currentLayers.indexOfFirst { it.id == _selectedLayerId.value && it is DrawingLayer }
        if (selectedIdx >= 0) {
            val existing = currentLayers[selectedIdx] as DrawingLayer
            currentLayers[selectedIdx] = existing.copy(strokes = existing.strokes + stroke)
            _project.value = _project.value.copy(layers = currentLayers)
        } else {
            val newDrawingLayer = DrawingLayer(
                name = "طراحی با قلم ${currentLayers.count { it is DrawingLayer } + 1}",
                strokes = listOf(stroke)
            )
            _project.value = _project.value.copy(layers = currentLayers + newDrawingLayer)
            _selectedLayerId.value = newDrawingLayer.id
        }
    }

    /**
     * Appends an interactive touch eraser stroke to ONLY the currently selected ImageLayer or DrawingLayer,
     * preserving Undo/Redo history and preventing unintended changes to other layers.
     */
    fun appendInteractiveEraserStroke(canvasNormalizedPoints: List<Point2D>) {
        if (canvasNormalizedPoints.isEmpty()) return
        val targetId = _selectedLayerId.value ?: return
        val currentLayers = _project.value.layers.toMutableList()
        val selectedIdx = currentLayers.indexOfFirst { it.id == targetId }
        if (selectedIdx < 0) return

        val selectedLayer = currentLayers[selectedIdx]
        if (selectedLayer.isLocked) {
            _statusBanner.value = "لایه انتخاب‌شده قفل است؛ ابتدا قفل آن را باز کنید."
            return
        }

        val cw = _project.value.canvasWidth.toFloat().coerceAtLeast(1f)
        val ch = _project.value.canvasHeight.toFloat().coerceAtLeast(1f)
        val brushWidth = _eraserBrushSize.value.coerceIn(4f, 90f)
        val effectivePoints = if (canvasNormalizedPoints.size == 1) {
            val pt = canvasNormalizedPoints.first()
            listOf(pt, Point2D(pt.x + 0.001f, pt.y + 0.001f))
        } else {
            canvasNormalizedPoints
        }

        when (selectedLayer) {
            is ImageLayer -> {
                val localPts = StudioCanvasRenderer.mapCanvasNormalizedToImageLocal(
                    layer = selectedLayer,
                    canvasPoints = effectivePoints,
                    viewportWidth = cw,
                    viewportHeight = ch
                )
                pushUndoSnapshot()
                val eraserStroke = DrawnStroke(
                    points = localPts,
                    strokeWidth = brushWidth / selectedLayer.scaleX.coerceAtLeast(0.25f),
                    neonGlow = false,
                    isEraser = true
                )
                currentLayers[selectedIdx] = selectedLayer.copy(
                    eraserStrokes = selectedLayer.eraserStrokes + eraserStroke,
                    eraserBrushSize = brushWidth
                )
                _project.value = _project.value.copy(layers = currentLayers)
            }
            is DrawingLayer -> {
                val localPts = StudioCanvasRenderer.mapCanvasNormalizedToDrawingLocal(
                    layer = selectedLayer,
                    canvasPoints = effectivePoints,
                    viewportWidth = cw,
                    viewportHeight = ch
                )
                pushUndoSnapshot()
                val eraserStroke = DrawnStroke(
                    points = localPts,
                    strokeWidth = brushWidth / selectedLayer.scaleX.coerceAtLeast(0.25f),
                    neonGlow = false,
                    isDashed = false,
                    isEraser = true
                )
                currentLayers[selectedIdx] = selectedLayer.copy(
                    strokes = selectedLayer.strokes + eraserStroke
                )
                _project.value = _project.value.copy(layers = currentLayers)
            }
            else -> {
                _statusBanner.value = "پاک‌کن تعاملی مخصوص لایه‌های «تصویر / استیکر» و «طراحی با قلم» است."
            }
        }
    }

    fun clearLayerEraserStrokes() {
        val sel = _project.value.layers.find { it.id == _selectedLayerId.value } ?: return
        when (sel) {
            is ImageLayer -> {
                if (sel.eraserStrokes.isEmpty()) return
                updateSelectedImageLayer { it.copy(eraserStrokes = emptyList()) }
                _statusBanner.value = "تمام خطوط پاک‌کن روی تصویر بازنشانی شد."
            }
            is DrawingLayer -> {
                if (sel.strokes.none { it.isEraser }) return
                updateSelectedLayer(recordUndo = true) { layer ->
                    if (layer is DrawingLayer) layer.copy(strokes = layer.strokes.filterNot { it.isEraser }) else layer
                }
                _statusBanner.value = "خطوط پاک‌کن روی نقاشی بازنشانی شد."
            }
            else -> {}
        }
    }

    fun duplicateLayer(layerId: String) {
        val target = _project.value.layers.find { it.id == layerId } ?: return
        pushUndoSnapshot()
        val newId = UUID.randomUUID().toString()
        val offset = 0.04f
        val copyLayer: StudioLayer = when (target) {
            is TextLayer -> target.copy(id = newId, name = "${target.name} (کپی)", x = (target.x + offset).coerceIn(-0.45f, 0.45f), y = (target.y + offset).coerceIn(-0.45f, 0.45f))
            is ShapeLayer -> target.copy(id = newId, name = "${target.name} (کپی)", x = (target.x + offset).coerceIn(-0.45f, 0.45f), y = (target.y + offset).coerceIn(-0.45f, 0.45f))
            is ImageLayer -> target.copy(id = newId, name = "${target.name} (کپی)", x = (target.x + offset).coerceIn(-0.45f, 0.45f), y = (target.y + offset).coerceIn(-0.45f, 0.45f))
            is DrawingLayer -> target.copy(id = newId, name = "${target.name} (کپی)", x = (target.x + offset).coerceIn(-0.45f, 0.45f), y = (target.y + offset).coerceIn(-0.45f, 0.45f))
        }
        _project.value = _project.value.copy(layers = _project.value.layers + copyLayer)
        _selectedLayerId.value = newId
    }

    fun deleteLayer(layerId: String) {
        if (_project.value.layers.isEmpty()) return
        pushUndoSnapshot()
        val remaining = _project.value.layers.filterNot { it.id == layerId }
        val updatedBg = if (remaining.isEmpty() && _project.value.background.backgroundBitmap != null) {
            _project.value.background.copy(
                type = BackgroundType.GRADIENT,
                backgroundBitmap = null
            )
        } else {
            _project.value.background
        }
        _project.value = _project.value.copy(
            layers = remaining,
            background = updatedBg
        )
        if (_selectedLayerId.value == layerId) {
            _selectedLayerId.value = remaining.lastOrNull()?.id
        }
    }

    fun clearAllLayers(clearBackgroundImage: Boolean = true) {
        pushUndoSnapshot()
        val currentBg = _project.value.background
        val cleanBg = if (clearBackgroundImage && (currentBg.type == BackgroundType.IMAGE || currentBg.backgroundBitmap != null)) {
            currentBg.copy(
                type = BackgroundType.GRADIENT,
                backgroundBitmap = null
            )
        } else {
            currentBg
        }
        _project.value = _project.value.copy(
            layers = emptyList(),
            background = cleanBg
        )
        _selectedLayerId.value = null
        _statusBanner.value = "تمام لایه‌های بوم با موفقیت پاک‌سازی شدند."
    }

    fun resetCanvasToBlank() {
        pushUndoSnapshot()
        _project.value = _project.value.copy(
            layers = emptyList(),
            background = CanvasBackground(
                type = BackgroundType.GRADIENT,
                primaryColorHex = "#090D16",
                secondaryColorHex = "#1E1B4B",
                gradientAngle = 135f,
                isRadialGradient = false,
                backgroundBitmap = null
            ),
            postFx = CanvasPostFx()
        )
        _selectedLayerId.value = null
        _statusBanner.value = "بوم کاملاً پاک‌سازی و به حالت اولیه بازنشانی شد."
    }

    fun unlockAllLayers() {
        if (_project.value.layers.none { it.isLocked }) {
            _statusBanner.value = "همه لایه‌ها از قبل باز هستند."
            return
        }
        pushUndoSnapshot()
        _project.value = _project.value.copy(
            layers = _project.value.layers.map { layer ->
                when (layer) {
                    is TextLayer -> layer.copy(isLocked = false)
                    is ShapeLayer -> layer.copy(isLocked = false)
                    is ImageLayer -> layer.copy(isLocked = false)
                    is DrawingLayer -> layer.copy(isLocked = false)
                }
            }
        )
        _statusBanner.value = "قفل تمام لایه‌ها باز شد؛ اکنون همه اجزا قابل انتخاب و جابه‌جایی هستند."
    }

    fun toggleLayerVisibility(layerId: String) {
        pushUndoSnapshot()
        _project.value = _project.value.copy(
            layers = _project.value.layers.map { layer ->
                if (layer.id != layerId) layer else when (layer) {
                    is TextLayer -> layer.copy(isVisible = !layer.isVisible)
                    is ShapeLayer -> layer.copy(isVisible = !layer.isVisible)
                    is ImageLayer -> layer.copy(isVisible = !layer.isVisible)
                    is DrawingLayer -> layer.copy(isVisible = !layer.isVisible)
                }
            }
        )
    }

    fun toggleLayerLock(layerId: String) {
        pushUndoSnapshot()
        _project.value = _project.value.copy(
            layers = _project.value.layers.map { layer ->
                if (layer.id != layerId) layer else when (layer) {
                    is TextLayer -> layer.copy(isLocked = !layer.isLocked)
                    is ShapeLayer -> layer.copy(isLocked = !layer.isLocked)
                    is ImageLayer -> layer.copy(isLocked = !layer.isLocked)
                    is DrawingLayer -> layer.copy(isLocked = !layer.isLocked)
                }
            }
        )
    }

    fun moveLayerOrder(layerId: String, moveUpInVisualStack: Boolean) {
        val list = _project.value.layers.toMutableList()
        val idx = list.indexOfFirst { it.id == layerId }
        if (idx == -1) return
        val targetIdx = if (moveUpInVisualStack) idx + 1 else idx - 1
        if (targetIdx !in list.indices) return
        pushUndoSnapshot()
        val item = list.removeAt(idx)
        list.add(targetIdx, item)
        _project.value = _project.value.copy(layers = list)
    }

    fun bringLayerToFront(layerId: String) {
        val list = _project.value.layers.toMutableList()
        val idx = list.indexOfFirst { it.id == layerId }
        if (idx == -1 || idx == list.lastIndex) return
        pushUndoSnapshot()
        val item = list.removeAt(idx)
        list.add(item)
        _project.value = _project.value.copy(layers = list)
    }

    fun sendLayerToBack(layerId: String) {
        val list = _project.value.layers.toMutableList()
        val idx = list.indexOfFirst { it.id == layerId }
        if (idx <= 0) return
        pushUndoSnapshot()
        val item = list.removeAt(idx)
        list.add(0, item)
        _project.value = _project.value.copy(layers = list)
    }

    fun mergeVisibleLayersToImage(context: Context) {
        val visibleCount = _project.value.layers.count { it.isVisible }
        if (visibleCount < 2) {
            _statusBanner.value = "برای ادغام لایه‌ها باید حداقل ۲ لایه قابل مشاهده وجود داشته باشد."
            return
        }
        pushUndoSnapshot()
        val tempProject = _project.value.copy(
            background = CanvasBackground(type = BackgroundType.TRANSPARENT),
            postFx = CanvasPostFx(vignetteEnabled = false, noiseEnabled = false, stripesEnabled = false)
        )
        val mergedBitmap = StudioCanvasRenderer.renderProjectToBitmap(
            context = context,
            project = tempProject,
            scaleMultiplier = 1f,
            includeCheckerboardForTransparent = false
        )
        val hiddenLayers = _project.value.layers.filter { !it.isVisible }
        val mergedLayer = ImageLayer(
            name = "لایه ادغام‌شده (${visibleCount} لایه)",
            bitmap = mergedBitmap
        )
        _project.value = _project.value.copy(layers = hiddenLayers + mergedLayer)
        _selectedLayerId.value = mergedLayer.id
        _statusBanner.value = "$visibleCount لایه با موفقیت به یک لایه تصویری شفاف ادغام شدند."
    }

    fun mergeSelectedLayersToImage(context: Context, layerIds: Collection<String>) {
        val idSet = layerIds.toSet()
        val targetLayers = _project.value.layers.filter { it.id in idSet }
        if (targetLayers.size < 2) {
            _statusBanner.value = "لطفاً حداقل ۲ لایه را برای ادغام انتخاب کنید."
            return
        }
        val mergedResult = StudioCanvasRenderer.renderSubsetLayersToMergedBitmap(
            context = context,
            project = _project.value,
            layerIdsToMerge = idSet
        ) ?: return
        val (mergedBitmap, normX, normY) = mergedResult
        pushUndoSnapshot()
        val firstSelectedIndex = _project.value.layers.indexOfFirst { it.id in idSet }.coerceAtLeast(0)
        val remainingLayers = _project.value.layers.filterNot { it.id in idSet }.toMutableList()
        val mergedLayer = ImageLayer(
            name = "ادغام انتخابی (${targetLayers.size} لایه)",
            bitmap = mergedBitmap,
            x = normX,
            y = normY
        )
        val insertIndex = firstSelectedIndex.coerceAtMost(remainingLayers.size)
        remainingLayers.add(insertIndex, mergedLayer)
        _project.value = _project.value.copy(layers = remainingLayers)
        _selectedLayerId.value = mergedLayer.id
        _statusBanner.value = "${targetLayers.size} لایه انتخابی با حفظ شفافیت در یک لایه ادغام شدند."
    }

    fun deleteSelectedLayers(layerIds: Collection<String>) {
        val idSet = layerIds.toSet()
        if (idSet.isEmpty()) return
        pushUndoSnapshot()
        val remaining = _project.value.layers.filterNot { it.id in idSet }
        _project.value = _project.value.copy(layers = remaining)
        if (_selectedLayerId.value in idSet) {
            _selectedLayerId.value = remaining.lastOrNull()?.id
        }
        _statusBanner.value = "${idSet.size} لایه انتخاب‌شده حذف شدند."
    }

    fun groupSelectedLayers(layerIds: Collection<String>) {
        val idSet = layerIds.toSet()
        if (idSet.size < 2) {
            _statusBanner.value = "حداقل ۲ لایه را برای گروه‌بندی انتخاب کنید."
            return
        }
        pushUndoSnapshot()
        val newGroupId = "group_${UUID.randomUUID().toString().take(8)}"
        val updatedLayers = _project.value.layers.map { layer ->
            if (layer.id in idSet) {
                when (layer) {
                    is TextLayer -> layer.copy(groupId = newGroupId)
                    is ShapeLayer -> layer.copy(groupId = newGroupId)
                    is ImageLayer -> layer.copy(groupId = newGroupId)
                    is DrawingLayer -> layer.copy(groupId = newGroupId)
                }
            } else layer
        }
        _project.value = _project.value.copy(layers = updatedLayers)
        _statusBanner.value = "${idSet.size} لایه با موفقیت در یک گروه قرار گرفتند."
    }

    fun ungroupLayers(groupId: String) {
        pushUndoSnapshot()
        val updatedLayers = _project.value.layers.map { layer ->
            if (layer.groupId == groupId) {
                when (layer) {
                    is TextLayer -> layer.copy(groupId = null)
                    is ShapeLayer -> layer.copy(groupId = null)
                    is ImageLayer -> layer.copy(groupId = null)
                    is DrawingLayer -> layer.copy(groupId = null)
                }
            } else layer
        }
        _project.value = _project.value.copy(layers = updatedLayers)
        _statusBanner.value = "گروه لایه‌ها تفکیک شد."
    }

    fun ungroupSelectedLayers(layerIds: Collection<String>) {
        val idSet = layerIds.toSet()
        pushUndoSnapshot()
        val updatedLayers = _project.value.layers.map { layer ->
            if (layer.id in idSet && layer.groupId != null) {
                when (layer) {
                    is TextLayer -> layer.copy(groupId = null)
                    is ShapeLayer -> layer.copy(groupId = null)
                    is ImageLayer -> layer.copy(groupId = null)
                    is DrawingLayer -> layer.copy(groupId = null)
                }
            } else layer
        }
        _project.value = _project.value.copy(layers = updatedLayers)
        _statusBanner.value = "لایه‌های انتخابی از گروه خارج شدند."
    }

    fun applyMemeGenerator(
        topText: String = "متن بالای میم",
        bottomText: String = "متن پایین میم"
    ) {
        pushUndoSnapshot()
        val topLayer = TextLayer(
            name = "متن بالا (Meme)",
            text = topText,
            fontSizeSp = 48f,
            colorHex = "#FFFFFF",
            fontId = StudioFont.LALEZAR.id,
            isBold = true,
            x = 0f,
            y = -0.38f,
            stroke = LayerStroke(enabled = true, colorHex = "#000000", width = 12f),
            shadow = LayerShadow(enabled = true, colorHex = "#CC000000", blurRadius = 14f, offsetX = 0f, offsetY = 6f)
        )
        val bottomLayer = TextLayer(
            name = "متن پایین (Meme)",
            text = bottomText,
            fontSizeSp = 48f,
            colorHex = "#FFFFFF",
            fontId = StudioFont.LALEZAR.id,
            isBold = true,
            x = 0f,
            y = 0.38f,
            stroke = LayerStroke(enabled = true, colorHex = "#000000", width = 12f),
            shadow = LayerShadow(enabled = true, colorHex = "#CC000000", blurRadius = 14f, offsetX = 0f, offsetY = 6f)
        )
        val currentLayers = _project.value.layers
        _project.value = _project.value.copy(
            canvasWidth = 1080,
            canvasHeight = 1080,
            layers = currentLayers + listOf(topLayer, bottomLayer)
        )
        _selectedLayerId.value = bottomLayer.id
        _statusBanner.value = "قالب میم با لایه‌های متنی استاندارد بالا و پایین اضافه شد."
    }

    fun copyLayerStyle(layerId: String) {
        val source = _project.value.layers.find { it.id == layerId } ?: return
        _copiedLayerStyleSource = source
        _hasCopiedLayerStyle.value = true
        _statusBanner.value = "استایل لایه «${source.name}» کپی شد. روی لایه دیگر Paste کنید."
    }

    fun pasteLayerStyle(targetLayerId: String) {
        val source = _copiedLayerStyleSource ?: return
        val target = _project.value.layers.find { it.id == targetLayerId } ?: return
        pushUndoSnapshot()
        val updatedLayers = _project.value.layers.map { layer ->
            if (layer.id != target.id) {
                layer
            } else {
                when (layer) {
                    is TextLayer -> {
                        if (source is TextLayer) {
                            layer.copy(
                                fontId = source.fontId,
                                customFontPath = source.customFontPath,
                                colorHex = source.colorHex,
                                useGradientFill = source.useGradientFill,
                                gradientSecondColorHex = source.gradientSecondColorHex,
                                gradientDirection = source.gradientDirection,
                                gradientStops = source.gradientStops,
                                stroke = source.stroke,
                                secondStrokeEnabled = source.secondStrokeEnabled,
                                secondStrokeColorHex = source.secondStrokeColorHex,
                                secondStrokeWidth = source.secondStrokeWidth,
                                shadow = source.shadow,
                                innerShadow = source.innerShadow,
                                emboss = source.emboss,
                                is3dEnabled = source.is3dEnabled,
                                depth3d = source.depth3d,
                                isPerspective3d = source.isPerspective3d,
                                obliqueAngle3d = source.obliqueAngle3d,
                                simulateLighting3d = source.simulateLighting3d,
                                lightAngle3d = source.lightAngle3d,
                                lightIntensity3d = source.lightIntensity3d,
                                ambientLight3d = source.ambientLight3d,
                                specularHardness3d = source.specularHardness3d,
                                bevel3d = source.bevel3d,
                                shadow3dEnabled = source.shadow3dEnabled,
                                reflectionEnabled = source.reflectionEnabled,
                                glitchOffsetPx = source.glitchOffsetPx,
                                backgroundPillEnabled = source.backgroundPillEnabled,
                                backgroundPillColorHex = source.backgroundPillColorHex,
                                backgroundPillGlassBlur = source.backgroundPillGlassBlur,
                                backgroundPillStrokeEnabled = source.backgroundPillStrokeEnabled,
                                backgroundPillStrokeColorHex = source.backgroundPillStrokeColorHex
                            )
                        } else {
                            layer.copy(
                                stroke = source.stroke,
                                shadow = source.shadow,
                                innerShadow = source.innerShadow,
                                emboss = source.emboss,
                                opacity = source.opacity
                            )
                        }
                    }
                    is ShapeLayer -> {
                        if (source is ShapeLayer) {
                            layer.copy(
                                fillColorHex = source.fillColorHex,
                                useGradientFill = source.useGradientFill,
                                gradientSecondColorHex = source.gradientSecondColorHex,
                                gradientDirection = source.gradientDirection,
                                gradientStops = source.gradientStops,
                                stroke = source.stroke,
                                shadow = source.shadow,
                                innerShadow = source.innerShadow,
                                emboss = source.emboss,
                                is3dExtruded = source.is3dExtruded,
                                depth3d = source.depth3d,
                                glitchOffsetPx = source.glitchOffsetPx
                            )
                        } else {
                            layer.copy(
                                stroke = source.stroke,
                                shadow = source.shadow,
                                innerShadow = source.innerShadow,
                                emboss = source.emboss,
                                opacity = source.opacity
                            )
                        }
                    }
                    is ImageLayer -> layer.copy(
                        stroke = source.stroke,
                        shadow = source.shadow,
                        innerShadow = source.innerShadow,
                        emboss = source.emboss,
                        opacity = source.opacity
                    )
                    is DrawingLayer -> layer.copy(
                        stroke = source.stroke,
                        shadow = source.shadow,
                        innerShadow = source.innerShadow,
                        emboss = source.emboss,
                        opacity = source.opacity
                    )
                }
            }
        }
        _project.value = _project.value.copy(layers = updatedLayers)
        _statusBanner.value = "استایل کپی‌شده روی لایه «${target.name}» اعمال شد."
    }

    fun toggleEyedropper(active: Boolean? = null) {
        val next = active ?: !_isEyedropperActive.value
        _isEyedropperActive.value = next
        _statusBanner.value = if (next) {
            "🎯 قطره‌چکان فعال شد: روی هر نقطه از بوم ضربه بزنید تا رنگ آن برداشته شود."
        } else {
            "قطره‌چکان غیرفعال شد."
        }
    }

    fun sampleEyedropperColorAt(context: Context, normX: Float, normY: Float) {
        val hex = StudioCanvasRenderer.sampleColorHexAtNormalizedPoint(
            context = context,
            project = _project.value,
            normX = normX,
            normY = normY
        )
        _isEyedropperActive.value = false
        val selected = _project.value.layers.find { it.id == _selectedLayerId.value }
        when (selected) {
            is TextLayer -> updateSelectedTextLayer { it.copy(colorHex = hex) }
            is ShapeLayer -> updateSelectedShapeLayer { it.copy(fillColorHex = hex) }
            else -> {}
        }
        if (hex !in _smartPaletteHexes.value) {
            _smartPaletteHexes.value = (listOf(hex) + _smartPaletteHexes.value).take(8)
        }
        _statusBanner.value = "🎨 رنگ $hex از روی بوم نمونه‌برداری و اعمال شد."
    }

    fun extractSmartPaletteFromCanvas(context: Context) {
        viewModelScope.launch {
            val paletteList = withContext(Dispatchers.Default) {
                StudioCanvasRenderer.extractSmartPaletteFromProject(context, _project.value)
            }
            _smartPaletteHexes.value = paletteList
            _statusBanner.value = "✨ پالت هوشمند (${paletteList.size} رنگ غالب) از بوم استخراج شد."
        }
    }

    fun saveCurrentLayerAsTextPreset() {
        val selected = _project.value.layers.find { it.id == _selectedLayerId.value } as? TextLayer ?: return
        val font = StudioFont.fromId(selected.fontId)
        val preset = TextStylePreset(
            id = "user_preset_${System.currentTimeMillis()}",
            titleFa = "استایل من ${_savedUserTextPresets.value.size + 1}",
            sampleText = selected.text.take(18).ifBlank { "استایل من" },
            font = font,
            primaryHex = selected.colorHex,
            secondaryHex = selected.gradientSecondColorHex,
            strokeHex = selected.stroke.colorHex,
            strokeWidth = selected.stroke.width,
            is3d = selected.is3dEnabled,
            depth3d = selected.depth3d,
            curveAngle = selected.curveAngle,
            emboss = selected.emboss.enabled,
            reflection = selected.reflectionEnabled
        )
        _savedUserTextPresets.value = listOf(preset) + _savedUserTextPresets.value
        _statusBanner.value = "⭐ استایل فعلی در بانک «استایل‌های من» ذخیره شد."
    }

    fun deleteSavedTextPreset(id: String) {
        _savedUserTextPresets.value = _savedUserTextPresets.value.filterNot { it.id == id }
    }

    // --- Interactive Gesture & Transform Mutations ---

    fun transformSelectedLayerByGesture(dxNorm: Float, dyNorm: Float, zoomChange: Float, rotationChangeDeg: Float) {
        val targetId = _selectedLayerId.value ?: return
        val currentLayer = _project.value.layers.find { it.id == targetId } ?: return
        if (currentLayer.isLocked) return

        var rawX = (currentLayer.x + dxNorm).coerceIn(-0.85f, 0.85f)
        var rawY = (currentLayer.y + dyNorm).coerceIn(-0.85f, 0.85f)

        // PixelLab Smart Magnetic Snap Guides (threshold: 0.015f)
        val snapThreshold = 0.015f
        val snappedX = if (kotlin.math.abs(rawX) < snapThreshold) {
            0f
        } else rawX
        val snappedY = if (kotlin.math.abs(rawY) < snapThreshold) {
            0f
        } else rawY

        _snapGuides.value = Pair(snappedX == 0f, snappedY == 0f)

        val newScaleX = (currentLayer.scaleX * zoomChange).coerceIn(0.15f, 4.5f)
        val newScaleY = (currentLayer.scaleY * zoomChange).coerceIn(0.15f, 4.5f)
        val newRotZ = ((currentLayer.rotationZ + rotationChangeDeg + 180f) % 360f) - 180f

        updateSelectedLayer(recordUndo = false) { layer ->
            when (layer) {
                is TextLayer -> layer.copy(x = snappedX, y = snappedY, scaleX = newScaleX, scaleY = newScaleY, rotationZ = newRotZ)
                is ShapeLayer -> layer.copy(x = snappedX, y = snappedY, scaleX = newScaleX, scaleY = newScaleY, rotationZ = newRotZ)
                is ImageLayer -> layer.copy(x = snappedX, y = snappedY, scaleX = newScaleX, scaleY = newScaleY, rotationZ = newRotZ)
                is DrawingLayer -> layer.copy(x = snappedX, y = snappedY, scaleX = newScaleX, scaleY = newScaleY, rotationZ = newRotZ)
            }
        }
    }

    fun finalizeGestureTransform() {
        _snapGuides.value = Pair(false, false)
        pushUndoSnapshot()
    }

    fun nudgeSelectedLayer(dxNorm: Float, dyNorm: Float) {
        val targetId = _selectedLayerId.value ?: return
        val currentLayer = _project.value.layers.find { it.id == targetId } ?: return
        if (currentLayer.isLocked) return
        pushUndoSnapshot()
        val newX = (currentLayer.x + dxNorm).coerceIn(-0.85f, 0.85f)
        val newY = (currentLayer.y + dyNorm).coerceIn(-0.85f, 0.85f)
        updateSelectedLayer(recordUndo = false) { layer ->
            when (layer) {
                is TextLayer -> layer.copy(x = newX, y = newY)
                is ShapeLayer -> layer.copy(x = newX, y = newY)
                is ImageLayer -> layer.copy(x = newX, y = newY)
                is DrawingLayer -> layer.copy(x = newX, y = newY)
            }
        }
    }

    fun centerSelectedLayer(horizontal: Boolean, vertical: Boolean) {
        updateSelectedLayer(recordUndo = true) { layer ->
            val nx = if (horizontal) 0f else layer.x
            val ny = if (vertical) 0f else layer.y
            when (layer) {
                is TextLayer -> layer.copy(x = nx, y = ny)
                is ShapeLayer -> layer.copy(x = nx, y = ny)
                is ImageLayer -> layer.copy(x = nx, y = ny)
                is DrawingLayer -> layer.copy(x = nx, y = ny)
            }
        }
    }

    fun updateSelectedLayerCommon(
        opacity: Float? = null,
        blendMode: LayerBlendMode? = null,
        rotationZ: Float? = null,
        rotationX: Float? = null,
        rotationY: Float? = null,
        scale: Float? = null,
        stroke: LayerStroke? = null,
        shadow: LayerShadow? = null,
        innerShadow: LayerInnerShadow? = null,
        emboss: LayerEmboss? = null
    ) {
        updateSelectedLayer(recordUndo = true) { layer ->
            val op = opacity ?: layer.opacity
            val bm = blendMode ?: layer.blendMode
            val rz = rotationZ ?: layer.rotationZ
            val rx = rotationX ?: layer.rotationX
            val ry = rotationY ?: layer.rotationY
            val sx = scale ?: layer.scaleX
            val sy = scale ?: layer.scaleY
            val st = stroke ?: layer.stroke
            val sh = shadow ?: layer.shadow
            val ish = innerShadow ?: layer.innerShadow
            val em = emboss ?: layer.emboss

            when (layer) {
                is TextLayer -> layer.copy(opacity = op, blendMode = bm, rotationZ = rz, rotationX = rx, rotationY = ry, scaleX = sx, scaleY = sy, stroke = st, shadow = sh, innerShadow = ish, emboss = em)
                is ShapeLayer -> layer.copy(opacity = op, blendMode = bm, rotationZ = rz, rotationX = rx, rotationY = ry, scaleX = sx, scaleY = sy, stroke = st, shadow = sh, innerShadow = ish, emboss = em)
                is ImageLayer -> layer.copy(opacity = op, blendMode = bm, rotationZ = rz, rotationX = rx, rotationY = ry, scaleX = sx, scaleY = sy, stroke = st, shadow = sh, innerShadow = ish, emboss = em)
                is DrawingLayer -> layer.copy(opacity = op, blendMode = bm, rotationZ = rz, rotationX = rx, rotationY = ry, scaleX = sx, scaleY = sy, stroke = st, shadow = sh, innerShadow = ish, emboss = em)
            }
        }
    }

    fun updateSelectedTextLayer(transform: (TextLayer) -> TextLayer) {
        updateSelectedLayer(recordUndo = true) { layer ->
            if (layer is TextLayer) transform(layer) else layer
        }
    }

    fun updateSelectedShapeLayer(transform: (ShapeLayer) -> ShapeLayer) {
        updateSelectedLayer(recordUndo = true) { layer ->
            if (layer is ShapeLayer) transform(layer) else layer
        }
    }

    fun updateSelectedImageLayer(transform: (ImageLayer) -> ImageLayer) {
        updateSelectedLayer(recordUndo = true) { layer ->
            if (layer is ImageLayer) transform(layer) else layer
        }
    }

    fun updateSelectedDrawingLayer(transform: (DrawingLayer) -> DrawingLayer) {
        updateSelectedLayer(recordUndo = true) { layer ->
            if (layer is DrawingLayer) transform(layer) else layer
        }
    }

    // --- Priority 4: Rectangular Image Crop & Flip (Horizontal / Vertical) ---

    fun cropSelectedImageLayer(
        leftNorm: Float,
        topNorm: Float,
        rightNorm: Float,
        bottomNorm: Float
    ): Boolean {
        val selected = _project.value.layers.find { it.id == _selectedLayerId.value } as? ImageLayer ?: return false
        val srcBmp = selected.bitmap ?: selected.originalBitmap ?: return false
        if (srcBmp.isRecycled || srcBmp.width <= 1 || srcBmp.height <= 1) return false

        val cLeft = leftNorm.coerceIn(0f, 0.92f)
        val cTop = topNorm.coerceIn(0f, 0.92f)
        val cRight = rightNorm.coerceIn(cLeft + 0.05f, 1f)
        val cBottom = bottomNorm.coerceIn(cTop + 0.05f, 1f)

        val cropX = (cLeft * srcBmp.width).toInt().coerceIn(0, srcBmp.width - 1)
        val cropY = (cTop * srcBmp.height).toInt().coerceIn(0, srcBmp.height - 1)
        val cropW = ((cRight - cLeft) * srcBmp.width).toInt().coerceIn(1, srcBmp.width - cropX)
        val cropH = ((cBottom - cTop) * srcBmp.height).toInt().coerceIn(1, srcBmp.height - cropY)

        return try {
            val croppedBmp = Bitmap.createBitmap(srcBmp, cropX, cropY, cropW, cropH)
            updateSelectedImageLayer { img ->
                img.copy(
                    bitmap = croppedBmp,
                    originalBitmap = img.originalBitmap ?: srcBmp,
                    cropLeft = cLeft,
                    cropTop = cTop,
                    cropRight = cRight,
                    cropBottom = cBottom
                )
            }
            _statusBanner.value = "✂️ برش تصویر (${croppedBmp.width}×${croppedBmp.height} پیکسل) با حفظ کیفیت اصلی اعمال شد."
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to crop image bitmap", e)
            _statusBanner.value = "خطا در برش تصویر."
            false
        }
    }

    fun resetSelectedImageCrop() {
        val selected = _project.value.layers.find { it.id == _selectedLayerId.value } as? ImageLayer ?: return
        val orig = selected.originalBitmap ?: return
        updateSelectedImageLayer { img ->
            img.copy(
                bitmap = orig,
                cropLeft = 0f,
                cropTop = 0f,
                cropRight = 1f,
                cropBottom = 1f
            )
        }
        _statusBanner.value = "تصویر به ابعاد و کیفیت اولیه پیش از برش بازگردانی شد."
    }

    private fun updateSelectedLayer(recordUndo: Boolean, transform: (StudioLayer) -> StudioLayer) {
        val targetId = _selectedLayerId.value ?: return
        if (recordUndo) pushUndoSnapshot()
        _project.value = _project.value.copy(
            layers = _project.value.layers.map { layer ->
                if (layer.id == targetId) transform(layer) else layer
            }
        )
    }

    // --- Canvas Background, Aspect Ratio & Post-FX ---

    fun applyTextPreset(preset: TextStylePreset) {
        pushUndoSnapshot()
        val selected = _project.value.layers.find { it.id == _selectedLayerId.value }
        if (selected is TextLayer) {
            updateSelectedTextLayer {
                it.copy(
                    fontId = preset.font.id,
                    colorHex = preset.primaryHex,
                    useGradientFill = true,
                    gradientSecondColorHex = preset.secondaryHex,
                    stroke = LayerStroke(enabled = true, colorHex = preset.strokeHex, width = preset.strokeWidth),
                    is3dEnabled = preset.is3d,
                    depth3d = preset.depth3d,
                    curveAngle = preset.curveAngle,
                    emboss = it.emboss.copy(enabled = preset.emboss),
                    reflectionEnabled = preset.reflection
                )
            }
        } else {
            val newLayer = TextLayer(
                name = preset.titleFa,
                text = preset.sampleText,
                fontId = preset.font.id,
                colorHex = preset.primaryHex,
                useGradientFill = true,
                gradientSecondColorHex = preset.secondaryHex,
                stroke = LayerStroke(enabled = true, colorHex = preset.strokeHex, width = preset.strokeWidth),
                is3dEnabled = preset.is3d,
                depth3d = preset.depth3d,
                curveAngle = preset.curveAngle,
                emboss = LayerEmboss(enabled = preset.emboss),
                reflectionEnabled = preset.reflection
            )
            _project.value = _project.value.copy(layers = _project.value.layers + newLayer)
            _selectedLayerId.value = newLayer.id
        }
    }

    fun updateProjectTitle(newTitle: String) {
        _project.value = _project.value.copy(title = newTitle)
    }

    fun setCanvasAspectPreset(preset: CanvasAspectPreset) {
        pushUndoSnapshot()
        _project.value = _project.value.copy(
            canvasWidth = preset.width,
            canvasHeight = preset.height
        )
        _statusBanner.value = "ابعاد بوم به ${preset.nameFa} (${preset.width}×${preset.height}) تغییر یافت."
    }

    /**
     * Priority 3: Custom Canvas Dimensions in pixels with numeric validation (128..4096 px)
     * and full preservation of normalized layer positions and rendering quality.
     */
    fun setCustomCanvasDimensions(widthPx: Int, heightPx: Int): Boolean {
        if (widthPx !in 128..4096 || heightPx !in 128..4096) {
            _statusBanner.value = "ابعاد واردشده نامعتبر است؛ عرض و ارتفاع باید بین ۱۲۸ تا ۴۰۹۶ پیکسل باشند."
            return false
        }
        pushUndoSnapshot()
        _project.value = _project.value.copy(
            canvasWidth = widthPx,
            canvasHeight = heightPx
        )
        _statusBanner.value = "ابعاد سفارشی بوم با موفقیت روی $widthPx×$heightPx پیکسل تنظیم شد."
        return true
    }

    /**
     * Priority 4: Interactive Rectangular Image Crop with aspect ratio support and quality preservation.
     */
    fun applyInteractiveImageCrop(
        cropLeft: Float,
        cropTop: Float,
        cropRight: Float,
        cropBottom: Float
    ) {
        val selected = _project.value.layers.find { it.id == _selectedLayerId.value } as? ImageLayer ?: return
        val sourceBmp = selected.originalBitmap ?: selected.bitmap ?: return
        if (sourceBmp.isRecycled) return

        val l = cropLeft.coerceIn(0f, 0.92f)
        val t = cropTop.coerceIn(0f, 0.92f)
        val r = cropRight.coerceIn(l + 0.08f, 1f)
        val b = cropBottom.coerceIn(t + 0.08f, 1f)

        val srcW = sourceBmp.width.coerceAtLeast(1)
        val srcH = sourceBmp.height.coerceAtLeast(1)
        val pxLeft = (l * srcW).toInt().coerceIn(0, srcW - 1)
        val pxTop = (t * srcH).toInt().coerceIn(0, srcH - 1)
        val pxWidth = ((r - l) * srcW).toInt().coerceIn(1, srcW - pxLeft)
        val pxHeight = ((b - t) * srcH).toInt().coerceIn(1, srcH - pxTop)

        val croppedBitmap = Bitmap.createBitmap(sourceBmp, pxLeft, pxTop, pxWidth, pxHeight)
        updateSelectedImageLayer { img ->
            img.copy(
                bitmap = croppedBitmap,
                originalBitmap = sourceBmp,
                cropLeft = l,
                cropTop = t,
                cropRight = r,
                cropBottom = b
            )
        }
        _statusBanner.value = "برش تصویر (${pxWidth}×${pxHeight} پیکسل) با حفظ کیفیت اصلی اعمال شد."
    }

    fun resetImageCropToOriginal() {
        val selected = _project.value.layers.find { it.id == _selectedLayerId.value } as? ImageLayer ?: return
        val orig = selected.originalBitmap ?: selected.bitmap ?: return
        updateSelectedImageLayer { img ->
            img.copy(
                bitmap = orig,
                originalBitmap = orig,
                cropLeft = 0f,
                cropTop = 0f,
                cropRight = 1f,
                cropBottom = 1f
            )
        }
        _statusBanner.value = "تصویر به ابعاد و کیفیت کامل اولیه بازگردانده شد."
    }

    fun toggleSelectedImageFlip(horizontal: Boolean = false, vertical: Boolean = false) {
        updateSelectedImageLayer { img ->
            img.copy(
                flipHorizontal = if (horizontal) !img.flipHorizontal else img.flipHorizontal,
                flipVertical = if (vertical) !img.flipVertical else img.flipVertical
            )
        }
    }

    /**
     * Priority 5: 4-way Text Alignment (Right, Center, Left, Justify).
     */
    fun setSelectedTextAlignMode(mode: TextAlignMode) {
        updateSelectedTextLayer { txt ->
            txt.copy(
                textAlignMode = mode,
                textAlignCenter = (mode == TextAlignMode.CENTER)
            )
        }
    }

    fun applyReelsCoverTemplate(template: ReelsCoverTemplate, context: Context) {
        pushUndoSnapshot()
        _isReelsSafeZoneVisible.value = true

        val targetWidth = 1080
        val targetHeight = 1920

        val newBackground = CanvasBackground(
            type = BackgroundType.GRADIENT,
            primaryColorHex = template.bgPrimaryHex,
            secondaryColorHex = template.bgSecondaryHex,
            gradientAngle = 90f
        )

        val badgeShapeLayer = ShapeLayer(
            name = "نشان: ${template.badgeLabel}",
            shapeKind = template.badgeShape,
            x = 0f,
            y = -0.20f,
            sizeRatio = 0.22f,
            fillColorHex = template.badgeColorHex,
            opacity = 0.95f,
            stroke = LayerStroke(enabled = true, colorHex = "#FFFFFF", width = 3f),
            shadow = LayerShadow(enabled = true, colorHex = "#88000000", blurRadius = 14f, offsetX = 0f, offsetY = 6f)
        )

        val badgeTextLayer = TextLayer(
            name = "متن نشان",
            text = template.badgeLabel,
            fontId = StudioFont.VAZIRMATN.id,
            fontSizeSp = 22f,
            x = 0f,
            y = -0.20f,
            colorHex = "#FFFFFF",
            isBold = true,
            stroke = LayerStroke(enabled = false)
        )

        val headlineLayer = TextLayer(
            name = "تیتر اصلی ریلز",
            text = template.headline,
            fontId = template.font.id,
            fontSizeSp = 50f,
            x = 0f,
            y = -0.04f,
            colorHex = template.headlinePrimaryHex,
            useGradientFill = true,
            gradientSecondColorHex = template.headlineSecondaryHex,
            stroke = LayerStroke(enabled = true, colorHex = template.headlineStrokeHex, width = 7f),
            shadow = LayerShadow(enabled = true, colorHex = "#EE000000", blurRadius = 24f, offsetX = 0f, offsetY = 12f),
            emboss = LayerEmboss(enabled = true, lightAngle = 45f, intensity = 75f),
            is3dEnabled = true,
            depth3d = 20,
            obliqueAngle3d = 135f
        )

        val subtitleLayer = TextLayer(
            name = "زیرعنوان و اقدام",
            text = template.subtitle,
            fontId = StudioFont.VAZIRMATN.id,
            fontSizeSp = 22f,
            x = 0f,
            y = 0.14f,
            colorHex = "#F8FAFC",
            backgroundPillEnabled = true,
            backgroundPillColorHex = "#CC0F172A",
            backgroundPillPadding = 16f,
            backgroundPillRadius = 18f,
            stroke = LayerStroke(enabled = false)
        )

        val stickerItem = BUILTIN_PIXELLAB_STICKERS.find { it.id == template.stickerId }
        val generatedLayers = mutableListOf<StudioLayer>(badgeShapeLayer, badgeTextLayer, headlineLayer, subtitleLayer)

        if (stickerItem != null) {
            val stickerBmp = StudioCanvasRenderer.renderBuiltinStickerToBitmap(
                symbolText = stickerItem.symbolText,
                primaryHex = stickerItem.primaryHex,
                secondaryHex = stickerItem.secondaryHex
            )
            val stickerLayer = ImageLayer(
                name = "استیکر: ${stickerItem.labelFa}",
                bitmap = stickerBmp,
                x = 0.32f,
                y = -0.15f,
                scaleX = 0.55f,
                scaleY = 0.55f,
                rotationZ = -12f
            )
            generatedLayers.add(0, stickerLayer)
        }

        _project.value = _project.value.copy(
            title = "کاور ریلز: ${template.titleFa}",
            canvasWidth = targetWidth,
            canvasHeight = targetHeight,
            background = newBackground,
            postFx = CanvasPostFx(vignetteEnabled = true, vignetteIntensity = 0.40f),
            layers = generatedLayers
        )
        _selectedLayerId.value = headlineLayer.id
        _statusBanner.value = "🚀 قالب کاور ریلز «${template.titleFa}» در ابعاد ۹:۱۶ با چیدمان ناحیه امن ۱:۱ اعمال شد."
    }

    fun generateInstagramReelsCover(topic: String, category: String, context: Context) {
        if (topic.isBlank()) {
            _statusBanner.value = "لطفاً موضوع یا عنوان ریلز را وارد کنید."
            return
        }
        viewModelScope.launch {
            _aiState.value = AiTaskUiState.Loading("در حال تولید خودکار کاور ریلز اینستاگرام برای: $topic")
            try {
                val matched = when {
                    category.contains("تکنولوژی") || topic.contains("هوش مصنوعی") || topic.contains("گوشی") || topic.contains("کد") -> "tech"
                    category.contains("بلاگری") || topic.contains("ولاگ") || topic.contains("روزمره") || topic.contains("سفر") -> "vlog"
                    category.contains("کسب") || topic.contains("درآمد") || topic.contains("دلار") || topic.contains("بیزینس") -> "business"
                    category.contains("فروش") || topic.contains("تخفیف") || topic.contains("خرید") || topic.contains("حراج") -> "sale"
                    else -> "cinema"
                }

                val template = when (matched) {
                    "tech" -> BUILTIN_REELS_TEMPLATES[0].copy(
                        headline = topic.take(32),
                        subtitle = "ترفندهای کاربردی و اختصاصی این ریلز 🔥"
                    )
                    "vlog" -> BUILTIN_REELS_TEMPLATES[1].copy(
                        headline = topic.take(32),
                        subtitle = "تا انتها تماشا کنید و کامنت بگذارید 👇"
                    )
                    "business" -> BUILTIN_REELS_TEMPLATES[2].copy(
                        headline = topic.take(32),
                        subtitle = "برای موفقیت در کسب‌وکار حتماً ذخیره کنید 💡"
                    )
                    "sale" -> BUILTIN_REELS_TEMPLATES[3].copy(
                        headline = topic.take(32),
                        subtitle = "پیشنهاد فوق‌العاده با مهلت محدود 🛒"
                    )
                    else -> BUILTIN_REELS_TEMPLATES[4].copy(
                        headline = topic.take(32),
                        subtitle = "حقایق ناگفته و پشت‌پرده‌ای که باید بدانید ✨"
                    )
                }

                applyReelsCoverTemplate(template, context)
                _aiState.value = AiTaskUiState.Success("طراحی کاور ریلز «$topic» با موفقیت تکمیل شد.")
                _statusBanner.value = "✨ کاور ریلز برای «$topic» با استانداردهای رسمی ریلز و استوری اینستاگرام آماده شد!"
            } catch (e: Exception) {
                Log.e(TAG, "Error generating reels cover", e)
                _aiState.value = AiTaskUiState.Error("خطا در ایجاد کاور ریلز: ${e.localizedMessage}")
                _statusBanner.value = "خطا در پردازش کاور ریلز."
            }
        }
    }

    fun updateCanvasBackground(transform: (CanvasBackground) -> CanvasBackground) {
        pushUndoSnapshot()
        _project.value = _project.value.copy(background = transform(_project.value.background))
    }

    fun updateCanvasPostFx(transform: (CanvasPostFx) -> CanvasPostFx) {
        pushUndoSnapshot()
        _project.value = _project.value.copy(postFx = transform(_project.value.postFx))
    }

    // --- PLP Archive, Cloud Persistence & Agent Diagnostics Operations ---

    val logEntries: StateFlow<List<AgentLogEntry>> = AgentDiagnosticsLogger.logEntries
    val diagnosticsReport: StateFlow<FullDiagnosticsReport> = AgentDiagnosticsLogger.diagnosticsReport

    fun refreshLocalPlpFiles(context: Context) {
        viewModelScope.launch {
            AgentDiagnosticsLogger.initialize(context)
            _localPlpFiles.value = PlpProjectManager.listLocalPlpFiles(context)
            refreshLearnedPlpStyles(context)
        }
    }

    fun refreshLearnedPlpStyles(context: Context) {
        val styles = PlpStyleLearningEngine.loadLearnedStyles(context)
        _learnedPlpStyles.value = styles
        val activeId = PlpStyleLearningEngine.getActiveStyleId(context)
        _activeLearnedStyle.value = styles.find { it.id == activeId } ?: styles.firstOrNull()
    }

    fun selectActiveLearnedStyle(context: Context, style: PlpLearnedStyleDNA) {
        PlpStyleLearningEngine.setActiveStyleId(context, style.id)
        _activeLearnedStyle.value = style
        _statusBanner.value = "🧠 استایل «${style.styleName}» در حافظه ایجنت فعال شد."
    }

    fun deleteLearnedStyle(context: Context, styleId: String) {
        val remaining = PlpStyleLearningEngine.deleteLearnedStyle(context, styleId)
        _learnedPlpStyles.value = remaining
        val activeId = PlpStyleLearningEngine.getActiveStyleId(context)
        _activeLearnedStyle.value = remaining.find { it.id == activeId } ?: remaining.firstOrNull()
        _statusBanner.value = "استایل از حافظه ایجنت حذف شد."
    }

    fun learnStyleFromCurrentCanvas(context: Context, customStyleName: String? = null) {
        val current = _project.value
        val extracted = PlpStyleLearningEngine.extractStyleDnaFromProject(current, customStyleName)
        val updatedList = PlpStyleLearningEngine.saveOrUpdateLearnedStyle(context, extracted)
        _learnedPlpStyles.value = updatedList
        _activeLearnedStyle.value = extracted
        _statusBanner.value = "🧠 کل DNA استایل «${extracted.styleName}» (${extracted.toSummaryFa()}) یاد گرفته و در حافظه ایجنت ذخیره شد!"
    }

    fun learnStyleFromPlpFile(file: File, context: Context) {
        viewModelScope.launch {
            val result = PlpProjectManager.loadPlpFromFile(file, context)
            result.onSuccess { loaded ->
                val extracted = PlpStyleLearningEngine.extractStyleDnaFromProject(
                    project = loaded,
                    customStyleName = file.nameWithoutExtension.replace("_", " ")
                )
                val updatedList = PlpStyleLearningEngine.saveOrUpdateLearnedStyle(context, extracted)
                _learnedPlpStyles.value = updatedList
                _activeLearnedStyle.value = extracted
                _statusBanner.value = "🧠 استایل فایل PLP «${extracted.styleName}» (${extracted.toSummaryFa()}) یاد گرفته شد و آماده ساخت طرح جدید است!"
            }.onFailure { err ->
                _statusBanner.value = "خطا در یادگیری استایل از فایل PLP: ${err.localizedMessage}"
            }
        }
    }

    fun learnStyleFromPlpUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = PlpProjectManager.importPlpFromUri(context, uri)
            result.onSuccess { imported ->
                val extracted = PlpStyleLearningEngine.extractStyleDnaFromProject(imported)
                val updatedList = PlpStyleLearningEngine.saveOrUpdateLearnedStyle(context, extracted)
                _learnedPlpStyles.value = updatedList
                _activeLearnedStyle.value = extracted
                _statusBanner.value = "🧠 استایل پروژه PLP «${extracted.styleName}» یاد گرفته و در حافظه ایجنت ثبت شد!"
            }.onFailure { err ->
                _statusBanner.value = "خطا در خواندن فایل PLP برای یادگیری استایل: ${err.localizedMessage}"
            }
        }
    }

    fun generateProjectFromLearnedStyle(
        context: Context,
        style: PlpLearnedStyleDNA,
        headlineText: String,
        subtitleText: String,
        badgeText: String = ""
    ) {
        pushUndoSnapshot()
        PlpStyleLearningEngine.setActiveStyleId(context, style.id)
        _activeLearnedStyle.value = style
        val clonedProject = PlpStyleLearningEngine.generateNewProjectFromStyle(
            style = style,
            newHeadline = headlineText,
            newSubtitle = subtitleText,
            newBadgeText = badgeText
        )
        _project.value = clonedProject
        _selectedLayerId.value = clonedProject.layers.filterIsInstance<TextLayer>().firstOrNull()?.id
            ?: clonedProject.layers.lastOrNull()?.id
        _statusBanner.value = "✨ طرح جدید با تیتر «${headlineText.take(20)}» دقیقاً به استایل «${style.styleName}» ساخته شد!"
    }

    fun applyLearnedStyleToCurrentCanvas(
        context: Context,
        style: PlpLearnedStyleDNA,
        applyLayoutPositions: Boolean = false
    ) {
        pushUndoSnapshot()
        PlpStyleLearningEngine.setActiveStyleId(context, style.id)
        _activeLearnedStyle.value = style
        val styledProject = PlpStyleLearningEngine.applyStyleDnaToCurrentCanvas(
            style = style,
            currentProject = _project.value,
            applyLayoutPositions = applyLayoutPositions,
            applyBackgroundAndFx = true
        )
        _project.value = styledProject
        _statusBanner.value = "🎨 استایل «${style.styleName}» بدون تغییر متن‌ها روی لایه‌های فعلی بوم اعمال شد!"
    }

    fun runConnectionDiagnostics(context: Context) {
        viewModelScope.launch {
            AgentDiagnosticsLogger.runConnectionDiagnostics(context)
        }
    }

    fun clearAgentLogs(context: Context) {
        AgentDiagnosticsLogger.clearLogs(context)
    }

    fun exportAgentLogs(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = AgentDiagnosticsLogger.exportLogToFileUri(context, uri)
            onResult(ok)
        }
    }

    fun getAgentLogShareIntent(context: Context): Intent {
        return AgentDiagnosticsLogger.createShareIntent(context)
    }

    fun saveProjectToCloudAndLocalPlp(context: Context, onRequireSignInForCloud: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val localFile = PlpProjectManager.savePlpToInternalStorage(context, _project.value)
                val updatedProject = _project.value.copy(plpFileName = localFile.name)
                _project.value = updatedProject
                refreshLocalPlpFiles(context)

                val repo = repository
                if (_currentUserId.value.isNullOrBlank() || repo == null) {
                    _statusBanner.value = "بسته PLP در حافظه داخلی (${localFile.name}) ذخیره شد. برای پشتیبان‌گیری ابری وارد حساب گوگل شوید."
                    onRequireSignInForCloud()
                    return@launch
                }

                val cloudRes = repo.saveProject(updatedProject)
                if (cloudRes.isSuccess) {
                    _statusBanner.value = "پروژه در فضای ابری و بسته PLP داخلی (${localFile.name}) ذخیره شد."
                } else {
                    _statusBanner.value = "بسته PLP ذخیره شد. خطا در همگام‌سازی ابری: ${cloudRes.exceptionOrNull()?.localizedMessage}"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "خطا در ذخیره پروژه: ${e.localizedMessage}"
            }
        }
    }

    fun exportPlpFileToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = PlpProjectManager.exportPlpToUri(context, _project.value, uri)
            if (result.isSuccess) {
                _statusBanner.value = "فایل پروژه (.plp) با موفقیت خروجی گرفته شد."
            } else {
                _statusBanner.value = "خطا در ذخیره فایل PLP: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun importPlpFileFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = PlpProjectManager.importPlpFromUri(context, uri)
            result.onSuccess { imported ->
                pushUndoSnapshot()
                _project.value = imported
                _selectedLayerId.value = imported.layers.lastOrNull()?.id
                // Automatically extract and learn the imported PLP project's style DNA!
                val learnedDna = PlpStyleLearningEngine.extractStyleDnaFromProject(imported)
                val stylesList = PlpStyleLearningEngine.saveOrUpdateLearnedStyle(context, learnedDna)
                _learnedPlpStyles.value = stylesList
                _activeLearnedStyle.value = learnedDna
                refreshCustomFonts(context)
                _statusBanner.value = "پروژه PLP «${imported.title}» باز شد و استایل آن در حافظه هوشمند ایجنت ذخیره گردید."
            }.onFailure { err ->
                _statusBanner.value = "خطا در باز کردن فایل PLP: ${err.localizedMessage}"
            }
        }
    }

    fun openLocalPlpFile(file: File, context: Context? = null) {
        viewModelScope.launch {
            val result = PlpProjectManager.loadPlpFromFile(file, context)
            result.onSuccess { loaded ->
                pushUndoSnapshot()
                _project.value = loaded
                _selectedLayerId.value = loaded.layers.lastOrNull()?.id
                if (context != null) {
                    val learnedDna = PlpStyleLearningEngine.extractStyleDnaFromProject(
                        project = loaded,
                        customStyleName = file.nameWithoutExtension.replace("_", " ")
                    )
                    val stylesList = PlpStyleLearningEngine.saveOrUpdateLearnedStyle(context, learnedDna)
                    _learnedPlpStyles.value = stylesList
                    _activeLearnedStyle.value = learnedDna
                    refreshCustomFonts(context)
                }
                _statusBanner.value = "پروژه PLP «${loaded.title}» با ${loaded.layers.size} لایه باز شد و استایل آن یاد گرفته شد."
            }.onFailure { err ->
                _statusBanner.value = "خطا در خواندن فایل PLP: ${err.localizedMessage}"
            }
        }
    }

    fun loadCloudProjectDoc(context: Context, doc: StudioProjectDoc) {
        viewModelScope.launch {
            val localFiles = PlpProjectManager.listLocalPlpFiles(context)
            val matchingPlp = localFiles.find { it.name == doc.plpFileName && doc.plpFileName.isNotBlank() }
            if (matchingPlp != null) {
                openLocalPlpFile(matchingPlp, context)
                return@launch
            }

            pushUndoSnapshot()
            val bgType = runCatching { BackgroundType.valueOf(doc.backgroundType) }.getOrDefault(BackgroundType.GRADIENT)
            val reconstructedLayer = TextLayer(
                name = doc.layerTitles.firstOrNull() ?: "متن اصلی",
                text = doc.primaryText.ifBlank { doc.title },
                fontId = doc.fontId,
                colorHex = doc.accentColorHex,
                is3dEnabled = doc.is3dEnabled,
                depth3d = doc.depth3d.toInt(),
                curveAngle = doc.curveAngle.toFloat()
            )
            val reconstructed = StudioProject(
                id = doc.id.ifBlank { UUID.randomUUID().toString() },
                title = doc.title,
                canvasWidth = doc.canvasWidth.toInt().coerceIn(256, 4096),
                canvasHeight = doc.canvasHeight.toInt().coerceIn(256, 4096),
                background = CanvasBackground(
                    type = bgType,
                    primaryColorHex = doc.backgroundColorHex,
                    secondaryColorHex = doc.backgroundSecondaryHex
                ),
                layers = listOf(reconstructedLayer),
                plpFileName = doc.plpFileName
            )
            _project.value = reconstructed
            _selectedLayerId.value = reconstructedLayer.id
            _statusBanner.value = "پروژه ابری «${doc.title}» روی بوم بازسازی شد."
        }
    }

    fun deleteCloudProject(projectId: String) {
        val repo = repository ?: return
        if (_currentUserId.value.isNullOrBlank()) return
        viewModelScope.launch {
            val res = repo.deleteProject(projectId)
            if (res.isSuccess) {
                _statusBanner.value = "پروژه از فضای ابری حذف شد."
            } else {
                _statusBanner.value = "خطا در حذف پروژه: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun exportHighResImageToGallery(
        context: Context,
        format: ExportFormat,
        qualityPreset: ExportQualityPreset
    ) {
        viewModelScope.launch {
            _statusBanner.value = "در حال رندر تصویر با کیفیت ${qualityPreset.labelFa}..."
            val bitmap = withContext(Dispatchers.Default) {
                StudioCanvasRenderer.renderProjectToBitmap(
                    context = context,
                    project = _project.value,
                    scaleMultiplier = qualityPreset.scaleMultiplier,
                    includeCheckerboardForTransparent = false
                )
            }
            val saveRes = PlpProjectManager.saveBitmapToGallery(
                context = context,
                bitmap = bitmap,
                projectTitle = _project.value.title,
                format = format,
                quality = qualityPreset.quality
            )
            saveRes.onSuccess {
                _statusBanner.value = "تصویر با کیفیت ${bitmap.width}×${bitmap.height} (${format.name}) در گالری ذخیره شد!"
            }.onFailure { err ->
                _statusBanner.value = "خطا در ذخیره تصویر: ${err.localizedMessage}"
            }
        }
    }

    /**
     * Priority 5: Direct Image Sharing (PNG or JPG) via the standard Android share sheet.
     */
    fun shareCanvasImageDirectly(
        context: Context,
        format: ExportFormat = ExportFormat.PNG,
        qualityPreset: ExportQualityPreset = ExportQualityPreset.HIGH
    ) {
        viewModelScope.launch {
            _statusBanner.value = "در حال آماده‌سازی تصویر ${format.extension.uppercase()} برای اشتراک‌گذاری مستقیم..."
            val bitmap = withContext(Dispatchers.Default) {
                StudioCanvasRenderer.renderProjectToBitmap(
                    context = context,
                    project = _project.value,
                    scaleMultiplier = qualityPreset.scaleMultiplier,
                    includeCheckerboardForTransparent = false
                )
            }
            val shareRes = PlpProjectManager.createDirectShareImageIntent(
                context = context,
                bitmap = bitmap,
                projectTitle = _project.value.title,
                format = format,
                quality = qualityPreset.quality
            )
            shareRes.onSuccess { chooserIntent ->
                try {
                    context.startActivity(chooserIntent)
                    _statusBanner.value = "منوی اشتراک‌گذاری استاندارد اندروید (${format.extension.uppercase()}) باز شد."
                } catch (e: Exception) {
                    _statusBanner.value = "خطا در باز کردن منوی اشتراک‌گذاری: ${e.localizedMessage}"
                }
            }.onFailure { err ->
                _statusBanner.value = "خطا در آماده‌سازی اشتراک‌گذاری: ${err.localizedMessage}"
            }
        }
    }

    // =========================================================================
    // PIXELLAB AI AGENT ENGINE (MULTI-TURN + FULL CANVAS TOOL EXECUTION)
    // =========================================================================

    fun sendCommandToPixelLabAgent(
        context: Context,
        userCommand: String,
        useCanvasVision: Boolean = true
    ) {
        val cleanCmd = userCommand.trim()
        if (cleanCmd.isBlank()) return

        syncSelectedGeminiModel(context)
        _lastGeminiError.value = null
        _activeTransactionSteps.value = emptyList()

        val userMsg = AgentChatMessage(
            isUser = true,
            text = cleanCmd
        )
        _agentMessages.value = _agentMessages.value + userMsg

        viewModelScope.launch {
            val preTransactionProject = _project.value
            val preTransactionSelectedId = _selectedLayerId.value
            _aiState.value = AiTaskUiState.Loading("ایجنت هوشمند (${_selectedGeminiModel.value}) در حال بررسی پروژه و برنامه‌ریزی عملیات...")
            try {
                // 1. Capture lightweight 512px canvas thumbnail for visual comprehension & before-preview
                val visionBitmap = withContext(Dispatchers.Default) {
                    try {
                        val maxDim = maxOf(preTransactionProject.canvasWidth, preTransactionProject.canvasHeight).coerceAtLeast(1)
                        val scale = (512f / maxDim).coerceIn(0.12f, 1.0f)
                        StudioCanvasRenderer.renderProjectToBitmap(
                            context = context,
                            project = preTransactionProject,
                            scaleMultiplier = scale,
                            includeCheckerboardForTransparent = false
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to render vision bitmap for agent", e)
                        null
                    }
                }

                // 2. Execute multi-turn Function Calling transaction with the 18 real editor tools
                val txOutcome = PixelLabAgentEngine.executeAgentTransactionWithFunctionCalling(
                    context = context,
                    userCommand = cleanCmd,
                    project = preTransactionProject,
                    selectedLayerId = preTransactionSelectedId,
                    recentHistory = _agentMessages.value.dropLast(1),
                    canvasVisionBitmap = if (useCanvasVision) visionBitmap else null,
                    activeLearnedStyle = _activeLearnedStyle.value,
                    onStepProgress = { stepStatusText ->
                        _aiState.value = AiTaskUiState.Loading(stepStatusText)
                    },
                    onLayerHighlighted = { layerId, _ ->
                        _activeAiMutatedLayerId.value = layerId
                    }
                )

                if (!txOutcome.geminiErrorFa.isNullOrBlank()) {
                    _lastGeminiError.value = txOutcome.geminiErrorFa
                }

                val replyFa = txOutcome.replyFa
                val actionsArray = txOutcome.legacyActionsJson ?: org.json.JSONArray()

                val executedBadges = mutableListOf<String>()
                executedBadges.addAll(txOutcome.session.steps.map { it.titleFa })

                var workingProject = txOutcome.session.workingProject
                var workingSelectedId = txOutcome.session.workingSelectedLayerId
                val functionCallingMutatedProject = workingProject != preTransactionProject

                for (i in 0 until actionsArray.length()) {
                    val act = actionsArray.optJSONObject(i) ?: continue
                    val actionType = act.optString("actionType", "")

                    // If Function Calling already executed the real layer tools on workingProject,
                    // only run auxiliary high-level actions (PLP style DNA, Nano Banana AI image gen, Zero-G physics).
                    val isAuxiliaryAction = actionType in setOf(
                        "LEARN_CURRENT_PLP_STYLE",
                        "CREATE_FROM_LEARNED_PLP_STYLE",
                        "APPLY_LEARNED_PLP_STYLE",
                        "TOGGLE_ANTI_GRAVITY",
                        "AUTO_LAYOUT_PREVIEW",
                        "AUTO_LAYOUT_HARMONIZE",
                        "GENERATE_AI_BACKGROUND",
                        "GENERATE_AI_STICKER",
                        "EDIT_SELECTED_IMAGE_AI"
                    )
                    if (functionCallingMutatedProject && !isAuxiliaryAction) {
                        continue
                    }

                    when (actionType) {
                        "LEARN_CURRENT_PLP_STYLE" -> {
                            val extracted = PlpStyleLearningEngine.extractStyleDnaFromProject(workingProject)
                            val updatedList = PlpStyleLearningEngine.saveOrUpdateLearnedStyle(context, extracted)
                            _learnedPlpStyles.value = updatedList
                            _activeLearnedStyle.value = extracted
                            executedBadges.add("یادگیری DNA استایل «${extracted.styleName}»")
                        }

                        "CREATE_FROM_LEARNED_PLP_STYLE" -> {
                            val activeStyle = _activeLearnedStyle.value
                                ?: PlpStyleLearningEngine.getActiveLearnedStyle(context)
                                ?: PlpStyleLearningEngine.extractStyleDnaFromProject(workingProject)
                            val newHeadline = act.optString("text", "").takeIf { it.isNotBlank() && it != "null" }
                                ?: "طراحی با استایل ${activeStyle.styleName}"
                            val newSubtitle = act.optString("subtitleText", "").takeIf { it.isNotBlank() && it != "null" }
                                ?: ""
                            workingProject = PlpStyleLearningEngine.generateNewProjectFromStyle(
                                style = activeStyle,
                                newHeadline = newHeadline,
                                newSubtitle = newSubtitle
                            )
                            workingSelectedId = workingProject.layers.filterIsInstance<TextLayer>().firstOrNull()?.id
                                ?: workingProject.layers.lastOrNull()?.id
                            executedBadges.add("تولید طرح جدید با استایل «${activeStyle.styleName}»")
                        }

                        "APPLY_LEARNED_PLP_STYLE" -> {
                            val activeStyle = _activeLearnedStyle.value
                                ?: PlpStyleLearningEngine.getActiveLearnedStyle(context)
                                ?: PlpStyleLearningEngine.extractStyleDnaFromProject(workingProject)
                            workingProject = PlpStyleLearningEngine.applyStyleDnaToCurrentCanvas(
                                style = activeStyle,
                                currentProject = workingProject,
                                applyLayoutPositions = false,
                                applyBackgroundAndFx = true
                            )
                            executedBadges.add("انتقال استایل «${activeStyle.styleName}» روی لایه‌های فعلی")
                        }

                        "CLEAR_ALL_LAYERS" -> {
                            workingProject = workingProject.copy(
                                layers = emptyList(),
                                background = if (workingProject.background.type == BackgroundType.IMAGE || workingProject.background.backgroundBitmap != null) {
                                    workingProject.background.copy(type = BackgroundType.GRADIENT, backgroundBitmap = null)
                                } else workingProject.background
                            )
                            workingSelectedId = null
                            executedBadges.add("پاک‌سازی همه لایه‌ها")
                        }

                        "RESET_CANVAS" -> {
                            workingProject = workingProject.copy(
                                layers = emptyList(),
                                background = CanvasBackground(
                                    type = BackgroundType.GRADIENT,
                                    primaryColorHex = "#090D16",
                                    secondaryColorHex = "#1E1B4B",
                                    gradientAngle = 135f,
                                    isRadialGradient = false,
                                    backgroundBitmap = null
                                ),
                                postFx = CanvasPostFx()
                            )
                            workingSelectedId = null
                            executedBadges.add("بازنشانی کامل بوم")
                        }

                        "UNLOCK_ALL_LAYERS" -> {
                            workingProject = workingProject.copy(
                                layers = workingProject.layers.map { l ->
                                    when (l) {
                                        is TextLayer -> l.copy(isLocked = false)
                                        is ShapeLayer -> l.copy(isLocked = false)
                                        is ImageLayer -> l.copy(isLocked = false)
                                        is DrawingLayer -> l.copy(isLocked = false)
                                    }
                                }
                            )
                            executedBadges.add("باز کردن قفل همه لایه‌ها")
                        }

                        "SELECT_LAYER" -> {
                            val target = resolveAgentTargetLayer(workingProject, act, workingSelectedId)
                            if (target != null) {
                                workingSelectedId = target.id
                                executedBadges.add("انتخاب لایه «${target.name}»")
                            }
                        }

                        "REORDER_LAYER" -> {
                            val target = resolveAgentTargetLayer(workingProject, act, workingSelectedId)
                            if (target != null) {
                                val list = workingProject.layers.toMutableList()
                                val idx = list.indexOfFirst { it.id == target.id }
                                if (idx != -1) {
                                    val dir = act.optString("orderDirection", "TO_FRONT").uppercase()
                                    val item = list.removeAt(idx)
                                    when (dir) {
                                        "TO_BACK" -> list.add(0, item)
                                        "MOVE_UP" -> list.add((idx + 1).coerceAtMost(list.size), item)
                                        "MOVE_DOWN" -> list.add((idx - 1).coerceAtLeast(0), item)
                                        else -> list.add(item)
                                    }
                                    workingProject = workingProject.copy(layers = list)
                                    workingSelectedId = item.id
                                    executedBadges.add("تغییر ترتیب لایه «${target.name}»")
                                }
                            }
                        }

                        "TOGGLE_LAYER_VISIBILITY" -> {
                            val target = resolveAgentTargetLayer(workingProject, act, workingSelectedId)
                            if (target != null) {
                                workingProject = workingProject.copy(
                                    layers = workingProject.layers.map { l ->
                                        if (l.id != target.id) l else when (l) {
                                            is TextLayer -> l.copy(isVisible = !l.isVisible)
                                            is ShapeLayer -> l.copy(isVisible = !l.isVisible)
                                            is ImageLayer -> l.copy(isVisible = !l.isVisible)
                                            is DrawingLayer -> l.copy(isVisible = !l.isVisible)
                                        }
                                    }
                                )
                                executedBadges.add("تغییر وضعیت نمایش «${target.name}»")
                            }
                        }

                        "TOGGLE_LAYER_LOCK" -> {
                            val target = resolveAgentTargetLayer(workingProject, act, workingSelectedId)
                            if (target != null) {
                                workingProject = workingProject.copy(
                                    layers = workingProject.layers.map { l ->
                                        if (l.id != target.id) l else when (l) {
                                            is TextLayer -> l.copy(isLocked = !l.isLocked)
                                            is ShapeLayer -> l.copy(isLocked = !l.isLocked)
                                            is ImageLayer -> l.copy(isLocked = !l.isLocked)
                                            is DrawingLayer -> l.copy(isLocked = !l.isLocked)
                                        }
                                    }
                                )
                                executedBadges.add("تغییر وضعیت قفل «${target.name}»")
                            }
                        }

                        "MERGE_VISIBLE_LAYERS" -> {
                            val visibleCount = workingProject.layers.count { it.isVisible }
                            if (visibleCount >= 2) {
                                val tempProject = workingProject.copy(
                                    background = CanvasBackground(type = BackgroundType.TRANSPARENT),
                                    postFx = CanvasPostFx(vignetteEnabled = false, noiseEnabled = false, stripesEnabled = false)
                                )
                                val mergedBitmap = withContext(Dispatchers.Default) {
                                    StudioCanvasRenderer.renderProjectToBitmap(
                                        context = context,
                                        project = tempProject,
                                        scaleMultiplier = 1f,
                                        includeCheckerboardForTransparent = false
                                    )
                                }
                                val hiddenLayers = workingProject.layers.filter { !it.isVisible }
                                val mergedLayer = ImageLayer(
                                    name = "لایه ادغام‌شده ($visibleCount لایه)",
                                    bitmap = mergedBitmap
                                )
                                workingProject = workingProject.copy(layers = hiddenLayers + mergedLayer)
                                workingSelectedId = mergedLayer.id
                                executedBadges.add("ادغام $visibleCount لایه")
                            }
                        }

                        "SET_CANVAS_ASPECT" -> {
                            val ratio = act.optString("aspectRatioLabel", "1:1")
                            val preset = DEFAULT_ASPECT_PRESETS.find { it.ratioLabel == ratio }
                                ?: DEFAULT_ASPECT_PRESETS.first()
                            workingProject = workingProject.copy(
                                canvasWidth = preset.width,
                                canvasHeight = preset.height
                            )
                            executedBadges.add("تغییر ابعاد بوم به ${preset.ratioLabel}")
                        }

                        "SET_CANVAS_BACKGROUND" -> {
                            val pHex = act.optString("primaryColorHex", "").takeIf { it.startsWith("#") }
                                ?: workingProject.background.primaryColorHex
                            val sHex = act.optString("secondaryColorHex", "").takeIf { it.startsWith("#") }
                                ?: workingProject.background.secondaryColorHex
                            val useGrad = if (act.has("useGradient") && !act.isNull("useGradient")) {
                                act.optBoolean("useGradient", true)
                            } else true

                            workingProject = workingProject.copy(
                                background = workingProject.background.copy(
                                    type = if (useGrad) BackgroundType.GRADIENT else BackgroundType.COLOR,
                                    primaryColorHex = pHex,
                                    secondaryColorHex = sHex,
                                    backgroundBitmap = null
                                )
                            )
                            executedBadges.add("تنظیم پس‌زمینه بوم ($pHex)")
                        }

                        "SET_POST_FX" -> {
                            val vig = if (act.has("vignetteEnabled") && !act.isNull("vignetteEnabled")) {
                                act.optBoolean("vignetteEnabled")
                            } else workingProject.postFx.vignetteEnabled
                            val noise = if (act.has("noiseEnabled") && !act.isNull("noiseEnabled")) {
                                act.optBoolean("noiseEnabled")
                            } else workingProject.postFx.noiseEnabled
                            val stripes = if (act.has("stripesEnabled") && !act.isNull("stripesEnabled")) {
                                act.optBoolean("stripesEnabled")
                            } else workingProject.postFx.stripesEnabled

                            workingProject = workingProject.copy(
                                postFx = workingProject.postFx.copy(
                                    vignetteEnabled = vig,
                                    noiseEnabled = noise,
                                    stripesEnabled = stripes
                                )
                            )
                            executedBadges.add("تنظیم افکت‌های نوری بوم")
                        }

                        "ADD_TEXT_LAYER" -> {
                            val txt = act.optString("text", "").takeIf { it.isNotBlank() && it != "null" }
                                ?: "متن هوشمند پیکسل‌لب"
                            val newTextLayer = applyJsonToTextLayer(
                                base = TextLayer(
                                    name = act.optString("layerName", "").takeIf { it.isNotBlank() && it != "null" }
                                        ?: "متن: ${txt.take(14)}",
                                    text = txt,
                                    y = ((workingProject.layers.size % 5) - 1) * 0.08f
                                ),
                                act = act
                            )
                            workingProject = workingProject.copy(layers = workingProject.layers + newTextLayer)
                            workingSelectedId = newTextLayer.id
                            executedBadges.add("افزودن لایه متن «${txt.take(16)}»")
                        }

                        "UPDATE_SELECTED_TEXT", "UPDATE_TEXT_BY_ID" -> {
                            val resolved = resolveAgentTargetLayer(workingProject, act, workingSelectedId)
                            val existingText = (resolved as? TextLayer)
                                ?: workingProject.layers.find { it.id == workingSelectedId } as? TextLayer
                                ?: workingProject.layers.filterIsInstance<TextLayer>().lastOrNull()

                            val normalizedUserCmd = com.example.ui.agent.OfflineDesignEngine.normalizePersianCommand(cleanCmd)
                            val isExplicitWriteCmd = normalizedUserCmd.startsWith("بنویس") ||
                                normalizedUserCmd.startsWith("متن ") ||
                                normalizedUserCmd.startsWith("تیتر ") ||
                                cleanCmd.contains(":")
                            val candidateNewText = act.optString("text", "").trim()
                            val normalizedCandidate = com.example.ui.agent.OfflineDesignEngine.normalizePersianCommand(candidateNewText)
                            val shouldProtectExistingText = existingText != null &&
                                candidateNewText.isNotEmpty() &&
                                !isExplicitWriteCmd &&
                                (normalizedCandidate == normalizedUserCmd ||
                                    com.example.ui.agent.OfflineDesignEngine.isLikelyDesignInstruction(normalizedCandidate))

                            val safeAct = if (shouldProtectExistingText) {
                                JSONObject(act.toString()).apply { remove("text") }
                            } else {
                                act
                            }

                            if (existingText != null) {
                                val updatedText = applyJsonToTextLayer(existingText, safeAct)
                                workingProject = workingProject.copy(
                                    layers = workingProject.layers.map {
                                        if (it.id == existingText.id) updatedText else it
                                    }
                                )
                                workingSelectedId = updatedText.id
                                executedBadges.add("ارتقای استایل لایه «${updatedText.text.take(15)}»")
                            } else {
                                val defaultTitle = if (com.example.ui.agent.OfflineDesignEngine.isLikelyDesignInstruction(normalizedUserCmd)) {
                                    "پیکسل‌لب استودیو"
                                } else {
                                    cleanCmd.take(24)
                                }
                                val created = applyJsonToTextLayer(TextLayer(text = defaultTitle), safeAct)
                                workingProject = workingProject.copy(layers = workingProject.layers + created)
                                workingSelectedId = created.id
                                executedBadges.add("ساخت و استایل‌دهی لایه متن")
                            }
                        }

                        "ADD_SHAPE_LAYER" -> {
                            val kindStr = act.optString("shapeKind", "HEXAGON")
                            val kind = runCatching { ShapeKind.valueOf(kindStr) }.getOrDefault(ShapeKind.HEXAGON)
                            val createdShape = applyJsonToShapeLayer(
                                base = ShapeLayer(
                                    name = act.optString("layerName", "").takeIf { it.isNotBlank() && it != "null" }
                                        ?: kind.labelFa,
                                    shapeKind = kind
                                ),
                                act = act
                            )
                            workingProject = workingProject.copy(layers = workingProject.layers + createdShape)
                            workingSelectedId = createdShape.id
                            executedBadges.add("افزودن شکل ${kind.labelFa}")
                        }

                        "UPDATE_SELECTED_SHAPE" -> {
                            val resolved = resolveAgentTargetLayer(workingProject, act, workingSelectedId)
                            val existingShape = (resolved as? ShapeLayer)
                                ?: (workingProject.layers.find { it.id == workingSelectedId } as? ShapeLayer)
                                ?: workingProject.layers.filterIsInstance<ShapeLayer>().lastOrNull()
                            if (existingShape != null) {
                                val updatedShape = applyJsonToShapeLayer(existingShape, act)
                                workingProject = workingProject.copy(
                                    layers = workingProject.layers.map {
                                        if (it.id == existingShape.id) updatedShape else it
                                    }
                                )
                                workingSelectedId = updatedShape.id
                                executedBadges.add("ویرایش شکل ${updatedShape.name}")
                            }
                        }

                        "UPDATE_SELECTED_IMAGE" -> {
                            val resolved = resolveAgentTargetLayer(workingProject, act, workingSelectedId)
                            val existingImg = (resolved as? ImageLayer)
                                ?: (workingProject.layers.find { it.id == workingSelectedId } as? ImageLayer)
                                ?: workingProject.layers.filterIsInstance<ImageLayer>().lastOrNull()
                            if (existingImg != null) {
                                val pHex = act.optString("primaryColorHex", "").takeIf { it.startsWith("#") }
                                val updatedImg = existingImg.copy(
                                    colorOverlayEnabled = pHex != null || existingImg.colorOverlayEnabled,
                                    colorOverlayHex = pHex ?: existingImg.colorOverlayHex,
                                    stroke = if (act.has("strokeEnabled")) {
                                        existingImg.stroke.copy(
                                            enabled = act.optBoolean("strokeEnabled", existingImg.stroke.enabled),
                                            colorHex = act.optString("strokeColorHex", existingImg.stroke.colorHex).takeIf { it.startsWith("#") } ?: existingImg.stroke.colorHex,
                                            width = act.optDouble("strokeWidth", existingImg.stroke.width.toDouble()).toFloat()
                                        )
                                    } else existingImg.stroke
                                )
                                workingProject = workingProject.copy(
                                    layers = workingProject.layers.map { if (it.id == existingImg.id) updatedImg else it }
                                )
                                workingSelectedId = updatedImg.id
                                executedBadges.add("ویرایش لایه تصویر «${updatedImg.name}»")
                            }
                        }

                        "ADD_BUILTIN_STICKER" -> {
                            val sId = act.optString("stickerId", "crown_gold").lowercase()
                            val found = BUILTIN_PIXELLAB_STICKERS.find { it.id == sId }
                                ?: BUILTIN_PIXELLAB_STICKERS.first()
                            val sBmp = StudioCanvasRenderer.renderBuiltinStickerToBitmap(
                                symbolText = found.symbolText,
                                primaryHex = found.primaryHex,
                                secondaryHex = found.secondaryHex
                            )
                            val sLayer = ImageLayer(
                                name = "استیکر: ${found.labelFa}",
                                bitmap = sBmp,
                                x = act.optDouble("x", 0.0).toFloat().coerceIn(-0.45f, 0.45f),
                                y = act.optDouble("y", 0.0).toFloat().coerceIn(-0.45f, 0.45f),
                                scaleX = act.optDouble("scale", 0.85).toFloat().coerceIn(0.3f, 2.5f),
                                scaleY = act.optDouble("scale", 0.85).toFloat().coerceIn(0.3f, 2.5f)
                            )
                            workingProject = workingProject.copy(layers = workingProject.layers + sLayer)
                            workingSelectedId = sLayer.id
                            executedBadges.add("افزودن استیکر «${found.labelFa}»")
                        }

                        "APPLY_QUOTE" -> {
                            val qText = act.optString("quoteText", "").takeIf { it.isNotBlank() }
                                ?: act.optString("text", "").takeIf { it.isNotBlank() }
                                ?: "طراحی هوشمند با ایجنت پیکسل‌لب"
                            val selText = workingProject.layers.find { it.id == workingSelectedId } as? TextLayer
                            if (selText != null) {
                                val updated = selText.copy(text = qText)
                                workingProject = workingProject.copy(
                                    layers = workingProject.layers.map { if (it.id == selText.id) updated else it }
                                )
                                executedBadges.add("جایگزینی متن با جمله: «${qText.take(16)}...»")
                            } else {
                                val newT = TextLayer(name = "متن جمله", text = qText, fontSizeSp = 32f, fontId = "vazirmatn")
                                workingProject = workingProject.copy(layers = workingProject.layers + newT)
                                workingSelectedId = newT.id
                                executedBadges.add("درج جمله جدید «${qText.take(16)}...»")
                            }
                        }

                        "TOGGLE_ANTI_GRAVITY" -> {
                            val next = !workingProject.isAntiGravityActive
                            workingProject = workingProject.copy(isAntiGravityActive = next)
                            _isAntiGravityActive.value = next
                            val engine = getOrCreatePhysicsEngine(context)
                            engine.setAntiGravityActive(next, workingProject.layers.map { it.id })
                            executedBadges.add(if (next) "فعال‌سازی فیزیک ضدجاذبه (Zero-G)" else "غیرفعال‌سازی حالت ضدجاذبه")
                        }

                        "AUTO_LAYOUT_PREVIEW", "AUTO_LAYOUT_HARMONIZE" -> {
                            val suggestion = com.example.ui.agent.OfflineDesignEngine.generateHarmonicAutoLayout(workingProject)
                            _activePreviewState.value = com.example.data.AgentPreviewState(
                                originalProject = workingProject,
                                suggestion = suggestion,
                                showOriginalComparison = false
                            )
                            executedBadges.add("ایجاد پیش‌نمایش چیدمان هوشمند با نسبت طلایی")
                        }

                        "TRANSFORM_SELECTED_LAYER" -> {
                            val target = resolveAgentTargetLayer(workingProject, act, workingSelectedId)
                            if (target != null) {
                                val nx = if (act.has("x") && !act.isNull("x")) act.optDouble("x", target.x.toDouble()).toFloat().coerceIn(-0.48f, 0.48f) else target.x
                                val ny = if (act.has("y") && !act.isNull("y")) act.optDouble("y", target.y.toDouble()).toFloat().coerceIn(-0.48f, 0.48f) else target.y
                                val sc = if (act.has("scale") && !act.isNull("scale")) act.optDouble("scale", target.scaleX.toDouble()).toFloat().coerceIn(0.25f, 3.5f) else target.scaleX
                                val rx = if (act.has("rotationX") && !act.isNull("rotationX")) act.optDouble("rotationX", target.rotationX.toDouble()).toFloat().coerceIn(-65f, 65f) else target.rotationX
                                val ry = if (act.has("rotationY") && !act.isNull("rotationY")) act.optDouble("rotationY", target.rotationY.toDouble()).toFloat().coerceIn(-65f, 65f) else target.rotationY
                                val rz = if (act.has("rotationZ") && !act.isNull("rotationZ")) act.optDouble("rotationZ", target.rotationZ.toDouble()).toFloat().coerceIn(-180f, 180f) else target.rotationZ
                                val skx = if (act.has("skewX") && !act.isNull("skewX")) act.optDouble("skewX", target.skewX.toDouble()).toFloat().coerceIn(-0.75f, 0.75f) else target.skewX
                                val sky = if (act.has("skewY") && !act.isNull("skewY")) act.optDouble("skewY", target.skewY.toDouble()).toFloat().coerceIn(-0.75f, 0.75f) else target.skewY
                                val op = if (act.has("opacity") && !act.isNull("opacity")) act.optDouble("opacity", target.opacity.toDouble()).toFloat().coerceIn(0.1f, 1f) else target.opacity

                                val transformed: StudioLayer = when (target) {
                                    is TextLayer -> target.copy(x = nx, y = ny, scaleX = sc, scaleY = sc, rotationX = rx, rotationY = ry, rotationZ = rz, skewX = skx, skewY = sky, opacity = op, isLocked = false)
                                    is ShapeLayer -> target.copy(x = nx, y = ny, scaleX = sc, scaleY = sc, rotationX = rx, rotationY = ry, rotationZ = rz, skewX = skx, skewY = sky, opacity = op, isLocked = false)
                                    is ImageLayer -> target.copy(x = nx, y = ny, scaleX = sc, scaleY = sc, rotationX = rx, rotationY = ry, rotationZ = rz, skewX = skx, skewY = sky, opacity = op, isLocked = false)
                                    is DrawingLayer -> target.copy(x = nx, y = ny, scaleX = sc, scaleY = sc, rotationX = rx, rotationY = ry, rotationZ = rz, skewX = skx, skewY = sky, opacity = op, isLocked = false)
                                }
                                workingProject = workingProject.copy(
                                    layers = workingProject.layers.map { if (it.id == target.id) transformed else it }
                                )
                                workingSelectedId = target.id
                                executedBadges.add("تغییر موقعیت/ابعاد «${target.name}»")
                            }
                        }

                        "DELETE_SELECTED_LAYER" -> {
                            val target = resolveAgentTargetLayer(workingProject, act, workingSelectedId)
                            if (target != null) {
                                val remaining = workingProject.layers.filterNot { it.id == target.id }
                                workingProject = workingProject.copy(
                                    layers = remaining,
                                    background = if (remaining.isEmpty() && workingProject.background.backgroundBitmap != null) {
                                        workingProject.background.copy(type = BackgroundType.GRADIENT, backgroundBitmap = null)
                                    } else workingProject.background
                                )
                                workingSelectedId = remaining.lastOrNull()?.id
                                executedBadges.add("حذف لایه «${target.name}»")
                            }
                        }

                        "DUPLICATE_SELECTED_LAYER" -> {
                            val target = resolveAgentTargetLayer(workingProject, act, workingSelectedId)
                            if (target != null) {
                                val newId = UUID.randomUUID().toString()
                                val dup: StudioLayer = when (target) {
                                    is TextLayer -> target.copy(id = newId, name = "${target.name} (کپی)", x = (target.x + 0.05f).coerceIn(-0.45f, 0.45f), y = (target.y + 0.05f).coerceIn(-0.45f, 0.45f))
                                    is ShapeLayer -> target.copy(id = newId, name = "${target.name} (کپی)", x = (target.x + 0.05f).coerceIn(-0.45f, 0.45f), y = (target.y + 0.05f).coerceIn(-0.45f, 0.45f))
                                    is ImageLayer -> target.copy(id = newId, name = "${target.name} (کپی)", x = (target.x + 0.05f).coerceIn(-0.45f, 0.45f), y = (target.y + 0.05f).coerceIn(-0.45f, 0.45f))
                                    is DrawingLayer -> target.copy(id = newId, name = "${target.name} (کپی)", x = (target.x + 0.05f).coerceIn(-0.45f, 0.45f), y = (target.y + 0.05f).coerceIn(-0.45f, 0.45f))
                                }
                                workingProject = workingProject.copy(layers = workingProject.layers + dup)
                                workingSelectedId = newId
                                executedBadges.add("کپی لایه «${target.name}»")
                            }
                        }

                        "GENERATE_AI_BACKGROUND" -> {
                            val imgPrompt = act.optString("imagePrompt", "").takeIf { it.isNotBlank() && it != "null" }
                                ?: cleanCmd
                            _aiState.value = AiTaskUiState.Loading("ایجنت در حال تولید تصویر پس‌زمینه با Nano Banana 2.1...")
                            val bgBitmap = withTimeout(60_000L) {
                                val imgResp = imageGenModel.generateContent(imgPrompt)
                                imgResp.candidates.firstOrNull()
                                    ?.content?.parts?.filterIsInstance<ImagePart>()
                                    ?.firstOrNull()?.image
                            }
                            if (bgBitmap != null) {
                                workingProject = workingProject.copy(
                                    background = workingProject.background.copy(
                                        type = BackgroundType.IMAGE,
                                        backgroundBitmap = bgBitmap
                                    )
                                )
                                executedBadges.add("تولید پس‌زمینه تصویری با جمینای")
                            }
                        }

                        "GENERATE_AI_STICKER" -> {
                            val stickerPrompt = act.optString("imagePrompt", "").takeIf { it.isNotBlank() && it != "null" }
                                ?: cleanCmd
                            _aiState.value = AiTaskUiState.Loading("ایجنت در حال طراحی استیکر شفاف با Nano Banana 2.1...")
                            val enriched = "Die-cut graphic design sticker of $stickerPrompt, isolated on a pure flat solid white #FFFFFF background, clean vector edges, vibrant studio illustration, centered, no cast shadow on background"
                            val rawSticker = withTimeout(60_000L) {
                                val imgResp = imageGenModel.generateContent(enriched)
                                imgResp.candidates.firstOrNull()
                                    ?.content?.parts?.filterIsInstance<ImagePart>()
                                    ?.firstOrNull()?.image
                            }
                            if (rawSticker != null) {
                                val cutout = withContext(Dispatchers.Default) {
                                    StudioCanvasRenderer.makeStickerTransparent(rawSticker, tolerance = 0.22f)
                                }
                                val stickerLayer = ImageLayer(
                                    name = "استیکر ایجنت",
                                    bitmap = cutout,
                                    y = act.optDouble("y", 0.0).toFloat().coerceIn(-0.45f, 0.45f),
                                    stroke = LayerStroke(enabled = true, colorHex = "#FFFFFF", width = 6f),
                                    shadow = LayerShadow(enabled = true, blurRadius = 18f, offsetY = 8f)
                                )
                                workingProject = workingProject.copy(layers = workingProject.layers + stickerLayer)
                                workingSelectedId = stickerLayer.id
                                executedBadges.add("تولید و دوربری استیکر شفاف")
                            }
                        }

                        "EDIT_SELECTED_IMAGE_AI" -> {
                            val editInstr = act.optString("imagePrompt", "").takeIf { it.isNotBlank() && it != "null" }
                                ?: cleanCmd
                            _aiState.value = AiTaskUiState.Loading("ایجنت در حال ویرایش هوشمند لایه تصویر...")
                            val selectedImg = workingProject.layers.find { it.id == workingSelectedId } as? ImageLayer
                            val srcBmp = selectedImg?.bitmap ?: withContext(Dispatchers.Default) {
                                StudioCanvasRenderer.renderProjectToBitmap(
                                    context = context,
                                    project = workingProject,
                                    scaleMultiplier = 0.75f,
                                    includeCheckerboardForTransparent = false
                                )
                            }
                            val editedBmp = withTimeout(60_000L) {
                                val imgResp = imageGenModel.generateContent(
                                    content {
                                        image(srcBmp)
                                        text(editInstr)
                                    }
                                )
                                imgResp.candidates.firstOrNull()
                                    ?.content?.parts?.filterIsInstance<ImagePart>()
                                    ?.firstOrNull()?.image
                            }
                            if (editedBmp != null) {
                                if (selectedImg != null) {
                                    val updatedImg = selectedImg.copy(bitmap = editedBmp, name = "${selectedImg.name} (AI)")
                                    workingProject = workingProject.copy(
                                        layers = workingProject.layers.map { if (it.id == selectedImg.id) updatedImg else it }
                                    )
                                } else {
                                    val addedImg = ImageLayer(name = "تصویر ویرایش‌شده ایجنت", bitmap = editedBmp)
                                    workingProject = workingProject.copy(layers = workingProject.layers + addedImg)
                                    workingSelectedId = addedImg.id
                                }
                                executedBadges.add("ویرایش هوشمند تصویر با جمینای")
                            }
                        }
                    }
                }

                val distinctBadges = executedBadges.distinct()
                val didModifyProject = workingProject != preTransactionProject && !txOutcome.session.wasRolledBack

                if (txOutcome.session.wasRolledBack) {
                    _project.value = preTransactionProject
                    _selectedLayerId.value = preTransactionSelectedId
                    _pendingAgentTransaction.value = null
                    _activeAiMutatedLayerId.value = null
                } else if (didModifyProject) {
                    pushUndoSnapshot()
                    _project.value = workingProject
                    _selectedLayerId.value = workingSelectedId

                    val postPreviewBitmap = withContext(Dispatchers.Default) {
                        txOutcome.session.renderedPreviewBitmap ?: runCatching {
                            StudioCanvasRenderer.renderProjectToBitmap(
                                context = context,
                                project = workingProject,
                                scaleMultiplier = 0.4f,
                                includeCheckerboardForTransparent = false
                            )
                        }.getOrNull()
                    }

                    val validationReport = txOutcome.session.lastValidationReport
                        ?: com.example.ui.agent.PixelLabGeminiTools.validateStudioDocument(workingProject)

                    _pendingAgentTransaction.value = com.example.ui.agent.PendingAgentTransaction(
                        userPrompt = cleanCmd,
                        modelUsed = txOutcome.modelName,
                        preTransactionProject = preTransactionProject,
                        preTransactionSelectedLayerId = preTransactionSelectedId,
                        postTransactionProject = workingProject,
                        postTransactionSelectedLayerId = workingSelectedId,
                        previewBitmap = postPreviewBitmap,
                        beforePreviewBitmap = visionBitmap,
                        validationReport = validationReport,
                        steps = txOutcome.session.steps.toList()
                    )
                }

                _activeTransactionSteps.value = txOutcome.session.steps.toList()

                // 3. Auto-sync to Cloud Firestore if Google Account is active
                val currentAuth = Firebase.auth.currentUser
                if (currentAuth != null && distinctBadges.isNotEmpty()) {
                    launch {
                        try {
                            repository?.saveProject(workingProject)
                            AgentDiagnosticsLogger.info(
                                tag = "AutoSync",
                                message = "Project auto-synced to cloud after Agent actions"
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Cloud auto-save warning: ${e.message}")
                        }
                    }
                }

                val agentMsg = AgentChatMessage(
                    isUser = false,
                    text = replyFa,
                    executedActions = distinctBadges,
                    canUndo = didModifyProject || distinctBadges.isNotEmpty(),
                    designCritique = txOutcome.designCritiqueFa,
                    followUpSuggestions = txOutcome.followUpSuggestionsFa,
                    latencyMs = txOutcome.latencyMs,
                    modelName = txOutcome.modelName,
                    usedCanvasVision = txOutcome.usedCanvasVision
                )
                _agentMessages.value = _agentMessages.value + agentMsg
                _aiState.value = AiTaskUiState.Success(replyFa)
                _statusBanner.value = "ایجنت پیکسل‌لب (${txOutcome.modelName}): ${distinctBadges.size} گام در ${txOutcome.latencyMs}ms اجرا شد."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val (diagCat, diagDet) = PixelLabAgentEngine.diagnoseError(e)
                Log.e(TAG, "Agent inference caught top-level exception: $diagCat | $diagDet", e)
                AgentDiagnosticsLogger.error(
                    tag = "AgentExecution",
                    message = "Agent execution exception: $diagCat",
                    details = diagDet,
                    throwable = e
                )
                val errText = "خطا در پردازش دستور ($diagCat): $diagDet"
                _lastGeminiError.value = errText
                _aiState.value = AiTaskUiState.Error(errText)
                _statusBanner.value = errText
            }
        }
    }

    private fun resolveAgentTargetLayer(
        project: StudioProject,
        act: JSONObject,
        selectedLayerId: String?
    ): StudioLayer? {
        val rawTarget = act.optString("targetLayerId", "").trim()
        if (rawTarget.isNotBlank() && rawTarget != "null" && rawTarget != "NONE") {
            project.layers.find { it.id == rawTarget }?.let { return it }
            project.layers.find { it.name.contains(rawTarget, ignoreCase = true) }?.let { return it }
            project.layers.filterIsInstance<TextLayer>()
                .find { it.text.contains(rawTarget, ignoreCase = true) }?.let { return it }
        }
        return project.layers.find { it.id == selectedLayerId } ?: project.layers.lastOrNull()
    }

    private fun applyJsonToTextLayer(base: TextLayer, act: JSONObject): TextLayer {
        val textVal = act.optString("text", "").takeIf { it.isNotBlank() && it != "null" } ?: base.text
        val nameVal = act.optString("layerName", "").takeIf { it.isNotBlank() && it != "null" } ?: base.name
        val fontVal = act.optString("fontId", "").takeIf { it.isNotBlank() && it != "null" } ?: base.fontId
        val sizeVal = if (act.has("fontSizeSp") && !act.isNull("fontSizeSp")) {
            act.optDouble("fontSizeSp", base.fontSizeSp.toDouble()).toFloat().coerceIn(16f, 128f)
        } else base.fontSizeSp

        val pHex = act.optString("primaryColorHex", "").takeIf { it.startsWith("#") } ?: base.colorHex
        val sHex = act.optString("secondaryColorHex", "").takeIf { it.startsWith("#") } ?: base.gradientSecondColorHex
        val useGrad = if (act.has("useGradient") && !act.isNull("useGradient")) {
            act.optBoolean("useGradient", base.useGradientFill)
        } else (act.has("secondaryColorHex") && act.optString("secondaryColorHex").startsWith("#")) || base.useGradientFill

        val strokeEn = if (act.has("strokeEnabled") && !act.isNull("strokeEnabled")) {
            act.optBoolean("strokeEnabled", base.stroke.enabled)
        } else base.stroke.enabled
        val strokeHex = act.optString("strokeColorHex", "").takeIf { it.startsWith("#") } ?: base.stroke.colorHex
        val strokeW = if (act.has("strokeWidth") && !act.isNull("strokeWidth")) {
            act.optDouble("strokeWidth", base.stroke.width.toDouble()).toFloat().coerceIn(1f, 22f)
        } else base.stroke.width

        val shadowEn = if (act.has("shadowEnabled") && !act.isNull("shadowEnabled")) {
            act.optBoolean("shadowEnabled", base.shadow.enabled)
        } else base.shadow.enabled
        val shadowHex = act.optString("shadowColorHex", "").takeIf { it.startsWith("#") } ?: base.shadow.colorHex
        val shadowBlur = if (act.has("shadowBlur") && !act.isNull("shadowBlur")) {
            act.optDouble("shadowBlur", base.shadow.blurRadius.toDouble()).toFloat().coerceIn(1f, 38f)
        } else base.shadow.blurRadius

        val embossEn = if (act.has("embossEnabled") && !act.isNull("embossEnabled")) {
            act.optBoolean("embossEnabled", base.emboss.enabled)
        } else base.emboss.enabled

        val is3d = if (act.has("is3dEnabled") && !act.isNull("is3dEnabled")) {
            act.optBoolean("is3dEnabled", base.is3dEnabled)
        } else if (act.has("depth3d") && !act.isNull("depth3d")) {
            act.optInt("depth3d", 0) > 0
        } else base.is3dEnabled

        val depth = if (act.has("depth3d") && !act.isNull("depth3d")) {
            act.optInt("depth3d", base.depth3d).coerceIn(1, 55)
        } else base.depth3d

        val obAngle = if (act.has("obliqueAngle3d") && !act.isNull("obliqueAngle3d")) {
            act.optDouble("obliqueAngle3d", base.obliqueAngle3d.toDouble()).toFloat().coerceIn(0f, 360f)
        } else base.obliqueAngle3d

        val rx = if (act.has("rotationX") && !act.isNull("rotationX")) {
            act.optDouble("rotationX", base.rotationX.toDouble()).toFloat().coerceIn(-65f, 65f)
        } else base.rotationX
        val ry = if (act.has("rotationY") && !act.isNull("rotationY")) {
            act.optDouble("rotationY", base.rotationY.toDouble()).toFloat().coerceIn(-65f, 65f)
        } else base.rotationY
        val rz = if (act.has("rotationZ") && !act.isNull("rotationZ")) {
            act.optDouble("rotationZ", base.rotationZ.toDouble()).toFloat().coerceIn(-180f, 180f)
        } else base.rotationZ

        val curve = if (act.has("curveAngle") && !act.isNull("curveAngle")) {
            act.optDouble("curveAngle", base.curveAngle.toDouble()).toFloat().coerceIn(-260f, 260f)
        } else base.curveAngle

        val refl = if (act.has("reflectionEnabled") && !act.isNull("reflectionEnabled")) {
            act.optBoolean("reflectionEnabled", base.reflectionEnabled)
        } else base.reflectionEnabled

        val sh3d = if (act.has("shadow3dEnabled") && !act.isNull("shadow3dEnabled")) {
            act.optBoolean("shadow3dEnabled", base.shadow3dEnabled)
        } else base.shadow3dEnabled

        val texPreset = act.optString("texturePreset", "").let { name ->
            runCatching { TexturePreset.valueOf(name) }.getOrNull() ?: base.texturePreset
        }

        val maskPreset = act.optString("maskMode", "").let { name ->
            runCatching { MaskPresetMode.valueOf(name) }.getOrNull() ?: base.maskMode
        }

        val skx = if (act.has("skewX") && !act.isNull("skewX")) {
            act.optDouble("skewX", base.skewX.toDouble()).toFloat().coerceIn(-0.75f, 0.75f)
        } else base.skewX
        val sky = if (act.has("skewY") && !act.isNull("skewY")) {
            act.optDouble("skewY", base.skewY.toDouble()).toFloat().coerceIn(-0.75f, 0.75f)
        } else base.skewY

        val pillEn = if (act.has("backgroundPillEnabled") && !act.isNull("backgroundPillEnabled")) {
            act.optBoolean("backgroundPillEnabled", base.backgroundPillEnabled)
        } else base.backgroundPillEnabled
        val pillHex = act.optString("backgroundPillColorHex", "").takeIf { it.startsWith("#") }
            ?: base.backgroundPillColorHex

        val nx = if (act.has("x") && !act.isNull("x")) {
            act.optDouble("x", base.x.toDouble()).toFloat().coerceIn(-0.46f, 0.46f)
        } else base.x
        val ny = if (act.has("y") && !act.isNull("y")) {
            act.optDouble("y", base.y.toDouble()).toFloat().coerceIn(-0.46f, 0.46f)
        } else base.y
        val sc = if (act.has("scale") && !act.isNull("scale")) {
            act.optDouble("scale", base.scaleX.toDouble()).toFloat().coerceIn(0.25f, 3.2f)
        } else base.scaleX
        val op = if (act.has("opacity") && !act.isNull("opacity")) {
            act.optDouble("opacity", base.opacity.toDouble()).toFloat().coerceIn(0.1f, 1f)
        } else base.opacity

        return base.copy(
            name = nameVal,
            text = textVal,
            fontId = fontVal,
            fontSizeSp = sizeVal,
            x = nx,
            y = ny,
            scaleX = sc,
            scaleY = sc,
            rotationX = rx,
            rotationY = ry,
            rotationZ = rz,
            skewX = skx,
            skewY = sky,
            opacity = op,
            colorHex = pHex,
            useGradientFill = useGrad,
            gradientSecondColorHex = sHex,
            texturePreset = texPreset,
            maskMode = maskPreset,
            stroke = base.stroke.copy(enabled = strokeEn, colorHex = strokeHex, width = strokeW),
            shadow = base.shadow.copy(enabled = shadowEn, colorHex = shadowHex, blurRadius = shadowBlur),
            emboss = base.emboss.copy(enabled = embossEn),
            is3dEnabled = is3d,
            depth3d = depth,
            obliqueAngle3d = obAngle,
            shadow3dEnabled = sh3d,
            curveAngle = curve,
            reflectionEnabled = refl,
            backgroundPillEnabled = pillEn,
            backgroundPillColorHex = pillHex
        )
    }

    private fun applyJsonToShapeLayer(base: ShapeLayer, act: JSONObject): ShapeLayer {
        val kindStr = act.optString("shapeKind", "")
        val kind = runCatching { ShapeKind.valueOf(kindStr) }.getOrDefault(base.shapeKind)
        val pHex = act.optString("primaryColorHex", "").takeIf { it.startsWith("#") } ?: base.fillColorHex
        val sHex = act.optString("secondaryColorHex", "").takeIf { it.startsWith("#") } ?: base.gradientSecondColorHex
        val useGrad = if (act.has("useGradient") && !act.isNull("useGradient")) {
            act.optBoolean("useGradient", base.useGradientFill)
        } else base.useGradientFill
        val texPreset = act.optString("texturePreset", "").let { name ->
            runCatching { TexturePreset.valueOf(name) }.getOrNull() ?: base.texturePreset
        }
        val is3d = if (act.has("is3dEnabled") && !act.isNull("is3dEnabled")) {
            act.optBoolean("is3dEnabled", base.is3dExtruded)
        } else base.is3dExtruded
        val depth = if (act.has("depth3d") && !act.isNull("depth3d")) {
            act.optInt("depth3d", base.depth3d).coerceIn(2, 40)
        } else base.depth3d
        val nx = if (act.has("x") && !act.isNull("x")) act.optDouble("x", base.x.toDouble()).toFloat().coerceIn(-0.46f, 0.46f) else base.x
        val ny = if (act.has("y") && !act.isNull("y")) act.optDouble("y", base.y.toDouble()).toFloat().coerceIn(-0.46f, 0.46f) else base.y
        val sc = if (act.has("scale") && !act.isNull("scale")) act.optDouble("scale", base.scaleX.toDouble()).toFloat().coerceIn(0.25f, 3.0f) else base.scaleX
        val op = if (act.has("opacity") && !act.isNull("opacity")) act.optDouble("opacity", base.opacity.toDouble()).toFloat().coerceIn(0.1f, 1f) else base.opacity

        return base.copy(
            shapeKind = kind,
            fillColorHex = pHex,
            useGradientFill = useGrad,
            gradientSecondColorHex = sHex,
            texturePreset = texPreset,
            is3dExtruded = is3d,
            depth3d = depth,
            x = nx,
            y = ny,
            scaleX = sc,
            scaleY = sc,
            opacity = op
        )
    }

    // =========================================================================
    // DIRECT GEMINI AI STUDIO TOOLS (Firebase AI Logic)
    // =========================================================================

    fun generateAiImage(prompt: String, applyAsCanvasBackground: Boolean) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _aiState.value = AiTaskUiState.Loading("در حال تولید تصویر با جمینای (Nano Banana 2.1)... ممکن است ۳۰ تا ۶۰ ثانیه زمان ببرد.")
            try {
                val bitmap = withTimeout(60_000L) {
                    val response = imageGenModel.generateContent(prompt)
                    response.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()?.image
                }
                if (bitmap != null) {
                    pushUndoSnapshot()
                    if (applyAsCanvasBackground) {
                        _project.value = _project.value.copy(
                            background = _project.value.background.copy(
                                type = BackgroundType.IMAGE,
                                backgroundBitmap = bitmap
                            )
                        )
                        _aiState.value = AiTaskUiState.Success("پس‌زمینه هوش مصنوعی با موفقیت تولید و روی بوم اعمال شد!")
                    } else {
                        addImageLayerFromBitmap(bitmap, "تصویر جمینای: ${prompt.take(18)}")
                        _aiState.value = AiTaskUiState.Success("لایه تصویر هوش مصنوعی با موفقیت به بوم اضافه شد!")
                    }
                } else {
                    _aiState.value = AiTaskUiState.Error("مدل تصویری برنگرداند. لطفاً دستور خود را واضح‌تر بنویسید.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI image generation timed out", e)
                _aiState.value = AiTaskUiState.Error("زمان تولید تصویر به پایان رسید (۶۰ ثانیه). لطفاً دوباره تلاش کنید.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI image generation failed", e)
                _aiState.value = AiTaskUiState.Error(e.localizedMessage ?: "خطا در تولید تصویر با هوش مصنوعی.")
            }
        }
    }

    fun generateAiSticker(stickerPrompt: String) {
        if (stickerPrompt.isBlank()) return
        viewModelScope.launch {
            _aiState.value = AiTaskUiState.Loading("در حال طراحی استیکر گرافیکی و جداسازی پس‌زمینه (۳۰ تا ۶۰ ثانیه)...")
            try {
                val enrichedPrompt = "Die-cut graphic design sticker of $stickerPrompt, isolated on a pure flat solid white #FFFFFF background, clean vector edges, vibrant studio illustration, centered, no cast shadow on background"
                val rawBitmap = withTimeout(60_000L) {
                    val response = imageGenModel.generateContent(enrichedPrompt)
                    response.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()?.image
                }
                if (rawBitmap != null) {
                    val transparentSticker = withContext(Dispatchers.Default) {
                        StudioCanvasRenderer.makeStickerTransparent(rawBitmap, tolerance = 0.22f)
                    }
                    pushUndoSnapshot()
                    val stickerLayer = ImageLayer(
                        name = "استیکر: ${stickerPrompt.take(16)}",
                        bitmap = transparentSticker,
                        stroke = LayerStroke(enabled = true, colorHex = "#FFFFFF", width = 6f),
                        shadow = LayerShadow(enabled = true, blurRadius = 18f, offsetY = 8f)
                    )
                    _project.value = _project.value.copy(layers = _project.value.layers + stickerLayer)
                    _selectedLayerId.value = stickerLayer.id
                    _aiState.value = AiTaskUiState.Success("استیکر شفاف تولید شد و به عنوان لایه مستقل روی بوم قرار گرفت!")
                } else {
                    _aiState.value = AiTaskUiState.Error("مدل تصویری برای استیکر برنگرداند.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI sticker generation timed out", e)
                _aiState.value = AiTaskUiState.Error("زمان تولید استیکر به پایان رسید (۶۰ ثانیه).")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI sticker generation failed", e)
                _aiState.value = AiTaskUiState.Error(e.localizedMessage ?: "خطا در تولید استیکر.")
            }
        }
    }

    fun editCurrentLayerOrCanvasWithAi(context: Context, editInstruction: String) {
        if (editInstruction.isBlank()) return
        viewModelScope.launch {
            _aiState.value = AiTaskUiState.Loading("در حال ویرایش هوشمند تصویر با جمینای (۳۰ تا ۶۰ ثانیه)...")
            try {
                val selectedLayer = _project.value.layers.find { it.id == _selectedLayerId.value }
                val sourceBitmap: Bitmap = if (selectedLayer is ImageLayer && selectedLayer.bitmap != null) {
                    selectedLayer.bitmap
                } else {
                    withContext(Dispatchers.Default) {
                        StudioCanvasRenderer.renderProjectToBitmap(
                            context = context,
                            project = _project.value,
                            scaleMultiplier = 0.75f,
                            includeCheckerboardForTransparent = false
                        )
                    }
                }

                val editedBitmap = withTimeout(60_000L) {
                    val response = imageGenModel.generateContent(
                        content {
                            image(sourceBitmap)
                            text(editInstruction)
                        }
                    )
                    response.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()?.image
                }

                if (editedBitmap != null) {
                    pushUndoSnapshot()
                    if (selectedLayer is ImageLayer) {
                        updateSelectedImageLayer { it.copy(bitmap = editedBitmap, name = "${it.name} (ویرایش AI)") }
                    } else {
                        addImageLayerFromBitmap(editedBitmap, "ویرایش هوشمند: ${editInstruction.take(15)}")
                    }
                    _aiState.value = AiTaskUiState.Success("ویرایش هوشمند تصویر با موفقیت روی لایه اعمال شد!")
                } else {
                    _aiState.value = AiTaskUiState.Error("مدل تصویر ویرایش‌شده‌ای برنگرداند.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI image edit timed out", e)
                _aiState.value = AiTaskUiState.Error("زمان ویرایش هوشمند تصویر به پایان رسید (۶۰ ثانیه).")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI image edit failed", e)
                _aiState.value = AiTaskUiState.Error(e.localizedMessage ?: "خطا در ویرایش هوشمند تصویر.")
            }
        }
    }

    fun generateAiAutoLayout(topicPrompt: String) {
        if (topicPrompt.isBlank()) return
        viewModelScope.launch {
            val activeModel = _selectedGeminiModel.value
            _aiState.value = AiTaskUiState.Loading("در حال طراحی خودکار چیدمان لایه‌ها، پالت رنگ و استایل سه‌بعدی با مدل $activeModel...")
            try {
                val fullPrompt = """
                    You are an expert graphic designer in PixelLab Studio.
                    Design a visually striking, multi-layer poster layout for the following user request:
                    "$topicPrompt"
                    Ensure high contrast between background hex colors and headline text hex colors.
                    If the user request is in Persian, write headlineText and subtitleText in eloquent Persian.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    getLayoutAiModel().generateContent(fullPrompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("پاسخ خالی از مدل چیدمان دریافت شد.")
                val json = JSONObject(jsonString)

                val projectTitle = json.optString("projectTitle", "پوستر هوشمند جمینای")
                val bgPrimary = json.optString("backgroundPrimaryHex", "#0F172A")
                val bgSecondary = json.optString("backgroundSecondaryHex", "#3B0764")
                val headlineText = json.optString("headlineText", topicPrompt)
                val headlineColor = json.optString("headlineColorHex", "#FDE047")
                val headlineSecColor = json.optString("headlineSecondaryHex", "#F59E0B")
                val headlineStroke = json.optString("headlineStrokeHex", "#090D16")
                val fontId = json.optString("fontId", StudioFont.LALEZAR.id)
                val depth3d = json.optInt("depth3d", 22).coerceIn(0, 50)
                val curveAngle = json.optInt("curveAngle", 0).toFloat().coerceIn(-180f, 180f)
                val subtitleText = json.optString("subtitleText", "")
                val subtitleColor = json.optString("subtitleColorHex", "#E2E8F0")
                val badgeShapeName = json.optString("badgeShape", "HEXAGON")
                val badgeColor = json.optString("badgeColorHex", "#38BDF8")

                val shapeKind = runCatching { ShapeKind.valueOf(badgeShapeName) }.getOrDefault(ShapeKind.HEXAGON)

                pushUndoSnapshot()
                val newLayers = mutableListOf<StudioLayer>()

                newLayers.add(
                    ShapeLayer(
                        name = "المان گرافیکی پس‌زمینه",
                        shapeKind = shapeKind,
                        sizeRatio = 0.52f,
                        fillColorHex = badgeColor,
                        useGradientFill = true,
                        gradientSecondColorHex = bgSecondary,
                        opacity = 0.28f,
                        is3dExtruded = true,
                        depth3d = 10
                    )
                )

                val mainHeadline = TextLayer(
                    name = "تیتر اصلی (AI)",
                    text = headlineText,
                    fontId = fontId,
                    fontSizeSp = 58f,
                    y = -0.06f,
                    colorHex = headlineColor,
                    useGradientFill = true,
                    gradientSecondColorHex = headlineSecColor,
                    stroke = LayerStroke(enabled = true, colorHex = headlineStroke, width = 7f),
                    shadow = LayerShadow(enabled = true, colorHex = "#CC000000", blurRadius = 22f, offsetY = 12f),
                    emboss = LayerEmboss(enabled = true, intensity = 60f),
                    is3dEnabled = depth3d > 0,
                    depth3d = depth3d,
                    curveAngle = curveAngle
                )
                newLayers.add(mainHeadline)

                if (subtitleText.isNotBlank()) {
                    newLayers.add(
                        TextLayer(
                            name = "زیرعنوان (AI)",
                            text = subtitleText,
                            fontId = StudioFont.VAZIRMATN.id,
                            fontSizeSp = 28f,
                            y = 0.22f,
                            colorHex = subtitleColor,
                            useGradientFill = false,
                            stroke = LayerStroke(enabled = false),
                            is3dEnabled = false,
                            backgroundPillEnabled = true,
                            backgroundPillColorHex = "#AA0B0E14",
                            backgroundPillPadding = 20f,
                            backgroundPillRadius = 24f
                        )
                    )
                }

                _project.value = _project.value.copy(
                    title = projectTitle,
                    background = CanvasBackground(
                        type = BackgroundType.GRADIENT,
                        primaryColorHex = bgPrimary,
                        secondaryColorHex = bgSecondary,
                        gradientAngle = 135f
                    ),
                    layers = newLayers
                )
                _selectedLayerId.value = mainHeadline.id
                _aiState.value = AiTaskUiState.Success("چیدمان چندلایه «$projectTitle» با موفقیت طراحی و روی بوم اعمال شد!")
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI auto-layout timed out", e)
                _aiState.value = AiTaskUiState.Error("زمان طراحی خودکار چیدمان به پایان رسید (۶۰ ثانیه).")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI auto-layout failed", e)
                _aiState.value = AiTaskUiState.Error(e.localizedMessage ?: "خطا در طراحی خودکار چیدمان با جمینای.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _physicsEngine?.release()
    }

    companion object {
        fun provideFactory(
            repository: StudioProjectRepository,
            initialUserId: String?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StudioViewModel(repository, initialUserId)
            }
        }

        private fun createInitialProject(): StudioProject {
            val emblemShape = ShapeLayer(
                name = "هسته شش‌ضلعی نئون",
                shapeKind = ShapeKind.HEXAGON,
                sizeRatio = 0.46f,
                fillColorHex = "#0284C7",
                useGradientFill = true,
                gradientSecondColorHex = "#7E22CE",
                opacity = 0.35f,
                stroke = LayerStroke(enabled = true, colorHex = "#38BDF8", width = 3f),
                is3dExtruded = true,
                depth3d = 12
            )
            val mainHeadline = TextLayer(
                name = "تیتر سه‌بعدی اصلی",
                text = "پیکسل‌لب هوش مصنوعی",
                fontId = StudioFont.LALEZAR.id,
                fontSizeSp = 56f,
                y = -0.05f,
                colorHex = "#FDE047",
                useGradientFill = true,
                gradientSecondColorHex = "#38BDF8",
                stroke = LayerStroke(enabled = true, colorHex = "#090D16", width = 8f),
                shadow = LayerShadow(enabled = true, colorHex = "#DD000000", blurRadius = 20f, offsetX = 0f, offsetY = 10f),
                emboss = LayerEmboss(enabled = true, lightAngle = 45f, intensity = 65f),
                is3dEnabled = true,
                depth3d = 22,
                obliqueAngle3d = 135f
            )
            val subHeadline = TextLayer(
                name = "زیرعنوان استودیو",
                text = "ایجنت هوشمند جمینای • تایپوگرافی 3D • پروژه PLP",
                fontId = StudioFont.VAZIRMATN.id,
                fontSizeSp = 24f,
                y = 0.20f,
                colorHex = "#F8FAFC",
                useGradientFill = false,
                stroke = LayerStroke(enabled = false),
                is3dEnabled = false,
                backgroundPillEnabled = true,
                backgroundPillColorHex = "#AA0F172A",
                backgroundPillPadding = 18f,
                backgroundPillRadius = 20f
            )
            return StudioProject(
                title = "پوستر سه‌بعدی PixelLab AI",
                canvasWidth = 1080,
                canvasHeight = 1080,
                background = CanvasBackground(type = BackgroundType.PRESET_NEBULA),
                postFx = CanvasPostFx(vignetteEnabled = true, vignetteIntensity = 0.35f),
                layers = listOf(emblemShape, mainHeadline, subHeadline)
            )
        }
    }
}
