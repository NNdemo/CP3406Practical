package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
    showTopBar: Boolean = true,
    onNavigateToJcu: () -> Unit = {}
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
            // JCU 课程导入卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                onClick = onNavigateToJcu
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "JCU",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "JCU Course Management",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Manage your JCU courses, import courses to schedule, view course statistics.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // API 使用提示卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Information",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "API Usage Recommendations",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "We recommend configuring and using the ChatGPT API first as it is more stable. The Grok API is still in testing phase and may experience instability.",
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // ChatGPT API Key
            OutlinedTextField(
                value = uiState.chatgptApiKey,
                onValueChange = { viewModel.updateChatgptApiKey(it) },
                label = { Text("ChatGPT API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Grok API Key
            OutlinedTextField(
                value = uiState.grokApiKey,
                onValueChange = { viewModel.updateGrokApiKey(it) },
                label = { Text("Grok API Key (X.AI)") },
                placeholder = { Text("Format: xai-xxxxxxxxxxxx") },
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
                        AiModel.CHATGPT -> "ChatGPT (Recommended)"
                        AiModel.GROK -> "Grok (X.AI) - Beta"
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
                        text = { Text("ChatGPT (Recommended)") },
                        onClick = {
                            viewModel.updateDefaultAiModel(AiModel.CHATGPT)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Grok (X.AI) - Beta") },
                        onClick = {
                            viewModel.updateDefaultAiModel(AiModel.GROK)
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