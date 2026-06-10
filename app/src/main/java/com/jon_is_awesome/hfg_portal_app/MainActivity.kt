package com.jon_is_awesome.hfg_portal_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jon_is_awesome.hfg_portal_app.ui.auth.AuthViewModel
import com.jon_is_awesome.hfg_portal_app.ui.auth.AuthViewModelFactory
import com.jon_is_awesome.hfg_portal_app.ui.auth.LoginScreen
import com.jon_is_awesome.hfg_portal_app.ui.budget.BudgetRemoteViewModel
import com.jon_is_awesome.hfg_portal_app.ui.budget.BudgetRemoteViewModelFactory
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarMaintenanceViewModel
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarMaintenanceViewModelFactory
import com.jon_is_awesome.hfg_portal_app.ui.navigation.NavGraph
import com.jon_is_awesome.hfg_portal_app.ui.navigation.Screen
import com.jon_is_awesome.hfg_portal_app.ui.theme.Hfg_portal_appTheme
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Hfg_portal_appTheme {
                val app = applicationContext as HfgApplication
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(app.remoteDataSource)
                )
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

                when (isLoggedIn) {
                    true -> MainScreen(authViewModel)
                    false -> LoginScreen(viewModel = authViewModel, onLoginSuccess = {})
                    null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val context = LocalContext.current
    val app = context.applicationContext as HfgApplication
    val userPrefs = app.userPreferencesRepository
    val userName by userPrefs.userName.collectAsState(initial = "User")

    val budgetVm: BudgetRemoteViewModel = viewModel(
        factory = BudgetRemoteViewModelFactory(app, app.remoteDataSource)
    )
    val carVm: CarMaintenanceViewModel = viewModel(
        factory = CarMaintenanceViewModelFactory(app, app.remoteDataSource)
    )

    var isAdminMode by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val refreshAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
        label = "refreshRotation"
    )

    // On first launch, navigate directly to the last accessed section
    LaunchedEffect(Unit) {
        val lastSection = userPrefs.lastSection.first()
        if (!lastSection.isNullOrBlank()) {
            navController.navigate(lastSection) {
                popUpTo(Screen.Dashboard.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val menuItems = listOf(
        Screen.Budget,
        Screen.CarLogs
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1E2632),
                drawerContentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ROOK",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Rounded.Pets, 
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = Color(0xFFD32F2F)
                            )
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.LightGray)
                            Spacer(Modifier.width(16.dp))
                            Text(userName, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        }
                        IconButton(onClick = { authViewModel.logout() }) {
                            Icon(Icons.Rounded.Logout, contentDescription = "Logout", tint = Color.LightGray)
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isAdminMode,
                            onCheckedChange = { isAdminMode = it },
                            colors = CheckboxDefaults.colors(uncheckedColor = Color.White)
                        )
                        Text("Admin Mode", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))

                    menuItems.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        
                        NavigationDrawerItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { 
                                Column {
                                    Text(screen.title)
                                }
                            },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                if (screen.route == Screen.Budget.route || screen.route == Screen.CarLogs.route) {
                                    scope.launch { userPrefs.saveLastSection(screen.route) }
                                }
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color.Transparent,
                                unselectedContainerColor = Color.Transparent,
                                selectedTextColor = Color.White,
                                unselectedTextColor = Color.LightGray,
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color.LightGray
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    Spacer(Modifier.weight(1f))
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Settings.route } == true,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color.Transparent,
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.LightGray,
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.LightGray
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when {
                                currentDestination?.route == Screen.Dashboard.route -> "Home"
                                currentDestination?.route?.startsWith("car_") == true -> "Car Maintenance"
                                currentDestination?.route?.startsWith("budget") == true
                                    || currentDestination?.route?.startsWith("bucket") == true
                                    || currentDestination?.route?.startsWith("add_bucket") == true
                                    || currentDestination?.route?.startsWith("add_transaction") == true -> "Budget"
                                currentDestination?.route == Screen.Settings.route -> "Settings"
                                else -> "HFG Portal"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        val route = currentDestination?.route
                        val isRefreshable = route == Screen.Budget.route
                            || route == Screen.CarLogs.route
                            || route?.startsWith("bucket_detail") == true
                            || route?.startsWith("service_history_detail") == true
                        if (isRefreshable) {
                            val bucketId = navBackStackEntry?.arguments?.getInt("bucketId")
                            val histId = navBackStackEntry?.arguments?.getInt("serviceHistoryId")
                            IconButton(
                                onClick = {
                                    if (!isRefreshing) {
                                        isRefreshing = true
                                        scope.launch {
                                            val minDelay = async { delay(2000) }
                                            val work = async {
                                                when {
                                                    route == Screen.Budget.route ->
                                                        budgetVm.refreshBudgetList()
                                                    route?.startsWith("bucket_detail") == true && bucketId != null ->
                                                        budgetVm.refreshBucketDetail(bucketId)
                                                    route == Screen.CarLogs.route ->
                                                        carVm.refreshCarList()
                                                    route?.startsWith("service_history_detail") == true && histId != null ->
                                                        carVm.refreshServiceHistoryDetail(histId)
                                                }
                                            }
                                            minDelay.await()
                                            work.await()
                                            isRefreshing = false
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = "Refresh",
                                    modifier = Modifier.rotate(if (isRefreshing) refreshAngle else 0f)
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
