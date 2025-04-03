/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.myapplication.ui.screens.CalendarScreen
import com.example.myapplication.ui.screens.DashboardScreen
import com.example.myapplication.ui.screens.SettingsScreen
import com.example.myapplication.ui.screens.TimelineScreen
import com.example.myapplication.ui.screens.TextCreateScreen
import com.example.myapplication.ui.screens.JcuScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.viewmodels.DashboardViewModel
import com.example.myapplication.ui.viewmodels.CalendarViewModel
import com.example.myapplication.ui.viewmodels.ScheduleViewModel
import com.example.myapplication.ui.viewmodels.SettingsViewModel
import com.example.myapplication.ui.viewmodels.JcuViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            MyApplicationTheme {
                var selectedTab by remember { mutableStateOf(0) }
                
                AdaptiveLayout(
                    windowSizeClass = windowSizeClass,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    }
}

@Composable
fun AdaptiveLayout(
    windowSizeClass: WindowSizeClass,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val useNavRail = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val showTopBar = windowSizeClass.heightSizeClass != WindowHeightSizeClass.Compact
    
    var showJcuScreen by remember { mutableStateOf(false) }

    Row(modifier = modifier.fillMaxSize()) {
        if (useNavRail) {
            NavigationRail {
                NavigationRailItem(
                    selected = selectedTab == 0 && !showJcuScreen,
                    onClick = { 
                        onTabSelected(0)
                        showJcuScreen = false
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationRailItem(
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Schedule") },
                    label = { Text("Schedule") }
                )
                NavigationRailItem(
                    selected = selectedTab == 2,
                    onClick = { onTabSelected(2) },
                    icon = { Icon(Icons.Default.Timeline, contentDescription = "Timeline") },
                    label = { Text("Timeline") }
                )
                NavigationRailItem(
                    selected = selectedTab == 3,
                    onClick = { onTabSelected(3) },
                    icon = { Icon(Icons.Default.TextSnippet, contentDescription = "Text") },
                    label = { Text("Text") }
                )
                NavigationRailItem(
                    selected = showJcuScreen,
                    onClick = { showJcuScreen = true },
                    icon = { Icon(Icons.Default.School, contentDescription = "JCU") },
                    label = { Text("JCU") }
                )
            }
        }

        Scaffold(
            modifier = Modifier.weight(1f),
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == 0 && !showJcuScreen,
                            onClick = { 
                                onTabSelected(0)
                                showJcuScreen = false
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1 && !showJcuScreen,
                            onClick = {
                                onTabSelected(1)
                                showJcuScreen = false
                            },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = "Schedule") },
                            label = { Text("Schedule") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2 && !showJcuScreen,
                            onClick = {
                                onTabSelected(2) 
                                showJcuScreen = false
                            },
                            icon = { Icon(Icons.Default.Timeline, contentDescription = "Timeline") },
                            label = { Text("Timeline") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3 && !showJcuScreen,
                            onClick = {
                                onTabSelected(3)
                                showJcuScreen = false
                            },
                            icon = { Icon(Icons.Default.TextSnippet, contentDescription = "Text") },
                            label = { Text("Text") }
                        )
                        NavigationBarItem(
                            selected = (selectedTab == 4 && !showJcuScreen) || showJcuScreen,
                            onClick = {
                                if (selectedTab == 4 && !showJcuScreen) {
                                    showJcuScreen = true
                                } else {
                                    onTabSelected(4)
                                    showJcuScreen = false
                                }
                            },
                            icon = { Icon(
                                if (showJcuScreen) Icons.Default.School 
                                else Icons.Default.Settings, 
                                contentDescription = if (showJcuScreen) "JCU" else "Settings"
                            ) },
                            label = { Text(if (showJcuScreen) "JCU" else "Settings") }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                if (showJcuScreen) {
                    JcuScreen(
                        viewModel = viewModel<JcuViewModel>()
                    )
                } else {
                    when (selectedTab) {
                        0 -> DashboardScreen(
                            viewModel = viewModel<DashboardViewModel>()
                        )
                        1 -> CalendarScreen(
                            viewModel = viewModel<CalendarViewModel>()
                        )
                        2 -> TimelineScreen(showTopBar = showTopBar)
                        3 -> TextCreateScreen(
                            onBackClick = {
                                // 切换回主屏幕
                                onTabSelected(0)
                            },
                            scheduleViewModel = viewModel<ScheduleViewModel>()
                        )
                        4 -> SettingsScreen(
                            viewModel = viewModel<SettingsViewModel>(),
                            onNavigateToJcu = { showJcuScreen = true }
                        )
                    }
                }
            }
        }
    }
}
