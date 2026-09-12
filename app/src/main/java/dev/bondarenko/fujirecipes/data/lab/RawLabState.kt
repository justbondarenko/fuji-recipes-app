package dev.bondarenko.fujirecipes.data.lab

import dev.bondarenko.fujirecipes.camera.usb.RawDevelopmentResult
import dev.bondarenko.fujirecipes.camera.usb.RawDevelopmentStage
import dev.bondarenko.fujirecipes.camera.usb.RawRenderQuality
import dev.bondarenko.fujirecipes.data.fields.FieldContext
import dev.bondarenko.fujirecipes.data.fields.RecipeField
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.fields.SensorGeneration
import dev.bondarenko.fujirecipes.data.fields.defaultRecipeSettings
import dev.bondarenko.fujirecipes.data.model.Recipe
import java.io.File
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** How far a RAF has got into the app's own cache, before the camera is involved at all. */
data class RawImportProgress(val written: Long, val total: Long?)

/** The last picture the camera gave back, and what produced it. */
data class RawLabPreview(
    val file: File,
    /** The settings this was rendered from — what makes a later edit *stale* rather than new. */
    val settings: JsonObject,
    val width: Int? = null,
    val height: Int? = null,
    val appliedFields: List<String> = emptyList(),
    val preservedFields: List<String> = emptyList(),
    val quality: RawRenderQuality = RawRenderQuality.FULL,
    val fromThumbnail: Boolean = false,
) {
    /** Whether this is the file to offer as a finished JPEG. */
    val isFullResolution: Boolean
        get() = quality == RawRenderQuality.FULL && !fromThumbnail
}

/** A body with no verified `0xD185` adapter, and the block it returned. */
data class RawLabCalibration(
    val cameraModel: String,
    val profile: ByteArray,
    val message: String,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (
            other is RawLabCalibration &&
                cameraModel == other.cameraModel &&
                message == other.message &&
                profile.contentEquals(other.profile)
            )

    override fun hashCode(): Int =
        (cameraModel.hashCode() * 31 + message.hashCode()) * 31 + profile.contentHashCode()
}

/**
 * Everything the lab is holding: a RAF, a set of settings, and the last render of the two.
 *
 * Pure and free of `android.*`, because the decisions worth getting right here — when a
 * preview has gone stale, whether an automatic render should fire, what "unchanged" means
 * after a recipe is applied — are decisions, not drawing. `RawLabWorkspace` holds one of
 * these above the nav graph; the screen renders it.
 *
 * Settings are a raw [JsonObject] for the same reason the editor keeps one: a typed class is a
 * list of the fields this build knows about, and a newer web client's key must survive being
 * loaded, edited around, and saved back (`coding-standards.md` P2).
 */
data class RawLabState(
    val raf: File? = null,
    val rafName: String = "",
    val importing: RawImportProgress? = null,
    val settings: JsonObject = defaultRecipeSettings(),
    /** What the current settings are compared against to decide whether anything changed. */
    val baseline: JsonObject = defaultRecipeSettings(),
    val appliedRecipeId: String? = null,
    val appliedRecipeName: String? = null,
    val preview: RawLabPreview? = null,
    val rendering: RawDevelopmentStage? = null,
    val autoPreview: Boolean = false,
    /** Consecutive render failures; two in a row switch [autoPreview] off. */
    val consecutiveFailures: Int = 0,
    val error: String? = null,
    val calibration: RawLabCalibration? = null,
    /** Makes each render's output file unique, so an image loader cannot show the last one. */
    val serial: Int = 0,
) {
    val hasRaf: Boolean get() = raf != null

    val isRendering: Boolean get() = rendering != null

    val isDirty: Boolean get() = settings != baseline

    /** A picture of settings that are no longer the current ones. */
    val isPreviewStale: Boolean get() = preview != null && preview.settings != settings

    val canRender: Boolean get() = hasRaf && !isRendering && importing == null

    /**
     * Any rendered file can be saved; [RawLabPreview.isFullResolution] decides whether the
     * screen says so plainly first, because saving a thumbnail believing it to be the JPEG
     * would be the worse failure.
     */
    val canSaveJpeg: Boolean get() = preview?.file?.isFile == true

    val canUpdateAppliedRecipe: Boolean get() = appliedRecipeId != null && isDirty

    /**
     * What the applicability predicates get to look at.
     *
     * No sensor generation anywhere in this app (FEAT-003 §8), so the widest field set is
     * assumed here exactly as the editor assumes it.
     */
    val fieldContext: FieldContext
        get() = FieldContext(
            generation = SensorGeneration.XTRANS_V,
            filmSimulationId = settings.text("filmSimulation") ?: "provia",
            grainEffectOff = (settings.text("grainEffect") ?: "off") == "off",
            whiteBalanceId = settings.text("whiteBalance") ?: "auto",
        )

    /** The fields to offer, split by whether the camera's RAW engine will act on them. */
    fun fields(supportedIds: Set<String>): RawLabFields {
        val applicable = RecipeFields.applicable(fieldContext)
        val (rendered, stored) = applicable.partition { it.id in supportedIds }
        return RawLabFields(rendered = rendered, storedOnly = stored)
    }

    /**
     * Whether an automatic render should start now.
     *
     * Deliberately narrow: something must have changed since the last picture, a render must
     * not already be running, and the automatic mode must still be on — it switches itself off
     * after [AUTO_PREVIEW_FAILURE_LIMIT] failures rather than hammering a camera that is
     * refusing.
     */
    fun shouldAutoRender(): Boolean = autoPreview && canRender && isPreviewStale

    // ─── Transitions ────────────────────────────────────────────────────────

    fun importing(total: Long?): RawLabState =
        copy(importing = RawImportProgress(0, total), error = null, calibration = null)

    fun importProgress(written: Long, total: Long?): RawLabState =
        copy(importing = RawImportProgress(written, total))

    /** A new RAF replaces the picture, never the settings: that is the point of trying one. */
    fun withRaf(file: File, name: String): RawLabState = copy(
        raf = file,
        rafName = name,
        importing = null,
        preview = null,
        error = null,
        calibration = null,
        consecutiveFailures = 0,
    )

    fun withoutRaf(): RawLabState =
        copy(raf = null, rafName = "", importing = null, preview = null, calibration = null)

    fun withSetting(fieldId: String, value: JsonElement?): RawLabState {
        val next = settings.toMutableMap()
        if (value == null) next.remove(fieldId) else next[fieldId] = value
        return copy(settings = JsonObject(next), error = null)
    }

    /**
     * Starts from a library recipe.
     *
     * The recipe's settings become both the working copy and the baseline, so "changed" means
     * changed from the recipe rather than from the defaults — which is what makes
     * **Update recipe** offerable and honest.
     */
    fun applying(recipe: Recipe): RawLabState = copy(
        settings = recipe.settings,
        baseline = recipe.settings,
        appliedRecipeId = recipe.id,
        appliedRecipeName = recipe.name,
        error = null,
    )

    /** Ground zero: the documented defaults, with no recipe behind them. */
    fun fromDefaults(): RawLabState = copy(
        settings = defaultRecipeSettings(),
        baseline = defaultRecipeSettings(),
        appliedRecipeId = null,
        appliedRecipeName = null,
        error = null,
    )

    /** Called after a save, so the thing just written becomes the new "unchanged". */
    fun savedAs(recipe: Recipe): RawLabState = copy(
        baseline = settings,
        appliedRecipeId = recipe.id,
        appliedRecipeName = recipe.name,
    )

    fun renderStarted(): RawLabState = copy(
        rendering = RawDevelopmentStage.Preparing,
        error = null,
        calibration = null,
        serial = serial + 1,
    )

    fun stage(stage: RawDevelopmentStage): RawLabState =
        if (rendering == null) this else copy(rendering = stage)

    fun renderSucceeded(result: RawDevelopmentResult, renderedSettings: JsonObject): RawLabState =
        copy(
            rendering = null,
            consecutiveFailures = 0,
            error = null,
            preview = RawLabPreview(
                file = result.jpeg,
                settings = renderedSettings,
                width = result.width,
                height = result.height,
                appliedFields = result.patch.appliedFields,
                preservedFields = result.patch.preservedFields,
                quality = result.quality,
                fromThumbnail = result.fromThumbnail,
            ),
        )

    /**
     * A failed render keeps the last picture: it is still the most recent true answer, and
     * clearing it would punish the user for the camera's refusal.
     */
    fun renderFailed(message: String): RawLabState {
        val failures = consecutiveFailures + 1
        return copy(
            rendering = null,
            error = message,
            consecutiveFailures = failures,
            autoPreview = autoPreview && failures < AUTO_PREVIEW_FAILURE_LIMIT,
        )
    }

    fun calibrationRequired(calibration: RawLabCalibration): RawLabState = copy(
        rendering = null,
        calibration = calibration,
        autoPreview = false,
    )

    fun withAutoPreview(enabled: Boolean): RawLabState =
        copy(autoPreview = enabled, consecutiveFailures = 0)

    fun withError(message: String?): RawLabState = copy(error = message)

    companion object {
        /** Two refusals in a row is a camera saying no, not a blip worth repeating. */
        const val AUTO_PREVIEW_FAILURE_LIMIT = 2
    }
}

/** The parameter panel's two bands. */
data class RawLabFields(
    /** Changing one of these changes the next render. */
    val rendered: List<RecipeField>,
    /** Saved into a recipe, but the camera's RAW engine will not act on it. */
    val storedOnly: List<RecipeField>,
)

private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.content
