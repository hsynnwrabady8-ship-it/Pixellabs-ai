package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.ui.canvas.StudioCanvasRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    PNG("PNG (شفاف و بدون افت کیفیت)", "png", "image/png"),
    JPG("JPG (حجم بهینه)", "jpg", "image/jpeg"),
    WEBP("WebP (مدرن و فشرده)", "webp", "image/webp")
}

enum class ExportQualityPreset(val labelFa: String, val scaleMultiplier: Float, val quality: Int) {
    DEFAULT("استاندارد (1x)", 1.0f, 92),
    HIGH("کیفیت بالا (1.5x HD)", 1.5f, 96),
    VERY_HIGH("فوق‌العاده (2x QHD)", 2.0f, 98),
    ULTRA_4K("اولترا ۴K (حداکثر رزولوشن)", 3.0f, 100)
}

/**
 * Manages PixelLab-compatible (.plp) ZIP project packages and high-resolution gallery exports.
 * Each .plp file is a structured ZIP archive containing:
 * - project.json: Complete multi-layer manifest (text, 3D, curve, stroke, shadow, emboss, shapes, drawing strokes, post-FX)
 * - assets/layer_<id>.png: Lossless PNG bitmaps for Image/Sticker layers and custom backgrounds
 */
object PlpProjectManager {

    private const val MANIFEST_ENTRY = "project.json"

    suspend fun savePlpToInternalStorage(context: Context, project: StudioProject): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "plp_projects").apply { mkdirs() }
        val safeName = project.title.replace(Regex("[^a-zA-Z0-9آ-ی_\\-]"), "_").ifBlank { "pixellab_project" }
        val fileName = "${safeName}_${project.id.take(6)}.plp"
        val outFile = File(dir, fileName)
        FileOutputStream(outFile).use { fos ->
            writePlpArchive(project, fos, context)
        }
        outFile
    }

    suspend fun exportPlpToUri(context: Context, project: StudioProject, targetUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(targetUri)?.use { os ->
                writePlpArchive(project, os, context)
            } ?: return@withContext Result.failure(IllegalStateException("امکان باز کردن مسیر ذخیره‌سازی فایل PLP وجود ندارد."))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importPlpFromUri(context: Context, sourceUri: Uri): Result<StudioProject> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { ins ->
                val project = readPlpArchive(ins, context)
                Result.success(project)
            } ?: Result.failure(IllegalStateException("فایل PLP قابل خواندن نیست."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listLocalPlpFiles(context: Context): List<File> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "plp_projects").apply { mkdirs() }
        // Auto-seed official PixelLab preset sample if present in assets
        try {
            val presetFileName = "پوستر_پیکسل‌لب_اینستاگرام.plp"
            val targetFile = File(dir, presetFileName)
            if (!targetFile.exists() || targetFile.length() == 0L) {
                context.assets.open("presets/poster_instagram.plp").use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (ignored: Exception) {}

        dir.listFiles { file -> file.extension.equals("plp", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    suspend fun loadPlpFromFile(file: File, context: Context? = null): Result<StudioProject> = withContext(Dispatchers.IO) {
        try {
            FileInputStream(file).use { fis ->
                Result.success(readPlpArchive(fis, context).copy(plpFileName = file.name))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writePlpArchive(project: StudioProject, outputStream: OutputStream, context: Context? = null) {
        val bufferedOut = java.io.BufferedOutputStream(outputStream, 32768)
        ZipOutputStream(bufferedOut).use { zos ->
            // 1. Generate official PixelLab "data.plab" JSON manifest
            val nativePlab = generateNativePixelLabJson(project)
            zos.putNextEntry(ZipEntry("data.plab"))
            zos.write(nativePlab.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. Generate and write official PixelLab "thumb" (320x320 JPEG project preview)
            try {
                val isRobolectric = Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
                if (isRobolectric) {
                    zos.putNextEntry(ZipEntry("thumb"))
                    zos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
                    zos.closeEntry()
                } else {
                    val thumbBmp = Bitmap.createBitmap(320, 320, Bitmap.Config.RGB_565)
                    val canvas = Canvas(thumbBmp)
                    if (context != null) {
                        StudioCanvasRenderer.renderProjectToCanvas(
                            context = context,
                            canvas = canvas,
                            project = project,
                            viewportWidth = 320f,
                            viewportHeight = 320f,
                            selectedLayerId = null,
                            showSelectionHandles = false,
                            showGrid = false,
                            includeCheckerboard = false
                        )
                    } else {
                        canvas.drawColor(hexColorToInt(project.background.primaryColorHex, android.graphics.Color.DKGRAY))
                    }
                    zos.putNextEntry(ZipEntry("thumb"))
                    thumbBmp.compress(Bitmap.CompressFormat.JPEG, 80, zos)
                    zos.closeEntry()
                    thumbBmp.recycle()
                }
            } catch (t: Throwable) {
                // Ignore thumb generation failure in restricted test environments
            }

            // 3. Write native PixelLab /res/ resources for images, textures, backgrounds
            project.background.backgroundBitmap?.let { bmp ->
                zos.putNextEntry(ZipEntry("res/bg_bitmap"))
                bmp.compress(Bitmap.CompressFormat.PNG, 100, zos)
                zos.closeEntry()
            }
            project.layers.filterIsInstance<ImageLayer>().forEach { imgLayer ->
                imgLayer.bitmap?.let { bmp ->
                    zos.putNextEntry(ZipEntry("res/img_${imgLayer.id.take(8)}"))
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, zos)
                    zos.closeEntry()
                }
            }
            project.layers.filterIsInstance<TextLayer>().forEach { txtLayer ->
                txtLayer.customTextureBitmap?.let { bmp ->
                    zos.putNextEntry(ZipEntry("res/texture_${txtLayer.id.take(8)}"))
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, zos)
                    zos.closeEntry()
                }
            }

            // 4. Also write project.json manifest and assets/ for full backward compatibility
            val rootJson = JSONObject().apply {
                put("format", "PIXELLAB_AI_PLP_V2")
                put("id", project.id)
                put("title", project.title)
                put("canvasWidth", project.canvasWidth)
                put("canvasHeight", project.canvasHeight)

                put("background", JSONObject().apply {
                    put("type", project.background.type.name)
                    put("primaryColorHex", project.background.primaryColorHex)
                    put("secondaryColorHex", project.background.secondaryColorHex)
                    put("gradientAngle", project.background.gradientAngle.toDouble())
                    put("isRadialGradient", project.background.isRadialGradient)
                    if (project.background.backgroundBitmap != null) {
                        put("bgAsset", "assets/bg_bitmap.png")
                    }
                })

                put("postFx", JSONObject().apply {
                    put("vignetteEnabled", project.postFx.vignetteEnabled)
                    put("vignetteColorHex", project.postFx.vignetteColorHex)
                    put("vignetteIntensity", project.postFx.vignetteIntensity.toDouble())
                    put("noiseEnabled", project.postFx.noiseEnabled)
                    put("noiseIntensity", project.postFx.noiseIntensity.toDouble())
                    put("stripesEnabled", project.postFx.stripesEnabled)
                    put("stripesAlpha", project.postFx.stripesAlpha.toDouble())
                    put("stripesSpacing", project.postFx.stripesSpacing.toDouble())
                    put("brightness", project.postFx.brightness.toDouble())
                    put("contrast", project.postFx.contrast.toDouble())
                    put("saturation", project.postFx.saturation.toDouble())
                    put("hue", project.postFx.hue.toDouble())
                })

                val layersArray = JSONArray()
                project.layers.forEach { layer ->
                    val layerObj = serializeLayer(layer)
                    layersArray.put(layerObj)
                }
                put("layers", layersArray)
            }

            zos.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // assets/ copies
            project.background.backgroundBitmap?.let { bmp ->
                zos.putNextEntry(ZipEntry("assets/bg_bitmap.png"))
                bmp.compress(Bitmap.CompressFormat.PNG, 100, zos)
                zos.closeEntry()
            }
            project.layers.filterIsInstance<ImageLayer>().forEach { imgLayer ->
                imgLayer.bitmap?.let { bmp ->
                    zos.putNextEntry(ZipEntry("assets/layer_${imgLayer.id}.png"))
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, zos)
                    zos.closeEntry()
                }
            }
            project.layers.filterIsInstance<TextLayer>().forEach { txtLayer ->
                txtLayer.customTextureBitmap?.let { bmp ->
                    zos.putNextEntry(ZipEntry("assets/texture_${txtLayer.id}.png"))
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, zos)
                    zos.closeEntry()
                }
            }
        }
    }

    fun hexColorToInt(hex: String, default: Int = -1): Int {
        return try {
            val clean = hex.removePrefix("#")
            when (clean.length) {
                6 -> (0xFF shl 24) or clean.toLong(16).toInt()
                8 -> clean.toLong(16).toInt()
                else -> default
            }
        } catch (e: Exception) {
            default
        }
    }

    private fun generateNativePixelLabJson(project: StudioProject): JSONObject {
        val root = JSONObject()
        root.put("bgWidth", project.canvasWidth)
        root.put("bgHeight", project.canvasHeight)

        val bgTypeInt = when (project.background.type) {
            BackgroundType.IMAGE -> 1
            BackgroundType.GRADIENT -> 2
            else -> 0
        }
        root.put("bgType", bgTypeInt)
        root.put("bgColor", hexColorToInt(project.background.primaryColorHex, -1))

        if (project.background.type == BackgroundType.GRADIENT) {
            val gradType = if (project.background.isRadialGradient) 4 else 0
            val c1 = hexColorToInt(project.background.primaryColorHex)
            val c2 = hexColorToInt(project.background.secondaryColorHex)
            root.put("bgGradientV2", "v2;$gradType;0.0/0.0/1.0/1.0;$c1/$c2;0.0/1.0")
        }
        if (project.background.backgroundBitmap != null) {
            val photoSrc = JSONObject().apply {
                put("imgSrcPath", "res/bg_bitmap")
            }
            root.put("bgPhotoSrc", photoSrc)
        }

        val fx = project.postFx
        root.put("effectVignetteEnabled", fx.vignetteEnabled)
        root.put("effectVignetteColor", hexColorToInt(fx.vignetteColorHex, -16777216))
        root.put("effectVignetteAlpha", (fx.vignetteIntensity * 255).toInt())
        root.put("effectNoiseEnabled", fx.noiseEnabled)
        root.put("effectNoiseAlpha", (fx.noiseIntensity * 255).toInt())
        root.put("effectStripesEnabled", fx.stripesEnabled)
        root.put("effectStripesAlpha", (fx.stripesAlpha * 255).toInt())
        root.put("effectBrightnessVal", (fx.brightness * 100).toInt())
        root.put("effectContrastVal", ((fx.contrast - 1f) * 100).toInt())
        root.put("effectSaturationVal", ((fx.saturation - 1f) * 100).toInt())
        root.put("effectHueVal", (fx.hue / 1.8f).toInt())

        val objectsIds = JSONArray()
        val objectsBundle = JSONObject()

        project.layers.forEachIndexed { index, layer ->
            val objKey = "object_${index + 1}"
            objectsIds.put(objKey)
            val obj = JSONObject()

            val w = project.canvasWidth.toDouble()
            val h = project.canvasHeight.toDouble()
            obj.put("shapeOldContainerWidth", w)
            obj.put("shapeOldContainerHeight", h)
            obj.put("shapeX", (layer.x + 0.5) * w)
            obj.put("shapeY", (layer.y + 0.5) * h)
            obj.put("shapeRotation", layer.rotationZ.toDouble())
            obj.put("shapeRotationX", layer.rotationX.toDouble())
            obj.put("shapeRotationY", layer.rotationY.toDouble())
            obj.put("shapeOpacity", (layer.opacity * 100).toInt())
            obj.put("textOpacity", (layer.opacity * 100).toInt())
            obj.put("objectLocked", layer.isLocked)
            obj.put("objectHidden", !layer.isVisible)

            when (layer) {
                is TextLayer -> {
                    obj.put("shapeType", "text")
                    obj.put("textTextString", layer.text)
                    obj.put("textTextSize", layer.fontSizeSp.toDouble())
                    obj.put("textCurColor", hexColorToInt(layer.colorHex))
                    obj.put("textFontTypeface", layer.fontId)

                    if (layer.useGradientFill) {
                        val c1 = hexColorToInt(layer.colorHex)
                        val c2 = hexColorToInt(layer.gradientSecondColorHex)
                        obj.put("textGradientV2", "v2;0;0.0/0.0/1.0/1.0;$c1/$c2;0.0/1.0")
                    }

                    obj.put("textStrokeEnabled", layer.stroke.enabled)
                    obj.put("textStrokeColor", hexColorToInt(layer.stroke.colorHex))
                    obj.put("textStrokeWidth", layer.stroke.width.toDouble())

                    obj.put("textOuterEnabled", layer.shadow.enabled)
                    obj.put("textOuterColor", hexColorToInt(layer.shadow.colorHex))
                    obj.put("textOuterRadius", layer.shadow.blurRadius.toDouble())
                    obj.put("textOuterDx", layer.shadow.offsetX.toDouble())
                    obj.put("textOuterDy", layer.shadow.offsetY.toDouble())
                    obj.put("textOuterGlowEnabled", layer.shadow.outerGlow)

                    obj.put("textInnerEnabled", layer.innerShadow.enabled)
                    obj.put("textInnerColor", hexColorToInt(layer.innerShadow.colorHex))
                    obj.put("textInnerRadius", layer.innerShadow.blurRadius.toDouble())
                    obj.put("textInnerDx", layer.innerShadow.offsetX.toDouble())
                    obj.put("textInnerDy", layer.innerShadow.offsetY.toDouble())

                    obj.put("textEmbossEnabled", layer.emboss.enabled)
                    obj.put("textThreeDEnabled", layer.is3dEnabled)
                    obj.put("textThreeDDepth", layer.depth3d)
                    obj.put("textThreeDObAn", layer.obliqueAngle3d.toDouble())
                    obj.put("textCurvingAngle", layer.curveAngle.toDouble())
                    obj.put("textReflectionEnabled", layer.reflectionEnabled)
                    obj.put("text3DShadowEnabled", layer.shadow3dEnabled)
                    obj.put("textBackgroundEnabled", layer.backgroundPillEnabled)
                    obj.put("textBackgroundColor", hexColorToInt(layer.backgroundPillColorHex))
                }
                is ShapeLayer -> {
                    obj.put("shapeType", "shape")
                    obj.put("shapeKind", layer.shapeKind.name)
                    obj.put("shapeCurColor", hexColorToInt(layer.fillColorHex))
                    if (layer.useGradientFill) {
                        val c1 = hexColorToInt(layer.fillColorHex)
                        val c2 = hexColorToInt(layer.gradientSecondColorHex)
                        obj.put("shapeGradientV2", "v2;0;0.0/0.0/1.0/1.0;$c1/$c2;0.0/1.0")
                    }
                    obj.put("shapeStrokeEnabled", layer.stroke.enabled)
                    obj.put("shapeStrokeColor", hexColorToInt(layer.stroke.colorHex))
                    obj.put("shapeStrokeWidth", layer.stroke.width.toDouble())
                    obj.put("shapeShadowEnabled", layer.shadow.enabled)
                    obj.put("shapeShadowColor", hexColorToInt(layer.shadow.colorHex))
                    obj.put("shapeShadowRadius", layer.shadow.blurRadius.toDouble())
                    obj.put("shapeThreeDEnabled", layer.is3dExtruded)
                    obj.put("shapeThreeDDepth", layer.depth3d)
                    obj.put("shapeEmbossEnabled", layer.emboss.enabled)
                }
                is ImageLayer -> {
                    obj.put("shapeType", "image")
                    obj.put("shapeImgSrcPath", "res/img_${layer.id.take(8)}")
                    obj.put("shapeImgAlpha", (layer.opacity * 255).toInt())
                    obj.put("shapeStrokeEnabled", layer.stroke.enabled)
                    obj.put("shapeStrokeColor", hexColorToInt(layer.stroke.colorHex))
                    obj.put("shapeStrokeWidth", layer.stroke.width.toDouble())
                }
                is DrawingLayer -> {
                    obj.put("shapeType", "drawing")
                    obj.put("drawingStrokesCount", layer.strokes.size)
                }
            }

            objectsBundle.put(objKey, obj)
        }

        root.put("objectsIds", objectsIds)
        root.put("objectsBundle", objectsBundle)
        return root
    }

    private fun decodeSampledBitmap(bytes: ByteArray, maxDim: Int = 2048): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val origW = options.outWidth
        val origH = options.outHeight
        if (origW <= 0 || origH <= 0) return null

        var sampleSize = 1
        while (origW / sampleSize > maxDim || origH / sampleSize > maxDim) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    }

    fun readPlpArchive(inputStream: InputStream, context: Context? = null): StudioProject {
        val allBytes = inputStream.readBytes()
        if (allBytes.isEmpty()) {
            throw IllegalArgumentException("فایل PLP خالی است.")
        }

        // Check if raw text JSON was provided (fallback for plain JSON exports)
        val firstNonWhitespace = allBytes.firstOrNull { !it.toInt().toChar().isWhitespace() }
        if (firstNonWhitespace == '{'.code.toByte()) {
            val jsonStr = String(allBytes, Charsets.UTF_8)
            return if (jsonStr.contains("\"objectsBundle\"") || jsonStr.contains("\"data.plab\"")) {
                parseNativePixelLabPlp(jsonStr, emptyMap(), emptyMap())
            } else {
                parseCustomManifest(jsonStr, emptyMap())
            }
        }

        var customManifestJson: String? = null
        var nativePlabJson: String? = null
        val bitmaps = mutableMapOf<String, Bitmap>()
        val fonts = mutableMapOf<String, String>()

        try {
            ZipInputStream(java.io.ByteArrayInputStream(allBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val rawName = entry.name
                    val cleanName = rawName.trimStart('/')
                    val baseName = File(rawName).name
                    val bytes = zis.readBytes()

                    if (baseName.equals("data.plab", ignoreCase = true)) {
                        nativePlabJson = String(bytes, Charsets.UTF_8)
                    } else if (cleanName == MANIFEST_ENTRY || baseName.equals("project.json", ignoreCase = true)) {
                        customManifestJson = String(bytes, Charsets.UTF_8)
                    } else if (baseName.endsWith(".json", ignoreCase = true)) {
                        if (customManifestJson == null) customManifestJson = String(bytes, Charsets.UTF_8)
                    }

                    // Check if image data (JPEG, PNG, WEBP)
                    val isImage = bytes.size >= 4 && (
                        (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) || // JPEG
                        (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) || // PNG
                        (bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte()) || // WEBP
                        cleanName.endsWith(".png", true) || cleanName.endsWith(".jpg", true) || cleanName.endsWith(".jpeg", true) || cleanName.endsWith(".webp", true)
                    )

                    if (isImage && !baseName.equals("thumb", ignoreCase = true)) {
                        try {
                            decodeSampledBitmap(bytes, maxDim = 2048)?.let { bmp ->
                                bitmaps[rawName] = bmp
                                bitmaps[cleanName] = bmp
                                bitmaps[baseName] = bmp
                            }
                        } catch (ignored: Throwable) {}
                    } else if (!baseName.equals("thumb", ignoreCase = true)) {
                        // Check if TTF/OTF font
                        val isFont = bytes.size >= 4 && (
                            (bytes[0] == 0x00.toByte() && bytes[1] == 0x01.toByte() && bytes[2] == 0x00.toByte() && bytes[3] == 0x00.toByte()) ||
                            (bytes[0] == 'O'.code.toByte() && bytes[1] == 'T'.code.toByte() && bytes[2] == 'T'.code.toByte() && bytes[3] == 'O'.code.toByte()) ||
                            baseName.endsWith(".ttf", true) || baseName.endsWith(".otf", true)
                        )
                        if (isFont) {
                            try {
                                val fontsDir = File(context?.filesDir ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp"), "plp_fonts").apply { mkdirs() }
                                val fontExt = if (bytes[0] == 'O'.code.toByte()) ".otf" else ".ttf"
                                val fontFile = File(fontsDir, if (baseName.contains('.')) baseName else "$baseName$fontExt")
                                fontFile.writeBytes(bytes)
                                fonts[rawName] = fontFile.absolutePath
                                fonts[cleanName] = fontFile.absolutePath
                                fonts[baseName] = fontFile.absolutePath
                            } catch (ignored: Exception) {}
                        }
                    }

                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            // If zip stream failed but bytes could be JSON text
            val textCandidate = String(allBytes, Charsets.UTF_8)
            if (textCandidate.contains("\"objectsBundle\"")) {
                return parseNativePixelLabPlp(textCandidate, emptyMap(), emptyMap())
            } else if (textCandidate.contains("\"layers\"")) {
                return parseCustomManifest(textCandidate, emptyMap())
            }
            throw IllegalArgumentException("خطا در باز کردن بسته PLP: ${e.localizedMessage}")
        }

        // Branch A: Official PixelLab project (data.plab)
        if (nativePlabJson != null) {
            return parseNativePixelLabPlp(nativePlabJson!!, bitmaps, fonts)
        }

        // Branch B: AI Studio custom project.json
        if (customManifestJson != null) {
            return parseCustomManifest(customManifestJson!!, bitmaps)
        }

        throw IllegalArgumentException("ساختار فایل PLP معتبر نیست: مانیفست لایه‌ها (data.plab یا project.json) در فایل یافت نشد.")
    }

    private fun parseCustomManifest(jsonStr: String, bitmapsByEntry: Map<String, Bitmap>): StudioProject {
        val root = JSONObject(jsonStr)

        val bgObj = root.optJSONObject("background") ?: JSONObject()
        val bgAssetPath = bgObj.optString("bgAsset", "")
        val bgBitmap = if (bgAssetPath.isNotBlank() && !bgAssetPath.equals("thumb", ignoreCase = true)) {
            bitmapsByEntry[bgAssetPath]
        } else null
        val rawBgType = runCatching { BackgroundType.valueOf(bgObj.optString("type", "GRADIENT")) }.getOrDefault(BackgroundType.GRADIENT)
        val resolvedBgType = if (rawBgType == BackgroundType.IMAGE && bgBitmap == null) BackgroundType.GRADIENT else rawBgType

        val background = CanvasBackground(
            type = resolvedBgType,
            primaryColorHex = bgObj.optString("primaryColorHex", "#0F172A"),
            secondaryColorHex = bgObj.optString("secondaryColorHex", "#311042"),
            gradientAngle = bgObj.optDouble("gradientAngle", 135.0).toFloat(),
            isRadialGradient = bgObj.optBoolean("isRadialGradient", false),
            backgroundBitmap = bgBitmap
        )

        val fxObj = root.optJSONObject("postFx") ?: JSONObject()
        val postFx = CanvasPostFx(
            vignetteEnabled = fxObj.optBoolean("vignetteEnabled", true),
            vignetteColorHex = fxObj.optString("vignetteColorHex", "#000000"),
            vignetteIntensity = fxObj.optDouble("vignetteIntensity", 0.35).toFloat(),
            noiseEnabled = fxObj.optBoolean("noiseEnabled", false),
            noiseIntensity = fxObj.optDouble("noiseIntensity", 0.12).toFloat(),
            stripesEnabled = fxObj.optBoolean("stripesEnabled", false),
            stripesAlpha = fxObj.optDouble("stripesAlpha", 0.08).toFloat(),
            stripesSpacing = fxObj.optDouble("stripesSpacing", 14.0).toFloat(),
            brightness = fxObj.optDouble("brightness", 0.0).toFloat(),
            contrast = fxObj.optDouble("contrast", 1.0).toFloat(),
            saturation = fxObj.optDouble("saturation", 1.0).toFloat(),
            hue = fxObj.optDouble("hue", 0.0).toFloat()
        )

        val layers = mutableListOf<StudioLayer>()
        val layersArray = root.optJSONArray("layers") ?: JSONArray()
        for (i in 0 until layersArray.length()) {
            val layerObj = layersArray.getJSONObject(i)
            deserializeLayer(layerObj, bitmapsByEntry)?.let { layers.add(it) }
        }

        return StudioProject(
            id = root.optString("id", UUID.randomUUID().toString()),
            title = root.optString("title", "پروژه بازیابی‌شده PLP"),
            canvasWidth = root.optInt("canvasWidth", 1080),
            canvasHeight = root.optInt("canvasHeight", 1080),
            background = background,
            postFx = postFx,
            layers = layers
        )
    }

    private data class PlabGradientResult(
        val primaryColorHex: String,
        val secondaryColorHex: String,
        val angle: Float,
        val isRadial: Boolean
    )

    private fun intToHexColor(color: Int): String {
        val alpha = (color ushr 24) and 0xFF
        val red = (color ushr 16) and 0xFF
        val green = (color ushr 8) and 0xFF
        val blue = color and 0xFF
        return if (alpha < 255) {
            String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
        } else {
            String.format("#%02X%02X%02X", red, green, blue)
        }
    }

    private fun parsePlabColorAndSpans(
        value: Any?,
        textLength: Int,
        defaultHex: String = "#FFFFFF"
    ): Pair<String, List<TextColorSpan>> {
        return when (value) {
            is Number -> intToHexColor(value.toInt()) to emptyList()
            is String -> {
                val trimmed = value.trim()
                val hex = if (trimmed.startsWith("#")) {
                    trimmed
                } else {
                    trimmed.toLongOrNull()?.let { intToHexColor(it.toInt()) } ?: defaultHex
                }
                hex to emptyList()
            }
            is JSONObject -> {
                var baseColorHex: String? = null
                val rawIntervals = mutableListOf<Triple<Int, Int, String>>()
                val keys = value.keys().asSequence().toList().sorted()
                for (k in keys) {
                    val sub = value.optJSONObject(k) ?: continue
                    if (sub.has("textsIntervalsColor")) {
                        val cInt = sub.optInt("textsIntervalsColor")
                        val hex = intToHexColor(cInt)
                        val start = sub.optInt("textsIntervalsStart", 0).coerceAtLeast(0)
                        val end = sub.optInt("textsIntervalsEnd", textLength).coerceAtLeast(start)
                        rawIntervals.add(Triple(start, end, hex))
                        if (start == 0 && end >= textLength && baseColorHex == null) {
                            baseColorHex = hex
                        }
                    }
                }
                val resolvedBase = baseColorHex ?: rawIntervals.firstOrNull()?.third ?: defaultHex
                val spans = rawIntervals
                    .filter { (s, e, h) -> e > s && !(s == 0 && e >= textLength && h.equals(resolvedBase, ignoreCase = true)) }
                    .map { (s, e, h) -> TextColorSpan(start = s, end = e, colorHex = h) }
                resolvedBase to spans
            }
            else -> defaultHex to emptyList()
        }
    }

    private fun parsePixelLabGradient(gradStr: String?): PlabGradientResult? {
        if (gradStr.isNullOrBlank() || !gradStr.startsWith("v2;")) return null
        val parts = gradStr.split(";")
        if (parts.size < 4) return null
        val type = parts.getOrNull(1)?.toIntOrNull() ?: 3
        val isRadial = type == 4 || type == 6
        val colorPart = parts.getOrNull(3) ?: return null
        val colorStrings = colorPart.split("/").filter { it.isNotBlank() }
        if (colorStrings.isEmpty()) return null
        val c1 = colorStrings.firstOrNull()?.toLongOrNull()?.toInt() ?: return null
        val c2 = colorStrings.getOrNull(1)?.toLongOrNull()?.toInt() ?: c1
        // In PixelLab radial gradients (type 4/6), c1 is inner center color and c2 is outer edge color.
        // StudioCanvasRenderer RadialGradient uses intArrayOf(c2, c1), so swap for radial to preserve exact center/outer colors:
        val primaryHex = if (isRadial) intToHexColor(c2) else intToHexColor(c1)
        val secondaryHex = if (isRadial) intToHexColor(c1) else intToHexColor(c2)
        return PlabGradientResult(
            primaryColorHex = primaryHex,
            secondaryColorHex = secondaryHex,
            angle = if (isRadial) 0f else 135f,
            isRadial = isRadial
        )
    }

    private fun parseNativePixelLabPlp(
        jsonStr: String,
        bitmaps: Map<String, Bitmap>,
        fonts: Map<String, String>
    ): StudioProject {
        val root = JSONObject(jsonStr)
        val canvasWidth = root.optInt("bgWidth", 1080).coerceAtLeast(100)
        val canvasHeight = root.optInt("bgHeight", 1920).coerceAtLeast(100)

        // Background: strictly NEVER use "thumb" preview image as canvas background!
        val bgColorInt = root.optInt("bgColor", -1)
        val bgColorHex = intToHexColor(bgColorInt)
        val bgGrad = parsePixelLabGradient(root.optString("bgGradientV2", ""))

        val bgPhotoObj = root.optJSONObject("bgPhotoSrc")
        val bgPhotoPath = bgPhotoObj?.optString("imgSrcPath", "") ?: ""
        val bgResName = if (bgPhotoPath.isNotBlank()) File(bgPhotoPath).name else ""
        val bgBitmap = if (bgResName.isNotBlank() && !bgResName.equals("thumb", ignoreCase = true)) {
            bitmaps[bgResName] ?: bitmaps["res/$bgResName"] ?: bitmaps["/res/$bgResName"]
        } else {
            null
        }

        val bgType = when {
            bgBitmap != null -> BackgroundType.IMAGE
            bgGrad != null || root.optInt("bgType", 0) == 1 -> BackgroundType.GRADIENT
            else -> BackgroundType.COLOR
        }

        val background = CanvasBackground(
            type = bgType,
            primaryColorHex = bgGrad?.primaryColorHex ?: bgColorHex,
            secondaryColorHex = bgGrad?.secondaryColorHex ?: "#1E1B4B",
            gradientAngle = bgGrad?.angle ?: 135f,
            isRadialGradient = bgGrad?.isRadial ?: false,
            backgroundBitmap = if (bgType == BackgroundType.IMAGE) bgBitmap else null
        )

        // Post FX
        val postFx = CanvasPostFx(
            vignetteEnabled = root.optBoolean("effectVignetteEnabled", false),
            vignetteColorHex = intToHexColor(root.optInt("effectVignetteColor", -16777216)),
            vignetteIntensity = (root.optInt("effectVignetteAlpha", 128).toFloat() / 255f).coerceIn(0f, 1f),
            noiseEnabled = root.optBoolean("effectNoiseEnabled", false),
            noiseIntensity = (root.optInt("effectNoiseAlpha", 50).toFloat() / 255f).coerceIn(0f, 1f),
            stripesEnabled = root.optBoolean("effectStripesEnabled", false),
            stripesAlpha = (root.optInt("effectStripesAlpha", 90).toFloat() / 255f).coerceIn(0f, 1f),
            brightness = (root.optInt("effectBrightnessVal", 0) / 100f).coerceIn(-1f, 1f),
            contrast = (1f + root.optInt("effectContrastVal", 0) / 100f).coerceIn(0.2f, 3f),
            saturation = (1f + root.optInt("effectSaturationVal", 0) / 100f).coerceIn(0f, 3f),
            hue = (root.optInt("effectHueVal", 0) * 1.8f)
        )

        val rawLayers = mutableListOf<StudioLayer>()
        val objectsIds = root.optJSONArray("objectsIds") ?: JSONArray()
        val objectsBundle = root.optJSONObject("objectsBundle") ?: JSONObject()

        var detectedProjectTitle: String? = null

        for (i in 0 until objectsIds.length()) {
            val id = objectsIds.optString(i) ?: continue
            val obj = objectsBundle.optJSONObject(id) ?: continue

            val isText = obj.has("textTextString") || id.startsWith("text", ignoreCase = true)
            val contW = obj.optDouble("shapeOldContainerWidth", canvasWidth.toDouble()).toFloat().coerceAtLeast(1f)
            val contH = obj.optDouble("shapeOldContainerHeight", canvasHeight.toDouble()).toFloat().coerceAtLeast(1f)

            val isLocked = obj.optBoolean("objectLocked", false)
            val isVisible = !obj.optBoolean("objectHidden", false)
            val opacityRaw = if (obj.has("shapeOpacity")) obj.optDouble("shapeOpacity", 100.0) else obj.optDouble("textOpacity", 100.0)
            val opacity = (opacityRaw / 100.0).toFloat().coerceIn(0f, 1f)
            val rotZ = (if (obj.has("shapeRotation")) obj.optDouble("shapeRotation", 0.0) else obj.optDouble("textRotation", 0.0)).toFloat()
            val rotX = (if (obj.has("shapeRotationX")) obj.optDouble("shapeRotationX", 0.0) else obj.optDouble("textRotationX", 0.0)).toFloat()
            val rotY = (if (obj.has("shapeRotationY")) obj.optDouble("shapeRotationY", 0.0) else obj.optDouble("textRotationY", 0.0)).toFloat()

            // Stroke
            val strokeEnabled = obj.optBoolean("shapeStrokeEnabled", obj.optBoolean("textStrokeEnabled", false))
            val strokeColorInt = if (obj.has("shapeStrokeColor")) obj.optInt("shapeStrokeColor", -16777216) else obj.optInt("textStrokeColor", -16777216)
            val strokeWidth = (if (obj.has("shapeStrokeWidth")) obj.optDouble("shapeStrokeWidth", 4.0) else obj.optDouble("textStrokeWidth", 4.0)).toFloat()
            val stroke = LayerStroke(
                enabled = strokeEnabled,
                colorHex = intToHexColor(strokeColorInt),
                width = strokeWidth
            )

            // Shadow
            val shadowEnabled = obj.optBoolean("shapeShadowEnabled", obj.optBoolean("textOuterEnabled", false))
            val shadowColorInt = if (obj.has("shapeShadowColor")) obj.optInt("shapeShadowColor", -16777216) else obj.optInt("textOuterColor", -16777216)
            val shadowBlur = (if (obj.has("shapeShadowRadius")) obj.optDouble("shapeShadowRadius", 12.0) else obj.optDouble("textOuterRadius", 12.0)).toFloat()
            val shadowDx = (if (obj.has("shapeShadowDx")) obj.optDouble("shapeShadowDx", 2.0) else obj.optDouble("textOuterDx", 2.0)).toFloat()
            val shadowDy = (if (obj.has("shapeShadowDy")) obj.optDouble("shapeShadowDy", 4.0) else obj.optDouble("textOuterDy", 4.0)).toFloat()
            val shadow = LayerShadow(
                enabled = shadowEnabled,
                colorHex = intToHexColor(shadowColorInt),
                blurRadius = shadowBlur,
                offsetX = shadowDx,
                offsetY = shadowDy,
                outerGlow = obj.optBoolean("textOuterGlowEnabled", false)
            )

            // Inner Shadow
            val innerEnabled = obj.optBoolean("shapeInnerEnabled", obj.optBoolean("textInnerEnabled", false))
            val innerColorInt = if (obj.has("shapeInnerColor")) obj.optInt("shapeInnerColor", -16777216) else obj.optInt("textInnerColor", -16777216)
            val innerBlur = (if (obj.has("shapeInnerRadius")) obj.optDouble("shapeInnerRadius", 6.0) else obj.optDouble("textInnerRadius", 6.0)).toFloat()
            val innerDx = (if (obj.has("shapeInnerDx")) obj.optDouble("shapeInnerDx", 0.0) else obj.optDouble("textInnerDx", 0.0)).toFloat()
            val innerDy = (if (obj.has("shapeInnerDy")) obj.optDouble("shapeInnerDy", 0.0) else obj.optDouble("textInnerDy", 0.0)).toFloat()
            val innerShadow = LayerInnerShadow(
                enabled = innerEnabled,
                colorHex = intToHexColor(innerColorInt),
                blurRadius = innerBlur,
                offsetX = innerDx,
                offsetY = innerDy
            )

            // Emboss
            val embossEnabled = obj.optBoolean("shapeEmbossEnabled", obj.optBoolean("textEmbossEnabled", false))
            val emboss = LayerEmboss(
                enabled = embossEnabled,
                lightAngle = obj.optDouble("shapeEmbossLightAngle", obj.optDouble("textEmbossLightAngle", 90.0)).toFloat(),
                intensity = obj.optDouble("shapeEmbossIntensity", obj.optDouble("textEmbossIntensity", 65.0)).toFloat(),
                ambientLight = obj.optDouble("shapeEmbossAmbient", obj.optDouble("textEmbossAmbient", 50.0)).toFloat(),
                specularHardness = obj.optDouble("shapeEmbossHardness", obj.optDouble("textEmbossHardness", 40.0)).toFloat(),
                bevelWidth = obj.optDouble("shapeEmbossBevel", obj.optDouble("textEmbossBevel", 6.0)).toFloat()
            )

            // 3D
            val is3dEnabled = obj.optBoolean("textThreeDEnabled", obj.optBoolean("shapeThreeDEnabled", false))
            val depth3d = obj.optInt("textThreeDDepth", obj.optInt("shapeThreeDDepth", 16))
            val darken3d = (obj.optDouble("textThreeDDepthDarken", 30.0) / 100.0).toFloat().coerceIn(0.1f, 0.9f)
            val obliqueAngle3d = obj.optDouble("textThreeDObAn", obj.optDouble("ShpThObAng", 45.0)).toFloat()
            val isPerspective3d = obj.optInt("textThreeDViewType", 1) == 2

            if (isText) {
                val textString = obj.optString("textTextString", "متن")
                if (detectedProjectTitle == null && textString.isNotBlank() && textString.length > 2 && !textString.contains("🤯")) {
                    detectedProjectTitle = textString.take(30)
                }
                val rawTextSize = obj.optDouble("textTextSize", 50.0).toFloat()
                // Scale text size from container coordinate space (e.g., 933) to 1080 reference space
                val containerToRefScale = 1080f / minOf(contW, contH)
                val scaledFontSizeSp = (rawTextSize * containerToRefScale).coerceIn(10f, 320f)

                val shapeX = obj.optDouble("shapeX", (contW / 2f).toDouble()).toFloat()
                val shapeY = obj.optDouble("shapeY", (contH / 2f).toDouble()).toFloat()

                // Font resolution first so we can measure accurate text box bounds
                var customFontPath = ""
                var fontId = StudioFont.LALEZAR.id
                val fontObj = obj.optJSONObject("textTextFont")
                if (fontObj != null) {
                    val keys = fontObj.keys()
                    while (keys.hasNext()) {
                        val iv = fontObj.optJSONObject(keys.next())
                        val fPath = iv?.optString("textsIntervalsFont", "") ?: ""
                        if (fPath.isNotBlank()) {
                            val base = File(fPath).name
                            fonts[base]?.let { customFontPath = it }
                        }
                    }
                }
                if (customFontPath.isBlank()) {
                    fontId = if (textString.any { it in '\u0600'..'\u06FF' }) StudioFont.LALEZAR.id else StudioFont.MONTSERRAT.id
                }

                // In PixelLab data.plab, shapeX and shapeY are the TOP-LEFT corner of the TextView in contW x contH space.
                // Measure or estimate the TextView width and height in contW space to compute centerX and centerY:
                val lines = textString.ifBlank { " " }.split("\n")
                var estBoxW = 0f
                var estBoxH = 0f
                try {
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = rawTextSize
                        if (customFontPath.isNotBlank()) {
                            val tfFile = File(customFontPath)
                            if (tfFile.exists()) {
                                typeface = android.graphics.Typeface.createFromFile(tfFile)
                            }
                        }
                    }
                    val measuredMaxLine = lines.maxOfOrNull { paint.measureText(it) } ?: 0f
                    val fm = paint.fontMetrics
                    val lineH = (fm.descent - fm.ascent)
                    if (measuredMaxLine > 4f && lineH > 4f) {
                        estBoxW = measuredMaxLine + 24f
                        estBoxH = lineH * lines.size + 18f
                    }
                } catch (ignored: Throwable) {}

                if (estBoxW <= 4f || estBoxH <= 4f) {
                    val maxChars = lines.maxOfOrNull { it.length } ?: 1
                    val isEmojiOnly = maxChars <= 3 && textString.any { Character.isHighSurrogate(it) }
                    val charWidthFactor = if (isEmojiOnly) 1.05f else 0.31f
                    estBoxW = maxChars * rawTextSize * charWidthFactor + 24f
                    estBoxH = lines.size * rawTextSize * 1.28f + 16f
                }

                // Detect if shapeX/shapeY was saved by generateNativePixelLabJson as center vs native PixelLab top-left
                val isOurExportedCenter = obj.has("textCurColor") && !obj.has("textTextColor")
                val centerX = if (isOurExportedCenter) shapeX else (shapeX + estBoxW / 2f)
                val centerY = if (isOurExportedCenter) shapeY else (shapeY + estBoxH / 2f)

                val normX = (centerX / contW - 0.5f).coerceIn(-0.48f, 0.48f)
                val normY = (centerY / contH - 0.5f).coerceIn(-0.48f, 0.48f)

                val colorSource = obj.opt("textTextColor") ?: obj.opt("textCurColor")
                val (colorHex, colorSpans) = parsePlabColorAndSpans(colorSource, textString.length, "#FFFFFF")
                val gradObj = parsePixelLabGradient(obj.optString("textTextGradientV2", obj.optString("textGradientV2", "")))
                val useGradient = (obj.optInt("textTextFillType", 1) == 2 || obj.has("textGradientV2")) && gradObj != null

                val bgPadL = obj.optDouble("textBgPaddingLeft", 18.0).toFloat()
                val bgPadR = obj.optDouble("textBgPaddingRight", 18.0).toFloat()
                val bgPadT = obj.optDouble("textBgPaddingTop", 10.0).toFloat()
                val bgPadB = obj.optDouble("textBgPaddingBottom", 10.0).toFloat()

                val textLayer = TextLayer(
                    id = id,
                    name = textString.take(20).ifBlank { "متن $id" },
                    text = textString,
                    fontSizeSp = if (isOurExportedCenter) rawTextSize else scaledFontSizeSp,
                    fontId = fontId,
                    customFontPath = customFontPath,
                    colorHex = colorHex,
                    colorSpans = colorSpans,
                    useGradientFill = useGradient,
                    gradientSecondColorHex = gradObj?.secondaryColorHex ?: "#38BDF8",
                    gradientAngle = gradObj?.angle ?: 45f,
                    x = normX,
                    y = normY,
                    rotationZ = rotZ,
                    rotationX = rotX,
                    rotationY = rotY,
                    opacity = opacity,
                    isVisible = isVisible,
                    isLocked = isLocked,
                    isBold = obj.optBoolean("textBold", false),
                    isItalic = obj.optBoolean("textItalic", false),
                    isUnderline = obj.optBoolean("textUnder", false),
                    letterSpacing = (obj.optDouble("textLetterSpacing", 0.0).toFloat() / 10f).coerceIn(-5f, 30f),
                    stroke = stroke,
                    shadow = shadow,
                    innerShadow = innerShadow,
                    emboss = emboss,
                    is3dEnabled = is3dEnabled,
                    depth3d = depth3d,
                    darken3d = darken3d,
                    obliqueAngle3d = obliqueAngle3d,
                    isPerspective3d = isPerspective3d,
                    curveAngle = obj.optDouble("textTextCurve", obj.optDouble("textCurvingAngle", 0.0)).toFloat(),
                    backgroundPillEnabled = obj.optBoolean("textBgEnabled", obj.optBoolean("textBackgroundEnabled", false)),
                    backgroundPillColorHex = intToHexColor(obj.optInt("textBgColor", obj.optInt("textBackgroundColor", -1275068416))),
                    backgroundPillPadding = maxOf(bgPadL, bgPadR, 12f),
                    backgroundPillPaddingLeft = bgPadL,
                    backgroundPillPaddingRight = bgPadR,
                    backgroundPillPaddingTop = bgPadT,
                    backgroundPillPaddingBottom = bgPadB,
                    backgroundPillRadius = obj.optDouble("textBgBorderRadius", 0.0).toFloat(),
                    reflectionEnabled = obj.optBoolean("textReflectionEnabled", false),
                    shadow3dEnabled = obj.optBoolean("textThreeDShadowEnabled", obj.optBoolean("text3DShadowEnabled", false)),
                    textAlignCenter = true
                )
                rawLayers.add(textLayer)
            } else {
                val shapeImgSrcObj = obj.optJSONObject("shapeImageSrc")
                val imgSrcPath = shapeImgSrcObj?.optString("imgSrcPath", obj.optString("shapeImgSrcPath", "")) ?: ""
                val resName = if (imgSrcPath.isNotBlank()) File(imgSrcPath).name else ""
                var bmp = if (resName.isNotBlank() && !resName.equals("thumb", ignoreCase = true)) {
                    bitmaps[resName] ?: bitmaps["res/$resName"] ?: bitmaps["/res/$resName"]
                } else null

                // Apply PixelLab shapeImageCropInfo if sub-region crop is specified (0..100000 scale)
                val cropInfo = obj.optJSONObject("shapeImageCropInfo")
                val flipH = cropInfo?.optBoolean("mHbGHc", false) ?: false
                val flipV = cropInfo?.optBoolean("n7gV4v", false) ?: false
                val isCirc = cropInfo?.optBoolean("cropInfCirc", false) ?: false
                if (bmp != null && cropInfo != null) {
                    val cLeft = (cropInfo.optInt("cropInfoPorLeft", 0) / 100000f).coerceIn(0f, 0.95f)
                    val cTop = (cropInfo.optInt("cropInfoPorTop", 0) / 100000f).coerceIn(0f, 0.95f)
                    val cRight = (cropInfo.optInt("cropInfoPorRight", 100000) / 100000f).coerceIn(cLeft + 0.05f, 1f)
                    val cBottom = (cropInfo.optInt("cropInfoPorBottom", 100000) / 100000f).coerceIn(cTop + 0.05f, 1f)
                    if (cLeft > 0.01f || cTop > 0.01f || cRight < 0.99f || cBottom < 0.99f) {
                        try {
                            val subX = (cLeft * bmp.width).toInt().coerceIn(0, bmp.width - 1)
                            val subY = (cTop * bmp.height).toInt().coerceIn(0, bmp.height - 1)
                            val subW = ((cRight - cLeft) * bmp.width).toInt().coerceIn(1, bmp.width - subX)
                            val subH = ((cBottom - cTop) * bmp.height).toInt().coerceIn(1, bmp.height - subY)
                            bmp = Bitmap.createBitmap(bmp, subX, subY, subW, subH)
                        } catch (ignored: Throwable) {}
                    }
                }

                val shapeW = obj.optDouble("shapeWidth", (contW * 0.4).toDouble()).toFloat()
                val shapeH = obj.optDouble("shapeHeight", (contH * 0.4).toDouble()).toFloat()
                val shapeX = obj.optDouble("shapeX", (contW / 2f).toDouble()).toFloat()
                val shapeY = obj.optDouble("shapeY", (contH / 2f).toDouble()).toFloat()

                // In native PixelLab data.plab, shapeX and shapeY are the TOP-LEFT corner (left, top) of the bounding box!
                val isOurExportedCenter = obj.has("shapeImgSrcPath") && !obj.has("shapeImageSrc")
                val centerX = if (isOurExportedCenter) shapeX else (shapeX + shapeW / 2f)
                val centerY = if (isOurExportedCenter) shapeY else (shapeY + shapeH / 2f)

                val layerX = (centerX / contW - 0.5f).coerceIn(-1.5f, 1.5f)
                val layerY = (centerY / contH - 0.5f).coerceIn(-1.5f, 1.5f)

                if (bmp != null) {
                    val minDim = minOf(canvasWidth, canvasHeight).toFloat()
                    val baseSize = minDim * 0.45f
                    val aspect = bmp.width.toFloat() / bmp.height.coerceAtLeast(1).toFloat()
                    val drawW = if (aspect >= 1f) baseSize else baseSize * aspect
                    val drawH = if (aspect >= 1f) baseSize / aspect else baseSize

                    val targetWidth = shapeW * (canvasWidth.toFloat() / contW)
                    val targetHeight = shapeH * (canvasHeight.toFloat() / contH)

                    val scaleX = if (drawW > 0f) (targetWidth / drawW).coerceIn(0.05f, 20f) else 1f
                    val scaleY = if (drawH > 0f) (targetHeight / drawH).coerceIn(0.05f, 20f) else 1f

                    val eraseEnabled = obj.optBoolean("shapeRemoveColorEnabled", false)
                    val eraseArr = obj.optJSONArray("shapeRemoveColorCT")
                    val eraseColorInt = eraseArr?.optString(0)?.toIntOrNull() ?: -1
                    val eraseTolRaw = eraseArr?.optString(1)?.toIntOrNull() ?: 100
                    val eraseTol = (eraseTolRaw / 1000f).coerceIn(0.05f, 0.9f)

                    val colorOverlayEnabled = obj.optBoolean("shapeColorOverlayEnabled", false)
                    val colorOverlayInt = obj.optInt("shapeColorOverlayColor", -65536)

                    val imageLayer = ImageLayer(
                        id = id,
                        name = "تصویر ${resName.ifBlank { id }}",
                        bitmap = bmp,
                        x = layerX,
                        y = layerY,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        rotationZ = rotZ,
                        rotationX = rotX,
                        rotationY = rotY,
                        opacity = opacity,
                        isVisible = isVisible,
                        isLocked = isLocked,
                        stroke = stroke,
                        shadow = shadow,
                        innerShadow = innerShadow,
                        emboss = emboss,
                        isCircularMask = isCirc,
                        flipHorizontal = flipH,
                        flipVertical = flipV,
                        colorOverlayEnabled = colorOverlayEnabled,
                        colorOverlayHex = intToHexColor(colorOverlayInt),
                        eraseColorEnabled = eraseEnabled,
                        eraseTargetColorHex = intToHexColor(eraseColorInt),
                        eraseTolerance = eraseTol
                    )
                    rawLayers.add(imageLayer)
                } else {
                    val shapeLayer = ShapeLayer(
                        id = id,
                        name = "شکل $id",
                        x = layerX,
                        y = layerY,
                        scaleX = (shapeW / (contW * 0.4f)).coerceIn(0.1f, 10f),
                        scaleY = (shapeH / (contH * 0.4f)).coerceIn(0.1f, 10f),
                        rotationZ = rotZ,
                        rotationX = rotX,
                        rotationY = rotY,
                        opacity = opacity,
                        isVisible = isVisible,
                        isLocked = isLocked,
                        stroke = stroke,
                        shadow = shadow,
                        innerShadow = innerShadow,
                        emboss = emboss
                    )
                    rawLayers.add(shapeLayer)
                }
            }
        }

        // If every single layer in the imported .plp template was locked by its creator, unlock them so the user can immediately select/edit on canvas
        val allLocked = rawLayers.isNotEmpty() && rawLayers.all { it.isLocked }
        val layers = if (allLocked) {
            rawLayers.map { l ->
                when (l) {
                    is TextLayer -> l.copy(isLocked = false)
                    is ImageLayer -> l.copy(isLocked = false)
                    is ShapeLayer -> l.copy(isLocked = false)
                    is DrawingLayer -> l.copy(isLocked = false)
                }
            }
        } else {
            rawLayers
        }

        return StudioProject(
            id = UUID.randomUUID().toString(),
            title = detectedProjectTitle ?: "پروژه پیکسل‌لب (${layers.size} لایه)",
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            background = background,
            postFx = postFx,
            layers = layers
        )
    }

    private fun serializeLayer(layer: StudioLayer): JSONObject = JSONObject().apply {
        put("id", layer.id)
        put("name", layer.name)
        put("isVisible", layer.isVisible)
        put("isLocked", layer.isLocked)
        put("opacity", layer.opacity.toDouble())
        put("blendMode", layer.blendMode.name)
        put("x", layer.x.toDouble())
        put("y", layer.y.toDouble())
        put("scaleX", layer.scaleX.toDouble())
        put("scaleY", layer.scaleY.toDouble())
        put("rotationZ", layer.rotationZ.toDouble())
        put("rotationX", layer.rotationX.toDouble())
        put("rotationY", layer.rotationY.toDouble())
        put("skewX", layer.skewX.toDouble())
        put("skewY", layer.skewY.toDouble())

        put("stroke", JSONObject().apply {
            put("enabled", layer.stroke.enabled)
            put("colorHex", layer.stroke.colorHex)
            put("width", layer.stroke.width.toDouble())
        })
        put("shadow", JSONObject().apply {
            put("enabled", layer.shadow.enabled)
            put("colorHex", layer.shadow.colorHex)
            put("blurRadius", layer.shadow.blurRadius.toDouble())
            put("offsetX", layer.shadow.offsetX.toDouble())
            put("offsetY", layer.shadow.offsetY.toDouble())
            put("outerGlow", layer.shadow.outerGlow)
        })
        put("innerShadow", JSONObject().apply {
            put("enabled", layer.innerShadow.enabled)
            put("colorHex", layer.innerShadow.colorHex)
            put("blurRadius", layer.innerShadow.blurRadius.toDouble())
            put("offsetX", layer.innerShadow.offsetX.toDouble())
            put("offsetY", layer.innerShadow.offsetY.toDouble())
        })
        put("emboss", JSONObject().apply {
            put("enabled", layer.emboss.enabled)
            put("lightAngle", layer.emboss.lightAngle.toDouble())
            put("intensity", layer.emboss.intensity.toDouble())
            put("ambientLight", layer.emboss.ambientLight.toDouble())
            put("specularHardness", layer.emboss.specularHardness.toDouble())
            put("bevelWidth", layer.emboss.bevelWidth.toDouble())
        })

        when (layer) {
            is TextLayer -> {
                put("layerType", "TEXT")
                put("text", layer.text)
                put("fontId", layer.fontId)
                put("customFontPath", layer.customFontPath)
                put("fontSizeSp", layer.fontSizeSp.toDouble())
                put("colorHex", layer.colorHex)
                if (layer.colorSpans.isNotEmpty()) {
                    val spansArr = JSONArray()
                    layer.colorSpans.forEach { sp ->
                        spansArr.put(JSONObject().apply {
                            put("start", sp.start)
                            put("end", sp.end)
                            put("colorHex", sp.colorHex)
                        })
                    }
                    put("colorSpans", spansArr)
                }
                put("useGradientFill", layer.useGradientFill)
                put("gradientSecondColorHex", layer.gradientSecondColorHex)
                put("gradientAngle", layer.gradientAngle.toDouble())
                put("paddingLeft", layer.paddingLeft.toDouble())
                put("paddingRight", layer.paddingRight.toDouble())
                put("texturePreset", layer.texturePreset.name)
                put("textureScale", layer.textureScale.toDouble())
                if (layer.customTextureBitmap != null) {
                    put("textureAssetPath", "assets/texture_${layer.id}.png")
                }
                put("maskMode", layer.maskMode.name)
                put("maskInvert", layer.maskInvert)
                put("maskOffset", layer.maskOffset.toDouble())
                put("isBold", layer.isBold)
                put("isItalic", layer.isItalic)
                put("isUnderline", layer.isUnderline)
                put("letterSpacing", layer.letterSpacing.toDouble())
                put("lineSpacing", layer.lineSpacing.toDouble())
                put("textAlignCenter", layer.textAlignCenter)
                put("textAlignMode", layer.textAlignMode.name)
                put("backgroundPillEnabled", layer.backgroundPillEnabled)
                put("backgroundPillColorHex", layer.backgroundPillColorHex)
                put("backgroundPillPadding", layer.backgroundPillPadding.toDouble())
                put("backgroundPillPaddingLeft", layer.backgroundPillPaddingLeft.toDouble())
                put("backgroundPillPaddingRight", layer.backgroundPillPaddingRight.toDouble())
                put("backgroundPillPaddingTop", layer.backgroundPillPaddingTop.toDouble())
                put("backgroundPillPaddingBottom", layer.backgroundPillPaddingBottom.toDouble())
                put("backgroundPillRadius", layer.backgroundPillRadius.toDouble())
                put("curveAngle", layer.curveAngle.toDouble())
                put("is3dEnabled", layer.is3dEnabled)
                put("depth3d", layer.depth3d)
                put("darken3d", layer.darken3d.toDouble())
                put("obliqueAngle3d", layer.obliqueAngle3d.toDouble())
                put("isPerspective3d", layer.isPerspective3d)
                put("shadow3dEnabled", layer.shadow3dEnabled)
                put("shadow3dColorHex", layer.shadow3dColorHex)
                put("shadow3dAlpha", layer.shadow3dAlpha.toDouble())
                put("shadow3dExpand", layer.shadow3dExpand.toDouble())
                put("reflectionEnabled", layer.reflectionEnabled)
                put("reflectionOffset", layer.reflectionOffset.toDouble())
                put("reflectionAlpha", layer.reflectionAlpha.toDouble())
            }
            is ShapeLayer -> {
                put("layerType", "SHAPE")
                put("shapeKind", layer.shapeKind.name)
                put("sizeRatio", layer.sizeRatio.toDouble())
                put("fillColorHex", layer.fillColorHex)
                put("useGradientFill", layer.useGradientFill)
                put("gradientSecondColorHex", layer.gradientSecondColorHex)
                put("texturePreset", layer.texturePreset.name)
                put("textureScale", layer.textureScale.toDouble())
                put("cornerRadius", layer.cornerRadius.toDouble())
                put("is3dExtruded", layer.is3dExtruded)
                put("depth3d", layer.depth3d)
            }
            is ImageLayer -> {
                put("layerType", "IMAGE")
                put("assetPath", "assets/layer_${layer.id}.png")
                put("isCircularMask", layer.isCircularMask)
                put("maskMode", layer.maskMode.name)
                put("maskInvert", layer.maskInvert)
                put("cornerRadius", layer.cornerRadius.toDouble())
                put("cropLeft", layer.cropLeft.toDouble())
                put("cropTop", layer.cropTop.toDouble())
                put("cropRight", layer.cropRight.toDouble())
                put("cropBottom", layer.cropBottom.toDouble())
                put("flipHorizontal", layer.flipHorizontal)
                put("flipVertical", layer.flipVertical)
                put("colorOverlayEnabled", layer.colorOverlayEnabled)
                put("colorOverlayHex", layer.colorOverlayHex)
                put("eraseColorEnabled", layer.eraseColorEnabled)
                put("eraseTargetColorHex", layer.eraseTargetColorHex)
                put("eraseTolerance", layer.eraseTolerance.toDouble())
                put("hue", layer.hue.toDouble())
                put("saturation", layer.saturation.toDouble())
                put("brightness", layer.brightness.toDouble())
                put("contrast", layer.contrast.toDouble())
                if (layer.eraserStrokes.isNotEmpty()) {
                    val erArr = JSONArray()
                    layer.eraserStrokes.forEach { st ->
                        val ptsArr = JSONArray()
                        st.points.forEach { pt ->
                            ptsArr.put(JSONObject().apply {
                                put("x", pt.x.toDouble())
                                put("y", pt.y.toDouble())
                            })
                        }
                        erArr.put(JSONObject().apply {
                            put("id", st.id)
                            put("strokeWidth", st.strokeWidth.toDouble())
                            put("isEraser", true)
                            put("points", ptsArr)
                        })
                    }
                    put("eraserStrokes", erArr)
                }
            }
            is DrawingLayer -> {
                put("layerType", "DRAWING")
                val strokesArr = JSONArray()
                layer.strokes.forEach { st ->
                    val ptsArr = JSONArray()
                    st.points.forEach { pt ->
                        ptsArr.put(JSONObject().apply {
                            put("x", pt.x.toDouble())
                            put("y", pt.y.toDouble())
                        })
                    }
                    strokesArr.put(JSONObject().apply {
                        put("id", st.id)
                        put("colorHex", st.colorHex)
                        put("strokeWidth", st.strokeWidth.toDouble())
                        put("smoothness", st.smoothness.toDouble())
                        put("isDashed", st.isDashed)
                        put("neonGlow", st.neonGlow)
                        put("isEraser", st.isEraser)
                        put("points", ptsArr)
                    })
                }
                put("strokes", strokesArr)
            }
        }
    }

    private fun deserializeLayer(obj: JSONObject, bitmaps: Map<String, Bitmap>): StudioLayer? {
        val id = obj.optString("id", UUID.randomUUID().toString())
        val name = obj.optString("name", "لایه")
        val isVisible = obj.optBoolean("isVisible", true)
        val isLocked = obj.optBoolean("isLocked", false)
        val opacity = obj.optDouble("opacity", 1.0).toFloat()
        val blendMode = runCatching { LayerBlendMode.valueOf(obj.optString("blendMode", "NORMAL")) }.getOrDefault(LayerBlendMode.NORMAL)
        val x = obj.optDouble("x", 0.0).toFloat()
        val y = obj.optDouble("y", 0.0).toFloat()
        val scaleX = obj.optDouble("scaleX", 1.0).toFloat()
        val scaleY = obj.optDouble("scaleY", 1.0).toFloat()
        val rotationZ = obj.optDouble("rotationZ", 0.0).toFloat()
        val rotationX = obj.optDouble("rotationX", 0.0).toFloat()
        val rotationY = obj.optDouble("rotationY", 0.0).toFloat()
        val skewX = obj.optDouble("skewX", 0.0).toFloat()
        val skewY = obj.optDouble("skewY", 0.0).toFloat()

        val stObj = obj.optJSONObject("stroke") ?: JSONObject()
        val stroke = LayerStroke(
            enabled = stObj.optBoolean("enabled", false),
            colorHex = stObj.optString("colorHex", "#000000"),
            width = stObj.optDouble("width", 6.0).toFloat()
        )
        val shObj = obj.optJSONObject("shadow") ?: JSONObject()
        val shadow = LayerShadow(
            enabled = shObj.optBoolean("enabled", true),
            colorHex = shObj.optString("colorHex", "#AA000000"),
            blurRadius = shObj.optDouble("blurRadius", 16.0).toFloat(),
            offsetX = shObj.optDouble("offsetX", 4.0).toFloat(),
            offsetY = shObj.optDouble("offsetY", 8.0).toFloat(),
            outerGlow = shObj.optBoolean("outerGlow", false)
        )
        val inShObj = obj.optJSONObject("innerShadow") ?: JSONObject()
        val innerShadow = LayerInnerShadow(
            enabled = inShObj.optBoolean("enabled", false),
            colorHex = inShObj.optString("colorHex", "#88000000"),
            blurRadius = inShObj.optDouble("blurRadius", 8.0).toFloat(),
            offsetX = inShObj.optDouble("offsetX", 2.0).toFloat(),
            offsetY = inShObj.optDouble("offsetY", 2.0).toFloat()
        )
        val emObj = obj.optJSONObject("emboss") ?: JSONObject()
        val emboss = LayerEmboss(
            enabled = emObj.optBoolean("enabled", false),
            lightAngle = emObj.optDouble("lightAngle", 45.0).toFloat(),
            intensity = emObj.optDouble("intensity", 65.0).toFloat(),
            ambientLight = emObj.optDouble("ambientLight", 50.0).toFloat(),
            specularHardness = emObj.optDouble("specularHardness", 40.0).toFloat(),
            bevelWidth = emObj.optDouble("bevelWidth", 6.0).toFloat()
        )

        return when (obj.optString("layerType", "TEXT")) {
            "TEXT" -> {
                val texAsset = obj.optString("textureAssetPath", "assets/texture_$id.png")
                val spansList = mutableListOf<TextColorSpan>()
                val spansArr = obj.optJSONArray("colorSpans")
                if (spansArr != null) {
                    for (i in 0 until spansArr.length()) {
                        val sp = spansArr.optJSONObject(i) ?: continue
                        spansList.add(
                            TextColorSpan(
                                start = sp.optInt("start", 0),
                                end = sp.optInt("end", 0),
                                colorHex = sp.optString("colorHex", "#FFFFFF")
                            )
                        )
                    }
                }
                val basePad = obj.optDouble("backgroundPillPadding", 18.0).toFloat()
                TextLayer(
                    id = id, name = name, isVisible = isVisible, isLocked = isLocked,
                    opacity = opacity, blendMode = blendMode, x = x, y = y,
                    scaleX = scaleX, scaleY = scaleY, rotationZ = rotationZ,
                    rotationX = rotationX, rotationY = rotationY, skewX = skewX, skewY = skewY,
                    stroke = stroke, shadow = shadow, innerShadow = innerShadow, emboss = emboss,
                    text = obj.optString("text", "متن"),
                    fontId = obj.optString("fontId", StudioFont.LALEZAR.id),
                    customFontPath = obj.optString("customFontPath", ""),
                    fontSizeSp = obj.optDouble("fontSizeSp", 54.0).toFloat(),
                    colorHex = obj.optString("colorHex", "#FFFFFF"),
                    colorSpans = spansList,
                    useGradientFill = obj.optBoolean("useGradientFill", false),
                    gradientSecondColorHex = obj.optString("gradientSecondColorHex", "#38BDF8"),
                    gradientAngle = obj.optDouble("gradientAngle", 45.0).toFloat(),
                    paddingLeft = obj.optDouble("paddingLeft", 0.0).toFloat(),
                    paddingRight = obj.optDouble("paddingRight", 0.0).toFloat(),
                    texturePreset = runCatching { TexturePreset.valueOf(obj.optString("texturePreset", "NONE")) }.getOrDefault(TexturePreset.NONE),
                    textureScale = obj.optDouble("textureScale", 1.0).toFloat(),
                    customTextureBitmap = bitmaps[texAsset],
                    maskMode = runCatching { MaskPresetMode.valueOf(obj.optString("maskMode", "NONE")) }.getOrDefault(MaskPresetMode.NONE),
                    maskInvert = obj.optBoolean("maskInvert", false),
                    maskOffset = obj.optDouble("maskOffset", 0.0).toFloat(),
                    isBold = obj.optBoolean("isBold", false),
                    isItalic = obj.optBoolean("isItalic", false),
                    isUnderline = obj.optBoolean("isUnderline", false),
                    letterSpacing = obj.optDouble("letterSpacing", 0.0).toFloat(),
                    lineSpacing = obj.optDouble("lineSpacing", 1.15).toFloat(),
                    textAlignCenter = obj.optBoolean("textAlignCenter", true),
                    textAlignMode = runCatching {
                        TextAlignMode.valueOf(
                            obj.optString(
                                "textAlignMode",
                                if (obj.optBoolean("textAlignCenter", true)) "CENTER" else "RIGHT"
                            )
                        )
                    }.getOrDefault(TextAlignMode.CENTER),
                    backgroundPillEnabled = obj.optBoolean("backgroundPillEnabled", false),
                    backgroundPillColorHex = obj.optString("backgroundPillColorHex", "#880F172A"),
                    backgroundPillPadding = basePad,
                    backgroundPillPaddingLeft = obj.optDouble("backgroundPillPaddingLeft", basePad.toDouble()).toFloat(),
                    backgroundPillPaddingRight = obj.optDouble("backgroundPillPaddingRight", basePad.toDouble()).toFloat(),
                    backgroundPillPaddingTop = obj.optDouble("backgroundPillPaddingTop", (basePad * 0.6f).toDouble()).toFloat(),
                    backgroundPillPaddingBottom = obj.optDouble("backgroundPillPaddingBottom", (basePad * 0.6f).toDouble()).toFloat(),
                    backgroundPillRadius = obj.optDouble("backgroundPillRadius", 16.0).toFloat(),
                    curveAngle = obj.optDouble("curveAngle", 0.0).toFloat(),
                    is3dEnabled = obj.optBoolean("is3dEnabled", false),
                    depth3d = obj.optInt("depth3d", 16),
                    darken3d = obj.optDouble("darken3d", 0.45).toFloat(),
                    obliqueAngle3d = obj.optDouble("obliqueAngle3d", 135.0).toFloat(),
                    isPerspective3d = obj.optBoolean("isPerspective3d", false),
                    shadow3dEnabled = obj.optBoolean("shadow3dEnabled", false),
                    shadow3dColorHex = obj.optString("shadow3dColorHex", "#000000"),
                    shadow3dAlpha = obj.optDouble("shadow3dAlpha", 0.65).toFloat(),
                    shadow3dExpand = obj.optDouble("shadow3dExpand", 1.0).toFloat(),
                    reflectionEnabled = obj.optBoolean("reflectionEnabled", false),
                    reflectionOffset = obj.optDouble("reflectionOffset", 12.0).toFloat(),
                    reflectionAlpha = obj.optDouble("reflectionAlpha", 0.35).toFloat()
                )
            }
            "SHAPE" -> ShapeLayer(
                id = id, name = name, isVisible = isVisible, isLocked = isLocked,
                opacity = opacity, blendMode = blendMode, x = x, y = y,
                scaleX = scaleX, scaleY = scaleY, rotationZ = rotationZ,
                rotationX = rotationX, rotationY = rotationY, skewX = skewX, skewY = skewY,
                stroke = stroke, shadow = shadow, innerShadow = innerShadow, emboss = emboss,
                shapeKind = runCatching { ShapeKind.valueOf(obj.optString("shapeKind", "HEXAGON")) }.getOrDefault(ShapeKind.HEXAGON),
                sizeRatio = obj.optDouble("sizeRatio", 0.32).toFloat(),
                fillColorHex = obj.optString("fillColorHex", "#38BDF8"),
                useGradientFill = obj.optBoolean("useGradientFill", true),
                gradientSecondColorHex = obj.optString("gradientSecondColorHex", "#A855F7"),
                texturePreset = runCatching { TexturePreset.valueOf(obj.optString("texturePreset", "NONE")) }.getOrDefault(TexturePreset.NONE),
                textureScale = obj.optDouble("textureScale", 1.0).toFloat(),
                cornerRadius = obj.optDouble("cornerRadius", 24.0).toFloat(),
                is3dExtruded = obj.optBoolean("is3dExtruded", false),
                depth3d = obj.optInt("depth3d", 12)
            )
            "IMAGE" -> {
                val assetPath = obj.optString("assetPath", "assets/layer_$id.png")
                val erList = mutableListOf<DrawnStroke>()
                val erArr = obj.optJSONArray("eraserStrokes")
                if (erArr != null) {
                    for (i in 0 until erArr.length()) {
                        val sObj = erArr.optJSONObject(i) ?: continue
                        val pts = mutableListOf<Point2D>()
                        val pArr = sObj.optJSONArray("points") ?: JSONArray()
                        for (j in 0 until pArr.length()) {
                            val pObj = pArr.getJSONObject(j)
                            pts.add(Point2D(pObj.optDouble("x", 0.0).toFloat(), pObj.optDouble("y", 0.0).toFloat()))
                        }
                        erList.add(
                            DrawnStroke(
                                id = sObj.optString("id", UUID.randomUUID().toString()),
                                points = pts,
                                strokeWidth = sObj.optDouble("strokeWidth", 24.0).toFloat(),
                                isEraser = true
                            )
                        )
                    }
                }
                val loadedBmp = bitmaps[assetPath]
                ImageLayer(
                    id = id, name = name, isVisible = isVisible, isLocked = isLocked,
                    opacity = opacity, blendMode = blendMode, x = x, y = y,
                    scaleX = scaleX, scaleY = scaleY, rotationZ = rotationZ,
                    rotationX = rotationX, rotationY = rotationY, skewX = skewX, skewY = skewY,
                    stroke = stroke, shadow = shadow, innerShadow = innerShadow, emboss = emboss,
                    bitmap = loadedBmp,
                    originalBitmap = loadedBmp,
                    isCircularMask = obj.optBoolean("isCircularMask", false),
                    cropLeft = obj.optDouble("cropLeft", 0.0).toFloat(),
                    cropTop = obj.optDouble("cropTop", 0.0).toFloat(),
                    cropRight = obj.optDouble("cropRight", 1.0).toFloat(),
                    cropBottom = obj.optDouble("cropBottom", 1.0).toFloat(),
                    maskMode = runCatching { MaskPresetMode.valueOf(obj.optString("maskMode", "NONE")) }.getOrDefault(MaskPresetMode.NONE),
                    maskInvert = obj.optBoolean("maskInvert", false),
                    cornerRadius = obj.optDouble("cornerRadius", 0.0).toFloat(),
                    flipHorizontal = obj.optBoolean("flipHorizontal", false),
                    flipVertical = obj.optBoolean("flipVertical", false),
                    eraserStrokes = erList,
                    colorOverlayEnabled = obj.optBoolean("colorOverlayEnabled", false),
                    colorOverlayHex = obj.optString("colorOverlayHex", "#38BDF8"),
                    eraseColorEnabled = obj.optBoolean("eraseColorEnabled", false),
                    eraseTargetColorHex = obj.optString("eraseTargetColorHex", "#FFFFFF"),
                    eraseTolerance = obj.optDouble("eraseTolerance", 0.18).toFloat(),
                    hue = obj.optDouble("hue", 0.0).toFloat(),
                    saturation = obj.optDouble("saturation", 1.0).toFloat(),
                    brightness = obj.optDouble("brightness", 0.0).toFloat(),
                    contrast = obj.optDouble("contrast", 1.0).toFloat()
                )
            }
            "DRAWING" -> {
                val strokesList = mutableListOf<DrawnStroke>()
                val arr = obj.optJSONArray("strokes") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val sObj = arr.getJSONObject(i)
                    val pts = mutableListOf<Point2D>()
                    val pArr = sObj.optJSONArray("points") ?: JSONArray()
                    for (j in 0 until pArr.length()) {
                        val pObj = pArr.getJSONObject(j)
                        pts.add(Point2D(pObj.optDouble("x", 0.0).toFloat(), pObj.optDouble("y", 0.0).toFloat()))
                    }
                    strokesList.add(
                        DrawnStroke(
                            id = sObj.optString("id", UUID.randomUUID().toString()),
                            points = pts,
                            colorHex = sObj.optString("colorHex", "#38BDF8"),
                            strokeWidth = sObj.optDouble("strokeWidth", 12.0).toFloat(),
                            smoothness = sObj.optDouble("smoothness", 0.5).toFloat(),
                            isDashed = sObj.optBoolean("isDashed", false),
                            neonGlow = sObj.optBoolean("neonGlow", true),
                            isEraser = sObj.optBoolean("isEraser", false)
                        )
                    )
                }
                DrawingLayer(
                    id = id, name = name, isVisible = isVisible, isLocked = isLocked,
                    opacity = opacity, blendMode = blendMode, x = x, y = y,
                    scaleX = scaleX, scaleY = scaleY, rotationZ = rotationZ,
                    rotationX = rotationX, rotationY = rotationY, skewX = skewX, skewY = skewY,
                    stroke = stroke, shadow = shadow, innerShadow = innerShadow, emboss = emboss,
                    strokes = strokesList
                )
            }
            else -> null
        }
    }

    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        projectTitle: String,
        format: ExportFormat,
        quality: Int
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = projectTitle.replace(Regex("[^a-zA-Z0-9آ-ی_\\-]"), "_").ifBlank { "PixelLab_AI" }
            val filename = "${cleanTitle}_${System.currentTimeMillis()}.${format.extension}"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PixelLabAI")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext Result.failure(IllegalStateException("خطا در ایجاد فایل در گالری دستگاه"))

            resolver.openOutputStream(uri)?.use { out ->
                val compressFormat = when (format) {
                    ExportFormat.PNG -> Bitmap.CompressFormat.PNG
                    ExportFormat.JPG -> Bitmap.CompressFormat.JPEG
                    ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                }
                bitmap.compress(compressFormat, quality.coerceIn(50, 100), out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Writes the rendered canvas bitmap (PNG or JPG) to cacheDir/shared_exports and builds a
     * standard Android ACTION_SEND chooser Intent backed by FileProvider.
     */
    suspend fun createDirectShareImageIntent(
        context: Context,
        bitmap: Bitmap,
        projectTitle: String,
        format: ExportFormat,
        quality: Int
    ): Result<Intent> = withContext(Dispatchers.IO) {
        try {
            val shareDir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
            val cleanTitle = projectTitle.replace(Regex("[^a-zA-Z0-9آ-ی_\\-]"), "_").ifBlank { "PixelLab_AI" }
            val outFile = File(shareDir, "${cleanTitle}_${System.currentTimeMillis()}.${format.extension}")
            FileOutputStream(outFile).use { out ->
                val compressFormat = when (format) {
                    ExportFormat.PNG -> Bitmap.CompressFormat.PNG
                    ExportFormat.JPG -> Bitmap.CompressFormat.JPEG
                    ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                }
                bitmap.compress(compressFormat, quality.coerceIn(50, 100), out)
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, "$projectTitle (${format.extension.uppercase()})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "اشتراک‌گذاری مستقیم تصویر (${format.extension.uppercase()})").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            Result.success(chooser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }
}
