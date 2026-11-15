package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BackgroundType
import com.example.data.ImageLayer
import com.example.data.PlpProjectManager
import com.example.data.StudioProject
import com.example.data.TextLayer
import com.example.ui.StudioViewModel
import com.example.ui.agent.OfflineDesignEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PixelLab AI", appName)
    }

    @Test
    fun `import poster_instagram plp reconstructs gradient background without thumb bug and parses 19 layers`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val localFiles = PlpProjectManager.listLocalPlpFiles(context)
        assertTrue("Preset PLP file should be seeded from assets/presets/poster_instagram.plp", localFiles.isNotEmpty())

        val loadResult = PlpProjectManager.loadPlpFromFile(localFiles.first(), context)
        assertTrue("Loading PLP file should succeed: ${loadResult.exceptionOrNull()}", loadResult.isSuccess)
        val project = loadResult.getOrThrow()

        // 1. Background MUST be GRADIENT (#F2C100 -> #332200) and NEVER the flattened 'thumb' bitmap!
        assertEquals(BackgroundType.GRADIENT, project.background.type)
        assertNull("thumb entry must never be assigned as backgroundBitmap", project.background.backgroundBitmap)
        assertTrue(project.background.isRadialGradient)

        // 2. All 19 layers from data.plab must be reconstructed and unlocked for immediate editing
        assertEquals(19, project.layers.size)
        assertTrue("All layers should be auto-unlocked when 100% of template layers were locked", project.layers.none { it.isLocked })

        // 3. Verify multi-color span parsing on "اینستـــا و تلگرامتو"
        val instaTextLayer = project.layers.filterIsInstance<TextLayer>().find {
            it.text.replace("ـ", "").contains("اینستا")
        }
        assertNotNull("Should find text layer containing 'اینستا'", instaTextLayer)
        assertTrue("Should extract partial color span for 'اینستا'", instaTextLayer!!.colorSpans.isNotEmpty())
        assertEquals("#F2C100", instaTextLayer.colorSpans.first().colorHex)

        // 4. Verify Chroma Key & Color Overlay on shape/image layers
        val imageLayers = project.layers.filterIsInstance<ImageLayer>()
        assertTrue("Should contain image layers with Chroma Key enabled", imageLayers.any { it.eraseColorEnabled })
        assertTrue("Should contain image layers with Color Overlay enabled", imageLayers.any { it.colorOverlayEnabled })
    }

    @Test
    fun `offline design engine and viewModel handle layer management and canvas cleanup without stuck image`() {
        val initialText = TextLayer(
            id = "t1",
            name = "تیتر آزمایشی",
            text = "تست هوش مصنوعی",
            is3dEnabled = false
        )
        val project = StudioProject(layers = listOf(initialText))

        // Unrecognized/general prompt must NOT blindly force is3dEnabled = true on initialText
        val result1 = OfflineDesignEngine.processCommand(
            userCommand = "یک متن جدید اضافه کن: سلام دنیا",
            project = project,
            selectedLayerId = "t1"
        )
        val actionsStr1 = result1.rawActionsJson.toString()
        assertTrue("Should generate ADD_TEXT_LAYER action", actionsStr1.contains("ADD_TEXT_LAYER"))
        assertFalse("Should not force is3dEnabled = true on existing layer", actionsStr1.contains("UPDATE_SELECTED_TEXT"))

        // Clear all layers / reset canvas command
        val resultClear = OfflineDesignEngine.processCommand(
            userCommand = "حذف همه لایه‌ها و پاکسازی بوم",
            project = project,
            selectedLayerId = "t1"
        )
        assertTrue("Should generate RESET_CANVAS action", resultClear.rawActionsJson.toString().contains("RESET_CANVAS"))
    }
}

