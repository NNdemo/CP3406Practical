package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.viewmodels.AiModel
import com.example.myapplication.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
    showTopBar: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Settings") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // DeepSeek API Key
//            OutlinedTextField(
//                value = uiState.deepseekApiKey,
//                onValueChange = { viewModel.updateDeepseekApiKey(it) },
//                label = { Text("DeepSeek API Key") },
//                modifier = Modifier.fillMaxWidth()
//            )
            
            // ChatGPT API Key
            OutlinedTextField(
                value = uiState.chatgptApiKey,
                onValueChange = { viewModel.updateChatgptApiKey(it) },
                label = { Text("ChatGPT API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Default AI Model Selection
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = when (uiState.defaultAiModel) {
//                        AiModel.DEEPSEEK -> "DeepSeek"
                        AiModel.CHATGPT -> "ChatGPT"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Default AI Model") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
//                    DropdownMenuItem(
//                        text = { Text("DeepSeek") },
//                        onClick = {
//                            viewModel.updateDefaultAiModel(AiModel.DEEPSEEK)
//                            expanded = false
//                        }
//                    )
                    DropdownMenuItem(
                        text = { Text("ChatGPT") },
                        onClick = {
                            viewModel.updateDefaultAiModel(AiModel.CHATGPT)
                            expanded = false
                        }
                    )
                }
            }
            
            // Save Button
            Button(
                onClick = { viewModel.saveSettings() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
} 