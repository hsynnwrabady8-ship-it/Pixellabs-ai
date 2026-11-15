package com.example.ui.agent

import android.content.Context
import android.graphics.Bitmap
import com.example.data.StudioLayer
import com.example.data.StudioProject
import com.example.data.TextLayer
import com.example.data.ShapeLayer
import com.example.data.ImageLayer
import com.example.data.DrawingLayer
import com.google.firebase.ai.type.Tool
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray
import org.json.JSONObject

/**
 * Layer / Adapter connecting Gemini Function Calling tool invocations directly
 * to actual canvas state mutations and StudioProject operations.
 *
 * Provides typed helpers for:
 * - Full 3D rotation, extrusion, shadows, and gradient styling (setLayerStyle3D)
 * - 9-point grid alignment (alignToGrid)
 * - Semantic scene graph inspection (buildSemanticSceneGraph)
 * - Live bounding box visual feedback callback (onLayerHighlighted)
 * - Transactional safety with atomic preview and rollback
 */
class GeminiCanvasAdapter(
    val session: AgentTransactionSession
) {

    /**
     * Optional callback fired whenever a canvas layer is targeted or mutated by an AI tool,
     * providing live bounding box highlight feedback on the active canvas layer.
     */
    var onLayerHighlighted: ((layerId: String?, toolName: String) -> Unit)? = null

    val toolBundle: Tool
        get() = PixelLabGeminiTools.geminiToolBundle

    val allToolNames: List<String>
        get() = PixelLabGeminiTools.allToolNames

    /**
     * Executes a tool call invoked by Gemini Function Calling, mutating the working
     * project in [session] and returning structured JsonObject for the model response.
     */
    fun executeTool(
        toolName: String,
        args: Map<String, JsonElement>
    ): JsonObject {
        val result = PixelLabGeminiTools.executeToolCall(
            toolName = toolName,
            args = args,
            session = session
        )

        // Identify the affected layer and notify visual feedback observer
        val explicitLayerId = args["layerId"]?.jsonPrimitive?.contentOrNull
        val affectedLayerId = if (!explicitLayerId.isNullOrBlank() && !explicitLayerId.equals("SELECTED", ignoreCase = true)) {
            session.workingProject.layers.find { it.id == explicitLayerId || it.name.contains(explicitLayerId, ignoreCase = true) }?.id
                ?: session.workingSelectedLayerId
        } else {
            session.workingSelectedLayerId
        }
        onLayerHighlighted?.invoke(affectedLayerId, toolName)

        return result
    }

    /**
     * Overload for direct invocation from org.json.JSONObject (e.g. from tests or UI).
     */
    fun executeToolFromOrgJson(
        toolName: String,
        orgJsonArgs: JSONObject
    ): JsonObject {
        val result = PixelLabGeminiTools.executeToolCallFromOrgJson(
            toolName = toolName,
            orgJsonArgs = orgJsonArgs,
            session = session
        )

        val explicitLayerId = orgJsonArgs.optString("layerId", "").ifBlank { null }
        val affectedLayerId = if (!explicitLayerId.isNullOrBlank() && !explicitLayerId.equals("SELECTED", ignoreCase = true)) {
            session.workingProject.layers.find { it.id == explicitLayerId || it.name.contains(explicitLayerId, ignoreCase = true) }?.id
                ?: session.workingSelectedLayerId
        } else {
            session.workingSelectedLayerId
        }
        onLayerHighlighted?.invoke(affectedLayerId, toolName)

        return result
    }

    fun executeToolAsOrgJson(
        toolName: String,
        orgJsonArgs: JSONObject
    ): JSONObject {
        val result = PixelLabGeminiTools.executeToolCallAsOrgJson(
            toolName = toolName,
            orgJsonArgs = orgJsonArgs,
            session = session
        )

        val explicitLayerId = orgJsonArgs.optString("layerId", "").ifBlank { null }
        val affectedLayerId = if (!explicitLayerId.isNullOrBlank() && !explicitLayerId.equals("SELECTED", ignoreCase = true)) {
            session.workingProject.layers.find { it.id == explicitLayerId || it.name.contains(explicitLayerId, ignoreCase = true) }?.id
                ?: session.workingSelectedLayerId
        } else {
            session.workingSelectedLayerId
        }
        onLayerHighlighted?.invoke(affectedLayerId, toolName)

        return result
    }

    // =========================================================================
    // DIRECT TYPED HELPER METHODS FOR LAYER MANAGEMENT
    // =========================================================================

    fun addTextLayer(
        text: String,
        fontId: String = "lalezar",
        fontSizeSp: Float = 52f,
        colorHex: String = "#FFFFFF",
        is3dEnabled: Boolean = true,
        depth3d: Int = 18,
        x: Float = 0f,
        y: Float = 0f
    ): JsonObject {
        val args = JSONObject().apply {
            put("text", text)
            put("fontId", fontId)
            put("fontSizeSp", fontSizeSp.toDouble())
            put("colorHex", colorHex)
            put("is3dEnabled", is3dEnabled)
            put("depth3d", depth3d)
            put("x", x.toDouble())
            put("y", y.toDouble())
        }
        return executeToolFromOrgJson("add_text_layer", args)
    }

    fun transformLayer(
        layerId: String,
        x: Float? = null,
        y: Float? = null,
        scale: Float? = null,
        rotationZ: Float? = null,
        rotationX: Float? = null,
        rotationY: Float? = null
    ): JsonObject {
        val args = JSONObject().apply {
            put("layerId", layerId)
            x?.let { put("x", it.toDouble()) }
            y?.let { put("y", it.toDouble()) }
            scale?.let { put("scale", it.toDouble()) }
            rotationZ?.let { put("rotationZ", it.toDouble()) }
            rotationX?.let { put("rotationX", it.toDouble()) }
            rotationY?.let { put("rotationY", it.toDouble()) }
        }
        return executeToolFromOrgJson("transform_layer", args)
    }

    fun updateTextLayer(
        layerId: String,
        text: String? = null,
        fontSizeSp: Float? = null,
        colorHex: String? = null,
        curveAngle: Float? = null
    ): JsonObject {
        val args = JSONObject().apply {
            put("layerId", layerId)
            text?.let {
                put("text", it)
                put("forceUpdateText", true)
            }
            fontSizeSp?.let { put("fontSizeSp", it.toDouble()) }
            colorHex?.let { put("colorHex", it) }
            curveAngle?.let { put("curveAngle", it.toDouble()) }
        }
        return executeToolFromOrgJson("update_text_layer", args)
    }

    fun addShapeLayer(
        shapeKind: String = "RECTANGLE",
        fillColorHex: String = "#0284C7",
        is3dEnabled: Boolean = true
    ): JsonObject {
        val args = JSONObject().apply {
            put("shapeKind", shapeKind)
            put("fillColorHex", fillColorHex)
            put("is3dEnabled", is3dEnabled)
        }
        return executeToolFromOrgJson("add_shape_layer", args)
    }

    /**
     * Advanced 3D & Styling mutation method supporting full 3D rotation, extrusion depth,
     * directional lighting, drop/inner shadow, and gradient styling.
     */
    fun setLayerStyle3D(
        layerId: String = "SELECTED",
        rotationX: Float? = null,
        rotationY: Float? = null,
        rotationZ: Float? = null,
        is3dEnabled: Boolean? = null,
        depth3d: Int? = null,
        obliqueAngle3d: Float? = null,
        darken3d: Float? = null,
        simulateLighting: Boolean? = null,
        lightAngle: Float? = null,
        lightIntensity: Float? = null,
        ambientLight: Float? = null,
        specularHardness: Float? = null,
        shadowEnabled: Boolean? = null,
        shadowColorHex: String? = null,
        shadowBlurRadius: Float? = null,
        shadowOffsetX: Float? = null,
        shadowOffsetY: Float? = null,
        innerShadowEnabled: Boolean? = null,
        embossEnabled: Boolean? = null,
        primaryColorHex: String? = null,
        secondaryColorHex: String? = null,
        useGradient: Boolean? = null
    ): JsonObject {
        val args = JSONObject().apply {
            put("layerId", layerId)
            rotationX?.let { put("rotationX", it.toDouble()) }
            rotationY?.let { put("rotationY", it.toDouble()) }
            rotationZ?.let { put("rotationZ", it.toDouble()) }
            is3dEnabled?.let { put("is3dEnabled", it) }
            depth3d?.let { put("depth3d", it) }
            obliqueAngle3d?.let { put("obliqueAngle3d", it.toDouble()) }
            darken3d?.let { put("darken3d", it.toDouble()) }
            simulateLighting?.let { put("simulateLighting", it) }
            lightAngle?.let { put("lightAngle", it.toDouble()) }
            lightIntensity?.let { put("lightIntensity", it.toDouble()) }
            ambientLight?.let { put("ambientLight", it.toDouble()) }
            specularHardness?.let { put("specularHardness", it.toDouble()) }
            shadowEnabled?.let { put("shadowEnabled", it) }
            shadowColorHex?.let { put("shadowColorHex", it) }
            shadowBlurRadius?.let { put("shadowBlurRadius", it.toDouble()) }
            shadowOffsetX?.let { put("shadowOffsetX", it.toDouble()) }
            shadowOffsetY?.let { put("shadowOffsetY", it.toDouble()) }
            innerShadowEnabled?.let { put("innerShadowEnabled", it) }
            embossEnabled?.let { put("embossEnabled", it) }
            primaryColorHex?.let { put("primaryColorHex", it) }
            secondaryColorHex?.let { put("secondaryColorHex", it) }
            useGradient?.let { put("useGradient", it) }
        }
        return executeToolFromOrgJson("set_layer_style", args)
    }

    /**
     * Positions a layer onto the 9-point composition grid:
     * TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER_LEFT, CENTER, CENTER_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT.
     */
    fun alignToGrid(
        layerId: String = "SELECTED",
        gridPosition: String = "CENTER"
    ): JsonObject {
        val args = JSONObject().apply {
            put("layerId", layerId)
            put("alignmentMode", gridPosition)
        }
        return executeToolFromOrgJson("align_layers", args)
    }

    fun deleteLayer(layerId: String, allowDeleteProductImage: Boolean = false): JsonObject {
        val args = JSONObject().apply {
            put("layerId", layerId)
            put("allowDeleteImage", allowDeleteProductImage)
            put("confirmModifyProductImage", allowDeleteProductImage)
        }
        return executeToolFromOrgJson("delete_layer", args)
    }

    fun alignLayers(layerId: String = "SELECTED", alignmentMode: String = "CENTER_BOTH"): JsonObject {
        val args = JSONObject().apply {
            put("layerId", layerId)
            put("alignmentMode", alignmentMode)
        }
        return executeToolFromOrgJson("align_layers", args)
    }

    fun renderPreview(scaleMultiplier: Float = 0.5f): JsonObject {
        val args = JSONObject().apply {
            put("scaleMultiplier", scaleMultiplier.toDouble())
        }
        return executeToolFromOrgJson("render_preview", args)
    }

    fun validateDocument(autoFix: Boolean = true): JsonObject {
        val args = JSONObject().apply {
            put("autoFix", autoFix)
        }
        return executeToolFromOrgJson("validate_document", args)
    }

    fun undoTransaction(reason: String = "درخواست بازگشت"): JsonObject {
        val args = JSONObject().apply {
            put("reason", reason)
        }
        return executeToolFromOrgJson("undo_transaction", args)
    }

    /**
     * Builds a comprehensive Semantic Scene Graph of the active canvas document,
     * assigning functional design roles (HERO_PRODUCT_IMAGE, HEADLINE_TEXT, SUBTITLE, BADGE_SHAPE, etc.)
     * and calculating spatial and visual relationships for Gemini reasoning.
     */
    fun buildSemanticSceneGraph(): JSONObject {
        val p = session.workingProject
        val root = JSONObject()

        // 1. Canvas Summary
        val canvasObj = JSONObject().apply {
            put("width", p.canvasWidth)
            put("height", p.canvasHeight)
            put("aspectRatio", String.format(java.util.Locale.US, "%.2f", p.canvasWidth.toFloat() / p.canvasHeight.coerceAtLeast(1)))
            put("backgroundType", p.background.type.name)
            put("primaryBackgroundHex", p.background.primaryColorHex)
            put("secondaryBackgroundHex", p.background.secondaryColorHex)
            put("hasBackgroundImage", p.background.backgroundBitmap != null)
        }
        root.put("canvas", canvasObj)

        // 2. Semantic Layers Analysis
        val layersArr = JSONArray()
        val textLayers = p.layers.filterIsInstance<TextLayer>()
        val maxFontSize = textLayers.maxOfOrNull { it.fontSizeSp } ?: 0f

        val visualRoles = mutableListOf<String>()

        p.layers.forEachIndexed { zIndex, layer ->
            val lObj = JSONObject()
            lObj.put("id", layer.id)
            lObj.put("name", layer.name)
            lObj.put("zIndex", zIndex)
            lObj.put("isVisible", layer.isVisible)
            lObj.put("isLocked", layer.isLocked)
            lObj.put("isSelected", layer.id == session.workingSelectedLayerId)

            val isProtectedProduct = layer.id in session.protectedProductImageIds
            lObj.put("isProductImmunityProtected", isProtectedProduct)

            // Determine semantic role
            val semanticRole = when (layer) {
                is ImageLayer -> {
                    if (isProtectedProduct || layer.name.contains("محصول", ignoreCase = true) || layer.name.contains("product", ignoreCase = true)) {
                        "HERO_PRODUCT_IMAGE"
                    } else {
                        "DECORATIVE_IMAGE_STICKER"
                    }
                }
                is TextLayer -> {
                    when {
                        layer.fontSizeSp >= maxFontSize && maxFontSize > 32f -> "HEADLINE_TEXT"
                        layer.fontSizeSp >= maxFontSize * 0.65f -> "SUBTITLE_TEXT"
                        layer.backgroundPillEnabled -> "CALL_TO_ACTION_PILL"
                        else -> "BODY_TEXT"
                    }
                }
                is ShapeLayer -> {
                    when {
                        zIndex < (p.layers.size / 2) -> "BACKGROUND_CONTAINER_SHAPE"
                        else -> "BADGE_SHAPE"
                    }
                }
                is DrawingLayer -> "FREEHAND_SKETCH"
            }
            lObj.put("semanticRole", semanticRole)
            visualRoles.add(semanticRole)

            // Geometry & Spatial layout
            val geom = JSONObject().apply {
                put("x", layer.x.toDouble())
                put("y", layer.y.toDouble())
                put("scaleX", layer.scaleX.toDouble())
                put("scaleY", layer.scaleY.toDouble())
                put("rotationZ", layer.rotationZ.toDouble())
                put("rotationX", layer.rotationX.toDouble())
                put("rotationY", layer.rotationY.toDouble())
            }
            lObj.put("geometry", geom)

            // Styling & 3D attributes
            val style = JSONObject().apply {
                put("opacity", layer.opacity.toDouble())
                put("strokeEnabled", layer.stroke.enabled)
                put("shadowEnabled", layer.shadow.enabled)
                put("innerShadowEnabled", layer.innerShadow.enabled)
                put("embossEnabled", layer.emboss.enabled)
            }
            when (layer) {
                is TextLayer -> {
                    style.put("text", layer.text)
                    style.put("fontId", layer.fontId)
                    style.put("fontSizeSp", layer.fontSizeSp.toDouble())
                    style.put("colorHex", layer.colorHex)
                    style.put("useGradientFill", layer.useGradientFill)
                    style.put("gradientSecondColorHex", layer.gradientSecondColorHex)
                    style.put("is3dEnabled", layer.is3dEnabled)
                    style.put("depth3d", layer.depth3d)
                    style.put("obliqueAngle3d", layer.obliqueAngle3d.toDouble())
                    style.put("lightAngle3d", layer.lightAngle3d.toDouble())
                }
                is ShapeLayer -> {
                    style.put("shapeKind", layer.shapeKind.name)
                    style.put("fillColorHex", layer.fillColorHex)
                    style.put("is3dExtruded", layer.is3dExtruded)
                    style.put("depth3d", layer.depth3d)
                }
                is ImageLayer -> {
                    style.put("is3dShapeEnabled", layer.is3dShapeEnabled)
                }
                is DrawingLayer -> {
                    style.put("strokeCount", layer.strokes.size)
                }
            }
            lObj.put("style", style)
            layersArr.put(lObj)
        }
        root.put("layers", layersArr)
        root.put("visualHierarchy", JSONArray(visualRoles.distinct()))
        root.put("totalLayerCount", p.layers.size)

        return root
    }

    fun getLayerById(id: String): StudioLayer? =
        session.workingProject.layers.find { it.id == id }

    val currentWorkingProject: StudioProject
        get() = session.workingProject

    val selectedLayerId: String?
        get() = session.workingSelectedLayerId

    val renderedPreviewBitmap: Bitmap?
        get() = session.renderedPreviewBitmap

    val validationReport: AgentValidationReport?
        get() = session.lastValidationReport

    val steps: List<AgentExecutionStep>
        get() = session.steps

    val wasRolledBack: Boolean
        get() = session.wasRolledBack

    val workingLayerCount: Int
        get() = session.workingProject.layers.size

    companion object {
        fun createSession(
            context: Context?,
            userCommand: String = "",
            initialProject: StudioProject,
            initialSelectedLayerId: String?,
            allowProductImageModification: Boolean = false
        ): GeminiCanvasAdapter {
            val session = AgentTransactionSession(
                context = context,
                userCommand = userCommand,
                preTransactionProject = initialProject,
                preTransactionSelectedLayerId = initialSelectedLayerId,
                allowProductImageModification = allowProductImageModification
            )
            return GeminiCanvasAdapter(session)
        }
    }
}
