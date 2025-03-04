package com.example.myapplication.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCreateScreen(
    modifier: Modifier = Modifier,
    viewModel: VoiceCreateViewModel = viewModel(),
    showTopBar: Boolean = true
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
            title = { Text("Microphone Permission Required") },
            text = { Text("To use voice creation feature, we need microphone permission. Please enable it in settings.") },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Voice Create") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recording status display
            Text(
                text = if (isRecording) "Recording..." else "Tap to Start Recording",
                style = MaterialTheme.typography.titleLarge
            )
            
            // Recording button
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
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    modifier = Modifier.size(48.dp)
                )
            }
            
            // Recognition result display
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
                            text = "Recognition Result:",
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
            
            // Create button
            if (viewModel.recognizedText.isNotEmpty() && !isRecording) {
                Button(
                    onClick = { viewModel.createSchedule() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Schedule")
                }
            }
        }
    }
} 