package com.example.myapplication

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.model.Schedule
import com.example.myapplication.ui.screens.TextCreateScreen
import com.example.myapplication.ui.screens.SettingsScreen
import com.example.myapplication.ui.screens.CalendarScreen
import com.example.myapplication.ui.screens.DashboardScreen
import com.example.myapplication.ui.viewmodels.ScheduleViewModel
import com.example.myapplication.ui.viewmodels.SettingsViewModel
import com.example.myapplication.ui.viewmodels.DashboardViewModel
import com.example.myapplication.ui.viewmodels.CalendarViewModel
import java.time.LocalDateTime

// 静态回调对象，不再需要
// object CallbackHolder {
//     var addScheduleCallback: ((Schedule) -> Long)? = null
// }

enum class AppScreen {
    HomeScreen,
    SettingsScreen,
    VoiceCreateScreen,
    TextCreateScreen,
    CalendarScreen
}

@Composable
fun GuideApp(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppScreen.HomeScreen.name
) {
    val context = LocalContext.current
    val scheduleViewModel = viewModel<ScheduleViewModel>()
    val dashboardViewModel = viewModel<DashboardViewModel>()
    val calendarViewModel = viewModel<CalendarViewModel>()
    
    // 明确定义回调函数而不是直接使用lambda
    fun handleScheduleAdded(events: List<Schedule>, currentDate: LocalDateTime) {
        println("GuideActivity: handleScheduleAdded被调用 - 添加${events.size}个日程")
        
        try {
            events.forEach { schedule ->
                println("GuideActivity: 尝试添加日程 - 标题=\"${schedule.title}\"")
                val id = scheduleViewModel.addSchedule(schedule)
                println("GuideActivity: 日程已添加，ID=$id")
            }
            
            scheduleViewModel.refreshSchedules(currentDate)
            println("GuideActivity: 刷新日程列表成功")
            
            // 通知用户添加成功
            Toast.makeText(context, "已添加${events.size}个日程", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            println("GuideActivity: 处理日程添加时出错 - ${e.message}")
            e.printStackTrace()
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = AppScreen.HomeScreen.name,
    ) {
        composable(route = AppScreen.HomeScreen.name) {
            println("GuideActivity: 导航到HomeScreen")
            
            DashboardScreen(
                viewModel = dashboardViewModel
            )
        }
        
        composable(route = AppScreen.SettingsScreen.name) {
            println("GuideActivity: 导航到SettingsScreen")
            
            SettingsScreen(
                viewModel = viewModel<SettingsViewModel>()
            )
        }
        
        composable(route = AppScreen.TextCreateScreen.name) {
            println("GuideActivity: 导航到TextCreateScreen")
            
            TextCreateScreen(
                onBackClick = {
                    navController.navigateUp()
                },
                scheduleViewModel = scheduleViewModel  // 直接传递scheduleViewModel
            )
        }
        
        composable(route = AppScreen.CalendarScreen.name) {
            println("GuideActivity: 导航到CalendarScreen")
            
            CalendarScreen(
                viewModel = calendarViewModel
            )
        }
    }
}

@Composable
fun BottomNavBar(
    currentScreen: String,
    onScreenChange: (AppScreen) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentScreen == AppScreen.HomeScreen.name,
            onClick = { onScreenChange(AppScreen.HomeScreen) },
            icon = { Icon(Icons.Default.Home, contentDescription = "主页") },
            label = { Text("主页") }
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.TextCreateScreen.name,
            onClick = { onScreenChange(AppScreen.TextCreateScreen) },
            icon = { Icon(Icons.Default.Add, contentDescription = "创建") },
            label = { Text("创建") }
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.CalendarScreen.name,
            onClick = { onScreenChange(AppScreen.CalendarScreen) },
            icon = { Icon(Icons.Default.DateRange, contentDescription = "日历") },
            label = { Text("日历") }
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.SettingsScreen.name,
            onClick = { onScreenChange(AppScreen.SettingsScreen) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
            label = { Text("设置") }
        )
    }
}
 