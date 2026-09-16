package com.example.data.repository

import com.example.data.local.TaskDao
import com.example.data.model.Task
import com.example.data.model.TaskPriority
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    fun getTaskByIdFlow(id: Long): Flow<Task?> = taskDao.getTaskByIdFlow(id)

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    fun searchTasks(query: String): Flow<List<Task>> = taskDao.searchTasks(query)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun deleteTaskById(id: Long): Boolean = taskDao.deleteTaskById(id) > 0

    suspend fun setTaskCompletion(id: Long, isCompleted: Boolean) = 
        taskDao.updateTaskCompletion(id, isCompleted)

    suspend fun seedInitialDataIfEmpty() {
        if (taskDao.getTaskCount() == 0) {
            val initialTasks = listOf(
                Task(
                    title = "Organizar prioridades do dia",
                    description = "Definir as 3 metas mais importantes e urgentes.",
                    isCompleted = true,
                    priority = TaskPriority.HIGH.name,
                    category = "Trabalho"
                ),
                Task(
                    title = "Treinar desenvolvimento Android",
                    description = "Explorar operações CRUD, Room Database e Jetpack Compose.",
                    isCompleted = false,
                    priority = TaskPriority.HIGH.name,
                    category = "Estudos"
                ),
                Task(
                    title = "Caminhada de 30 minutos",
                    description = "Alongamento leve e caminhada ao ar livre.",
                    isCompleted = false,
                    priority = TaskPriority.MEDIUM.name,
                    category = "Saúde"
                ),
                Task(
                    title = "Comprar café e frutas",
                    description = "Passar no mercado após o expediente.",
                    isCompleted = false,
                    priority = TaskPriority.LOW.name,
                    category = "Compras"
                )
            )
            for (task in initialTasks) {
                taskDao.insertTask(task)
            }
        }
    }
}
