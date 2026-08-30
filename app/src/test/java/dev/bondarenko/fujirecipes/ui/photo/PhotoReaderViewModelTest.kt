package dev.bondarenko.fujirecipes.ui.photo

import dev.bondarenko.fujirecipes.data.model.Recipe
import dev.bondarenko.fujirecipes.data.photo.PhotoReadFailure
import dev.bondarenko.fujirecipes.data.photo.SyntheticJpeg
import dev.bondarenko.fujirecipes.data.repo.ImportOutcome
import dev.bondarenko.fujirecipes.data.repo.LibraryState
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoReaderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repo = object : RecipeRepository {
        override val library: Flow<LibraryState> = MutableStateFlow(LibraryState(recipes = emptyList(), hasLoaded = true))
        override suspend fun load() = Unit
        override suspend fun create(body: kotlinx.serialization.json.JsonObject): dev.bondarenko.fujirecipes.core.result.LibraryResult<Recipe> = throw NotImplementedError()
        override suspend fun update(id: String, body: kotlinx.serialization.json.JsonObject): dev.bondarenko.fujirecipes.core.result.LibraryResult<Recipe> = throw NotImplementedError()
        override suspend fun delete(id: String): dev.bondarenko.fujirecipes.core.result.LibraryResult<Unit> = throw NotImplementedError()
        override suspend fun importAll(body: kotlinx.serialization.json.JsonObject): dev.bondarenko.fujirecipes.core.result.LibraryResult<ImportOutcome> = throw NotImplementedError()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `read valid jpeg updates state to Result`() = runTest(testDispatcher) {
        val jpeg = SyntheticJpeg.fujifilm(
            tags = listOf(
                SyntheticJpeg.u16(5121, 1536),
                SyntheticJpeg.u16(5122, 513),
                SyntheticJpeg.i32(4161, -32),
                SyntheticJpeg.i32(4160, -16),
                SyntheticJpeg.u16(4099, 256),
                SyntheticJpeg.u16(4097, 132),
                SyntheticJpeg.u16(4110, 736),
                SyntheticJpeg.i32(4111, 2000),
                SyntheticJpeg.u16(4167, 64),
                SyntheticJpeg.u16(4172, 32),
                SyntheticJpeg.u16(4168, 32),
                SyntheticJpeg.u16(4174, 64),
                SyntheticJpeg.u16(4098, 4080),
                SyntheticJpeg.u16(4101, 6500),
            ),
            wbShift = 40 to -80,
        )
        val vm = PhotoReaderViewModel(
            repository = repo,
            readPhoto = { jpeg },
            defaultDispatcher = testDispatcher,
        )

        vm.read("content://photo.jpg")
        advanceUntilIdle()

        assertIs<PhotoReaderStage.Result>(vm.state.value.stage)
    }

    @Test
    fun `read non-jpeg sets state to Failed`() = runTest(testDispatcher) {
        val vm = PhotoReaderViewModel(
            repository = repo,
            readPhoto = { SyntheticJpeg.notJpeg() },
            defaultDispatcher = testDispatcher,
        )

        vm.read("content://photo.jpg")
        advanceUntilIdle()

        val stage = vm.state.value.stage
        assertIs<PhotoReaderStage.Failed>(stage)
        assertEquals(PhotoReadFailure.NOT_JPEG, stage.reason)
    }

    @Test
    fun `read unreadable uri sets state to Failed`() = runTest(testDispatcher) {
        val vm = PhotoReaderViewModel(
            repository = repo,
            readPhoto = { null },
            defaultDispatcher = testDispatcher,
        )

        vm.read("content://invalid.jpg")
        advanceUntilIdle()

        val stage = vm.state.value.stage
        assertIs<PhotoReaderStage.Failed>(stage)
        assertEquals(PhotoReadFailure.UNREADABLE, stage.reason)
    }

    @Test
    fun `read same uri twice does not re-read when already in Result stage`() = runTest(testDispatcher) {
        var readCount = 0
        val jpeg = SyntheticJpeg.fujifilm(listOf(SyntheticJpeg.u16(5121, 1536)))
        val vm = PhotoReaderViewModel(
            repository = repo,
            readPhoto = {
                readCount++
                jpeg
            },
            defaultDispatcher = testDispatcher,
        )

        vm.read("content://photo.jpg")
        advanceUntilIdle()
        assertEquals(1, readCount)

        vm.read("content://photo.jpg")
        advanceUntilIdle()
        assertEquals(1, readCount)
    }

    @Test
    fun `reset clears state and allows re-reading`() = runTest(testDispatcher) {
        var readCount = 0
        val jpeg = SyntheticJpeg.fujifilm(listOf(SyntheticJpeg.u16(5121, 1536)))
        val vm = PhotoReaderViewModel(
            repository = repo,
            readPhoto = {
                readCount++
                jpeg
            },
            defaultDispatcher = testDispatcher,
        )

        vm.read("content://photo.jpg")
        advanceUntilIdle()
        assertEquals(1, readCount)
        assertIs<PhotoReaderStage.Result>(vm.state.value.stage)

        vm.reset()
        assertIs<PhotoReaderStage.Empty>(vm.state.value.stage)

        vm.read("content://photo.jpg")
        advanceUntilIdle()
        assertEquals(2, readCount)
    }

    @Test
    fun `addPhotoToRecipe saves image and updates recipe in repo`() = runTest(testDispatcher) {
        val testRecipe = Recipe(
            id = "recipe-1",
            name = "Test Recipe",
            images = listOf("existing.webp"),
        )
        var updatedRecipeJson: kotlinx.serialization.json.JsonObject? = null
        val repoWithRecipe = object : RecipeRepository {
            override val library: Flow<LibraryState> = MutableStateFlow(LibraryState(recipes = listOf(testRecipe), hasLoaded = true))
            override suspend fun load() = Unit
            override suspend fun create(body: kotlinx.serialization.json.JsonObject): dev.bondarenko.fujirecipes.core.result.LibraryResult<Recipe> = throw NotImplementedError()
            override suspend fun update(id: String, body: kotlinx.serialization.json.JsonObject): dev.bondarenko.fujirecipes.core.result.LibraryResult<Recipe> {
                updatedRecipeJson = body
                return dev.bondarenko.fujirecipes.core.result.LibraryResult.Success(Recipe.fromJson(body))
            }
            override suspend fun delete(id: String): dev.bondarenko.fujirecipes.core.result.LibraryResult<Unit> = throw NotImplementedError()
            override suspend fun importAll(body: kotlinx.serialization.json.JsonObject): dev.bondarenko.fujirecipes.core.result.LibraryResult<ImportOutcome> = throw NotImplementedError()
        }

        val jpeg = SyntheticJpeg.fujifilm(listOf(SyntheticJpeg.u16(5121, 1536)))
        val vm = PhotoReaderViewModel(
            repository = repoWithRecipe,
            readPhoto = { jpeg },
            savePhoto = { "new_photo.webp" },
            defaultDispatcher = testDispatcher,
        )

        vm.read("content://photo.jpg")
        advanceUntilIdle()

        vm.addPhotoToRecipe("recipe-1")
        advanceUntilIdle()

        assertEquals(true, vm.state.value.addedPhotoUris.contains("content://photo.jpg"))
        assertEquals(false, vm.state.value.isAddingPhoto)
        val savedRecipe = updatedRecipeJson?.let { Recipe.fromJson(it) }
        assertEquals(listOf("existing.webp", "new_photo.webp"), savedRecipe?.images)
    }

    @Test
    fun `read multiple uris populates list and supports selectPhoto`() = runTest(testDispatcher) {
        val jpeg1 = SyntheticJpeg.fujifilm(listOf(SyntheticJpeg.u16(5121, 1536))) // Classic Chrome
        val jpeg2 = SyntheticJpeg.fujifilm(listOf(SyntheticJpeg.u16(5121, 1280))) // Velvia

        val vm = PhotoReaderViewModel(
            repository = repo,
            readPhoto = { uri ->
                if (uri.contains("1")) jpeg1 else jpeg2
            },
            defaultDispatcher = testDispatcher,
        )

        vm.read(listOf("content://photo1.jpg", "content://photo2.jpg"))
        advanceUntilIdle()

        val stage = vm.state.value.stage
        assertIs<PhotoReaderStage.Result>(stage)
        assertEquals(2, stage.photos.size)
        assertEquals("content://photo1.jpg", stage.currentPhoto.uri)
        assertEquals(0, stage.selectedIndex)

        vm.selectPhoto(1)
        val updatedStage = vm.state.value.stage
        assertIs<PhotoReaderStage.Result>(updatedStage)
        assertEquals(1, updatedStage.selectedIndex)
        assertEquals("content://photo2.jpg", updatedStage.currentPhoto.uri)
    }
}
