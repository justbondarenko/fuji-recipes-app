package dev.bondarenko.fujirecipes.core.store

import java.io.ByteArrayInputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RawDevelopmentCacheTest {
    @Test
    fun `validates and atomically imports a RAF`() {
        val directory = createTempDirectory("raw-cache-test").toFile()
        val cache = RawDevelopmentCache(directory)
        val bytes = "FUJIFILMCCD-RAW payload".toByteArray()

        val file = cache.importRaf("DSCF 0001.RAF", ByteArrayInputStream(bytes), bytes.size.toLong())

        assertEquals("DSCF_0001.raf", file.name)
        assertContentEquals(bytes, file.readBytes())
        assertFalse(directory.resolve("DSCF_0001.raf.part").exists())
    }

    @Test
    fun `rejects a non-RAF without leaving a partial file`() {
        val directory = createTempDirectory("raw-cache-test").toFile()
        val cache = RawDevelopmentCache(directory)

        assertFailsWith<IllegalArgumentException> {
            cache.importRaf("wrong.bin", ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3)
        }

        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }
}
