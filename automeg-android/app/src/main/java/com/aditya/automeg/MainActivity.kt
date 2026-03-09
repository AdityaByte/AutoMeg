package com.aditya.automeg

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aditya.automeg.memory.ConversationMemory
import com.aditya.automeg.log.LogType
import com.aditya.automeg.memory.MemoryStore
import com.aditya.automeg.log.SystemLog
import com.aditya.automeg.notification.NotificationService
import com.aditya.automeg.ui.components.*
import com.aditya.automeg.ui.theme.AutomegandroidTheme
import com.aditya.automeg.ui.theme.ElectricBlue
import com.aditya.automeg.ui.theme.DarkBg
import com.aditya.automeg.ui.theme.CyberGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SocialApp(
    val name: String,
    val packageName: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

val SupportedApps = listOf(
    SocialApp("WhatsApp", "com.whatsapp", Color(0xFF25D366), Icons.Default.Chat),
    SocialApp("Telegram", "org.telegram.messenger", Color(0xFF0088CC), Icons.Default.Send),
    SocialApp("Instagram", "com.instagram.android", Color(0xFFE1306C), Icons.Default.CameraAlt),
    SocialApp("Messenger", "com.facebook.orca", Color(0xFF0084FF), Icons.Default.Forum),
    SocialApp("Line", "jp.naver.line.android", Color(0xFF00B900), Icons.Default.Chat)
)

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Agent", Icons.Default.SmartToy)
    object Logs : Screen("logs", "History", Icons.Default.History)
    object SystemLogs : Screen("system_logs", "Logs", Icons.Default.Terminal)
    object Identity : Screen("identity", "Identity", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutomegandroidTheme {
                MainAppContainer()
            }
        }
    }
}

@Composable
fun MainAppContainer() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("automeg_prefs", Context.MODE_PRIVATE) }

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        containerColor = DarkBg
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(sharedPrefs)
            }
            composable(Screen.Logs.route) {
                LogsScreen()
            }
            composable(Screen.SystemLogs.route) {
                SystemLogsScreen()
            }
            composable(Screen.Identity.route) {
                IdentityScreen(sharedPrefs)
            }
        }
    }
}

@Composable
fun AppBottomNavigation(navController: NavHostController) {
    val items = listOf(Screen.Dashboard, Screen.Logs, Screen.SystemLogs, Screen.Identity)
    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = Modifier.background(Color.Black.copy(alpha = 0.2f))
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = null) },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ElectricBlue,
                    selectedTextColor = ElectricBlue,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = ElectricBlue.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
fun DashboardScreen(sharedPrefs: android.content.SharedPreferences) {
    val context = LocalContext.current
    val memoryStore = remember { MemoryStore(context) }
    var isAgentOn by remember { mutableStateOf(sharedPrefs.getBoolean("agent_enabled", false)) }
    var hasAccess by remember { mutableStateOf(isNotificationServiceEnabled(context)) }

    val enabledApps = remember {
        mutableStateMapOf<String, Boolean>().apply {
            SupportedApps.forEach { app ->
                put(app.packageName, sharedPrefs.getBoolean(app.packageName, false))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("AutoMeg", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = ElectricBlue)
        Text("Autonomous Messaging Agent", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(32.dp))

        if (!hasAccess) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                color = Color.Red.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Notification Access Required", fontWeight = FontWeight.Bold)
                        Text("Enable access to allow agent to respond.", fontSize = 12.sp)
                    }
                    TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) {
                        Text("GRANT", color = ElectricBlue)
                    }
                }
            }
        }

        MainSwitch(isActive = isAgentOn) { active ->
            isAgentOn = active
            sharedPrefs.edit().putBoolean("agent_enabled", active).apply()
            val status = if (active) "Activated" else "Deactivated"
            memoryStore.addSystemLog("Agent $status", if (active) LogType.SUCCESS else LogType.WARNING)
            if (active) {
                nudgeNotificationService(context)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Monitored Platforms", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(SupportedApps) { app ->
                AppGridItem(
                    name = app.name,
                    icon = app.icon,
                    isActive = enabledApps[app.packageName] ?: false,
                    onToggle = { active ->
                        enabledApps[app.packageName] = active
                        sharedPrefs.edit().putBoolean(app.packageName, active).apply()
                        val status = if (active) "Enabled" else "Disabled"
                        memoryStore.addSystemLog("${app.name} Monitoring $status", if (active) LogType.INFO else LogType.WARNING)
                    }
                )
            }
        }
    }
}

@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val memoryStore = remember { MemoryStore(context) }
    var conversations by remember { mutableStateOf(listOf<ConversationMemory>()) }

    LaunchedEffect(Unit) {
        conversations = memoryStore.getAllConversations().sortedByDescending {
            it.messages.lastOrNull()?.timestamp ?: 0L
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Interaction History", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Recent automated conversations", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No history found", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(conversations) { conv ->
                    val lastMsg = conv.messages.lastOrNull()
                    val appInfo = SupportedApps.find { conv.conversationId.startsWith(it.packageName) }

                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = appInfo?.icon ?: Icons.Default.Chat,
                                contentDescription = null,
                                tint = appInfo?.color ?: ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = appInfo?.name ?: "Unknown",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.weight(1f))
                            val date = lastMsg?.timestamp?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) } ?: ""
                            Text(date, color = Color.Gray, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = lastMsg?.sender ?: "Unknown",
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue,
                            fontSize = 13.sp
                        )
                        Text(
                            text = lastMsg?.text ?: "No message",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SystemLogsScreen() {
    val context = LocalContext.current
    val memoryStore = remember { MemoryStore(context) }
    var logs by remember { mutableStateOf(listOf<SystemLog>()) }

    LaunchedEffect(Unit) {
        logs = memoryStore.getSystemLogs()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("System Terminal", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Background agent activity logs", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            modifier = Modifier.fillMaxSize().weight(1f),
            color = Color.Black.copy(alpha = 0.4f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Terminal ready. Waiting for events...", color = Color.DarkGray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { log ->
                        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        val color = when (log.type) {
                            LogType.SUCCESS -> CyberGreen
                            LogType.WARNING -> Color.Yellow
                            LogType.ERROR -> Color.Red
                            LogType.INFO -> ElectricBlue
                        }

                        Row {
                            Text("[$time]", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = log.message,
                                color = color.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IdentityScreen(sharedPrefs: android.content.SharedPreferences) {
    var identity by remember {
        mutableStateOf(sharedPrefs.getString("user_identity", "") ?: "")
    }
    val context = LocalContext.current
    val memoryStore = remember { MemoryStore(context) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("User Identity", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Provide context about yourself for better agent replies.", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(24.dp))

        GlassCard {
            TextField(
                value = identity,
                onValueChange = { identity = it },
                placeholder = { Text("Example: I am a software engineer, usually busy in the mornings. Be polite but direct.") },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    cursorColor = ElectricBlue,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                sharedPrefs.edit().putString("user_identity", identity).apply()
                memoryStore.addSystemLog("User Identity Updated", LogType.INFO)
                Toast.makeText(context, "Identity Saved", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("SAVE IDENTITY", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "The agent will use this information to personalize responses on your behalf.",
            fontSize = 12.sp,
            color = ElectricBlue.copy(alpha = 0.7f)
        )
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

private fun nudgeNotificationService(context: Context) {
    val componentName = ComponentName(context, NotificationService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        NotificationListenerService.requestRebind(componentName)
    }
    val pm = context.packageManager
    pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
}
