package com.jon_is_awesome.hfg_portal_app.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jon_is_awesome.hfg_portal_app.data.model.Budget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetScreen(
    onNavigateBack: () -> Unit,
    onSaveBudget: (Budget) -> Unit
) {
    var category by remember { mutableStateOf("") }
    var limitAmount by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Budget") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (e.g., Food, Rent)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = limitAmount,
                onValueChange = { limitAmount = it },
                label = { Text("Monthly Limit ($)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (category.isNotBlank() && limitAmount.isNotBlank()) {
                        onSaveBudget(
                            Budget(
                                category = category,
                                limitAmount = limitAmount.toDoubleOrNull() ?: 0.0
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = category.isNotBlank() && limitAmount.isNotBlank()
            ) {
                Text("Save Budget")
            }
        }
    }
}
