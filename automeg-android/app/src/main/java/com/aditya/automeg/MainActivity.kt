package com.aditya.automeg

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.automeg.ui.theme.AutomegandroidTheme

data class SocialApp(
    val name: String,
    val packageName: String,
    val color: Color
)

val SupportedApps = listOf(
    SocialApp("WhatsApp", "com.whatsapp", Color(0xFF25D366)),
    SocialApp("Telegram", "org.telegram.messenger", Color(0xFF0088CC)),
    SocialApp("Instagram", "com.instagram.android", Color(0xFFE1306C)),
    SocialApp("Messenger", "com.facebook.orca", Color(0xFF0084FF))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutomegandroidTheme {
                AutoMegDashboard()
            }
        }
    }
}

@Composable
fun AutoMegDashboard() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("automeg_prefs", Context.MODE_PRIVATE) }

    var isAgentOn by remember {
        mutableStateOf(sharedPrefs.getBoolean("agent_enabled", false))
    }
    var hasNotificationAccess by remember { mutableStateOf(false) }

    val enabledApps = remember {
        mutableStateMapOf<String, Boolean>().apply {
            SupportedApps.forEach { app ->
                put(app.packageName, sharedPrefs.getBoolean(app.packageName, false))
            }
        }
    }

    LaunchedEffect(Unit) {
        hasNotificationAccess = isNotificationServiceEnabled(context)
    }

    val logs = remember {
        mutableStateListOf(
            "System Initialized",
            "Waiting for activation..."
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B0B0B),
                        Color(0xFF140000),
                        Color(0xFF1A0000)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, start = 20.dp, end = 20.dp)
        ) {
            Text(
                text = "AutoMeg",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF4D4D)
            )

            Text(
                text = "Autonomous Agent",
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Permission Warning Card
            if (!hasNotificationAccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FF0000)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Notification Access Required", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Please enable access to extract messages.", color = Color.LightGray, fontSize = 12.sp)
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
                            modifier = Modifier.padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D))
                        ) {
                            Text("Grant Access")
                        }
                    }
                }
            }

            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (isAgentOn) Color(0xFFFF4444) else Color.DarkGray
                    Text(text = "●", color = statusColor, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAgentOn) "ACTIVE" else "OFFLINE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = isAgentOn,
                        onCheckedChange = {
                            isAgentOn = it
                            sharedPrefs.edit().putBoolean("agent_enabled", it).apply()
                            logs.add(if (it) "Agent Started" else "Agent Stopped")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Monitored Apps",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlassCard {
                Column {
                    SupportedApps.forEach { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = app.color,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = app.name, color = Color.White, modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = enabledApps[app.packageName] ?: false,
                                onCheckedChange = { isChecked ->
                                    enabledApps[app.packageName] = isChecked
                                    sharedPrefs.edit().putBoolean(app.packageName, isChecked).apply()
                                    logs.add("${if (isChecked) "Enabled" else "Disabled"} monitoring for ${app.name}")
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFFFF4D4D),
                                    uncheckedColor = Color.Gray
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Activity Logs", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(modifier = Modifier.weight(1f)) {
                LazyColumn {
                    items(logs.asReversed()) { log ->
                        Text(text = "> $log", color = Color(0xFFFF6B6B), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!TextUtils.isEmpty(flat)) {
        val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (name in names) {
            val cn = ComponentName.unflattenFromString(name)
            if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                return true
            }
        }
    }
    return false
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), content = content)
    }
}
