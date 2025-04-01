package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Schedule
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleItem(
    schedule: Schedule,
    onAccept: (Schedule) -> Unit,
    onReject: (Schedule) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行
            Text(
                text = schedule.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 时间信息
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${schedule.startTime.format(formatter)} - ${schedule.endTime.format(formatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 地点信息（如果有）
            if (schedule.location.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "地点: ${schedule.location}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 类别和优先级
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "类别: ${schedule.category} • 优先级: ${schedule.priority}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 描述信息（如果有）
            if (schedule.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = schedule.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 接受按钮
                IconButton(
                    onClick = { onAccept(schedule) }
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "接受",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 拒绝按钮
                IconButton(
                    onClick = { onReject(schedule) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "拒绝",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
} 