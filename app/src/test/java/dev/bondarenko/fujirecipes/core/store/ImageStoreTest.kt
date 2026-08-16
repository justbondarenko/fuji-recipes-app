package dev.bondarenko.fujirecipes.core.store

import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `saveStream writes file and getFile returns it`() = runTest {
        val imageStore = ImageStore(tempFolder.root)
        val content = "fake image data".toByteArray()

        val file = imageStore.saveStream("test.webp", ByteArrayInputStream(content))
        assertEquals("test.webp", file.name)

        val retrieved = imageStore.getFile("test.webp")
        assertTrue(retrieved.exists())
        assertEquals("fake image data", retrieved.readText())
    }

    @Test
    fun `delete removes specific image file`() = runTest {
        val imageStore = ImageStore(tempFolder.root)
        imageStore.saveStream("sample.webp", ByteArrayInputStream("sample".toByteArray()))

        assertTrue(imageStore.getFile("sample.webp").exists())
        val deleted = imageStore.delete("sample.webp")
        assertTrue(deleted)
        assertFalse(imageStore.getFile("sample.webp").exists())
    }

    @Test
    fun `cleanOrphans deletes unreferenced images`() = runTest {
        val imageStore = ImageStore(tempFolder.root)
        imageStore.saveStream("used1.webp", ByteArrayInputStream("used1".toByteArray()))
        imageStore.saveStream("used2.webp", ByteArrayInputStream("used2".toByteArray()))
        imageStore.saveStream("orphan.webp", ByteArrayInputStream("orphan".toByteArray()))

        imageStore.cleanOrphans(setOf("used1.webp", "used2.webp"))

        assertTrue(imageStore.getFile("used1.webp").exists())
        assertTrue(imageStore.getFile("used2.webp").exists())
        assertFalse(imageStore.getFile("orphan.webp").exists())
    }
}
