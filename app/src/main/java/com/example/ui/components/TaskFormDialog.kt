package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Task
import com.example.data.model.TaskPriority

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskFormDialog(
    taskToEdit: Task?,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, priority: TaskPriority, category: String) -> Unit
) {
    var title by remember(taskToEdit) { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember(taskToEdit) { mutableStateOf(taskToEdit?.description ?: "") }
    var selectedPriority by remember(taskToEdit) {
        mutableStateOf(
            taskToEdit?.let { TaskPriority.fromString(it.priority) } ?: TaskPriority.MEDIUM
        )
    }
    var selectedCategory by remember(taskToEdit) {
        mutableStateOf(taskToEdit?.category ?: "Trabalho")
    }
    var hasError by remember { mutableStateOf(false) }

    val categories = listOf("Trabalho", "Estudos", "Saúde", "Compras", "Pessoal", "Geral")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (taskToEdit == null) {
                    stringResource(R.string.add_task_title)
                } else {
                    "Editar Tarefa #${taskToEdit.id}"
                },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (hasError && it.isNotBlank()) hasError = false
                    },
                    label = { Text(stringResource(R.string.task_title_label)) },
                    placeholder = { Text("Ex: Finalizar relatório mensal") },
                    isError = hasError,
                    supportingText = {
                        if (hasError) {
                            Text("O título da tarefa não pode ficar em branco.")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.task_description_label)) },
                    placeholder = { Text("Adicione detalhes, links ou observações...") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_desc_input")
                )

                // Priority Selection
                Text(
                    text = stringResource(R.string.task_priority_label),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.entries.forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    modifier = Modifier.height(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("priority_chip_${priority.name.lowercase()}")
                        )
                    }
                }

                // Category Selection
                Text(
                    text = stringResource(R.string.task_category_label),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory.equals(category, ignoreCase = true),
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            modifier = Modifier.testTag("category_chip_${category.lowercase()}")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        hasError = true
                    } else {
                        onSave(title, description, selectedPriority, selectedCategory)
                    }
                },
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_task_button")
            ) {
                Text(stringResource(R.string.cancel_button))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
