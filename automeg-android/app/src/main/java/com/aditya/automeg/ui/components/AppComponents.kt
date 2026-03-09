package com.aditya.automeg.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.automeg.ui.theme.ElectricBlue
import com.aditya.automeg.ui.theme.GlassSurface
import com.aditya.automeg.ui.theme.CyberGreen

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.05f),
        contentColor = Color.White,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
fun AppGridItem(
    name: String,
    icon: ImageVector,
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onToggle(!isActive) }
            .border(
                width = 1.dp,
                brush = if (isActive) {
                    Brush.verticalGradient(listOf(ElectricBlue.copy(alpha = 0.6f), ElectricBlue.copy(alpha = 0.2f)))
                } else {
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)))
                },
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) ElectricBlue.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f)
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) ElectricBlue else Color.Gray.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = name,
                    fontSize = 10.sp,
                    color = if (isActive) Color.White else Color.Gray,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            // Active Indicator Dot
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(CyberGreen, CircleShape)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
fun MainSwitch(
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isActive) }
            .border(
                width = 1.2.dp,
                brush = if (isActive) {
                    Brush.horizontalGradient(listOf(ElectricBlue.copy(alpha = 0.8f), ElectricBlue.copy(alpha = 0.3f)))
                } else {
                    Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.1f)))
                },
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = if (isActive) ElectricBlue.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Agent Status",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = if (isActive) "Autonomous Mode Active" else "Agent Offline",
                    color = if (isActive) ElectricBlue else Color.Gray,
                    fontSize = 14.sp
                )
            }
            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ElectricBlue,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}
