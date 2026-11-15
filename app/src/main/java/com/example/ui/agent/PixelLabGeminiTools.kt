package com.example.ui.agent

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.BUILTIN_PIXELLAB_STICKERS
import com.example.data.BackgroundType
import com.example.data.CanvasBackground
import com.example.data.CanvasPostFx
import com.example.data.DEFAULT_ASPECT_PRESETS
import com.example.data.DrawingLayer
import com.example.data.ImageLayer
import com.example.data.LayerBlendMode
import com.example.data.LayerEmboss
import com.example.data.LayerInnerShadow
import com.example.data.LayerShadow
import com.example.data.LayerStroke
import com.example.data.ShapeKind
import com.example.data.ShapeLayer
import com.example.data.StudioFont
import com.example.data.StudioLayer
import com.example.data.StudioProject
import com.example.data.TextAlignMode
import com.example.data.TextLayer
import com.example.data.TexturePreset
import com.example.ui.canvas.StudioCanvasRenderer
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class AgentStepStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    ROLLED_BACK
}

data class AgentExecutionStep(
    val id: String = UUID.randomUUID().toString(),
    val stepIndex: Int,
    val toolName: String,
    val titleFa: String,
    val argsSummary: String = "",
    val resultSummaryFa: String = "",
    val status: AgentStepStatus = AgentStepStatus.COMPLETED,
    val durationMs: Long = 0L
)

data class AgentValidationReport(
    val isValid: Boolean,
    val scoreOutOf100: Int,
    val issuesFa: List<String>,
    val autoFixesFa: List<String>,
    val summaryFa: String
)

data class PendingAgentTransaction(
    val transactionId: String = UUID.randomUUID().toString(),
    val userPrompt: String,
    val modelUsed: String,
    val preTransactionProject: StudioProject,
    val preTransactionSelectedLayerId: String?,
    val postTransactionProject: StudioProject,
    val postTransactionSelectedLayerId: String?,
    val previewBitmap: Bitmap?,
    val beforePreviewBitmap: Bitmap?,
    val validationReport: AgentValidationReport,
    val steps: List<AgentExecutionStep>,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/**
 * Holds live mutable state during a multi-step Gemini Function Calling transaction,
 * supporting inspection, atomic mutation, preview rendering, validation, and rollback.
 */
class AgentTransactionSession(
    val context: Context?,
    val userCommand: String = "",
    val preTransactionProject: StudioProject,
    val preTransactionSelectedLayerId: String?,
    val allowProductImageModification: Boolean = false
) {
    var workingProject: StudioProject = preTransactionProject
    var workingSelectedLayerId: String? = preTransactionSelectedLayerId
    var renderedPreviewBitmap: Bitmap? = null
    var lastValidationReport: AgentValidationReport? = null
    var wasRolledBack: Boolean = false

    private val _steps = mutableListOf<AgentExecutionStep>()
    val steps: List<AgentExecutionStep> get() = _steps.toList()

    // IDs of existing ImageLayers before the transaction started so we never overwrite or delete
    // product images unless the user explicitly requested it.
    val protectedProductImageIds: Set<String> = preTransactionProject.layers
        .filterIsInstance<ImageLayer>()
        .map { it.id }
        .toSet()

    fun recordStep(
        toolName: String,
        titleFa: String,
        argsSummary: String,
        resultSummaryFa: String,
        status: AgentStepStatus = AgentStepStatus.COMPLETED,
        durationMs: Long = 0L
    ): AgentExecutionStep {
        val step = AgentExecutionStep(
            stepIndex = _steps.size + 1,
            toolName = toolName,
            titleFa = titleFa,
            argsSummary = argsSummary,
            resultSummaryFa = resultSummaryFa,
            status = status,
            durationMs = durationMs
        )
        _steps.add(step)
        return step
    }

    fun rollbackToPreTransaction(): StudioProject {
        workingProject = preTransactionProject
        workingSelectedLayerId = preTransactionSelectedLayerId
        wasRolledBack = true
        return workingProject
    }
}

/**
 * Official 18 Real Editor Tools for the PixelLab Gemini AI Agent.
 * Connected directly to StudioProject, StudioLayer, and StudioCanvasRenderer.
 */
object PixelLabGeminiTools {

    val ALL_TOOL_NAMES = listOf(
        "inspect_document",
        "inspect_layers",
        "inspect_layer",
        "add_text_layer",
        "update_text_layer",
        "add_image_layer",
        "add_shape_layer",
        "transform_layer",
        "set_layer_style",
        "set_background",
        "reorder_layers",
        "align_layers",
        "duplicate_layer",
        "delete_layer",
        "group_layers",
        "render_preview",
        "validate_document",
        "undo_transaction"
    )

    // =========================================================================
    // 1. GEMINI FUNCTION DECLARATIONS (18 TOOLS)
    // =========================================================================

    val inspectDocumentDeclaration = FunctionDeclaration(
        name = "inspect_document",
        description = "Inspects the current PixelLab project document: canvas width, height, aspect ratio, background settings, post-FX, layer counts, and selected layer status.",
        parameters = mapOf(
            "includeLayerSummary" to Schema.boolean(description = "Whether to include a concise list of layers in the document summary")
        ),
        optionalParameters = listOf("includeLayerSummary")
    )

    val inspectLayersDeclaration = FunctionDeclaration(
        name = "inspect_layers",
        description = "Lists all layers in the project from bottom to top with their IDs, types (TEXT, SHAPE, IMAGE, DRAWING), text content, coordinates, size, rotation, lock, and visibility state.",
        parameters = mapOf(
            "filterType" to Schema.string(description = "Optional filter: ALL, TEXT, SHAPE, IMAGE, or DRAWING"),
            "onlyVisible" to Schema.boolean(description = "If true, only returns visible layers")
        ),
        optionalParameters = listOf("filterType", "onlyVisible")
    )

    val inspectLayerDeclaration = FunctionDeclaration(
        name = "inspect_layer",
        description = "Inspects full styling, geometry, 3D depth, stroke, shadow, font, alignment, and image/shape details of a specific layer by ID, name, or SELECTED.",
        parameters = mapOf(
            "layerId" to Schema.string(description = "Layer ID, layer name substring, or SELECTED")
        ),
        optionalParameters = listOf("layerId")
    )

    val addTextLayerDeclaration = FunctionDeclaration(
        name = "add_text_layer",
        description = "Adds a new Persian or English text layer to the canvas with full typography, alignment, color/gradient, 3D extrusion, stroke, shadow, and background pill options.",
        parameters = mapOf(
            "text" to Schema.string(description = "The Persian or English text content to display on the canvas"),
            "layerName" to Schema.string(description = "Optional descriptive name for the layer"),
            "fontId" to Schema.string(description = "Font ID: lalezar, vazirmatn, amiri, montserrat, playfair, or pacifico"),
            "fontSizeSp" to Schema.double(description = "Font size in sp (16..128)"),
            "colorHex" to Schema.string(description = "Primary fill hex color (#RRGGBB)"),
            "secondaryColorHex" to Schema.string(description = "Secondary gradient hex color (#RRGGBB)"),
            "useGradient" to Schema.boolean(description = "Whether to use a two-color gradient fill"),
            "textAlign" to Schema.string(description = "Text alignment: RIGHT, CENTER, LEFT, or JUSTIFY"),
            "x" to Schema.double(description = "Normalized horizontal position (-0.45..0.45, 0 is center)"),
            "y" to Schema.double(description = "Normalized vertical position (-0.45..0.45, 0 is center)"),
            "scale" to Schema.double(description = "Uniform scale multiplier (0.3..3.0, default 1.0)"),
            "rotationZ" to Schema.double(description = "Rotation angle in degrees (-180..180)"),
            "is3dEnabled" to Schema.boolean(description = "Enable 3D extrusion depth"),
            "depth3d" to Schema.integer(description = "3D depth amount (1..60)"),
            "strokeEnabled" to Schema.boolean(description = "Enable outline stroke around text"),
            "strokeColorHex" to Schema.string(description = "Outline stroke hex color (#RRGGBB)"),
            "strokeWidth" to Schema.double(description = "Outline stroke width (1..20)"),
            "shadowEnabled" to Schema.boolean(description = "Enable drop shadow behind text"),
            "backgroundPillEnabled" to Schema.boolean(description = "Enable rounded background pill behind text"),
            "backgroundPillColorHex" to Schema.string(description = "Background pill hex color (#AARRGGBB or #RRGGBB)")
        ),
        optionalParameters = listOf(
            "layerName", "fontId", "fontSizeSp", "colorHex", "secondaryColorHex",
            "useGradient", "textAlign", "x", "y", "scale", "rotationZ",
            "is3dEnabled", "depth3d", "strokeEnabled", "strokeColorHex", "strokeWidth",
            "shadowEnabled", "backgroundPillEnabled", "backgroundPillColorHex"
        )
    )

    val updateTextLayerDeclaration = FunctionDeclaration(
        name = "update_text_layer",
        description = "Updates an existing text layer's text content, font, size, alignment, spacing, curve angle, or colors. Omit 'text' when only changing style so original text is preserved.",
        parameters = mapOf(
            "layerId" to Schema.string(description = "Target text layer ID, name/text substring, or SELECTED"),
            "text" to Schema.string(description = "New text string ONLY if explicitly changing the words of the layer"),
            "fontId" to Schema.string(description = "Font ID: lalezar, vazirmatn, amiri, montserrat, playfair, or pacifico"),
            "fontSizeSp" to Schema.double(description = "Font size in sp (16..128)"),
            "colorHex" to Schema.string(description = "Primary fill hex color (#RRGGBB)"),
            "secondaryColorHex" to Schema.string(description = "Secondary gradient hex color (#RRGGBB)"),
            "useGradient" to Schema.boolean(description = "Enable two-color gradient fill"),
            "textAlign" to Schema.string(description = "Alignment mode: RIGHT, CENTER, LEFT, or JUSTIFY"),
            "letterSpacing" to Schema.double(description = "Letter spacing (-0.1..0.5)"),
            "lineSpacing" to Schema.double(description = "Line spacing multiplier (0.8..2.2)"),
            "curveAngle" to Schema.double(description = "Curve text angle in degrees (-360..360, 0 is straight)"),
            "isBold" to Schema.boolean(description = "Bold style"),
            "isItalic" to Schema.boolean(description = "Italic style"),
            "isUnderline" to Schema.boolean(description = "Underline style"),
            "backgroundPillEnabled" to Schema.boolean(description = "Enable rounded background pill"),
            "backgroundPillColorHex" to Schema.string(description = "Background pill hex color")
        ),
        optionalParameters = listOf(
            "layerId", "text", "fontId", "fontSizeSp", "colorHex", "secondaryColorHex",
            "useGradient", "textAlign", "letterSpacing", "lineSpacing", "curveAngle",
            "isBold", "isItalic", "isUnderline", "backgroundPillEnabled", "backgroundPillColorHex"
        )
    )

    val addImageLayerDeclaration = FunctionDeclaration(
        name = "add_image_layer",
        description = "Adds a graphic sticker, emblem, or decorative badge image layer to the canvas.",
        parameters = mapOf(
            "stickerId" to Schema.string(description = "Built-in graphic sticker ID: crown_gold, fire, rocket, diamond, verified, heart, or badge_star"),
            "layerName" to Schema.string(description = "Optional name for the image layer"),
            "primaryColorHex" to Schema.string(description = "Primary hex color for the rendered graphic"),
            "secondaryColorHex" to Schema.string(description = "Secondary hex color for the rendered graphic"),
            "x" to Schema.double(description = "Normalized X position (-0.45..0.45)"),
            "y" to Schema.double(description = "Normalized Y position (-0.45..0.45)"),
            "scale" to Schema.double(description = "Scale factor (0.25..2.5)")
        ),
        optionalParameters = listOf("stickerId", "layerName", "primaryColorHex", "secondaryColorHex", "x", "y", "scale")
    )

    val addShapeLayerDeclaration = FunctionDeclaration(
        name = "add_shape_layer",
        description = "Adds a geometric or decorative vector shape layer (RECTANGLE, CIRCLE, TRIANGLE, PENTAGON, STAR, HEXAGON, HEART, BADGE, ARROW, SPEECH_BUBBLE) to the canvas.",
        parameters = mapOf(
            "shapeKind" to Schema.string(description = "Shape type: RECTANGLE, CIRCLE, TRIANGLE, PENTAGON, STAR, HEXAGON, HEART, BADGE, ARROW, or SPEECH_BUBBLE"),
            "layerName" to Schema.string(description = "Optional layer name"),
            "fillColorHex" to Schema.string(description = "Primary fill hex color (#RRGGBB)"),
            "secondaryColorHex" to Schema.string(description = "Secondary gradient hex color (#RRGGBB)"),
            "useGradient" to Schema.boolean(description = "Enable gradient fill"),
            "cornerRadius" to Schema.double(description = "Corner radius for rectangles (0..120)"),
            "x" to Schema.double(description = "Normalized horizontal offset (-0.45..0.45)"),
            "y" to Schema.double(description = "Normalized vertical offset (-0.45..0.45)"),
            "scaleX" to Schema.double(description = "Horizontal scale multiplier (0.2..3.5)"),
            "scaleY" to Schema.double(description = "Vertical scale multiplier (0.2..3.5)"),
            "rotationZ" to Schema.double(description = "Rotation in degrees (-180..180)"),
            "opacity" to Schema.double(description = "Opacity (0.1..1.0)"),
            "is3dExtruded" to Schema.boolean(description = "Enable 3D shape extrusion"),
            "depth3d" to Schema.integer(description = "3D extrusion depth (2..45)")
        ),
        optionalParameters = listOf(
            "layerName", "fillColorHex", "secondaryColorHex", "useGradient",
            "cornerRadius", "x", "y", "scaleX", "scaleY", "rotationZ", "opacity",
            "is3dExtruded", "depth3d"
        )
    )

    val transformLayerDeclaration = FunctionDeclaration(
        name = "transform_layer",
        description = "Transforms a layer's position (x, y), scale (scaleX, scaleY), 2D/3D rotation (rotationX, rotationY, rotationZ), skew, flip, opacity, lock, or visibility.",
        parameters = mapOf(
            "layerId" to Schema.string(description = "Layer ID, name/text substring, or SELECTED"),
            "x" to Schema.double(description = "Normalized horizontal offset from center (-0.48..0.48)"),
            "y" to Schema.double(description = "Normalized vertical offset from center (-0.48..0.48)"),
            "scaleX" to Schema.double(description = "Horizontal scale multiplier (0.2..3.5)"),
            "scaleY" to Schema.double(description = "Vertical scale multiplier (0.2..3.5)"),
            "rotationX" to Schema.double(description = "3D X-axis tilt in degrees (-65..65)"),
            "rotationY" to Schema.double(description = "3D Y-axis tilt in degrees (-65..65)"),
            "rotationZ" to Schema.double(description = "2D Z-axis rotation in degrees (-180..180)"),
            "skewX" to Schema.double(description = "Horizontal perspective skew (-0.7..0.7)"),
            "skewY" to Schema.double(description = "Vertical perspective skew (-0.7..0.7)"),
            "opacity" to Schema.double(description = "Layer opacity (0.05..1.0)"),
            "flipHorizontal" to Schema.boolean(description = "Flip image layer horizontally"),
            "flipVertical" to Schema.boolean(description = "Flip image layer vertically"),
            "isVisible" to Schema.boolean(description = "Set layer visibility"),
            "isLocked" to Schema.boolean(description = "Set layer lock state")
        ),
        optionalParameters = listOf(
            "layerId", "x", "y", "scaleX", "scaleY", "rotationX", "rotationY", "rotationZ",
            "skewX", "skewY", "opacity", "flipHorizontal", "flipVertical", "isVisible", "isLocked"
        )
    )

    val setLayerStyleDeclaration = FunctionDeclaration(
        name = "set_layer_style",
        description = "Configures visual styling of a text, shape, or image layer: colors, gradient, stroke, double stroke, drop shadow, inner shadow, emboss, 3D depth/lighting, reflection, and texture.",
        parameters = mapOf(
            "layerId" to Schema.string(description = "Layer ID, name/text substring, or SELECTED"),
            "primaryColorHex" to Schema.string(description = "Primary color hex (#RRGGBB)"),
            "secondaryColorHex" to Schema.string(description = "Secondary gradient color hex (#RRGGBB)"),
            "useGradient" to Schema.boolean(description = "Enable gradient fill"),
            "strokeEnabled" to Schema.boolean(description = "Enable outline stroke"),
            "strokeColorHex" to Schema.string(description = "Stroke color hex (#RRGGBB)"),
            "strokeWidth" to Schema.double(description = "Stroke width (1..24)"),
            "secondStrokeEnabled" to Schema.boolean(description = "Enable outer second stroke on text"),
            "secondStrokeColorHex" to Schema.string(description = "Second stroke color hex (#RRGGBB)"),
            "secondStrokeWidth" to Schema.double(description = "Second stroke width (2..26)"),
            "shadowEnabled" to Schema.boolean(description = "Enable drop shadow"),
            "shadowColorHex" to Schema.string(description = "Drop shadow color hex (#AARRGGBB or #RRGGBB)"),
            "shadowBlur" to Schema.double(description = "Drop shadow blur radius (1..40)"),
            "shadowOffsetX" to Schema.double(description = "Drop shadow X offset (-30..30)"),
            "shadowOffsetY" to Schema.double(description = "Drop shadow Y offset (-30..30)"),
            "innerShadowEnabled" to Schema.boolean(description = "Enable inner shadow"),
            "embossEnabled" to Schema.boolean(description = "Enable bevel emboss effect"),
            "is3dEnabled" to Schema.boolean(description = "Enable 3D depth extrusion"),
            "depth3d" to Schema.integer(description = "3D extrusion depth (1..60)"),
            "obliqueAngle3d" to Schema.double(description = "3D extrusion angle in degrees (0..360)"),
            "lightAngle3d" to Schema.double(description = "3D light source angle (0..360)"),
            "reflectionEnabled" to Schema.boolean(description = "Enable mirror reflection below text"),
            "texturePreset" to Schema.string(description = "Texture preset: NONE, GOLD_FOIL, SILVER_CHROME, CARBON_FIBER, CYBER_GRID, or FIRE_LAVA"),
            "blendMode" to Schema.string(description = "Blend mode: NORMAL, MULTIPLY, SCREEN, OVERLAY, or ADD")
        ),
        optionalParameters = listOf(
            "layerId", "primaryColorHex", "secondaryColorHex", "useGradient",
            "strokeEnabled", "strokeColorHex", "strokeWidth",
            "secondStrokeEnabled", "secondStrokeColorHex", "secondStrokeWidth",
            "shadowEnabled", "shadowColorHex", "shadowBlur", "shadowOffsetX", "shadowOffsetY",
            "innerShadowEnabled", "embossEnabled", "is3dEnabled", "depth3d",
            "obliqueAngle3d", "lightAngle3d", "reflectionEnabled", "texturePreset", "blendMode"
        )
    )

    val setBackgroundDeclaration = FunctionDeclaration(
        name = "set_background",
        description = "Configures the canvas background (solid color, linear/radial gradient, or transparent), canvas pixel dimensions (128..4096) or aspect ratio preset, and post-FX (vignette, noise).",
        parameters = mapOf(
            "backgroundType" to Schema.string(description = "Background type: COLOR, GRADIENT, or TRANSPARENT"),
            "primaryColorHex" to Schema.string(description = "Primary background hex color (#RRGGBB)"),
            "secondaryColorHex" to Schema.string(description = "Secondary gradient hex color (#RRGGBB)"),
            "gradientAngle" to Schema.double(description = "Gradient angle in degrees (0..360)"),
            "isRadialGradient" to Schema.boolean(description = "True for radial gradient, false for linear"),
            "aspectRatioPreset" to Schema.string(description = "Optional aspect ratio preset: 1:1, 4:5, 16:9, 9:16, or 3:1"),
            "canvasWidth" to Schema.integer(description = "Optional custom canvas width in pixels (128..4096)"),
            "canvasHeight" to Schema.integer(description = "Optional custom canvas height in pixels (128..4096)"),
            "vignetteEnabled" to Schema.boolean(description = "Enable dark cinema vignette on canvas edges"),
            "noiseEnabled" to Schema.boolean(description = "Enable subtle film grain texture")
        ),
        optionalParameters = listOf(
            "backgroundType", "primaryColorHex", "secondaryColorHex", "gradientAngle",
            "isRadialGradient", "aspectRatioPreset", "canvasWidth", "canvasHeight",
            "vignetteEnabled", "noiseEnabled"
        )
    )

    val reorderLayersDeclaration = FunctionDeclaration(
        name = "reorder_layers",
        description = "Changes the z-order of a layer in the layer stack (bring to front, send to back, move up one step, move down one step, or move to specific index).",
        parameters = mapOf(
            "layerId" to Schema.string(description = "Layer ID, name/text substring, or SELECTED"),
            "direction" to Schema.string(description = "Order direction: TO_FRONT, TO_BACK, MOVE_UP, MOVE_DOWN, or TO_INDEX"),
            "targetIndex" to Schema.integer(description = "Target index when direction is TO_INDEX")
        ),
        optionalParameters = listOf("layerId", "direction", "targetIndex")
    )

    val alignLayersDeclaration = FunctionDeclaration(
        name = "align_layers",
        description = "Aligns one or more layers relative to the canvas or distributes multiple layers evenly with harmonic spacing.",
        parameters = mapOf(
            "layerIds" to Schema.string(description = "Comma-separated layer IDs, SELECTED, ALL, or TEXT"),
            "alignmentMode" to Schema.string(description = "One of: CENTER_HORIZONTAL, CENTER_VERTICAL, CENTER_BOTH, ALIGN_LEFT, ALIGN_RIGHT, ALIGN_TOP, ALIGN_BOTTOM, DISTRIBUTE_VERTICAL, DISTRIBUTE_HORIZONTAL, GOLDEN_RATIO_STACK")
        ),
        optionalParameters = listOf("layerIds")
    )

    val duplicateLayerDeclaration = FunctionDeclaration(
        name = "duplicate_layer",
        description = "Duplicates an existing layer on the canvas with a slight position offset.",
        parameters = mapOf(
            "layerId" to Schema.string(description = "Layer ID, name/text substring, or SELECTED"),
            "offsetX" to Schema.double(description = "Normalized X offset for the copy (-0.3..0.3, default 0.04)"),
            "offsetY" to Schema.double(description = "Normalized Y offset for the copy (-0.3..0.3, default 0.04)"),
            "newName" to Schema.string(description = "Optional name for the duplicated layer")
        ),
        optionalParameters = listOf("layerId", "offsetX", "offsetY", "newName")
    )

    val deleteLayerDeclaration = FunctionDeclaration(
        name = "delete_layer",
        description = "Deletes a specified layer from the canvas. Note: Product image layers are protected by default unless allowDeleteImage is true.",
        parameters = mapOf(
            "layerId" to Schema.string(description = "Layer ID, name/text substring, or SELECTED"),
            "allowDeleteImage" to Schema.boolean(description = "Set true ONLY if the user explicitly asked to delete an image layer")
        ),
        optionalParameters = listOf("layerId", "allowDeleteImage")
    )

    val groupLayersDeclaration = FunctionDeclaration(
        name = "group_layers",
        description = "Groups multiple layers to move, scale, center them together while preserving relative spacing, or raster-merges them into a single composite layer.",
        parameters = mapOf(
            "layerIds" to Schema.string(description = "Comma-separated layer IDs, VISIBLE, or TEXT_AND_SHAPES"),
            "groupAction" to Schema.string(description = "Action: ALIGN_GROUP_CENTER, MOVE_GROUP, SCALE_GROUP, or MERGE_TO_IMAGE_LAYER"),
            "deltaX" to Schema.double(description = "Horizontal shift for MOVE_GROUP (-0.4..0.4)"),
            "deltaY" to Schema.double(description = "Vertical shift for MOVE_GROUP (-0.4..0.4)"),
            "scaleFactor" to Schema.double(description = "Scale multiplier for SCALE_GROUP (0.5..2.0)"),
            "groupName" to Schema.string(description = "Optional name for the grouped/merged layer")
        ),
        optionalParameters = listOf("layerIds", "deltaX", "deltaY", "scaleFactor", "groupName")
    )

    val renderPreviewDeclaration = FunctionDeclaration(
        name = "render_preview",
        description = "Renders a live bitmap preview of the current canvas state so the user and agent can inspect the visual output.",
        parameters = mapOf(
            "scaleMultiplier" to Schema.double(description = "Preview resolution scale multiplier (0.25..1.0, default 0.5)")
        ),
        optionalParameters = listOf("scaleMultiplier")
    )

    val validateDocumentDeclaration = FunctionDeclaration(
        name = "validate_document",
        description = "Validates the current design for out-of-bounds elements, text readability/contrast, overlapping text layers, and product image preservation, with optional auto-fix.",
        parameters = mapOf(
            "autoFix" to Schema.boolean(description = "Automatically fix out-of-bounds coordinates, low contrast, or overlapping text layers")
        ),
        optionalParameters = listOf("autoFix")
    )

    val undoTransactionDeclaration = FunctionDeclaration(
        name = "undo_transaction",
        description = "Rolls back the current transaction to the exact project state before the command started.",
        parameters = mapOf(
            "reason" to Schema.string(description = "Optional reason for rolling back the transaction")
        ),
        optionalParameters = listOf("reason")
    )

    val allDeclarations: List<FunctionDeclaration> = listOf(
        inspectDocumentDeclaration,
        inspectLayersDeclaration,
        inspectLayerDeclaration,
        addTextLayerDeclaration,
        updateTextLayerDeclaration,
        addImageLayerDeclaration,
        addShapeLayerDeclaration,
        transformLayerDeclaration,
        setLayerStyleDeclaration,
        setBackgroundDeclaration,
        reorderLayersDeclaration,
        alignLayersDeclaration,
        duplicateLayerDeclaration,
        deleteLayerDeclaration,
        groupLayersDeclaration,
        renderPreviewDeclaration,
        validateDocumentDeclaration,
        undoTransactionDeclaration
    )

    val allToolNames: List<String> = listOf(
        "inspect_document",
        "inspect_layers",
        "inspect_layer",
        "add_text_layer",
        "update_text_layer",
        "add_image_layer",
        "add_shape_layer",
        "transform_layer",
        "set_layer_style",
        "set_background",
        "reorder_layers",
        "align_layers",
        "duplicate_layer",
        "delete_layer",
        "group_layers",
        "render_preview",
        "validate_document",
        "undo_transaction"
    )

    val geminiToolBundle: Tool by lazy {
        Tool.functionDeclarations(allDeclarations)
    }

    // =========================================================================
    // 2. REAL TOOL DISPATCHER & EXECUTION ENGINE
    // =========================================================================

    fun executeToolCall(
        toolName: String,
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val startNs = System.nanoTime()
        val argsSummary = args.entries.joinToString(", ") { (k, v) ->
            "$k=${v.toString().replace("\"", "").take(28)}"
        }

        return try {
            val resultObj = when (toolName) {
                "inspect_document" -> execInspectDocument(args, session)
                "inspect_layers" -> execInspectLayers(args, session)
                "inspect_layer" -> execInspectLayer(args, session)
                "add_text_layer" -> execAddTextLayer(args, session)
                "update_text_layer" -> execUpdateTextLayer(args, session)
                "add_image_layer" -> execAddImageLayer(args, session)
                "add_shape_layer" -> execAddShapeLayer(args, session)
                "transform_layer" -> execTransformLayer(args, session)
                "set_layer_style" -> execSetLayerStyle(args, session)
                "set_background" -> execSetBackground(args, session)
                "reorder_layers" -> execReorderLayers(args, session)
                "align_layers" -> execAlignLayers(args, session)
                "duplicate_layer" -> execDuplicateLayer(args, session)
                "delete_layer" -> execDeleteLayer(args, session)
                "group_layers" -> execGroupLayers(args, session)
                "render_preview" -> execRenderPreview(args, session)
                "validate_document" -> execValidateDocument(args, session)
                "undo_transaction" -> execUndoTransaction(args, session)
                else -> JsonObject(
                    mapOf(
                        "status" to JsonPrimitive("error"),
                        "message" to JsonPrimitive("Unknown tool: $toolName")
                    )
                )
            }

            val durationMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
            val summaryFa = resultObj["summaryFa"]?.jsonPrimitive?.contentOrNull
                ?: "ابزار $toolName اجرا شد"
            val titleFa = toolTitleFa(toolName)

            session.recordStep(
                toolName = toolName,
                titleFa = titleFa,
                argsSummary = argsSummary,
                resultSummaryFa = summaryFa,
                status = if (toolName == "undo_transaction") AgentStepStatus.ROLLED_BACK else AgentStepStatus.COMPLETED,
                durationMs = durationMs
            )
            resultObj
        } catch (e: Exception) {
            val durationMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
            val errMsg = e.localizedMessage ?: e.javaClass.simpleName
            session.recordStep(
                toolName = toolName,
                titleFa = toolTitleFa(toolName),
                argsSummary = argsSummary,
                resultSummaryFa = "خطا در اجرای $toolName: $errMsg",
                status = AgentStepStatus.FAILED,
                durationMs = durationMs
            )
            JsonObject(
                mapOf(
                    "status" to JsonPrimitive("error"),
                    "tool" to JsonPrimitive(toolName),
                    "message" to JsonPrimitive(errMsg),
                    "summaryFa" to JsonPrimitive("خطا در اجرای $toolName: $errMsg")
                )
            )
        }
    }

    /**
     * Convenience overload for JSONObject arguments (used by local tool planner & unit tests).
     */
    fun executeToolCallFromOrgJson(
        toolName: String,
        orgJsonArgs: JSONObject,
        session: AgentTransactionSession
    ): JsonObject {
        val map = mutableMapOf<String, JsonElement>()
        val keys = orgJsonArgs.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = orgJsonArgs.opt(k)
            val elem: JsonElement = when (v) {
                null, JSONObject.NULL -> JsonNull
                is Boolean -> JsonPrimitive(v)
                is Number -> JsonPrimitive(v)
                is String -> JsonPrimitive(v)
                else -> JsonPrimitive(v.toString())
            }
            map[k] = elem
        }
        return executeToolCall(toolName, map, session)
    }

    /**
     * Convenience helper returning an org.json.JSONObject for direct ViewModel / unit test assertions.
     */
    fun executeToolCallAsOrgJson(
        toolName: String,
        orgJsonArgs: JSONObject,
        session: AgentTransactionSession
    ): JSONObject {
        val resultJson = executeToolCallFromOrgJson(toolName, orgJsonArgs, session)
        return JSONObject(resultJson.toString())
    }

    /**
     * Standalone validation helper for a StudioProject.
     */
    fun validateStudioDocument(project: StudioProject): AgentValidationReport {
        val tempSession = AgentTransactionSession(
            context = null,
            userCommand = "validate",
            preTransactionProject = project,
            preTransactionSelectedLayerId = project.layers.lastOrNull()?.id
        )
        execValidateDocument(mapOf("autoFixIssues" to JsonPrimitive(false)), tempSession)
        return tempSession.lastValidationReport ?: AgentValidationReport(
            isValid = true,
            scoreOutOf100 = 100,
            issuesFa = emptyList(),
            autoFixesFa = emptyList(),
            summaryFa = "سند معتبر است."
        )
    }

    fun toolTitleFa(toolName: String): String = when (toolName) {
        "inspect_document" -> "بررسی ساختار سند و بوم (inspect_document)"
        "inspect_layers" -> "واکاوی لیست لایه‌ها (inspect_layers)"
        "inspect_layer" -> "بررسی جزئیات لایه (inspect_layer)"
        "add_text_layer" -> "افزودن لایه متن جدید (add_text_layer)"
        "update_text_layer" -> "ویرایش محتوا و تایپوگرافی متن (update_text_layer)"
        "add_image_layer" -> "افزودن لایه تصویر/استیکر (add_image_layer)"
        "add_shape_layer" -> "افزودن لایه شکل برداری (add_shape_layer)"
        "transform_layer" -> "تنظیم موقعیت، ابعاد و چرخش (transform_layer)"
        "set_layer_style" -> "اعمال استایل، رنگ و افکت ۳بعدی (set_layer_style)"
        "set_background" -> "تنظیم پس‌زمینه و ابعاد بوم (set_background)"
        "reorder_layers" -> "تغییر ترتیب لایه‌ها (reorder_layers)"
        "align_layers" -> "ترازبندی و توزیع هوشمند عناصر (align_layers)"
        "duplicate_layer" -> "تکثیر لایه (duplicate_layer)"
        "delete_layer" -> "حذف لایه (delete_layer)"
        "group_layers" -> "گروه‌بندی و هماهنگ‌سازی لایه‌ها (group_layers)"
        "render_preview" -> "رندر پیش‌نمایش زنده بوم (render_preview)"
        "validate_document" -> "اعتبارسنجی نهایی ترکیب‌بندی و کنتراست (validate_document)"
        "undo_transaction" -> "لغو تراکنش و بازگشت به وضعیت اولیه (undo_transaction)"
        else -> toolName
    }

    // =========================================================================
    // 3. INDIVIDUAL TOOL IMPLEMENTATIONS
    // =========================================================================

    private fun execInspectDocument(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val includeLayers = args["includeLayerSummary"].boolOr(true)
        val gcd = gcd(p.canvasWidth, p.canvasHeight).coerceAtLeast(1)
        val ratioStr = "${p.canvasWidth / gcd}:${p.canvasHeight / gcd}"

        val textCount = p.layers.count { it is TextLayer }
        val shapeCount = p.layers.count { it is ShapeLayer }
        val imageCount = p.layers.count { it is ImageLayer }
        val drawingCount = p.layers.count { it is DrawingLayer }
        val lockedCount = p.layers.count { it.isLocked }
        val visibleCount = p.layers.count { it.isVisible }

        val layersSummary = if (includeLayers) {
            JsonArray(
                p.layers.mapIndexed { idx, l ->
                    JsonObject(
                        mapOf(
                            "index" to JsonPrimitive(idx),
                            "id" to JsonPrimitive(l.id),
                            "name" to JsonPrimitive(l.name),
                            "type" to JsonPrimitive(layerTypeString(l)),
                            "isSelected" to JsonPrimitive(l.id == session.workingSelectedLayerId),
                            "isVisible" to JsonPrimitive(l.isVisible),
                            "isLocked" to JsonPrimitive(l.isLocked),
                            "x" to JsonPrimitive(l.x),
                            "y" to JsonPrimitive(l.y)
                        )
                    )
                }
            )
        } else JsonArray(emptyList())

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "projectTitle" to JsonPrimitive(p.title),
                "canvasWidth" to JsonPrimitive(p.canvasWidth),
                "canvasHeight" to JsonPrimitive(p.canvasHeight),
                "aspectRatio" to JsonPrimitive(ratioStr),
                "backgroundType" to JsonPrimitive(p.background.type.name),
                "backgroundPrimaryHex" to JsonPrimitive(p.background.primaryColorHex),
                "backgroundSecondaryHex" to JsonPrimitive(p.background.secondaryColorHex),
                "hasBackgroundImage" to JsonPrimitive(p.background.backgroundBitmap != null),
                "layerCount" to JsonPrimitive(p.layers.size),
                "totalLayers" to JsonPrimitive(p.layers.size),
                "visibleLayers" to JsonPrimitive(visibleCount),
                "lockedLayers" to JsonPrimitive(lockedCount),
                "textLayerCount" to JsonPrimitive(textCount),
                "shapeLayerCount" to JsonPrimitive(shapeCount),
                "imageLayerCount" to JsonPrimitive(imageCount),
                "drawingLayerCount" to JsonPrimitive(drawingCount),
                "selectedLayerId" to JsonPrimitive(session.workingSelectedLayerId ?: "NONE"),
                "layers" to layersSummary,
                "summaryFa" to JsonPrimitive("سند بررسی شد: ابعاد ${p.canvasWidth}×${p.canvasHeight} پیکسل، ${p.layers.size} لایه ($textCount متن، $shapeCount شکل، $imageCount تصویر)")
            )
        )
    }

    private fun execInspectLayers(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val filterType = args["filterType"].strOr("ALL").uppercase()
        val onlyVisible = args["onlyVisible"].boolOr(false)

        val filtered = p.layers.filter { layer ->
            val matchesVis = !onlyVisible || layer.isVisible
            val matchesType = when (filterType) {
                "TEXT" -> layer is TextLayer
                "SHAPE" -> layer is ShapeLayer
                "IMAGE" -> layer is ImageLayer
                "DRAWING" -> layer is DrawingLayer
                else -> true
            }
            matchesVis && matchesType
        }

        val arr = JsonArray(
            filtered.map { l ->
                layerToJsonObject(l, p.layers.indexOf(l), l.id == session.workingSelectedLayerId)
            }
        )

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "count" to JsonPrimitive(filtered.size),
                "layers" to arr,
                "summaryFa" to JsonPrimitive("مشخصات ${filtered.size} لایه استخراج شد")
            )
        )
    }

    private fun execInspectLayer(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val target = resolveTargetLayer(session.workingProject, args["layerId"].strOr("SELECTED"), session.workingSelectedLayerId)
            ?: return JsonObject(
                mapOf(
                    "status" to JsonPrimitive("error"),
                    "message" to JsonPrimitive("Target layer not found"),
                    "summaryFa" to JsonPrimitive("لایه مورد نظر برای بررسی یافت نشد")
                )
            )
        val idx = session.workingProject.layers.indexOfFirst { it.id == target.id }
        val detail = layerToJsonObject(target, idx, target.id == session.workingSelectedLayerId)
        return JsonObject(
            detail + mapOf(
                "status" to JsonPrimitive("ok"),
                "summaryFa" to JsonPrimitive("جزئیات کامل لایه «${target.name}» (${layerTypeString(target)}) بررسی شد")
            )
        )
    }

    private fun execAddTextLayer(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val rawText = args["text"].strOr("متن هوشمند").ifBlank { "متن هوشمند" }
        val fontId = args["fontId"].strOr(StudioFont.LALEZAR.id).lowercase()
        val fontSize = args["fontSizeSp"].floatOr(52f).coerceIn(16f, 128f)
        val colorHex = args["colorHex"].hexOr("#FFFFFF")
        val secHex = args["secondaryColorHex"].hexOr("#38BDF8")
        val useGrad = args["useGradient"].boolOr(args.containsKey("secondaryColorHex"))
        val alignStr = args["textAlign"].strOr("CENTER").uppercase()
        val alignMode = runCatching { TextAlignMode.valueOf(alignStr) }.getOrDefault(TextAlignMode.CENTER)

        val defaultY = when (p.layers.filterIsInstance<TextLayer>().size) {
            0 -> -0.06f
            1 -> 0.14f
            2 -> 0.28f
            else -> ((p.layers.size % 5) - 2) * 0.10f
        }
        val x = args["x"].floatOr(0f).coerceIn(-0.46f, 0.46f)
        val y = args["y"].floatOr(defaultY).coerceIn(-0.46f, 0.46f)
        val scale = args["scale"].floatOr(1f).coerceIn(0.25f, 3.2f)
        val rotZ = args["rotationZ"].floatOr(0f).coerceIn(-180f, 180f)
        val is3d = args["is3dEnabled"].boolOr(true)
        val depth3d = args["depth3d"].intOr(18).coerceIn(1, 60)
        val strokeEn = args["strokeEnabled"].boolOr(true)
        val strokeHex = args["strokeColorHex"].hexOr("#0B132B")
        val strokeW = args["strokeWidth"].floatOr(6f).coerceIn(1f, 22f)
        val shadowEn = args["shadowEnabled"].boolOr(true)
        val pillEn = args["backgroundPillEnabled"].boolOr(false)
        val pillHex = args["backgroundPillColorHex"].hexOr("#AA0F172A")

        val newLayer = TextLayer(
            name = args["layerName"].strOr("متن: ${rawText.take(16)}"),
            text = rawText,
            fontId = fontId,
            fontSizeSp = fontSize,
            colorHex = colorHex,
            useGradientFill = useGrad,
            gradientSecondColorHex = secHex,
            textAlignMode = alignMode,
            textAlignCenter = alignMode == TextAlignMode.CENTER,
            x = x,
            y = y,
            scaleX = scale,
            scaleY = scale,
            rotationZ = rotZ,
            is3dEnabled = is3d,
            depth3d = depth3d,
            stroke = LayerStroke(enabled = strokeEn, colorHex = strokeHex, width = strokeW),
            shadow = LayerShadow(enabled = shadowEn, colorHex = "#CC000000", blurRadius = 18f, offsetY = 8f),
            backgroundPillEnabled = pillEn,
            backgroundPillColorHex = pillHex
        )

        session.workingProject = p.copy(layers = p.layers + newLayer)
        session.workingSelectedLayerId = newLayer.id

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "layerId" to JsonPrimitive(newLayer.id),
                "text" to JsonPrimitive(newLayer.text),
                "x" to JsonPrimitive(newLayer.x),
                "y" to JsonPrimitive(newLayer.y),
                "summaryFa" to JsonPrimitive("لایه متن «${rawText.take(22)}» با فونت $fontId اضافه شد")
            )
        )
    }

    private fun execUpdateTextLayer(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val target = (resolveTargetLayer(p, args["layerId"].strOr("SELECTED"), session.workingSelectedLayerId) as? TextLayer)
            ?: p.layers.filterIsInstance<TextLayer>().find { it.id == session.workingSelectedLayerId }
            ?: p.layers.filterIsInstance<TextLayer>().firstOrNull()
            ?: return JsonObject(
                mapOf(
                    "status" to JsonPrimitive("error"),
                    "message" to JsonPrimitive("No text layer found to update"),
                    "summaryFa" to JsonPrimitive("هیچ لایه متنی برای ویرایش یافت نشد")
                )
            )

        // Protect existing text from being overwritten by a design instruction unless explicit
        val rawCandidateText = args["text"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val forceUpdate = args["forceUpdateText"].boolOr(false)
        val normCmd = OfflineDesignEngine.normalizePersianCommand(session.userCommand)
        val normCandidate = OfflineDesignEngine.normalizePersianCommand(rawCandidateText)
        val isExplicitTextChange = forceUpdate ||
            normCmd.contains("بنویس") ||
            normCmd.contains("تغییر") ||
            normCmd.contains("متن") ||
            normCmd.contains("تیتر") ||
            session.userCommand.contains(":") ||
            session.userCommand.contains("«") ||
            session.userCommand.isBlank()
        val shouldKeepOriginalText = rawCandidateText.isBlank() ||
            (!isExplicitTextChange && (normCandidate == normCmd || OfflineDesignEngine.isLikelyDesignInstruction(normCandidate)))

        val finalText = if (shouldKeepOriginalText) target.text else rawCandidateText
        val fontId = args["fontId"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: target.fontId
        val fontSize = args["fontSizeSp"]?.floatOrNull()?.coerceIn(16f, 128f) ?: target.fontSizeSp
        val colorHex = args["colorHex"]?.hexOrNull() ?: target.colorHex
        val secHex = args["secondaryColorHex"]?.hexOrNull() ?: target.gradientSecondColorHex
        val useGrad = args["useGradient"]?.boolOrNull()
            ?: (args.containsKey("secondaryColorHex") || target.useGradientFill)
        val alignMode = args["textAlign"]?.jsonPrimitive?.contentOrNull?.uppercase()?.let {
            runCatching { TextAlignMode.valueOf(it) }.getOrNull()
        } ?: target.textAlignMode
        val letterSp = args["letterSpacing"]?.floatOrNull()?.coerceIn(-0.1f, 0.5f) ?: target.letterSpacing
        val lineSp = args["lineSpacing"]?.floatOrNull()?.coerceIn(0.8f, 2.4f) ?: target.lineSpacing
        val curve = args["curveAngle"]?.floatOrNull()?.coerceIn(-360f, 360f) ?: target.curveAngle
        val isBold = args["isBold"]?.boolOrNull() ?: target.isBold
        val isItalic = args["isItalic"]?.boolOrNull() ?: target.isItalic
        val isUnderline = args["isUnderline"]?.boolOrNull() ?: target.isUnderline
        val pillEn = args["backgroundPillEnabled"]?.boolOrNull() ?: target.backgroundPillEnabled
        val pillHex = args["backgroundPillColorHex"]?.hexOrNull() ?: target.backgroundPillColorHex

        val updated = target.copy(
            text = finalText,
            fontId = fontId,
            fontSizeSp = fontSize,
            colorHex = colorHex,
            useGradientFill = useGrad,
            gradientSecondColorHex = secHex,
            textAlignMode = alignMode,
            textAlignCenter = alignMode == TextAlignMode.CENTER,
            letterSpacing = letterSp,
            lineSpacing = lineSp,
            curveAngle = curve,
            isBold = isBold,
            isItalic = isItalic,
            isUnderline = isUnderline,
            backgroundPillEnabled = pillEn,
            backgroundPillColorHex = pillHex
        )

        session.workingProject = p.copy(
            layers = p.layers.map { if (it.id == target.id) updated else it }
        )
        session.workingSelectedLayerId = updated.id

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "layerId" to JsonPrimitive(updated.id),
                "text" to JsonPrimitive(updated.text),
                "fontId" to JsonPrimitive(updated.fontId),
                "fontSizeSp" to JsonPrimitive(updated.fontSizeSp),
                "textAlign" to JsonPrimitive(updated.textAlignMode.name),
                "summaryFa" to JsonPrimitive("لایه متن «${updated.text.take(20)}» به‌روزرسانی شد")
            )
        )
    }

    private fun execAddImageLayer(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val stickerId = args["stickerId"].strOr("crown_gold").lowercase()
        val found = BUILTIN_PIXELLAB_STICKERS.find { it.id == stickerId }
            ?: BUILTIN_PIXELLAB_STICKERS.first()
        val pHex = args["primaryColorHex"].hexOr(found.primaryHex)
        val sHex = args["secondaryColorHex"].hexOr(found.secondaryHex)
        val x = args["x"].floatOr(0f).coerceIn(-0.45f, 0.45f)
        val y = args["y"].floatOr(-0.26f).coerceIn(-0.45f, 0.45f)
        val scale = args["scale"].floatOr(0.85f).coerceIn(0.25f, 2.5f)

        val bmp = StudioCanvasRenderer.renderBuiltinStickerToBitmap(
            symbolText = found.symbolText,
            primaryHex = pHex,
            secondaryHex = sHex
        )

        val imgLayer = ImageLayer(
            name = args["layerName"].strOr("استیکر: ${found.labelFa}"),
            bitmap = bmp,
            originalBitmap = bmp,
            x = x,
            y = y,
            scaleX = scale,
            scaleY = scale
        )

        session.workingProject = p.copy(layers = p.layers + imgLayer)
        session.workingSelectedLayerId = imgLayer.id

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "layerId" to JsonPrimitive(imgLayer.id),
                "name" to JsonPrimitive(imgLayer.name),
                "summaryFa" to JsonPrimitive("لایه گرافیکی «${imgLayer.name}» به بوم اضافه شد")
            )
        )
    }

    private fun execAddShapeLayer(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val kindStr = args["shapeKind"].strOr("HEXAGON").uppercase()
        val kind = runCatching { ShapeKind.valueOf(kindStr) }.getOrDefault(ShapeKind.HEXAGON)
        val fillHex = args["fillColorHex"].hexOr("#38BDF8")
        val secHex = args["secondaryColorHex"].hexOr("#A855F7")
        val useGrad = args["useGradient"].boolOr(true)
        val radius = args["cornerRadius"].floatOr(24f).coerceIn(0f, 120f)
        val x = args["x"].floatOr(0f).coerceIn(-0.46f, 0.46f)
        val y = args["y"].floatOr(0f).coerceIn(-0.46f, 0.46f)
        val sx = args["scaleX"].floatOr(1f).coerceIn(0.2f, 3.5f)
        val sy = args["scaleY"].floatOr(sx).coerceIn(0.2f, 3.5f)
        val rotZ = args["rotationZ"].floatOr(0f).coerceIn(-180f, 180f)
        val op = args["opacity"].floatOr(1f).coerceIn(0.1f, 1f)
        val is3d = args["is3dExtruded"].boolOr(false)
        val depth = args["depth3d"].intOr(12).coerceIn(2, 45)

        val shapeLayer = ShapeLayer(
            name = args["layerName"].strOr(kind.labelFa),
            shapeKind = kind,
            fillColorHex = fillHex,
            useGradientFill = useGrad,
            gradientSecondColorHex = secHex,
            cornerRadius = radius,
            x = x,
            y = y,
            scaleX = sx,
            scaleY = sy,
            rotationZ = rotZ,
            opacity = op,
            is3dExtruded = is3d,
            depth3d = depth
        )

        session.workingProject = p.copy(layers = p.layers + shapeLayer)
        session.workingSelectedLayerId = shapeLayer.id

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "layerId" to JsonPrimitive(shapeLayer.id),
                "shapeKind" to JsonPrimitive(kind.name),
                "summaryFa" to JsonPrimitive("لایه شکل «${shapeLayer.name}» اضافه شد")
            )
        )
    }

    private fun execTransformLayer(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val target = resolveTargetLayer(p, args["layerId"].strOr("SELECTED"), session.workingSelectedLayerId)
            ?: return JsonObject(
                mapOf(
                    "status" to JsonPrimitive("error"),
                    "message" to JsonPrimitive("Layer not found for transform"),
                    "summaryFa" to JsonPrimitive("لایه مورد نظر برای تغییر موقعیت/ابعاد یافت نشد")
                )
            )

        val nx = args["x"]?.floatOrNull()?.coerceIn(-0.48f, 0.48f) ?: target.x
        val ny = args["y"]?.floatOrNull()?.coerceIn(-0.48f, 0.48f) ?: target.y
        val sx = args["scaleX"]?.floatOrNull()?.coerceIn(0.2f, 3.5f) ?: target.scaleX
        val sy = args["scaleY"]?.floatOrNull()?.coerceIn(0.2f, 3.5f) ?: args["scaleX"]?.floatOrNull()?.coerceIn(0.2f, 3.5f) ?: target.scaleY
        val rx = args["rotationX"]?.floatOrNull()?.coerceIn(-65f, 65f) ?: target.rotationX
        val ry = args["rotationY"]?.floatOrNull()?.coerceIn(-65f, 65f) ?: target.rotationY
        val rz = args["rotationZ"]?.floatOrNull()?.coerceIn(-180f, 180f) ?: target.rotationZ
        val skx = args["skewX"]?.floatOrNull()?.coerceIn(-0.75f, 0.75f) ?: target.skewX
        val sky = args["skewY"]?.floatOrNull()?.coerceIn(-0.75f, 0.75f) ?: target.skewY
        val op = args["opacity"]?.floatOrNull()?.coerceIn(0.05f, 1f) ?: target.opacity
        val vis = args["isVisible"]?.boolOrNull() ?: target.isVisible
        val lck = args["isLocked"]?.boolOrNull() ?: target.isLocked

        val updated: StudioLayer = when (target) {
            is TextLayer -> target.copy(
                x = nx, y = ny, scaleX = sx, scaleY = sy,
                rotationX = rx, rotationY = ry, rotationZ = rz,
                skewX = skx, skewY = sky, opacity = op,
                isVisible = vis, isLocked = lck
            )
            is ShapeLayer -> target.copy(
                x = nx, y = ny, scaleX = sx, scaleY = sy,
                rotationX = rx, rotationY = ry, rotationZ = rz,
                skewX = skx, skewY = sky, opacity = op,
                isVisible = vis, isLocked = lck
            )
            is ImageLayer -> {
                val fh = args["flipHorizontal"]?.boolOrNull() ?: target.flipHorizontal
                val fv = args["flipVertical"]?.boolOrNull() ?: target.flipVertical
                // Never hide a protected product image unless explicitly requested
                val safeVis = if (target.id in session.protectedProductImageIds && !session.allowProductImageModification) {
                    true
                } else vis
                target.copy(
                    x = nx, y = ny, scaleX = sx, scaleY = sy,
                    rotationX = rx, rotationY = ry, rotationZ = rz,
                    skewX = skx, skewY = sky, opacity = op,
                    flipHorizontal = fh, flipVertical = fv,
                    isVisible = safeVis, isLocked = lck
                )
            }
            is DrawingLayer -> target.copy(
                x = nx, y = ny, scaleX = sx, scaleY = sy,
                rotationX = rx, rotationY = ry, rotationZ = rz,
                skewX = skx, skewY = sky, opacity = op,
                isVisible = vis, isLocked = lck
            )
        }

        session.workingProject = p.copy(
            layers = p.layers.map { if (it.id == target.id) updated else it }
        )
        session.workingSelectedLayerId = updated.id

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "layerId" to JsonPrimitive(updated.id),
                "x" to JsonPrimitive(updated.x),
                "y" to JsonPrimitive(updated.y),
                "scaleX" to JsonPrimitive(updated.scaleX),
                "rotationZ" to JsonPrimitive(updated.rotationZ),
                "summaryFa" to JsonPrimitive("تبدیل هندسی لایه «${updated.name}» اعمال شد (x=${"%.2f".format(updated.x)}, y=${"%.2f".format(updated.y)})")
            )
        )
    }

    private fun execSetLayerStyle(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val target = resolveTargetLayer(p, args["layerId"].strOr("SELECTED"), session.workingSelectedLayerId)
            ?: return JsonObject(
                mapOf(
                    "status" to JsonPrimitive("error"),
                    "message" to JsonPrimitive("Target layer not found for set_layer_style"),
                    "summaryFa" to JsonPrimitive("لایه مورد نظر برای اعمال استایل یافت نشد")
                )
            )

        val pHex = args["primaryColorHex"]?.hexOrNull()
        val sHex = args["secondaryColorHex"]?.hexOrNull()
        val useGrad = args["useGradient"]?.boolOrNull()

        val strokeEn = args["strokeEnabled"]?.boolOrNull() ?: target.stroke.enabled
        val strokeHex = args["strokeColorHex"]?.hexOrNull() ?: target.stroke.colorHex
        val strokeW = args["strokeWidth"]?.floatOrNull()?.coerceIn(1f, 24f) ?: target.stroke.width
        val newStroke = target.stroke.copy(enabled = strokeEn, colorHex = strokeHex, width = strokeW)

        val shadowEn = args["shadowEnabled"]?.boolOrNull() ?: target.shadow.enabled
        val shadowHex = args["shadowColorHex"]?.hexOrNull() ?: target.shadow.colorHex
        val shadowBlur = args["shadowBlur"]?.floatOrNull()?.coerceIn(1f, 40f) ?: target.shadow.blurRadius
        val shadowOx = args["shadowOffsetX"]?.floatOrNull()?.coerceIn(-30f, 30f) ?: target.shadow.offsetX
        val shadowOy = args["shadowOffsetY"]?.floatOrNull()?.coerceIn(-30f, 30f) ?: target.shadow.offsetY
        val newShadow = target.shadow.copy(
            enabled = shadowEn,
            colorHex = shadowHex,
            blurRadius = shadowBlur,
            offsetX = shadowOx,
            offsetY = shadowOy
        )

        val innerEn = args["innerShadowEnabled"]?.boolOrNull() ?: target.innerShadow.enabled
        val newInner = target.innerShadow.copy(enabled = innerEn)

        val embossEn = args["embossEnabled"]?.boolOrNull() ?: target.emboss.enabled
        val newEmboss = target.emboss.copy(enabled = embossEn)

        val blend = args["blendMode"]?.jsonPrimitive?.contentOrNull?.uppercase()?.let {
            runCatching { LayerBlendMode.valueOf(it) }.getOrNull()
        } ?: target.blendMode

        val texPreset = args["texturePreset"]?.jsonPrimitive?.contentOrNull?.uppercase()?.let {
            runCatching { TexturePreset.valueOf(it) }.getOrNull()
        }

        val rx = args["rotationX"]?.floatOrNull()?.coerceIn(-75f, 75f)
        val ry = args["rotationY"]?.floatOrNull()?.coerceIn(-75f, 75f)
        val rz = args["rotationZ"]?.floatOrNull()?.coerceIn(-180f, 180f)

        val updated: StudioLayer = when (target) {
            is TextLayer -> {
                val is3d = args["is3dEnabled"]?.boolOrNull()
                    ?: (if (args.containsKey("depth3d")) (args["depth3d"]?.intOrNull() ?: 0) > 0 else target.is3dEnabled)
                val depth = args["depth3d"]?.intOrNull()?.coerceIn(1, 60) ?: target.depth3d
                val obAngle = args["obliqueAngle3d"]?.floatOrNull()?.coerceIn(0f, 360f) ?: target.obliqueAngle3d
                val lAngle = (args["lightAngle3d"] ?: args["lightAngle"])?.floatOrNull()?.coerceIn(0f, 360f) ?: target.lightAngle3d
                val simLight = (args["simulateLighting3d"] ?: args["simulateLighting"])?.boolOrNull() ?: target.simulateLighting3d
                val lInt = (args["lightIntensity3d"] ?: args["lightIntensity"])?.floatOrNull()?.coerceIn(10f, 100f) ?: target.lightIntensity3d
                val ambLight = (args["ambientLight3d"] ?: args["ambientLight"])?.floatOrNull()?.coerceIn(10f, 100f) ?: target.ambientLight3d
                val specHard = (args["specularHardness3d"] ?: args["specularHardness"])?.floatOrNull()?.coerceIn(10f, 100f) ?: target.specularHardness3d
                val shadow3d = args["shadow3dEnabled"]?.boolOrNull() ?: target.shadow3dEnabled
                val refl = args["reflectionEnabled"]?.boolOrNull() ?: target.reflectionEnabled
                val secStrokeEn = args["secondStrokeEnabled"]?.boolOrNull() ?: target.secondStrokeEnabled
                val secStrokeHex = args["secondStrokeColorHex"]?.hexOrNull() ?: target.secondStrokeColorHex
                val secStrokeW = args["secondStrokeWidth"]?.floatOrNull()?.coerceIn(2f, 26f) ?: target.secondStrokeWidth

                target.copy(
                    rotationX = rx ?: target.rotationX,
                    rotationY = ry ?: target.rotationY,
                    rotationZ = rz ?: target.rotationZ,
                    colorHex = pHex ?: target.colorHex,
                    gradientSecondColorHex = sHex ?: target.gradientSecondColorHex,
                    useGradientFill = useGrad ?: (sHex != null || target.useGradientFill),
                    stroke = newStroke,
                    secondStrokeEnabled = secStrokeEn,
                    secondStrokeColorHex = secStrokeHex,
                    secondStrokeWidth = secStrokeW,
                    shadow = newShadow,
                    innerShadow = newInner,
                    emboss = newEmboss,
                    is3dEnabled = is3d,
                    depth3d = depth,
                    obliqueAngle3d = obAngle,
                    simulateLighting3d = simLight,
                    lightAngle3d = lAngle,
                    lightIntensity3d = lInt,
                    ambientLight3d = ambLight,
                    specularHardness3d = specHard,
                    shadow3dEnabled = shadow3d,
                    reflectionEnabled = refl,
                    texturePreset = texPreset ?: target.texturePreset,
                    blendMode = blend
                )
            }
            is ShapeLayer -> {
                val is3d = args["is3dEnabled"]?.boolOrNull() ?: target.is3dExtruded
                val depth = args["depth3d"]?.intOrNull()?.coerceIn(2, 45) ?: target.depth3d
                val obAngle = args["obliqueAngle3d"]?.floatOrNull()?.coerceIn(0f, 360f) ?: target.obliqueAngle3d
                target.copy(
                    rotationX = rx ?: target.rotationX,
                    rotationY = ry ?: target.rotationY,
                    rotationZ = rz ?: target.rotationZ,
                    fillColorHex = pHex ?: target.fillColorHex,
                    gradientSecondColorHex = sHex ?: target.gradientSecondColorHex,
                    useGradientFill = useGrad ?: (sHex != null || target.useGradientFill),
                    stroke = newStroke,
                    shadow = newShadow,
                    innerShadow = newInner,
                    emboss = newEmboss,
                    is3dExtruded = is3d,
                    depth3d = depth,
                    obliqueAngle3d = obAngle,
                    texturePreset = texPreset ?: target.texturePreset,
                    blendMode = blend
                )
            }
            is ImageLayer -> {
                // Preserve main product image content! Only apply non-destructive border/shadow/3D/tint if requested
                val isProtectedProduct = target.id in session.protectedProductImageIds && !session.allowProductImageModification
                val is3d = args["is3dEnabled"]?.boolOrNull() ?: target.is3dShapeEnabled
                val depth = args["depth3d"]?.intOrNull()?.coerceIn(2, 40) ?: target.depth3d
                target.copy(
                    rotationX = rx ?: target.rotationX,
                    rotationY = ry ?: target.rotationY,
                    rotationZ = rz ?: target.rotationZ,
                    stroke = newStroke,
                    shadow = newShadow,
                    innerShadow = newInner,
                    emboss = newEmboss,
                    is3dShapeEnabled = is3d,
                    depth3d = depth,
                    // Do not tint over product photos unless explicitly allowed
                    colorOverlayEnabled = if (isProtectedProduct) false else (pHex != null || target.colorOverlayEnabled),
                    colorOverlayHex = pHex ?: target.colorOverlayHex,
                    blendMode = blend
                )
            }
            is DrawingLayer -> target.copy(
                rotationX = rx ?: target.rotationX,
                rotationY = ry ?: target.rotationY,
                rotationZ = rz ?: target.rotationZ,
                stroke = newStroke,
                shadow = newShadow,
                innerShadow = newInner,
                emboss = newEmboss,
                blendMode = blend
            )
        }

        session.workingProject = p.copy(
            layers = p.layers.map { if (it.id == target.id) updated else it }
        )
        session.workingSelectedLayerId = updated.id

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "layerId" to JsonPrimitive(updated.id),
                "summaryFa" to JsonPrimitive("استایل لایه «${updated.name}» ارتقا یافت")
            )
        )
    }

    private fun execSetBackground(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val bgTypeStr = args["backgroundType"]?.jsonPrimitive?.contentOrNull?.uppercase()
        val bgType = bgTypeStr?.let { runCatching { BackgroundType.valueOf(it) }.getOrNull() }
            ?: BackgroundType.GRADIENT
        val pHex = args["primaryColorHex"]?.hexOrNull() ?: p.background.primaryColorHex
        val sHex = args["secondaryColorHex"]?.hexOrNull() ?: p.background.secondaryColorHex
        val angle = args["gradientAngle"]?.floatOrNull()?.coerceIn(0f, 360f) ?: p.background.gradientAngle
        val radial = args["isRadialGradient"]?.boolOrNull() ?: p.background.isRadialGradient

        var newWidth = p.canvasWidth
        var newHeight = p.canvasHeight
        val presetRatio = args["aspectRatioPreset"]?.jsonPrimitive?.contentOrNull
        if (!presetRatio.isNullOrBlank()) {
            val found = DEFAULT_ASPECT_PRESETS.find { it.ratioLabel == presetRatio.trim() }
            if (found != null) {
                newWidth = found.width
                newHeight = found.height
            }
        }
        args["canvasWidth"]?.intOrNull()?.let { newWidth = it.coerceIn(128, 4096) }
        args["canvasHeight"]?.intOrNull()?.let { newHeight = it.coerceIn(128, 4096) }

        val vig = args["vignetteEnabled"]?.boolOrNull() ?: p.postFx.vignetteEnabled
        val noise = args["noiseEnabled"]?.boolOrNull() ?: p.postFx.noiseEnabled

        session.workingProject = p.copy(
            canvasWidth = newWidth,
            canvasHeight = newHeight,
            background = p.background.copy(
                type = bgType,
                primaryColorHex = pHex,
                secondaryColorHex = sHex,
                gradientAngle = angle,
                isRadialGradient = radial,
                backgroundBitmap = if (bgType == BackgroundType.IMAGE) p.background.backgroundBitmap else null
            ),
            postFx = p.postFx.copy(
                vignetteEnabled = vig,
                noiseEnabled = noise
            )
        )

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "canvasWidth" to JsonPrimitive(newWidth),
                "canvasHeight" to JsonPrimitive(newHeight),
                "backgroundType" to JsonPrimitive(bgType.name),
                "primaryColorHex" to JsonPrimitive(pHex),
                "secondaryColorHex" to JsonPrimitive(sHex),
                "summaryFa" to JsonPrimitive("پس‌زمینه بوم ($pHex → $sHex) و ابعاد ${newWidth}×${newHeight} تنظیم شد")
            )
        )
    }

    private fun execReorderLayers(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val target = resolveTargetLayer(p, args["layerId"].strOr("SELECTED"), session.workingSelectedLayerId)
            ?: return JsonObject(
                mapOf(
                    "status" to JsonPrimitive("error"),
                    "message" to JsonPrimitive("Layer not found for reorder"),
                    "summaryFa" to JsonPrimitive("لایه مورد نظر برای تغییر ترتیب یافت نشد")
                )
            )

        val list = p.layers.toMutableList()
        val oldIdx = list.indexOfFirst { it.id == target.id }
        if (oldIdx == -1) {
            return JsonObject(mapOf("status" to JsonPrimitive("error"), "summaryFa" to JsonPrimitive("لایه یافت نشد")))
        }

        val item = list.removeAt(oldIdx)
        val dir = args["direction"].strOr("TO_FRONT").uppercase()
        when (dir) {
            "TO_BACK" -> list.add(0, item)
            "MOVE_UP" -> list.add((oldIdx + 1).coerceAtMost(list.size), item)
            "MOVE_DOWN" -> list.add((oldIdx - 1).coerceAtLeast(0), item)
            "TO_INDEX" -> {
                val idx = args["targetIndex"].intOr(list.size).coerceIn(0, list.size)
                list.add(idx, item)
            }
            else -> list.add(item) // TO_FRONT
        }

        val newIdx = list.indexOfFirst { it.id == item.id }
        session.workingProject = p.copy(layers = list)
        session.workingSelectedLayerId = item.id

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "layerId" to JsonPrimitive(item.id),
                "oldIndex" to JsonPrimitive(oldIdx),
                "newIndex" to JsonPrimitive(newIdx),
                "summaryFa" to JsonPrimitive("ترتیب لایه «${item.name}» از جایگاه $oldIdx به $newIdx تغییر یافت")
            )
        )
    }

    private fun execAlignLayers(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val rawIds = (args["layerIds"] ?: args["layerId"] ?: args["targetLayerIds"]).strOr("ALL").trim()
        val mode = (args["alignmentMode"] ?: args["alignment"]).strOr("CENTER_HORIZONTAL").uppercase()

        val targetIds: Set<String> = when {
            rawIds.equals("ALL", ignoreCase = true) -> p.layers.filter { it.isVisible && !it.isLocked }.map { it.id }.toSet()
            rawIds.equals("TEXT", ignoreCase = true) -> p.layers.filterIsInstance<TextLayer>().map { it.id }.toSet()
            rawIds.equals("SELECTED", ignoreCase = true) -> setOfNotNull(session.workingSelectedLayerId ?: p.layers.lastOrNull()?.id)
            else -> rawIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { token ->
                resolveTargetLayer(p, token, session.workingSelectedLayerId)?.id
            }.toSet()
        }.ifEmpty { p.layers.map { it.id }.toSet() }

        val targetList = p.layers.filter { it.id in targetIds }
        if (targetList.isEmpty()) {
            return JsonObject(mapOf("status" to JsonPrimitive("error"), "summaryFa" to JsonPrimitive("لایه‌ای برای ترازبندی یافت نشد")))
        }

        val updatedLayers = when (mode) {
            "DISTRIBUTE_VERTICAL", "GOLDEN_RATIO_STACK" -> {
                // Sort shapes/images behind, then texts by fontSize descending (headline on top, subtitle below)
                val textTargets = targetList.filterIsInstance<TextLayer>().sortedByDescending { it.fontSizeSp }
                val nonTextTargets = targetList.filter { it !is TextLayer }
                val totalText = textTargets.size
                val textYMap = mutableMapOf<String, Float>()
                if (totalText == 1) {
                    textYMap[textTargets[0].id] = 0.0f
                } else if (totalText >= 2) {
                    val startY = -0.18f
                    val stepY = (0.38f / (totalText - 1).coerceAtLeast(1)).coerceIn(0.12f, 0.24f)
                    textTargets.forEachIndexed { idx, tl ->
                        textYMap[tl.id] = (startY + idx * stepY).coerceIn(-0.40f, 0.40f)
                    }
                }
                p.layers.map { layer ->
                    if (layer.id !in targetIds) layer
                    else if (layer is TextLayer && layer.id in textYMap) {
                        layer.copy(x = 0f, y = textYMap[layer.id] ?: layer.y)
                    } else if (nonTextTargets.any { it.id == layer.id }) {
                        updateLayerCoords(layer, x = 0f, y = layer.y.coerceIn(-0.35f, 0.35f))
                    } else layer
                }
            }
            "DISTRIBUTE_HORIZONTAL" -> {
                val count = targetList.size
                val posMap = targetList.mapIndexed { idx, l ->
                    val nx = if (count <= 1) 0f else (-0.32f + idx * (0.64f / (count - 1)))
                    l.id to nx
                }.toMap()
                p.layers.map { l ->
                    if (l.id in posMap) updateLayerCoords(l, x = posMap[l.id] ?: l.x, y = l.y) else l
                }
            }
            else -> {
                p.layers.map { l ->
                    if (l.id !in targetIds) l
                    else when (mode) {
                        "CENTER_HORIZONTAL" -> updateLayerCoords(l, x = 0f, y = l.y)
                        "CENTER_VERTICAL" -> updateLayerCoords(l, x = l.x, y = 0f)
                        "CENTER_BOTH", "CENTER" -> updateLayerCoords(l, x = 0f, y = 0f)
                        "ALIGN_LEFT" -> updateLayerCoords(l, x = -0.28f, y = l.y)
                        "ALIGN_RIGHT" -> updateLayerCoords(l, x = 0.28f, y = l.y)
                        "ALIGN_TOP" -> updateLayerCoords(l, x = l.x, y = -0.32f)
                        "ALIGN_BOTTOM" -> updateLayerCoords(l, x = l.x, y = 0.32f)
                        "TOP_LEFT" -> updateLayerCoords(l, x = -0.28f, y = -0.32f)
                        "TOP_CENTER" -> updateLayerCoords(l, x = 0f, y = -0.32f)
                        "TOP_RIGHT" -> updateLayerCoords(l, x = 0.28f, y = -0.32f)
                        "CENTER_LEFT" -> updateLayerCoords(l, x = -0.28f, y = 0f)
                        "CENTER_RIGHT" -> updateLayerCoords(l, x = 0.28f, y = 0f)
                        "BOTTOM_LEFT" -> updateLayerCoords(l, x = -0.28f, y = 0.32f)
                        "BOTTOM_CENTER" -> updateLayerCoords(l, x = 0f, y = 0.32f)
                        "BOTTOM_RIGHT" -> updateLayerCoords(l, x = 0.28f, y = 0.32f)
                        else -> updateLayerCoords(l, x = 0f, y = l.y)
                    }
                }
            }
        }

        session.workingProject = p.copy(layers = updatedLayers)

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "alignedCount" to JsonPrimitive(targetList.size),
                "alignmentMode" to JsonPrimitive(mode),
                "summaryFa" to JsonPrimitive("ترازبندی و چیدمان ${targetList.size} لایه با حالت $mode انجام شد")
            )
        )
    }

    private fun execDuplicateLayer(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val target = resolveTargetLayer(p, args["layerId"].strOr("SELECTED"), session.workingSelectedLayerId)
            ?: return JsonObject(
                mapOf(
                    "status" to JsonPrimitive("error"),
                    "message" to JsonPrimitive("Layer not found to duplicate"),
                    "summaryFa" to JsonPrimitive("لایه مورد نظر برای کپی یافت نشد")
                )
            )

        val ox = args["offsetX"].floatOr(0.04f).coerceIn(-0.3f, 0.3f)
        val oy = args["offsetY"].floatOr(0.04f).coerceIn(-0.3f, 0.3f)
        val newId = UUID.randomUUID().toString()
        val copyName = args["newName"].strOr("${target.name} (کپی)")
        val nx = (target.x + ox).coerceIn(-0.46f, 0.46f)
        val ny = (target.y + oy).coerceIn(-0.46f, 0.46f)

        val duplicated: StudioLayer = when (target) {
            is TextLayer -> target.copy(id = newId, name = copyName, x = nx, y = ny)
            is ShapeLayer -> target.copy(id = newId, name = copyName, x = nx, y = ny)
            is ImageLayer -> target.copy(id = newId, name = copyName, x = nx, y = ny)
            is DrawingLayer -> target.copy(id = newId, name = copyName, x = nx, y = ny)
        }

        session.workingProject = p.copy(layers = p.layers + duplicated)
        session.workingSelectedLayerId = newId

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "originalLayerId" to JsonPrimitive(target.id),
                "duplicatedLayerId" to JsonPrimitive(newId),
                "summaryFa" to JsonPrimitive("لایه «${target.name}» کپی شد")
            )
        )
    }

    private fun execDeleteLayer(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val target = resolveTargetLayer(p, args["layerId"].strOr("SELECTED"), session.workingSelectedLayerId)
            ?: return JsonObject(
                mapOf(
                    "status" to JsonPrimitive("error"),
                    "message" to JsonPrimitive("Layer not found to delete"),
                    "summaryFa" to JsonPrimitive("لایه مورد نظر برای حذف یافت نشد")
                )
            )

        val allowImgDel = (args["allowDeleteImage"] ?: args["confirmModifyProductImage"]).boolOr(false) || session.allowProductImageModification
        if (target is ImageLayer && target.id in session.protectedProductImageIds && !allowImgDel) {
            return JsonObject(
                mapOf(
                    "status" to JsonPrimitive("blocked"),
                    "protectionStatus" to JsonPrimitive("protected"),
                    "layerId" to JsonPrimitive(target.id),
                    "message" to JsonPrimitive("Product image layer is protected from automatic deletion unless explicitly requested by the user."),
                    "summaryFa" to JsonPrimitive("لایه تصویر محصول «${target.name}» بدون درخواست مستقیم کاربر تغییر یا حذف نشد")
                )
            )
        }

        val remaining = p.layers.filterNot { it.id == target.id }
        session.workingProject = p.copy(layers = remaining)
        if (session.workingSelectedLayerId == target.id) {
            session.workingSelectedLayerId = remaining.lastOrNull()?.id
        }

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "deletedLayerId" to JsonPrimitive(target.id),
                "remainingLayerCount" to JsonPrimitive(remaining.size),
                "summaryFa" to JsonPrimitive("لایه «${target.name}» حذف شد")
            )
        )
    }

    private fun execGroupLayers(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val rawIds = (args["layerIds"] ?: args["layerIdsCsv"]).strOr("VISIBLE").trim()
        val action = args["groupAction"].strOr("ALIGN_GROUP_CENTER").uppercase()

        val groupTargets = when {
            rawIds.equals("VISIBLE", ignoreCase = true) -> p.layers.filter { it.isVisible }
            rawIds.equals("TEXT_AND_SHAPES", ignoreCase = true) -> p.layers.filter { it is TextLayer || it is ShapeLayer }
            else -> rawIds.split(",").mapNotNull { token ->
                resolveTargetLayer(p, token.trim(), session.workingSelectedLayerId)
            }
        }.ifEmpty { p.layers.filter { it.isVisible } }

        if (groupTargets.isEmpty()) {
            return JsonObject(mapOf("status" to JsonPrimitive("error"), "summaryFa" to JsonPrimitive("لایه‌ای برای گروه‌بندی یافت نشد")))
        }

        val groupIds = groupTargets.map { it.id }.toSet()

        if (action == "MERGE_TO_IMAGE_LAYER" && session.context != null && groupTargets.size >= 2) {
            val tempProject = p.copy(
                background = CanvasBackground(type = BackgroundType.TRANSPARENT),
                postFx = CanvasPostFx(vignetteEnabled = false, noiseEnabled = false, stripesEnabled = false),
                layers = groupTargets
            )
            val mergedBitmap = StudioCanvasRenderer.renderProjectToBitmap(
                context = session.context,
                project = tempProject,
                scaleMultiplier = 1f,
                includeCheckerboardForTransparent = false
            )
            val groupName = args["groupName"].strOr("گروه ادغام‌شده (${groupTargets.size} لایه)")
            val mergedLayer = ImageLayer(
                name = groupName,
                bitmap = mergedBitmap,
                originalBitmap = mergedBitmap
            )
            val remaining = p.layers.filterNot { it.id in groupIds }
            session.workingProject = p.copy(layers = remaining + mergedLayer)
            session.workingSelectedLayerId = mergedLayer.id
            return JsonObject(
                mapOf(
                    "status" to JsonPrimitive("ok"),
                    "mergedLayerId" to JsonPrimitive(mergedLayer.id),
                    "mergedCount" to JsonPrimitive(groupTargets.size),
                    "summaryFa" to JsonPrimitive("${groupTargets.size} لایه در قالب گروه تصویری «$groupName» ادغام شدند")
                )
            )
        }

        // Spatial group transformation (preserves relative offsets between layers in the group!)
        val avgX = groupTargets.map { it.x }.average().toFloat()
        val avgY = groupTargets.map { it.y }.average().toFloat()
        val dx = when (action) {
            "ALIGN_GROUP_CENTER" -> -avgX
            else -> args["deltaX"].floatOr(0f)
        }
        val dy = when (action) {
            "ALIGN_GROUP_CENTER" -> -avgY
            else -> args["deltaY"].floatOr(0f)
        }
        val scaleFactor = if (action == "SCALE_GROUP") args["scaleFactor"].floatOr(1f).coerceIn(0.5f, 2.0f) else 1f

        val updatedLayers = p.layers.map { l ->
            if (l.id !in groupIds) l
            else {
                val relX = (l.x - avgX) * scaleFactor
                val relY = (l.y - avgY) * scaleFactor
                val nx = (avgX + relX + dx).coerceIn(-0.46f, 0.46f)
                val ny = (avgY + relY + dy).coerceIn(-0.46f, 0.46f)
                val nsx = (l.scaleX * scaleFactor).coerceIn(0.25f, 3.2f)
                val nsy = (l.scaleY * scaleFactor).coerceIn(0.25f, 3.2f)
                updateLayerCoordsAndScale(l, nx, ny, nsx, nsy)
            }
        }

        session.workingProject = p.copy(layers = updatedLayers)

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "groupSize" to JsonPrimitive(groupTargets.size),
                "groupAction" to JsonPrimitive(action),
                "summaryFa" to JsonPrimitive("گروه‌بندی و هماهنگ‌سازی ${groupTargets.size} لایه ($action) انجام شد")
            )
        )
    }

    private fun execRenderPreview(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val p = session.workingProject
        val scale = args["scaleMultiplier"].floatOr(0.5f).coerceIn(0.2f, 1.0f)
        val previewBmp = if (session.context != null) {
            runCatching {
                StudioCanvasRenderer.renderProjectToBitmap(
                    context = session.context,
                    project = p,
                    scaleMultiplier = scale,
                    includeCheckerboardForTransparent = false
                )
            }.getOrNull()
        } else {
            val w = (p.canvasWidth * scale).toInt().coerceAtLeast(64)
            val h = (p.canvasHeight * scale).toInt().coerceAtLeast(64)
            runCatching { Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) }.getOrNull()
        }

        session.renderedPreviewBitmap = previewBmp
        val w = previewBmp?.width ?: (p.canvasWidth * scale).toInt()
        val h = previewBmp?.height ?: (p.canvasHeight * scale).toInt()

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "previewWidth" to JsonPrimitive(w),
                "previewHeight" to JsonPrimitive(h),
                "layerCount" to JsonPrimitive(p.layers.size),
                "summaryFa" to JsonPrimitive("پیش‌نمایش زنده بوم با ابعاد ${w}×${h} پیکسل تولید شد")
            )
        )
    }

    private fun execValidateDocument(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val autoFix = args["autoFix"].boolOr(true)
        var p = session.workingProject
        val issues = mutableListOf<String>()
        val fixes = mutableListOf<String>()
        var score = 100

        // 1. Check empty or invisible canvas
        val visibleLayers = p.layers.filter { it.isVisible }
        if (visibleLayers.isEmpty()) {
            issues.add("هیچ لایه قابل مشاهده‌ای روی بوم وجود ندارد.")
            score -= 25
        }

        // 2. Check out-of-bounds layers (|x| > 0.44 or |y| > 0.44)
        val boundedLayers = p.layers.map { l ->
            if (!l.isVisible) l
            else if (abs(l.x) > 0.44f || abs(l.y) > 0.44f) {
                issues.add("لایه «${l.name}» بیش از حد به لبه بوم نزدیک است.")
                if (autoFix) {
                    val clampedX = l.x.coerceIn(-0.38f, 0.38f)
                    val clampedY = l.y.coerceIn(-0.38f, 0.38f)
                    fixes.add("موقعیت لایه «${l.name}» به محدوده امن بوم منتقل شد.")
                    updateLayerCoords(l, clampedX, clampedY)
                } else {
                    score -= 10
                    l
                }
            } else l
        }
        p = p.copy(layers = boundedLayers)

        // 3. Check overlapping text layers
        val textLayers = p.layers.filterIsInstance<TextLayer>().filter { it.isVisible }
        if (textLayers.size >= 2) {
            val fixedTextIds = mutableMapOf<String, Float>()
            for (i in 0 until textLayers.size) {
                for (j in i + 1 until textLayers.size) {
                    val a = textLayers[i]
                    val b = textLayers[j]
                    val dy = abs((fixedTextIds[a.id] ?: a.y) - (fixedTextIds[b.id] ?: b.y))
                    val dx = abs(a.x - b.x)
                    if (dy < 0.09f && dx < 0.18f) {
                        issues.add("تداخل عمودی بین لایه متن «${a.text.take(12)}» و «${b.text.take(12)}» مشاهده شد.")
                        if (autoFix) {
                            val newBY = ((fixedTextIds[a.id] ?: a.y) + 0.16f).coerceIn(-0.38f, 0.38f)
                            fixedTextIds[b.id] = newBY
                            fixes.add("فاصله عمودی لایه «${b.text.take(12)}» برای جلوگیری از هم‌پوشانی تنظیم شد.")
                        } else {
                            score -= 12
                        }
                    }
                }
            }
            if (fixedTextIds.isNotEmpty()) {
                p = p.copy(
                    layers = p.layers.map { l ->
                        if (l is TextLayer && l.id in fixedTextIds) l.copy(y = fixedTextIds[l.id] ?: l.y) else l
                    }
                )
            }
        }

        // 4. Check text contrast against background
        val bgHex = p.background.primaryColorHex
        val bgLum = computeRelativeLuminance(bgHex)
        p = p.copy(
            layers = p.layers.map { l ->
                if (l is TextLayer && l.isVisible) {
                    val txtLum = computeRelativeLuminance(l.colorHex)
                    val ratio = (max(bgLum, txtLum) + 0.05) / (min(bgLum, txtLum) + 0.05)
                    if (ratio < 2.2 && !l.stroke.enabled && !l.backgroundPillEnabled) {
                        issues.add("کنتراست رنگ متن «${l.text.take(12)}» با پس‌زمینه کم است.")
                        if (autoFix) {
                            val highContrastHex = if (bgLum < 0.45) "#FFFFFF" else "#0F172A"
                            val strokeHex = if (bgLum < 0.45) "#0B132B" else "#FFFFFF"
                            fixes.add("کنتراست و دورخط لایه متن «${l.text.take(12)}» بهینه‌سازی شد.")
                            l.copy(
                                colorHex = highContrastHex,
                                stroke = l.stroke.copy(enabled = true, colorHex = strokeHex, width = max(l.stroke.width, 5f))
                            )
                        } else {
                            score -= 10
                            l
                        }
                    } else l
                } else l
            }
        )

        // 5. Verify product image preservation
        val currentImageIds = p.layers.filterIsInstance<ImageLayer>().map { it.id }.toSet()
        if (!session.allowProductImageModification && !currentImageIds.containsAll(session.protectedProductImageIds)) {
            val missing = session.preTransactionProject.layers.filterIsInstance<ImageLayer>()
                .filter { it.id in session.protectedProductImageIds && it.id !in currentImageIds }
            if (missing.isNotEmpty()) {
                p = p.copy(layers = missing + p.layers)
                fixes.add("تصویر اصلی محصول که نباید بدون درخواست کاربر حذف می‌شد، بازگردانی گردید.")
            }
        }

        session.workingProject = p
        val finalScore = score.coerceIn(60, 100)
        val summaryFa = if (issues.isEmpty()) {
            "اعتبارسنجی با امتیاز کامل ($finalScore/100): چیدمان، کنتراست، سلسله‌مراتب تایپوگرافی و مرزهای بوم استاندارد است."
        } else if (fixes.isNotEmpty()) {
            "اعتبارسنجی انجام شد (امتیاز $finalScore/100): ${fixes.size} مورد بهینه‌سازی خودکار روی چیدمان و کنتراست اعمال گردید."
        } else {
            "اعتبارسنجی انجام شد (امتیاز $finalScore/100): ${issues.size} نکته طراحی بررسی شد."
        }

        val report = AgentValidationReport(
            isValid = finalScore >= 70,
            scoreOutOf100 = finalScore,
            issuesFa = issues,
            autoFixesFa = fixes,
            summaryFa = summaryFa
        )
        session.lastValidationReport = report

        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "isValid" to JsonPrimitive(report.isValid),
                "scoreOutOf100" to JsonPrimitive(report.scoreOutOf100),
                "issuesCount" to JsonPrimitive(issues.size),
                "autoFixesCount" to JsonPrimitive(fixes.size),
                "summaryFa" to JsonPrimitive(summaryFa)
            )
        )
    }

    private fun execUndoTransaction(
        args: Map<String, JsonElement>,
        session: AgentTransactionSession
    ): JsonObject {
        val reason = args["reason"].strOr("درخواست بازگردانی تراکنش")
        session.rollbackToPreTransaction()
        return JsonObject(
            mapOf(
                "status" to JsonPrimitive("ok"),
                "rollbackStatus" to JsonPrimitive("rolled_back"),
                "reason" to JsonPrimitive(reason),
                "restoredLayerCount" to JsonPrimitive(session.workingProject.layers.size),
                "summaryFa" to JsonPrimitive("تراکنش لغو شد و بوم به وضعیت اولیه قبل از تغییرات بازگشت ($reason)")
            )
        )
    }

    // =========================================================================
    // 4. HELPERS FOR LAYER RESOLUTION, CONTRAST & JSON SERIALIZATION
    // =========================================================================

    fun resolveTargetLayer(
        project: StudioProject,
        rawIdOrQuery: String?,
        selectedLayerId: String?
    ): StudioLayer? {
        val q = rawIdOrQuery?.trim().orEmpty()
        if (q.isBlank() || q.equals("SELECTED", ignoreCase = true) || q.equals("NONE", ignoreCase = true)) {
            return project.layers.find { it.id == selectedLayerId } ?: project.layers.lastOrNull()
        }
        project.layers.find { it.id == q }?.let { return it }
        project.layers.find { it.name.contains(q, ignoreCase = true) }?.let { return it }
        project.layers.filterIsInstance<TextLayer>().find { it.text.contains(q, ignoreCase = true) }?.let { return it }

        val norm = OfflineDesignEngine.normalizePersianCommand(q)
        if (norm.contains("تیتر") || norm.contains("عنوان") || norm.contains("headline")) {
            return project.layers.filterIsInstance<TextLayer>().maxByOrNull { it.fontSizeSp }
        }
        if (norm.contains("زیرنویس") || norm.contains("subtitle") || norm.contains("توضیحات")) {
            return project.layers.filterIsInstance<TextLayer>().minByOrNull { it.fontSizeSp }
        }
        if (norm.contains("شکل") || norm.contains("shape")) {
            return project.layers.filterIsInstance<ShapeLayer>().lastOrNull()
        }
        if (norm.contains("تصویر") || norm.contains("عکس") || norm.contains("image")) {
            return project.layers.filterIsInstance<ImageLayer>().lastOrNull()
        }
        return project.layers.find { it.id == selectedLayerId } ?: project.layers.lastOrNull()
    }

    private fun layerTypeString(layer: StudioLayer): String = when (layer) {
        is TextLayer -> "TEXT"
        is ShapeLayer -> "SHAPE"
        is ImageLayer -> "IMAGE"
        is DrawingLayer -> "DRAWING"
    }

    private fun layerToJsonObject(layer: StudioLayer, index: Int, isSelected: Boolean): JsonObject {
        val base = mutableMapOf<String, JsonElement>(
            "index" to JsonPrimitive(index),
            "id" to JsonPrimitive(layer.id),
            "name" to JsonPrimitive(layer.name),
            "layerType" to JsonPrimitive(layerTypeString(layer)),
            "isSelected" to JsonPrimitive(isSelected),
            "isVisible" to JsonPrimitive(layer.isVisible),
            "isLocked" to JsonPrimitive(layer.isLocked),
            "x" to JsonPrimitive(layer.x),
            "y" to JsonPrimitive(layer.y),
            "scaleX" to JsonPrimitive(layer.scaleX),
            "scaleY" to JsonPrimitive(layer.scaleY),
            "rotationX" to JsonPrimitive(layer.rotationX),
            "rotationY" to JsonPrimitive(layer.rotationY),
            "rotationZ" to JsonPrimitive(layer.rotationZ),
            "opacity" to JsonPrimitive(layer.opacity),
            "strokeEnabled" to JsonPrimitive(layer.stroke.enabled),
            "strokeColorHex" to JsonPrimitive(layer.stroke.colorHex),
            "strokeWidth" to JsonPrimitive(layer.stroke.width),
            "shadowEnabled" to JsonPrimitive(layer.shadow.enabled)
        )
        when (layer) {
            is TextLayer -> {
                base["text"] = JsonPrimitive(layer.text)
                base["fontId"] = JsonPrimitive(layer.fontId)
                base["fontSizeSp"] = JsonPrimitive(layer.fontSizeSp)
                base["colorHex"] = JsonPrimitive(layer.colorHex)
                base["useGradientFill"] = JsonPrimitive(layer.useGradientFill)
                base["gradientSecondColorHex"] = JsonPrimitive(layer.gradientSecondColorHex)
                base["textAlignMode"] = JsonPrimitive(layer.textAlignMode.name)
                base["is3dEnabled"] = JsonPrimitive(layer.is3dEnabled)
                base["depth3d"] = JsonPrimitive(layer.depth3d)
                base["curveAngle"] = JsonPrimitive(layer.curveAngle)
                base["backgroundPillEnabled"] = JsonPrimitive(layer.backgroundPillEnabled)
            }
            is ShapeLayer -> {
                base["shapeKind"] = JsonPrimitive(layer.shapeKind.name)
                base["fillColorHex"] = JsonPrimitive(layer.fillColorHex)
                base["useGradientFill"] = JsonPrimitive(layer.useGradientFill)
                base["gradientSecondColorHex"] = JsonPrimitive(layer.gradientSecondColorHex)
                base["cornerRadius"] = JsonPrimitive(layer.cornerRadius)
                base["is3dExtruded"] = JsonPrimitive(layer.is3dExtruded)
                base["depth3d"] = JsonPrimitive(layer.depth3d)
            }
            is ImageLayer -> {
                base["bitmapWidth"] = JsonPrimitive(layer.bitmap?.width ?: 0)
                base["bitmapHeight"] = JsonPrimitive(layer.bitmap?.height ?: 0)
                base["flipHorizontal"] = JsonPrimitive(layer.flipHorizontal)
                base["flipVertical"] = JsonPrimitive(layer.flipVertical)
                base["cropLeft"] = JsonPrimitive(layer.cropLeft)
                base["cropTop"] = JsonPrimitive(layer.cropTop)
                base["cropRight"] = JsonPrimitive(layer.cropRight)
                base["cropBottom"] = JsonPrimitive(layer.cropBottom)
            }
            is DrawingLayer -> {
                base["strokeCount"] = JsonPrimitive(layer.strokes.size)
            }
        }
        return JsonObject(base)
    }

    private fun updateLayerCoords(layer: StudioLayer, x: Float, y: Float): StudioLayer = when (layer) {
        is TextLayer -> layer.copy(x = x, y = y)
        is ShapeLayer -> layer.copy(x = x, y = y)
        is ImageLayer -> layer.copy(x = x, y = y)
        is DrawingLayer -> layer.copy(x = x, y = y)
    }

    private fun updateLayerCoordsAndScale(
        layer: StudioLayer,
        x: Float,
        y: Float,
        scaleX: Float,
        scaleY: Float
    ): StudioLayer = when (layer) {
        is TextLayer -> layer.copy(x = x, y = y, scaleX = scaleX, scaleY = scaleY)
        is ShapeLayer -> layer.copy(x = x, y = y, scaleX = scaleX, scaleY = scaleY)
        is ImageLayer -> layer.copy(x = x, y = y, scaleX = scaleX, scaleY = scaleY)
        is DrawingLayer -> layer.copy(x = x, y = y, scaleX = scaleX, scaleY = scaleY)
    }

    private fun computeRelativeLuminance(hex: String): Double {
        return try {
            val c = Color.parseColor(hex)
            val r = Color.red(c) / 255.0
            val g = Color.green(c) / 255.0
            val b = Color.blue(c) / 255.0
            fun chan(v: Double) = if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
            0.2126 * chan(r) + 0.7152 * chan(g) + 0.0722 * chan(b)
        } catch (e: Exception) {
            0.2
        }
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    private fun JsonElement?.strOr(default: String): String =
        this?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" } ?: default

    private fun JsonElement?.hexOrNull(): String? =
        this?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.startsWith("#") && it.length in 7..9 }

    private fun JsonElement?.hexOr(default: String): String =
        this.hexOrNull() ?: default

    private fun JsonElement?.boolOrNull(): Boolean? =
        this?.jsonPrimitive?.booleanOrNull

    private fun JsonElement?.boolOr(default: Boolean): Boolean =
        this.boolOrNull() ?: default

    private fun JsonElement?.floatOrNull(): Float? =
        this?.jsonPrimitive?.floatOrNull ?: this?.jsonPrimitive?.doubleOrNull?.toFloat()

    private fun JsonElement?.floatOr(default: Float): Float =
        this.floatOrNull() ?: default

    private fun JsonElement?.intOrNull(): Int? =
        this?.jsonPrimitive?.intOrNull ?: this?.jsonPrimitive?.doubleOrNull?.toInt()

    private fun JsonElement?.intOr(default: Int): Int =
        this.intOrNull() ?: default
}
