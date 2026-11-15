package com.example.ui.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.example.data.BackgroundType
import com.example.data.BezierNode
import com.example.data.CanvasBackground
import com.example.data.CanvasPostFx
import com.example.data.DrawingLayer
import com.example.data.GradientDirectionPreset
import com.example.data.GradientStop
import com.example.data.ImageLayer
import com.example.data.LayerBlendMode
import com.example.data.MaskPresetMode
import com.example.data.PerspectiveQuad
import com.example.data.Point2D
import com.example.data.ShapeKind
import com.example.data.ShapeLayer
import com.example.data.StudioFont
import com.example.data.StudioGuideOverlayMode
import com.example.data.StudioLayer
import com.example.data.StudioProject
import com.example.data.TextLayer
import com.example.data.TexturePreset
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Unified Graphic Design Studio Renderer for PixelLab AI.
 * Supports real-time interactive viewport rendering AND offscreen Ultra/4K bitmap export.
 */
object StudioCanvasRenderer {

    private val typefaceCache = mutableMapOf<String, Typeface>()
    private val proceduralTextureCache = mutableMapOf<TexturePreset, Bitmap>()

    // Hardware-managed LruCache for chroma-key (Erase Color) bitmaps
    private val erasedBitmapLruCache = object : android.util.LruCache<String, Bitmap>(24) {
        override fun entryRemoved(evicted: Boolean, key: String?, oldValue: Bitmap?, newValue: Bitmap?) {
            // Let GC collect unreferenced bitmaps without recycling in-flight draw references
        }
    }

    // Reusable pre-allocated graphic helpers to avoid GC thrashing during 60/120 FPS gestures
    private val reusableMatrix = Matrix()
    private val reusableCamera = Camera()
    private val reusableRectF = RectF()
    private val reusablePath = Path()
    private val reusableColorMatrix = ColorMatrix()

    fun parseHexColor(hex: String, fallback: Int = Color.WHITE): Int {
        return try {
            val clean = if (hex.startsWith("#")) hex else "#$hex"
            Color.parseColor(clean)
        } catch (e: Exception) {
            fallback
        }
    }

    fun getTypeface(
        context: Context,
        fontId: String,
        isBold: Boolean,
        isItalic: Boolean,
        customFontPath: String = ""
    ): Typeface {
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val cacheKey = "${fontId}_${customFontPath}_$style"
        typefaceCache[cacheKey]?.let { return it }

        val baseTypeface = if (customFontPath.isNotBlank()) {
            try {
                val file = File(customFontPath)
                if (file.exists()) Typeface.createFromFile(file) else null
            } catch (e: Exception) {
                null
            }
        } else {
            null
        } ?: try {
            val studioFont = StudioFont.fromId(fontId)
            ResourcesCompat.getFont(context, studioFont.fontRes) ?: Typeface.DEFAULT
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

        val resolved = Typeface.create(baseTypeface, style)
        typefaceCache[cacheKey] = resolved
        return resolved
    }

    /**
     * Generates or retrieves procedural bitmap textures for the PixelLab Texture Tool.
     */
    private fun getProceduralTextureBitmap(preset: TexturePreset): Bitmap? {
        if (preset == TexturePreset.NONE || preset == TexturePreset.CUSTOM_BITMAP) return null
        proceduralTextureCache[preset]?.let { if (!it.isRecycled) return it }

        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        when (preset) {
            TexturePreset.GOLD_FOIL -> {
                p.shader = LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    intArrayOf(
                        Color.parseColor("#FEF08A"),
                        Color.parseColor("#F59E0B"),
                        Color.parseColor("#78350F"),
                        Color.parseColor("#FDE047"),
                        Color.parseColor("#B45309")
                    ),
                    floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
                    Shader.TileMode.MIRROR
                )
                c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
                val flakePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val rng = Random(77)
                for (i in 0 until 240) {
                    flakePaint.color = if (rng.nextBoolean()) Color.parseColor("#66FFFBEB") else Color.parseColor("#55451A03")
                    val rx = rng.nextFloat() * size
                    val ry = rng.nextFloat() * size
                    c.drawCircle(rx, ry, rng.nextFloat() * 6f + 1.5f, flakePaint)
                }
            }
            TexturePreset.SILVER_CHROME -> {
                p.shader = LinearGradient(
                    0f, 0f, 0f, size.toFloat(),
                    intArrayOf(
                        Color.parseColor("#F8FAFC"),
                        Color.parseColor("#94A3B8"),
                        Color.parseColor("#1E293B"),
                        Color.parseColor("#E2E8F0"),
                        Color.parseColor("#475569")
                    ),
                    floatArrayOf(0f, 0.3f, 0.5f, 0.58f, 1f),
                    Shader.TileMode.CLAMP
                )
                c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
            }
            TexturePreset.CARBON_FIBER -> {
                c.drawColor(Color.parseColor("#111827"))
                val cell = 16f
                val darkP = Paint().apply { color = Color.parseColor("#1F2937") }
                val shineP = Paint().apply { color = Color.parseColor("#374151") }
                var y = 0f
                var row = 0
                while (y < size) {
                    var x = 0f
                    var col = 0
                    while (x < size) {
                        if ((row + col) % 2 == 0) {
                            c.drawRect(x, y, x + cell, y + cell, darkP)
                            c.drawLine(x, y, x + cell, y + cell, shineP)
                        }
                        x += cell
                        col++
                    }
                    y += cell
                    row++
                }
            }
            TexturePreset.CYBER_GRID -> {
                c.drawColor(Color.parseColor("#090D16"))
                val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#38BDF8")
                    strokeWidth = 2.5f
                }
                val glowP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#A855F7")
                    strokeWidth = 3f
                }
                val step = 32f
                var pos = 0f
                while (pos < size) {
                    c.drawLine(pos, 0f, pos, size.toFloat(), lineP)
                    c.drawLine(0f, pos, size.toFloat(), pos, glowP)
                    pos += step
                }
            }
            TexturePreset.FIRE_LAVA -> {
                p.shader = RadialGradient(
                    size / 2f, size / 2f, size * 0.7f,
                    intArrayOf(
                        Color.parseColor("#FEF08A"),
                        Color.parseColor("#F97316"),
                        Color.parseColor("#DC2626"),
                        Color.parseColor("#450A0A")
                    ),
                    floatArrayOf(0f, 0.35f, 0.7f, 1f),
                    Shader.TileMode.MIRROR
                )
                c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
            }
            else -> Unit
        }

        proceduralTextureCache[preset] = bmp
        return bmp
    }

    private fun buildTextureShader(
        preset: TexturePreset,
        customBitmap: Bitmap?,
        scale: Float
    ): Shader? {
        val bmp = when (preset) {
            TexturePreset.NONE -> null
            TexturePreset.CUSTOM_BITMAP -> customBitmap?.takeIf { !it.isRecycled }
                ?: getProceduralTextureBitmap(TexturePreset.GOLD_FOIL)
            else -> getProceduralTextureBitmap(preset)
        } ?: return null

        val shader = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        val matrix = Matrix()
        val s = scale.coerceIn(0.25f, 4.0f)
        matrix.setScale(s, s)
        shader.setLocalMatrix(matrix)
        return shader
    }

    /**
     * Builds a multi-stop or 2-color Linear/Radial gradient Shader supporting all 6 PixelLab direction presets.
     */
    fun buildGradientShader(
        rect: RectF,
        primaryHex: String,
        secondaryHex: String,
        angleDeg: Float,
        direction: GradientDirectionPreset = GradientDirectionPreset.LINEAR_DIAG_TL_BR,
        stops: List<GradientStop> = emptyList(),
        isRadialOverride: Boolean = false
    ): Shader {
        val sortedStops = stops.sortedBy { it.position.coerceIn(0f, 1f) }
        val colors: IntArray
        val positions: FloatArray?
        if (sortedStops.size >= 2) {
            colors = sortedStops.map { parseHexColor(it.colorHex, Color.WHITE) }.toIntArray()
            positions = sortedStops.map { it.position.coerceIn(0f, 1f) }.toFloatArray()
        } else {
            val c1 = parseHexColor(primaryHex, Color.WHITE)
            val c2 = parseHexColor(secondaryHex, Color.parseColor("#38BDF8"))
            colors = intArrayOf(c1, c2)
            positions = null
        }

        val cx = rect.centerX()
        val cy = rect.centerY()
        val w = rect.width().coerceAtLeast(4f)
        val h = rect.height().coerceAtLeast(4f)

        if (isRadialOverride || direction == GradientDirectionPreset.RADIAL_CENTER) {
            return RadialGradient(
                cx, cy, max(w, h) * 0.65f,
                colors, positions, Shader.TileMode.CLAMP
            )
        }
        if (direction == GradientDirectionPreset.RADIAL_CORNER) {
            return RadialGradient(
                rect.left, rect.top, max(w, h) * 1.15f,
                colors, positions, Shader.TileMode.CLAMP
            )
        }

        val effectiveAngle = when (direction) {
            GradientDirectionPreset.LINEAR_HORIZ -> 0f
            GradientDirectionPreset.LINEAR_VERT -> 90f
            GradientDirectionPreset.LINEAR_DIAG_BL_TR -> 315f
            GradientDirectionPreset.LINEAR_DIAG_TL_BR -> angleDeg
            else -> angleDeg
        }
        val rad = Math.toRadians(effectiveAngle.toDouble())
        val gx = cos(rad).toFloat() * w * 0.5f
        val gy = sin(rad).toFloat() * h * 0.5f
        return LinearGradient(
            cx - gx, cy - gy,
            cx + gx, cy + gy,
            colors, positions, Shader.TileMode.CLAMP
        )
    }

    /**
     * Builds a clipping Path for the PixelLab Mask Tool (including customizable Bezier Curve Mask).
     */
    private fun applyMaskClipIfNeeded(
        canvas: Canvas,
        bounds: RectF,
        maskMode: MaskPresetMode,
        maskInvert: Boolean,
        maskOffset: Float = 0f,
        maskControlX: Float = 0f,
        maskControlY: Float = 0.25f,
        maskAngle: Float = 0f
    ) {
        if (maskMode == MaskPresetMode.NONE) return
        val w = bounds.width()
        val h = bounds.height()
        val cx = bounds.centerX()
        val cy = bounds.centerY() + maskOffset * h * 0.35f

        val maskRegionPath = Path()
        when (maskMode) {
            MaskPresetMode.BEZIER_CURVE -> {
                val ctrlXOffset = maskControlX.coerceIn(-0.6f, 0.6f) * w
                val ctrlYAmplitude = maskControlY.coerceIn(-0.6f, 0.6f) * h
                maskRegionPath.moveTo(bounds.left - 80f, bounds.top - 80f)
                maskRegionPath.lineTo(bounds.right + 80f, bounds.top - 80f)
                maskRegionPath.lineTo(bounds.right + 80f, cy)
                maskRegionPath.cubicTo(
                    cx + w * 0.28f + ctrlXOffset, cy + ctrlYAmplitude,
                    cx - w * 0.28f + ctrlXOffset, cy - ctrlYAmplitude,
                    bounds.left - 80f, cy
                )
                maskRegionPath.close()
                if (abs(maskAngle) > 0.5f) {
                    val rotM = Matrix().apply { setRotate(maskAngle, cx, cy) }
                    maskRegionPath.transform(rotM)
                }
            }
            MaskPresetMode.WAVE_HORIZ -> {
                maskRegionPath.moveTo(bounds.left - 40f, bounds.top - 40f)
                maskRegionPath.lineTo(bounds.right + 40f, bounds.top - 40f)
                maskRegionPath.lineTo(bounds.right + 40f, cy)
                maskRegionPath.cubicTo(
                    cx + w * 0.25f, cy + h * 0.28f,
                    cx - w * 0.25f, cy - h * 0.28f,
                    bounds.left - 40f, cy
                )
                maskRegionPath.close()
            }
            MaskPresetMode.DIAGONAL_SLASH -> {
                maskRegionPath.moveTo(bounds.left - 40f, bounds.top - 40f)
                maskRegionPath.lineTo(bounds.right + 40f, bounds.top - 40f)
                maskRegionPath.lineTo(bounds.left - 40f, bounds.bottom + 40f)
                maskRegionPath.close()
            }
            MaskPresetMode.BOTTOM_CUT -> {
                maskRegionPath.addRect(
                    bounds.left - 40f,
                    bounds.top - 40f,
                    bounds.right + 40f,
                    cy,
                    Path.Direction.CW
                )
            }
            MaskPresetMode.TOP_CUT -> {
                maskRegionPath.addRect(
                    bounds.left - 40f,
                    cy,
                    bounds.right + 40f,
                    bounds.bottom + 40f,
                    Path.Direction.CW
                )
            }
            MaskPresetMode.CENTER_SPLIT -> {
                val gap = h * 0.14f
                maskRegionPath.addRect(
                    bounds.left - 40f,
                    bounds.top - 40f,
                    bounds.right + 40f,
                    cy - gap,
                    Path.Direction.CW
                )
                maskRegionPath.addRect(
                    bounds.left - 40f,
                    cy + gap,
                    bounds.right + 40f,
                    bounds.bottom + 40f,
                    Path.Direction.CW
                )
            }
            MaskPresetMode.NONE -> return
        }

        if (maskInvert) {
            val fullPath = Path().apply {
                addRect(
                    bounds.left - 160f,
                    bounds.top - 160f,
                    bounds.right + 160f,
                    bounds.bottom + 160f,
                    Path.Direction.CW
                )
            }
            fullPath.op(maskRegionPath, Path.Op.DIFFERENCE)
            canvas.clipPath(fullPath)
        } else {
            canvas.clipPath(maskRegionPath)
        }
    }

    /**
     * Renders the entire StudioProject to a high-resolution Bitmap for export or AI inspection.
     */
    fun renderProjectToBitmap(
        context: Context,
        project: StudioProject,
        scaleMultiplier: Float = 1f,
        includeCheckerboardForTransparent: Boolean = false
    ): Bitmap {
        val targetW = (project.canvasWidth * scaleMultiplier).toInt().coerceIn(128, 4096)
        val targetH = (project.canvasHeight * scaleMultiplier).toInt().coerceIn(128, 4096)
        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        renderProjectToCanvas(
            context = context,
            canvas = canvas,
            project = project,
            viewportWidth = targetW.toFloat(),
            viewportHeight = targetH.toFloat(),
            selectedLayerId = null,
            showSelectionHandles = false,
            showGrid = false,
            includeCheckerboard = includeCheckerboardForTransparent
        )
        return output
    }

    /**
     * PixelLab Merge Selected Layers: renders ONLY the selected layer IDs onto a transparent
     * high-resolution bitmap, auto-crops transparent margins, and returns the merged bitmap
     * along with its normalized center offset (x, y) and scale ratio.
     */
    fun renderSubsetLayersToMergedBitmap(
        context: Context,
        project: StudioProject,
        layerIdsToMerge: Set<String>
    ): Triple<Bitmap, Float, Float>? {
        val targetLayers = project.layers.filter { it.id in layerIdsToMerge && it.isVisible }
        if (targetLayers.isEmpty()) return null

        val w = project.canvasWidth.coerceIn(256, 2048)
        val h = project.canvasHeight.coerceIn(256, 2048)
        val fullBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(fullBmp)
        val referenceScale = min(w, h) / 1080f

        targetLayers.forEach { layer ->
            renderSingleLayer(
                context = context,
                canvas = canvas,
                layer = layer,
                viewportWidth = w.toFloat(),
                viewportHeight = h.toFloat(),
                referenceScale = referenceScale,
                isSelected = false,
                floatOffset = null
            )
        }

        // Find non-transparent bounding box so the merged ImageLayer is tightly framed
        val pixels = IntArray(w * h)
        fullBmp.getPixels(pixels, 0, w, 0, 0, w, h)
        var minX = w
        var minY = h
        var maxX = 0
        var maxY = 0
        val step = 2
        var y = 0
        while (y < h) {
            val rowOffset = y * w
            var x = 0
            while (x < w) {
                if ((pixels[rowOffset + x] ushr 24) > 8) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
                x += step
            }
            y += step
        }

        if (maxX <= minX || maxY <= minY) {
            return Triple(fullBmp, 0f, 0f)
        }

        val pad = 16
        val cropL = (minX - pad).coerceAtLeast(0)
        val cropT = (minY - pad).coerceAtLeast(0)
        val cropR = (maxX + pad).coerceAtMost(w)
        val cropB = (maxY + pad).coerceAtMost(h)
        val cropW = (cropR - cropL).coerceAtLeast(32)
        val cropH = (cropB - cropT).coerceAtLeast(32)

        val cropped = Bitmap.createBitmap(fullBmp, cropL, cropT, cropW, cropH)
        val normCenterX = ((cropL + cropW / 2f) / w.toFloat()) - 0.5f
        val normCenterY = ((cropT + cropH / 2f) / h.toFloat()) - 0.5f
        return Triple(cropped, normCenterX.coerceIn(-0.45f, 0.45f), normCenterY.coerceIn(-0.45f, 0.45f))
    }

    /**
     * Live Canvas Eyedropper: samples the exact hex color at normalized coordinates (0f..1f, 0f..1f).
     */
    fun sampleColorHexAtNormalizedPoint(
        context: Context,
        project: StudioProject,
        normX: Float,
        normY: Float
    ): String {
        val sampleBmp = renderProjectToBitmap(
            context = context,
            project = project,
            scaleMultiplier = 0.35f,
            includeCheckerboardForTransparent = false
        )
        val px = (normX.coerceIn(0f, 1f) * (sampleBmp.width - 1)).toInt().coerceIn(0, sampleBmp.width - 1)
        val py = (normY.coerceIn(0f, 1f) * (sampleBmp.height - 1)).toInt().coerceIn(0, sampleBmp.height - 1)
        val color = sampleBmp.getPixel(px, py)
        return String.format("#%02X%02X%02X", Color.red(color), Color.green(color), Color.blue(color))
    }

    /**
     * Smart Image Palette Extractor: extracts 6 distinct vibrant & complementary colors from the current canvas/image.
     */
    fun extractSmartPaletteFromProject(context: Context, project: StudioProject): List<String> {
        val bmp = renderProjectToBitmap(
            context = context,
            project = project,
            scaleMultiplier = 0.25f,
            includeCheckerboardForTransparent = false
        )
        val samplePoints = listOf(
            0.2f to 0.2f,
            0.8f to 0.2f,
            0.5f to 0.35f,
            0.5f to 0.65f,
            0.2f to 0.8f,
            0.8f to 0.8f,
            0.35f to 0.5f,
            0.65f to 0.5f
        )
        val extracted = mutableListOf<String>()
        samplePoints.forEach { (nx, ny) ->
            val x = (nx * (bmp.width - 1)).toInt().coerceIn(0, bmp.width - 1)
            val y = (ny * (bmp.height - 1)).toInt().coerceIn(0, bmp.height - 1)
            val c = bmp.getPixel(x, y)
            val hex = String.format("#%02X%02X%02X", Color.red(c), Color.green(c), Color.blue(c))
            if (hex !in extracted) extracted.add(hex)
        }
        if (extracted.size < 4) {
            extracted.addAll(listOf("#FDE047", "#38BDF8", "#A855F7", "#F43F5E"))
        }
        return extracted.distinct().take(6)
    }

    /**
     * Core rendering pipeline shared between interactive preview and high-res export.
     */
    fun renderProjectToCanvas(
        context: Context,
        canvas: Canvas,
        project: StudioProject,
        viewportWidth: Float,
        viewportHeight: Float,
        selectedLayerId: String?,
        showSelectionHandles: Boolean,
        showGrid: Boolean,
        includeCheckerboard: Boolean = true,
        layerFloatingOffsets: Map<String, com.example.ui.physics.LayerPhysicsOffset> = emptyMap(),
        cosmicParticles: List<com.example.ui.physics.CosmicZeroGParticle> = emptyList(),
        showReelsSafeZone: Boolean = false,
        aiMutatedLayerId: String? = null
    ) {
        val referenceScale = min(viewportWidth, viewportHeight) / 1080f

        // Apply global Brightness / Contrast / Saturation / Hue via layer save if active
        val postFx = project.postFx
        val needsColorFilter = postFx.brightness != 0f || postFx.contrast != 1f ||
            postFx.saturation != 1f || postFx.hue != 0f

        val globalPaint = if (needsColorFilter) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(
                    buildColorAdjustmentMatrix(
                        brightness = postFx.brightness,
                        contrast = postFx.contrast,
                        saturation = postFx.saturation,
                        hue = postFx.hue
                    )
                )
            }
        } else null

        val rootSave = if (globalPaint != null) {
            canvas.saveLayer(0f, 0f, viewportWidth, viewportHeight, globalPaint)
        } else {
            canvas.save()
        }

        // 1. Render Background
        renderBackground(canvas, project.background, viewportWidth, viewportHeight, includeCheckerboard)

        // Cosmic Anti-Gravity Floating Particles (Zero-G space stardust)
        if (project.isAntiGravityActive && cosmicParticles.isNotEmpty()) {
            val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            cosmicParticles.forEach { p ->
                particlePaint.color = parseHexColor(p.colorHex, Color.CYAN)
                particlePaint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
                canvas.drawCircle(
                    p.x * viewportWidth,
                    p.y * viewportHeight,
                    p.radius * referenceScale,
                    particlePaint
                )
            }
        }

        // 2. Render Ordered Layers (Bottom to Top)
        project.layers.forEach { layer ->
            if (layer.isVisible) {
                renderSingleLayer(
                    context = context,
                    canvas = canvas,
                    layer = layer,
                    viewportWidth = viewportWidth,
                    viewportHeight = viewportHeight,
                    referenceScale = referenceScale,
                    isSelected = showSelectionHandles && layer.id == selectedLayerId,
                    isAiMutated = (layer.id == aiMutatedLayerId),
                    floatOffset = layerFloatingOffsets[layer.id]
                )
            }
        }

        canvas.restoreToCount(rootSave)

        // 3. Render Post-FX Overlays (Vignette, Stripes, Film Grain Noise)
        renderPostFxOverlays(canvas, postFx, viewportWidth, viewportHeight)

        // 4. Render Alignment & Snap Grid or Pro Guide Overlay if toggled in studio
        if (showGrid || project.guideOverlayMode != StudioGuideOverlayMode.NONE) {
            renderStudioGrid(canvas, viewportWidth, viewportHeight, project.guideOverlayMode)
        }

        // 5. Render Instagram Reels Safe Zone if active
        if (showReelsSafeZone) {
            renderReelsSafeZone(canvas, viewportWidth, viewportHeight, referenceScale)
        }
    }

    private fun renderReelsSafeZone(canvas: Canvas, w: Float, h: Float, referenceScale: Float) {
        val feedSquareSize = w
        val feedSquareTop = (h - feedSquareSize) / 2f
        val feedSquareBottom = feedSquareTop + feedSquareSize

        val feedBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#E038BDF8")
            strokeWidth = max(2f, 2.5f * referenceScale)
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }

        val overlayDimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#33000000")
        }

        // Top outside feed area
        canvas.drawRect(0f, 0f, w, feedSquareTop, overlayDimPaint)
        // Bottom outside feed area
        canvas.drawRect(0f, feedSquareBottom, w, h, overlayDimPaint)
        // Feed 1:1 box outline
        canvas.drawRect(0f, feedSquareTop, w, feedSquareBottom, feedBoxPaint)

        // Bottom Reels engagement danger zone (~20% from bottom where caption & audio appear)
        val engagementBottomTop = h * 0.80f
        val dangerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#CCF43F5E")
            strokeWidth = max(1.8f, 2f * referenceScale)
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
        }
        val dangerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#22F43F5E")
        }
        canvas.drawRect(0f, engagementBottomTop, w, h, dangerFill)
        canvas.drawRect(0f, engagementBottomTop, w, h, dangerPaint)

        // Labels
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            textSize = max(20f, 24f * referenceScale)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 0f, 2f, Color.BLACK)
        }
        val dangerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FECDD3")
            textSize = max(18f, 20f * referenceScale)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 0f, 2f, Color.BLACK)
        }

        canvas.drawText("کادر نمایش در فید اینستاگرام (1:1)", w / 2f, feedSquareTop + 32f * referenceScale, textPaint)
        canvas.drawText("ناحیه زیرین کپشن و پروفایل (حفظ فضای خالی)", w / 2f, engagementBottomTop + 32f * referenceScale, dangerTextPaint)
    }

    private fun renderBackground(
        canvas: Canvas,
        bg: CanvasBackground,
        w: Float,
        h: Float,
        includeCheckerboard: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (bg.type) {
            BackgroundType.TRANSPARENT -> {
                if (includeCheckerboard) {
                    drawCheckerboard(canvas, w, h)
                }
            }
            BackgroundType.COLOR -> {
                paint.color = parseHexColor(bg.primaryColorHex, Color.parseColor("#0F172A"))
                canvas.drawRect(0f, 0f, w, h, paint)
            }
            BackgroundType.GRADIENT -> {
                paint.shader = buildGradientShader(
                    rect = RectF(0f, 0f, w, h),
                    primaryHex = bg.primaryColorHex,
                    secondaryHex = bg.secondaryColorHex,
                    angleDeg = bg.gradientAngle,
                    direction = bg.gradientDirection,
                    stops = bg.gradientStops,
                    isRadialOverride = bg.isRadialGradient
                )
                canvas.drawRect(0f, 0f, w, h, paint)
            }
            BackgroundType.PRESET_NEBULA -> {
                // Multi-stop luxury nebula studio backdrop
                paint.shader = LinearGradient(
                    0f, 0f, w, h,
                    intArrayOf(
                        Color.parseColor("#090D16"),
                        Color.parseColor("#1E1B4B"),
                        Color.parseColor("#3B0764"),
                        Color.parseColor("#0F172A")
                    ),
                    floatArrayOf(0f, 0.4f, 0.75f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w, h, paint)

                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.75f, h * 0.25f, max(w, h) * 0.55f,
                        intArrayOf(Color.parseColor("#5500B4D8"), Color.TRANSPARENT),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, glowPaint)

                val glowPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.25f, h * 0.8f, max(w, h) * 0.55f,
                        intArrayOf(Color.parseColor("#55A855F7"), Color.TRANSPARENT),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, glowPaint2)
            }
            BackgroundType.IMAGE -> {
                val bmp = bg.backgroundBitmap
                if (bmp != null && !bmp.isRecycled) {
                    val dst = RectF(0f, 0f, w, h)
                    canvas.drawBitmap(bmp, null, dst, paint)
                } else {
                    paint.color = parseHexColor(bg.primaryColorHex, Color.parseColor("#0F172A"))
                    canvas.drawRect(0f, 0f, w, h, paint)
                }
            }
        }
    }

    private fun drawCheckerboard(canvas: Canvas, w: Float, h: Float) {
        val cellSize = max(16f, min(w, h) / 28f)
        val lightPaint = Paint().apply { color = Color.parseColor("#2A2F3D") }
        val darkPaint = Paint().apply { color = Color.parseColor("#1B1F2A") }
        canvas.drawRect(0f, 0f, w, h, darkPaint)
        var y = 0f
        var row = 0
        while (y < h) {
            var x = 0f
            var col = 0
            while (x < w) {
                if ((row + col) % 2 == 0) {
                    canvas.drawRect(x, y, min(x + cellSize, w), min(y + cellSize, h), lightPaint)
                }
                x += cellSize
                col++
            }
            y += cellSize
            row++
        }
    }

    private fun renderSingleLayer(
        context: Context,
        canvas: Canvas,
        layer: StudioLayer,
        viewportWidth: Float,
        viewportHeight: Float,
        referenceScale: Float,
        isSelected: Boolean,
        isAiMutated: Boolean = false,
        floatOffset: com.example.ui.physics.LayerPhysicsOffset? = null
    ) {
        val fx = (floatOffset?.deltaX ?: 0f) * viewportWidth
        val fy = (floatOffset?.deltaY ?: 0f) * viewportHeight
        val fRot = floatOffset?.deltaRotation ?: 0f

        val centerX = viewportWidth * (0.5f + layer.x) + fx
        val centerY = viewportHeight * (0.5f + layer.y) + fy

        val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
            xfermode = when (layer.blendMode) {
                LayerBlendMode.NORMAL -> null
                LayerBlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                LayerBlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                LayerBlendMode.OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
                LayerBlendMode.ADD -> PorterDuffXfermode(PorterDuff.Mode.ADD)
            }
        }

        val saveCount = if (layer.opacity < 0.99f || layer.blendMode != LayerBlendMode.NORMAL) {
            canvas.saveLayer(0f, 0f, viewportWidth, viewportHeight, layerPaint)
        } else {
            canvas.save()
        }

        // Apply 2D + 3D Camera Rotation, Scale, Skew & 4-Point PolyToPoly Perspective around layer center
        if (layer is DrawingLayer) {
            canvas.translate(layer.x * viewportWidth + fx, layer.y * viewportHeight + fy)
            canvas.scale(layer.scaleX, layer.scaleY, viewportWidth / 2f, viewportHeight / 2f)
            canvas.rotate(layer.rotationZ + fRot, viewportWidth / 2f, viewportHeight / 2f)
        } else {
            val matrix = Matrix()
            if (abs(layer.rotationX) > 0.5f || abs(layer.rotationY) > 0.5f) {
                val camera = Camera()
                camera.save()
                camera.setLocation(0f, 0f, -18f)
                camera.rotateX(layer.rotationX)
                camera.rotateY(layer.rotationY)
                camera.getMatrix(matrix)
                camera.restore()
                matrix.preTranslate(-centerX, -centerY)
                matrix.postTranslate(centerX, centerY)
            }
            if (abs(layer.skewX) > 0.01f || abs(layer.skewY) > 0.01f) {
                matrix.postSkew(
                    layer.skewX.coerceIn(-0.85f, 0.85f),
                    layer.skewY.coerceIn(-0.85f, 0.85f),
                    centerX,
                    centerY
                )
            }
            // 4-Point Free Perspective Warp (Matrix.setPolyToPoly)
            val quad = layer.perspectiveQuad
            if (quad.enabled && (
                    abs(quad.topLeftX) > 0.005f || abs(quad.topLeftY) > 0.005f ||
                    abs(quad.topRightX) > 0.005f || abs(quad.topRightY) > 0.005f ||
                    abs(quad.bottomRightX) > 0.005f || abs(quad.bottomRightY) > 0.005f ||
                    abs(quad.bottomLeftX) > 0.005f || abs(quad.bottomLeftY) > 0.005f
                )
            ) {
                val refW = min(viewportWidth, viewportHeight) * 0.48f
                val refH = min(viewportWidth, viewportHeight) * 0.30f
                val l = centerX - refW / 2f
                val t = centerY - refH / 2f
                val r = centerX + refW / 2f
                val b = centerY + refH / 2f
                val src = floatArrayOf(l, t, r, t, r, b, l, b)
                val dst = floatArrayOf(
                    l + quad.topLeftX * refW, t + quad.topLeftY * refH,
                    r + quad.topRightX * refW, t + quad.topRightY * refH,
                    r + quad.bottomRightX * refW, b + quad.bottomRightY * refH,
                    l + quad.bottomLeftX * refW, b + quad.bottomLeftY * refH
                )
                val polyMatrix = Matrix()
                if (polyMatrix.setPolyToPoly(src, 0, dst, 0, 4)) {
                    matrix.postConcat(polyMatrix)
                }
            }
            matrix.postScale(layer.scaleX.coerceAtLeast(0.1f), layer.scaleY.coerceAtLeast(0.1f), centerX, centerY)
            matrix.postRotate(layer.rotationZ + fRot, centerX, centerY)
            canvas.concat(matrix)
        }

        val bounds = when (layer) {
            is TextLayer -> renderTextLayer(context, canvas, layer, centerX, centerY, referenceScale)
            is ShapeLayer -> renderShapeLayer(canvas, layer, centerX, centerY, min(viewportWidth, viewportHeight), isSelected, referenceScale)
            is ImageLayer -> renderImageLayer(canvas, layer, centerX, centerY, min(viewportWidth, viewportHeight), referenceScale)
            is DrawingLayer -> renderDrawingLayer(canvas, layer, viewportWidth, viewportHeight, referenceScale)
        }

        if (isAiMutated && bounds != null) {
            drawAiMutatedHighlightFrame(canvas, bounds, referenceScale)
        } else if (isSelected && bounds != null) {
            drawSelectionFrame(canvas, bounds, referenceScale)
        }

        canvas.restoreToCount(saveCount)
    }

    private fun renderTextLayer(
        context: Context,
        canvas: Canvas,
        layer: TextLayer,
        centerX: Float,
        centerY: Float,
        referenceScale: Float
    ): RectF {
        val scaledTextSize = (layer.fontSizeSp * 1.0f * referenceScale).coerceAtLeast(12f)
        val typeface = getTypeface(
            context = context,
            fontId = layer.fontId,
            isBold = layer.isBold,
            isItalic = layer.isItalic,
            customFontPath = layer.customFontPath
        )

        val effectiveAlignMode = if (layer.textAlignMode == com.example.data.TextAlignMode.CENTER && !layer.textAlignCenter) {
            com.example.data.TextAlignMode.RIGHT
        } else {
            layer.textAlignMode
        }

        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = scaledTextSize
            this.isUnderlineText = layer.isUnderline
            this.letterSpacing = layer.letterSpacing
            this.textAlign = when (effectiveAlignMode) {
                com.example.data.TextAlignMode.CENTER -> Paint.Align.CENTER
                com.example.data.TextAlignMode.RIGHT -> Paint.Align.RIGHT
                com.example.data.TextAlignMode.LEFT -> Paint.Align.LEFT
                com.example.data.TextAlignMode.JUSTIFY -> Paint.Align.RIGHT
            }
        }

        val lines = layer.text.ifBlank { " " }.split("\n")
        val fontMetrics = basePaint.fontMetrics
        val singleLineHeight = (fontMetrics.descent - fontMetrics.ascent) * layer.lineSpacing
        val totalTextHeight = singleLineHeight * lines.size
        val maxLineWidth = lines.maxOfOrNull { basePaint.measureText(it) } ?: scaledTextSize

        val padL = layer.paddingLeft * referenceScale * 1.5f
        val padR = layer.paddingRight * referenceScale * 1.5f
        val halfW = maxLineWidth / 2f + 24f * referenceScale
        val halfH = totalTextHeight / 2f + 20f * referenceScale
        val boundingBox = RectF(
            centerX - halfW - padL,
            centerY - halfH,
            centerX + halfW + padR,
            centerY + halfH
        )

        // Apply PixelLab Mask Tool clipping (including Bezier Curve Mask) if active
        val maskSaveCount = canvas.save()
        applyMaskClipIfNeeded(
            canvas = canvas,
            bounds = boundingBox,
            maskMode = layer.maskMode,
            maskInvert = layer.maskInvert,
            maskOffset = layer.maskOffset,
            maskControlX = layer.maskControlX,
            maskControlY = layer.maskControlY,
            maskAngle = layer.maskAngle
        )

        // 0. PixelLab 3D Ground Shadow (سایه کف سه‌بعدی در زیر متن)
        if (layer.shadow3dEnabled) {
            val baseFloorColor = parseHexColor(layer.shadow3dColorHex, Color.BLACK)
            val floorAlpha = (layer.shadow3dAlpha.coerceIn(0.05f, 1f) * 235).toInt()
            val floorColor = Color.argb(
                floorAlpha,
                Color.red(baseFloorColor),
                Color.green(baseFloorColor),
                Color.blue(baseFloorColor)
            )
            val expandFactor = layer.shadow3dExpand.coerceIn(0.4f, 2.0f)
            val floorHalfW = halfW * expandFactor
            val floorHalfH = max(10f * referenceScale, totalTextHeight * 0.14f * expandFactor)
            val floorCy = boundingBox.bottom + 8f * referenceScale
            val floorRect = RectF(
                centerX - floorHalfW,
                floorCy - floorHalfH,
                centerX + floorHalfW,
                floorCy + floorHalfH
            )
            val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = floorColor
                maskFilter = BlurMaskFilter((22f * referenceScale * expandFactor).coerceAtLeast(4f), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawOval(floorRect, floorPaint)
        }

        // 1. Background Pill (Text Background tool with 4-sided PixelLab padding + Glassmorphism & Border)
        if (layer.backgroundPillEnabled) {
            val pillLeftPad = layer.backgroundPillPaddingLeft * referenceScale * 1.5f
            val pillRightPad = layer.backgroundPillPaddingRight * referenceScale * 1.5f
            val pillTopPad = layer.backgroundPillPaddingTop * referenceScale * 1.5f
            val pillBottomPad = layer.backgroundPillPaddingBottom * referenceScale * 1.5f
            val pillRect = RectF(
                boundingBox.left - pillLeftPad,
                boundingBox.top - pillTopPad,
                boundingBox.right + pillRightPad,
                boundingBox.bottom + pillBottomPad
            )
            val radius = layer.backgroundPillRadius * referenceScale * 1.5f
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = parseHexColor(layer.backgroundPillColorHex, Color.parseColor("#880F172A"))
            }
            canvas.drawRoundRect(pillRect, radius, radius, pillPaint)

            // Glassmorphism Frosted Specular Top Shine
            if (layer.backgroundPillGlassBlur) {
                val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        pillRect.left, pillRect.top,
                        pillRect.left, pillRect.bottom,
                        intArrayOf(
                            Color.argb(95, 255, 255, 255),
                            Color.argb(20, 255, 255, 255),
                            Color.argb(45, 56, 189, 248)
                        ),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(pillRect, radius, radius, glassPaint)
            }

            // Custom Border around Background Pill
            if (layer.backgroundPillStrokeEnabled && layer.backgroundPillStrokeWidth > 0f) {
                val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = layer.backgroundPillStrokeWidth * referenceScale * 1.4f
                    color = parseHexColor(layer.backgroundPillStrokeColorHex, Color.parseColor("#38BDF8"))
                }
                canvas.drawRoundRect(pillRect, radius, radius, pillBorderPaint)
            }
        }

        val primaryColor = parseHexColor(layer.colorHex, Color.WHITE)

        // Helper function to draw all lines or curved text with a specific Paint at (dx, dy)
        fun drawTextGeometry(paint: Paint, dx: Float, dy: Float) {
            if (abs(layer.curveAngle) > 2f) {
                val combinedText = lines.joinToString(" ")
                val textWidth = max(paint.measureText(combinedText), scaledTextSize * 2f)
                val sweepDegrees = layer.curveAngle.coerceIn(-340f, 340f)
                val sweepRadians = abs(sweepDegrees) * (PI.toFloat() / 180f)
                val radius = (textWidth / sweepRadians).coerceIn(scaledTextSize * 0.8f, 4000f)

                val arcPath = Path()
                if (sweepDegrees > 0) {
                    val oval = RectF(
                        centerX + dx - radius,
                        centerY + dy - radius * 0.3f,
                        centerX + dx + radius,
                        centerY + dy + radius * 1.7f
                    )
                    val startAngle = -90f - sweepDegrees / 2f
                    arcPath.addArc(oval, startAngle, sweepDegrees)
                } else {
                    val absSweep = abs(sweepDegrees)
                    val oval = RectF(
                        centerX + dx - radius,
                        centerY + dy - radius * 1.7f,
                        centerX + dx + radius,
                        centerY + dy + radius * 0.3f
                    )
                    val startAngle = 90f + absSweep / 2f
                    arcPath.addArc(oval, startAngle, -absSweep)
                }
                val arcPaint = Paint(paint).apply {
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawTextOnPath(combinedText, arcPath, 0f, 0f, arcPaint)
            } else {
                val firstBaselineY = centerY - totalTextHeight / 2f - fontMetrics.ascent
                val innerLeft = centerX - maxLineWidth / 2f
                val innerRight = centerX + maxLineWidth / 2f
                lines.forEachIndexed { idx, line ->
                    val yPos = firstBaselineY + idx * singleLineHeight + dy
                    val isLastLine = idx == lines.lastIndex
                    val words = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                    if (effectiveAlignMode == com.example.data.TextAlignMode.JUSTIFY && !isLastLine && words.size > 1 && maxLineWidth > 10f) {
                        val isRtlLine = line.any { it in '\u0600'..'\u06FF' }
                        val wordPaint = Paint(paint).apply {
                            textAlign = if (isRtlLine) Paint.Align.RIGHT else Paint.Align.LEFT
                        }
                        val totalWordsWidth = words.sumOf { wordPaint.measureText(it).toDouble() }.toFloat()
                        val minSpaceWidth = wordPaint.measureText(" ") * (words.size - 1)
                        val extraSpace = (maxLineWidth - totalWordsWidth).coerceAtLeast(minSpaceWidth)
                        val gapWidth = extraSpace / (words.size - 1).toFloat()
                        if (isRtlLine) {
                            var cursorX = innerRight + dx
                            words.forEach { w ->
                                canvas.drawText(w, cursorX, yPos, wordPaint)
                                cursorX -= (wordPaint.measureText(w) + gapWidth)
                            }
                        } else {
                            var cursorX = innerLeft + dx
                            words.forEach { w ->
                                canvas.drawText(w, cursorX, yPos, wordPaint)
                                cursorX += (wordPaint.measureText(w) + gapWidth)
                            }
                        }
                    } else {
                        val linePaint = Paint(paint)
                        val xPos = when (effectiveAlignMode) {
                            com.example.data.TextAlignMode.CENTER -> {
                                linePaint.textAlign = Paint.Align.CENTER
                                centerX + dx
                            }
                            com.example.data.TextAlignMode.RIGHT -> {
                                linePaint.textAlign = Paint.Align.RIGHT
                                innerRight + dx
                            }
                            com.example.data.TextAlignMode.LEFT -> {
                                linePaint.textAlign = Paint.Align.LEFT
                                innerLeft + dx
                            }
                            com.example.data.TextAlignMode.JUSTIFY -> {
                                val isRtlLine = line.any { it in '\u0600'..'\u06FF' }
                                if (isRtlLine) {
                                    linePaint.textAlign = Paint.Align.RIGHT
                                    innerRight + dx
                                } else {
                                    linePaint.textAlign = Paint.Align.LEFT
                                    innerLeft + dx
                                }
                            }
                        }
                        canvas.drawText(line, xPos, yPos, linePaint)
                    }
                }
            }
        }

        // 2. 3D Extrusion Pass (PixelLab 3D Text Depth, Oblique/Perspective & 3D Lighting Simulation)
        if (layer.is3dEnabled && layer.depth3d > 0) {
            val steps = layer.depth3d.coerceIn(1, 80)
            val angleRad = Math.toRadians(layer.obliqueAngle3d.toDouble())
            val stepX = cos(angleRad).toFloat() * 1.6f * referenceScale
            val stepY = sin(angleRad).toFloat() * 1.6f * referenceScale

            val extrudePaint = Paint(basePaint).apply {
                style = if (layer.stroke.enabled) Paint.Style.FILL_AND_STROKE else Paint.Style.FILL
                strokeWidth = if (layer.stroke.enabled) layer.stroke.width * referenceScale * 1.2f else 0f
                strokeJoin = Paint.Join.ROUND
            }

            val baseExtrudeColor = if (layer.stroke.enabled) {
                parseHexColor(layer.stroke.colorHex, primaryColor)
            } else {
                primaryColor
            }

            // Directional 3D Lighting modulation based on angle between extrusion and 3D light source
            val lightAlignment = if (layer.simulateLighting3d) {
                val diffRad = Math.toRadians((layer.obliqueAngle3d - layer.lightAngle3d).toDouble())
                val alignment = (cos(diffRad).toFloat() + 1f) * 0.5f // 0..1
                val ambient = (layer.ambientLight3d / 100f).coerceIn(0.15f, 1f)
                val intensity = (layer.lightIntensity3d / 100f).coerceIn(0.1f, 1f)
                (ambient + alignment * intensity * 0.55f).coerceIn(0.25f, 1.25f)
            } else 1f

            for (step in steps downTo 1) {
                val progress = step.toFloat() / steps.toFloat()
                val darkenFactor = ((1f - layer.darken3d * (0.4f + 0.6f * progress)) * lightAlignment).coerceIn(0.08f, 1f)
                extrudePaint.color = darkenColor(baseExtrudeColor, darkenFactor)

                if (layer.isPerspective3d) {
                    canvas.save()
                    val perspScale = 1f - (step * 0.004f)
                    canvas.scale(perspScale, perspScale, centerX, centerY)
                    drawTextGeometry(extrudePaint, step * stepX * 0.6f, step * stepY * 0.6f)
                    canvas.restore()
                } else {
                    drawTextGeometry(extrudePaint, step * stepX, step * stepY)
                }
            }
        }

        // 3. Drop Shadow / Outer Glow Pass
        if (layer.shadow.enabled) {
            val shadowPaint = Paint(basePaint).apply {
                style = Paint.Style.FILL
                color = parseHexColor(layer.shadow.colorHex, Color.BLACK)
                val blur = (layer.shadow.blurRadius * referenceScale * 1.5f).coerceAtLeast(1f)
                maskFilter = BlurMaskFilter(blur, if (layer.shadow.outerGlow) BlurMaskFilter.Blur.OUTER else BlurMaskFilter.Blur.NORMAL)
            }
            val ox = layer.shadow.offsetX * referenceScale * 1.5f
            val oy = layer.shadow.offsetY * referenceScale * 1.5f
            drawTextGeometry(shadowPaint, ox, oy)
        }

        // 3b. Beyond-PixelLab: RGB Chromatic Aberration / Glitch Pass
        if (layer.glitchOffsetPx > 0.5f) {
            val gOffset = layer.glitchOffsetPx * referenceScale * 1.4f
            val cyanGlitchPaint = Paint(basePaint).apply {
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 2f * referenceScale
                color = Color.parseColor("#CC00F0FF")
            }
            val magentaGlitchPaint = Paint(basePaint).apply {
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 2f * referenceScale
                color = Color.parseColor("#CCFF0055")
            }
            drawTextGeometry(cyanGlitchPaint, -gOffset, -gOffset * 0.25f)
            drawTextGeometry(magentaGlitchPaint, gOffset, gOffset * 0.25f)
        }

        // 3c. Beyond-PixelLab: Second Outer Contour Stroke (دورخط دوم تامنیل و ریلز)
        if (layer.secondStrokeEnabled && layer.secondStrokeWidth > 0f) {
            val innerW = if (layer.stroke.enabled) layer.stroke.width else 0f
            val outerStrokePaint = Paint(basePaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = (innerW + layer.secondStrokeWidth) * referenceScale * 1.85f
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                color = parseHexColor(layer.secondStrokeColorHex, Color.parseColor("#FDE047"))
            }
            drawTextGeometry(outerStrokePaint, 0f, 0f)
        }

        // 4. Primary Stroke / Outline Pass
        if (layer.stroke.enabled && layer.stroke.width > 0f) {
            val strokePaint = Paint(basePaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = layer.stroke.width * referenceScale * 1.8f
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                color = parseHexColor(layer.stroke.colorHex, Color.BLACK)
            }
            drawTextGeometry(strokePaint, 0f, 0f)
        }

        // 5. Main Fill / Texture / Multi-Stop Gradient Pass
        val textureShader = buildTextureShader(
            preset = layer.texturePreset,
            customBitmap = layer.customTextureBitmap,
            scale = layer.textureScale * referenceScale
        )
        val fillPaint = Paint(basePaint).apply {
            style = Paint.Style.FILL
            when {
                textureShader != null -> {
                    shader = textureShader
                }
                layer.useGradientFill -> {
                    shader = buildGradientShader(
                        rect = boundingBox,
                        primaryHex = layer.colorHex,
                        secondaryHex = layer.gradientSecondColorHex,
                        angleDeg = layer.gradientAngle,
                        direction = layer.gradientDirection,
                        stops = layer.gradientStops
                    )
                }
                else -> {
                    color = primaryColor
                }
            }
        }
        drawTextGeometry(fillPaint, 0f, 0f)

        // 5b. PixelLab Partial Text Color & Font Spans (رنگ‌آمیزی و تغییر فونت بخشی از متن با حفظ کامل اتصال حروف فارسی)
        if ((layer.colorSpans.isNotEmpty() || layer.fontSpans.isNotEmpty()) && abs(layer.curveAngle) <= 2f && textureShader == null) {
            val firstBaselineY = centerY - totalTextHeight / 2f - fontMetrics.ascent
            val innerLeft = centerX - maxLineWidth / 2f
            val innerRight = centerX + maxLineWidth / 2f
            var charOffset = 0
            lines.forEachIndexed { idx, line ->
                val lineStart = charOffset
                val lineEnd = charOffset + line.length
                val yPos = firstBaselineY + idx * singleLineHeight
                val isRtlLine = line.any { it in '\u0600'..'\u06FF' }
                val totalLineW = basePaint.measureText(line)
                val (xPos, lineLeft, spanAlign) = when (effectiveAlignMode) {
                    com.example.data.TextAlignMode.CENTER -> Triple(centerX, centerX - totalLineW / 2f, Paint.Align.CENTER)
                    com.example.data.TextAlignMode.RIGHT -> Triple(innerRight, innerRight - totalLineW, Paint.Align.RIGHT)
                    com.example.data.TextAlignMode.LEFT -> Triple(innerLeft, innerLeft, Paint.Align.LEFT)
                    com.example.data.TextAlignMode.JUSTIFY -> if (isRtlLine) {
                        Triple(innerRight, innerRight - totalLineW, Paint.Align.RIGHT)
                    } else {
                        Triple(innerLeft, innerLeft, Paint.Align.LEFT)
                    }
                }
                val lineRight = lineLeft + totalLineW

                // Render Color Spans
                layer.colorSpans.forEach { span ->
                    val segStart = (max(span.start, lineStart) - lineStart).coerceIn(0, line.length)
                    val segEnd = (min(span.end, lineEnd) - lineStart).coerceIn(segStart, line.length)
                    if (segEnd > segStart) {
                        val prefixW = if (segStart > 0) basePaint.measureText(line, 0, segStart) else 0f
                        val upToEndW = basePaint.measureText(line, 0, segEnd)
                        val clipLeft = if (isRtlLine) (lineRight - upToEndW) else (lineLeft + prefixW)
                        val clipRight = if (isRtlLine) (lineRight - prefixW) else (lineLeft + upToEndW)
                        if (clipRight > clipLeft) {
                            canvas.save()
                            canvas.clipRect(
                                clipLeft - 2f * referenceScale,
                                yPos + fontMetrics.ascent - 12f * referenceScale,
                                clipRight + 2f * referenceScale,
                                yPos + fontMetrics.descent + 12f * referenceScale
                            )
                            val spanPaint = Paint(basePaint).apply {
                                style = Paint.Style.FILL
                                textAlign = spanAlign
                                color = parseHexColor(span.colorHex, primaryColor)
                            }
                            canvas.drawText(line, xPos, yPos, spanPaint)
                            canvas.restore()
                        }
                    }
                }

                // Render Font / Style Spans
                layer.fontSpans.forEach { fSpan ->
                    val segStart = (max(fSpan.start, lineStart) - lineStart).coerceIn(0, line.length)
                    val segEnd = (min(fSpan.end, lineEnd) - lineStart).coerceIn(segStart, line.length)
                    if (segEnd > segStart) {
                        val prefixW = if (segStart > 0) basePaint.measureText(line, 0, segStart) else 0f
                        val upToEndW = basePaint.measureText(line, 0, segEnd)
                        val clipLeft = if (isRtlLine) (lineRight - upToEndW) else (lineLeft + prefixW)
                        val clipRight = if (isRtlLine) (lineRight - prefixW) else (lineLeft + upToEndW)
                        if (clipRight > clipLeft) {
                            canvas.save()
                            canvas.clipRect(
                                clipLeft - 2f * referenceScale,
                                yPos + fontMetrics.ascent - 12f * referenceScale,
                                clipRight + 2f * referenceScale,
                                yPos + fontMetrics.descent + 12f * referenceScale
                            )
                            val spanTypeface = getTypeface(
                                context = context,
                                fontId = fSpan.fontId.ifBlank { layer.fontId },
                                isBold = fSpan.isBold ?: layer.isBold,
                                isItalic = fSpan.isItalic ?: layer.isItalic,
                                customFontPath = if (fSpan.fontId.isBlank()) layer.customFontPath else ""
                            )
                            val matchingColor = layer.colorSpans.lastOrNull {
                                it.start <= fSpan.start && it.end >= fSpan.end
                            }?.colorHex
                            val fontSpanPaint = Paint(fillPaint).apply {
                                this.typeface = spanTypeface
                                this.textAlign = spanAlign
                                this.isUnderlineText = fSpan.isUnderline ?: layer.isUnderline
                                if (matchingColor != null) {
                                    this.shader = null
                                    this.color = parseHexColor(matchingColor, primaryColor)
                                }
                            }
                            canvas.drawText(line, xPos, yPos, fontSpanPaint)
                            canvas.restore()
                        }
                    }
                }
                charOffset = lineEnd + 1 // +1 for '\n'
            }
        }

        // 6. Inner Shadow Pass
        if (layer.innerShadow.enabled) {
            val innerPaint = Paint(basePaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(2f, 3.5f * referenceScale)
                color = parseHexColor(layer.innerShadow.colorHex, Color.parseColor("#88000000"))
                val blur = (layer.innerShadow.blurRadius * referenceScale).coerceAtLeast(1f)
                maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
            }
            drawTextGeometry(
                innerPaint,
                layer.innerShadow.offsetX * referenceScale,
                layer.innerShadow.offsetY * referenceScale
            )
        }

        // 7. Emboss / Bevel OR 3D Specular Highlight Pass
        if (layer.emboss.enabled || (layer.is3dEnabled && layer.simulateLighting3d && layer.bevel3d > 0.5f)) {
            val lightDeg = if (layer.emboss.enabled) layer.emboss.lightAngle else layer.lightAngle3d
            val bevelWidthVal = if (layer.emboss.enabled) layer.emboss.bevelWidth else layer.bevel3d
            val specIntensity = if (layer.emboss.enabled) layer.emboss.intensity else layer.specularHardness3d

            val lightRad = Math.toRadians(lightDeg.toDouble())
            val bevelOffset = (bevelWidthVal * 0.4f * referenceScale).coerceAtLeast(1.2f)
            val lx = cos(lightRad).toFloat() * bevelOffset
            val ly = sin(lightRad).toFloat() * bevelOffset
            val highlightAlpha = ((specIntensity / 100f) * 195).toInt().coerceIn(20, 235)

            val specularPaint = Paint(basePaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = (bevelWidthVal * 0.35f * referenceScale).coerceAtLeast(1.5f)
                color = Color.argb(highlightAlpha, 255, 255, 255)
            }
            drawTextGeometry(specularPaint, -lx, -ly)

            val bevelShadowPaint = Paint(basePaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = (bevelWidthVal * 0.35f * referenceScale).coerceAtLeast(1.5f)
                color = Color.argb((highlightAlpha * 0.7f).toInt(), 0, 0, 0)
            }
            drawTextGeometry(bevelShadowPaint, lx, ly)
        }

        // 8. Mirror Reflection Pass
        if (layer.reflectionEnabled) {
            canvas.save()
            val reflectionTop = boundingBox.bottom + layer.reflectionOffset * referenceScale * 2f
            canvas.translate(0f, reflectionTop * 2f - (boundingBox.bottom - boundingBox.height() * 0.2f))
            canvas.scale(1f, -0.65f, centerX, centerY)
            val reflectPaint = Paint(fillPaint).apply {
                alpha = (layer.reflectionAlpha.coerceIn(0.05f, 0.85f) * 255).toInt()
            }
            drawTextGeometry(reflectPaint, 0f, 0f)
            canvas.restore()
        }

        canvas.restoreToCount(maskSaveCount)
        return boundingBox
    }

    private fun renderShapeLayer(
        canvas: Canvas,
        layer: ShapeLayer,
        centerX: Float,
        centerY: Float,
        minCanvasDim: Float,
        isSelected: Boolean = false,
        referenceScale: Float = 1f
    ): RectF {
        val halfSize = (minCanvasDim * layer.sizeRatio * 0.5f).coerceAtLeast(24f)
        val rect = RectF(centerX - halfSize, centerY - halfSize, centerX + halfSize, centerY + halfSize)
        val path = buildShapePath(
            kind = layer.shapeKind,
            rect = rect,
            cornerRadius = layer.cornerRadius,
            bezierNodes = layer.bezierNodes,
            isBezierClosed = layer.isBezierClosed
        )

        val primaryColor = parseHexColor(layer.fillColorHex, Color.parseColor("#38BDF8"))

        // 3D Extrusion for shapes
        if (layer.is3dExtruded && layer.depth3d > 0) {
            val angleRad = Math.toRadians(layer.obliqueAngle3d.toDouble())
            val sx = cos(angleRad).toFloat() * 1.6f * referenceScale
            val sy = sin(angleRad).toFloat() * 1.6f * referenceScale
            val extrudePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = if (layer.shapeKind == ShapeKind.BEZIER_PATH && !layer.isBezierFilled) Paint.Style.STROKE else Paint.Style.FILL
                strokeWidth = layer.stroke.width * 1.5f
            }
            for (d in layer.depth3d downTo 1) {
                val factor = (1f - layer.darken3d * (0.4f + 0.6f * (d.toFloat() / layer.depth3d))).coerceIn(0.15f, 1f)
                extrudePaint.color = darkenColor(primaryColor, factor)
                canvas.save()
                canvas.translate(d * sx, d * sy)
                canvas.drawPath(path, extrudePaint)
                canvas.restore()
            }
        }

        // Shadow
        if (layer.shadow.enabled) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = if (layer.shapeKind == ShapeKind.BEZIER_PATH && !layer.isBezierFilled) Paint.Style.STROKE else Paint.Style.FILL
                strokeWidth = layer.stroke.width * 1.5f
                color = parseHexColor(layer.shadow.colorHex, Color.BLACK)
                maskFilter = BlurMaskFilter(layer.shadow.blurRadius.coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.save()
            canvas.translate(layer.shadow.offsetX, layer.shadow.offsetY)
            canvas.drawPath(path, shadowPaint)
            canvas.restore()
        }

        // RGB Glitch for Shapes
        if (layer.glitchOffsetPx > 0.5f) {
            val go = layer.glitchOffsetPx * referenceScale
            val cPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f * referenceScale
                color = Color.parseColor("#CC00F0FF")
            }
            val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f * referenceScale
                color = Color.parseColor("#CCFF0055")
            }
            canvas.save()
            canvas.translate(-go, 0f)
            canvas.drawPath(path, cPaint)
            canvas.restore()
            canvas.save()
            canvas.translate(go, 0f)
            canvas.drawPath(path, mPaint)
            canvas.restore()
        }

        // Fill or Texture (if not an open/unfilled Bezier curve)
        val shouldFill = layer.shapeKind != ShapeKind.BEZIER_PATH || layer.isBezierFilled
        if (shouldFill) {
            val shapeTextureShader = buildTextureShader(
                preset = layer.texturePreset,
                customBitmap = null,
                scale = layer.textureScale
            )
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                when {
                    shapeTextureShader != null -> {
                        shader = shapeTextureShader
                    }
                    layer.useGradientFill -> {
                        shader = buildGradientShader(
                            rect = rect,
                            primaryHex = layer.fillColorHex,
                            secondaryHex = layer.gradientSecondColorHex,
                            angleDeg = 45f,
                            direction = layer.gradientDirection,
                            stops = layer.gradientStops
                        )
                    }
                    else -> {
                        color = primaryColor
                    }
                }
            }
            canvas.drawPath(path, fillPaint)
        }

        // Stroke
        if ((layer.stroke.enabled && layer.stroke.width > 0f) || (layer.shapeKind == ShapeKind.BEZIER_PATH && !layer.isBezierFilled)) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = (layer.stroke.width.coerceAtLeast(3f)) * 1.5f
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                color = parseHexColor(layer.stroke.colorHex, Color.WHITE)
            }
            canvas.drawPath(path, strokePaint)
        }

        // Draw Bezier Control Nodes & Handles when selected
        if (isSelected && layer.shapeKind == ShapeKind.BEZIER_PATH && layer.bezierNodes.isNotEmpty()) {
            val w = rect.width()
            val h = rect.height()
            val cx = rect.centerX()
            val cy = rect.centerY()
            val tangentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(1.5f, 2f * referenceScale)
                color = Color.parseColor("#FDE047")
                pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
            }
            val nodeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.parseColor("#38BDF8")
            }
            val ctrlFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.parseColor("#F43F5E")
            }
            layer.bezierNodes.forEach { n ->
                val nx = cx + n.x * w
                val ny = cy + n.y * h
                val hx = cx + n.ctrlX * w
                val hy = cy + n.ctrlY * h
                canvas.drawLine(nx, ny, hx, hy, tangentPaint)
                canvas.drawCircle(hx, hy, max(5f, 6.5f * referenceScale), ctrlFill)
                canvas.drawCircle(nx, ny, max(6f, 8f * referenceScale), nodeFill)
            }
        }

        return rect
    }

    private fun buildShapePath(
        kind: ShapeKind,
        rect: RectF,
        cornerRadius: Float,
        bezierNodes: List<BezierNode> = emptyList(),
        isBezierClosed: Boolean = true
    ): Path {
        val path = Path()
        val cx = rect.centerX()
        val cy = rect.centerY()
        val r = rect.width() / 2f

        when (kind) {
            ShapeKind.RECTANGLE -> path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            ShapeKind.CIRCLE -> path.addOval(rect, Path.Direction.CW)
            ShapeKind.TRIANGLE -> {
                path.moveTo(cx, rect.top)
                path.lineTo(rect.right, rect.bottom)
                path.lineTo(rect.left, rect.bottom)
                path.close()
            }
            ShapeKind.PENTAGON -> {
                for (i in 0 until 5) {
                    val angle = Math.toRadians((i * 72.0) - 90.0)
                    val px = cx + (cos(angle) * r).toFloat()
                    val py = cy + (sin(angle) * r).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            ShapeKind.STAR -> {
                val points = 5
                val innerRadius = r * 0.45f
                for (i in 0 until points * 2) {
                    val radius = if (i % 2 == 0) r else innerRadius
                    val angle = Math.toRadians((i * 36.0) - 90.0)
                    val px = cx + (cos(angle) * radius).toFloat()
                    val py = cy + (sin(angle) * radius).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            ShapeKind.HEXAGON -> {
                for (i in 0 until 6) {
                    val angle = Math.toRadians((i * 60.0) - 30.0)
                    val px = cx + (cos(angle) * r).toFloat()
                    val py = cy + (sin(angle) * r).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            ShapeKind.HEART -> {
                val w = rect.width()
                val h = rect.height()
                path.moveTo(cx, rect.bottom - h * 0.08f)
                path.cubicTo(
                    rect.left - w * 0.15f, cy + h * 0.05f,
                    rect.left + w * 0.05f, rect.top + h * 0.05f,
                    cx, rect.top + h * 0.28f
                )
                path.cubicTo(
                    rect.right - w * 0.05f, rect.top + h * 0.05f,
                    rect.right + w * 0.15f, cy + h * 0.05f,
                    cx, rect.bottom - h * 0.08f
                )
                path.close()
            }
            ShapeKind.BADGE -> {
                val spikes = 12
                val innerR = r * 0.82f
                for (i in 0 until spikes * 2) {
                    val rad = if (i % 2 == 0) r else innerR
                    val angle = Math.toRadians(i * (180.0 / spikes))
                    val px = cx + (cos(angle) * rad).toFloat()
                    val py = cy + (sin(angle) * rad).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            ShapeKind.ARROW -> {
                val l = rect.left
                val right = rect.right
                val t = rect.top
                val b = rect.bottom
                val shaftTop = cy - r * 0.32f
                val shaftBottom = cy + r * 0.32f
                val headLeft = cx + r * 0.15f
                path.moveTo(l, shaftTop)
                path.lineTo(headLeft, shaftTop)
                path.lineTo(headLeft, t + r * 0.2f)
                path.lineTo(right, cy)
                path.lineTo(headLeft, b - r * 0.2f)
                path.lineTo(headLeft, shaftBottom)
                path.lineTo(l, shaftBottom)
                path.close()
            }
            ShapeKind.SPEECH_BUBBLE -> {
                val bubbleRect = RectF(rect.left, rect.top, rect.right, rect.bottom - r * 0.35f)
                path.addRoundRect(bubbleRect, r * 0.3f, r * 0.3f, Path.Direction.CW)
                path.moveTo(cx - r * 0.25f, bubbleRect.bottom)
                path.lineTo(cx - r * 0.45f, rect.bottom)
                path.lineTo(cx + r * 0.1f, bubbleRect.bottom)
                path.close()
            }
            ShapeKind.BEZIER_PATH -> {
                val w = rect.width()
                val h = rect.height()
                if (bezierNodes.size >= 2) {
                    val first = bezierNodes.first()
                    path.moveTo(cx + first.x * w, cy + first.y * h)
                    for (i in 1 until bezierNodes.size) {
                        val prev = bezierNodes[i - 1]
                        val curr = bezierNodes[i]
                        path.quadTo(
                            cx + prev.ctrlX * w,
                            cy + prev.ctrlY * h,
                            cx + curr.x * w,
                            cy + curr.y * h
                        )
                    }
                    if (isBezierClosed) {
                        val last = bezierNodes.last()
                        path.quadTo(
                            cx + last.ctrlX * w,
                            cy + last.ctrlY * h,
                            cx + first.x * w,
                            cy + first.y * h
                        )
                        path.close()
                    }
                } else {
                    path.addOval(rect, Path.Direction.CW)
                }
            }
        }
        return path
    }

    private fun renderImageLayer(
        canvas: Canvas,
        layer: ImageLayer,
        centerX: Float,
        centerY: Float,
        minCanvasDim: Float,
        referenceScale: Float = 1f
    ): RectF {
        val rawBmp = layer.bitmap
        val baseSize = minCanvasDim * 0.45f
        if (rawBmp == null || rawBmp.isRecycled) {
            val rect = RectF(centerX - baseSize / 2f, centerY - baseSize / 2f, centerX + baseSize / 2f, centerY + baseSize / 2f)
            val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4438BDF8")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(rect, 24f, 24f, placeholderPaint)
            return rect
        }

        val processedBmp = if (layer.eraseColorEnabled) {
            getChromaKeyBitmap(rawBmp, layer.id, layer.eraseTargetColorHex, layer.eraseTolerance)
        } else {
            rawBmp
        }

        val aspect = processedBmp.width.toFloat() / processedBmp.height.coerceAtLeast(1).toFloat()
        val drawW = if (aspect >= 1f) baseSize else baseSize * aspect
        val drawH = if (aspect >= 1f) baseSize / aspect else baseSize
        val dst = RectF(centerX - drawW / 2f, centerY - drawH / 2f, centerX + drawW / 2f, centerY + drawH / 2f)

        val flipSave = canvas.save()
        if (layer.flipHorizontal || layer.flipVertical) {
            canvas.scale(
                if (layer.flipHorizontal) -1f else 1f,
                if (layer.flipVertical) -1f else 1f,
                centerX,
                centerY
            )
        }

        val isCutoutSticker = processedBmp.hasAlpha() || layer.eraseColorEnabled

        // 0. PixelLab 3D Shape Extrusion for transparent PNGs / Stickers / Merged layers
        if (layer.is3dShapeEnabled && layer.depth3d > 0) {
            val steps = layer.depth3d.coerceIn(1, 48)
            val angleRad = Math.toRadians(layer.obliqueAngle3d.toDouble())
            val sx = cos(angleRad).toFloat() * 1.6f * referenceScale
            val sy = sin(angleRad).toFloat() * 1.6f * referenceScale
            val extrudeBaseColor = if (layer.stroke.enabled) {
                parseHexColor(layer.stroke.colorHex, Color.parseColor("#1E293B"))
            } else {
                Color.parseColor("#1E293B")
            }
            for (step in steps downTo 1) {
                val progress = step.toFloat() / steps.toFloat()
                val darkenFactor = (1f - layer.darken3d * (0.35f + 0.65f * progress)).coerceIn(0.1f, 1f)
                val stepColor = darkenColor(extrudeBaseColor, darkenFactor)
                val extrudePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    colorFilter = PorterDuffColorFilter(stepColor, PorterDuff.Mode.SRC_IN)
                }
                val stepRect = RectF(dst).apply { offset(step * sx, step * sy) }
                canvas.drawBitmap(processedBmp, null, stepRect, extrudePaint)
            }
        }

        // 1. Shadow behind image/sticker (supports alpha silhouette for transparent PNGs!)
        if (layer.shadow.enabled && layer.shadow.blurRadius > 0.5f) {
            val shadowColor = parseHexColor(layer.shadow.colorHex, Color.BLACK)
            if (isCutoutSticker && !layer.isCircularMask && layer.cornerRadius <= 1f) {
                val silShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    colorFilter = PorterDuffColorFilter(shadowColor, PorterDuff.Mode.SRC_IN)
                    maskFilter = BlurMaskFilter(layer.shadow.blurRadius.coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
                }
                val shadowRect = RectF(dst).apply { offset(layer.shadow.offsetX, layer.shadow.offsetY) }
                canvas.drawBitmap(processedBmp, null, shadowRect, silShadowPaint)
            } else {
                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = shadowColor
                    maskFilter = BlurMaskFilter(layer.shadow.blurRadius.coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
                }
                val shadowRect = RectF(dst).apply { offset(layer.shadow.offsetX, layer.shadow.offsetY) }
                if (layer.isCircularMask) {
                    canvas.drawOval(shadowRect, shadowPaint)
                } else {
                    canvas.drawRoundRect(shadowRect, layer.cornerRadius, layer.cornerRadius, shadowPaint)
                }
            }
        }

        // 2. PixelLab Cutout Outline Stroke around transparent PNGs/Stickers
        if (layer.stroke.enabled && layer.stroke.width > 0f && isCutoutSticker && !layer.isCircularMask && layer.cornerRadius <= 1f) {
            val strokeColor = parseHexColor(layer.stroke.colorHex, Color.WHITE)
            val contourPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = PorterDuffColorFilter(strokeColor, PorterDuff.Mode.SRC_IN)
            }
            val r = (layer.stroke.width * 1.3f).coerceIn(1f, 36f)
            val offsets = listOf(
                -r to 0f, r to 0f, 0f to -r, 0f to r,
                -r * 0.7f to -r * 0.7f, r * 0.7f to -r * 0.7f,
                -r * 0.7f to r * 0.7f, r * 0.7f to r * 0.7f
            )
            offsets.forEach { (ox, oy) ->
                val strokeRect = RectF(dst).apply { offset(ox, oy) }
                canvas.drawBitmap(processedBmp, null, strokeRect, contourPaint)
            }
        }

        // 3. Main Image with Color Overlay (Tint) or Hue/Sat/Bright/Contrast Filter + Manual Eraser Strokes
        val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (layer.colorOverlayEnabled) {
            imgPaint.colorFilter = PorterDuffColorFilter(
                parseHexColor(layer.colorOverlayHex, Color.WHITE),
                PorterDuff.Mode.SRC_IN
            )
        } else if (layer.hue != 0f || layer.saturation != 1f || layer.brightness != 0f || layer.contrast != 1f) {
            imgPaint.colorFilter = ColorMatrixColorFilter(
                buildColorAdjustmentMatrix(
                    brightness = layer.brightness,
                    contrast = layer.contrast,
                    saturation = layer.saturation,
                    hue = layer.hue
                )
            )
        }

        val imgLayerSave = if (layer.eraserStrokes.isNotEmpty()) {
            canvas.saveLayer(dst, null)
        } else {
            canvas.save()
        }
        applyMaskClipIfNeeded(
            canvas = canvas,
            bounds = dst,
            maskMode = layer.maskMode,
            maskInvert = layer.maskInvert,
            maskOffset = layer.maskOffset,
            maskControlX = layer.maskControlX,
            maskControlY = layer.maskControlY
        )
        val clipPath = Path()
        if (layer.isCircularMask) {
            clipPath.addOval(dst, Path.Direction.CW)
            canvas.clipPath(clipPath)
        } else if (layer.cornerRadius > 0f) {
            clipPath.addRoundRect(dst, layer.cornerRadius, layer.cornerRadius, Path.Direction.CW)
            canvas.clipPath(clipPath)
        }
        canvas.drawBitmap(processedBmp, null, dst, imgPaint)

        // Manual Brush Eraser strokes (PorterDuff.Mode.CLEAR)
        if (layer.eraserStrokes.isNotEmpty()) {
            val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            layer.eraserStrokes.forEach { erStroke ->
                if (erStroke.points.isNotEmpty()) {
                    erasePaint.strokeWidth = erStroke.strokeWidth * referenceScale * 1.6f
                    val p = Path()
                    val first = erStroke.points.first()
                    val fx = dst.left + first.x * dst.width()
                    val fy = dst.top + first.y * dst.height()
                    p.moveTo(fx, fy)
                    if (erStroke.points.size == 1) {
                        p.lineTo(fx + 0.5f, fy + 0.5f)
                    } else {
                        for (i in 1 until erStroke.points.size) {
                            val prev = erStroke.points[i - 1]
                            val curr = erStroke.points[i]
                            val px = dst.left + prev.x * dst.width()
                            val py = dst.top + prev.y * dst.height()
                            val cx = dst.left + curr.x * dst.width()
                            val cy = dst.top + curr.y * dst.height()
                            p.quadTo(px, py, (px + cx) / 2f, (py + cy) / 2f)
                        }
                    }
                    canvas.drawPath(p, erasePaint)
                }
            }
        }
        canvas.restoreToCount(imgLayerSave)

        // 4. Optional frame border for non-cutout or circular/rounded images
        if (layer.stroke.enabled && layer.stroke.width > 0f && (!isCutoutSticker || layer.isCircularMask || layer.cornerRadius > 1f)) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = layer.stroke.width * 1.4f
                color = parseHexColor(layer.stroke.colorHex, Color.WHITE)
            }
            if (layer.isCircularMask) {
                canvas.drawOval(dst, borderPaint)
            } else {
                canvas.drawRoundRect(dst, layer.cornerRadius, layer.cornerRadius, borderPaint)
            }
        }

        canvas.restoreToCount(flipSave)
        return dst
    }

    /**
     * Generates a high-resolution transparent badge sticker bitmap for BuiltinStickerItem.
     */
    fun renderBuiltinStickerToBitmap(
        symbolText: String,
        primaryHex: String,
        secondaryHex: String
    ): Bitmap {
        val size = 384
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = size / 2f
        val cy = size / 2f
        val r = size * 0.42f

        val c1 = parseHexColor(primaryHex, Color.parseColor("#FDE047"))
        val c2 = parseHexColor(secondaryHex, Color.parseColor("#D97706"))

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                cx - r, cy - r, cx + r, cy + r,
                c1, c2, Shader.TileMode.CLAMP
            )
        }
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 14f
            color = Color.WHITE
        }

        val badgePath = buildShapePath(ShapeKind.BADGE, RectF(cx - r, cy - r, cx + r, cy + r), 24f)
        c.drawPath(badgePath, badgePaint)
        c.drawPath(badgePath, ringPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size * 0.44f
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
        }
        val fm = textPaint.fontMetrics
        val baseline = cy - (fm.ascent + fm.descent) / 2f
        c.drawText(symbolText, cx, baseline, textPaint)
        return bmp
    }

    /**
     * PixelLab Erase Color (Chroma-Key) filter: removes target color within tolerance.
     */
    fun getChromaKeyBitmap(
        source: Bitmap,
        layerId: String,
        targetHex: String,
        tolerance: Float
    ): Bitmap {
        val key = "${layerId}_${source.hashCode()}_${targetHex}_${(tolerance * 100).toInt()}"
        erasedBitmapLruCache.get(key)?.let { if (!it.isRecycled) return it }

        val targetColor = parseHexColor(targetHex, Color.WHITE)
        val tr = Color.red(targetColor)
        val tg = Color.green(targetColor)
        val tb = Color.blue(targetColor)
        val maxDist = (tolerance.coerceIn(0.02f, 0.75f) * 441.67f)

        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = Color.alpha(c)
            if (a == 0) continue
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val dist = sqrt(((r - tr) * (r - tr) + (g - tg) * (g - tg) + (b - tb) * (b - tb)).toFloat())
            if (dist <= maxDist) {
                val ratio = (dist / maxDist).coerceIn(0f, 1f)
                val newAlpha = if (ratio < 0.65f) 0 else ((ratio - 0.65f) / 0.35f * a).toInt()
                pixels[i] = Color.argb(newAlpha, r, g, b)
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        erasedBitmapLruCache.put(key, out)
        return out
    }

    /**
     * Auto-removes corner background color from AI-generated stickers.
     */
    fun makeStickerTransparent(source: Bitmap, tolerance: Float = 0.22f): Bitmap {
        val cornerColor = source.getPixel(0, 0)
        val hex = String.format("#%06X", 0xFFFFFF and cornerColor)
        return getChromaKeyBitmap(source, "sticker_${System.nanoTime()}", hex, tolerance)
    }

    /**
     * Maps canvas-normalized touch points (0..1, 0..1) into an ImageLayer's local dst normalized coordinates (0..1, 0..1)
     * so interactive erasing erases the exact pixels under the user's finger regardless of layer position, scale, rotation, or flip.
     */
    fun mapCanvasNormalizedToImageLocal(
        layer: ImageLayer,
        canvasPoints: List<Point2D>,
        viewportWidth: Float,
        viewportHeight: Float
    ): List<Point2D> {
        val centerX = viewportWidth * (0.5f + layer.x)
        val centerY = viewportHeight * (0.5f + layer.y)
        val minCanvasDim = min(viewportWidth, viewportHeight).coerceAtLeast(1f)
        val baseSize = minCanvasDim * 0.45f
        val bmp = layer.bitmap
        val aspect = if (bmp != null && !bmp.isRecycled && bmp.height > 0) {
            bmp.width.toFloat() / bmp.height.toFloat()
        } else 1f
        val drawW = (if (aspect >= 1f) baseSize else baseSize * aspect).coerceAtLeast(1f)
        val drawH = (if (aspect >= 1f) baseSize / aspect else baseSize).coerceAtLeast(1f)

        val rad = Math.toRadians(-layer.rotationZ.toDouble())
        val cosR = cos(rad).toFloat()
        val sinR = sin(rad).toFloat()
        val effScaleX = layer.scaleX.coerceAtLeast(0.1f) * (if (layer.flipHorizontal) -1f else 1f)
        val effScaleY = layer.scaleY.coerceAtLeast(0.1f) * (if (layer.flipVertical) -1f else 1f)

        return canvasPoints.map { pt ->
            val px = pt.x * viewportWidth
            val py = pt.y * viewportHeight
            val dx = px - centerX
            val dy = py - centerY
            val rx = dx * cosR - dy * sinR
            val ry = dx * sinR + dy * cosR
            val sx = rx / effScaleX
            val sy = ry / effScaleY
            Point2D(
                x = 0.5f + sx / drawW,
                y = 0.5f + sy / drawH
            )
        }
    }

    /**
     * Maps canvas-normalized touch points (0..1, 0..1) into a DrawingLayer's local normalized coordinates (0..1, 0..1).
     */
    fun mapCanvasNormalizedToDrawingLocal(
        layer: DrawingLayer,
        canvasPoints: List<Point2D>,
        viewportWidth: Float,
        viewportHeight: Float
    ): List<Point2D> {
        val w = viewportWidth.coerceAtLeast(1f)
        val h = viewportHeight.coerceAtLeast(1f)
        val rad = Math.toRadians(-layer.rotationZ.toDouble())
        val cosR = cos(rad).toFloat()
        val sinR = sin(rad).toFloat()
        val sx = layer.scaleX.coerceAtLeast(0.1f)
        val sy = layer.scaleY.coerceAtLeast(0.1f)

        return canvasPoints.map { pt ->
            val tx = (pt.x - layer.x) * w - w / 2f
            val ty = (pt.y - layer.y) * h - h / 2f
            val rx = (tx * cosR - ty * sinR) / sx
            val ry = (tx * sinR + ty * cosR) / sy
            Point2D(
                x = (w / 2f + rx) / w,
                y = (h / 2f + ry) / h
            )
        }
    }

    private fun renderDrawingLayer(
        canvas: Canvas,
        layer: DrawingLayer,
        viewportWidth: Float,
        viewportHeight: Float,
        referenceScale: Float
    ): RectF? {
        if (layer.strokes.isEmpty()) return null
        var minX = viewportWidth
        var minY = viewportHeight
        var maxX = 0f
        var maxY = 0f
        var hasVisibleBounds = false

        val hasEraserStrokes = layer.strokes.any { it.isEraser }
        val drawLayerSave = if (hasEraserStrokes) {
            canvas.saveLayer(0f, 0f, viewportWidth, viewportHeight, null)
        } else {
            canvas.save()
        }

        layer.strokes.forEach { stroke ->
            if (stroke.points.isEmpty()) return@forEach
            val path = Path()
            val first = stroke.points.first()
            val startX = first.x * viewportWidth
            val startY = first.y * viewportHeight
            path.moveTo(startX, startY)

            if (!stroke.isEraser || !hasVisibleBounds) {
                minX = min(minX, startX)
                minY = min(minY, startY)
                maxX = max(maxX, startX)
                maxY = max(maxY, startY)
                if (!stroke.isEraser) hasVisibleBounds = true
            }

            for (i in 1 until stroke.points.size) {
                val prev = stroke.points[i - 1]
                val curr = stroke.points[i]
                val px = prev.x * viewportWidth
                val py = prev.y * viewportHeight
                val cx = curr.x * viewportWidth
                val cy = curr.y * viewportHeight
                path.quadTo(px, py, (px + cx) / 2f, (py + cy) / 2f)

                if (!stroke.isEraser || !hasVisibleBounds) {
                    minX = min(minX, cx)
                    minY = min(minY, cy)
                    maxX = max(maxX, cx)
                    maxY = max(maxY, cy)
                }
            }

            val widthPx = (stroke.strokeWidth * referenceScale * 1.5f).coerceAtLeast(2f)

            if (stroke.isEraser) {
                val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = widthPx
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
                canvas.drawPath(path, erasePaint)
            } else {
                val strokeColor = parseHexColor(stroke.colorHex, Color.CYAN)
                if (stroke.neonGlow) {
                    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = widthPx * 2.2f
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        color = strokeColor
                        alpha = 110
                        maskFilter = BlurMaskFilter(widthPx * 1.4f, BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.drawPath(path, glowPaint)
                }

                val penPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = widthPx
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    color = strokeColor
                    if (stroke.isDashed) {
                        pathEffect = DashPathEffect(floatArrayOf(widthPx * 2.2f, widthPx * 1.6f), 0f)
                    }
                }
                canvas.drawPath(path, penPaint)
            }
        }

        canvas.restoreToCount(drawLayerSave)
        return RectF(minX - 16f, minY - 16f, maxX + 16f, maxY + 16f)
    }

    private fun renderPostFxOverlays(canvas: Canvas, fx: CanvasPostFx, w: Float, h: Float) {
        // 1. Scanline Stripes
        if (fx.stripesEnabled) {
            val stripePaint = Paint().apply {
                color = Color.WHITE
                alpha = (fx.stripesAlpha.coerceIn(0.01f, 0.5f) * 255).toInt()
                strokeWidth = 2f
            }
            val step = fx.stripesSpacing.coerceAtLeast(6f)
            var y = 0f
            while (y < h) {
                canvas.drawLine(0f, y, w, y, stripePaint)
                y += step
            }
        }

        // 2. Film Grain Noise
        if (fx.noiseEnabled && fx.noiseIntensity > 0.01f) {
            val noisePaint = Paint()
            val rng = Random(42)
            val alpha = (fx.noiseIntensity.coerceIn(0.01f, 0.5f) * 140).toInt()
            val dotCount = 900
            for (i in 0 until dotCount) {
                val nx = rng.nextFloat() * w
                val ny = rng.nextFloat() * h
                noisePaint.color = if (rng.nextBoolean()) {
                    Color.argb(alpha, 255, 255, 255)
                } else {
                    Color.argb(alpha, 0, 0, 0)
                }
                canvas.drawCircle(nx, ny, 1.5f, noisePaint)
            }
        }

        // 3. Vignette Overlay
        if (fx.vignetteEnabled && fx.vignetteIntensity > 0.02f) {
            val vigColor = parseHexColor(fx.vignetteColorHex, Color.BLACK)
            val maxAlpha = (fx.vignetteIntensity.coerceIn(0f, 1f) * 235).toInt()
            val edgeColor = Color.argb(maxAlpha, Color.red(vigColor), Color.green(vigColor), Color.blue(vigColor))
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    w / 2f, h / 2f,
                    max(w, h) * 0.72f,
                    intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, edgeColor),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w, h, vignettePaint)
        }
    }

    private fun renderStudioGrid(
        canvas: Canvas,
        w: Float,
        h: Float,
        mode: StudioGuideOverlayMode = StudioGuideOverlayMode.GRID_6X6
    ) {
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4438BDF8")
            strokeWidth = 1.5f
        }
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#AA00B4D8")
            strokeWidth = 2.2f
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#CCFACC15")
            strokeWidth = 2.4f
        }

        when (mode) {
            StudioGuideOverlayMode.RULE_OF_THIRDS -> {
                for (i in 1..2) {
                    val x = (w / 3f) * i
                    val y = (h / 3f) * i
                    canvas.drawLine(x, 0f, x, h, goldPaint)
                    canvas.drawLine(0f, y, w, y, goldPaint)
                }
            }
            StudioGuideOverlayMode.GOLDEN_RATIO -> {
                val phi1 = 0.382f
                val phi2 = 0.618f
                canvas.drawLine(w * phi1, 0f, w * phi1, h, goldPaint)
                canvas.drawLine(w * phi2, 0f, w * phi2, h, goldPaint)
                canvas.drawLine(0f, h * phi1, w, h * phi1, goldPaint)
                canvas.drawLine(0f, h * phi2, w, h * phi2, goldPaint)
                canvas.drawCircle(w * 0.5f, h * 0.5f, min(w, h) * 0.309f, goldPaint)
            }
            StudioGuideOverlayMode.YOUTUBE_THUMBNAIL_SAFE -> {
                // Safe margin + bottom-right YouTube timestamp exclusion zone
                val marginX = w * 0.05f
                val marginY = h * 0.06f
                canvas.drawRect(marginX, marginY, w - marginX, h - marginY, centerPaint)
                val tsRect = RectF(w * 0.78f, h * 0.80f, w * 0.96f, h * 0.94f)
                val dangerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.parseColor("#66EF4444")
                }
                canvas.drawRoundRect(tsRect, 12f, 12f, dangerPaint)
            }
            else -> {
                // Standard 6x6 PixelLab Grid
                for (i in 1..5) {
                    val x = (w / 6f) * i
                    val y = (h / 6f) * i
                    val p = if (i == 3) centerPaint else gridPaint
                    canvas.drawLine(x, 0f, x, h, p)
                    canvas.drawLine(0f, y, w, y, p)
                }
            }
        }
    }

    /**
     * Renders a live glowing neon cyan / emerald bounding box highlight on layers actively
     * mutated or previewed by the Gemini AI agent.
     */
    private fun drawAiMutatedHighlightFrame(canvas: Canvas, bounds: RectF, referenceScale: Float) {
        // 1. Outer Glow Halo
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#4D38BDF8") // 30% alpha neon cyan
            strokeWidth = max(6f, 9f * referenceScale)
        }
        val expandedBounds = RectF(
            bounds.left - 4f * referenceScale,
            bounds.top - 4f * referenceScale,
            bounds.right + 4f * referenceScale,
            bounds.bottom + 4f * referenceScale
        )
        canvas.drawRoundRect(expandedBounds, 8f * referenceScale, 8f * referenceScale, glowPaint)

        // 2. Neon Dashed Precision Bounding Frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#38BDF8")
            strokeWidth = max(3f, 4f * referenceScale)
            pathEffect = DashPathEffect(floatArrayOf(12f, 6f), 0f)
        }
        canvas.drawRoundRect(bounds, 6f * referenceScale, 6f * referenceScale, framePaint)

        // 3. Precision Corner Brackets
        val cornerLen = max(16f, 24f * referenceScale)
        val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#0284C7")
            strokeWidth = max(4f, 5.5f * referenceScale)
            strokeCap = Paint.Cap.ROUND
        }
        // Top-Left
        canvas.drawLine(bounds.left, bounds.top, bounds.left + cornerLen, bounds.top, cornerPaint)
        canvas.drawLine(bounds.left, bounds.top, bounds.left, bounds.top + cornerLen, cornerPaint)
        // Top-Right
        canvas.drawLine(bounds.right, bounds.top, bounds.right - cornerLen, bounds.top, cornerPaint)
        canvas.drawLine(bounds.right, bounds.top, bounds.right, bounds.top + cornerLen, cornerPaint)
        // Bottom-Left
        canvas.drawLine(bounds.left, bounds.bottom, bounds.left + cornerLen, bounds.bottom, cornerPaint)
        canvas.drawLine(bounds.left, bounds.bottom, bounds.left, bounds.bottom - cornerLen, cornerPaint)
        // Bottom-Right
        canvas.drawLine(bounds.right, bounds.bottom, bounds.right - cornerLen, bounds.bottom, cornerPaint)
        canvas.drawLine(bounds.right, bounds.bottom, bounds.right, bounds.bottom - cornerLen, cornerPaint)

        // 4. "AI GEMINI" Indicator Badge
        val badgeText = "✦ GEMINI AI"
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = max(11f, 15f * referenceScale)
            typeface = Typeface.DEFAULT_BOLD
        }
        val textWidth = badgeTextPaint.measureText(badgeText)
        val badgePadX = 14f * referenceScale
        val badgePadY = 6f * referenceScale
        val badgeHeight = max(18f, 24f * referenceScale)
        val badgeRect = RectF(
            bounds.left,
            bounds.top - badgeHeight - 6f * referenceScale,
            bounds.left + textWidth + badgePadX * 2f,
            bounds.top - 6f * referenceScale
        )
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#0284C7")
        }
        canvas.drawRoundRect(badgeRect, 6f * referenceScale, 6f * referenceScale, badgeBgPaint)
        canvas.drawText(
            badgeText,
            badgeRect.left + badgePadX,
            badgeRect.bottom - badgePadY,
            badgeTextPaint
        )
    }

    private fun drawSelectionFrame(canvas: Canvas, bounds: RectF, referenceScale: Float) {
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#38BDF8")
            strokeWidth = max(2.5f, 3.5f * referenceScale)
            pathEffect = DashPathEffect(floatArrayOf(14f, 8f), 0f)
        }
        canvas.drawRect(bounds, framePaint)

        val handleRadius = max(7f, 10f * referenceScale)
        val handleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        val handleStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(2f, 3f * referenceScale)
            color = Color.parseColor("#0284C7")
        }

        val topCorners = listOf(
            bounds.left to bounds.top,
            bounds.right to bounds.top
        )
        topCorners.forEach { (cx, cy) ->
            canvas.drawCircle(cx, cy, handleRadius * 0.85f, handleFill)
            canvas.drawCircle(cx, cy, handleRadius * 0.85f, handleStroke)
        }

        // PixelLab Bottom-Right Scale Handle (Larger Cyan Badge)
        val scaleHandleR = handleRadius * 1.45f
        val scaleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#38BDF8")
        }
        canvas.drawCircle(bounds.right, bounds.bottom, scaleHandleR, scaleFill)
        canvas.drawCircle(bounds.right, bounds.bottom, scaleHandleR, handleFill.apply { style = Paint.Style.STROKE; strokeWidth = max(2f, 2.5f * referenceScale) })

        // PixelLab Bottom-Left Rotate Handle (Purple/Gold Badge)
        val rotFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#A855F7")
        }
        canvas.drawCircle(bounds.left, bounds.bottom, scaleHandleR, rotFill)
        canvas.drawCircle(bounds.left, bounds.bottom, scaleHandleR, handleFill)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }

    private fun buildColorAdjustmentMatrix(
        brightness: Float,
        contrast: Float,
        saturation: Float,
        hue: Float
    ): ColorMatrix {
        val cm = ColorMatrix()

        // 1. Saturation
        val satMatrix = ColorMatrix().apply { setSaturation(saturation.coerceIn(0f, 3f)) }
        cm.postConcat(satMatrix)

        // 2. Contrast & Brightness
        val c = contrast.coerceIn(0.2f, 2.5f)
        val b = brightness.coerceIn(-100f, 100f) * 2f
        val translate = (1f - c) * 128f + b
        val cbMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, translate,
                0f, c, 0f, 0f, translate,
                0f, 0f, c, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(cbMatrix)

        // 3. Hue Rotation
        if (abs(hue) > 0.5f) {
            val rad = Math.toRadians(hue.toDouble())
            val cosVal = cos(rad).toFloat()
            val sinVal = sin(rad).toFloat()
            val lumR = 0.213f
            val lumG = 0.715f
            val lumB = 0.072f
            val hueMatrix = ColorMatrix(
                floatArrayOf(
                    lumR + cosVal * (1 - lumR) + sinVal * (-lumR),
                    lumG + cosVal * (-lumG) + sinVal * (-lumG),
                    lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0f, 0f,
                    lumR + cosVal * (-lumR) + sinVal * 0.143f,
                    lumG + cosVal * (1 - lumG) + sinVal * 0.140f,
                    lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0f, 0f,
                    lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)),
                    lumG + cosVal * (-lumG) + sinVal * lumG,
                    lumB + cosVal * (1 - lumB) + sinVal * lumB, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(hueMatrix)
        }

        return cm
    }
}
