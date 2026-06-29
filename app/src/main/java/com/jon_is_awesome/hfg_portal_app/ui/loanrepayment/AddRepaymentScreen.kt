package com.jon_is_awesome.hfg_portal_app.ui.loanrepayment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRepaymentScreen(
    loanId: Int,
    viewModel: LoanRepaymentViewModel,
    onNavigateBack: () -> Unit
) {
    var paidAmount by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isFormValid = paidAmount.isNotBlank() && paidAmount.toDoubleOrNull() != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Payment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = paidAmount,
                onValueChange = { paidAmount = it },
                label = { Text("Amount *") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = paidAmount.isNotBlank() && paidAmount.toDoubleOrNull() == null,
                supportingText = {
                    if (paidAmount.isNotBlank() && paidAmount.toDoubleOrNull() == null) {
                        Text("Enter a valid amount")
                    }
                }
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        isSaving = true
                        errorMessage = null
                        viewModel.addRepayment(
                            loanId = loanId,
                            paidAmount = paidAmount.trim()
                        ) { success ->
                            isSaving = false
                            if (success) onNavigateBack()
                            else errorMessage = "Failed to save payment. Please try again."
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isFormValid && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        }
    }
}
