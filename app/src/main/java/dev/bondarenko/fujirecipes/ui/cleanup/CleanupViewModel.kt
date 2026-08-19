package dev.bondarenko.fujirecipes.ui.cleanup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.data.cleanup.CleanupScanResult
import dev.bondarenko.fujirecipes.data.cleanup.DuplicateFinder
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CleanupStage {
    /** Initial idle state before user presses "Find duplicated". */
    data object Initial : CleanupStage

    /** Scanning in progress. */
    data object Scanning : CleanupStage

    /** Scan completed and results ready. */
    data class Results(
        val result: CleanupScanResult,
        /** Maps exact duplicate groupId to the recipeId the user selected to KEEP. */
        val selectedKeepMap: Map<String, String>,
        val isDeleting: Boolean = false,
    ) : CleanupStage
}

data class CleanupUiState(
    val stage: CleanupStage = CleanupStage.Initial,
    val totalRecipes: Int = 0,
)

class CleanupViewModel(
    private val repository: RecipeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CleanupUiState())
    val state: StateFlow<CleanupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.library.collect { lib ->
                _state.update { current ->
                    current.copy(totalRecipes = lib.recipes.size)
                }
            }
        }
    }

    /**
     * Finds duplicates across the library with a minimum spinner duration.
     */
    fun findDuplicates(minDelayMs: Long = 3000L) {
        viewModelScope.launch {
            _state.update { it.copy(stage = CleanupStage.Scanning) }

            val startTime = System.currentTimeMillis()
            val recipes = repository.library.first().recipes
            val scanResult = DuplicateFinder.findDuplicates(recipes)
            val keepMap = scanResult.exactGroups.associate { it.id to it.defaultKeepId }

            val elapsed = System.currentTimeMillis() - startTime
            val remaining = (minDelayMs - elapsed).coerceAtLeast(0L)
            if (remaining > 0) {
                delay(remaining)
            }

            _state.update {
                it.copy(
                    stage = CleanupStage.Results(
                        result = scanResult,
                        selectedKeepMap = keepMap,
                    ),
                )
            }
        }
    }

    /**
     * Resets stage back to Initial.
     */
    fun reset() {
        _state.update { it.copy(stage = CleanupStage.Initial) }
    }

    /**
     * User chooses which recipe to keep in a duplicate group.
     */
    fun selectKeep(groupId: String, recipeId: String) {
        _state.update { current ->
            val stage = current.stage as? CleanupStage.Results ?: return@update current
            current.copy(
                stage = stage.copy(
                    selectedKeepMap = stage.selectedKeepMap + (groupId to recipeId),
                ),
            )
        }
    }

    /**
     * Deletes the unselected duplicate recipes in the specified group.
     */
    fun deleteDuplicatesForGroup(groupId: String) {
        val currentStage = _state.value.stage as? CleanupStage.Results ?: return
        val group = currentStage.result.exactGroups.firstOrNull { it.id == groupId } ?: return
        val keepId = currentStage.selectedKeepMap[groupId] ?: group.defaultKeepId
        val toDelete = group.recipes.filterNot { it.id == keepId }

        viewModelScope.launch {
            _state.update {
                val results = it.stage as? CleanupStage.Results
                if (results != null) it.copy(stage = results.copy(isDeleting = true)) else it
            }
            for (recipe in toDelete) {
                repository.delete(recipe.id)
            }
            findDuplicates(minDelayMs = 0L)
        }
    }

    /**
     * Deletes all unselected duplicate recipes across all exact match groups.
     */
    fun deleteAllDuplicates() {
        val currentStage = _state.value.stage as? CleanupStage.Results ?: return
        val toDelete = mutableListOf<String>()

        for (group in currentStage.result.exactGroups) {
            val keepId = currentStage.selectedKeepMap[group.id] ?: group.defaultKeepId
            toDelete += group.recipes.filterNot { it.id == keepId }.map { it.id }
        }

        if (toDelete.isEmpty()) return

        viewModelScope.launch {
            _state.update {
                val results = it.stage as? CleanupStage.Results
                if (results != null) it.copy(stage = results.copy(isDeleting = true)) else it
            }
            for (id in toDelete) {
                repository.delete(id)
            }
            findDuplicates(minDelayMs = 0L)
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                CleanupViewModel(container.recipeRepository)
            }
        }
    }
}
