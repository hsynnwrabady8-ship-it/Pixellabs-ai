package com.example.ui.agent

import android.content.Context
import android.graphics.Bitmap
import com.example.data.BackgroundType
import com.example.data.CanvasBackground
import com.example.data.CanvasPostFx
import com.example.data.LayerEmboss
import com.example.data.LayerShadow
import com.example.data.LayerStroke
import com.example.data.ShapeKind
import com.example.data.ShapeLayer
import com.example.data.StudioFont
import com.example.data.StudioLayer
import com.example.data.StudioProject
import com.example.data.TextLayer
import com.example.data.TexturePreset
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Learned typography, 3D extrusion, stroke, shadow, pill background, and layout coordinates
 * extracted from a role (Headline, Subtitle, or Badge) in a .plp project.
 */
data class LearnedTextRoleStyle(
    val roleName: String = "HEADLINE",
    val fontId: String = StudioFont.LALEZAR.id,
    val customFontPath: String = "",
    val fontSizeSp: Float = 54f,
    val colorHex: String = "#FFFFFF",
    val useGradientFill: Boolean = true,
    val gradientSecondColorHex: String = "#38BDF8",
    val gradientAngle: Float = 45f,
    val texturePresetName: String = TexturePreset.NONE.name,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val letterSpacing: Float = 0f,
    val lineSpacing: Float = 1.15f,
    val strokeEnabled: Boolean = true,
    val strokeColorHex: String = "#090D16",
    val strokeWidth: Float = 6f,
    val shadowEnabled: Boolean = true,
    val shadowColorHex: String = "#CC000000",
    val shadowBlurRadius: Float = 18f,
    val shadowOffsetX: Float = 0f,
    val shadowOffsetY: Float = 10f,
    val outerGlow: Boolean = false,
    val embossEnabled: Boolean = false,
    val embossLightAngle: Float = 45f,
    val embossIntensity: Float = 65f,
    val backgroundPillEnabled: Boolean = false,
    val backgroundPillColorHex: String = "#880F172A",
    val backgroundPillPadding: Float = 18f,
    val backgroundPillRadius: Float = 16f,
    val curveAngle: Float = 0f,
    val is3dEnabled: Boolean = true,
    val depth3d: Int = 18,
    val darken3d: Float = 0.45f,
    val obliqueAngle3d: Float = 135f,
    val isPerspective3d: Boolean = false,
    val shadow3dEnabled: Boolean = false,
    val reflectionEnabled: Boolean = false,
    val x: Float = 0f,
    val y: Float = -0.06f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotationX: Float = 0f,
    val rotationY: Float = 0f,
    val rotationZ: Float = 0f,
    val skewX: Float = 0f,
    val skewY: Float = 0f
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("roleName", roleName)
        put("fontId", fontId)
        put("customFontPath", customFontPath)
        put("fontSizeSp", fontSizeSp.toDouble())
        put("colorHex", colorHex)
        put("useGradientFill", useGradientFill)
        put("gradientSecondColorHex", gradientSecondColorHex)
        put("gradientAngle", gradientAngle.toDouble())
        put("texturePresetName", texturePresetName)
        put("isBold", isBold)
        put("isItalic", isItalic)
        put("letterSpacing", letterSpacing.toDouble())
        put("lineSpacing", lineSpacing.toDouble())
        put("strokeEnabled", strokeEnabled)
        put("strokeColorHex", strokeColorHex)
        put("strokeWidth", strokeWidth.toDouble())
        put("shadowEnabled", shadowEnabled)
        put("shadowColorHex", shadowColorHex)
        put("shadowBlurRadius", shadowBlurRadius.toDouble())
        put("shadowOffsetX", shadowOffsetX.toDouble())
        put("shadowOffsetY", shadowOffsetY.toDouble())
        put("outerGlow", outerGlow)
        put("embossEnabled", embossEnabled)
        put("embossLightAngle", embossLightAngle.toDouble())
        put("embossIntensity", embossIntensity.toDouble())
        put("backgroundPillEnabled", backgroundPillEnabled)
        put("backgroundPillColorHex", backgroundPillColorHex)
        put("backgroundPillPadding", backgroundPillPadding.toDouble())
        put("backgroundPillRadius", backgroundPillRadius.toDouble())
        put("curveAngle", curveAngle.toDouble())
        put("is3dEnabled", is3dEnabled)
        put("depth3d", depth3d)
        put("darken3d", darken3d.toDouble())
        put("obliqueAngle3d", obliqueAngle3d.toDouble())
        put("isPerspective3d", isPerspective3d)
        put("shadow3dEnabled", shadow3dEnabled)
        put("reflectionEnabled", reflectionEnabled)
        put("x", x.toDouble())
        put("y", y.toDouble())
        put("scaleX", scaleX.toDouble())
        put("scaleY", scaleY.toDouble())
        put("rotationX", rotationX.toDouble())
        put("rotationY", rotationY.toDouble())
        put("rotationZ", rotationZ.toDouble())
        put("skewX", skewX.toDouble())
        put("skewY", skewY.toDouble())
    }

    companion object {
        fun fromTextLayer(layer: TextLayer, roleName: String): LearnedTextRoleStyle {
            return LearnedTextRoleStyle(
                roleName = roleName,
                fontId = layer.fontId,
                customFontPath = layer.customFontPath,
                fontSizeSp = layer.fontSizeSp,
                colorHex = layer.colorHex,
                useGradientFill = layer.useGradientFill,
                gradientSecondColorHex = layer.gradientSecondColorHex,
                gradientAngle = layer.gradientAngle,
                texturePresetName = layer.texturePreset.name,
                isBold = layer.isBold,
                isItalic = layer.isItalic,
                letterSpacing = layer.letterSpacing,
                lineSpacing = layer.lineSpacing,
                strokeEnabled = layer.stroke.enabled,
                strokeColorHex = layer.stroke.colorHex,
                strokeWidth = layer.stroke.width,
                shadowEnabled = layer.shadow.enabled,
                shadowColorHex = layer.shadow.colorHex,
                shadowBlurRadius = layer.shadow.blurRadius,
                shadowOffsetX = layer.shadow.offsetX,
                shadowOffsetY = layer.shadow.offsetY,
                outerGlow = layer.shadow.outerGlow,
                embossEnabled = layer.emboss.enabled,
                embossLightAngle = layer.emboss.lightAngle,
                embossIntensity = layer.emboss.intensity,
                backgroundPillEnabled = layer.backgroundPillEnabled,
                backgroundPillColorHex = layer.backgroundPillColorHex,
                backgroundPillPadding = layer.backgroundPillPadding,
                backgroundPillRadius = layer.backgroundPillRadius,
                curveAngle = layer.curveAngle,
                is3dEnabled = layer.is3dEnabled,
                depth3d = layer.depth3d,
                darken3d = layer.darken3d,
                obliqueAngle3d = layer.obliqueAngle3d,
                isPerspective3d = layer.isPerspective3d,
                shadow3dEnabled = layer.shadow3dEnabled,
                reflectionEnabled = layer.reflectionEnabled,
                x = layer.x,
                y = layer.y,
                scaleX = layer.scaleX,
                scaleY = layer.scaleY,
                rotationX = layer.rotationX,
                rotationY = layer.rotationY,
                rotationZ = layer.rotationZ,
                skewX = layer.skewX,
                skewY = layer.skewY
            )
        }

        fun fromJson(json: JSONObject): LearnedTextRoleStyle {
            return LearnedTextRoleStyle(
                roleName = json.optString("roleName", "HEADLINE"),
                fontId = json.optString("fontId", StudioFont.LALEZAR.id),
                customFontPath = json.optString("customFontPath", ""),
                fontSizeSp = json.optDouble("fontSizeSp", 54.0).toFloat(),
                colorHex = json.optString("colorHex", "#FFFFFF"),
                useGradientFill = json.optBoolean("useGradientFill", true),
                gradientSecondColorHex = json.optString("gradientSecondColorHex", "#38BDF8"),
                gradientAngle = json.optDouble("gradientAngle", 45.0).toFloat(),
                texturePresetName = json.optString("texturePresetName", TexturePreset.NONE.name),
                isBold = json.optBoolean("isBold", false),
                isItalic = json.optBoolean("isItalic", false),
                letterSpacing = json.optDouble("letterSpacing", 0.0).toFloat(),
                lineSpacing = json.optDouble("lineSpacing", 1.15).toFloat(),
                strokeEnabled = json.optBoolean("strokeEnabled", true),
                strokeColorHex = json.optString("strokeColorHex", "#090D16"),
                strokeWidth = json.optDouble("strokeWidth", 6.0).toFloat(),
                shadowEnabled = json.optBoolean("shadowEnabled", true),
                shadowColorHex = json.optString("shadowColorHex", "#CC000000"),
                shadowBlurRadius = json.optDouble("shadowBlurRadius", 18.0).toFloat(),
                shadowOffsetX = json.optDouble("shadowOffsetX", 0.0).toFloat(),
                shadowOffsetY = json.optDouble("shadowOffsetY", 10.0).toFloat(),
                outerGlow = json.optBoolean("outerGlow", false),
                embossEnabled = json.optBoolean("embossEnabled", false),
                embossLightAngle = json.optDouble("embossLightAngle", 45.0).toFloat(),
                embossIntensity = json.optDouble("embossIntensity", 65.0).toFloat(),
                backgroundPillEnabled = json.optBoolean("backgroundPillEnabled", false),
                backgroundPillColorHex = json.optString("backgroundPillColorHex", "#880F172A"),
                backgroundPillPadding = json.optDouble("backgroundPillPadding", 18.0).toFloat(),
                backgroundPillRadius = json.optDouble("backgroundPillRadius", 16.0).toFloat(),
                curveAngle = json.optDouble("curveAngle", 0.0).toFloat(),
                is3dEnabled = json.optBoolean("is3dEnabled", true),
                depth3d = json.optInt("depth3d", 18),
                darken3d = json.optDouble("darken3d", 0.45).toFloat(),
                obliqueAngle3d = json.optDouble("obliqueAngle3d", 135.0).toFloat(),
                isPerspective3d = json.optBoolean("isPerspective3d", false),
                shadow3dEnabled = json.optBoolean("shadow3dEnabled", false),
                reflectionEnabled = json.optBoolean("reflectionEnabled", false),
                x = json.optDouble("x", 0.0).toFloat(),
                y = json.optDouble("y", -0.06).toFloat(),
                scaleX = json.optDouble("scaleX", 1.0).toFloat(),
                scaleY = json.optDouble("scaleY", 1.0).toFloat(),
                rotationX = json.optDouble("rotationX", 0.0).toFloat(),
                rotationY = json.optDouble("rotationY", 0.0).toFloat(),
                rotationZ = json.optDouble("rotationZ", 0.0).toFloat(),
                skewX = json.optDouble("skewX", 0.0).toFloat(),
                skewY = json.optDouble("skewY", 0.0).toFloat()
            )
        }
    }
}

/**
 * Learned decorative shape / badge backdrop extracted from a .plp project.
 */
data class LearnedShapeBlueprint(
    val shapeKindName: String = ShapeKind.HEXAGON.name,
    val sizeRatio: Float = 0.36f,
    val fillColorHex: String = "#38BDF8",
    val useGradientFill: Boolean = true,
    val gradientSecondColorHex: String = "#A855F7",
    val strokeEnabled: Boolean = true,
    val strokeColorHex: String = "#FFFFFF",
    val strokeWidth: Float = 4f,
    val opacity: Float = 0.35f,
    val cornerRadius: Float = 24f,
    val is3dExtruded: Boolean = false,
    val depth3d: Int = 12,
    val x: Float = 0f,
    val y: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotationZ: Float = 0f
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("shapeKindName", shapeKindName)
        put("sizeRatio", sizeRatio.toDouble())
        put("fillColorHex", fillColorHex)
        put("useGradientFill", useGradientFill)
        put("gradientSecondColorHex", gradientSecondColorHex)
        put("strokeEnabled", strokeEnabled)
        put("strokeColorHex", strokeColorHex)
        put("strokeWidth", strokeWidth.toDouble())
        put("opacity", opacity.toDouble())
        put("cornerRadius", cornerRadius.toDouble())
        put("is3dExtruded", is3dExtruded)
        put("depth3d", depth3d)
        put("x", x.toDouble())
        put("y", y.toDouble())
        put("scaleX", scaleX.toDouble())
        put("scaleY", scaleY.toDouble())
        put("rotationZ", rotationZ.toDouble())
    }

    companion object {
        fun fromShapeLayer(layer: ShapeLayer): LearnedShapeBlueprint {
            return LearnedShapeBlueprint(
                shapeKindName = layer.shapeKind.name,
                sizeRatio = layer.sizeRatio,
                fillColorHex = layer.fillColorHex,
                useGradientFill = layer.useGradientFill,
                gradientSecondColorHex = layer.gradientSecondColorHex,
                strokeEnabled = layer.stroke.enabled,
                strokeColorHex = layer.stroke.colorHex,
                strokeWidth = layer.stroke.width,
                opacity = layer.opacity,
                cornerRadius = layer.cornerRadius,
                is3dExtruded = layer.is3dExtruded,
                depth3d = layer.depth3d,
                x = layer.x,
                y = layer.y,
                scaleX = layer.scaleX,
                scaleY = layer.scaleY,
                rotationZ = layer.rotationZ
            )
        }

        fun fromJson(json: JSONObject): LearnedShapeBlueprint {
            return LearnedShapeBlueprint(
                shapeKindName = json.optString("shapeKindName", ShapeKind.HEXAGON.name),
                sizeRatio = json.optDouble("sizeRatio", 0.36).toFloat(),
                fillColorHex = json.optString("fillColorHex", "#38BDF8"),
                useGradientFill = json.optBoolean("useGradientFill", true),
                gradientSecondColorHex = json.optString("gradientSecondColorHex", "#A855F7"),
                strokeEnabled = json.optBoolean("strokeEnabled", true),
                strokeColorHex = json.optString("strokeColorHex", "#FFFFFF"),
                strokeWidth = json.optDouble("strokeWidth", 4.0).toFloat(),
                opacity = json.optDouble("opacity", 0.35).toFloat(),
                cornerRadius = json.optDouble("cornerRadius", 24.0).toFloat(),
                is3dExtruded = json.optBoolean("is3dExtruded", false),
                depth3d = json.optInt("depth3d", 12),
                x = json.optDouble("x", 0.0).toFloat(),
                y = json.optDouble("y", 0.0).toFloat(),
                scaleX = json.optDouble("scaleX", 1.0).toFloat(),
                scaleY = json.optDouble("scaleY", 1.0).toFloat(),
                rotationZ = json.optDouble("rotationZ", 0.0).toFloat()
            )
        }
    }
}

/**
 * Complete Style DNA extracted from a .plp project or active StudioProject.
 */
data class PlpLearnedStyleDNA(
    val id: String = UUID.randomUUID().toString(),
    val styleName: String,
    val sourcePlpName: String = "",
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1080,
    val bgPrimaryHex: String = "#0F172A",
    val bgSecondaryHex: String = "#1E1B4B",
    val bgGradientAngle: Float = 135f,
    val bgIsRadial: Boolean = false,
    val vignetteEnabled: Boolean = true,
    val vignetteIntensity: Float = 0.38f,
    val noiseEnabled: Boolean = false,
    val stripesEnabled: Boolean = false,
    val headlineStyle: LearnedTextRoleStyle,
    val subtitleStyle: LearnedTextRoleStyle? = null,
    val badgeTextStyle: LearnedTextRoleStyle? = null,
    val shapeBlueprints: List<LearnedShapeBlueprint> = emptyList(),
    val sourceLayerCount: Int = 3,
    val sampleHeadlineText: String = "",
    val sampleSubtitleText: String = "",
    val learnedAtMs: Long = System.currentTimeMillis()
) {
    val aspectLabel: String
        get() = when {
            canvasHeight > canvasWidth * 1.5f -> "9:16"
            canvasWidth > canvasHeight * 1.5f -> "16:9"
            canvasHeight > canvasWidth * 1.15f -> "4:5"
            else -> "1:1"
        }

    fun toSummaryFa(): String {
        val features = mutableListOf<String>()
        if (headlineStyle.is3dEnabled) features.add("۳بعدی (عمق ${headlineStyle.depth3d})")
        if (headlineStyle.useGradientFill) features.add("گرادیان دو رنگ")
        if (headlineStyle.strokeEnabled) features.add("دورخط ${headlineStyle.strokeWidth.toInt()}px")
        if (subtitleStyle?.backgroundPillEnabled == true || headlineStyle.backgroundPillEnabled) {
            features.add("کادر کپسولی")
        }
        if (shapeBlueprints.isNotEmpty()) features.add("${shapeBlueprints.size} شکل پایه")
        return features.joinToString(" • ").ifBlank { "استایل استاندارد PLP" }
    }

    fun toPromptContextString(): String {
        return """
            ACTIVE LEARNED PLP STYLE DNA ("$styleName", source="$sourcePlpName", aspect=$aspectLabel, ${canvasWidth}x${canvasHeight}):
            - Background Gradient: primary=$bgPrimaryHex, secondary=$bgSecondaryHex, radial=$bgIsRadial, vignette=$vignetteEnabled
            - Headline Style: font=${headlineStyle.fontId}, size=${headlineStyle.fontSizeSp}sp, primaryColor=${headlineStyle.colorHex}, secondaryColor=${headlineStyle.gradientSecondColorHex}, gradient=${headlineStyle.useGradientFill}, 3D=${headlineStyle.is3dEnabled} (depth=${headlineStyle.depth3d}, oblique=${headlineStyle.obliqueAngle3d}), stroke=${headlineStyle.strokeEnabled} (${headlineStyle.strokeColorHex}, width=${headlineStyle.strokeWidth}), shadow=${headlineStyle.shadowEnabled} (${headlineStyle.shadowColorHex}, blur=${headlineStyle.shadowBlurRadius}), pill=${headlineStyle.backgroundPillEnabled} (${headlineStyle.backgroundPillColorHex}), pos=(${headlineStyle.x}, ${headlineStyle.y})
            - Subtitle Style: ${subtitleStyle?.let { "font=${it.fontId}, size=${it.fontSizeSp}sp, color=${it.colorHex}, pill=${it.backgroundPillEnabled} (${it.backgroundPillColorHex}), pos=(${it.x}, ${it.y})" } ?: "None"}
            - Shapes Count: ${shapeBlueprints.size}
        """.trimIndent()
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("styleName", styleName)
        put("sourcePlpName", sourcePlpName)
        put("canvasWidth", canvasWidth)
        put("canvasHeight", canvasHeight)
        put("bgPrimaryHex", bgPrimaryHex)
        put("bgSecondaryHex", bgSecondaryHex)
        put("bgGradientAngle", bgGradientAngle.toDouble())
        put("bgIsRadial", bgIsRadial)
        put("vignetteEnabled", vignetteEnabled)
        put("vignetteIntensity", vignetteIntensity.toDouble())
        put("noiseEnabled", noiseEnabled)
        put("stripesEnabled", stripesEnabled)
        put("headlineStyle", headlineStyle.toJson())
        if (subtitleStyle != null) {
            put("subtitleStyle", subtitleStyle.toJson())
        }
        if (badgeTextStyle != null) {
            put("badgeTextStyle", badgeTextStyle.toJson())
        }
        val shapesArr = JSONArray()
        shapeBlueprints.forEach { shapesArr.put(it.toJson()) }
        put("shapeBlueprints", shapesArr)
        put("sourceLayerCount", sourceLayerCount)
        put("sampleHeadlineText", sampleHeadlineText)
        put("sampleSubtitleText", sampleSubtitleText)
        put("learnedAtMs", learnedAtMs)
    }

    companion object {
        fun fromJson(json: JSONObject): PlpLearnedStyleDNA {
            val shapesArr = json.optJSONArray("shapeBlueprints")
            val shapes = mutableListOf<LearnedShapeBlueprint>()
            if (shapesArr != null) {
                for (i in 0 until shapesArr.length()) {
                    shapesArr.optJSONObject(i)?.let { shapes.add(LearnedShapeBlueprint.fromJson(it)) }
                }
            }
            return PlpLearnedStyleDNA(
                id = json.optString("id", UUID.randomUUID().toString()),
                styleName = json.optString("styleName", "استایل PLP"),
                sourcePlpName = json.optString("sourcePlpName", ""),
                canvasWidth = json.optInt("canvasWidth", 1080),
                canvasHeight = json.optInt("canvasHeight", 1080),
                bgPrimaryHex = json.optString("bgPrimaryHex", "#0F172A"),
                bgSecondaryHex = json.optString("bgSecondaryHex", "#1E1B4B"),
                bgGradientAngle = json.optDouble("bgGradientAngle", 135.0).toFloat(),
                bgIsRadial = json.optBoolean("bgIsRadial", false),
                vignetteEnabled = json.optBoolean("vignetteEnabled", true),
                vignetteIntensity = json.optDouble("vignetteIntensity", 0.38).toFloat(),
                noiseEnabled = json.optBoolean("noiseEnabled", false),
                stripesEnabled = json.optBoolean("stripesEnabled", false),
                headlineStyle = json.optJSONObject("headlineStyle")?.let { LearnedTextRoleStyle.fromJson(it) }
                    ?: LearnedTextRoleStyle(),
                subtitleStyle = json.optJSONObject("subtitleStyle")?.let { LearnedTextRoleStyle.fromJson(it) },
                badgeTextStyle = json.optJSONObject("badgeTextStyle")?.let { LearnedTextRoleStyle.fromJson(it) },
                shapeBlueprints = shapes,
                sourceLayerCount = json.optInt("sourceLayerCount", 3),
                sampleHeadlineText = json.optString("sampleHeadlineText", ""),
                sampleSubtitleText = json.optString("sampleSubtitleText", ""),
                learnedAtMs = json.optLong("learnedAtMs", System.currentTimeMillis())
            )
        }
    }
}

/**
 * Engine for extracting, storing, and reproducing full .plp project style DNA.
 */
object PlpStyleLearningEngine {

    private const val PREFS_NAME = "pixellab_learned_plp_styles"
    private const val KEY_STYLES_JSON = "learned_styles_json_v1"
    private const val KEY_ACTIVE_STYLE_ID = "active_learned_style_id"

    /**
     * Extracts the complete visual DNA (3D extrusion, lighting, stroke, shadow, pill background,
     * layout coordinates, decorative shapes, and background palette) from any StudioProject.
     */
    fun extractStyleDnaFromProject(
        project: StudioProject,
        customStyleName: String? = null
    ): PlpLearnedStyleDNA {
        val textLayers = project.layers.filterIsInstance<TextLayer>().filter { it.isVisible }
            .ifEmpty { project.layers.filterIsInstance<TextLayer>() }
        val shapeLayers = project.layers.filterIsInstance<ShapeLayer>().filter { it.isVisible }

        // Sort text layers by effective visual prominence (fontSizeSp * scaleX)
        val sortedTexts = textLayers.sortedByDescending { it.fontSizeSp * it.scaleX }

        val rawHeadline = sortedTexts.firstOrNull()
        val rawSubtitle = sortedTexts.getOrNull(1)
        val rawBadge = sortedTexts.getOrNull(2)

        val headlineDna = if (rawHeadline != null) {
            LearnedTextRoleStyle.fromTextLayer(rawHeadline, "HEADLINE")
        } else {
            LearnedTextRoleStyle(
                roleName = "HEADLINE",
                fontId = StudioFont.LALEZAR.id,
                fontSizeSp = 56f,
                colorHex = "#FDE047",
                useGradientFill = true,
                gradientSecondColorHex = "#F59E0B",
                strokeEnabled = true,
                strokeColorHex = "#090D16",
                strokeWidth = 7f,
                is3dEnabled = true,
                depth3d = 24,
                y = -0.05f
            )
        }

        val subtitleDna = if (rawSubtitle != null) {
            LearnedTextRoleStyle.fromTextLayer(rawSubtitle, "SUBTITLE")
        } else {
            // Synthesize a complementary subtitle style derived from the headline's palette and position
            LearnedTextRoleStyle(
                roleName = "SUBTITLE",
                fontId = StudioFont.VAZIRMATN.id,
                customFontPath = headlineDna.customFontPath,
                fontSizeSp = (headlineDna.fontSizeSp * 0.48f).coerceIn(20f, 34f),
                colorHex = "#FFFFFF",
                useGradientFill = false,
                gradientSecondColorHex = headlineDna.colorHex,
                strokeEnabled = headlineDna.strokeEnabled,
                strokeColorHex = headlineDna.strokeColorHex,
                strokeWidth = (headlineDna.strokeWidth * 0.55f).coerceIn(2f, 5f),
                shadowEnabled = true,
                shadowColorHex = "#DD000000",
                shadowBlurRadius = 14f,
                backgroundPillEnabled = true,
                backgroundPillColorHex = "#CC111827",
                backgroundPillPadding = 18f,
                backgroundPillRadius = 18f,
                is3dEnabled = false,
                depth3d = 0,
                x = headlineDna.x,
                y = (headlineDna.y + 0.16f).coerceIn(-0.35f, 0.38f)
            )
        }

        val badgeDna = rawBadge?.let { LearnedTextRoleStyle.fromTextLayer(it, "BADGE") }

        val shapesDna = shapeLayers.take(4).map { LearnedShapeBlueprint.fromShapeLayer(it) }

        // Extract background colors (even if the project had a bitmap background, sample its colors)
        val (bgPrimary, bgSecondary) = extractBackgroundPalette(project)

        val cleanTitle = customStyleName?.takeIf { it.isNotBlank() }
            ?: project.plpFileName.removeSuffix(".plp").replace("_", " ").takeIf { it.isNotBlank() }
            ?: project.title.ifBlank { "استایل پروژه PLP" }

        return PlpLearnedStyleDNA(
            id = UUID.randomUUID().toString(),
            styleName = cleanTitle,
            sourcePlpName = project.plpFileName.ifBlank { "${project.title}.plp" },
            canvasWidth = project.canvasWidth,
            canvasHeight = project.canvasHeight,
            bgPrimaryHex = bgPrimary,
            bgSecondaryHex = bgSecondary,
            bgGradientAngle = project.background.gradientAngle,
            bgIsRadial = project.background.isRadialGradient,
            vignetteEnabled = project.postFx.vignetteEnabled,
            vignetteIntensity = project.postFx.vignetteIntensity,
            noiseEnabled = project.postFx.noiseEnabled,
            stripesEnabled = project.postFx.stripesEnabled,
            headlineStyle = headlineDna,
            subtitleStyle = subtitleDna,
            badgeTextStyle = badgeDna,
            shapeBlueprints = shapesDna,
            sourceLayerCount = project.layers.size.coerceAtLeast(1),
            sampleHeadlineText = rawHeadline?.text?.take(32) ?: cleanTitle,
            sampleSubtitleText = rawSubtitle?.text?.take(40) ?: ""
        )
    }

    private fun extractBackgroundPalette(project: StudioProject): Pair<String, String> {
        val bg = project.background
        val bmp = bg.backgroundBitmap
        if (bmp != null && !bmp.isRecycled && bmp.width > 4 && bmp.height > 4) {
            return try {
                val p1 = bmp.getPixel(bmp.width / 4, bmp.height / 4)
                val p2 = bmp.getPixel((bmp.width * 3) / 4, (bmp.height * 3) / 4)
                val hex1 = String.format("#%06X", 0xFFFFFF and p1)
                val hex2 = String.format("#%06X", 0xFFFFFF and p2)
                hex1 to hex2
            } catch (e: Exception) {
                bg.primaryColorHex to bg.secondaryColorHex
            }
        }
        return bg.primaryColorHex to bg.secondaryColorHex
    }

    /**
     * Converts a LearnedTextRoleStyle back into a full TextLayer with new user-specified text.
     */
    fun buildTextLayerFromDna(
        style: LearnedTextRoleStyle,
        text: String,
        layerName: String
    ): TextLayer {
        val tex = runCatching { TexturePreset.valueOf(style.texturePresetName) }.getOrDefault(TexturePreset.NONE)
        return TextLayer(
            id = UUID.randomUUID().toString(),
            name = layerName,
            text = text,
            fontId = style.fontId,
            customFontPath = style.customFontPath,
            fontSizeSp = style.fontSizeSp,
            colorHex = style.colorHex,
            useGradientFill = style.useGradientFill,
            gradientSecondColorHex = style.gradientSecondColorHex,
            gradientAngle = style.gradientAngle,
            texturePreset = if (tex == TexturePreset.CUSTOM_BITMAP) TexturePreset.NONE else tex,
            isBold = style.isBold,
            isItalic = style.isItalic,
            letterSpacing = style.letterSpacing,
            lineSpacing = style.lineSpacing,
            stroke = LayerStroke(
                enabled = style.strokeEnabled,
                colorHex = style.strokeColorHex,
                width = style.strokeWidth
            ),
            shadow = LayerShadow(
                enabled = style.shadowEnabled,
                colorHex = style.shadowColorHex,
                blurRadius = style.shadowBlurRadius,
                offsetX = style.shadowOffsetX,
                offsetY = style.shadowOffsetY,
                outerGlow = style.outerGlow
            ),
            emboss = LayerEmboss(
                enabled = style.embossEnabled,
                lightAngle = style.embossLightAngle,
                intensity = style.embossIntensity
            ),
            backgroundPillEnabled = style.backgroundPillEnabled,
            backgroundPillColorHex = style.backgroundPillColorHex,
            backgroundPillPadding = style.backgroundPillPadding,
            backgroundPillPaddingLeft = style.backgroundPillPadding,
            backgroundPillPaddingRight = style.backgroundPillPadding,
            backgroundPillRadius = style.backgroundPillRadius,
            curveAngle = style.curveAngle,
            is3dEnabled = style.is3dEnabled,
            depth3d = style.depth3d,
            darken3d = style.darken3d,
            obliqueAngle3d = style.obliqueAngle3d,
            isPerspective3d = style.isPerspective3d,
            shadow3dEnabled = style.shadow3dEnabled,
            reflectionEnabled = style.reflectionEnabled,
            x = style.x,
            y = style.y,
            scaleX = style.scaleX,
            scaleY = style.scaleY,
            rotationX = style.rotationX,
            rotationY = style.rotationY,
            rotationZ = style.rotationZ,
            skewX = style.skewX,
            skewY = style.skewY
        )
    }

    /**
     * Transfers a LearnedTextRoleStyle onto an existing TextLayer while strictly preserving
     * the existing TextLayer's text content and optionally its position.
     */
    fun transferRoleStyleToExistingTextLayer(
        existing: TextLayer,
        style: LearnedTextRoleStyle,
        applyLearnedPosition: Boolean = false
    ): TextLayer {
        val tex = runCatching { TexturePreset.valueOf(style.texturePresetName) }.getOrDefault(existing.texturePreset)
        return existing.copy(
            fontId = style.fontId,
            customFontPath = style.customFontPath.ifBlank { existing.customFontPath },
            fontSizeSp = style.fontSizeSp,
            colorHex = style.colorHex,
            useGradientFill = style.useGradientFill,
            gradientSecondColorHex = style.gradientSecondColorHex,
            gradientAngle = style.gradientAngle,
            texturePreset = if (tex == TexturePreset.CUSTOM_BITMAP) existing.texturePreset else tex,
            isBold = style.isBold,
            isItalic = style.isItalic,
            letterSpacing = style.letterSpacing,
            lineSpacing = style.lineSpacing,
            stroke = existing.stroke.copy(
                enabled = style.strokeEnabled,
                colorHex = style.strokeColorHex,
                width = style.strokeWidth
            ),
            shadow = existing.shadow.copy(
                enabled = style.shadowEnabled,
                colorHex = style.shadowColorHex,
                blurRadius = style.shadowBlurRadius,
                offsetX = style.shadowOffsetX,
                offsetY = style.shadowOffsetY,
                outerGlow = style.outerGlow
            ),
            emboss = existing.emboss.copy(
                enabled = style.embossEnabled,
                lightAngle = style.embossLightAngle,
                intensity = style.embossIntensity
            ),
            backgroundPillEnabled = style.backgroundPillEnabled,
            backgroundPillColorHex = style.backgroundPillColorHex,
            backgroundPillPadding = style.backgroundPillPadding,
            backgroundPillRadius = style.backgroundPillRadius,
            curveAngle = style.curveAngle,
            is3dEnabled = style.is3dEnabled,
            depth3d = style.depth3d,
            darken3d = style.darken3d,
            obliqueAngle3d = style.obliqueAngle3d,
            isPerspective3d = style.isPerspective3d,
            shadow3dEnabled = style.shadow3dEnabled,
            reflectionEnabled = style.reflectionEnabled,
            x = if (applyLearnedPosition) style.x else existing.x,
            y = if (applyLearnedPosition) style.y else existing.y,
            rotationX = style.rotationX,
            rotationY = style.rotationY,
            rotationZ = if (applyLearnedPosition) style.rotationZ else existing.rotationZ,
            skewX = style.skewX,
            skewY = style.skewY,
            isLocked = false
        )
    }

    /**
     * Mode 1: Generates a complete new StudioProject cloned from the learned PLP Style DNA
     * using the user's new headline, subtitle, and optional badge text.
     */
    fun generateNewProjectFromStyle(
        style: PlpLearnedStyleDNA,
        newHeadline: String,
        newSubtitle: String = "",
        newBadgeText: String = ""
    ): StudioProject {
        val cleanHeadline = newHeadline.trim().ifBlank {
            style.sampleHeadlineText.ifBlank { "طراحی با استایل ${style.styleName}" }
        }
        val layers = mutableListOf<StudioLayer>()

        // 1. Reconstruct learned decorative shape layers
        style.shapeBlueprints.forEachIndexed { index, bp ->
            val kind = runCatching { ShapeKind.valueOf(bp.shapeKindName) }.getOrDefault(ShapeKind.HEXAGON)
            layers.add(
                ShapeLayer(
                    id = UUID.randomUUID().toString(),
                    name = "المان استایل ${index + 1} (${kind.labelFa})",
                    shapeKind = kind,
                    sizeRatio = bp.sizeRatio,
                    fillColorHex = bp.fillColorHex,
                    useGradientFill = bp.useGradientFill,
                    gradientSecondColorHex = bp.gradientSecondColorHex,
                    stroke = LayerStroke(
                        enabled = bp.strokeEnabled,
                        colorHex = bp.strokeColorHex,
                        width = bp.strokeWidth
                    ),
                    opacity = bp.opacity,
                    cornerRadius = bp.cornerRadius,
                    is3dExtruded = bp.is3dExtruded,
                    depth3d = bp.depth3d,
                    x = bp.x,
                    y = bp.y,
                    scaleX = bp.scaleX,
                    scaleY = bp.scaleY,
                    rotationZ = bp.rotationZ
                )
            )
        }

        // 2. Reconstruct main headline with learned DNA
        layers.add(
            buildTextLayerFromDna(
                style = style.headlineStyle,
                text = cleanHeadline,
                layerName = "تیتر اصلی (${style.styleName})"
            )
        )

        // 3. Reconstruct subtitle if provided or if source project had a subtitle
        val effectiveSubtitle = newSubtitle.trim()
        if (effectiveSubtitle.isNotBlank() && style.subtitleStyle != null) {
            layers.add(
                buildTextLayerFromDna(
                    style = style.subtitleStyle,
                    text = effectiveSubtitle,
                    layerName = "زیرنویس (${style.styleName})"
                )
            )
        }

        // 4. Reconstruct optional badge/tag text layer
        val effectiveBadge = newBadgeText.trim()
        if (effectiveBadge.isNotBlank() && style.badgeTextStyle != null) {
            layers.add(
                buildTextLayerFromDna(
                    style = style.badgeTextStyle,
                    text = effectiveBadge,
                    layerName = "نشان (${style.styleName})"
                )
            )
        }

        return StudioProject(
            id = UUID.randomUUID().toString(),
            title = "${style.styleName} - ${cleanHeadline.take(18)}",
            canvasWidth = style.canvasWidth,
            canvasHeight = style.canvasHeight,
            background = CanvasBackground(
                type = BackgroundType.GRADIENT,
                primaryColorHex = style.bgPrimaryHex,
                secondaryColorHex = style.bgSecondaryHex,
                gradientAngle = style.bgGradientAngle,
                isRadialGradient = style.bgIsRadial,
                backgroundBitmap = null
            ),
            postFx = CanvasPostFx(
                vignetteEnabled = style.vignetteEnabled,
                vignetteIntensity = style.vignetteIntensity,
                noiseEnabled = style.noiseEnabled,
                stripesEnabled = style.stripesEnabled
            ),
            layers = layers
        )
    }

    /**
     * Mode 2: Transfers the learned PLP Style DNA onto the existing layers on the current canvas
     * WITHOUT changing the user's text strings.
     */
    fun applyStyleDnaToCurrentCanvas(
        style: PlpLearnedStyleDNA,
        currentProject: StudioProject,
        applyLayoutPositions: Boolean = false,
        applyBackgroundAndFx: Boolean = true
    ): StudioProject {
        val textLayers = currentProject.layers.filterIsInstance<TextLayer>()
        val sortedBySize = textLayers.sortedByDescending { it.fontSizeSp * it.scaleX }
        val primaryTextId = sortedBySize.firstOrNull()?.id
        val secondaryTextId = sortedBySize.getOrNull(1)?.id

        var shapeIdx = 0
        val updatedLayers = currentProject.layers.map { layer ->
            when (layer) {
                is TextLayer -> {
                    when (layer.id) {
                        primaryTextId -> transferRoleStyleToExistingTextLayer(
                            existing = layer,
                            style = style.headlineStyle,
                            applyLearnedPosition = applyLayoutPositions
                        )
                        secondaryTextId -> {
                            val subStyle = style.subtitleStyle ?: style.headlineStyle
                            transferRoleStyleToExistingTextLayer(
                                existing = layer,
                                style = subStyle,
                                applyLearnedPosition = applyLayoutPositions
                            )
                        }
                        else -> {
                            val badgeStyle = style.badgeTextStyle ?: style.subtitleStyle ?: style.headlineStyle
                            transferRoleStyleToExistingTextLayer(
                                existing = layer,
                                style = badgeStyle,
                                applyLearnedPosition = false
                            )
                        }
                    }
                }
                is ShapeLayer -> {
                    val bp = style.shapeBlueprints.getOrNull(shapeIdx++) ?: style.shapeBlueprints.firstOrNull()
                    if (bp != null) {
                        layer.copy(
                            fillColorHex = bp.fillColorHex,
                            useGradientFill = bp.useGradientFill,
                            gradientSecondColorHex = bp.gradientSecondColorHex,
                            stroke = layer.stroke.copy(
                                enabled = bp.strokeEnabled,
                                colorHex = bp.strokeColorHex,
                                width = bp.strokeWidth
                            ),
                            is3dExtruded = bp.is3dExtruded,
                            depth3d = bp.depth3d,
                            isLocked = false
                        )
                    } else layer
                }
                else -> layer
            }
        }

        val nextBackground = if (applyBackgroundAndFx && currentProject.background.backgroundBitmap == null) {
            currentProject.background.copy(
                type = BackgroundType.GRADIENT,
                primaryColorHex = style.bgPrimaryHex,
                secondaryColorHex = style.bgSecondaryHex,
                gradientAngle = style.bgGradientAngle,
                isRadialGradient = style.bgIsRadial
            )
        } else currentProject.background

        val nextPostFx = if (applyBackgroundAndFx) {
            currentProject.postFx.copy(
                vignetteEnabled = style.vignetteEnabled,
                vignetteIntensity = style.vignetteIntensity,
                noiseEnabled = style.noiseEnabled,
                stripesEnabled = style.stripesEnabled
            )
        } else currentProject.postFx

        return currentProject.copy(
            background = nextBackground,
            postFx = nextPostFx,
            layers = updatedLayers
        )
    }

    // =========================================================================
    // PERSISTENCE IN SHARED PREFERENCES (STYLE MEMORY LIBRARY)
    // =========================================================================

    fun loadLearnedStyles(context: Context): List<PlpLearnedStyleDNA> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_STYLES_JSON, null)
        val loaded = mutableListOf<PlpLearnedStyleDNA>()
        if (!raw.isNullOrBlank()) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { loaded.add(PlpLearnedStyleDNA.fromJson(it)) }
                }
            } catch (ignored: Exception) {}
        }
        if (loaded.isEmpty()) {
            val defaultSeeds = createDefaultLearnedPlpStyles()
            saveAllLearnedStyles(context, defaultSeeds)
            return defaultSeeds
        }
        return loaded
    }

    fun saveAllLearnedStyles(context: Context, styles: List<PlpLearnedStyleDNA>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        styles.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_STYLES_JSON, arr.toString()).apply()
    }

    fun saveOrUpdateLearnedStyle(context: Context, style: PlpLearnedStyleDNA): List<PlpLearnedStyleDNA> {
        val existing = loadLearnedStyles(context).toMutableList()
        val idx = existing.indexOfFirst { it.styleName.equals(style.styleName, ignoreCase = true) }
        if (idx >= 0) {
            existing[idx] = style
        } else {
            existing.add(0, style)
        }
        saveAllLearnedStyles(context, existing)
        setActiveStyleId(context, style.id)
        return existing
    }

    fun deleteLearnedStyle(context: Context, styleId: String): List<PlpLearnedStyleDNA> {
        val remaining = loadLearnedStyles(context).filterNot { it.id == styleId }
        saveAllLearnedStyles(context, remaining)
        if (getActiveStyleId(context) == styleId) {
            setActiveStyleId(context, remaining.firstOrNull()?.id)
        }
        return remaining
    }

    fun getActiveStyleId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ACTIVE_STYLE_ID, null)
    }

    fun setActiveStyleId(context: Context, styleId: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACTIVE_STYLE_ID, styleId).apply()
    }

    fun getActiveLearnedStyle(context: Context): PlpLearnedStyleDNA? {
        val all = loadLearnedStyles(context)
        val activeId = getActiveStyleId(context)
        return all.find { it.id == activeId } ?: all.firstOrNull()
    }

    /**
     * Built-in learned PLP Style DNA presets modeled after real Iranian Instagram/YouTube
     * PixelLab (.plp) projects (including the yellow/dark viral Reels cover style).
     */
    fun createDefaultLearnedPlpStyles(): List<PlpLearnedStyleDNA> {
        val viralReelsPlpDna = PlpLearnedStyleDNA(
            id = "plp_dna_viral_reels_yellow",
            styleName = "کاور وایرال اینستا و تلگرام (PLP)",
            sourcePlpName = "موبایل.plp",
            canvasWidth = 1080,
            canvasHeight = 1920,
            bgPrimaryHex = "#FACC15",
            bgSecondaryHex = "#CA8A04",
            bgGradientAngle = 90f,
            bgIsRadial = true,
            vignetteEnabled = true,
            vignetteIntensity = 0.42f,
            headlineStyle = LearnedTextRoleStyle(
                roleName = "HEADLINE",
                fontId = StudioFont.LALEZAR.id,
                fontSizeSp = 56f,
                colorHex = "#FFFFFF",
                useGradientFill = true,
                gradientSecondColorHex = "#FDE047",
                strokeEnabled = true,
                strokeColorHex = "#1C1917",
                strokeWidth = 7f,
                shadowEnabled = true,
                shadowColorHex = "#DD000000",
                shadowBlurRadius = 22f,
                backgroundPillEnabled = true,
                backgroundPillColorHex = "#CC292524",
                backgroundPillPadding = 20f,
                backgroundPillRadius = 22f,
                is3dEnabled = true,
                depth3d = 18,
                obliqueAngle3d = 135f,
                x = 0f,
                y = 0.04f
            ),
            subtitleStyle = LearnedTextRoleStyle(
                roleName = "SUBTITLE",
                fontId = StudioFont.LALEZAR.id,
                fontSizeSp = 40f,
                colorHex = "#FDE047",
                useGradientFill = false,
                gradientSecondColorHex = "#F59E0B",
                strokeEnabled = true,
                strokeColorHex = "#1C1917",
                strokeWidth = 5f,
                shadowEnabled = true,
                shadowColorHex = "#DD000000",
                shadowBlurRadius = 18f,
                backgroundPillEnabled = true,
                backgroundPillColorHex = "#D91C1917",
                backgroundPillPadding = 18f,
                backgroundPillRadius = 20f,
                is3dEnabled = false,
                depth3d = 0,
                x = 0f,
                y = 0.16f
            ),
            shapeBlueprints = listOf(
                LearnedShapeBlueprint(
                    shapeKindName = ShapeKind.BADGE.name,
                    sizeRatio = 0.42f,
                    fillColorHex = "#1C1917",
                    useGradientFill = true,
                    gradientSecondColorHex = "#44403C",
                    strokeEnabled = true,
                    strokeColorHex = "#FDE047",
                    strokeWidth = 4f,
                    opacity = 0.28f,
                    is3dExtruded = true,
                    depth3d = 10,
                    x = 0f,
                    y = 0.09f
                )
            ),
            sourceLayerCount = 19,
            sampleHeadlineText = "اینستا و تلگرامو",
            sampleSubtitleText = "از نت مخفی کن!"
        )

        val luxury3dPlpDna = PlpLearnedStyleDNA(
            id = "plp_dna_royal_3d_poster",
            styleName = "پوستر سه‌بعدی طلایی پیکسل‌لب (PLP)",
            sourcePlpName = "پوستر_پیکسل‌لب_اینستاگرام.plp",
            canvasWidth = 1080,
            canvasHeight = 1080,
            bgPrimaryHex = "#090D16",
            bgSecondaryHex = "#1E1B4B",
            bgGradientAngle = 135f,
            bgIsRadial = false,
            vignetteEnabled = true,
            vignetteIntensity = 0.38f,
            headlineStyle = LearnedTextRoleStyle(
                roleName = "HEADLINE",
                fontId = StudioFont.LALEZAR.id,
                fontSizeSp = 58f,
                colorHex = "#FDE047",
                useGradientFill = true,
                gradientSecondColorHex = "#F59E0B",
                strokeEnabled = true,
                strokeColorHex = "#451A03",
                strokeWidth = 8f,
                shadowEnabled = true,
                shadowColorHex = "#F59E0B",
                shadowBlurRadius = 24f,
                embossEnabled = true,
                is3dEnabled = true,
                depth3d = 28,
                obliqueAngle3d = 135f,
                x = 0f,
                y = -0.06f
            ),
            subtitleStyle = LearnedTextRoleStyle(
                roleName = "SUBTITLE",
                fontId = StudioFont.VAZIRMATN.id,
                fontSizeSp = 25f,
                colorHex = "#F8FAFC",
                useGradientFill = false,
                strokeEnabled = false,
                backgroundPillEnabled = true,
                backgroundPillColorHex = "#CC0F172A",
                backgroundPillPadding = 18f,
                backgroundPillRadius = 20f,
                is3dEnabled = false,
                depth3d = 0,
                x = 0f,
                y = 0.20f
            ),
            shapeBlueprints = listOf(
                LearnedShapeBlueprint(
                    shapeKindName = ShapeKind.HEXAGON.name,
                    sizeRatio = 0.45f,
                    fillColorHex = "#38BDF8",
                    useGradientFill = true,
                    gradientSecondColorHex = "#7E22CE",
                    opacity = 0.32f,
                    is3dExtruded = true,
                    depth3d = 12,
                    x = 0f,
                    y = -0.04f
                )
            ),
            sourceLayerCount = 5,
            sampleHeadlineText = "پیکسل‌لب هوش مصنوعی",
            sampleSubtitleText = "طراحی سه‌بعدی حرفه‌ای"
        )

        return listOf(viralReelsPlpDna, luxury3dPlpDna)
    }
}
