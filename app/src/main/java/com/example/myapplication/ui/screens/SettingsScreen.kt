package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
                    title = { Text("设置") },
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
                            contentDescription = "信息",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "API 使用建议",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "我们建议首先配置并使用ChatGPT API，因为它更稳定。Grok API仍处于测试阶段，可能会出现不稳定现象。",
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // ChatGPT API Key
            OutlinedTextField(
                value = uiState.chatgptApiKey,
                onValueChange = { viewModel.updateChatgptApiKey(it) },
                label = { Text("ChatGPT API 密钥") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Grok API Key
            OutlinedTextField(
                value = uiState.grokApiKey,
                onValueChange = { viewModel.updateGrokApiKey(it) },
                label = { Text("Grok API 密钥 (X.AI)") },
                placeholder = { Text("格式: xai-xxxxxxxxxxxx") },
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
                        AiModel.CHATGPT -> "ChatGPT (推荐)"
                        AiModel.GROK -> "Grok (X.AI) - 测试版"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("默认 AI 模型") },
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
                        text = { Text("ChatGPT (推荐)") },
                        onClick = {
                            viewModel.updateDefaultAiModel(AiModel.CHATGPT)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Grok (X.AI) - 测试版") },
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
                Text("保存设置")
            }
        }
    }
} 