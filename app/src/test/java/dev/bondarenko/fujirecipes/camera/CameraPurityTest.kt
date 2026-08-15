package dev.bondarenko.fujirecipes.camera

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * `coding-standards.md` P4, enforced structurally rather than by review.
 *
 * "Nothing under `camera/plan/` may import `android.*`" — extended to `camera/ptp/` for the
 * same reason: the byte layer and the decision layer are what make the protocol testable with
 * no hardware attached, and one `android.*` import is all it takes to lose that. It would not
 * fail loudly; it would just mean the highest-value suite in the project can only run on a
 * device, and then it stops being run.
 *
 * A grep would find this too. A test finds it in CI, on the commit that caused it.
 */
class CameraPurityTest {

    private val pureDirectories = listOf(
        "src/main/java/dev/bondarenko/fujirecipes/camera/ptp",
        "src/main/java/dev/bondarenko/fujirecipes/camera/plan",
    )

    @Test
    fun `the pure camera layers import nothing from android`() {
        val checked = mutableListOf<File>()

        pureDirectories.forEach { path ->
            val directory = File(path)
            // `plan/` arrives with FEAT-006. A directory that does not exist yet is not a
            // failure; a directory that exists and is empty would be, and is reported below.
            if (!directory.isDirectory) return@forEach

            directory.walkTopDown().filter { it.extension == "kt" }.forEach { source ->
                checked += source
                source.readLines().forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("import android.") ||
                        trimmed.startsWith("import androidx.")
                    ) {
                        fail("${source.path}:${index + 1} imports $trimmed — P4 forbids it")
                    }
                }
            }
        }

        assertTrue(
            checked.isNotEmpty(),
            "no source files were checked; the test is looking in the wrong place " +
                "(working directory is ${File(".").absolutePath})",
        )
    }
}
