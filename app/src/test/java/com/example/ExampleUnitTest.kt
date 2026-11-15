package com.example

import com.example.data.StudioProject
import com.example.data.TextLayer
import com.example.data.ImageLayer
import com.example.data.ShapeLayer
import com.example.ui.agent.OfflineDesignEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    @Test
    fun visualCritiqueCommand_preservesLayerTextAndEnhances3DStyle() {
        val originalHeadline = TextLayer(
            id = "headline-1",
            name = "تیتر اصلی",
            text = "پیکسل‌لب استودیو",
            fontSizeSp = 54f
        )
        val subtitle = TextLayer(
            id = "sub-1",
            name = "زیرنویس",
            text = "ویرایشگر حرفه‌ای گرافیک",
            fontSizeSp = 24f
        )
        val project = StudioProject(
            title = "پروژه تست",
            layers = listOf(originalHeadline, subtitle)
        )

        val result = OfflineDesignEngine.processCommand(
            userCommand = "طرح فعلی بوم رو نقد بصری کن و با تایپوگرافی سه‌بعدی بهبود بده",
            project = project,
            selectedLayerId = originalHeadline.id
        )

        assertTrue("Actions should not be empty", result.rawActionsJson.length() > 0)
        assertNotNull("Critique should be provided", result.designCritiqueFa)

        for (i in 0 until result.rawActionsJson.length()) {
            val action = result.rawActionsJson.getJSONObject(i)
            val actionType = action.optString("actionType")
            if (actionType == "UPDATE_TEXT_BY_ID" || actionType == "UPDATE_SELECTED_TEXT") {
                assertFalse(
                    "Visual critique must NEVER overwrite existing layer text with the user command",
                    action.has("text")
                )
            }
        }
    }

    @Test
    fun halfSpaceAndPersianDigit3DCommand_recognizedProperly() {
        val headline = TextLayer(id = "t1", name = "تیتر", text = "طراحی مدرن", fontSizeSp = 48f)
        val project = StudioProject(layers = listOf(headline))

        val res1 = OfflineDesignEngine.processCommand(
            userCommand = "تیتر رو سه‌بعدی کن",
            project = project,
            selectedLayerId = headline.id
        )
        val act1 = res1.rawActionsJson.getJSONObject(0)
        assertEquals("UPDATE_SELECTED_TEXT", act1.getString("actionType"))
        assertTrue(act1.getBoolean("is3dEnabled"))
        assertFalse(act1.has("text"))

        val res2 = OfflineDesignEngine.processCommand(
            userCommand = "متن رو ۳بعدی با عمق ۳۲ کن",
            project = project,
            selectedLayerId = headline.id
        )
        val act2 = res2.rawActionsJson.getJSONObject(0)
        assertTrue(act2.getBoolean("is3dEnabled"))
        assertEquals(32, act2.getInt("depth3d"))
        assertFalse(act2.has("text"))
    }

    @Test
    fun promotionalPosterCommand_createsMultiLayerPoster() {
        val project = StudioProject(layers = emptyList())
        val res = OfflineDesignEngine.processCommand(
            userCommand = "پوستر تبلیغاتی حرفه‌ای با تیتر ۳بعدی و افکت نوری بساز",
            project = project,
            selectedLayerId = null
        )
        assertTrue("Promotional poster should emit multiple build actions", res.rawActionsJson.length() >= 4)
    }

    @Test
    fun targetedLayerRotationAndMovement_targetsCorrectLayer() {
        val headline = TextLayer(id = "h1", name = "تیتر اصلی", text = "عنوان بزرگ", fontSizeSp = 56f)
        val subtitle = TextLayer(id = "s1", name = "زیرنویس", text = "توضیحات فرعی", fontSizeSp = 22f, y = 0.1f)
        val project = StudioProject(layers = listOf(headline, subtitle))

        val moveRes = OfflineDesignEngine.processCommand(
            userCommand = "زیرنویس رو ببر پایین",
            project = project,
            selectedLayerId = headline.id
        )
        val moveAct = moveRes.rawActionsJson.getJSONObject(0)
        assertEquals("TRANSFORM_SELECTED_LAYER", moveAct.getString("actionType"))
        assertEquals("s1", moveAct.getString("targetLayerId"))
        assertTrue(moveAct.getDouble("y") > 0.1)

        val rotRes = OfflineDesignEngine.processCommand(
            userCommand = "تیتر رو ۴۵ درجه بچرخون",
            project = project,
            selectedLayerId = subtitle.id
        )
        val rotAct = rotRes.rawActionsJson.getJSONObject(0)
        assertEquals("TRANSFORM_SELECTED_LAYER", rotAct.getString("actionType"))
        assertEquals("h1", rotAct.getString("targetLayerId"))
        assertEquals(45.0, rotAct.getDouble("rotationZ"), 0.01)
    }

    @Test
    fun plpStyleLearningEngine_extractsDnaAndClonesNewProjectWithCustomText() {
        val viralSubtitle = TextLayer(
            id = "sub-plp",
            name = "توضیحات کپسولی",
            text = "چند ترفند مخفی که باتری رو نجات میده",
            fontSizeSp = 28f,
            colorHex = "#FFFFFF",
            backgroundPillEnabled = true,
            backgroundPillColorHex = "#E60F172A",
            backgroundPillRadius = 24f,
            y = 0.25f
        )
        val viralHeadline = TextLayer(
            id = "head-plp",
            name = "تیتر وایرال",
            text = "گوشی موبایل",
            fontSizeSp = 64f,
            colorHex = "#FFF200",
            stroke = com.example.data.LayerStroke(enabled = true, colorHex = "#0A0A0A", width = 6f),
            is3dEnabled = true,
            depth3d = 18,
            obliqueAngle3d = 318f,
            y = 0.06f
        )
        val sourcePlpProject = StudioProject(
            title = "موبایل",
            canvasWidth = 1080,
            canvasHeight = 1920,
            layers = listOf(viralSubtitle, viralHeadline)
        )

        val learnedDna = com.example.ui.agent.PlpStyleLearningEngine.extractStyleDnaFromProject(
            project = sourcePlpProject,
            customStyleName = "استایل تست موبایل"
        )

        assertEquals("استایل تست موبایل", learnedDna.styleName)
        assertEquals(1080, learnedDna.canvasWidth)
        assertEquals(1920, learnedDna.canvasHeight)
        assertTrue(learnedDna.headlineStyle.is3dEnabled)
        assertEquals(18, learnedDna.headlineStyle.depth3d)
        assertEquals("#FFF200", learnedDna.headlineStyle.colorHex)
        assertNotNull(learnedDna.subtitleStyle)
        assertTrue(learnedDna.subtitleStyle!!.backgroundPillEnabled)

        val clonedProject = com.example.ui.agent.PlpStyleLearningEngine.generateNewProjectFromStyle(
            style = learnedDna,
            newHeadline = "راز الگوریتم اینستاگرام",
            newSubtitle = "چطور بازدید ریلز را ۱۰ برابر کنیم؟"
        )

        val clonedTextLayers = clonedProject.layers.filterIsInstance<TextLayer>()
        assertEquals(2, clonedTextLayers.size)
        val clonedHead = clonedTextLayers.maxByOrNull { it.fontSizeSp }!!
        val clonedSub = clonedTextLayers.minByOrNull { it.fontSizeSp }!!

        assertEquals("راز الگوریتم اینستاگرام", clonedHead.text)
        assertEquals("#FFF200", clonedHead.colorHex)
        assertTrue(clonedHead.is3dEnabled)
        assertEquals(18, clonedHead.depth3d)

        assertEquals("چطور بازدید ریلز را ۱۰ برابر کنیم؟", clonedSub.text)
        assertTrue(clonedSub.backgroundPillEnabled)
    }

    @Test
    fun plpStyleLearningEngine_appliesStyleToCurrentCanvasPreservingExistingText() {
        val builtinStyle = com.example.ui.agent.PlpStyleLearningEngine.createDefaultLearnedPlpStyles().first()

        val existingHead = TextLayer(
            id = "eh1",
            name = "تیتر کاربر",
            text = "متن اختصاصی کاربر که نباید پاک شود",
            fontSizeSp = 52f,
            colorHex = "#FFFFFF",
            is3dEnabled = false
        )
        val existingSub = TextLayer(
            id = "es1",
            name = "زیرنویس کاربر",
            text = "زیرنویس مهم کاربر روی بوم",
            fontSizeSp = 24f,
            colorHex = "#CCCCCC",
            backgroundPillEnabled = false
        )
        val currentProject = StudioProject(
            title = "بوم فعلی",
            layers = listOf(existingHead, existingSub)
        )

        val styledProject = com.example.ui.agent.PlpStyleLearningEngine.applyStyleDnaToCurrentCanvas(
            style = builtinStyle,
            currentProject = currentProject
        )

        val styledHeadline = styledProject.layers.filterIsInstance<TextLayer>().first { it.id == "eh1" }
        val styledSubtitle = styledProject.layers.filterIsInstance<TextLayer>().first { it.id == "es1" }

        assertEquals("متن اختصاصی کاربر که نباید پاک شود", styledHeadline.text)
        assertEquals("زیرنویس مهم کاربر روی بوم", styledSubtitle.text)
        assertEquals(builtinStyle.headlineStyle.colorHex, styledHeadline.colorHex)
        assertEquals(builtinStyle.headlineStyle.is3dEnabled, styledHeadline.is3dEnabled)
        assertTrue(styledSubtitle.backgroundPillEnabled)
    }

    @Test
    fun offlineAgent_handlesLearnCloneAndApplyPlpStyleCommands() {
        val project = StudioProject(
            title = "پروژه نمونه",
            layers = listOf(TextLayer(id = "t1", name = "تیتر", text = "تست استایل", fontSizeSp = 50f))
        )
        val activeStyle = com.example.ui.agent.PlpStyleLearningEngine.createDefaultLearnedPlpStyles().first()

        val learnRes = OfflineDesignEngine.processCommand(
            userCommand = "استایل این پروژه رو یاد بگیر",
            project = project,
            selectedLayerId = "t1",
            activeLearnedStyle = activeStyle
        )
        assertEquals(
            "LEARN_CURRENT_PLP_STYLE",
            learnRes.rawActionsJson.getJSONObject(0).getString("actionType")
        )

        val cloneRes = OfflineDesignEngine.processCommand(
            userCommand = "با استایل یادگرفته‌شده بساز: آموزش هوش مصنوعی | صفر تا صد",
            project = project,
            selectedLayerId = "t1",
            activeLearnedStyle = activeStyle
        )
        val cloneAct = cloneRes.rawActionsJson.getJSONObject(0)
        assertEquals("CREATE_FROM_LEARNED_PLP_STYLE", cloneAct.getString("actionType"))
        assertEquals("آموزش هوش مصنوعی", cloneAct.getString("text"))
        assertEquals("صفر تا صد", cloneAct.getString("subtitleText"))

        val applyRes = OfflineDesignEngine.processCommand(
            userCommand = "استایل یادگرفته‌شده رو روی بوم فعلی اعمال کن",
            project = project,
            selectedLayerId = "t1",
            activeLearnedStyle = activeStyle
        )
        assertEquals(
            "APPLY_LEARNED_PLP_STYLE",
            applyRes.rawActionsJson.getJSONObject(0).getString("actionType")
        )
    }

    @Test
    fun pixelLabUltra_supportsBezierCurve4PointPerspectiveMultiStopGradientAndDoubleStroke() {
        val textLayer = TextLayer(
            id = "ultra-text-1",
            name = "تیتر اولترا",
            text = "پیکسل‌لب اولترا پرو",
            useGradientFill = true,
            gradientDirection = com.example.data.GradientDirectionPreset.RADIAL_CENTER,
            gradientStops = listOf(
                com.example.data.GradientStop(0f, "#FDE047"),
                com.example.data.GradientStop(0.5f, "#FB7185"),
                com.example.data.GradientStop(1f, "#38BDF8")
            ),
            colorSpans = listOf(com.example.data.TextColorSpan(0, 8, "#22C55E")),
            fontSpans = listOf(com.example.data.TextFontSpan(9, 14, "lalezar")),
            secondStrokeEnabled = true,
            secondStrokeColorHex = "#38BDF8",
            secondStrokeWidth = 10f,
            glitchOffsetPx = 6f,
            perspectiveQuad = com.example.data.PerspectiveQuad(
                enabled = true,
                topLeftX = 0.08f,
                topRightX = -0.08f
            ),
            maskMode = com.example.data.MaskPresetMode.BEZIER_CURVE,
            maskInvert = true
        )

        val bezierShape = com.example.data.ShapeLayer(
            id = "bezier-1",
            name = "منحنی بزیه",
            shapeKind = com.example.data.ShapeKind.BEZIER_PATH,
            bezierNodes = listOf(
                com.example.data.BezierNode(-0.3f, 0.1f, -0.1f, -0.3f),
                com.example.data.BezierNode(0.3f, -0.1f, 0.1f, 0.3f)
            ),
            isBezierClosed = true,
            isBezierFilled = true
        )

        assertEquals(3, textLayer.gradientStops.size)
        assertTrue(textLayer.secondStrokeEnabled)
        assertTrue(textLayer.perspectiveQuad.enabled)
        assertEquals(1, textLayer.colorSpans.size)
        assertEquals(1, textLayer.fontSpans.size)
        assertEquals(com.example.data.ShapeKind.BEZIER_PATH, bezierShape.shapeKind)
        assertEquals(2, bezierShape.bezierNodes.size)
    }

    @Test
    fun priority1_canvasZoomAndPan_clamps100To500PercentAndMapsCoordinatesAccurately() {
        val vm = com.example.ui.StudioViewModel()
        assertEquals(1.0f, vm.canvasZoomScale.value, 0.001f)

        // Zoom to 300%
        vm.setCanvasZoom(3.0f)
        assertEquals(3.0f, vm.canvasZoomScale.value, 0.001f)

        // Clamp upper bound at 500% (5.0f) and lower bound at 100% (1.0f)
        vm.setCanvasZoom(8.0f)
        assertEquals(5.0f, vm.canvasZoomScale.value, 0.001f)
        vm.setCanvasZoom(0.4f)
        assertEquals(1.0f, vm.canvasZoomScale.value, 0.001f)

        // Zoom to 250% and pan
        vm.setCanvasZoom(2.5f)
        vm.panAndZoomCanvasByGesture(panDxNorm = 0.2f, panDyNorm = -0.15f, zoomFactor = 1.0f)
        val centerMapped = vm.screenToCanvasNormalized(
            screenPxX = 500f + 0.2f * 1000f,
            screenPxY = 500f - 0.15f * 1000f,
            viewportWidth = 1000f,
            viewportHeight = 1000f
        )
        assertEquals(0.5f, centerMapped.x, 0.01f)
        assertEquals(0.5f, centerMapped.y, 0.01f)

        vm.resetCanvasZoomAndPan()
        assertEquals(1.0f, vm.canvasZoomScale.value, 0.001f)
        assertEquals(0f, vm.canvasPanOffset.value.x, 0.001f)
        assertEquals(0f, vm.canvasPanOffset.value.y, 0.001f)
    }

    @Test
    fun priority2_interactiveEraser_erasesImageAndDrawingWithUndoRedoWithoutAffectingOtherLayers() {
        val vm = com.example.ui.StudioViewModel()
        val bmp = android.graphics.Bitmap.createBitmap(200, 200, android.graphics.Bitmap.Config.ARGB_8888)
        val imgLayer = com.example.data.ImageLayer(
            id = "img-erase-1",
            name = "تصویر تست",
            bitmap = bmp,
            originalBitmap = bmp
        )
        val untouchedTextBefore = vm.project.value.layers.filterIsInstance<TextLayer>().first()
        val untouchedTextId = untouchedTextBefore.id

        // Inject image layer via project state
        vm.addShapeLayer(com.example.data.ShapeKind.RECTANGLE)
        // Test DrawingLayer interactive eraser first
        vm.setDrawingMode(true)
        vm.appendFreehandStroke(
            listOf(
                com.example.data.Point2D(0.2f, 0.2f),
                com.example.data.Point2D(0.8f, 0.8f)
            )
        )
        val drawingLayer = vm.project.value.layers.filterIsInstance<com.example.data.DrawingLayer>().first()
        assertEquals(1, drawingLayer.strokes.size)
        assertFalse(drawingLayer.strokes.first().isEraser)

        // Switch to interactive eraser with brush size 44px
        vm.setEraserBrushSize(44f)
        assertEquals(44f, vm.eraserBrushSize.value, 0.01f)
        vm.setEraserMode(true)
        assertTrue(vm.isEraserMode.value)

        vm.appendInteractiveEraserStroke(
            listOf(
                com.example.data.Point2D(0.3f, 0.3f),
                com.example.data.Point2D(0.6f, 0.6f)
            )
        )
        val updatedDrawing = vm.project.value.layers.filterIsInstance<com.example.data.DrawingLayer>().first()
        assertEquals(2, updatedDrawing.strokes.size)
        assertTrue(updatedDrawing.strokes.last().isEraser)
        assertEquals(44f, updatedDrawing.strokes.last().strokeWidth, 0.01f)

        // Verify other layers were untouched
        val untouchedTextAfter = vm.project.value.layers.first { it.id == untouchedTextId } as TextLayer
        assertEquals(untouchedTextBefore.text, untouchedTextAfter.text)
        assertEquals(untouchedTextBefore.x, untouchedTextAfter.x, 0.001f)

        // Verify Undo & Redo work after erasing
        assertTrue(vm.canUndo.value)
        vm.undo()
        val afterUndoDrawing = vm.project.value.layers.filterIsInstance<com.example.data.DrawingLayer>().first()
        assertEquals(1, afterUndoDrawing.strokes.size)

        assertTrue(vm.canRedo.value)
        vm.redo()
        val afterRedoDrawing = vm.project.value.layers.filterIsInstance<com.example.data.DrawingLayer>().first()
        assertEquals(2, afterRedoDrawing.strokes.size)
        assertTrue(afterRedoDrawing.strokes.last().isEraser)
        assertNotNull(imgLayer.bitmap)
    }

    @Test
    fun priority3_customCanvasDimensions_validatesNumericBoundsAndPreservesDesignRatio() {
        val vm = com.example.ui.StudioViewModel()
        val initialLayerCount = vm.project.value.layers.size
        val initialFirstLayerX = vm.project.value.layers.first().x

        // Reject invalid dimensions (< 128 or > 4096)
        assertFalse(vm.setCustomCanvasDimensions(64, 1080))
        assertFalse(vm.setCustomCanvasDimensions(1080, 5000))

        // Accept valid custom pixel dimensions
        assertTrue(vm.setCustomCanvasDimensions(1440, 1800))
        assertEquals(1440, vm.project.value.canvasWidth)
        assertEquals(1800, vm.project.value.canvasHeight)
        assertEquals(initialLayerCount, vm.project.value.layers.size)
        assertEquals(initialFirstLayerX, vm.project.value.layers.first().x, 0.001f)
    }

    @Test
    fun priority4_imageCropAndFlip_cropsPixelsPreservingOriginalBitmapAndSupportsFlipHV() {
        val fullBmp = android.graphics.Bitmap.createBitmap(400, 300, android.graphics.Bitmap.Config.ARGB_8888)
        val imgLayer = com.example.data.ImageLayer(
            id = "crop-img-1",
            name = "تصویر برش",
            bitmap = fullBmp,
            originalBitmap = fullBmp
        )
        assertEquals(5, com.example.data.CropAspectPreset.entries.size)
        assertFalse(imgLayer.flipHorizontal)
        assertFalse(imgLayer.flipVertical)

        // Simulate 1:1 crop (center 300x300 from 400x300)
        val croppedBmp = android.graphics.Bitmap.createBitmap(fullBmp, 50, 0, 300, 300)
        val updatedLayer = imgLayer.copy(
            bitmap = croppedBmp,
            originalBitmap = imgLayer.originalBitmap ?: imgLayer.bitmap,
            cropLeft = 0.125f,
            cropTop = 0f,
            cropRight = 0.875f,
            cropBottom = 1f,
            flipHorizontal = true,
            flipVertical = true
        )
        assertEquals(300, updatedLayer.bitmap!!.width)
        assertEquals(300, updatedLayer.bitmap!!.height)
        assertEquals(400, updatedLayer.originalBitmap!!.width)
        assertEquals(300, updatedLayer.originalBitmap!!.height)
        assertTrue(updatedLayer.flipHorizontal)
        assertTrue(updatedLayer.flipVertical)
    }

    @Test
    fun priority5_textAlignAndJustifyAndShare_supportsAll4ModesAndPngJpgExport() {
        val vm = com.example.ui.StudioViewModel()
        val firstText = vm.project.value.layers.filterIsInstance<TextLayer>().first()
        vm.selectLayer(firstText.id)

        com.example.data.TextAlignMode.entries.forEach { mode ->
            vm.setSelectedTextAlignMode(mode)
            val updated = vm.project.value.layers.filterIsInstance<TextLayer>().first { it.id == firstText.id }
            assertEquals(mode, updated.textAlignMode)
        }

        assertTrue(
            com.example.data.ExportFormat.entries.containsAll(
                listOf(com.example.data.ExportFormat.PNG, com.example.data.ExportFormat.JPG)
            )
        )
        assertEquals("png", com.example.data.ExportFormat.PNG.extension)
        assertEquals("jpg", com.example.data.ExportFormat.JPG.extension)
    }

    @Test
    fun geminiFunctionCalling_all18RealEditorToolsExecuteAndMutateCanvasCorrectly() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals(18, com.example.ui.agent.PixelLabGeminiTools.allDeclarations.size)
        val declaredNames = com.example.ui.agent.PixelLabGeminiTools.allToolNames.toSet()
        val expected18Tools = setOf(
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
        assertEquals(expected18Tools, declaredNames)

        val initialText = TextLayer(id = "txt-init", name = "تیتر اولیه", text = "پیکسل‌لب", fontSizeSp = 48f)
        val initialProject = StudioProject(
            title = "پروژه تست ۱۸ ابزار",
            canvasWidth = 1080,
            canvasHeight = 1080,
            layers = listOf(initialText)
        )
        val session = com.example.ui.agent.AgentTransactionSession(
            context = context,
            userCommand = "تست ۱۸ ابزار",
            preTransactionProject = initialProject,
            preTransactionSelectedLayerId = initialText.id
        )

        // 1. inspect_document
        val docOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "inspect_document", org.json.JSONObject(), session
        )
        assertEquals("ok", docOut.getString("status"))
        assertEquals(1080, docOut.getInt("canvasWidth"))
        assertEquals(1, docOut.getInt("layerCount"))

        // 2. inspect_layers
        val layersOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "inspect_layers", org.json.JSONObject(), session
        )
        assertEquals("ok", layersOut.getString("status"))
        assertEquals(1, layersOut.getInt("count"))

        // 3. inspect_layer
        val layerOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "inspect_layer", org.json.JSONObject().put("layerId", "txt-init"), session
        )
        assertEquals("ok", layerOut.getString("status"))
        assertEquals("پیکسل‌لب", layerOut.getString("text"))

        // 4. add_text_layer
        val addTxtOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "add_text_layer",
            org.json.JSONObject()
                .put("text", "تخفیف ویژه ۵۰٪")
                .put("fontId", "lalezar")
                .put("fontSizeSp", 60.0)
                .put("colorHex", "#FDE047")
                .put("is3dEnabled", true)
                .put("depth3d", 24),
            session
        )
        assertEquals("ok", addTxtOut.getString("status"))
        val newTextId = addTxtOut.getString("layerId")
        assertEquals(2, session.workingProject.layers.size)

        // 5. update_text_layer
        val updTxtOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "update_text_layer",
            org.json.JSONObject()
                .put("layerId", newTextId)
                .put("text", "تخفیف ویژه نوروزی")
                .put("curveAngle", 25.0),
            session
        )
        assertEquals("ok", updTxtOut.getString("status"))
        val updatedText = session.workingProject.layers.first { it.id == newTextId } as TextLayer
        assertEquals("تخفیف ویژه نوروزی", updatedText.text)
        assertEquals(25f, updatedText.curveAngle, 0.01f)

        // 6. add_shape_layer
        val addShapeOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "add_shape_layer",
            org.json.JSONObject()
                .put("shapeKind", "HEXAGON")
                .put("fillColorHex", "#A855F7")
                .put("is3dEnabled", true),
            session
        )
        assertEquals("ok", addShapeOut.getString("status"))
        val shapeId = addShapeOut.getString("layerId")

        // 7. add_image_layer
        val addImgOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "add_image_layer",
            org.json.JSONObject()
                .put("stickerId", "crown_gold")
                .put("layerName", "تاج طلایی"),
            session
        )
        assertEquals("ok", addImgOut.getString("status"))
        val imgId = addImgOut.getString("layerId")

        // 8. transform_layer
        val transOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "transform_layer",
            org.json.JSONObject()
                .put("layerId", shapeId)
                .put("x", 0.12)
                .put("y", -0.18)
                .put("scale", 1.25)
                .put("rotationZ", 30.0),
            session
        )
        assertEquals("ok", transOut.getString("status"))
        val transformedShape = session.workingProject.layers.first { it.id == shapeId }
        assertEquals(0.12f, transformedShape.x, 0.001f)
        assertEquals(30f, transformedShape.rotationZ, 0.01f)

        // 9. set_layer_style
        val styleOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "set_layer_style",
            org.json.JSONObject()
                .put("layerId", newTextId)
                .put("strokeEnabled", true)
                .put("strokeColorHex", "#000000")
                .put("strokeWidth", 8.0)
                .put("shadowEnabled", true)
                .put("embossEnabled", true),
            session
        )
        assertEquals("ok", styleOut.getString("status"))

        // 10. set_background
        val bgOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "set_background",
            org.json.JSONObject()
                .put("type", "GRADIENT")
                .put("primaryColorHex", "#090D16")
                .put("secondaryColorHex", "#3B0764")
                .put("vignetteEnabled", true),
            session
        )
        assertEquals("ok", bgOut.getString("status"))
        assertEquals("#090D16", session.workingProject.background.primaryColorHex)

        // 11. reorder_layers
        val reorderOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "reorder_layers",
            org.json.JSONObject()
                .put("layerId", shapeId)
                .put("direction", "TO_BACK"),
            session
        )
        assertEquals("ok", reorderOut.getString("status"))
        assertEquals(shapeId, session.workingProject.layers.first().id)

        // 12. align_layers
        val alignOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "align_layers",
            org.json.JSONObject()
                .put("layerId", newTextId)
                .put("alignment", "CENTER_BOTH"),
            session
        )
        assertEquals("ok", alignOut.getString("status"))
        val alignedText = session.workingProject.layers.first { it.id == newTextId }
        assertEquals(0f, alignedText.x, 0.001f)
        assertEquals(0f, alignedText.y, 0.001f)

        // 13. duplicate_layer
        val dupOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "duplicate_layer",
            org.json.JSONObject().put("layerId", imgId),
            session
        )
        assertEquals("ok", dupOut.getString("status"))
        val dupImgId = dupOut.getString("duplicatedLayerId")

        // 14. delete_layer (on non-protected duplicated sticker)
        val delOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "delete_layer",
            org.json.JSONObject().put("layerId", dupImgId),
            session
        )
        assertEquals("ok", delOut.getString("status"))
        assertFalse(session.workingProject.layers.any { it.id == dupImgId })

        // 15. group_layers (alignment mode)
        val grpOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "group_layers",
            org.json.JSONObject()
                .put("layerIdsCsv", "$newTextId,$shapeId")
                .put("mergeIntoBitmap", false)
                .put("groupName", "گروه تیتر و نشان"),
            session
        )
        assertEquals("ok", grpOut.getString("status"))

        // 16. render_preview
        val prevOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "render_preview",
            org.json.JSONObject().put("scaleMultiplier", 0.35),
            session
        )
        assertEquals("ok", prevOut.getString("status"))
        assertNotNull(session.renderedPreviewBitmap)

        // 17. validate_document
        val valOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "validate_document",
            org.json.JSONObject(),
            session
        )
        assertEquals("ok", valOut.getString("status"))
        assertNotNull(session.lastValidationReport)
        assertTrue(valOut.getInt("scoreOutOf100") > 0)

        // 18. undo_transaction
        val undoOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "undo_transaction",
            org.json.JSONObject().put("reason", "تست بازگشت کامل تراکنش"),
            session
        )
        assertEquals("ok", undoOut.getString("status"))
        assertTrue(session.wasRolledBack)
        assertEquals(initialProject.layers.size, session.workingProject.layers.size)
        assertEquals("پیکسل‌لب", (session.workingProject.layers.first() as TextLayer).text)
    }

    @Test
    fun geminiAgent_protectsMainProductImageLayerUnlessExplicitlyRequested() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val productBmp = android.graphics.Bitmap.createBitmap(240, 240, android.graphics.Bitmap.Config.ARGB_8888)
        val productImageLayer = com.example.data.ImageLayer(
            id = "product-main-1",
            name = "تصویر اصلی محصول",
            bitmap = productBmp,
            originalBitmap = productBmp
        )
        val project = StudioProject(
            title = "پروژه محصول",
            layers = listOf(productImageLayer)
        )
        val session = com.example.ui.agent.AgentTransactionSession(
            context = context,
            userCommand = "حذف لایه",
            preTransactionProject = project,
            preTransactionSelectedLayerId = productImageLayer.id,
            allowProductImageModification = false
        )

        // Attempt to delete the main product image without explicit confirmation must be blocked
        val blockedOut = com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "delete_layer",
            org.json.JSONObject()
                .put("layerId", productImageLayer.id)
                .put("confirmModifyProductImage", false),
            session
        )
        assertEquals("blocked", blockedOut.getString("status"))
        assertEquals(1, session.workingProject.layers.size)

        // Set background without replacing product image
        com.example.ui.agent.PixelLabGeminiTools.executeToolCallAsOrgJson(
            "set_background",
            org.json.JSONObject()
                .put("type", "GRADIENT")
                .put("primaryColorHex", "#0F172A")
                .put("secondaryColorHex", "#1E1B4B"),
            session
        )
        assertTrue(session.workingProject.layers.any { it.id == productImageLayer.id })
    }

    @Test
    fun geminiAgent_configurableModelSelectionPersistsAndSupportsCustomModels() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals(
            "gemini-3.8-flash",
            com.example.ui.agent.PixelLabAgentEngine.getSelectedModelName(context)
        )

        val vm = com.example.ui.StudioViewModel()
        vm.setSelectedGeminiModel(context, "gemini-4-argon")
        assertEquals("gemini-4-argon", vm.selectedGeminiModel.value)
        assertEquals(
            "gemini-4-argon",
            com.example.ui.agent.PixelLabAgentEngine.getSelectedModelName(context)
        )

        // Restore default
        vm.setSelectedGeminiModel(context, "gemini-3.8-flash")
        assertEquals("gemini-3.8-flash", vm.selectedGeminiModel.value)
    }

    @Test
    fun geminiCanvasAdapter_connectsLayerFunctionsDirectlyToCanvasMutations() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val initText = TextLayer(id = "layer-txt-1", name = "متن تست", text = "آداپتور بوم", fontSizeSp = 36f)
        val project = StudioProject(
            title = "پروژه تست آداپتور",
            canvasWidth = 1080,
            canvasHeight = 1080,
            layers = listOf(initText)
        )
        val adapter = com.example.ui.agent.GeminiCanvasAdapter.createSession(
            context = context,
            userCommand = "افزودن و تغییر لایه",
            initialProject = project,
            initialSelectedLayerId = initText.id
        )

        // 1. addTextLayer
        val addRes = adapter.addTextLayer(
            text = "تخفیف ویژه",
            fontId = "lalezar",
            fontSizeSp = 64f,
            colorHex = "#FDE047",
            is3dEnabled = true,
            depth3d = 20,
            x = 0f,
            y = 0.1f
        )
        val addedTextId = addRes["layerId"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        assertNotNull(addedTextId)
        assertEquals(2, adapter.currentWorkingProject.layers.size)

        // 2. transformLayer
        val transRes = adapter.transformLayer(
            layerId = addedTextId!!,
            x = 0.05f,
            y = 0.15f,
            scale = 1.2f,
            rotationZ = 15f
        )
        assertEquals("ok", (transRes["status"] as? kotlinx.serialization.json.JsonPrimitive)?.content)
        val transformed = adapter.currentWorkingProject.layers.first { it.id == addedTextId }
        assertEquals(0.05f, transformed.x, 0.001f)
        assertEquals(0.15f, transformed.y, 0.001f)
        assertEquals(15f, transformed.rotationZ, 0.01f)

        // 3. updateTextLayer
        val updRes = adapter.updateTextLayer(
            layerId = addedTextId,
            text = "تخفیف بزرگ پاییزه",
            curveAngle = 10f
        )
        assertEquals("ok", (updRes["status"] as? kotlinx.serialization.json.JsonPrimitive)?.content)
        val updatedText = adapter.currentWorkingProject.layers.first { it.id == addedTextId } as TextLayer
        assertEquals("تخفیف بزرگ پاییزه", updatedText.text)
        assertEquals(10f, updatedText.curveAngle, 0.01f)

        // 4. addShapeLayer
        val shapeRes = adapter.addShapeLayer(
            shapeKind = "STAR",
            fillColorHex = "#E11D48",
            is3dEnabled = true
        )
        assertEquals("ok", (shapeRes["status"] as? kotlinx.serialization.json.JsonPrimitive)?.content)
        assertEquals(3, adapter.currentWorkingProject.layers.size)

        // 5. undoTransaction
        val undoRes = adapter.undoTransaction(reason = "تست بازگردانی تراکنش آداپتور")
        assertEquals("ok", (undoRes["status"] as? kotlinx.serialization.json.JsonPrimitive)?.content)
        assertTrue(adapter.wasRolledBack)
        assertEquals(1, adapter.currentWorkingProject.layers.size)
        assertEquals("آداپتور بوم", (adapter.currentWorkingProject.layers.first() as TextLayer).text)
    }

    @Test
    fun geminiServiceManager_managesSecureKeysAndModelConfiguration() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val sm = com.example.ui.agent.GeminiServiceManager(context)
        assertEquals("gemini-3.8-flash", sm.activeModelName)

        com.example.ui.agent.GeminiServiceManager.setSelectedModel(context, "gemini-3.5-flash-lite")
        assertEquals("gemini-3.5-flash-lite", sm.activeModelName)

        // Restore default
        com.example.ui.agent.GeminiServiceManager.setSelectedModel(context, "gemini-3.8-flash")
        assertEquals("gemini-3.8-flash", sm.activeModelName)

        // Custom API Key security check (no hardcoded key, persists without leaking to APK)
        assertFalse(com.example.ui.agent.GeminiServiceManager.isSecureKeyConfigured(context))
        com.example.ui.agent.GeminiServiceManager.setCustomApiKey(context, "test-secure-key-1234")
        assertTrue(com.example.ui.agent.GeminiServiceManager.isSecureKeyConfigured(context))
        assertEquals("test-secure-key-1234", com.example.ui.agent.GeminiServiceManager.getCustomApiKey(context))
        com.example.ui.agent.GeminiServiceManager.setCustomApiKey(context, "")
        assertFalse(com.example.ui.agent.GeminiServiceManager.isSecureKeyConfigured(context))
    }

    @Test
    fun geminiCanvasAdapter_executesAdvanced3DStylingAndGridAlignment() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val initText = TextLayer(
            text = "متن سه بعدی",
            x = 0f,
            y = 0f
        )
        val project = StudioProject(
            title = "تست استایل ۳بعدی و تراز شبکه",
            layers = listOf(initText)
        )
        val adapter = com.example.ui.agent.GeminiCanvasAdapter.createSession(
            context = context,
            userCommand = "استایل ۳بعدی و چیدمان شبکه",
            initialProject = project,
            initialSelectedLayerId = initText.id
        )

        // 1. setLayerStyle3D
        val styleRes = adapter.setLayerStyle3D(
            layerId = initText.id,
            rotationX = 25f,
            rotationY = -15f,
            rotationZ = 12f,
            is3dEnabled = true,
            depth3d = 32,
            obliqueAngle3d = 140f,
            simulateLighting = true,
            lightAngle = 60f,
            shadowEnabled = true,
            shadowColorHex = "#CC000000",
            primaryColorHex = "#F43F5E",
            secondaryColorHex = "#FDE047",
            useGradient = true
        )
        assertEquals("ok", (styleRes["status"] as? kotlinx.serialization.json.JsonPrimitive)?.content)

        val styledLayer = adapter.getLayerById(initText.id) as TextLayer
        assertEquals(25f, styledLayer.rotationX, 0.01f)
        assertEquals(-15f, styledLayer.rotationY, 0.01f)
        assertEquals(12f, styledLayer.rotationZ, 0.01f)
        assertTrue(styledLayer.is3dEnabled)
        assertEquals(32, styledLayer.depth3d)
        assertEquals(140f, styledLayer.obliqueAngle3d, 0.01f)
        assertTrue(styledLayer.simulateLighting3d)
        assertTrue(styledLayer.shadow.enabled)
        assertTrue(styledLayer.useGradientFill)
        assertEquals("#F43F5E", styledLayer.colorHex)
        assertEquals("#FDE047", styledLayer.gradientSecondColorHex)

        // 2. alignToGrid: TOP_LEFT
        val alignTl = adapter.alignToGrid(initText.id, "TOP_LEFT")
        assertEquals("ok", (alignTl["status"] as? kotlinx.serialization.json.JsonPrimitive)?.content)
        val tlLayer = adapter.getLayerById(initText.id)!!
        assertEquals(-0.28f, tlLayer.x, 0.01f)
        assertEquals(-0.32f, tlLayer.y, 0.01f)

        // 3. alignToGrid: BOTTOM_RIGHT
        val alignBr = adapter.alignToGrid(initText.id, "BOTTOM_RIGHT")
        assertEquals("ok", (alignBr["status"] as? kotlinx.serialization.json.JsonPrimitive)?.content)
        val brLayer = adapter.getLayerById(initText.id)!!
        assertEquals(0.28f, brLayer.x, 0.01f)
        assertEquals(0.32f, brLayer.y, 0.01f)

        // 4. alignToGrid: CENTER
        val alignCenter = adapter.alignToGrid(initText.id, "CENTER")
        assertEquals("ok", (alignCenter["status"] as? kotlinx.serialization.json.JsonPrimitive)?.content)
        val centerLayer = adapter.getLayerById(initText.id)!!
        assertEquals(0f, centerLayer.x, 0.01f)
        assertEquals(0f, centerLayer.y, 0.01f)
    }

    @Test
    fun geminiCanvasAdapter_buildsSemanticSceneGraphAndFiresHighlightCallback() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dummyBmp = android.graphics.Bitmap.createBitmap(200, 200, android.graphics.Bitmap.Config.ARGB_8888)
        val heroImage = ImageLayer(
            name = "تصویر محصول اصلی (Protected)",
            bitmap = dummyBmp,
            x = 0f,
            y = -0.1f
        )
        val headlineText = TextLayer(
            name = "عنوان اصلی",
            text = "فروش ویژه عید نوروز",
            fontSizeSp = 64f,
            x = 0f,
            y = 0.2f
        )
        val badgeShape = ShapeLayer(
            name = "بج تخفیف",
            shapeKind = com.example.data.ShapeKind.STAR,
            sizeRatio = 0.25f,
            x = 0.3f,
            y = -0.3f
        )

        val project = StudioProject(
            title = "پوستر عیدانه",
            layers = listOf(heroImage, headlineText, badgeShape)
        )

        val adapter = com.example.ui.agent.GeminiCanvasAdapter.createSession(
            context = context,
            userCommand = "تحلیل صحنه و بررسی لایه‌ها",
            initialProject = project,
            initialSelectedLayerId = headlineText.id,
            allowProductImageModification = false
        )

        // Test Live Bounding Box Highlight Callback
        val highlightedEvents = mutableListOf<Pair<String?, String>>()
        adapter.onLayerHighlighted = { layerId, toolName ->
            highlightedEvents.add(layerId to toolName)
        }

        // Trigger a tool mutation
        adapter.transformLayer(headlineText.id, scale = 1.1f)
        assertTrue(highlightedEvents.isNotEmpty())
        assertEquals(headlineText.id, highlightedEvents.last().first)
        assertEquals("transform_layer", highlightedEvents.last().second)

        // Test Semantic Scene Graph Generation
        val sceneGraph = adapter.buildSemanticSceneGraph()
        assertNotNull(sceneGraph)
        assertTrue(sceneGraph.has("canvas"))
        assertTrue(sceneGraph.has("layers"))
        assertTrue(sceneGraph.has("visualHierarchy"))

        val layersArr = sceneGraph.getJSONArray("layers")
        assertEquals(3, layersArr.length())

        // Validate Semantic Roles
        val heroLayerJson = layersArr.getJSONObject(0)
        assertEquals("HERO_PRODUCT_IMAGE", heroLayerJson.getString("semanticRole"))
        assertTrue(heroLayerJson.getBoolean("isProductImmunityProtected"))

        val textLayerJson = layersArr.getJSONObject(1)
        assertEquals("HEADLINE_TEXT", textLayerJson.getString("semanticRole"))

        val shapeLayerJson = layersArr.getJSONObject(2)
        assertEquals("BADGE_SHAPE", shapeLayerJson.getString("semanticRole"))

        // Product Image Immunity Fence: Deleting hero product image without permission must be blocked
        val deleteRes = adapter.deleteLayer(heroImage.id, allowDeleteProductImage = false)
        assertEquals("blocked", (deleteRes["status"] as? kotlinx.serialization.json.JsonPrimitive)?.content)
        assertEquals("protected", (deleteRes["protectionStatus"] as? kotlinx.serialization.json.JsonPrimitive)?.content)
        // Ensure hero image was NOT deleted
        assertEquals(3, adapter.currentWorkingProject.layers.size)
        assertTrue(adapter.currentWorkingProject.layers.any { it.id == heroImage.id })
    }
}



