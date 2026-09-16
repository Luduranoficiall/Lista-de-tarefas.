package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskPriority(val label: String, val level: Int) {
    HIGH("Alta", 3),
    MEDIUM("Média", 2),
    LOW("Baixa", 1);

    companion object {
        fun fromString(value: String): TaskPriority {
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) 
            } ?: MEDIUM
        }
    }
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: String = TaskPriority.MEDIUM.name,
    val category: String = "Geral",
    val createdAt: Long = System.currentTimeMillis()
)
