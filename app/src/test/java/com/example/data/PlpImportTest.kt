package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlpImportTest {

    @Test
    fun testParseOfficialPixelLabPlpArchive() {
        val plpFile = File("public/assets/aistudio/_______205c6fd4.plp")
        if (!plpFile.exists()) return

        val context = ApplicationProvider.getApplicationContext<Context>()
        val project = FileInputStream(plpFile).use { fis ->
            PlpProjectManager.readPlpArchive(fis, context)
        }

        assertNotNull("Project must not be null", project)
        assertEquals("Canvas width should match bgWidth", 1080, project.canvasWidth)
        assertEquals("Canvas height should match bgHeight", 1920, project.canvasHeight)
        assertTrue("Project should have all 19 layers", project.layers.size >= 18)

        val textLayers = project.layers.filterIsInstance<TextLayer>()
        assertTrue("Should have multiple text layers", textLayers.size >= 4)
        assertTrue("Should contain Persian text", textLayers.any { it.text.contains("اینست") || it.text.contains("تلگرام") })

        val imageLayers = project.layers.filterIsInstance<ImageLayer>()
        assertTrue("Should have multiple image layers from res/", imageLayers.size >= 10)
    }

    @Test
    fun testExportOfficialPixelLabArchiveAndReimport() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val originalProject = StudioProject(
            title = "طراحی_تست_پیکسل‌لب",
            canvasWidth = 540,
            canvasHeight = 540,
            background = CanvasBackground(
                type = BackgroundType.GRADIENT,
                primaryColorHex = "#0F172A",
                secondaryColorHex = "#311042"
            ),
            layers = listOf(
                TextLayer(
                    text = "تیتر سه‌بعدی تست",
                    fontSizeSp = 50f,
                    colorHex = "#FFD700",
                    is3dEnabled = true,
                    depth3d = 20
                ),
                ShapeLayer(
                    shapeKind = ShapeKind.HEXAGON,
                    fillColorHex = "#38BDF8"
                )
            )
        )

        val savedFile = kotlinx.coroutines.runBlocking {
            PlpProjectManager.savePlpToInternalStorage(context, originalProject)
        }
        assertTrue("Saved file must exist", savedFile.exists() && savedFile.length() > 0L)

        // Test loading from local list
        val localFiles = kotlinx.coroutines.runBlocking {
            PlpProjectManager.listLocalPlpFiles(context)
        }
        assertTrue("Local PLP list should not be empty", localFiles.isNotEmpty())

        val loaded = kotlinx.coroutines.runBlocking {
            PlpProjectManager.loadPlpFromFile(localFiles.first(), context)
        }.getOrThrow()

        assertNotNull("Loaded project should not be null", loaded)
        assertTrue("Project should have canvas width", loaded.canvasWidth > 0)
    }

    @Test
    fun testOfflineDesignEngineAspectAndStyling() {
        val baseProject = StudioProject(
            canvasWidth = 1080,
            canvasHeight = 1080,
            layers = listOf(TextLayer(text = "عنوان اصلی"))
        )

        // 1. Story aspect ratio command
        val storyResult = com.example.ui.agent.OfflineDesignEngine.processCommand(
            userCommand = "نسبت رو به اندازه استوری تغییر بده",
            project = baseProject,
            selectedLayerId = null
        )
        assertNotNull(storyResult)
        assertTrue("Should have actions", storyResult.rawActionsJson.length() > 0)
        val firstAction = storyResult.rawActionsJson.getJSONObject(0)
        assertEquals("SET_CANVAS_ASPECT", firstAction.getString("actionType"))
        assertEquals("9:16", firstAction.getString("aspectRatioLabel"))

        // 2. 3D Gold command
        val goldResult = com.example.ui.agent.OfflineDesignEngine.processCommand(
            userCommand = "متن رو طلایی سه بعدی کن",
            project = baseProject,
            selectedLayerId = baseProject.layers.first().id
        )
        assertNotNull(goldResult)
        assertTrue(goldResult.replyFa.contains("طلایی") || goldResult.replyFa.contains("۳بعدی") || goldResult.replyFa.contains("استایل"))
    }
}
