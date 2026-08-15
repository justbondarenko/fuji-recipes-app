package dev.bondarenko.fujirecipes.data.exporting

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The archive half of an export — FEAT-008 T-07.
 *
 * `java.util.zip` from the standard library, which `steering/tech-stack.md` §2 already names
 * for exactly this: "Import/export only". A compression dependency for one call would be a
 * dependency to keep current for ever.
 *
 * Kept separate from [archiveEntries] so the naming and de-duplication rules can be tested
 * without a compressor in the way, which is how the reference splits it too.
 *
 * Whole-archive-in-memory. A library is bounded at roughly 2,000 recipes of a few hundred
 * bytes each, so the worst case is a couple of megabytes — and the file is going to a
 * `content://` URI that has to be a real file anyway.
 */
fun zipOf(entries: Map<String, String>): ByteArray {
    val out = ByteArrayOutputStream()

    ZipOutputStream(out).use { zip ->
        entries.forEach { (name, content) ->
            zip.putNextEntry(ZipEntry(name))
            // Explicitly UTF-8. `String.toByteArray()` defaults to it on Android, but SF-001
            // makes the encoding part of the format rather than a platform default to inherit.
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    return out.toByteArray()
}
