package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Task
import com.example.data.model.TaskPriority
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskFilter(val label: String) {
    ALL("Todas"),
    PENDING("Pendentes"),
    COMPLETED("Concluídas")
}

data class SearchByIdResult(
    val hasSearched: Boolean = false,
    val task: Task? = null,
    val errorMessage: String? = null
)

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todas")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Form Dialog state (Create / Edit)
    private val _editingTask = MutableStateFlow<Task?>(null)
    val editingTask: StateFlow<Task?> = _editingTask.asStateFlow()

    private val _isFormOpen = MutableStateFlow(false)
    val isFormOpen: StateFlow<Boolean> = _isFormOpen.asStateFlow()

    // Delete Confirmation state
    private val _taskToDelete = MutableStateFlow<Task?>(null)
    val taskToDelete: StateFlow<Task?> = _taskToDelete.asStateFlow()

    // Search by ID Modal / Result state
    private val _isSearchByIdDialogOpen = MutableStateFlow(false)
    val isSearchByIdDialogOpen: StateFlow<Boolean> = _isSearchByIdDialogOpen.asStateFlow()

    private val _searchByIdInput = MutableStateFlow("")
    val searchByIdInput: StateFlow<String> = _searchByIdInput.asStateFlow()

    private val _searchByIdResult = MutableStateFlow(SearchByIdResult())
    val searchByIdResult: StateFlow<SearchByIdResult> = _searchByIdResult.asStateFlow()

    // Snackbar event for Undo
    private val _recentlyDeletedTask = MutableStateFlow<Task?>(null)
    val recentlyDeletedTask: StateFlow<Task?> = _recentlyDeletedTask.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    val tasks: StateFlow<List<Task>> = combine(
        repository.allTasks,
        _searchQuery,
        _selectedFilter,
        _selectedCategory
    ) { allList, query, filter, category ->
        allList.filter { task ->
            // Filter by completion status
            val matchesFilter = when (filter) {
                TaskFilter.ALL -> true
                TaskFilter.PENDING -> !task.isCompleted
                TaskFilter.COMPLETED -> task.isCompleted
            }

            // Filter by category
            val matchesCategory = if (category == "Todas") true else task.category.equals(category, ignoreCase = true)

            // Filter by search query (supports matching by title, description or numeric ID)
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val cleanQuery = query.trim().removePrefix("#")
                val matchesId = cleanQuery.toLongOrNull()?.let { it == task.id } ?: false
                matchesId ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)
            }

            matchesFilter && matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stats: StateFlow<TaskStats> = repository.allTasks.combine(_searchQuery) { allList, _ ->
        val total = allList.size
        val completed = allList.count { it.isCompleted }
        val pending = total - completed
        val percent = if (total > 0) (completed.toFloat() / total) else 0f
        TaskStats(total = total, completed = completed, pending = pending, completionRate = percent)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskStats()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelect(filter: TaskFilter) {
        _selectedFilter.value = filter
    }

    fun onCategorySelect(category: String) {
        _selectedCategory.value = category
    }

    fun openCreateDialog() {
        _editingTask.value = null
        _isFormOpen.value = true
    }

    fun openEditDialog(task: Task) {
        _editingTask.value = task
        _isFormOpen.value = true
    }

    fun closeFormDialog() {
        _isFormOpen.value = false
        _editingTask.value = null
    }

    fun saveTask(
        title: String,
        description: String,
        priority: TaskPriority,
        category: String
    ) {
        val currentEdit = _editingTask.value
        viewModelScope.launch {
            if (currentEdit != null) {
                repository.updateTask(
                    currentEdit.copy(
                        title = title.trim(),
                        description = description.trim(),
                        priority = priority.name,
                        category = category.trim().ifBlank { "Geral" }
                    )
                )
            } else {
                repository.insertTask(
                    Task(
                        title = title.trim(),
                        description = description.trim(),
                        isCompleted = false,
                        priority = priority.name,
                        category = category.trim().ifBlank { "Geral" }
                    )
                )
            }
            closeFormDialog()
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.setTaskCompletion(task.id, !task.isCompleted)
            // Update search result if the task is currently open in search dialog
            if (_searchByIdResult.value.task?.id == task.id) {
                _searchByIdResult.value = _searchByIdResult.value.copy(
                    task = task.copy(isCompleted = !task.isCompleted)
                )
            }
        }
    }

    fun requestDeleteTask(task: Task) {
        _taskToDelete.value = task
    }

    fun dismissDeleteDialog() {
        _taskToDelete.value = null
    }

    fun confirmDeleteTask() {
        val task = _taskToDelete.value ?: return
        viewModelScope.launch {
            _recentlyDeletedTask.value = task
            repository.deleteTask(task)
            _taskToDelete.value = null
            // If deleting from search-by-id modal
            if (_searchByIdResult.value.task?.id == task.id) {
                _searchByIdResult.value = SearchByIdResult(
                    hasSearched = true,
                    task = null,
                    errorMessage = "Tarefa #${task.id} removida com sucesso."
                )
            }
        }
    }

    fun undoDelete() {
        val task = _recentlyDeletedTask.value ?: return
        viewModelScope.launch {
            repository.insertTask(task)
            _recentlyDeletedTask.value = null
        }
    }

    fun clearUndoSnackbar() {
        _recentlyDeletedTask.value = null
    }

    // Search by ID Modal methods
    fun openSearchByIdDialog() {
        _searchByIdInput.value = ""
        _searchByIdResult.value = SearchByIdResult()
        _isSearchByIdDialogOpen.value = true
    }

    fun closeSearchByIdDialog() {
        _isSearchByIdDialogOpen.value = false
        _searchByIdInput.value = ""
        _searchByIdResult.value = SearchByIdResult()
    }

    fun onSearchByIdInputChange(value: String) {
        _searchByIdInput.value = value.filter { it.isDigit() }
    }

    fun executeSearchById() {
        val raw = _searchByIdInput.value.trim()
        val id = raw.toLongOrNull()
        if (id == null) {
            _searchByIdResult.value = SearchByIdResult(
                hasSearched = true,
                task = null,
                errorMessage = "Por favor, digite um número de ID válido."
            )
            return
        }

        viewModelScope.launch {
            val task = repository.getTaskById(id)
            if (task != null) {
                _searchByIdResult.value = SearchByIdResult(
                    hasSearched = true,
                    task = task,
                    errorMessage = null
                )
            } else {
                _searchByIdResult.value = SearchByIdResult(
                    hasSearched = true,
                    task = null,
                    errorMessage = "Nenhuma tarefa encontrada com o ID #$id."
                )
            }
        }
    }

    fun deleteTaskByIdFromSearch(id: Long) {
        viewModelScope.launch {
            val task = repository.getTaskById(id)
            if (task != null) {
                _recentlyDeletedTask.value = task
                repository.deleteTask(task)
                _searchByIdResult.value = SearchByIdResult(
                    hasSearched = true,
                    task = null,
                    errorMessage = "Tarefa #$id removida com sucesso!"
                )
            }
        }
    }
}

data class TaskStats(
    val total: Int = 0,
    val completed: Int = 0,
    val pending: Int = 0,
    val completionRate: Float = 0f
)

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
