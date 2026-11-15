package com.example.data

import android.graphics.Bitmap
import androidx.annotation.FontRes
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.UUID

/**
 * Cloud-synced Firestore representation of a PixelLab AI Studio project.
 * Uses native Firestore types, @DocumentId, and @ServerTimestamp.
 */
@IgnoreExtraProperties
data class StudioProjectDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "پروژه جدید",
    val canvasWidth: Long = 1080L,
    val canvasHeight: Long = 1080L,
    val backgroundType: String = "GRADIENT",
    val backgroundColorHex: String = "#0F1117",
    val backgroundSecondaryHex: String = "#1E1B4B",
    val layerCount: Long = 1L,
    val layerTitles: List<String> = emptyList(),
    val primaryText: String = "PixelLab AI",
    val fontId: String = "lalezar",
    val accentColorHex: String = "#38BDF8",
    val is3dEnabled: Boolean = true,
    val depth3d: Long = 20L,
    val curveAngle: Long = 0L,
    val plpFileName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class BackgroundType {
    COLOR,
    GRADIENT,
    TRANSPARENT,
    PRESET_NEBULA,
    IMAGE
}

enum class LayerBlendMode(val labelFa: String) {
    NORMAL("عادی (Normal)"),
    MULTIPLY("ضرب (Multiply)"),
    SCREEN("اسکرین (Screen)"),
    OVERLAY("اورلی (Overlay)"),
    ADD("درخشش (Add)")
}

enum class ShapeKind(val labelFa: String) {
    RECTANGLE("مربع / مستطیل"),
    CIRCLE("دایره / بیضی"),
    TRIANGLE("مثلث"),
    PENTAGON("پنج‌ضلعی"),
    STAR("ستاره ۵ پر"),
    HEXAGON("شش‌ضلعی"),
    HEART("قلب"),
    BADGE("بج تزیینی"),
    ARROW("پیکان گرافیکی"),
    SPEECH_BUBBLE("حباب گفتگو"),
    BEZIER_PATH("منحنی بزیه (Bezier)")
}

/**
 * PixelLab 6-way Gradient Direction Presets + Multi-stop color bar.
 */
enum class GradientDirectionPreset(val labelFa: String) {
    LINEAR_HORIZ("افقی (چپ به راست)"),
    LINEAR_VERT("عمودی (بالا به پایین)"),
    LINEAR_DIAG_TL_BR("قطری (چپ‌بالا به راست‌پایین)"),
    LINEAR_DIAG_BL_TR("قطری معکوس"),
    RADIAL_CENTER("شعاعی مرکزی"),
    RADIAL_CORNER("شعاعی گوشه")
}

data class GradientStop(
    val position: Float, // 0.0f..1.0f
    val colorHex: String
)

/**
 * PixelLab Bezier Curve Tool node with control handle.
 * Coordinates are normalized (-0.48f..0.48f) within the shape bounding box.
 */
data class BezierNode(
    val x: Float,
    val y: Float,
    val ctrlX: Float = x,
    val ctrlY: Float = y
)

/**
 * PixelLab 4-Point Free Perspective Quad (Matrix.setPolyToPoly).
 * Offsets are normalized (-0.45f..0.45f) relative to layer bounding box width/height.
 */
data class PerspectiveQuad(
    val enabled: Boolean = false,
    val topLeftX: Float = 0f,
    val topLeftY: Float = 0f,
    val topRightX: Float = 0f,
    val topRightY: Float = 0f,
    val bottomRightX: Float = 0f,
    val bottomRightY: Float = 0f,
    val bottomLeftX: Float = 0f,
    val bottomLeftY: Float = 0f
)

/**
 * PixelLab Texture Tool presets + custom gallery bitmap fill inside text/shapes.
 */
enum class TexturePreset(val labelFa: String) {
    NONE("بدون بافت"),
    GOLD_FOIL("ورق طلا (Gold Foil)"),
    SILVER_CHROME("کروم نقره‌ای (Chrome)"),
    CARBON_FIBER("فیبر کربن (Carbon)"),
    CYBER_GRID("شبکه نئون (Cyber Grid)"),
    FIRE_LAVA("گدازه آتشین (Lava)"),
    CUSTOM_BITMAP("تصویر دلخواه گالری")
}

/**
 * PixelLab Mask Tool path modes for slicing/cutting out parts of text or images.
 */
enum class MaskPresetMode(val labelFa: String) {
    NONE("بدون ماسک"),
    BEZIER_CURVE("ماسک منحنی بزیه (Bezier)"),
    WAVE_HORIZ("برش موجی (Wave)"),
    DIAGONAL_SLASH("برش قطری (Diagonal)"),
    BOTTOM_CUT("برش نیمه پایین"),
    TOP_CUT("برش نیمه بالا"),
    CENTER_SPLIT("شکاف میانی (Split)")
}

enum class StudioFont(
    val id: String,
    val displayName: String,
    @param:FontRes val fontRes: Int,
    val sampleText: String
) {
    LALEZAR("lalezar", "لاله‌زار (تیتر ضخیم)", R.font.lalezar, "لاله‌زار"),
    VAZIRMATN("vazirmatn", "وزیرمتن (مدرن)", R.font.vazirmatn, "وزیرمتن"),
    AMIRI("amiri", "امیری (کلاسیک/نسخ)", R.font.amiri, "خط امیری"),
    MONTSERRAT("montserrat", "Montserrat Bold", R.font.montserrat, "Montserrat"),
    PLAYFAIR("playfair", "Playfair Display", R.font.playfair_display, "Playfair"),
    PACIFICO("pacifico", "Pacifico Script", R.font.pacifico, "Pacifico");

    companion object {
        fun fromId(id: String): StudioFont = entries.find { it.id == id } ?: LALEZAR
    }
}

data class CustomUserFont(
    val id: String,
    val displayName: String,
    val filePath: String
)

data class LayerStroke(
    val enabled: Boolean = false,
    val colorHex: String = "#000000",
    val width: Float = 6f
)

data class LayerShadow(
    val enabled: Boolean = true,
    val colorHex: String = "#AA000000",
    val blurRadius: Float = 16f,
    val offsetX: Float = 4f,
    val offsetY: Float = 8f,
    val outerGlow: Boolean = false
)

data class LayerInnerShadow(
    val enabled: Boolean = false,
    val colorHex: String = "#88000000",
    val blurRadius: Float = 8f,
    val offsetX: Float = 2f,
    val offsetY: Float = 2f
)

data class LayerEmboss(
    val enabled: Boolean = false,
    val lightAngle: Float = 45f,
    val intensity: Float = 65f,
    val ambientLight: Float = 50f,
    val specularHardness: Float = 40f,
    val bevelWidth: Float = 6f
)

data class Point2D(
    val x: Float,
    val y: Float
)

data class DrawnStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Point2D> = emptyList(),
    val colorHex: String = "#38BDF8",
    val strokeWidth: Float = 12f,
    val smoothness: Float = 0.5f,
    val isDashed: Boolean = false,
    val neonGlow: Boolean = true,
    val isEraser: Boolean = false
)

enum class TextAlignMode(val labelFa: String) {
    RIGHT("راست‌چین"),
    CENTER("وسط‌چین"),
    LEFT("چپ‌چین"),
    JUSTIFY("هم‌تراز دوطرفه")
}

enum class CropAspectPreset(
    val labelFa: String,
    val ratioWidth: Float?,
    val ratioHeight: Float?
) {
    FREE("آزاد (Free)", null, null),
    RATIO_1_1("۱:۱", 1f, 1f),
    RATIO_4_5("۴:۵", 4f, 5f),
    RATIO_16_9("۱۶:۹", 16f, 9f),
    RATIO_9_16("۹:۱۶", 9f, 16f)
}

sealed interface StudioLayer {
    val id: String
    val name: String
    val isVisible: Boolean
    val isLocked: Boolean
    val groupId: String? get() = null
    val opacity: Float // 0f..1f
    val blendMode: LayerBlendMode
    val x: Float // Normalized center offset (-0.5f..0.5f relative to canvas)
    val y: Float
    val scaleX: Float
    val scaleY: Float
    val rotationZ: Float // -180f..180f
    val rotationX: Float // 3D X tilt (-75f..75f)
    val rotationY: Float // 3D Y tilt (-75f..75f)
    val skewX: Float // PixelLab Perspective horizontal skew (-0.8f..0.8f)
    val skewY: Float // PixelLab Perspective vertical skew (-0.8f..0.8f)
    val perspectiveQuad: PerspectiveQuad
    val stroke: LayerStroke
    val shadow: LayerShadow
    val innerShadow: LayerInnerShadow
    val emboss: LayerEmboss
}

data class TextColorSpan(
    val start: Int,
    val end: Int,
    val colorHex: String
)

data class TextFontSpan(
    val start: Int,
    val end: Int,
    val fontId: String = "",
    val isBold: Boolean? = null,
    val isItalic: Boolean? = null,
    val isUnderline: Boolean? = null
)

data class TextLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "لایه متن",
    override val isVisible: Boolean = true,
    override val isLocked: Boolean = false,
    override val groupId: String? = null,
    override val opacity: Float = 1f,
    override val blendMode: LayerBlendMode = LayerBlendMode.NORMAL,
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val scaleX: Float = 1f,
    override val scaleY: Float = 1f,
    override val rotationZ: Float = 0f,
    override val rotationX: Float = 0f,
    override val rotationY: Float = 0f,
    override val skewX: Float = 0f,
    override val skewY: Float = 0f,
    override val perspectiveQuad: PerspectiveQuad = PerspectiveQuad(),
    override val stroke: LayerStroke = LayerStroke(enabled = true, colorHex = "#0B132B", width = 6f),
    override val shadow: LayerShadow = LayerShadow(enabled = true, colorHex = "#CC000000", blurRadius = 18f, offsetX = 0f, offsetY = 10f),
    override val innerShadow: LayerInnerShadow = LayerInnerShadow(),
    override val emboss: LayerEmboss = LayerEmboss(),
    val text: String = "پیکسل لب هوش مصنوعی",
    val fontId: String = StudioFont.LALEZAR.id,
    val customFontPath: String = "",
    val fontSizeSp: Float = 54f,
    val colorHex: String = "#FFFFFF",
    val colorSpans: List<TextColorSpan> = emptyList(),
    val fontSpans: List<TextFontSpan> = emptyList(),
    val useGradientFill: Boolean = true,
    val gradientSecondColorHex: String = "#38BDF8",
    val gradientAngle: Float = 45f,
    val gradientDirection: GradientDirectionPreset = GradientDirectionPreset.LINEAR_DIAG_TL_BR,
    val gradientStops: List<GradientStop> = emptyList(),
    val paddingLeft: Float = 0f,
    val paddingRight: Float = 0f,
    // Beyond-PixelLab: Double Stroke (Outer Contour for Thumbnails/Reels)
    val secondStrokeEnabled: Boolean = false,
    val secondStrokeColorHex: String = "#FDE047",
    val secondStrokeWidth: Float = 10f,
    // Beyond-PixelLab: RGB Chromatic Glitch Split
    val glitchOffsetPx: Float = 0f,
    // PixelLab Texture Fill tool
    val texturePreset: TexturePreset = TexturePreset.NONE,
    val textureScale: Float = 1f,
    val customTextureBitmap: Bitmap? = null,
    // PixelLab Mask tool (with Bezier control handle & angle)
    val maskMode: MaskPresetMode = MaskPresetMode.NONE,
    val maskInvert: Boolean = false,
    val maskOffset: Float = 0f,
    val maskControlX: Float = 0f,
    val maskControlY: Float = 0.25f,
    val maskAngle: Float = 0f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val letterSpacing: Float = 0f,
    val lineSpacing: Float = 1.15f,
    val textAlignCenter: Boolean = true,
    val textAlignMode: TextAlignMode = TextAlignMode.CENTER,
    val backgroundPillEnabled: Boolean = false,
    val backgroundPillColorHex: String = "#880F172A",
    val backgroundPillPadding: Float = 18f,
    val backgroundPillPaddingLeft: Float = 18f,
    val backgroundPillPaddingRight: Float = 18f,
    val backgroundPillPaddingTop: Float = 10f,
    val backgroundPillPaddingBottom: Float = 10f,
    val backgroundPillRadius: Float = 16f,
    val backgroundPillStrokeEnabled: Boolean = false,
    val backgroundPillStrokeColorHex: String = "#38BDF8",
    val backgroundPillStrokeWidth: Float = 3f,
    val backgroundPillGlassBlur: Boolean = false,
    // Curve tool (-360..360 degrees)
    val curveAngle: Float = 0f,
    // 3D Text Extrusion & Lighting tool
    val is3dEnabled: Boolean = true,
    val depth3d: Int = 18, // 1..80
    val darken3d: Float = 0.45f, // 0f..0.9f
    val obliqueAngle3d: Float = 135f, // 0f..360f
    val isPerspective3d: Boolean = false,
    val simulateLighting3d: Boolean = true,
    val lightAngle3d: Float = 45f,
    val lightIntensity3d: Float = 70f,
    val ambientLight3d: Float = 55f,
    val specularHardness3d: Float = 50f,
    val bevel3d: Float = 4f,
    // PixelLab 3D Ground Shadow tool (flat shadow underneath 3D text)
    val shadow3dEnabled: Boolean = false,
    val shadow3dColorHex: String = "#000000",
    val shadow3dAlpha: Float = 0.65f,
    val shadow3dExpand: Float = 1.0f,
    // Reflection tool
    val reflectionEnabled: Boolean = false,
    val reflectionOffset: Float = 12f,
    val reflectionAlpha: Float = 0.35f
) : StudioLayer

data class ShapeLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "لایه شکل",
    override val isVisible: Boolean = true,
    override val isLocked: Boolean = false,
    override val groupId: String? = null,
    override val opacity: Float = 1f,
    override val blendMode: LayerBlendMode = LayerBlendMode.NORMAL,
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val scaleX: Float = 1f,
    override val scaleY: Float = 1f,
    override val rotationZ: Float = 0f,
    override val rotationX: Float = 0f,
    override val rotationY: Float = 0f,
    override val skewX: Float = 0f,
    override val skewY: Float = 0f,
    override val perspectiveQuad: PerspectiveQuad = PerspectiveQuad(),
    override val stroke: LayerStroke = LayerStroke(enabled = true, colorHex = "#FFFFFF", width = 4f),
    override val shadow: LayerShadow = LayerShadow(enabled = true),
    override val innerShadow: LayerInnerShadow = LayerInnerShadow(),
    override val emboss: LayerEmboss = LayerEmboss(),
    val shapeKind: ShapeKind = ShapeKind.HEXAGON,
    val sizeRatio: Float = 0.32f,
    val fillColorHex: String = "#38BDF8",
    val useGradientFill: Boolean = true,
    val gradientSecondColorHex: String = "#A855F7",
    val gradientDirection: GradientDirectionPreset = GradientDirectionPreset.LINEAR_DIAG_TL_BR,
    val gradientStops: List<GradientStop> = emptyList(),
    val texturePreset: TexturePreset = TexturePreset.NONE,
    val textureScale: Float = 1f,
    val cornerRadius: Float = 24f,
    val is3dExtruded: Boolean = false,
    val depth3d: Int = 12,
    val obliqueAngle3d: Float = 135f,
    val darken3d: Float = 0.45f,
    val bezierNodes: List<BezierNode> = listOf(
        BezierNode(x = -0.35f, y = 0.20f, ctrlX = -0.20f, ctrlY = -0.35f),
        BezierNode(x = 0.0f, y = -0.28f, ctrlX = 0.22f, ctrlY = -0.32f),
        BezierNode(x = 0.35f, y = 0.20f, ctrlX = 0.0f, ctrlY = 0.38f)
    ),
    val isBezierClosed: Boolean = true,
    val isBezierFilled: Boolean = true,
    val maskMode: MaskPresetMode = MaskPresetMode.NONE,
    val maskInvert: Boolean = false,
    val maskOffset: Float = 0f,
    val maskControlX: Float = 0f,
    val maskControlY: Float = 0.25f,
    val maskAngle: Float = 0f,
    val glitchOffsetPx: Float = 0f
) : StudioLayer

data class ImageLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "لایه تصویر / استیکر",
    override val isVisible: Boolean = true,
    override val isLocked: Boolean = false,
    override val groupId: String? = null,
    override val opacity: Float = 1f,
    override val blendMode: LayerBlendMode = LayerBlendMode.NORMAL,
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val scaleX: Float = 1f,
    override val scaleY: Float = 1f,
    override val rotationZ: Float = 0f,
    override val rotationX: Float = 0f,
    override val rotationY: Float = 0f,
    override val skewX: Float = 0f,
    override val skewY: Float = 0f,
    override val perspectiveQuad: PerspectiveQuad = PerspectiveQuad(),
    override val stroke: LayerStroke = LayerStroke(enabled = false, colorHex = "#FFFFFF", width = 8f),
    override val shadow: LayerShadow = LayerShadow(enabled = true),
    override val innerShadow: LayerInnerShadow = LayerInnerShadow(),
    override val emboss: LayerEmboss = LayerEmboss(),
    val bitmap: Bitmap? = null,
    val originalBitmap: Bitmap? = null,
    val base64Png: String = "",
    val isCircularMask: Boolean = false,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val maskMode: MaskPresetMode = MaskPresetMode.NONE,
    val maskInvert: Boolean = false,
    val maskOffset: Float = 0f,
    val maskControlX: Float = 0f,
    val maskControlY: Float = 0.25f,
    val maskAngle: Float = 0f,
    val cornerRadius: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    // PixelLab 3D Shape extrusion for transparent PNGs / merged layers
    val is3dShapeEnabled: Boolean = false,
    val depth3d: Int = 12,
    val obliqueAngle3d: Float = 135f,
    val darken3d: Float = 0.45f,
    // Manual Brush Eraser strokes (normalized -0.5..0.5 relative to image bounds)
    val eraserStrokes: List<DrawnStroke> = emptyList(),
    val eraserBrushSize: Float = 24f,
    // Color Overlay (Tint) tool
    val colorOverlayEnabled: Boolean = false,
    val colorOverlayHex: String = "#38BDF8",
    // Erase Color (Chroma Key) tool
    val eraseColorEnabled: Boolean = false,
    val eraseTargetColorHex: String = "#FFFFFF",
    val eraseTolerance: Float = 0.18f, // 0.01f..0.6f
    // Color Filter adjustments per image layer
    val hue: Float = 0f, // -180..180
    val saturation: Float = 1f, // 0..2
    val brightness: Float = 0f, // -100..100
    val contrast: Float = 1f // 0.5..2.0
) : StudioLayer

data class DrawingLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "لایه طراحی قلم",
    override val isVisible: Boolean = true,
    override val isLocked: Boolean = false,
    override val groupId: String? = null,
    override val opacity: Float = 1f,
    override val blendMode: LayerBlendMode = LayerBlendMode.NORMAL,
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val scaleX: Float = 1f,
    override val scaleY: Float = 1f,
    override val rotationZ: Float = 0f,
    override val rotationX: Float = 0f,
    override val rotationY: Float = 0f,
    override val skewX: Float = 0f,
    override val skewY: Float = 0f,
    override val perspectiveQuad: PerspectiveQuad = PerspectiveQuad(),
    override val stroke: LayerStroke = LayerStroke(enabled = false),
    override val shadow: LayerShadow = LayerShadow(enabled = false),
    override val innerShadow: LayerInnerShadow = LayerInnerShadow(),
    override val emboss: LayerEmboss = LayerEmboss(),
    val strokes: List<DrawnStroke> = emptyList()
) : StudioLayer

enum class StudioGuideOverlayMode(val labelFa: String) {
    NONE("بدون خط راهنما"),
    GRID_6X6("شبکه شطرنجی (6×6)"),
    RULE_OF_THIRDS("قانون یک‌سوم (Rule of Thirds)"),
    GOLDEN_RATIO("نسبت طلایی فیبوناچی (Golden Ratio)"),
    YOUTUBE_THUMBNAIL_SAFE("ناحیه امن تامنیل یوتیوب")
}

data class CanvasBackground(
    val type: BackgroundType = BackgroundType.GRADIENT,
    val primaryColorHex: String = "#0F172A",
    val secondaryColorHex: String = "#311042",
    val gradientAngle: Float = 135f,
    val isRadialGradient: Boolean = false,
    val gradientDirection: GradientDirectionPreset = GradientDirectionPreset.LINEAR_DIAG_TL_BR,
    val gradientStops: List<GradientStop> = emptyList(),
    val backgroundBitmap: Bitmap? = null
)

data class CanvasPostFx(
    val vignetteEnabled: Boolean = true,
    val vignetteColorHex: String = "#000000",
    val vignetteIntensity: Float = 0.35f,
    val noiseEnabled: Boolean = false,
    val noiseIntensity: Float = 0.12f,
    val stripesEnabled: Boolean = false,
    val stripesAlpha: Float = 0.08f,
    val stripesSpacing: Float = 14f,
    val brightness: Float = 0f, // -100f..100f
    val contrast: Float = 1f, // 0.5f..1.8f
    val saturation: Float = 1f, // 0f..2f
    val hue: Float = 0f // -180f..180f
)

data class StudioProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "پوستر سه‌بعدی PixelLab",
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1080,
    val background: CanvasBackground = CanvasBackground(),
    val postFx: CanvasPostFx = CanvasPostFx(),
    val layers: List<StudioLayer> = emptyList(),
    val plpFileName: String = "",
    val isAntiGravityActive: Boolean = false,
    val antiGravitySpeed: Float = 1.0f,
    val antiGravitySensorEnabled: Boolean = true,
    val antiGravityParticlesEnabled: Boolean = true,
    val guideOverlayMode: StudioGuideOverlayMode = StudioGuideOverlayMode.NONE
)

data class CanvasAspectPreset(
    val nameFa: String,
    val ratioLabel: String,
    val width: Int,
    val height: Int
)

val DEFAULT_ASPECT_PRESETS = listOf(
    CanvasAspectPreset("مربع (پست اینستاگرام)", "1:1", 1080, 1080),
    CanvasAspectPreset("استوری و ریلز", "9:16", 1080, 1920),
    CanvasAspectPreset("کاور یوتیوب (Full HD)", "16:9", 1920, 1080),
    CanvasAspectPreset("پوستر عمودی (۴:۵)", "4:5", 1080, 1350),
    CanvasAspectPreset("کاور توییتر / بنر", "3:1", 1500, 500),
    CanvasAspectPreset("لوگو و استیکر شفاف", "1:1", 1024, 1024)
)

data class TextStylePreset(
    val id: String,
    val titleFa: String,
    val sampleText: String,
    val font: StudioFont,
    val primaryHex: String,
    val secondaryHex: String,
    val strokeHex: String,
    val strokeWidth: Float,
    val is3d: Boolean,
    val depth3d: Int,
    val curveAngle: Float = 0f,
    val emboss: Boolean = false,
    val reflection: Boolean = false
)

val BUILTIN_TEXT_PRESETS = listOf(
    TextStylePreset(
        id = "cyber_gold",
        titleFa = "طلایی سه‌بعدی سلطنتی",
        sampleText = "طراحی لوکس سه‌بعدی",
        font = StudioFont.LALEZAR,
        primaryHex = "#FDE047",
        secondaryHex = "#D97706",
        strokeHex = "#451A03",
        strokeWidth = 8f,
        is3d = true,
        depth3d = 26,
        emboss = true,
        reflection = true
    ),
    TextStylePreset(
        id = "neon_cyan",
        titleFa = "نئون سایبرپانک",
        sampleText = "استودیو جمینای",
        font = StudioFont.LALEZAR,
        primaryHex = "#38BDF8",
        secondaryHex = "#A855F7",
        strokeHex = "#082F49",
        strokeWidth = 6f,
        is3d = true,
        depth3d = 18,
        reflection = true
    ),
    TextStylePreset(
        id = "curved_badge",
        titleFa = "مهر و لوگوی منحنی",
        sampleText = "PIXELLAB PRO STUDIO",
        font = StudioFont.MONTSERRAT,
        primaryHex = "#F8FAFC",
        secondaryHex = "#94A3B8",
        strokeHex = "#0F172A",
        strokeWidth = 5f,
        is3d = false,
        depth3d = 0,
        curveAngle = 140f
    ),
    TextStylePreset(
        id = "persian_calligraphy",
        titleFa = "تایپوگرافی کلاسیک فارسی",
        sampleText = "هنر برتر از گوهر آمد پدید",
        font = StudioFont.AMIRI,
        primaryHex = "#F43F5E",
        secondaryHex = "#FB923C",
        strokeHex = "#1E1B4B",
        strokeWidth = 6f,
        is3d = true,
        depth3d = 14,
        emboss = true
    ),
    TextStylePreset(
        id = "emerald_3d",
        titleFa = "زمرد برجسته",
        sampleText = "کیفیت اولترا ۴K",
        font = StudioFont.VAZIRMATN,
        primaryHex = "#34D399",
        secondaryHex = "#059669",
        strokeHex = "#022C22",
        strokeWidth = 7f,
        is3d = true,
        depth3d = 22
    )
)

/**
 * PixelLab Built-in Quotes Library (بانک جملات آماده پیکسل‌لب)
 */
data class StudioQuoteItem(
    val categoryFa: String,
    val text: String,
    val recommendedFont: StudioFont = StudioFont.LALEZAR
)

val BUILTIN_PIXELLAB_QUOTES = listOf(
    StudioQuoteItem("انگیزشی", "موفقیت از آن کسانی است که یک ثانیه دیرتر ناامید می‌شوند", StudioFont.LALEZAR),
    StudioQuoteItem("انگیزشی", "رویاهایت را نساز، برایشان بجنگ!", StudioFont.LALEZAR),
    StudioQuoteItem("شعر فارسی", "هنر برتر از گوهر آمد پدید", StudioFont.AMIRI),
    StudioQuoteItem("شعر فارسی", "چو ایران نباشد تن من مباد", StudioFont.AMIRI),
    StudioQuoteItem("شعر فارسی", "زندگی صحنه یکتای هنرمندی ماست", StudioFont.AMIRI),
    StudioQuoteItem("فروش و تبلیغات", "تخفیف ویژه ۵۰٪ فقط تا پایان امشب!", StudioFont.LALEZAR),
    StudioQuoteItem("فروش و تبلیغات", "جشنواره فروش بزرگ آغاز شد", StudioFont.LALEZAR),
    StudioQuoteItem("مناسبتی", "یلدایتان پر از لبخند و روشنایی باد", StudioFont.LALEZAR),
    StudioQuoteItem("مناسبتی", "تولدت مبارک بهترین رفیق دنیا", StudioFont.VAZIRMATN),
    StudioQuoteItem("English", "CREATE YOUR OWN FUTURE", StudioFont.MONTSERRAT),
    StudioQuoteItem("English", "Dream Big, Work Hard, Stay Focused", StudioFont.PLAYFAIR),
    StudioQuoteItem("English", "Stay Hungry, Stay Foolish", StudioFont.PACIFICO)
)

/**
 * PixelLab Built-in Stickers & Graphic Badges (استیکرهای آماده پیکسل‌لب)
 */
data class BuiltinStickerItem(
    val id: String,
    val labelFa: String,
    val symbolText: String,
    val primaryHex: String,
    val secondaryHex: String
)

val BUILTIN_PIXELLAB_STICKERS = listOf(
    BuiltinStickerItem("crown", "تاج سلطنتی", "👑", "#FDE047", "#D97706"),
    BuiltinStickerItem("fire", "آتش داغ", "🔥", "#FB923C", "#EF4444"),
    BuiltinStickerItem("bolt", "صاعقه نئون", "⚡", "#38BDF8", "#6366F1"),
    BuiltinStickerItem("diamond", "الماس لوکس", "💎", "#67E8F9", "#0284C7"),
    BuiltinStickerItem("rocket", "موشک استارتاپ", "🚀", "#A855F7", "#EC4899"),
    BuiltinStickerItem("trophy", "جام قهرمانی", "🏆", "#FBBF24", "#B45309"),
    BuiltinStickerItem("target", "هدف دقیق", "🎯", "#F43F5E", "#9F1239"),
    BuiltinStickerItem("star", "ستاره طلایی", "⭐", "#FDE047", "#CA8A04"),
    BuiltinStickerItem("check", "تایید ویژه", "✅", "#34D399", "#059669"),
    BuiltinStickerItem("mic", "پادکست و استودیو", "🎙️", "#C084FC", "#7E22CE")
)

/**
 * Smart Vision & Auto-Layout Suggestion Model
 */
data class AgentLayoutSuggestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val titleFa: String,
    val descriptionFa: String,
    val designHealthScore: Int = 92,
    val balanceTagFa: String = "تعادل طلایی",
    val suggestedProject: StudioProject,
    val visualCritique: String = ""
)

/**
 * State representing an active animated preview on the canvas
 */
data class AgentPreviewState(
    val originalProject: StudioProject,
    val suggestion: AgentLayoutSuggestion,
    val showOriginalComparison: Boolean = false
)

/**
 * Instagram Reels & Story Cover Template Model (کاور ریلز و استوری اینستاگرام)
 */
data class ReelsCoverTemplate(
    val id: String,
    val titleFa: String,
    val categoryFa: String,
    val headline: String,
    val subtitle: String,
    val badgeLabel: String,
    val font: StudioFont,
    val bgPrimaryHex: String,
    val bgSecondaryHex: String,
    val headlinePrimaryHex: String,
    val headlineSecondaryHex: String,
    val headlineStrokeHex: String,
    val badgeShape: ShapeKind,
    val badgeColorHex: String,
    val stickerId: String
)

val BUILTIN_REELS_TEMPLATES = listOf(
    ReelsCoverTemplate(
        id = "reels_tech_ai",
        titleFa = "تکنولوژی و هوش مصنوعی",
        categoryFa = "تکنولوژی",
        headline = "ترفند مخفی هوش مصنوعی",
        subtitle = "آموزش گام‌به‌گام و کاملاً کاربردی",
        badgeLabel = "۲۰۲۶ جدید",
        font = StudioFont.LALEZAR,
        bgPrimaryHex = "#0B0F19",
        bgSecondaryHex = "#1E1B4B",
        headlinePrimaryHex = "#38BDF8",
        headlineSecondaryHex = "#A855F7",
        headlineStrokeHex = "#02131F",
        badgeShape = ShapeKind.HEXAGON,
        badgeColorHex = "#38BDF8",
        stickerId = "rocket"
    ),
    ReelsCoverTemplate(
        id = "reels_vlog_lifestyle",
        titleFa = "بلاگری و ولاگ روزمره",
        categoryFa = "بلاگری",
        headline = "یک روز کامل با من در تهران!",
        subtitle = "نکاتی که هرگز درباره‌اش صحبت نکردم",
        badgeLabel = "ولاگ هفتگی",
        font = StudioFont.VAZIRMATN,
        bgPrimaryHex = "#4C0519",
        bgSecondaryHex = "#831843",
        headlinePrimaryHex = "#FEF08A",
        headlineSecondaryHex = "#F59E0B",
        headlineStrokeHex = "#450A0A",
        badgeShape = ShapeKind.BADGE,
        badgeColorHex = "#F43F5E",
        stickerId = "star"
    ),
    ReelsCoverTemplate(
        id = "reels_business_money",
        titleFa = "کسب‌وکار و درآمد دلاری",
        categoryFa = "کسب‌وکار",
        headline = "۳ راز چندبرابر کردن درآمد",
        subtitle = "فرمول طلایی ویژه تولیدکنندگان محتوا",
        badgeLabel = "نکته طلایی",
        font = StudioFont.LALEZAR,
        bgPrimaryHex = "#042F2E",
        bgSecondaryHex = "#064E3B",
        headlinePrimaryHex = "#FDE047",
        headlineSecondaryHex = "#EAB308",
        headlineStrokeHex = "#022C22",
        badgeShape = ShapeKind.STAR,
        badgeColorHex = "#10B981",
        stickerId = "diamond"
    ),
    ReelsCoverTemplate(
        id = "reels_sale_shop",
        titleFa = "فروشگاهی و تخفیف ویژه",
        categoryFa = "فروشگاهی",
        headline = "حراج شگفت‌انگیز فصل",
        subtitle = "۵۰٪ تخفیف ویژه فقط تا پایان امشب!",
        badgeLabel = "پیشنهاد ویژه",
        font = StudioFont.LALEZAR,
        bgPrimaryHex = "#7F1D1D",
        bgSecondaryHex = "#991B1B",
        headlinePrimaryHex = "#FFFBEB",
        headlineSecondaryHex = "#FCD34D",
        headlineStrokeHex = "#450A0A",
        badgeShape = ShapeKind.BADGE,
        badgeColorHex = "#EF4444",
        stickerId = "fire"
    ),
    ReelsCoverTemplate(
        id = "reels_cinema_story",
        titleFa = "سینمایی و مستند داستانی",
        categoryFa = "سینمایی",
        headline = "حقیقتی که پنهان کردند",
        subtitle = "روایت افشاگرانه از اتفاقات اخیر",
        badgeLabel = "قسمت اول",
        font = StudioFont.AMIRI,
        bgPrimaryHex = "#09090B",
        bgSecondaryHex = "#27272A",
        headlinePrimaryHex = "#FAFAF9",
        headlineSecondaryHex = "#D6D3D1",
        headlineStrokeHex = "#0C0A09",
        badgeShape = ShapeKind.RECTANGLE,
        badgeColorHex = "#E11D48",
        stickerId = "crown"
    )
)
