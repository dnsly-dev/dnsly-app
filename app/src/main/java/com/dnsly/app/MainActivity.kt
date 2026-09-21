package com.dnsly.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dnsly.app.data.DnsRepository
import com.dnsly.app.service.DnsVpnService
import com.dnsly.app.service.blocklist.BlocklistManager
import com.dnsly.app.ui.screens.DnsListScreen
import com.dnsly.app.ui.screens.MainScreen
import com.dnsly.app.ui.screens.ReportsScreen
import com.dnsly.app.ui.screens.ShieldScreen
import com.dnsly.app.ui.theme.DNSlyTheme

enum class MainNavTab {
    HOME,
    ACTIVITY
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: DnsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = DnsRepository.getInstance(applicationContext)
        com.dnsly.app.service.api.DnslyApiClient.getInstance(applicationContext).scheduleHeartbeat(repository)

        requestNotificationPermission()

        setContent {
            DNSlyTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DnslyApp(
                        repository = repository,
                        onToggleVpn = { handleVpnToggle() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) {
            com.dnsly.app.service.api.DnslyApiClient.getInstance(applicationContext).scheduleHeartbeat(repository)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleVpnToggle() {
        val isConnected = repository.isVpnConnected.value
        if (isConnected) {
            stopVpnService()
        } else {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                vpnLauncher.launch(vpnIntent)
            } else {
                startVpnService()
            }
        }
    }

    private val vpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "VPN permission is required for DNS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopVpnService() {
        val intent = Intent(this, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_STOP
        }
        startService(intent)
    }
}

@Composable
fun DnslyApp(
    repository: DnsRepository,
    onToggleVpn: () -> Unit
) {
    val navController = rememberNavController()
    val isConnected by repository.isVpnConnected.collectAsState()
    val blockedQueries by repository.blockedQueries.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(MainNavTab.HOME) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == MainNavTab.HOME,
                            onClick = { selectedTab = MainNavTab.HOME },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == MainNavTab.HOME) Icons.Filled.Shield else Icons.Outlined.Shield,
                                    contentDescription = "Home"
                                )
                            },
                            label = {
                                Text(
                                    "Home",
                                    fontWeight = if (selectedTab == MainNavTab.HOME) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == MainNavTab.ACTIVITY,
                            onClick = { selectedTab = MainNavTab.ACTIVITY },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (blockedQueries > 0) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            ) {
                                                Text(if (blockedQueries > 99) "99+" else "$blockedQueries")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selectedTab == MainNavTab.ACTIVITY) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                        contentDescription = "Activity"
                                    )
                                }
                            },
                            label = {
                                Text(
                                    "Activity",
                                    fontWeight = if (selectedTab == MainNavTab.ACTIVITY) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            ) { innerPadding ->
                Crossfade(
                    targetState = selectedTab,
                    animationSpec = tween(durationMillis = 200),
                    modifier = Modifier.padding(innerPadding),
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        MainNavTab.HOME -> {
                            MainScreen(
                                repository = repository,
                                onToggleVpn = onToggleVpn,
                                onNavigateToList = { navController.navigate("list") },
                                onNavigateToReports = { selectedTab = MainNavTab.ACTIVITY },
                                onNavigateToShield = { navController.navigate("shield") },
                                onNavigateToAbout = { navController.navigate("about") }
                            )
                        }
                        MainNavTab.ACTIVITY -> {
                            ReportsScreen(
                                repository = repository
                            )
                        }
                    }
                }
            }
        }
        composable("list") {
            DnsListScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onServerSelected = {
                    if (isConnected) {
                        onToggleVpn()
                        onToggleVpn()
                    }
                    navController.popBackStack()
                }
            )
        }
        composable("shield") {
            val context = androidx.compose.ui.platform.LocalContext.current
            val blocklistManager = androidx.compose.runtime.remember { BlocklistManager.getInstance(context) }
            ShieldScreen(
                blocklistManager = blocklistManager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("about") {
            com.dnsly.app.ui.screens.AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
