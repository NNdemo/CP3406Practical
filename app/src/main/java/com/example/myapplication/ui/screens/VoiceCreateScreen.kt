package com.example.myapplication.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.viewmodels.VoiceCreateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCreateScreen(
    modifier: Modifier = Modifier,
    viewModel: VoiceCreateViewModel = viewModel()
) {
    var isRecording by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isRecording = true
            viewModel.startRecording()
        } else {
            showPermissionDialog = true
        }
    }
    
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要录音权限") },
            text = { Text("为了使用语音创建功能，我们需要录音权限。请在设置中开启录音权限。") },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语音创建") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 录音状态显示
            Text(
                text = if (isRecording) "正在录音..." else "点击开始录音",
                style = MaterialTheme.typography.titleLarge
            )
            
            // 录音按钮
            FilledTonalButton(
                onClick = {
                    if (isRecording) {
                        isRecording = false
                        viewModel.stopRecording()
                    } else {
                        if (viewModel.hasRecordPermission()) {
                            isRecording = true
                            viewModel.startRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier.size(120.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "停止录音" else "开始录音",
                    modifier = Modifier.size(48.dp)
                )
            }
            
            // 识别结果显示
            if (viewModel.recognizedText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "识别结果：",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.recognizedText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            
            // 创建按钮
            if (viewModel.recognizedText.isNotEmpty() && !isRecording) {
                Button(
                    onClick = { viewModel.createSchedule() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("创建日程")
                }
            }
        }
    }
} 