package com.example.ui.agent

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.data.DrawingLayer
import com.example.data.ImageLayer
import com.example.data.ShapeLayer
import com.example.data.StudioProject
import com.example.data.TextLayer
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class AgentChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val executedActions: List<String> = emptyList(),
    val executionSteps: List<AgentExecutionStep> = emptyList(),
    val validationReport: AgentValidationReport? = null,
    val errorBanner: String? = null,
    val canUndo: Boolean = false,
    val designCritique: String? = null,
    val followUpSuggestions: List<String> = emptyList(),
    val latencyMs: Long? = null,
    val modelName: String = "gemini-3.8-flash",
    val usedCanvasVision: Boolean = false
)

data class AgentInferenceResult(
    val replyFa: String,
    val designCritiqueFa: String?,
    val followUpSuggestionsFa: List<String>,
    val projectTitle: String?,
    val rawActionsJson: JSONArray,
    val latencyMs: Long,
    val usedCanvasVision: Boolean,
    val modelName: String = "gemini-3.8-flash",
    val executedSteps: List<AgentExecutionStep> = emptyList(),
    val validationReport: AgentValidationReport? = null,
    val geminiErrorFa: String? = null
)

data class AgentTransactionExecutionOutcome(
    val session: AgentTransactionSession,
    val replyFa: String,
    val designCritiqueFa: String?,
    val followUpSuggestionsFa: List<String>,
    val modelName: String,
    val latencyMs: Long,
    val usedCanvasVision: Boolean,
    val geminiErrorFa: String? = null,
    val legacyActionsJson: JSONArray = JSONArray()
)

object PixelLabAgentEngine {

    const val DEFAULT_GEMINI_MODEL = GeminiServiceManager.DEFAULT_MODEL

    fun getSelectedModelName(context: Context?): String {
        return GeminiServiceManager.getSelectedModel(context)
    }

    fun setSelectedModelName(context: Context, modelName: String) {
        GeminiServiceManager.setSelectedModel(context, modelName)
    }

    fun getCustomApiKey(context: Context): String {
        return GeminiServiceManager.getCustomApiKey(context)
    }

    fun setCustomApiKey(context: Context, apiKey: String) {
        GeminiServiceManager.setCustomApiKey(context, apiKey)
    }

    /**
     * Compact, fast-compiling Structured Output schema for legacy JSON action mode.
     */
    val agentActionItemSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "actionType" to Schema.string(
                    description = "One of: LEARN_CURRENT_PLP_STYLE, CREATE_FROM_LEARNED_PLP_STYLE, APPLY_LEARNED_PLP_STYLE, ADD_TEXT_LAYER, UPDATE_SELECTED_TEXT, UPDATE_TEXT_BY_ID, ADD_SHAPE_LAYER, UPDATE_SELECTED_SHAPE, UPDATE_SELECTED_IMAGE, SET_CANVAS_BACKGROUND, SET_CANVAS_ASPECT, SET_POST_FX, TRANSFORM_SELECTED_LAYER, SELECT_LAYER, REORDER_LAYER, TOGGLE_LAYER_VISIBILITY, TOGGLE_LAYER_LOCK, UNLOCK_ALL_LAYERS, DELETE_SELECTED_LAYER, DUPLICATE_SELECTED_LAYER, MERGE_VISIBLE_LAYERS, CLEAR_ALL_LAYERS, RESET_CANVAS, ADD_BUILTIN_STICKER, APPLY_QUOTE, TOGGLE_ANTI_GRAVITY, AUTO_LAYOUT_HARMONIZE, GENERATE_AI_BACKGROUND, GENERATE_AI_STICKER, EDIT_SELECTED_IMAGE_AI"
                ),
                "targetLayerId" to Schema.string(description = "Layer ID or substring of layer name/text to target"),
                "orderDirection" to Schema.string(description = "For REORDER_LAYER: TO_FRONT, TO_BACK, MOVE_UP, or MOVE_DOWN"),
                "text" to Schema.string(description = "ONLY provide when creating a new text layer or explicitly changing text"),
                "subtitleText" to Schema.string(description = "Optional subtitle text"),
                "fontId" to Schema.string(description = "Font ID: lalezar, vazirmatn, amiri, montserrat, playfair, or pacifico"),
                "fontSizeSp" to Schema.double(description = "Font size in sp (18..120)"),
                "primaryColorHex" to Schema.string(description = "Primary hex color (#RRGGBB)"),
                "secondaryColorHex" to Schema.string(description = "Secondary gradient hex color (#RRGGBB)"),
                "useGradient" to Schema.boolean(description = "Enable two-color gradient fill"),
                "strokeEnabled" to Schema.boolean(description = "Enable outline stroke"),
                "shadowEnabled" to Schema.boolean(description = "Enable outer drop shadow"),
                "is3dEnabled" to Schema.boolean(description = "Enable 3D extrusion depth"),
                "depth3d" to Schema.integer(description = "3D extrusion depth (1..50)"),
                "x" to Schema.double(description = "Normalized horizontal offset (-0.45..0.45)"),
                "y" to Schema.double(description = "Normalized vertical offset (-0.45..0.45)"),
                "scale" to Schema.double(description = "Uniform scale multiplier (0.25..3.0)"),
                "rotationZ" to Schema.double(description = "2D Z-axis rotation in degrees (-180..180)"),
                "shapeKind" to Schema.string(description = "Shape kind: RECTANGLE, CIRCLE, HEXAGON, STAR, HEART, BADGE, ARROW, SPEECH_BUBBLE"),
                "aspectRatioLabel" to Schema.string(description = "Canvas aspect ratio: 1:1, 16:9, 9:16, 4:5, or 3:1"),
                "stickerId" to Schema.string(description = "Sticker ID for ADD_BUILTIN_STICKER"),
                "imagePrompt" to Schema.string(description = "English prompt for AI image/sticker generation")
            ),
            optionalProperties = listOf(
                "targetLayerId", "orderDirection", "text", "subtitleText", "fontId", "fontSizeSp",
                "primaryColorHex", "secondaryColorHex", "useGradient", "strokeEnabled", "shadowEnabled",
                "is3dEnabled", "depth3d", "x", "y", "scale", "rotationZ", "shapeKind",
                "aspectRatioLabel", "stickerId", "imagePrompt"
            )
        )
    }

    val agentResponseSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "agentReplyFa" to Schema.string(description = "Helpful explanation in Persian"),
                "designCritiqueFa" to Schema.string(description = "Expert Persian visual design critique"),
                "followUpSuggestionsFa" to Schema.array(items = Schema.string(), description = "2-3 short Persian suggestions"),
                "projectTitle" to Schema.string(description = "Optional updated project title in Persian"),
                "actions" to Schema.array(items = agentActionItemSchema, description = "Ordered list of PixelLab tool actions")
            ),
            optionalProperties = listOf("projectTitle", "designCritiqueFa")
        )
    }

    fun serializeCanvasState(project: StudioProject, selectedLayerId: String?): String {
        val root = JSONObject()
        root.put("projectTitle", project.title)
        root.put("canvasWidth", project.canvasWidth)
        root.put("canvasHeight", project.canvasHeight)
        root.put("selectedLayerId", selectedLayerId ?: "NONE")

        val bg = JSONObject().apply {
            put("type", project.background.type.name)
            put("primaryColorHex", project.background.primaryColorHex)
            put("secondaryColorHex", project.background.secondaryColorHex)
            put("isRadialGradient", project.background.isRadialGradient)
            put("hasBackgroundBitmap", project.background.backgroundBitmap != null)
        }
        root.put("background", bg)

        val fx = JSONObject().apply {
            put("vignetteEnabled", project.postFx.vignetteEnabled)
            put("noiseEnabled", project.postFx.noiseEnabled)
            put("stripesEnabled", project.postFx.stripesEnabled)
            put("brightness", project.postFx.brightness)
            put("contrast", project.postFx.contrast)
            put("saturation", project.postFx.saturation)
        }
        root.put("postFx", fx)

        val layersArr = JSONArray()
        project.layers.forEachIndexed { index, layer ->
            val lObj = JSONObject()
            lObj.put("index", index)
            lObj.put("id", layer.id)
            lObj.put("isSelected", layer.id == selectedLayerId)
            lObj.put("name", layer.name)
            lObj.put("isVisible", layer.isVisible)
            lObj.put("isLocked", layer.isLocked)
            lObj.put("x", layer.x)
            lObj.put("y", layer.y)
            lObj.put("scaleX", layer.scaleX)
            lObj.put("scaleY", layer.scaleY)
            lObj.put("rotationX", layer.rotationX)
            lObj.put("rotationY", layer.rotationY)
            lObj.put("rotationZ", layer.rotationZ)
            lObj.put("opacity", layer.opacity)

            when (layer) {
                is TextLayer -> {
                    lObj.put("layerType", "TEXT")
                    lObj.put("text", layer.text)
                    lObj.put("fontId", layer.fontId)
                    lObj.put("fontSizeSp", layer.fontSizeSp)
                    lObj.put("textAlignMode", layer.textAlignMode.name)
                    lObj.put("colorHex", layer.colorHex)
                    lObj.put("useGradientFill", layer.useGradientFill)
                    lObj.put("gradientSecondColorHex", layer.gradientSecondColorHex)
                    lObj.put("is3dEnabled", layer.is3dEnabled)
                    lObj.put("depth3d", layer.depth3d)
                    lObj.put("curveAngle", layer.curveAngle)
                    lObj.put("backgroundPillEnabled", layer.backgroundPillEnabled)
                }
                is ShapeLayer -> {
                    lObj.put("layerType", "SHAPE")
                    lObj.put("shapeKind", layer.shapeKind.name)
                    lObj.put("fillColorHex", layer.fillColorHex)
                    lObj.put("useGradientFill", layer.useGradientFill)
                    lObj.put("is3dExtruded", layer.is3dExtruded)
                    lObj.put("depth3d", layer.depth3d)
                }
                is ImageLayer -> {
                    lObj.put("layerType", "IMAGE")
                    lObj.put("bitmapWidth", layer.bitmap?.width ?: 0)
                    lObj.put("bitmapHeight", layer.bitmap?.height ?: 0)
                    lObj.put("flipHorizontal", layer.flipHorizontal)
                    lObj.put("flipVertical", layer.flipVertical)
                    lObj.put("eraseColorEnabled", layer.eraseColorEnabled)
                    lObj.put("colorOverlayEnabled", layer.colorOverlayEnabled)
                    lObj.put("isCircularMask", layer.isCircularMask)
                }
                is DrawingLayer -> {
                    lObj.put("layerType", "DRAWING")
                    lObj.put("strokeCount", layer.strokes.size)
                }
            }
            layersArr.put(lObj)
        }
        root.put("layers", layersArr)
        return root.toString()
    }

    fun buildFunctionCallingSystemPrompt(
        canvasStateJson: String,
        recentHistory: List<AgentChatMessage>,
        activeLearnedStyle: PlpLearnedStyleDNA?
    ): String {
        val historySnippet = recentHistory.takeLast(6).joinToString("\n") { msg ->
            val speaker = if (msg.isUser) "User" else "PixelLabAgent"
            "$speaker: ${msg.text}"
        }
        val learnedStyleContext = activeLearnedStyle?.toPromptContextString()
            ?: "Default professional Persian 3D poster styles available."

        return """
            You are the official PixelLab Gemini Design Agent (دستیار طراحی هوشمند جمینای در پیکسل‌لب).
            Your mission is to understand Persian design requests, inspect the live document and layers, plan multi-step design operations in the right order, and invoke the 18 real editor tools via Function Calling:
            - inspect_document
            - inspect_layers
            - inspect_layer
            - add_text_layer
            - update_text_layer
            - add_image_layer
            - add_shape_layer
            - transform_layer
            - set_layer_style
            - set_background
            - reorder_layers
            - align_layers
            - duplicate_layer
            - delete_layer
            - group_layers
            - render_preview
            - validate_document
            - undo_transaction

            STRICT WORKFLOW & DESIGN RULES:
            1. Understand the user's Persian request accurately.
            2. Inspect the current document/layers (`inspect_document` or `inspect_layers`) when needed.
            3. Execute multi-step operations in logical order (e.g., `set_background` -> `add_shape_layer` -> `add_text_layer` -> `set_layer_style` -> `align_layers` -> `render_preview` -> `validate_document`).
            4. PRODUCT IMAGE PROTECTION: Never delete, hide, or overwrite an existing `IMAGE` layer (`ImageLayer`) unless the user explicitly asks to delete or change the image.
            5. TEXT PROTECTION: When styling, coloring, aligning, or making an existing text layer 3D, DO NOT overwrite its `text` content with the user's instruction sentence. Omit `text` in `update_text_layer` unless the user explicitly asks to write new words.
            6. After completing all tool calls, provide a clear, helpful Persian summary of the changes made.

            CURRENT PROJECT SNAPSHOT (JSON):
            $canvasStateJson

            LEARNED PLP STYLE DNA:
            $learnedStyleContext

            RECENT CHAT HISTORY:
            $historySnippet
        """.trimIndent()
    }

    fun buildAgentPrompt(
        userCommand: String,
        canvasStateJson: String,
        recentHistory: List<AgentChatMessage>,
        activeLearnedStyle: PlpLearnedStyleDNA? = null
    ): String {
        return buildFunctionCallingSystemPrompt(canvasStateJson, recentHistory, activeLearnedStyle) +
            "\n\nUSER COMMAND:\n\"$userCommand\""
    }

    fun doesCommandAllowProductImageModification(userCommand: String): Boolean {
        val norm = OfflineDesignEngine.normalizePersianCommand(userCommand)
        return (norm.contains("حذف") || norm.contains("پاک") || norm.contains("قرینه") || norm.contains("تغییر")) &&
            (norm.contains("عکس") || norm.contains("تصویر") || norm.contains("همه لایه") || norm.contains("ریست بوم"))
    }

    /**
     * Executes a full multi-step Gemini Function Calling transaction with:
     * 1. Real Gemini Function Calling (`Tool.functionDeclarations(PixelLabGeminiTools.allDeclarations)`)
     * 2. Configurable model selection (`getSelectedModelName(context)`)
     * 3. Automatic 8-step lifecycle (`inspect_document` -> tool calls -> `render_preview` -> `validate_document`)
     * 4. Full Transaction & Rollback support via `AgentTransactionSession`
     */
    suspend fun executeAgentTransactionWithFunctionCalling(
        context: Context?,
        userCommand: String,
        project: StudioProject,
        selectedLayerId: String?,
        recentHistory: List<AgentChatMessage>,
        canvasVisionBitmap: Bitmap? = null,
        activeLearnedStyle: PlpLearnedStyleDNA? = null,
        onStepProgress: ((String) -> Unit)? = null,
        onLayerHighlighted: ((layerId: String?, toolName: String) -> Unit)? = null
    ): AgentTransactionExecutionOutcome {
        val allowImageMod = doesCommandAllowProductImageModification(userCommand)
        val canvasAdapter = GeminiCanvasAdapter.createSession(
            context = context,
            userCommand = userCommand,
            initialProject = project,
            initialSelectedLayerId = selectedLayerId,
            allowProductImageModification = allowImageMod
        )
        canvasAdapter.onLayerHighlighted = onLayerHighlighted
        val serviceManager = GeminiServiceManager(context)
        return serviceManager.executeTransaction(
            userCommand = userCommand,
            canvasAdapter = canvasAdapter,
            recentHistory = recentHistory,
            canvasVisionBitmap = canvasVisionBitmap,
            activeLearnedStyle = activeLearnedStyle ?: (context?.let { PlpStyleLearningEngine.getActiveLearnedStyle(it) }),
            onStepProgress = onStepProgress
        )
    }

    /**
     * Translates a Persian user request into real calls to the 18 tools in [PixelLabGeminiTools]
     * and also returns any specialized PLP/AI actions.
     */
    fun executeLocalRealToolsPlan(
        userCommand: String,
        session: AgentTransactionSession,
        activeLearnedStyle: PlpLearnedStyleDNA? = null
    ): AgentInferenceResult {
        val normCmd = OfflineDesignEngine.normalizePersianCommand(userCommand)

        // Check if the user asked to undo/rollback the transaction
        if (normCmd == "لغو" || normCmd.contains("بازگردانی تراکنش") || normCmd.contains("undo transaction")) {
            PixelLabGeminiTools.executeToolCall(
                toolName = "undo_transaction",
                args = mapOf("reason" to kotlinx.serialization.json.JsonPrimitive("درخواست مستقیم کاربر")),
                session = session
            )
            return AgentInferenceResult(
                replyFa = "تراکنش لغو شد و تغییرات به وضعیت اولیه بازگردانده شد.",
                designCritiqueFa = null,
                followUpSuggestionsFa = emptyList(),
                projectTitle = null,
                rawActionsJson = JSONArray(),
                latencyMs = 5L,
                usedCanvasVision = false,
                modelName = "gemini-3.8-flash"
            )
        }

        val offlineResult = OfflineDesignEngine.processCommand(
            userCommand = userCommand,
            project = session.workingProject,
            selectedLayerId = session.workingSelectedLayerId,
            activeLearnedStyle = activeLearnedStyle
        )

        val passthroughSpecialActions = JSONArray()
        val actions = offlineResult.rawActionsJson

        for (i in 0 until actions.length()) {
            val act = actions.optJSONObject(i) ?: continue
            val actionType = act.optString("actionType", "")

            when (actionType) {
                "SET_CANVAS_BACKGROUND" -> {
                    val toolArgs = JSONObject().apply {
                        put("backgroundType", if (act.optBoolean("useGradient", true)) "GRADIENT" else "COLOR")
                        if (act.has("primaryColorHex")) put("primaryColorHex", act.optString("primaryColorHex"))
                        if (act.has("secondaryColorHex")) put("secondaryColorHex", act.optString("secondaryColorHex"))
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("set_background", toolArgs, session)
                }
                "SET_CANVAS_ASPECT" -> {
                    val toolArgs = JSONObject().apply {
                        put("aspectRatioPreset", act.optString("aspectRatioLabel", "1:1"))
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("set_background", toolArgs, session)
                }
                "SET_POST_FX" -> {
                    val toolArgs = JSONObject().apply {
                        if (act.has("vignetteEnabled")) put("vignetteEnabled", act.optBoolean("vignetteEnabled"))
                        if (act.has("noiseEnabled")) put("noiseEnabled", act.optBoolean("noiseEnabled"))
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("set_background", toolArgs, session)
                }
                "ADD_TEXT_LAYER" -> {
                    val toolArgs = JSONObject().apply {
                        put("text", act.optString("text", "متن هوشمند پیکسل‌لب"))
                        if (act.has("layerName")) put("layerName", act.optString("layerName"))
                        if (act.has("fontId")) put("fontId", act.optString("fontId"))
                        if (act.has("fontSizeSp")) put("fontSizeSp", act.optDouble("fontSizeSp"))
                        if (act.has("primaryColorHex")) put("colorHex", act.optString("primaryColorHex"))
                        if (act.has("secondaryColorHex")) put("secondaryColorHex", act.optString("secondaryColorHex"))
                        if (act.has("useGradient")) put("useGradient", act.optBoolean("useGradient"))
                        if (act.has("x")) put("x", act.optDouble("x"))
                        if (act.has("y")) put("y", act.optDouble("y"))
                        if (act.has("scale")) put("scale", act.optDouble("scale"))
                        if (act.has("rotationZ")) put("rotationZ", act.optDouble("rotationZ"))
                        if (act.has("is3dEnabled")) put("is3dEnabled", act.optBoolean("is3dEnabled"))
                        if (act.has("depth3d")) put("depth3d", act.optInt("depth3d"))
                        if (act.has("strokeEnabled")) put("strokeEnabled", act.optBoolean("strokeEnabled"))
                        if (act.has("strokeColorHex")) put("strokeColorHex", act.optString("strokeColorHex"))
                        if (act.has("strokeWidth")) put("strokeWidth", act.optDouble("strokeWidth"))
                        if (act.has("shadowEnabled")) put("shadowEnabled", act.optBoolean("shadowEnabled"))
                        if (act.has("backgroundPillEnabled")) put("backgroundPillEnabled", act.optBoolean("backgroundPillEnabled"))
                        if (act.has("backgroundPillColorHex")) put("backgroundPillColorHex", act.optString("backgroundPillColorHex"))
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("add_text_layer", toolArgs, session)
                }
                "UPDATE_SELECTED_TEXT", "UPDATE_TEXT_BY_ID" -> {
                    val targetId = act.optString("targetLayerId", "SELECTED")
                    val updateArgs = JSONObject().apply {
                        put("layerId", targetId)
                        if (act.has("text")) put("text", act.optString("text"))
                        if (act.has("fontId")) put("fontId", act.optString("fontId"))
                        if (act.has("fontSizeSp")) put("fontSizeSp", act.optDouble("fontSizeSp"))
                        if (act.has("primaryColorHex")) put("colorHex", act.optString("primaryColorHex"))
                        if (act.has("secondaryColorHex")) put("secondaryColorHex", act.optString("secondaryColorHex"))
                        if (act.has("useGradient")) put("useGradient", act.optBoolean("useGradient"))
                        if (act.has("curveAngle")) put("curveAngle", act.optDouble("curveAngle"))
                        if (act.has("backgroundPillEnabled")) put("backgroundPillEnabled", act.optBoolean("backgroundPillEnabled"))
                        if (act.has("backgroundPillColorHex")) put("backgroundPillColorHex", act.optString("backgroundPillColorHex"))
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("update_text_layer", updateArgs, session)

                    val styleArgs = JSONObject().apply {
                        put("layerId", targetId)
                        if (act.has("primaryColorHex")) put("primaryColorHex", act.optString("primaryColorHex"))
                        if (act.has("secondaryColorHex")) put("secondaryColorHex", act.optString("secondaryColorHex"))
                        if (act.has("useGradient")) put("useGradient", act.optBoolean("useGradient"))
                        if (act.has("strokeEnabled")) put("strokeEnabled", act.optBoolean("strokeEnabled"))
                        if (act.has("strokeColorHex")) put("strokeColorHex", act.optString("strokeColorHex"))
                        if (act.has("strokeWidth")) put("strokeWidth", act.optDouble("strokeWidth"))
                        if (act.has("shadowEnabled")) put("shadowEnabled", act.optBoolean("shadowEnabled"))
                        if (act.has("shadowColorHex")) put("shadowColorHex", act.optString("shadowColorHex"))
                        if (act.has("shadowBlur")) put("shadowBlur", act.optDouble("shadowBlur"))
                        if (act.has("embossEnabled")) put("embossEnabled", act.optBoolean("embossEnabled"))
                        if (act.has("is3dEnabled")) put("is3dEnabled", act.optBoolean("is3dEnabled"))
                        if (act.has("depth3d")) put("depth3d", act.optInt("depth3d"))
                        if (act.has("obliqueAngle3d")) put("obliqueAngle3d", act.optDouble("obliqueAngle3d"))
                        if (act.has("reflectionEnabled")) put("reflectionEnabled", act.optBoolean("reflectionEnabled"))
                        if (act.has("texturePreset")) put("texturePreset", act.optString("texturePreset"))
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("set_layer_style", styleArgs, session)

                    if (act.has("x") || act.has("y") || act.has("scale") || act.has("rotationZ") || act.has("rotationX") || act.has("rotationY")) {
                        val trArgs = JSONObject().apply {
                            put("layerId", targetId)
                            if (act.has("x")) put("x", act.optDouble("x"))
                            if (act.has("y")) put("y", act.optDouble("y"))
                            if (act.has("scale")) {
                                put("scaleX", act.optDouble("scale"))
                                put("scaleY", act.optDouble("scale"))
                            }
                            if (act.has("rotationX")) put("rotationX", act.optDouble("rotationX"))
                            if (act.has("rotationY")) put("rotationY", act.optDouble("rotationY"))
                            if (act.has("rotationZ")) put("rotationZ", act.optDouble("rotationZ"))
                        }
                        PixelLabGeminiTools.executeToolCallFromOrgJson("transform_layer", trArgs, session)
                    }
                }
                "ADD_SHAPE_LAYER" -> {
                    val toolArgs = JSONObject().apply {
                        put("shapeKind", act.optString("shapeKind", "HEXAGON"))
                        if (act.has("layerName")) put("layerName", act.optString("layerName"))
                        if (act.has("primaryColorHex")) put("fillColorHex", act.optString("primaryColorHex"))
                        if (act.has("secondaryColorHex")) put("secondaryColorHex", act.optString("secondaryColorHex"))
                        if (act.has("useGradient")) put("useGradient", act.optBoolean("useGradient"))
                        if (act.has("x")) put("x", act.optDouble("x"))
                        if (act.has("y")) put("y", act.optDouble("y"))
                        if (act.has("scale")) {
                            put("scaleX", act.optDouble("scale"))
                            put("scaleY", act.optDouble("scale"))
                        }
                        if (act.has("opacity")) put("opacity", act.optDouble("opacity"))
                        if (act.has("is3dEnabled")) put("is3dExtruded", act.optBoolean("is3dEnabled"))
                        if (act.has("depth3d")) put("depth3d", act.optInt("depth3d"))
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("add_shape_layer", toolArgs, session)
                }
                "UPDATE_SELECTED_SHAPE", "UPDATE_SELECTED_IMAGE" -> {
                    val styleArgs = JSONObject().apply {
                        put("layerId", act.optString("targetLayerId", "SELECTED"))
                        if (act.has("primaryColorHex")) put("primaryColorHex", act.optString("primaryColorHex"))
                        if (act.has("secondaryColorHex")) put("secondaryColorHex", act.optString("secondaryColorHex"))
                        if (act.has("useGradient")) put("useGradient", act.optBoolean("useGradient"))
                        if (act.has("strokeEnabled")) put("strokeEnabled", act.optBoolean("strokeEnabled"))
                        if (act.has("strokeColorHex")) put("strokeColorHex", act.optString("strokeColorHex"))
                        if (act.has("strokeWidth")) put("strokeWidth", act.optDouble("strokeWidth"))
                        if (act.has("is3dEnabled")) put("is3dEnabled", act.optBoolean("is3dEnabled"))
                        if (act.has("depth3d")) put("depth3d", act.optInt("depth3d"))
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("set_layer_style", styleArgs, session)
                }
                "ADD_BUILTIN_STICKER" -> {
                    val toolArgs = JSONObject().apply {
                        put("stickerId", act.optString("stickerId", "crown_gold"))
                        if (act.has("x")) put("x", act.optDouble("x"))
                        if (act.has("y")) put("y", act.optDouble("y"))
                        if (act.has("scale")) put("scale", act.optDouble("scale"))
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("add_image_layer", toolArgs, session)
                }
                "TRANSFORM_SELECTED_LAYER" -> {
                    val toolArgs = JSONObject().apply {
                        put("layerId", act.optString("targetLayerId", "SELECTED"))
                        if (act.has("x")) put("x", act.optDouble("x"))
                        if (act.has("y")) put("y", act.optDouble("y"))
                        if (act.has("scale")) {
                            put("scaleX", act.optDouble("scale"))
                            put("scaleY", act.optDouble("scale"))
                        }
                        if (act.has("rotationX")) put("rotationX", act.optDouble("rotationX"))
                        if (act.has("rotationY")) put("rotationY", act.optDouble("rotationY"))
                        if (act.has("rotationZ")) put("rotationZ", act.optDouble("rotationZ"))
                        if (act.has("skewX")) put("skewX", act.optDouble("skewX"))
                        if (act.has("skewY")) put("skewY", act.optDouble("skewY"))
                        if (act.has("opacity")) put("opacity", act.optDouble("opacity"))
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("transform_layer", toolArgs, session)
                }
                "REORDER_LAYER" -> {
                    val toolArgs = JSONObject().apply {
                        put("layerId", act.optString("targetLayerId", "SELECTED"))
                        put("direction", act.optString("orderDirection", "TO_FRONT"))
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("reorder_layers", toolArgs, session)
                }
                "DUPLICATE_SELECTED_LAYER" -> {
                    val toolArgs = JSONObject().apply {
                        put("layerId", act.optString("targetLayerId", "SELECTED"))
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("duplicate_layer", toolArgs, session)
                }
                "DELETE_SELECTED_LAYER" -> {
                    val toolArgs = JSONObject().apply {
                        put("layerId", act.optString("targetLayerId", "SELECTED"))
                        put("allowDeleteImage", session.allowProductImageModification)
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("delete_layer", toolArgs, session)
                }
                "MERGE_VISIBLE_LAYERS" -> {
                    val toolArgs = JSONObject().apply {
                        put("layerIds", "VISIBLE")
                        put("groupAction", "MERGE_TO_IMAGE_LAYER")
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("group_layers", toolArgs, session)
                }
                "AUTO_LAYOUT_PREVIEW", "AUTO_LAYOUT_HARMONIZE" -> {
                    val toolArgs = JSONObject().apply {
                        put("layerIds", "ALL")
                        put("alignmentMode", "GOLDEN_RATIO_STACK")
                    }
                    PixelLabGeminiTools.executeToolCallFromOrgJson("align_layers", toolArgs, session)
                    passthroughSpecialActions.put(act)
                }
                "CLEAR_ALL_LAYERS" -> {
                    // Only clear non-protected layers unless product image deletion is explicitly allowed
                    val keptImages = if (session.allowProductImageModification) {
                        emptyList()
                    } else {
                        session.workingProject.layers.filterIsInstance<ImageLayer>()
                            .filter { it.id in session.protectedProductImageIds }
                    }
                    session.workingProject = session.workingProject.copy(layers = keptImages)
                }
                else -> {
                    passthroughSpecialActions.put(act)
                }
            }
        }

        if (! offlineResult.projectTitle.isNullOrBlank()) {
            session.workingProject = session.workingProject.copy(title = offlineResult.projectTitle)
        }

        return offlineResult.copy(rawActionsJson = passthroughSpecialActions)
    }

    /**
     * Legacy inference entrypoint preserved for full compatibility with existing callers/tests.
     */
    suspend fun executeAgentInference(
        context: Context,
        userCommand: String,
        project: StudioProject,
        selectedLayerId: String?,
        recentHistory: List<AgentChatMessage>,
        canvasVisionBitmap: Bitmap? = null,
        activeLearnedStyle: PlpLearnedStyleDNA? = null
    ): AgentInferenceResult {
        val startTime = System.currentTimeMillis()
        val hasVision = canvasVisionBitmap != null
        val selectedModel = getSelectedModelName(context)
        val effectiveLearnedStyle = activeLearnedStyle ?: PlpStyleLearningEngine.getActiveLearnedStyle(context)

        val canvasStateJson = serializeCanvasState(project, selectedLayerId)
        val prompt = buildAgentPrompt(userCommand, canvasStateJson, recentHistory, effectiveLearnedStyle)

        val strictConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = agentResponseSchema
        }

        val flexibleJsonConfig = generationConfig {
            responseMimeType = "application/json"
        }

        var usedModelName = selectedModel
        var effectiveVision = hasVision && canvasVisionBitmap != null

        val response = try {
            val primaryModel = Firebase.ai.generativeModel(
                modelName = selectedModel,
                generationConfig = strictConfig
            )
            withTimeout(35_000L) {
                if (canvasVisionBitmap != null) {
                    primaryModel.generateContent(
                        content {
                            image(canvasVisionBitmap)
                            text(prompt)
                        }
                    )
                } else {
                    primaryModel.generateContent(prompt)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (primaryErr: Exception) {
            val (diagCategory, diagDetails) = diagnoseError(primaryErr)
            AgentDiagnosticsLogger.warn(
                tag = "AgentInference",
                message = "Primary strict schema call ($selectedModel) warning: $diagCategory. Retrying $selectedModel with flexible JSON...",
                details = diagDetails
            )
            usedModelName = selectedModel
            val fallbackModel = Firebase.ai.generativeModel(
                modelName = selectedModel,
                generationConfig = flexibleJsonConfig
            )
            try {
                withTimeout(30_000L) {
                    if (effectiveVision && canvasVisionBitmap != null) {
                        fallbackModel.generateContent(
                            content {
                                image(canvasVisionBitmap)
                                text(prompt)
                            }
                        )
                    } else {
                        fallbackModel.generateContent(prompt)
                    }
                }
            } catch (fallbackCancel: CancellationException) {
                throw fallbackCancel
            } catch (fallbackErr: Exception) {
                val latency = System.currentTimeMillis() - startTime
                val (fCat, fDet) = diagnoseError(fallbackErr)
                AgentDiagnosticsLogger.error(
                    tag = "AgentInference",
                    message = "Cloud Gemini AI switched to local tool planner: $fCat",
                    details = fDet
                )
                val offlineResult = OfflineDesignEngine.processCommand(
                    userCommand = userCommand,
                    project = project,
                    selectedLayerId = selectedLayerId,
                    activeLearnedStyle = effectiveLearnedStyle
                )
                return offlineResult.copy(
                    latencyMs = latency,
                    modelName = selectedModel,
                    geminiErrorFa = fCat
                )
            }
        }

        return try {
            val latency = System.currentTimeMillis() - startTime
            val rawJsonText = response.text ?: throw IllegalStateException("پاسخ خالی از مدل جمینای دریافت شد.")
            val parsed = parseInferenceJson(
                rawJsonText = rawJsonText,
                latencyMs = latency,
                usedVision = effectiveVision,
                modelName = usedModelName
            )
            if (parsed.rawActionsJson.length() == 0) {
                val supplemental = OfflineDesignEngine.processCommand(
                    userCommand = userCommand,
                    project = project,
                    selectedLayerId = selectedLayerId,
                    activeLearnedStyle = effectiveLearnedStyle
                )
                if (supplemental.rawActionsJson.length() > 0) {
                    return parsed.copy(rawActionsJson = supplemental.rawActionsJson)
                }
            }
            parsed
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            val fallback = OfflineDesignEngine.processCommand(
                userCommand = userCommand,
                project = project,
                selectedLayerId = selectedLayerId,
                activeLearnedStyle = effectiveLearnedStyle
            )
            fallback.copy(latencyMs = latency, modelName = usedModelName)
        }
    }

    private fun extractJsonObject(rawText: String): JSONObject {
        val trimmed = rawText.trim()
        val cleaned = if (trimmed.startsWith("```")) {
            trimmed
                .removePrefix("```json")
                .removePrefix("```JSON")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        } else {
            val firstBrace = trimmed.indexOf('{')
            val lastBrace = trimmed.lastIndexOf('}')
            if (firstBrace != -1 && lastBrace > firstBrace) {
                trimmed.substring(firstBrace, lastBrace + 1)
            } else {
                trimmed
            }
        }
        return JSONObject(cleaned)
    }

    private fun parseInferenceJson(
        rawJsonText: String,
        latencyMs: Long,
        usedVision: Boolean,
        modelName: String
    ): AgentInferenceResult {
        val json = extractJsonObject(rawJsonText)
        val replyFa = json.optString("agentReplyFa", "دستور شما روی بوم اجرا شد.")
        val designCritiqueFa = json.optString("designCritiqueFa").takeIf { it.isNotBlank() && it != "null" }
        val suggestionsArr = json.optJSONArray("followUpSuggestionsFa")
        val suggestions = if (suggestionsArr != null) {
            (0 until suggestionsArr.length()).mapNotNull { idx ->
                suggestionsArr.optString(idx).takeIf { it.isNotBlank() }
            }
        } else {
            emptyList()
        }
        val projectTitle = json.optString("projectTitle").takeIf { it.isNotBlank() && it != "null" }
        val actions = json.optJSONArray("actions") ?: JSONArray()

        return AgentInferenceResult(
            replyFa = replyFa,
            designCritiqueFa = designCritiqueFa,
            followUpSuggestionsFa = suggestions,
            projectTitle = projectTitle,
            rawActionsJson = actions,
            latencyMs = latencyMs,
            usedCanvasVision = usedVision,
            modelName = modelName
        )
    }

    fun diagnoseError(e: Throwable): Pair<String, String> {
        val chain = generateSequence(e) { it.cause }.toList()
        val allMessages = chain.joinToString(" -> ") { "${it.javaClass.simpleName}: ${it.message ?: "No message"}" }

        val isNetwork = chain.any {
            it is java.net.UnknownHostException ||
            it is java.net.SocketTimeoutException ||
            it is java.net.ConnectException ||
            it is java.net.NoRouteToHostException ||
            it.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
            it.message?.contains("network", ignoreCase = true) == true ||
            it.message?.contains("SSLHandshakeException", ignoreCase = true) == true
        }

        val isAppCheck = chain.any {
            it.message?.contains("AppCheck", ignoreCase = true) == true ||
            it.message?.contains("attestation", ignoreCase = true) == true ||
            it.message?.contains("403", ignoreCase = true) == true ||
            it.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ||
            it.message?.contains("app-check", ignoreCase = true) == true
        }

        val isQuota = chain.any {
            it.message?.contains("429", ignoreCase = true) == true ||
            it.message?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true ||
            it.message?.contains("Quota", ignoreCase = true) == true
        }

        val isTimeout = chain.any {
            it is kotlinx.coroutines.TimeoutCancellationException ||
            it is java.net.SocketTimeoutException ||
            it.message?.contains("timeout", ignoreCase = true) == true
        }

        val isHighDemand = chain.any {
            it.message?.contains("high demand", ignoreCase = true) == true ||
            it.message?.contains("spikes in demand", ignoreCase = true) == true ||
            it.message?.contains("503", ignoreCase = true) == true
        }

        val categoryFa = when {
            isHighDemand -> "ترافیک بالای سرورهای جمینای (High Demand) - در حال استفاده از مدل پشتیبان"
            isAppCheck -> "عدم تایید گواهی امنیتی Firebase App Check (کد 403)"
            isNetwork -> "قطع اتصال شبکه یا عدم دسترسی به سرورهای Gemini"
            isQuota -> "محدودیت سهمیه درخواست‌های Gemini API (کد 429)"
            isTimeout -> "اتمام مهلت پاسخ سرور Gemini (Timeout)"
            else -> "خطای ارتباطی سرویس هوش مصنوعی (${e.javaClass.simpleName})"
        }

        return categoryFa to allMessages
    }
}
